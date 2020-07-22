package no.ntnu;

import no.ntnu.config.ApiConfig;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.ticket.JavaTicket;
import no.ntnu.ticket.PythonTicket;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.DebugLogger;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO: Fix the absoulute fuking chaos that is exeption handeling here
 */


public class DockerManager {



    private static final DebugLogger dbl = new DebugLogger(true);

    public static final File systemSaveDataDir = new File(System.getenv("SAVE_DATA_SYS_PATH"));

    public static final File dckerfilesDir = new File("/runtypes");
    public static final File saveDataDir   = new File("/save_data");
    public static final File runDir        = new File(saveDataDir, "run");
    public static final File saveDir       = new File(saveDataDir, "save");
    public static final File logDir        = new File(saveDataDir, "logs");
    public static final File buildHelpers  = new File(saveDataDir, "build_helpers");

    // a dir with no children to use as build context
    public static final File buildHole     = new File(buildHelpers, "build_hole");

    // docker volume not a dir
    public static final File sendDir       = new File("/send");

    public static File translateSaveDataFileToHostFile(File file){
        dbl.log("in path", file);
        String fp = file.getAbsolutePath().replaceFirst(saveDataDir.getAbsolutePath(),"");
        File osPath = new File(systemSaveDataDir, fp);

        dbl.log("out path", osPath);
        return osPath;
    }


    public DockerManager(){

        // delete the files not belonging to any tickets.
        PowerOnChecks.removeUnusedFiles();

        // deletes the half built images and files for the tickets that where installing dureing the last poweroff, if any.
        PowerOnChecks.resetWhereInstalling();

        // puts the tickets that where running during the last poweroff first in que
        this.backlog.addAll(Arrays.asList(PowerOnChecks.getWhereRunning()));

        // appends the ticket that were redy to run but waiting to the que
        this.backlog.addAll(Arrays.asList(PowerOnChecks.getWhereReady()));


        // makes the common dirs if they do not exist
        runDir.mkdir();
        saveDir.mkdir();
        logDir.mkdir();
        buildHelpers.mkdir();
        sendDir.mkdir();
        buildHole.mkdir();
    }


    /**
     * How many images to premtivly build while waiting for a run slot
     */
    private final int queSize = 5;


    /**
     * How many ticket can run simultaniosly. this wil be removed and changed for a recource manager at some point
     */
    private final int runSlots = 1;

    private ArrayList<Ticket> backlog = new ArrayList<>();
    private ArrayList<Ticket> running = new ArrayList<>();

    public void mainLoop(){
        while (true){
            try {
                this.pruneTickets();
                this.updateQue();
                this.updateRunning();



            } catch (Exception e){
                e.printStackTrace();
            }

            try{
                Thread.sleep(5000);
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private void pruneTickets() throws SQLException {
        // removes any ticket that are ether done or voided
        this.running.removeAll(this.running.stream()
                .filter(Ticket::isDone)
                .collect(Collectors.toCollection(ArrayList::new)));


        // if a ticket was voided on install it is removed here
        this.backlog.removeAll(this.backlog.stream()
                .filter(Ticket::isDone)
                .collect(Collectors.toCollection(ArrayList::new)));



        // this should tecnicly never be nececery

        // there should be no item in local running and not in db
        UUID[] que = PsqlInterface.getTicketsWithStatus(TicketStatus.RUNNING);
        Arrays.sort(que);
        assert this.running.stream()
                .map(Ticket::getTicketId)
                .noneMatch(ticket -> Arrays.binarySearch(que,ticket) > 0);

        // there should be no item running in db not in local
        UUID[] tmp = this.running.stream().map(Ticket::getTicketId).toArray(UUID[]::new);
        Arrays.sort(tmp);
        assert  Arrays.stream(que)
                .noneMatch(ticket -> Arrays.binarySearch(que,ticket) > 0);
    }

    private void updateRunning(){
        while (this.running.size() < this.runSlots && this.backlog.size() > 0){
            // super do not like... but probably wont start spinnining so ok
            this.startTicket(backlog.get(0));
        }
    }

    private void updateQue()throws SQLException{
        if (this.backlog.size() < this.queSize){
            UUID[] sortedQue = PsqlInterface.getTicketsByPriority();
            dbl.log("backlog: ", backlog.toString());
            dbl.log("running: ", running.toString());

            for (UUID id: sortedQue){
                if (this.backlog.size() < this.queSize && id != null){
                    if (Stream.of(backlog, running)
                            .flatMap(tickets -> tickets.stream().map(Ticket::getTicketId))
                            .noneMatch(id::equals)){
                        dbl.log(id);
                        this.addToBacklog(id);
                    }

                } else {
                    break;
                }
            }
        }
    }

    //remove if nothing more is added fluff is unececery
    private void addToBacklog(UUID ticketID){
        dbl.log("added ticket:", ticketID);

        Ticket ticket = Ticket.getTicketFromUUID(ticketID);
        if (ticket != null){
            if(ticket.getState() == TicketStatus.WAITING){
                ticket.build();
            }
            this.backlog.add(ticket);
        }
    }

    private void startTicket(Ticket ticket){
        // Todo: put the recource managment stuff here
        dbl.log("started ticket:", ticket.getTicketId());
        ticket.run();
        this.backlog.remove(ticket);
        this.running.add(ticket);
    }




    public static void main( String[] args ) {
        DockerManager manager = new DockerManager();
        manager.mainLoop();
    }
}
