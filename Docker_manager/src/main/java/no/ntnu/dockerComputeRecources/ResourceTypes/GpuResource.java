package no.ntnu.dockerComputeRecources.ResourceTypes;

import no.ntnu.dockerComputeRecources.ResourceType;
import no.ntnu.util.DebugLogger;

import java.util.Vector;

public class GpuResource implements ComputeResource {

    private final ResourceType resourceType = ResourceType.GPU;
    private final String commandString = "--gpus '\"device=%s\"'";
    private final DebugLogger dbl = new DebugLogger(false);
    private final int numGpus;
    private final String[] gpuSlots;

    public GpuResource(int numGpus) {
        this.numGpus = numGpus;
        this.gpuSlots = new String[numGpus];
    }

    private Vector<Integer> getFreeGpuSlots() {
        Vector<Integer> freeSlots = new Vector<>();
        for (int i = 0; i < gpuSlots.length; i++) {
            if (gpuSlots[i] == null) {
                freeSlots.add(i);
            }
        }
        dbl.log("Free gpu slots: ", freeSlots);
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
        return this.getFreeGpuSlots().size();
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
            Vector<Integer> freeSlots = getFreeGpuSlots();
            String commandPart = "";
            for (int i = 0; i < amount; i++) {
                dbl.log(freeSlots.size(), freeSlots);
                dbl.log(gpuSlots.length, gpuSlots);
                gpuSlots[freeSlots.get(i)] = key;
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
        for (int i = 0; i < gpuSlots.length; i++) {
            if (gpuSlots[i] != null) {
                if (gpuSlots[i].equals(key)) {
                    gpuSlots[i] = null;
                }
            }
        }
    }
}
