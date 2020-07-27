package no.ntnu.dockerComputeRecources.ResourceTypes;


import no.ntnu.dockerComputeRecources.ResourceType;

/**
 * A simpel way of reprecenting some compute recource in docker.
 * TODO: expand comment
 */
public interface ComputeResource {

    /**
     * Returns the Resource type of this resource.
     *
     * @return The Resource type of this resource.
     */
    ResourceType getResourceType();

    /**
     * Returns the ammount of this resource that is free.
     *
     * @return the ammount of this resource that is free.
     */
    int getAmountFreeResource();

    /**
     * Returns true if the provided ammount of resource is free
     *
     * @param amount the ammont to check if is free
     * @return true if the provided ammount is freee false if not;
     */
    boolean isAmountResourceFree(int amount);

    /**
     * Sets the given ammount of recource to the provided key.
     * If there is not requested resource size is larger than the what's free null is returned.
     *
     * @param amount the ammount of resoure to request
     * @param key    the id for the user of this resource
     * @return a string containing the docker command part that allocates the provided ammount of resource
     */
    String useResource(int amount, String key);

    /**
     * frees the resource binded to the provided key
     *
     * @param key the key of the resources to free
     */
    void freeResource(String key);

}
