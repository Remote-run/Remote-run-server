package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctons;
import no.ntnu.DockerInterface.DockerRunCommand;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class JavaTicket extends Ticket {

    private DockerRunCommand runCommand;

    protected JavaTicket(UUID ticketId, int gpu) {
        super(ticketId);
        String ticketName = "ticket_" + ticketId;

        // TODO: Chek whether or not to have these on a network mtp segmentation
        runCommand = new DockerRunCommand(ticketName, ticketName, gpu, "host" );

    }


    public static boolean javaTicketBuild(UUID ticketId, File buildTargetDir) throws IOException {

        boolean suc = true;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    String.format("docker image build %s -t %s", buildTargetDir.getCanonicalPath(), "ticket_" + ticketId)
            );

            //TODO: maby have a place to save the erro logs from here
            Process process = builder.start();
            process.waitFor();
        } catch (Exception e){
            e.printStackTrace();
            suc = false;
        }
        return suc;

    }

    public static Process javaTicketStart(UUID ticketId){

        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    // TODO: maby remove -it this wold detatch the process. making peaking at the process objet not possible
                    //          for determening if the session is done
                    String.format("docker run --gpus 1 --rm -it " +
                            " -v run_data_volume/%s:/app " +
                            " -v save_data_volume/%s:/save " +
                            " -v mavenPackages:/root/.m2/repository " +
                            " java_gpu_test:latest", "ticket_" + ticketId, "ticket_" + ticketId)
            );

            //TODO: maby have a place to save the erro logs from here
            process = builder.start();

        } catch (Exception e){
            e.printStackTrace();
        }
        return process;
    }

    @Override
    protected DockerRunCommand getStartCommand() {
        return runCommand;
    }

    @Override
    protected void build() {

    }
}
