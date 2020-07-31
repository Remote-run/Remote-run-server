package no.ntnu.dockerComputeRecources.ResourceTypes;

import no.ntnu.util.DebugLogger;

import java.util.HashMap;
import java.util.UUID;

public class RamResource implements ComputeResource {

    private final ResourceType resourceType = ResourceType.RAM;
    private final String commandString = "-m %sG --memory-swap 2G";
    private final DebugLogger dbl = new DebugLogger(false);
    private final int ramGbSize;

    private HashMap<UUID, Integer> usedRam = new HashMap<>();

    public RamResource(int ramGbSize) {
        this.ramGbSize = ramGbSize;
    }

    /**
     * Returns all keys currently reserving some resource
     *
     * @return all keys currently reserving some resource
     */
    public UUID[] getKeys(){
        return this.usedRam.keySet().stream()
                .distinct()
                .toArray(UUID[]::new);
    }

    /**
     * Returns the total amount of this compute resource, used or not.
     *
     * @return the total amount of this compute resource, used or not.
     */
    public int getTotalAmountOfResource(){
        return ramGbSize;
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
        return this.ramGbSize -  this.usedRam.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Returns true if the provided ammount of resource is free
     *
     * @param amount the ammont to check if is free
     * @return true if the provided ammount is freee false if not;
     */
    @Override
    public boolean isAmountResourceFree(int amount) {
        return amount <= this.getAmountFreeResource();
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
        if (isAmountResourceFree(amount)){
            this.usedRam.put(key, amount);
            return true;
        }else {
            return false;
        }
    }

    /**
     * Retruns a string containing the docker command part that allocates the currently reserved ammount of resoure
     * @param key the key whos resourses to get the command for
     * @return a string containing the docker command part that allocates the currently reserved ammount of resoure
     */
    public String getResourceCommand(UUID key){
        return String.format(this.commandString, this.usedRam.get(key));
    }

    /**
     * frees the resource binded to the provided key
     *
     * @param key the key of the resources to free
     */
    @Override
    public void freeResource(UUID key) {
        this.usedRam.remove(key);
    }
}
