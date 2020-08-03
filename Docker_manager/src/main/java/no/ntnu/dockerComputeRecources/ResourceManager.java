package no.ntnu.dockerComputeRecources;

import no.ntnu.dockerComputeRecources.ResourceTypes.ComputeResource;
import no.ntnu.dockerComputeRecources.ResourceTypes.ResourceType;
import no.ntnu.ticket.RunnableTicket;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.DebugLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ResourceManager {
    protected final DebugLogger dbl = new DebugLogger(false);

    private final HashMap<ResourceType, ComputeResource> resourceMap = new HashMap<>();

    protected HashMap<ResourceType, ComputeResource> getResourceMap() {
        return resourceMap;
    }

    public ResourceManager(ComputeResource[] resources) {
        dbl.log("%%%%%%%%%%%%%%%%%\nnew resorse manager");
        for (ComputeResource resource : resources) {
            dbl.log("adding resource ", resource.getResourceType(), resource.getTotalAmountOfResource());

            resourceMap.put(resource.getResourceType(), resource);
        }
        dbl.log("%%%%%%%%%%%%%%%%%");
    }

    /////////////////////////
    //  local
    /////////////////////////

    /**
     * Iterates throgh the que and returns a list with the tickets that can be started
     * The resources for these tickets have been reseved with the current manager
     *
     * @param que the que of tickets to run
     * @return A vector containing the tickets from the que that where allocated resources for
     */
    public Vector<RunnableTicket> getStartQue(Vector<RunnableTicket> que) {
        dbl.log("try add que", que);
        Vector<RunnableTicket> allocated = new Vector<>();
        for (RunnableTicket ticket : que) {
            dbl.log("ticket ", ticket);
            if (this.areTicketResourcesFree(ticket)) {
                dbl.log("is added ", ticket);
                if (this.allocateTicketResources(ticket)){
                    allocated.add(ticket);
                }
            }
        }
        return allocated;
    }

    /**
     * Mark the resources currently being reserved by the provided ticket as free
     *
     * @param ticket the ticket whom's resources to free
     */
    public void freeTicketResources(Ticket ticket) {
        for (ComputeResource resource : this.resourceMap.values()) {
            resource.freeResource(ticket.getTicketId());
        }
    }

    /**
     * Checks if the provided tickets required resources are greater than the currently free resources
     *
     * @param ticket the ticket to check
     * @return true if the system has enough resources free to run the ticket
     */
    public boolean areTicketResourcesFree(Ticket ticket) {
        boolean isFree = true;
        dbl.log("------ resourse for ticket ", ticket.getTicketId());
        for (Map.Entry<ResourceType, Integer> entry : ComputeResources.mapUnitToTypeMap(ticket.getResourceKey()).entrySet()) {
            dbl.log(entry.getKey().name(), entry.getValue());
            if (resourceMap.containsKey(entry.getKey())) {
                if (!resourceMap.get(entry.getKey()).isAmountResourceFree(entry.getValue()) && entry.getValue() != -1) {
                    dbl.log("TYPE ", entry.getKey().name(), "is not free for ticket", ticket.getTicketId());
                    isFree = false;
                    break;
                }
            }
        }

        return isFree;
    }



    /**
     * Reserve the amount of resource the provided ticket needs
     *
     * @param ticket the ticket to reserve the resources to
     * @return true if sucsessfull false if not
     */
    protected boolean allocateTicketResources(Ticket ticket) {
        boolean susses  = true;
        if (this.areTicketResourcesFree(ticket)) {
            for (Map.Entry<ResourceType, Integer> entry : ComputeResources.mapUnitToTypeMap(ticket.getResourceKey()).entrySet()) {
                if (resourceMap.containsKey(entry.getKey())) {
                    susses = susses && resourceMap.get(entry.getKey()).useResource(entry.getValue(), ticket.getTicketId());

                }
            }
            return susses;
        } else {
            return susses;
        }
    }

    /**
     * Returns the run string to give docker to use these resources
     *
     * @param ticket the ticket the resoures are reseved to
     * @return The run string to give to the DockerRunCommand to use the resources. empty string is returned if no resorses are reserved
     */
    public Vector<String> getTicketAllocationCommand(Ticket ticket) {
        Vector<String> commandParts = new Vector<>();
        dbl.log("Getting command part for: ", ticket.getTicketId());
        dbl.log("ere resources free: ", this.areTicketResourcesFree(ticket));
        for (Map.Entry<ResourceType, Integer> entry : ComputeResources.mapUnitToTypeMap(ticket.getResourceKey()).entrySet()) {
            if (resourceMap.containsKey(entry.getKey())) {
                commandParts.add(
                        resourceMap.get(entry.getKey()).getResourceCommand(ticket.getTicketId())
                );
            }
        }
        return commandParts;
    }



    /////////////////////////
    //  manager
    /////////////////////////


    






}
