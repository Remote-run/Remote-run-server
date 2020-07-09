package no.ntnu.DockerInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.UUID;

public class DockerImageBuildCommand extends DockerCommand {

    private UUID ticketId;
    private File buildDir;
    private File dockerFile;

    public DockerImageBuildCommand(UUID ticketId, File buildDir, File dockerFile) throws FileNotFoundException, FileSystemException {
        this.ticketId = ticketId;
        this.buildDir = buildDir;
        this.dockerFile = dockerFile;

        this.validateBuildDir();
        this.validateDockerfile();
    }


    @Override
    protected ArrayList<String> buildCommand() {
        ArrayList<String> commandParts = new ArrayList<>();

        // dockerfile
        commandParts.add("docker image build ");
        commandParts.add("-f " + dockerFile.getAbsolutePath());

        // tag (name)
        commandParts.add("-t " + "ticket_" + ticketId);

        // build root
        commandParts.add(buildDir.getAbsolutePath());

        System.out.println(commandParts);

        return commandParts;
    }

    private void validateBuildDir() throws FileNotFoundException, FileSystemException {
        if (!buildDir.exists()){
            throw new FileNotFoundException("Docker build dir not found");
        } else if (!buildDir.isDirectory()){
            throw new FileSystemException("Docker build dir is not a dir");
        }
    }

    private void validateDockerfile() throws FileNotFoundException, FileSystemException {
        if (!dockerFile.exists()){
            throw new FileNotFoundException("Docker file not found");
        } else if (!dockerFile.isFile()){
            throw new FileSystemException("Docker file is not a file");
        }
    }
}
