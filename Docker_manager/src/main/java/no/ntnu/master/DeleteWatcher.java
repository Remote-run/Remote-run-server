package no.ntnu.master;

import no.ntnu.DockerManager;
import no.ntnu.sql.TicketDbFunctions;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.FileUtils;

import java.io.File;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class DeleteWatcher extends Watcher {

    /*
    Delete unused resource keys

     */


    public DeleteWatcher(Long waitTime) {
        super(waitTime);
    }

    private void deleteTicketFiles(UUID idToDelete){
        String ticketCommonName = Ticket.commonPrefix + idToDelete;

        File logDir = new File(DockerManager.logDir, ticketCommonName);
        File saveDir = new File(DockerManager.saveDir, ticketCommonName);
        File runDir = new File(DockerManager.runDir, ticketCommonName);
        File outFile = new File(DockerManager.sendDir, ticketCommonName + ".zip");

        dbl.log("-- Deleting ", idToDelete, " data --");

        FileUtils.deleteDir(logDir);
        FileUtils.deleteDir(saveDir);
        FileUtils.deleteDir(runDir);

        outFile.delete();
    }

    /**
     * The actions the watcher needs to preform at every time step
     */
    @Override
    public void act() {
        HashMap<UUID, Long> killList = TicketDbFunctions.getCompleteList();
        Long currentTime = Instant.now().getEpochSecond();

        killList.forEach((uuid, kill_at) -> {
            if (kill_at < currentTime){
                dbl.log("Deleting ticket files for : ", uuid);
                deleteTicketFiles(uuid);

                TicketDbFunctions.cleanTicket(uuid);

            }
        });
    }
}
