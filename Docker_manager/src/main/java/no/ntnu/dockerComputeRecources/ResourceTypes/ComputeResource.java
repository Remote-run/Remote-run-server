package no.ntnu.dockerComputeRecources.ResourceTypes;


import java.util.UUID;

/**
 * A simpel way of reprecenting some compute recource in docker.
 * TODO: expand comment
 */
public interface ComputeResource {

    /**
     * Returns all keys currently reserving some resource
     *
     * @return all keys currently reserving some resource
     */
    UUID[] getKeys();


    /**
     * Returns the total amount of this compute resource, used or not.
     *
     * @return the total amount of this compute resource, used or not.
     */
    int getTotalAmountOfResource();

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
     * If there is not requested resource size is larger than the what's free false is returned.
     *
     * @param amount the ammount of resoure to request
     * @param key    the id for the user of this resource
     * @return true if the provided ammount of resource can be reserved false if not
     */
    boolean useResource(int amount, UUID key);

    /**
     * Retruns a string containing the docker command part that allocates the currently reserved ammount of resoure
     * @param key the key whos resourses to get the command for
     * @return a string containing the docker command part that allocates the currently reserved ammount of resoure
     */
    String getResourceCommand(UUID key);

    /**
     * frees the resource binded to the provided key
     *
     * @param key the key of the resources to free
     */
    void freeResource(UUID key);

}
