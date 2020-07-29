package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctions;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.Main;
import no.ntnu.config.ApiConfig;
import no.ntnu.dockerComputeRecources.ResourceManager;
import no.ntnu.dockerComputeRecources.ResourceType;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.exeptions.TicketErrorException;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.util.Compression;
import no.ntnu.util.DebugLogger;
import no.ntnu.util.Mail;


import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

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


    private Vector<String> resourceAllocationCommand = new Vector<>();

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
        System.out.println(state);
        this.state = state;
        PsqlInterface.updateTicketStatus(ticketId, state);

    }

    /**
     * Creates a ticket
     * @param ticketId the id to give the ticket
     */
    protected Ticket(UUID ticketId) {
        dbl.log("NEW TICKET ", ticketId);
        this.ticketId = ticketId;
        this.commonName = Ticket.commonPrefix + ticketId;
        runDir = new File(DockerManager.runDir, commonName);
        saveDir = new File(DockerManager.saveDir, commonName);
        outFile = new File(DockerManager.sendDir,commonName + ".zip");
        logDir = new File(DockerManager.logDir, commonName);
        saveDir.mkdir();
        logDir.mkdir();

    }



    /**
     * Sets the contents of the resources allocation part of the docker run command of the ticket
     * @param commandParts the contents of the resources allocation part of the docker run command of the ticket.
     */
    public void setResourceAllocationCommand(Vector<String> commandParts){
        resourceAllocationCommand = commandParts;
    }


    /**
     * Starts the build -> run -> save cycle. If the image is already bult it wil not be rebuilt
     *
     * This wil spawn another thread and will not block
     */
    public void run() {
        // avoid potentially running doubble
        if (this.isRunning){
            // todo: this shold throw an exeption
            return;
        }

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
                        runCommand.setResourceAllocationParts(this.resourceAllocationCommand);

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


    //todo: mabye remove
    public abstract ApiConfig getTicketConfig();


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
        this.setState(TicketStatus.VOIDED);
        System.out.println("\n// ############### TICKET VOIDED ############### //");
        System.out.println("ticket id: " + this.ticketId);
        System.out.println("// ############################################# //\n");

        try {
            Compression.zip(logDir, new File(saveDir, "logs.zip"));
            Compression.zip(saveDir, outFile);
            this.sendCompleteMail(voidReason);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                this.sendCompleteMail(TicketExitReason.complete);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            dbl.log("RUN ERROR FOR TICKET ID:", ticketId);
            this.voidTicket(TicketExitReason.runError);
        }

        // ether way remove the images
        DockerFunctions.cleanTicket(this.ticketId);
    }

    public static Ticket getTicketFromUUID(UUID ticketID){
        Ticket ticket = null;

        try {

            File ticketRunDir = new File(DockerManager.runDir, Ticket.commonPrefix + ticketID);
            RunType runType = ApiConfig.getRunType(new File(ticketRunDir, ApiConfig.commonConfigName));

            ticket = switch (runType){
                case JAVA -> new JavaTicket(ticketID, 1);
                case PYTHON -> new PythonTicket(ticketID, 1);
                default -> null;
            };


        } catch (FileNotFoundException e){
            dbl.log("Config not found voiding ticket");
            PsqlInterface.updateTicketStatus(ticketID, TicketStatus.VOIDED);
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }

        return ticket;
    }


    private void sendCompleteMail(TicketExitReason reason){
        String subject = "";
        String contents = "";
        String dlLink = "https://remote-run.uials.no/download/" + commonName;
        switch (reason){
            case complete:
                subject = "Your run ticket is complete";
                contents = "your results can be downloaded from " + dlLink;
                break;
            case runError:
                subject = "Your run ticket encountered an error";
                contents = "your results (if any) and the error logs can be downloaded from " + dlLink;
                break;
            case buildError:
                subject = "Your run ticket encountered an error";
                contents = "your results (if any) and the error logs can be downloaded from " + dlLink;
                break;
            case mavenInstallError:
                subject = "Your run ticket encountered an error";
                contents = "your results (if any) and the error logs can be downloaded from " + dlLink;
                break;
            case timeout:
                subject = "";
                contents = "";
                break;
        }
        try {
            Mail.sendGmail(ApiConfig.getReturnMail(new File(runDir, ApiConfig.commonConfigName)), subject,contents);
        } catch (IOException e){}

    }
}
