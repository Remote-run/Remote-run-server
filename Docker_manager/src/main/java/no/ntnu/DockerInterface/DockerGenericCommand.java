package no.ntnu.DockerInterface;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * simple wrapper to the DockerCommand class to alow for simple commands like image list etc..
 */
public class DockerGenericCommand extends DockerCommand {
    private final ArrayList<String> command = new ArrayList<>();

    /**
     * builds a docker generic command
     *
     * @param commandParts the commandpart to build the command from
     */
    public DockerGenericCommand(String... commandParts) {
        command.addAll(Arrays.asList(commandParts));
    }

    /**
     * Builds the docker command and returns a list of the command parts.
     *
     * @return A list of all the command parts for the command to run.
     */
    @Override
    protected ArrayList<String> buildCommand() {
        return command;
    }
}
