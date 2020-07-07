package no.ntnu.DockerInterface;

import java.lang.management.BufferPoolMXBean;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class DockerRunCommand {

    private HashMap<String,String> volumes = new HashMap<>();
    private int gpu = 0;
    private String image;
    private String containerName;
    private String network;

    private Runnable onComplete;

    public void setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public void addVolume(String from, String to){
        volumes.put(from, to);
    }

    public int getGpu() {
        return gpu;
    }

    public void setGpu(int gpu) {
        this.gpu = gpu;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }


    public DockerRunCommand(String image, String containerName, int gpu, String network) {
        this.image = image;
        this.containerName = containerName;
        this.gpu = gpu;
        this.network = network;
    }

    private ArrayList<String> buildCommand(){
        ArrayList<String> commandParts = new ArrayList<>();

        commandParts.add("docker image run");
        commandParts.add("--rm");
        //commandParts.add("-it");

        commandParts.add("--name " + this.containerName);

        // gpu
        switch (this.gpu){
            case -1:
                commandParts.add("--gpus all");
                break;
            case 0:
                break;
            default:
                commandParts.add("--gpus " + this.gpu);
        }

        // network
        if (network != null){
            commandParts.add("--net=" + this.network);
        }

        // volumes
        for (Map.Entry volume:this.volumes.entrySet()){
            commandParts.add(String.format("-v %s:%s", volume.getKey(),volume.getValue()));
        }

        commandParts.add(this.image);

        return commandParts;
    }

    public void run(){
        ProcessBuilder builder = new ProcessBuilder(this.buildCommand());
        Process process = null;
        try {
            //TODO: maby have a place to save the erro logs from here
            process = builder.start();

            process.onExit().thenRun(this.onComplete);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
