package no.ntnu;

import no.ntnu.sql.PsqlInterface;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.DebugLogger;
import no.ntnu.util.FileUtils;

import java.io.File;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class DeleteWatcher {

    private DebugLogger dbl = new DebugLogger(true);

    /**
     * num seconds to wait between cheking if files shold be deleted
     */
    private int checkInterval = 3600;

    private static DeleteWatcher instance;

    public static DeleteWatcher getInstance(){
        if (instance == null){
            instance = new DeleteWatcher();
        }
        return instance;
    }

    private DeleteWatcher(){
        Thread watchTread = new Thread(this::watchLoop);
        watchTread.start();
    }


    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    private void watchLoop(){
        while (true){
            try{
                HashMap<UUID, Long> killList = PsqlInterface.getKillList();
                Long currentTime = Instant.now().getEpochSecond();

                killList.forEach((uuid, kill_at) -> {
                    if (kill_at < currentTime){
                        dbl.log("Deleting ticket files for : ", uuid);
                        deleteTicketFiles(uuid);
                    }
                });
                Thread.sleep(checkInterval * 100);
            } catch (SQLException | InterruptedException e){
                e.printStackTrace();
            }


        }
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
}
