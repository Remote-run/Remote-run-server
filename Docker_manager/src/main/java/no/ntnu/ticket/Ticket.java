package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctons;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.Packager;
import no.ntnu.enums.TicketStatus;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.util.Compression;

import java.io.File;
import java.security.PrivateKey;
import java.sql.SQLException;
import java.util.UUID;

public abstract class Ticket {

    private UUID ticketId;

    private File runDir;
    private File saveDir;
    private File outFile;



    private boolean done = false;

    protected Ticket(UUID ticketId) {
        this.ticketId = ticketId;
        runDir = new File("/save_data/run/ticket_" + ticketId);
        saveDir = new File("/save_data/save/ticket_" + ticketId);
        outFile = new File("/send/ticket_" + ticketId + ".zip");

    }

    public boolean isDone() {
        return done;
    }

    protected abstract DockerRunCommand getStartCommand();


    /**
     * Builds the image(es) and does the actions needed for the ticket to be run
     */
    protected abstract void build();



    /**
     * Starts the ticket
     */
    private void run(){
        this.build();
        DockerRunCommand runCommand = this.getStartCommand();

        runCommand.setOnComplete(this::onComplete);
        try {
            PsqlInterface.updateTicketStatus(ticketId, TicketStatus.RUNNING);
            runCommand.run();


        } catch (Exception e) {
            e.printStackTrace();
        }


    }



    private void onComplete(){
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
