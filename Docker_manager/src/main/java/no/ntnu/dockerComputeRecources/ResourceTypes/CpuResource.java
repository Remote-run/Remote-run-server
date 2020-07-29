package no.ntnu.dockerComputeRecources.ResourceTypes;

import no.ntnu.dockerComputeRecources.ResourceType;
import no.ntnu.util.DebugLogger;

import java.util.Vector;

public class CpuResource implements ComputeResource {

    // TODO: this can be abstracted with GpuComputeResoure as a sequensial numbered resource
    // TODO: the put at first free aproutch used here may be a tad inefficent pga cashing

    private final ResourceType resourceType = ResourceType.CPU;
    private final String commandString =  "--cpuset-cpus=\"%s\"" ;
    private final DebugLogger dbl = new DebugLogger(false);
    private final int numCpus;
    private final String[] cpuSlots;

    public CpuResource(int numCpus) {
        this.numCpus = numCpus;
        this.cpuSlots = new String[numCpus];
    }

    private Vector<Integer> getFreeCpuSlots() {
        Vector<Integer> freeSlots = new Vector<>();
        for (int i = 0; i < cpuSlots.length; i++) {
            if (cpuSlots[i] == null) {
                freeSlots.add(i);
            }
        }
        dbl.log("Free Cpu slots: ", freeSlots);
        return freeSlots;
    }

    /**
     * Returns the Resource type of this resource.
     *
     * @return The Resource type of this resource.
     */
    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Returns the ammount of this resource that is free.
     *
     * @return the ammount of this resource that is free.
     */
    @Override
    public int getAmountFreeResource() {
        return this.getFreeCpuSlots().size();
    }

    /**
     * Returns true if the provided ammount of resource is free
     *
     * @param amount the ammont to check if is free
     * @return true if the provided ammount is freee false if not;
     */
    @Override
    public boolean isAmountResourceFree(int amount) {
        return getAmountFreeResource() >= amount;
    }

    /**
     * Sets the given ammount of recource to the provided key.
     * If there is not requested resource size is larger than the what's free null is returned.
     *
     * @param amount the ammount of resoure to request
     * @param key    the id for the user of this resource
     * @return a string containing the docker command part that allocates the provided ammount of resource
     */
    @Override
    public String useResource(int amount, String key) {
        dbl.log("requesting resource: ", amount, key);
        if (this.isAmountResourceFree(amount)) {
            dbl.log("is free providing");
            Vector<Integer> freeSlots = getFreeCpuSlots();
            String commandPart = "";
            for (int i = 0; i < amount; i++) {
                dbl.log(freeSlots.size(), freeSlots);
                dbl.log(cpuSlots.length, cpuSlots);
                cpuSlots[freeSlots.get(i)] = key;
                commandPart += "," + freeSlots.get(i);
            }

            commandPart = commandPart.replaceFirst(",", "");

            return String.format(this.commandString, commandPart);
        } else {
            return null;
        }
    }

    /**
     * frees the resource binded to the provided key
     *
     * @param key the key of the resources to free
     */
    @Override
    public void freeResource(String key) {
        for (int i = 0; i < cpuSlots.length; i++) {
            if (cpuSlots[i] != null) {
                if (cpuSlots[i].equals(key)) {
                    cpuSlots[i] = null;
                }
            }
        }
    }
}
