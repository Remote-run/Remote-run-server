package no.ntnu;


import no.ntnu.DockerInterface.DockerFunctions;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import java.util.Vector;

/**
 *
 * TODO:
 *      On powerup:
 *          - the INSTALLING ticket have to be flushed on porerup
 *          - remove the run and logg files for the instances that are listed as runnin in the db to avoid conflict
 *          - add the conteiner network if dont exist
 *
 */
public class PowerOnChecks {

    /**
     * Returns an array with the tickets that are listed as running in the db, and resets the state to ready.
     * These tickets where probably running when the last poweroff happened.
     * @return a array with the tickets that are listed as running in the db.
     */
    public static Ticket[] getWhereRunning(){
        Vector<Ticket> whereRunning = getTicketsWithState(TicketStatus.RUNNING);
        whereRunning.forEach(ticket -> {
            ticket.setState(TicketStatus.READY); // unshure whether or not to do this here
        });
        return whereRunning.toArray(Ticket[]::new);
    }

    /**
     * Returns an array with the tickets that are listed as waiting in the db.
     * @return a array with the tickets that are listed as waiting in the db.
     */
    public static Ticket[] getWhereReady(){
        Vector<Ticket> WhereReady = getTicketsWithState(TicketStatus.READY);

        return WhereReady.toArray(Ticket[]::new);
    }




    /**
     * Deletes all images related to the tickets listed as installing in the db, and resets the ticket state to waiting.
     * These tickets where probably installing when the last poweroff happened, and may have created half initialized images
     */
    public static void resetWhereInstalling(){
        Vector<Ticket> whereInstalling = getTicketsWithState(TicketStatus.INSTALLING);
        whereInstalling.forEach(ticket -> {
            DockerFunctions.cleanTicket(ticket.getTicketId());
            ticket.setState(TicketStatus.WAITING);
        });
    }


    /**
     * goes throgh all the save data dirs and deletes all files that does not have a recognized id
     */
    public static void removeUnusedFiles(){
        Vector<Ticket> toBacklog = new Vector<>();
        try {
            UUID[] allTicketUUID = PsqlInterface.getAllTicketUUID();
            String[] commonTicketNames = Arrays.stream(allTicketUUID)
                    .map(uuid -> Ticket.commonPrefix + uuid)
                    .toArray(String[]::new);

            Arrays.sort(commonTicketNames);// tecnicly i cold do this once for the uuid and i think the streams wold preserve order

            String[] commonZipNames = Arrays.stream(allTicketUUID)
                    .map(uuid -> Ticket.commonPrefix + uuid + ".zip")
                    .toArray(String[]::new);

            Arrays.sort(commonTicketNames);

            File[] checkLocations = new File[]{
                    DockerManager.runDir,
                    DockerManager.saveDir,
                    DockerManager.logDir
            };

            // check all the run save and log dir for dirs with names that are not in the db
            Arrays.asList(checkLocations).forEach(file -> {
                File[] contents = file.listFiles();

                if (contents != null){
                    Arrays.asList(contents).forEach(checkFile -> {
                        if (Arrays.binarySearch(commonTicketNames, checkFile.getName())< 0){
                            FileUtils.deleteDir(checkFile);
                        }
                    });
                }
            });

            // chek the out dir for files that are not in the db
            File[] sendDirContents = DockerManager.sendDir.listFiles();
            if (sendDirContents != null){
                Arrays.asList(sendDirContents).forEach(checkFile -> {
                    if (Arrays.binarySearch(commonZipNames, checkFile.getName())< 0){
                        FileUtils.deleteDir(checkFile);
                    }
                });
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Returns a vector with the id's of all ticket registerd with the given state
     * @param status the state that the tickets you want shold have
     * @return a vector containg all the tickets with the provided state
     */
    private static Vector<Ticket> getTicketsWithState(TicketStatus status){
        Vector<Ticket> tickets = new Vector<>();
        try {
            UUID[] ids = PsqlInterface.getTicketsWithStatus(status);

            if (ids.length != 0){
                Arrays.stream(ids)
                        .map(Ticket::getTicketFromUUID)
                        .forEach(tickets::add);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return tickets;
    }


}
