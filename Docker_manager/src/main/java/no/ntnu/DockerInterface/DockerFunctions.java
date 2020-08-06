package no.ntnu.DockerInterface;

import no.ntnu.ticket.Ticket;
import no.trygvejw.debugLogger.DebugLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;


/**
 * contains different practical docker functions manly
 * used to start, stop, and get info about tickets being run
 */
public class DockerFunctions {

    private static final DebugLogger dbl = new DebugLogger(true);

    /**
     * Gets a list of the ticket id's that has a image in the local docker repo,
     * that follow the standard ticket naming conventions, that is:
     * Ticket.commonPrefix + ticketId
     *
     * @return A array containing the uuid's for the tickets with images
     */
    public static UUID[] getTicketImages() {
        UUID[] ret = Arrays.stream(DockerFunctions.getImages())
                .filter(image -> image.startsWith(Ticket.commonPrefix))
                .map(commonName -> commonName.replaceFirst(Ticket.commonPrefix, ""))
                .map(UUID::fromString).toArray(UUID[]::new);

        return ret;
    }

    /**
     * Return a list of all images currently in the local repo.
     *
     * @return A list of all images currently in the local repo.
     */
    public static String[] getImages() {
        ArrayList<String> idList = new ArrayList<>();

        DockerGenericCommand command = new DockerGenericCommand(
                "docker image ls --format \"{{.Repository}}\""
        );

        command.setBlocking(true);
        command.setKeepOutput(true);
        Process process = command.run();


        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                idList.add(line);
            }
            bufferedReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return idList.toArray(String[]::new);
    }

    /**
     * Returns a map with the name status pair of all the containers for the given system
     *
     * @return A map with the name status pair of all the containers for the given system
     */
    public static HashMap<String, String> getContainerStatuses() {
        HashMap<String, String> statusMap = new HashMap<>();
        DockerGenericCommand command = new DockerGenericCommand(
                "docker container ls -a --format \"{{.Names}}:{{.Status}}\""
        );

        command.setBlocking(true);
        command.setKeepOutput(true);
        Process process = command.run();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                String[] nameStatusPair = line.split(":");
                statusMap.put(nameStatusPair[0], nameStatusPair[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusMap;
    }

    /**
     * Deletes any container or image to the provided ticket id using the normal naming conentions.
     * same as calling
     * DockerFunctons.removeContainer(Ticket.commonPrefix + ticketId);
     * DockerFunctons.removeImage(Ticket.commonPrefix + ticketId);
     *
     * @param ticketId the id of the ticket to clean
     */
    public static void cleanTicket(UUID ticketId) {
        DockerFunctions.removeContainer(Ticket.commonPrefix + ticketId);
        DockerFunctions.removeImage(Ticket.commonPrefix + ticketId);
    }

    /**
     * Deletes the container with the provided name
     *
     * @param containerName the name of the container to delete
     */
    public static void removeContainer(String containerName) {
        DockerGenericCommand command = new DockerGenericCommand(
                String.format("docker container rm %s", containerName));
    }

    /**
     * Deletes the image with the provided name
     *
     * @param imageName the name of the image to delete
     */
    public static void removeImage(String imageName) {
        DockerGenericCommand command = new DockerGenericCommand(
                String.format("docker image rm %s", imageName));
        command.run();
    }
}
