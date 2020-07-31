package no.ntnu.dockerComputeRecources.ResourceTypes;

import no.ntnu.util.DebugLogger;

import java.util.Arrays;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

public class GpuResource implements ComputeResource {

    private final ResourceType resourceType = ResourceType.GPU;
    private final String commandString = "--gpus '\"device=%s\"'";
    private final DebugLogger dbl = new DebugLogger(false);
    private final int numGpus;
    private final UUID[] gpuSlots;

    public GpuResource(int numGpus) {
        this.numGpus = numGpus;
        this.gpuSlots = new UUID[numGpus];
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
     * Returns all keys currently reserving some resource
     *
     * @return all keys currently reserving some resource
     */
    public UUID[] getKeys(){
        return Arrays.stream(this.gpuSlots)
                .distinct()
                .toArray(UUID[]::new);
    }

    /**
     * Returns the total amount of this compute resource, used or not.
     *
     * @return the total amount of this compute resource, used or not.
     */
    public int getTotalAmountOfResource(){
        return numGpus;
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
     * If there is not requested resource size is larger than the what's free false is returned.
     *
     * @param amount the ammount of resoure to request
     * @param key    the id for the user of this resource
     * @return true if the provided ammount of resource can be reserved false if not
     */
    @Override
    public boolean useResource(int amount, UUID key) {
        dbl.log("requesting resource: ", amount, key);
        if (this.isAmountResourceFree(amount)) {
            dbl.log("is free providing");
            Vector<Integer> freeSlots = getFreeGpuSlots();
            for (int i = 0; i < amount; i++) {
                dbl.log(freeSlots.size(), freeSlots);
                dbl.log(gpuSlots.length, gpuSlots);
                gpuSlots[freeSlots.get(i)] = key;
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Retruns a string containing the docker command part that allocates the currently reserved ammount of resoure
     * @param key the key whos resourses to get the command for
     * @return a string containing the docker command part that allocates the currently reserved ammount of resoure
     */
    public String getResourceCommand(UUID key){
        Vector<Integer> keyPos = new Vector<>();
        for (int i = 0; i < this.gpuSlots.length; i++) {
            if (gpuSlots[i] != null){
                if (gpuSlots[i].equals(key)){
                    keyPos.add(i);
                }

            }
        }

        return String.format(this.commandString, keyPos.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
    }

    /**
     * frees the resource binded to the provided key
     *
     * @param key the key of the resources to free
     */
    @Override
    public void freeResource(UUID key) {
        for (int i = 0; i < gpuSlots.length; i++) {
            if (gpuSlots[i] != null) {
                if (gpuSlots[i].equals(key)) {
                    gpuSlots[i] = null;
                }
            }
        }
    }
}
