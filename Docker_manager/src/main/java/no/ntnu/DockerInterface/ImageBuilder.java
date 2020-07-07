package no.ntnu.DockerInterface;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ImageBuilder {

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


}
/*


#

docker run --gpus all -it --rm \
-v run_data_volume:/app
-v save_data_volume:/save
-v mavenPackages:/root/.m2/repository
java_gpu_test:latest


 */