package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctons;
import no.ntnu.DockerInterface.DockerImageBuildCommand;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.config.JavaApiConfig;
import no.ntnu.enums.TicketStatus;
import no.ntnu.sql.PsqlInterface;

import java.io.File;
import java.util.UUID;

public class JavaTicket extends Ticket {

    private        final File javaDockerFile = new File(DockerManager.dckerfilesDir, "JAVA/maven_ticket_run/Dockerfile");
    private static final File m2RepoDir      = new File("/save_data/m2/repository");

    private DockerRunCommand runCommand;
    private JavaApiConfig ticketConfig;

    public JavaTicket(UUID ticketId, int gpu) {
        super(ticketId);

        // TODO: Chek whether or not to have these on a network mtp segmentation
        runCommand = new DockerRunCommand(super.commonName, super.commonName, gpu, "host" );
        ticketConfig = new JavaApiConfig();


    }

    /**
     * Runs the maven install container for this ticket, this installs all the maven deps to the shared volume.
     * the current thread will wait for the process to complete
     *
     * The method is synchronized to avoid multiple maven instances installing at the same time
     */
    private static synchronized void installMavenDeps(String commonName, File runDir){
        String containerName = commonName + "builder";
        DockerRunCommand installCmd = new DockerRunCommand(
                "maven_install_image:latest", containerName, 0, "host");

        installCmd.addVolume(JavaTicket.m2RepoDir.getAbsolutePath(),"/root/.m2/repository");
        installCmd.addVolume(runDir.getAbsolutePath(), "/app/");

        // block the thread so the run image is not built before the install image is done
        installCmd.setBlocking(true);
        installCmd.run();

        DockerFunctons.removeContainer(containerName);
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
    public void buildRunTypeTicket() {
        try {
            JavaTicket.installMavenDeps(super.commonName, super.runDir);

            DockerFunctons.buildTicketImage(super.ticketId, super.runDir, this.javaDockerFile);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
