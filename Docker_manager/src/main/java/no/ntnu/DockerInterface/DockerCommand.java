package no.ntnu.DockerInterface;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public abstract class DockerCommand {
    private boolean isBlocking = false;
    public boolean dumpIO = false;
    private BiConsumer<Process,Throwable> onComplete;

    protected abstract ArrayList<String> buildCommand();

    public void setOnComplete(BiConsumer<Process,Throwable> onComplete) {
        this.onComplete = onComplete;
    }

    public void setBlocking(boolean blocking) {
        isBlocking = blocking;
    }

    public Process run(){
        ArrayList<String> commandParts = new ArrayList<>();

        commandParts.add("bash");
        commandParts.add("-c");

        commandParts.add(String.join(" ",this.buildCommand()));
        ProcessBuilder builder = new ProcessBuilder(commandParts);

        if (dumpIO){
            builder.inheritIO();
        }

        Process process = null;
        try {
            //TODO: maby have a place to save the erro logs from here
            process = builder.start();




            if (this.onComplete != null){
                process.onExit().whenComplete(onComplete);
                        //thenrun(this.onComplete);
            }

            if (this.isBlocking){
                process.waitFor();
            }


        } catch (Exception e){
            e.printStackTrace();
        }

        return process;
    }

}
/*
System.out.println("DEBUG PROCESS ---------aaa");;
            BufferedReader ok = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader er = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String l = null;
            while ((l = ok.readLine()) != null){
                System.out.println(l);
            }

            l = null;
            while ((l = er.readLine()) != null){
                System.out.println(l);
            }
            System.out.println("DEBUG PROCESS ---------");
 */