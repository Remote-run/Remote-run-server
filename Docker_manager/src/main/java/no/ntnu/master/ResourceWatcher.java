package no.ntnu.master;

import no.ntnu.dockerComputeRecources.WorkerNodeResourceManager;
import no.ntnu.sql.SystemDbFunctions;
import no.ntnu.sql.TicketDbFunctions;
import no.ntnu.ticket.Ticket;

import java.time.Instant;
import java.util.*;

public class ResourceWatcher extends Watcher{

    // TODO: move this to wherever the chekin waiting is set
    private Long checkInIntervalBuffer = 30L; // 30 sec leway
    private Long checkInInterval = 60L; // 60 sec checkIn


    private Vector<WorkerNodeResourceManager> workers = new Vector<>();

    public ResourceWatcher(Long waitTime, Long checkInIntervalBuffer, Long checkInInterval) {
        super(waitTime);
        this.checkInIntervalBuffer = checkInIntervalBuffer;
        this.checkInInterval = checkInInterval;
    }



    /**
     * Checks that all the worker has completed their last check in and add any missing ones
     *
     * Also cheks that all the currently mapped resource usage is correct
     *
     */
    private void updateWorkers(){
        HashMap<UUID,Long> nodeIdAndTimeots = SystemDbFunctions.getComputeNodesWithCheckIn();


        long currentTime = Instant.now().getEpochSecond();

        dbl.log("curr workers ",nodeIdAndTimeots.toString());

        nodeIdAndTimeots.forEach((workerId, checkIn) -> {
            boolean missedCheckIn = checkIn + checkInInterval + checkInIntervalBuffer < currentTime;
            if (workers.stream().map(WorkerNodeResourceManager::getWorkerID).anyMatch(workerId::equals)){
                // is a currently known node with a resource manager
                if (missedCheckIn){
                    // if the last check in is more than an interval and buffer back in time
                    dbl.log("worker", workerId , " missed check in  deleteing it.");
                    SystemDbFunctions.cleanOutWorker(workerId);
                    workers.removeIf(worker -> worker.getWorkerID().equals(workerId));
                } else {
                    // valid checkIn. update resourceUsage
                    workers.forEach(WorkerNodeResourceManager::updateResourceUsage);
                }
            } else {
                // is a node without a resourceManager
                if (missedCheckIn){
                    // old with missed timeout delete
                    dbl.log("worker", workerId , " missed check in  deleteing it.");
                    SystemDbFunctions.cleanOutWorker(workerId);
                    workers.removeIf(worker -> worker.getWorkerID().equals(workerId));
                } else {
                    // valid checkIn. create node
                    dbl.log("New worker", workerId , " found adding it.");
                    workers.add(SystemDbFunctions.getWorkerResourceManagerById(workerId));
                }

            }

        });

    }


    private void activateTickets(){
        UUID[] sortedQue = TicketDbFunctions.getTicketsByPriority();



        Arrays.stream(sortedQue)
                .map(Ticket::new)
                .forEach(ticket -> {

                    TicketDbFunctions.setTicketResourceKey(ticket.getTicketId(), ticket.getResourceKey());

                    WorkerNodeResourceManager[] freeManagers = this.workers.stream()
                            .filter(resourceManager -> resourceManager.areTicketResourcesFree(ticket))
                            .toArray(WorkerNodeResourceManager[]::new);

                    if (freeManagers.length > 0){
                        // there is one or more managers capable of starting this ticket. pick the first one
                        freeManagers[0].stageAndAllocateTicketForWorker(ticket);
                        dbl.log("Directly starting ", ticket.getCommonName(), " on runner ", freeManagers[0].getWorkerID());

                    } else {
                        // no manager can start the ticket currently so the ticket is put in the backlog of the
                        // first capable worker with the smallest backlog. if any exists
                        Vector<UUID> workerIdsByBacklog = SystemDbFunctions.getWorkerIdsSortedByBacklog();
                        WorkerNodeResourceManager[] capableManagers = this.workers.stream()
                                .filter(resourceManager -> resourceManager.isCapableOfTicket(ticket))
                                .sorted(Comparator.comparingInt(o -> workerIdsByBacklog.indexOf(o.getWorkerID())))
                                .toArray(WorkerNodeResourceManager[]::new);

                        if (capableManagers.length > 0){
                            //Arrays.sort(capableManagers, Comparator.comparingInt(o -> workerIdsByBacklog.indexOf(o.getWorkerID())));
                            capableManagers[0].stageTicketForWorker(ticket);
                            dbl.log("Adding ", ticket.getCommonName(), " to backlog on runner ", freeManagers[0].getWorkerID());
                        } else {
                            dbl.log("\n NO CAPABLE RUNNERS FOR:", ticket.getTicketId());
                          // TODO: this means there are no system capable of running the ticket atm just wait
                        }

                    }
                });
    }



    /**
     * The actions the watcher needs to preform at every time step
     */
    @Override
    public void act() {
        this.updateWorkers();
        this.activateTickets();
    }
}
