package no.ntnu.DockerInterface;

import no.ntnu.util.DebugLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.function.BiConsumer;


public abstract class DockerCommand {

    private DebugLogger dbl = new DebugLogger(true);

    private boolean dumpIO = false;

    private File outputFile = null;
    private File errorFile  = null;

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setErrorFile(File errorFile) {
        this.errorFile = errorFile;
    }

    private BiConsumer<Process,Throwable> onComplete;

    private boolean isBlocking = false;

    public void setDumpIO(boolean dumpIO) {
        this.dumpIO = dumpIO;
    }

    public void setOnComplete(BiConsumer<Process,Throwable> onComplete) {
        this.onComplete = onComplete;
    }

    public void setBlocking(boolean blocking) {
        isBlocking = blocking;
    }

    private boolean keepOutput = false;
    private boolean keepError  = false;

    public void setKeepOutput(boolean keepOutput) {
        this.keepOutput = keepOutput;
    }

    public void setKeepError(boolean keepError) {
        this.keepError = keepError;
    }

    public Process run(){
        ArrayList<String> commandParts = new ArrayList<>();

        commandParts.add("/bin/bash");
        commandParts.add("-c");

        commandParts.add(String.join(" ",this.buildCommand()));
        dbl.log("running command: ", commandParts);
        ProcessBuilder builder = new ProcessBuilder(commandParts);

        // it seems that if the io is not drained the process can block or even dedlock
        if (this.outputFile != null){
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFile));
        } else if (!keepOutput){
            builder.redirectOutput(new File("/dev/null"));
        }


        if (this.errorFile != null){
            builder.redirectError(ProcessBuilder.Redirect.appendTo(errorFile));
        } else if(!keepError){
            builder.redirectError(new File("/dev/null"));
        }

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

    protected abstract ArrayList<String> buildCommand();



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