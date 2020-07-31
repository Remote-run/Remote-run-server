package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctions;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.dockerComputeRecources.ComputeResources;
import no.ntnu.dockerComputeRecources.ResourceManager;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.exeptions.TicketErrorException;
import no.ntnu.sql.TicketDbFunctions;
import no.ntnu.util.Compression;
import no.ntnu.util.DebugLogger;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * The representation of a run request
 */
public abstract class Ticket {

    protected static DebugLogger dbl = new DebugLogger(true);

    /**
     * The prefix to be used in common naming of ticket related things like images and dirs
     * to allow for easy distinction of what is system stuff and ticket stuff
     */
    public static final String commonPrefix = "ticket_";

    /**
     * Simple way of ensuring the container wont have 2 build opperations
     * or a build and an run opperation at the same time.
     *
     * The ticket only have one thread slot to do stuff
     */
    public Thread buildThread = null;

    protected UUID ticketId;
    protected String commonName;

    protected File runDir;
    protected File saveDir;
    protected File logDir;
    protected File outDir;
    protected File outFile;


    private TicketStatus state = TicketStatus.WAITING;


    private ComputeResources.ResourceKey resourceKey = ComputeResources.defaultKey;


    public ComputeResources.ResourceKey getResourceKey() {
        if (this.resourceKey.resourceId.equals(ComputeResources.defaultKey.resourceId)){
            try {
                dbl.log(this.getRunTypeConfig());
                dbl.log(this.getRunTypeConfig().getResourceKey());
                resourceKey = ComputeResources.TranslateComputeResourceKey(this.getRunTypeConfig().getResourceKey());
            } catch (Exception e){
                dbl.log("error getting the resource key ");
            }

        }

        return resourceKey;
    }


    // to ensure no double run
    private boolean isRunning = false;


    /**
     *  -- tbd -- Have to think about whether or not to use the sql or only local
     */
    public TicketStatus getState() {
        // TODO: do a sql call here to avoid a potential sync issue
        return state;
    }

    /**
     * Returns the common name of the ticket, that is the common prfix + the ticket id
     * @return The common name of the ticket
     */
    public String getCommonName() {
        return commonName;
    }

    /**
     * Sets the the new state of the ticket and updates the database
     * @param state the new state of the ticket
     */
    public void setState(TicketStatus state) {
        dbl.log(state);
        this.state = state;
        TicketDbFunctions.updateTicketStatus(ticketId, state);

    }



    /**
     * Creates a ticket
     * @param ticketId the id to give the ticket
     * @param safeBuild
     */
    protected Ticket(UUID ticketId, Boolean safeBuild) {
        dbl.log("NEW TICKET ", ticketId);
        this.ticketId = ticketId;
        this.commonName = Ticket.commonPrefix + ticketId;
        runDir = new File(DockerManager.runDir, commonName);
        saveDir = new File(DockerManager.saveDir, commonName);
        outFile = new File(DockerManager.sendDir,commonName + ".zip");
        logDir = new File(DockerManager.logDir, commonName);
        saveDir.mkdir();
        logDir.mkdir();
        try{
            this.state = TicketDbFunctions.getTicketStatus(ticketId);
        }catch (Exception e){
            e.printStackTrace();
        }




        // TODO: this has to change its needed but it shold not be run on every ticket build
        if (safeBuild){
            switch (this.state) {
                case INSTALLING, READY, RUNNING -> {
                    DockerFunctions.cleanTicket(ticketId);
                    this.setState(TicketStatus.WAITING);
                }
            }
        }
    }


    private ResourceManager usedResourceManager;

    /**
     * Starts the build -> run -> save cycle. If the image is already bult it wil not be rebuilt
     *
     * This wil spawn another thread and will not block
     */
    public void run(ResourceManager resourceManager) {
        // avoid potentially running doubble
        if (this.isRunning){
            // todo: this shold throw an exeption
            return;
        }
        this.usedResourceManager = resourceManager;

        this.isRunning = true;

        // If the image exists bump the ticket to ready
        if(this.state.equals(TicketStatus.WAITING) && this.doesImageExist()){
            dbl.log("Image found for id, bumping it to ready. ID: ", this.getTicketId());
            setState(TicketStatus.READY);
        }

        Thread thread = new Thread(()->{
            try{
                switch (this.getState()){
                    case WAITING:
                        this.build();
                    case INSTALLING:
                        this.buildThread.join();
                    case READY:


                        // TODO: remove or catch
                        if (!this.doesImageExist()){
                            throw new TicketErrorException("Ticket image is not built");
                        }

                        DockerRunCommand runCommand = this.getStartCommand();
                        runCommand.setResourceAllocationParts(resourceManager.getTicketAllocationCommand(this));

                        runCommand.setErrorFile(new File(this.logDir, "run_error"));
                        runCommand.setOutputFile(new File(this.logDir, "run_out"));
                        runCommand.setOnComplete(this::onComplete);

                        this.setState(TicketStatus.RUNNING);
                        runCommand.run();
                }
            } catch (Exception e){
                this.voidTicket(TicketExitReason.runError);
                e.printStackTrace();
            }
        });
        thread.start();
    }

    /**
     * Spawns another thread to build the image.
     * This wil be called from run if the image does not exist.
     * Can be called premtivly here to ready the ticket
     */
    public void build() {
        dbl.log("Build requested for ID: ", this.getTicketId());
        if (this.state.equals(TicketStatus.WAITING)) {
            this.buildThread = new Thread(() -> {
                if (buildRunTypeTicket()){
                    this.setState(TicketStatus.READY);
                } else {
                    // build error void ticket
                    this.voidTicket(TicketExitReason.buildError);
                }
            });
            this.setState(TicketStatus.INSTALLING);
            this.buildThread.start();
        }
    }

    /**
     * returns the runType api config of the the ticket. 
     * If no config is found null is returned and the ticket is voided
     * 
     * @return returns the runType api config of the the ticket. 
     */
    public ApiConfig getTicketApiConfig(){
        // todo: remove this and do this opperation in the constructor
        ApiConfig apiConfig = getRunTypeConfig();
        if (apiConfig == null){
            //this.voidTicket(TicketExitReason.buildError);
        }
        return apiConfig;
    }

    /**
     * Return whether or not the ticket is don executing that is if it's done or voided
     * @return Whether or not the ticket is don executing.
     */
    public boolean isDone() {
        return this.state.equals(TicketStatus.DONE) || this.state.equals(TicketStatus.VOIDED) ;
    }

    /**
     * Returns the ticket id.
     * @return The ticket id.
     */
    public UUID getTicketId() {
        return ticketId;
    }

    /**
     * Gets the DockerRunCommand object to run when the ticket should start executing.
     * @return The DockerRunCommand object to run when the ticket should start executing.
     */
    protected abstract DockerRunCommand getStartCommand();

    /**
     * returns the runType api config of the the ticket
     * @return the runType api config of the the ticket
     */
    protected abstract ApiConfig getRunTypeConfig();


    /**
     * Completes all the build and run actions necessary to be able to run the DockerRunCommand
     * on ticket execution.
     * This method should be blocking until all preparation tasks are done,
     * and should return a bool indicating whether or not the the prep actions where successful
     * @return Whether or not the the prep actions where successful.
     */
    protected abstract boolean buildRunTypeTicket();


    /**
     * Returns a bool indicating whether or not a image with the tickets common name exists in the local repo.
     * @return Whether or not a image with the tickets common name exists in the local repo.
     */
    private boolean doesImageExist(){
        UUID[] imageList = DockerFunctions.getTicketImages();
        return Arrays.stream(imageList).anyMatch(uuid -> uuid.equals(this.ticketId));
    }


    /**
     * Som error has occurred making the execution of the ticket not possible.
     *
     * the ticket wil be removed and the owner notified
     */
    private void voidTicket(TicketExitReason voidReason) {

        System.out.println("\n// ############### TICKET VOIDED ############### //");
        System.out.println("ticket id: " + this.ticketId);
        System.out.println("void reason: " + voidReason.name());
        System.out.println("// ############################################# //\n");

        try {
            Compression.zip(logDir, new File(saveDir, "logs.zip"));
            Compression.zip(saveDir, outFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setState(TicketStatus.VOIDED);
        this.cleanTicket(voidReason);

    }



    /**
     * The method run on completion of the tickets execution
     * @param process the process of the execution
     * @param throwable if an error occurred, the error, else null
     */
    private void onComplete(Process process, Throwable throwable){
        if (process.exitValue() == 0){
            // all ok save and complete
            try {
                Compression.zip(saveDir, outFile);
                this.setState(TicketStatus.DONE);
                this.cleanTicket(TicketExitReason.complete);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            dbl.log("RUN ERROR FOR TICKET ID:", ticketId);
            this.voidTicket(TicketExitReason.runError);
        }
    }

    private void cleanTicket(TicketExitReason exitReason){
        DockerFunctions.cleanTicket(this.ticketId);
        TicketDbFunctions.setTicketComplete(this.ticketId, exitReason);
        if (this.usedResourceManager != null){
            this.usedResourceManager.freeTicketResources(this);
        }
    }

    public static Ticket getTicketFromUUID(UUID ticketID) {
        return getTicketFromUUID(ticketID, false);
    }

    /**
     * Generates the correct ticket type for the ticket with the given id.
     *
     * This can ether be done un-safely that is not removing whatever progress this ticket has already done, or safely
     * which is basicly resetting the ticket progress
     *
     * if the ticket encounters an error while budding or the id does not exist in the db null is returned
     *
     * @param ticketID the id of the ticket to generate
     * @param safeBuild weather to build safely or not
     * @return The ticket if successful in building null if not
     */
    public static Ticket getTicketFromUUID(UUID ticketID, boolean safeBuild){
        Ticket ticket = null;

        try {

            File ticketRunDir = new File(DockerManager.runDir, Ticket.commonPrefix + ticketID);
            RunType runType = ApiConfig.getRunType(new File(ticketRunDir, ApiConfig.commonConfigName));

            ticket = switch (runType){
                case JAVA -> new JavaTicket(ticketID, safeBuild);
                case PYTHON -> new PythonTicket(ticketID, safeBuild);
                default -> null;
            };


        } catch (FileNotFoundException e){
            dbl.log("Config not found voiding ticket");
            TicketDbFunctions.updateTicketStatus(ticketID, TicketStatus.VOIDED);
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }

        return ticket;
    }



}
