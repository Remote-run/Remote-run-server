package no.ntnu.dockerComputeRecources.ResourceTypes;

import no.ntnu.dockerComputeRecources.ResourceType;
import no.ntnu.util.DebugLogger;

import java.lang.invoke.LambdaMetafactory;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;

public class RamResource implements ComputeResource {

    private final ResourceType resourceType = ResourceType.RAM;
    private final String commandString = "-m %sG --memory-swap 2G";
    private final DebugLogger dbl = new DebugLogger(false);
    private final int ramGbSize;

    private HashMap<String, Integer> usedRam = new HashMap<>();

    public RamResource(int ramGbSize) {
        this.ramGbSize = ramGbSize;
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
     * If there is not requested resource size is larger than the what's free null is returned.
     *
     * @param amount the ammount of resoure to request
     * @param key    the id for the user of this resource
     * @return a string containing the docker command part that allocates the provided ammount of resource
     */
    @Override
    public String useResource(int amount, String key) {
        if (isAmountResourceFree(amount)){
            this.usedRam.put(key, amount);
            return String.format(this.commandString, amount);
        }else {
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
        this.usedRam.remove(key);
    }
}
