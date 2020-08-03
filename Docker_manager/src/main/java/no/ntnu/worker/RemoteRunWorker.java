package no.ntnu.worker;

import no.ntnu.dockerComputeRecources.ComputeResources;
import no.ntnu.dockerComputeRecources.ResourceManager;
import no.ntnu.dockerComputeRecources.ResourceTypes.ComputeResource;
import no.ntnu.enums.TicketStatus;
import no.ntnu.sql.SystemDbFunctions;
import no.ntnu.sql.TicketDbFunctions;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.DebugLogger;

import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;

public class RemoteRunWorker {
    private DebugLogger dbl = new DebugLogger(true);

    private final UUID workerId = UUID.randomUUID();

    private long checkInInterval;
    private long workerLoopInterval;





    private Vector<RunnableTicket> backlog = new Vector<>();
    private Vector<RunnableTicket> running = new Vector<>();
    private ResourceManager resourceManager;
    private ComputeResources.ResourceKey resourceKey;

    public RemoteRunWorker(long workerLoopInterval, long checkInInterval) {
        this.checkInInterval = checkInInterval;
        this.workerLoopInterval = workerLoopInterval;

        resourceKey = ComputeResources.getSystemResourceKey();

        resourceManager = new ResourceManager(ComputeResources.mapUnitToComputeResource(resourceKey));
    }


    private void resourceCheckIn(){
        if (SystemDbFunctions.getWorkerResourceManagerById(this.workerId) == null){
            dbl.log("\n Worker generated \n");
            SystemDbFunctions.addWorker(resourceKey, workerId);
        }
        SystemDbFunctions.workerChekIn(workerId);

    }

    public void startWorker(){
        var checkInThread = new Thread(this::checkInLoop);
        var workerThread  = new Thread(this::workerLoop);

        checkInThread.start();
        workerThread.start();

    }
    private void checkInLoop(){
        while (true){
            try {
                Thread.sleep(checkInInterval * 1000);
                resourceCheckIn();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    private void workerLoop(){
        while (true){
            try {
                Thread.sleep(workerLoopInterval * 1000);
                this.removeCompletedTickets();
                this.fillQue();
                this.tryStartQueTickets();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void  removeCompletedTickets(){
        backlog.removeIf(Ticket::isDone);
        running.removeIf(Ticket::isDone);

    }


    private void fillQue() {
        UUID[] sortedQue = TicketDbFunctions.getWorkersActiveIds(this.workerId);
        Stream<UUID> systemTicketStream = Stream.of(backlog, running)
                .flatMap(tickets -> tickets.stream()
                .map(Ticket::getTicketId));

        for (UUID id: sortedQue){
            if (id != null){
                if (systemTicketStream.noneMatch(id::equals)){
                    this.addToBacklog(id);
                }
            } else {
                break;
            }
        }

    }
    private void tryStartQueTickets(){
        Vector<RunnableTicket> added = resourceManager.getStartQue(this.backlog);

        added.forEach(ticket -> ticket.run(resourceManager));

        this.backlog.removeAll(added);
        this.running.addAll(added);
    }

    private void addToBacklog(UUID ticketID){
        dbl.log("added ticket to backlog:", ticketID);

        RunnableTicket ticket = RunnableTicket.getTicketFromUUID(ticketID);
        if (ticket != null){
            if(ticket.getState() == TicketStatus.WAITING){
                ticket.build();
            }
            this.backlog.add(ticket);
        }
    }



}
