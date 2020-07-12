package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctons;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.config.JavaApiConfig;

import javax.imageio.IIOException;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class JavaTicket extends Ticket {

    private        final File javaDockerFile = new File(DockerManager.dckerfilesDir, "JAVA/maven_ticket_run/Dockerfile");
    private static final File m2RepoDir      = new File(DockerManager.buildHelpers, "java/m2_repo");

    private DockerRunCommand runCommand;
    private JavaApiConfig ticketConfig;

    public JavaTicket(UUID ticketId, int gpu) {
        super(ticketId);

        // TODO: Chek whether or not to have these on a network mtp segmentation
        runCommand = new DockerRunCommand(super.commonName, super.commonName, gpu, "host" );
        runCommand.addVolume(DockerManager.translateSaveDataFileToHostFile(super.runDir).getAbsolutePath(),"/app/");
        runCommand.addVolume(DockerManager.translateSaveDataFileToHostFile(super.saveDir).getAbsolutePath(),"/save/");

        ticketConfig = new JavaApiConfig();
    }

    /**
     * Runs the maven install container for this ticket, this installs all the maven deps to the shared volume.
     * the current thread will wait for the process to complete
     *
     * The method is synchronized to avoid multiple maven instances installing at the same time
     */
    private static synchronized void installMavenDeps(Ticket ticket){
        dbl.log("maven install requested");
        String containerName = "builder_" + ticket.commonName;
        DockerRunCommand installCmd = new DockerRunCommand(
                "maven_install_image:latest", containerName, 0, "host");

        installCmd.addVolume(DockerManager.translateSaveDataFileToHostFile(JavaTicket.m2RepoDir).getAbsolutePath(),"/root/.m2/repository");
        installCmd.addVolume(DockerManager.translateSaveDataFileToHostFile(ticket.runDir).getAbsolutePath(), "/app/");


        installCmd.setBlocking(true);
        installCmd.setErrorFile(new File(ticket.logDir, "maven_error_logs"));
        installCmd.setOutputFile(new File(ticket.logDir, "maven_norm_logs"));
        //installCmd.setDumpIO(true);
        Process process = installCmd.run();
        dbl.log("exit value java build", process.exitValue());

        //DockerFunctons.removeContainer(containerName);
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
    public boolean buildRunTypeTicket() {
        boolean success = true;
        try {
            JavaTicket.installMavenDeps(this);

            Process p = DockerFunctons.buildTicketImage(super.ticketId, DockerManager.buildHelpers, this.javaDockerFile);


            success = p.exitValue() == 0;
        } catch (Exception e){
            success = false;
            e.printStackTrace();
        }


        return success;
    }


}
