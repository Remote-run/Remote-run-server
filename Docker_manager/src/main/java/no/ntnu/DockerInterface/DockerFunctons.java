package no.ntnu.DockerInterface;

import no.ntnu.enums.TicketStatus;
import no.ntnu.sql.PsqlInterface;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.DebugLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;


/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * problemet er at stuff blir bygd og trenger stuff utenfor conexten dette kan potensielt fikses med litt trickery men det er hva issuen er per nå
 *
 *
 * /sidenote veldig mye mindre stablit og debug vennelig en fåretrokket
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */




public class DockerFunctons {

    private static DebugLogger dbl = new DebugLogger(true);


    public static UUID[] getImages(){
        //docker image ls  --format "{{.Repository}}"
        ArrayList<UUID> idList = new ArrayList<>();

        DockerGenericCommand command = new DockerGenericCommand(
                "docker image ls --format \"{{.Repository}}\""
        );

        command.setBlocking(true);
        command.setKeepOutput(true);
        Process process = command.run();


        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));


            String line;
            while ((line = bufferedReader.readLine()) != null){
                if (line.startsWith(Ticket.commonPrefix)){
                    idList.add(UUID.fromString(line.replaceFirst(Ticket.commonPrefix,"")));
                }
            }
            bufferedReader.close();

        } catch (Exception e){
            e.printStackTrace();
        }

        return idList.toArray(UUID[]::new);

    }

    /**
     * Returns a map with the name status pair of all the containers for the given system
     * @return A map with the name status pair of all the containers for the given system
     */
    public static HashMap<String,String> getContainerStatuses(){
        HashMap<String,String> statusMap = new HashMap<>();;
        DockerGenericCommand command = new DockerGenericCommand(
                "docker container ls -a --format \"{{.Names}}:{{.Status}}\""
        );

        command.setBlocking(true);
        command.setKeepOutput(true);
        Process process = command.run();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;
            while ((line = bufferedReader.readLine()) != null){
                String[] nameStatusPair = line.split(":");
                statusMap.put(nameStatusPair[0], nameStatusPair[1]);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return statusMap;
    }

    /**
     * Deletes any container or image to the provided ticket id using the normal naming conentions.
     * same as calling
     * DockerFunctons.removeContainer(Ticket.commonPrefix + ticketId);
     * DockerFunctons.removeImage(Ticket.commonPrefix + ticketId);
     * @param ticketId the id of the ticket to clean
     */
    public static void cleanTicket(UUID ticketId){
        DockerFunctons.removeContainer(Ticket.commonPrefix + ticketId);
        DockerFunctons.removeImage(Ticket.commonPrefix + ticketId);
    }

    /**
     * Deletes the container with the provided name
     * @param containerName the name of the container to delete
     */
    public static void removeContainer(String containerName){
        DockerGenericCommand command = new DockerGenericCommand(
                String.format("docker container rm %s" , containerName));
    }

    /**
     * Deletes the image with the provided name
     * @param imageName the name of the image to delete
     */
    public static void removeImage(String imageName){
        DockerGenericCommand command = new DockerGenericCommand(
                String.format("docker image rm %s" , imageName));
        command.run();
    }


    public static Process buildTicketImage(UUID ticketId, File buildDir, File dockerFile) throws IOException {
        dbl.log("building", ticketId);
        //dbl.fileLog(buildDir);
        //dbl.fileLog(dockerFile);
        DockerImageBuildCommand command = new DockerImageBuildCommand(ticketId, buildDir, dockerFile);
        command.setOnComplete((process, throwable) -> {
            if (throwable == null && process != null){
                dbl.log("check that is zero at normal term ->", process.exitValue());
                if (process.exitValue() == 0){
                    try {
                        // todo: this may have issues with beeing caugth, given the asyncness
                        PsqlInterface.updateTicketStatus(ticketId, TicketStatus.READY);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                } else {
                    System.out.println("ERROR BUILDING IMAGA CODE:" + process.exitValue());
                }
            }
        });

        command.setBlocking(true);
        return command.run();

    }
}
