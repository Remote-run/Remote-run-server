package no.ntnu.DockerInterface;

import no.ntnu.ticket.Ticket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Gives a easy way of building images for a spesific file and context
 */
public class DockerImageBuildCommand extends DockerCommand {

    private String imageName;
    private File buildDir;
    private File dockerFile;

    /**
     * Creates a docker build command, with image name bult form a ticket id
     * @param ticketId The id to buld the image name from
     * @param buildDir The context used in the build
     * @param dockerFile The docker file to build from
     * @throws FileNotFoundException If the docker file or the build context is not found.
     * @throws FileSystemException If the docker file is a dir or the build context is a file.
     */
    public DockerImageBuildCommand(UUID ticketId, File buildDir, File dockerFile) throws FileNotFoundException, FileSystemException {
        this.imageName = Ticket.commonPrefix + ticketId;
        this.buildDir = buildDir;
        this.dockerFile = dockerFile;

        this.validateBuildDir();
        this.validateDockerfile();
    }

    /**
     * Creates a docker build command
     * @param imageName The image name
     * @param buildDir The context used in the build
     * @param dockerFile The docker file to build from
     * @throws FileNotFoundException If the docker file or the build context is not found.
     * @throws FileSystemException If the docker file is a dir or the build context is a file.
     */
    public DockerImageBuildCommand(String imageName, File buildDir, File dockerFile) throws FileNotFoundException, FileSystemException {
        this.imageName = imageName;
        this.buildDir = buildDir;
        this.dockerFile = dockerFile;

        this.validateBuildDir();
        this.validateDockerfile();
    }

    /**
     * Builds the docker command and returns a list of the command parts.
     * @return A list of all the command parts for the command to run.
     */
    @Override
    protected ArrayList<String> buildCommand() {
        ArrayList<String> commandParts = new ArrayList<>();

        // dockerfile
        commandParts.add("docker image build ");
        commandParts.add("-f " + dockerFile.getAbsolutePath());

        // tag (name)
        commandParts.add("-t " + this.imageName);

        // build root
        commandParts.add(buildDir.getAbsolutePath());

        System.out.println(commandParts);

        return commandParts;
    }

    /**
     * Validates the build dir is ok
     * @throws FileNotFoundException if the build dir is not found.
     * @throws FileSystemException If the build dir is a file.
     */
    private void validateBuildDir() throws FileNotFoundException, FileSystemException {
        if (!buildDir.exists()){
            throw new FileNotFoundException("Docker build dir not found");
        } else if (!buildDir.isDirectory()){
            throw new FileSystemException("Docker build dir is not a dir");
        }
    }

    /**
     * Validates the docker file is ok
     * @throws FileNotFoundException if the docker file is not found.
     * @throws FileSystemException If the docker file is a dir.
     */
    private void validateDockerfile() throws FileNotFoundException, FileSystemException {
        if (!dockerFile.exists()){
            throw new FileNotFoundException("Docker file not found");
        } else if (!dockerFile.isFile()){
            throw new FileSystemException("Docker file is not a file");
        }
    }
}
