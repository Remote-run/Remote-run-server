package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerImageBuildCommand;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.config.JavaApiConfig;
import no.ntnu.config.PythonApiConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.UUID;

public class PythonTicket extends Ticket {

    private final File PythonDockerFile = new File(DockerManager.dckerfilesDir, "PYTHON/python_variable_image/Dockerfile");

    private DockerRunCommand runCommand;
    private PythonApiConfig ticketConfig;

    public PythonTicket(UUID ticketId, int gpu) {
        super(ticketId);
        runCommand = new DockerRunCommand(super.commonName, super.commonName, gpu);
        runCommand.setNetwork("ticketNetwork");
        runCommand.addVolume(DockerManager.translateSaveDataFileToHostFile(super.runDir).getAbsolutePath(),"/app/");
        runCommand.addVolume(DockerManager.translateSaveDataFileToHostFile(super.saveDir).getAbsolutePath(),"/save/");

        ticketConfig = new PythonApiConfig(new File(super.runDir, ApiConfig.commonConfigName));

        runCommand.addEnvVariable("runfile", new File("/app", ticketConfig.getFileToExecute().getAbsolutePath()).getAbsolutePath());
        runCommand.addEnvVariable("args", ticketConfig.getExecutionArgs());
    }


    @Override
    protected DockerRunCommand getStartCommand() {
        return runCommand;
    }

    @Override
    protected ApiConfig getTicketConfig() {
        return null;
    }

    @Override
    protected boolean buildRunTypeTicket() {
        boolean sucsess = true;
        try{
            DockerImageBuildCommand buildCommand = new DockerImageBuildCommand(ticketId, super.runDir, PythonDockerFile);
            buildCommand.setBuildArg("image",ticketConfig.getImage());
            buildCommand.setBlocking(true);
            buildCommand.setErrorFile(new File(super.logDir, "python_build_error"));
            buildCommand.setOutputFile(new File(super.logDir, "python_build_out"));
            Process process = buildCommand.run();
            sucsess = process.exitValue() == 0;
        } catch (Exception e){
            e.printStackTrace();
            sucsess= false;
        }
        return sucsess;

    }
}
