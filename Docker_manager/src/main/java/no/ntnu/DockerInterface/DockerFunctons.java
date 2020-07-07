package no.ntnu.DockerInterface;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DockerFunctons {

    /**
     * Returns a map with the name status pair of all the containers for the given system
     * @return A map with the name status pair of all the containers for the given system
     */
    private HashMap<String,String> getContainerStatuses(){
        HashMap<String,String> statusMap = new HashMap<>();
        Process process = null;

        try {
            ProcessBuilder builder = new ProcessBuilder("docker container ls -a --format \"{{.Names}}:{{.Status}}\"");

            //TODO: maby have a place to save the erro logs from here
            process = builder.start();
            process.waitFor();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = bufferedReader.readLine();

            while ((line = bufferedReader.readLine()) != null){
                String[] nameStatusPair = line.split(":");
                statusMap.put(nameStatusPair[0], nameStatusPair[1]);
            }

            return statusMap;

        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes any container or image to the provided ticket id
     * @param ticketId the id of the ticket to clean
     */
    public static void cleanTicket(UUID ticketId){
        try {
            ProcessBuilder cleanImageBuilder = new ProcessBuilder(
                    String.format("docker image rm %s" , "ticket_" + ticketId)
            );

            ProcessBuilder cleanContainerBuilder = new ProcessBuilder(
                    String.format("docker container rm %s" , "ticket_" + ticketId)
            );

            //TODO: maby have a place to save the erro logs from here
            Process cleanImageProcess = cleanImageBuilder.start();
            Process cleanContainerProcess = cleanContainerBuilder.start();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void buildImage(UUID ticketId, File buildDir, File dockerFile){

    }
}
