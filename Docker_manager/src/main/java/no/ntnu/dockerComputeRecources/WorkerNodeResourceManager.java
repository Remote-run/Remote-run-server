package no.ntnu.dockerComputeRecources;

import no.ntnu.dockerComputeRecources.ResourceTypes.ComputeResource;
import no.ntnu.dockerComputeRecources.ResourceTypes.ResourceType;
import no.ntnu.sql.SystemDbFunctions;
import no.ntnu.ticket.Ticket;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

public class WorkerNodeResourceManager extends ResourceManager {
    private final UUID workerID;

    public UUID getWorkerID() {
        return workerID;
    }

    public WorkerNodeResourceManager(ComputeResource[] resources, UUID workerID) {
        super(resources);
        this.workerID = workerID;
    }




    public void updateResourceUsage(){
        Vector<UUID> dbActivIds = Arrays
                .stream(SystemDbFunctions.getActiveWorkerTickets(this.workerID))
                .collect(Collectors.toCollection(Vector::new));

        Vector<UUID> localActiveIds = this.getResourceMap().values()
                .stream()
                .flatMap(resource -> Arrays.stream(resource.getKeys()))
                .distinct()
                .collect(Collectors.toCollection(Vector::new));

        // every ticket that is active localy but not in the db shod have its resources freed
        localActiveIds.stream()
                .filter(uuid -> !dbActivIds.contains(uuid))
                .forEach(uuid -> {
                    this.getResourceMap().values().forEach(resource -> resource.freeResource(uuid));
                });

        // every ticket that is active in the db but not localy has to be updated localy
        dbActivIds.stream()
                .filter(uuid -> !localActiveIds.contains(uuid))
                .map(Ticket::new)  // this is not ideal
                .forEach(this::allocateTicketResources);




    }


    public boolean isCapableOfTicket(Ticket ticket){
        boolean capable = true;

        for (Map.Entry<ResourceType, Integer> entry : ComputeResources.mapUnitToTypeMap(ticket.getResourceKey()).entrySet()) {
            if (this.getResourceMap().containsKey(entry.getKey())) {
                if (!(capable = capable && this.getResourceMap().get(entry.getKey()).getTotalAmountOfResource() >= entry.getValue())) {
                    dbl.log("NOT CAPABLE OF R:", entry.getKey().name());
                    break;
                }
            }
        }
        return capable;
    }



    public void stageTicketForWorker(Ticket ticket){
        SystemDbFunctions.activateTicket(ticket.getTicketId(), workerID);

    }

    public void stageAndAllocateTicketForWorker(Ticket ticket){
        if (super.allocateTicketResources(ticket)){
            stageTicketForWorker(ticket);
        } else {
            dbl.log("\n\n Super not good, trying to stage under capasity");
        }
    }
}
