package no.ntnu.dockerComputeRecources;

import no.ntnu.ticket.Ticket;
import no.ntnu.util.DebugLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ResourceManager {
    private DebugLogger dbl = new DebugLogger(true);

    private HashMap<ResourceType, ComputeResource> resourceMap = new HashMap<>();



    public ResourceManager(ComputeResource[] resources){
        for (ComputeResource resource: resources){
            resourceMap.put(resource.getResourceType(),resource);
        }
    }

    /**
     * Iterates throgh the que and starts as many tickets as there are free resources to do
     * @param que the que of tickets to run
     * @return A vector containing the tickets from the que that where allocated resources for
     */
    public Vector<Ticket> tryStartQue(Vector<Ticket> que){
        dbl.log("try add que", que);
        Vector<Ticket> allocated = new Vector<>();
        for (Ticket ticket: que){
            dbl.log("ticket ", ticket);
            if (this.areTicketResourcesFree(ticket)){
                dbl.log("is added ", ticket);
                Vector<String> commandParts = this.allocateTicketResources(ticket);
                allocated.add(ticket);
                if (commandParts != null){
                    ticket.setResourceAllocationCommand(commandParts);
                }
            }
        }
        return allocated;
    }

    public void freeTicketResources(Ticket ticket){
        for (ComputeResource resource: this.resourceMap.values()) {
            resource.freeResource(ticket.getCommonName());
        }
    }

    private boolean areTicketResourcesFree(Ticket ticket){
        boolean isFree = true;
        for (Map.Entry<ResourceType, Integer> entry: ticket.getRequiredResources().entrySet()){
            if (resourceMap.containsKey(entry.getKey())){
                if (!resourceMap.get(entry.getKey()).isAmountResourceFree(entry.getValue())){
                    isFree = false;
                    break;
                }
            }
        }
        return isFree;
    }

    private Vector<String> allocateTicketResources(Ticket ticket){
        Vector<String> commandParts = new Vector<>();
        if (this.areTicketResourcesFree(ticket)){
            for (Map.Entry<ResourceType, Integer> entry: ticket.getRequiredResources().entrySet()){
                if (resourceMap.containsKey(entry.getKey())){
                    commandParts.add(
                            resourceMap.get(entry.getKey()).useResource(entry.getValue(),ticket.getCommonName())
                    );
                }
            }
            return commandParts;
        } else {
            return null;
        }
    }



}
