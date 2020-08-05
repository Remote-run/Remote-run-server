package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctions;
import no.ntnu.DockerInterface.DockerImageBuildCommand;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.config.JavaApiConfig;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

public class JavaTicket extends RunnableTicket {

    private        final File javaDockerFile = new File(DockerManager.dckerfilesDir, "JAVA/maven_ticket_run/Dockerfile");
    private static final File m2RepoDir      = new File(DockerManager.buildHelpers, "java/m2_repo");

    private static final File mavenInstallDockerfile = new File(DockerManager.dckerfilesDir, "JAVA/maven_install_image/Dockerfile");
    private static final File cudaOpenjdkDockerfile = new File(DockerManager.dckerfilesDir, "JAVA/cuda_maven_openjdk/Dockerfile");

    private static boolean isCudaOpenjdkBuilt = false;
    private static boolean isMavenImageBuilt = false;


    private DockerRunCommand runCommand;
    private JavaApiConfig ticketConfig;




    public JavaTicket(UUID ticketId, Boolean safeBuild) {
        super(ticketId, safeBuild);

        // TODO: Chek whether or not to have these on a network mtp segmentation
        runCommand = new DockerRunCommand(super.commonName, super.commonName);
        runCommand.setNetwork("ticketNetwork");
        runCommand.addVolume(DockerManager.translateSaveDataFileToHostFile(super.runDir).getAbsolutePath(),"/app/");
        runCommand.addVolume(DockerManager.translateSaveDataFileToHostFile(super.saveDir).getAbsolutePath(),"/save/");

        ticketConfig = new JavaApiConfig(new File(super.runDir, ApiConfig.commonConfigName));
        dbl.log("java api config ", ticketConfig);
    }

    private static boolean isInstallPossible(){
        boolean possible = false;
        if (!(isMavenImageBuilt && isCudaOpenjdkBuilt)){
            String[] images = DockerFunctions.getImages();
            try {
                if (Arrays.stream(images).anyMatch(s -> s.equals("cuda_openjdk"))){
                    isCudaOpenjdkBuilt = true;
                } else {
                    DockerImageBuildCommand buildCommand = new DockerImageBuildCommand("cuda_openjdk", DockerManager.buildHole ,JavaTicket.cudaOpenjdkDockerfile);
                    buildCommand.setBlocking(true);
                    Process installProcess = buildCommand.run();

                    if (installProcess.exitValue() == 0){
                        isCudaOpenjdkBuilt = true;
                    }
                }

                if (Arrays.stream(images).anyMatch(s -> s.equals("cuda_openjdk"))){
                    isMavenImageBuilt = true;
                } else {
                    DockerImageBuildCommand buildCommand = new DockerImageBuildCommand("maven_install_image", DockerManager.buildHole ,JavaTicket.mavenInstallDockerfile);
                    buildCommand.setBlocking(true);
                    Process installProcess = buildCommand.run();

                    if (installProcess.exitValue() == 0){
                        isMavenImageBuilt = true;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if (isMavenImageBuilt && isCudaOpenjdkBuilt){
            possible = true;
        }

        return possible;


    }

    /**
     * Runs the maven install container for this ticket, this installs all the maven deps to the shared volume.
     * the current thread will wait for the process to complete
     *
     * The method is synchronized to avoid multiple maven instances installing at the same time
     */
    private static synchronized boolean installMavenDeps(Ticket ticket){
        boolean sucsess = true;
        if (JavaTicket.isInstallPossible()){
            dbl.log("maven install requested");
            String containerName = "builder_" + ticket.commonName;
            DockerRunCommand installCmd = new DockerRunCommand(
                    "maven_install_image", containerName);

            installCmd.setNetwork("ticketNetwork");
            installCmd.addVolume(DockerManager.translateSaveDataFileToHostFile(JavaTicket.m2RepoDir).getAbsolutePath(),"/root/.m2/repository");
            installCmd.addVolume(DockerManager.translateSaveDataFileToHostFile(ticket.runDir).getAbsolutePath(), "/app/");


            installCmd.setBlocking(true);
            installCmd.setErrorFile(new File(ticket.logDir, "maven_error_logs"));
            installCmd.setOutputFile(new File(ticket.logDir, "maven_norm_logs"));
            Process process = installCmd.run();
            DockerFunctions.removeContainer(containerName);


            if (process.exitValue() != 0){
                sucsess = false;
            }
        }else {
            sucsess = false;
        }


        return sucsess;
    }



    @Override
    protected DockerRunCommand getStartCommand() {
        return runCommand;
    }

    @Override
    public ApiConfig getRunTypeConfig() {
        return ticketConfig;
    }

    @Override
    public boolean buildRunTypeTicket() {
        boolean success;

        try {
            if (JavaTicket.installMavenDeps(this)){
                DockerImageBuildCommand buildCommand = new DockerImageBuildCommand(this.ticketId,DockerManager.buildHelpers, this.javaDockerFile);
                buildCommand.setBlocking(true);
                Process process = buildCommand.run();
                success = process.exitValue() == 0;

            } else {
                success = false;
            }


        } catch (Exception e){
            success = false;
            e.printStackTrace();
        }


        return success;
    }


}
