package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctons;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.enums.TicketStatus;
import no.ntnu.exeptions.TicketErrorException;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.util.Compression;
import no.ntnu.util.DebugLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedParameterizedType;
import java.util.Arrays;
import java.util.UUID;

public abstract class Ticket {

    protected static DebugLogger dbl = new DebugLogger(true);

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
    protected File outFile;


    private TicketStatus state = TicketStatus.WAITING;

    // debounce to ensure no double runs
    private boolean isRunning = false;

    public TicketStatus getState() {
        // TODO: do a sql call here to avoid a potential sync issue
        return state;
    }

    public String getCommonName() {
        return commonName;
    }

    private void setState(TicketStatus state) {
        System.out.println(state);
        this.state = state;
        try {
            PsqlInterface.updateTicketStatus(ticketId, state);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    protected Ticket(UUID ticketId) {
        dbl.log("NEW TICKET ", ticketId);
        this.ticketId = ticketId;
        this.commonName = Ticket.commonPrefix + ticketId;
        runDir = new File(DockerManager.runDir, commonName);
        saveDir = new File(DockerManager.saveDir, commonName);
        outFile = new File(DockerManager.sendDir,commonName + ".zip");
        logDir = new File(DockerManager.logDir, commonName);
        logDir.mkdir();

    }


    /**
     * Spawns a thread that builds the image if it isnt alredy built and starts the ticket
     */
    public void run() {
        // TODO: this shold return a bool sying whether or not it was sucsessfull

        // avoid potentially running doubble
        if (this.isRunning){
            return;
        }

        this.isRunning = true;
        dbl.log("start requested for aaaaa \n\n\n\n", this.getTicketId());

        // If the image exists bump the ticket to redy
        if(this.state.equals(TicketStatus.WAITING) && this.doesImageExist()){
            dbl.log("image found for id bumping it", this.getTicketId());
            setState(TicketStatus.READY);
        }

        Thread thread = new Thread(()->{
            try{
                switch (this.getState()){
                    case WAITING:
                        this.build();
                    case INSTALLING:
                        if (!doesImageExist()){// todo: messy remove
                            this.build();
                        }
                        this.buildThread.join();
                    case READY:
                        if (!this.doesImageExist()){
                            throw new TicketErrorException();
                        }
                        DockerRunCommand runCommand = this.getStartCommand();
                        //runCommand.setDumpIO(true);

                        runCommand.setErrorFile(new File(this.logDir, "run_error"));
                        runCommand.setOutputFile(new File(this.logDir, "run_out"));
                        runCommand.setOnComplete(this::onComplete);

                        this.setState(TicketStatus.RUNNING);
                        runCommand.run();
                }
            } catch (TicketErrorException e){
                dbl.log("Build error on ticket ", ticketId);
                this.setState(TicketStatus.VOIDED);
            } catch (Exception e){
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
        dbl.log("build requested for ", this.getTicketId());
        if (this.state.equals(TicketStatus.WAITING)) {
            this.buildThread = new Thread(() -> {
                dbl.log("start requested for\n\n\n\n bbbbbbbbbbbbbbb \n\n\n\n", this.getTicketId());
                if (buildRunTypeTicket()){
                    this.setState(TicketStatus.READY);
                } else {
                    // build error void ticket
                    this.voidTicket();
                }
            });
            this.setState(TicketStatus.INSTALLING);
            this.buildThread.start();
        }
    }

    public boolean isDone() {
        return this.state.equals(TicketStatus.DONE) || this.state.equals(TicketStatus.VOIDED) ;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    protected abstract DockerRunCommand getStartCommand();

    protected abstract ApiConfig getTicketConfig();

    /**
     * Builds the image(es) and does the actions needed for the ticket to be run
     *
     *
     * @return whether or not the build was successful
     */
    protected abstract boolean buildRunTypeTicket();



    private boolean doesImageExist(){
        UUID[] imageList = DockerFunctons.getImages();
        return Arrays.stream(imageList).anyMatch(uuid -> uuid.equals(this.ticketId));
    }

    private boolean isTicketThreadFree(){
        boolean free = true;

        if (this.buildThread != null){
            if (this.buildThread.isAlive()){
                free = false;
            }
        }
        return free;
    }

    /**
     * Some error has caused this ticket to become un completeable log what can be logged and remove the ticket
     */
    private void voidTicket() {
        this.setState(TicketStatus.VOIDED);
        System.out.println("// ############### TICKET VOIDED ############### //");
        System.out.println("ticket id: " + this.ticketId);
        System.out.println("// ############################################# //");

    }

    /**
     * removes the run files, logs and so on for the ticket
     */
    private void cleanOutRemains(){

    }



    private void onComplete(Process process, Throwable throwable){
        System.out.println(process.exitValue());
        System.out.println(throwable);
        if (process.exitValue() == 0){
            // all ok save and complete
            try {
                Compression.zip(saveDir, outFile);
                this.setState(TicketStatus.DONE);
                // bip.bop.sendMail skal her
                TicketDoneMail.sendMail(ApiConfig.getReturnMail(new File(runDir, ApiConfig.commonConfigName)), this);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            dbl.log("RUN ERROR FOR TICKET ", ticketId);
            this.setState(TicketStatus.VOIDED);
        }

        // ether way remove the images
        DockerFunctons.cleanTicket(this.ticketId);
    }

    // Mabye move theese over to DockerCommand


    public static void moveTicketToBuildHelperContext(Ticket ticket) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("bash", "-c","mv", ticket.runDir.getCanonicalPath(), DockerManager.buildHelpers.getAbsolutePath() + "/" );
        builder.start().waitFor();
    }

    public static void moveTicketBackFromBuildHelperContext(Ticket ticket) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("bash", "-c","mv", new File(DockerManager.buildHelpers.getAbsolutePath(), ticket.commonName).getAbsolutePath(), DockerManager.runDir.getAbsolutePath() + "/" );
        builder.start().waitFor();
    }



}
