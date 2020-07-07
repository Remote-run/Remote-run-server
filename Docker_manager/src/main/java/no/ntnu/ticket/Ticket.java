package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.sql.PsqlInterface;

import java.io.File;

public abstract class Ticket {

    private int ticketId;

    /**
     * Builds the image(es) and does the actions needed for the ticket to be run
     */
    protected abstract void build();

    /**
     * Starts the ticket
     */
    private void run(){

    }

    protected abstract DockerRunCommand getStartCommand();

    private void updateSqlStatus(){
        PsqlInterface.
    }


    private boolean onComplete(){
        PsqlInterface.
    }

    private File getRunFile(){
        File runDir = new File("/save_data/run/ticket_" + ticketId);

        if (runDir.isDirectory()){
            return runDir;
        } else {
            return null;
        }
    }

    private File getSaveFile(){
        File savedir = new File("/save_data/save/ticket_" + ticketId);

        if (savedir.isDirectory()){
            return savedir;
        } else {
            return null;
        }

    }
}
