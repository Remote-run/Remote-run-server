package no.ntnu.DockerInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

// todo: documentation here is shit

/**
 * The default run command for a container. Can be configured
 */
public class DockerRunCommand extends DockerCommand {

    private final HashMap<String, String> volumes = new HashMap<>();
    private final HashMap<String, String> envVariables = new HashMap<>();
    private String image;
    private final String containerName;
    private String network;
    private Vector<String> resourceAllocationParts;

    /**
     * Creates a new runCommand
     *
     * @param image         the image to build from
     * @param containerName the name to give the container while running
     */
    public DockerRunCommand(String image, String containerName) {
        this.image = image;
        this.containerName = containerName;
        this.network = "host";
    }

    /**
     * Adds a volume to the run command.
     *
     * @param from the absolute path on the host machine or the volume to bind from
     * @param to   the absolute path on the container to mount the from dir/volume
     */
    public void addVolume(String from, String to) {
        volumes.put(from, to);
    }

    /**
     * Adds an environment variable to the run command
     *
     * @param varName  the name of the env variable to set
     * @param varValue the value the the env variable to set
     */
    public void addEnvVariable(String varName, String varValue) {
        envVariables.put(varName, varValue);
    }

    /**
     * Sets the network used by the container.
     *
     * @param network The network used by the container.
     */
    public void setNetwork(String network) {
        this.network = network;
    }


    /**
     * Sets the resource allocation parts of the command
     *
     * @param commandParts the resource allocation parts of the command to set.
     */
    public void setResourceAllocationParts(Vector<String> commandParts) {
        this.resourceAllocationParts = commandParts;
    }


    /**
     * Sets the image to build from.
     *
     * @param image The image to build from.
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Builds the docker command and returns a list of the command parts.
     *
     * @return A list of all the command parts for the command to run.
     */
    protected ArrayList<String> buildCommand() {
        ArrayList<String> commandParts = new ArrayList<>();

        commandParts.add("docker");
        commandParts.add("container run");
        commandParts.add("--rm");
        //commandParts.add("-it");

        commandParts.add("--name " + this.containerName);

        // compute resources
        if (this.resourceAllocationParts != null) {
            commandParts.addAll(this.resourceAllocationParts);
        }


        // network
        if (network != null) {
            commandParts.add("--net=" + this.network);
        }

        // volumes
        for (Map.Entry volume : this.volumes.entrySet()) {
            commandParts.add(String.format("-v %s:%s", volume.getKey(), volume.getValue()));
        }

        // env vars
        for (Map.Entry var : this.envVariables.entrySet()) {
            commandParts.add(String.format("-e %s=\"%s\"", var.getKey(), var.getValue()));
        }

        commandParts.add(this.image);

        return commandParts;
    }


}
