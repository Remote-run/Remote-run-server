package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctons;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.enums.TicketStatus;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.util.Compression;
import no.ntnu.util.DebugLogger;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

public abstract class Ticket {

    private static DebugLogger dbl = new DebugLogger(true);

    public static final String commonPrefix = "ticket_";

    /**
     * Simple way of ensuring the container wont have 2 build opperations
     * or a build and an run opperation at the same time.
     *
     * The ticket only have one thread slot to do stuff
     */
    public Thread runThread = null;

    protected UUID ticketId;
    protected String commonName;

    protected File runDir;
    protected File saveDir;
    protected File outFile;

    private boolean done = false;
    private boolean started = false;

    protected Ticket(UUID ticketId) {
        this.ticketId = ticketId;
        this.commonName = Ticket.commonPrefix + ticketId;
        runDir = new File(DockerManager.runDir, commonName);
        saveDir = new File(DockerManager.saveDir, commonName);
        outFile = new File(DockerManager.sendDir,commonName + ".zip");
    }

    public boolean isDone() {
        return done;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    protected abstract DockerRunCommand getStartCommand();

    protected abstract ApiConfig getTicketConfig();

    private boolean doesImageExist(){
        UUID[] imageList = DockerFunctons.getImages();
        return Arrays.stream(imageList).anyMatch(uuid -> uuid.equals(this.ticketId));
    }

    private boolean isTicketThreadFree(){
        boolean free = true;

        if (this.runThread != null){
            if (this.runThread.isAlive()){
                free = false;
            }
        }
        dbl.log("is thred free ", free);
        return free;
    }



    /**
     * Spawns another thread to build the image.
     * This wil be called from run if the image does not exist.
     * Can be called premtivly here to ready the ticket
     */
    public void build() {
        dbl.log("build started");
        if (isTicketThreadFree()) {
            dbl.log("fr build started");
            this.runThread = new Thread(() -> {
                dbl.log("b thread");
                buildRunTypeTicket();
                try {
                    PsqlInterface.updateTicketStatus(ticketId, TicketStatus.READY);
                } catch (Exception e ){
                    e.printStackTrace();
                }
            });
            this.runThread.start();
            /*try {
                // this is not ideal the whole wher is who owns has to be sorted out
                runThread.join();
            }catch (Exception e){e.printStackTrace();}*/

        }
        // todo: maby return a bool tru false flag here
    }


    /**
     * Builds the image(es) and does the actions needed for the ticket to be run
     *
     *
     */
    protected abstract void buildRunTypeTicket();


    /**
     * Spawns a thread that builds the image if it isnt alredy built and starts the ticket
     */
    public void run() {
        // jup, i am aware this is a semi horible way of doing it. At the end of the day there are not going to be
        //      so many tickets at once that generating 3 fluff threds per ticket is going to cause any mesurable
        //      preformance impact. Simplicity is more important

        if (!this.started){
            this.started = true;
            Thread thread = new Thread(()->{
                dbl.log("try start run");
                try {
                    if (!this.doesImageExist()) {
                        dbl.log("no image found");

                        if (isTicketThreadFree()) {
                            dbl.log("trying to make image");
                            build();
                        }

                        dbl.log("waiting for image completion");
                        // Ether we just started, or the ticket thread was probably alredy, building the image.
                        // so we just wait for it
                        this.runThread.join();
                        dbl.log("image is built");
                    }

                    if (!this.doesImageExist()) {
                        // TODO: super change this later
                        //          atm its here to hardblock wrongly coded tickets
                        throw new RuntimeException("BUILD ERROR");
                    }
                    DockerRunCommand runCommand = this.getStartCommand();

                    runCommand.setOnComplete(this::onComplete);
                    PsqlInterface.updateTicketStatus(ticketId, TicketStatus.RUNNING);
                    runCommand.run();
                } catch (Exception e){
                    e.printStackTrace();
                }
            });
            thread.start();
        }

    }



    private void onComplete(Process process, Throwable throwable){
        System.out.println(process.exitValue());
        System.out.println(throwable);
        this.done = true;

        try {
            Compression.zip(saveDir, outFile);
            PsqlInterface.updateTicketStatus(this.ticketId, TicketStatus.DONE);
            DockerFunctons.cleanTicket(this.ticketId);
            // bip.bop.sendMail skal her
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
