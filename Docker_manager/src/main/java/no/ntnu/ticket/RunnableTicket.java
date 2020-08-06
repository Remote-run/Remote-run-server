package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctions;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.dockerComputeRecources.ResourceManager;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.exeptions.TicketErrorException;
import no.ntnu.sql.TicketDbFunctions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

public abstract class RunnableTicket extends Ticket {

    // to ensure no double run
    private boolean isRunning = false;


    /**
     * Creates a ticket
     *
     * @param ticketId  the id to give the ticket
     * @param safeBuild
     */
    protected RunnableTicket(UUID ticketId, Boolean safeBuild) {
        super(ticketId);

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

    /**
     * Completes all the build and run actions necessary to be able to run the DockerRunCommand
     * on ticket execution.
     * This method should be blocking until all preparation tasks are done,
     * and should return a bool indicating whether or not the the prep actions where successful
     * @return Whether or not the the prep actions where successful.
     */
    protected abstract boolean buildRunTypeTicket();






    /**
     * The method run on completion of the tickets execution
     * @param process the process of the execution
     * @param throwable if an error occurred, the error, else null
     */
    private void onComplete(Process process, Throwable throwable){
        if (process.exitValue() == 0){
            // all ok save and complete
            try {
                this.compressResults(false);
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


    /**
     * Gets the DockerRunCommand object to run when the ticket should start executing.
     * @return The DockerRunCommand object to run when the ticket should start executing.
     */
    protected abstract DockerRunCommand getStartCommand();

    /**
     * returns the runType api config of the the ticket
     * @return the runType api config of the the ticket
     */
    public abstract ApiConfig getRunTypeConfig();

    /**
     * Returns a bool indicating whether or not a image with the tickets common name exists in the local repo.
     * @return Whether or not a image with the tickets common name exists in the local repo.
     */
    private boolean doesImageExist(){
        UUID[] imageList = DockerFunctions.getTicketImages();
        return Arrays.stream(imageList).anyMatch(uuid -> uuid.equals(this.ticketId));
    }


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
                switch (this.state){
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

    public static RunnableTicket getTicketFromUUID(UUID ticketID) {
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
    public static RunnableTicket getTicketFromUUID(UUID ticketID, boolean safeBuild){
        RunnableTicket ticket = null;

        try {

            File ticketRunDir = new File(DockerManager.runDir, Ticket.commonPrefix + ticketID);
            RunType runType = ApiConfig.getRunType(new File(ticketRunDir, ApiConfig.commonConfigName));

            ticket = switch (runType){
                case JAVA -> new JavaTicket(ticketID, safeBuild);
                case PYTHON -> new PythonTicket(ticketID, safeBuild);
                default -> null;
            };


        } catch (IOException e){
            if (triesMap.containsKey(ticketID)){
                if (triesMap.get(ticketID) > numTries){
                    dbl.log("Config not found voiding ticket");
                    TicketDbFunctions.setTicketComplete(ticketID, TicketExitReason.buildError);
                    e.printStackTrace();
                } else {
                    triesMap.replace(ticketID, triesMap.get(ticketID) + 1);
                }
            } else {
                triesMap.put(ticketID, 1);
            }

        }

        return ticket;
    }
    private static int numTries = 3;
    private static HashMap<UUID,Integer> triesMap = new HashMap<>();
}
