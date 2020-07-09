package no.ntnu.DockerInterface;

import java.util.ArrayList;
import java.util.Arrays;

public class DockerGenericCommand extends DockerCommand {
    private ArrayList<String> command = new ArrayList<>();

    public DockerGenericCommand(String ...commandParts){
        command.addAll(Arrays.asList(commandParts));
    }

    @Override
    protected ArrayList<String> buildCommand() {
        return command;
    }
}
