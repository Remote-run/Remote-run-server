package no.ntnu;

import no.ntnu.DockerInterface.DockerGenericCommand;
import no.ntnu.master.CompleteWatcher;
import no.ntnu.master.DeleteWatcher;
import no.ntnu.master.PowerOnChecks;
import no.ntnu.master.ResourceWatcher;
import no.ntnu.util.DebugLogger;
import no.ntnu.worker.RemoteRunWorker;

import java.io.File;

/**
 * TODO: Fix the absoulute fuking chaos that is exeption handeling here
 */


public class DockerManager {



    private static final DebugLogger dbl = new DebugLogger(true);

    /**
     * The interval for the check in of the compute nodes
     */
    private static final long checkInInterval = 60L; // 1 min

    /**
     * The additional time on top of the interval the workers have to check in before being deleted
     */
    private static final long checkInIntervalBuffer = 30L; // 30 sec leway


    //  intervals
    private static final long workerLoopInterval = 10L;
    private static final long completeWatcherInterval = 10L;
    private static final long deleteWatcherInterval = 10L;
    private static final long resourceWatcherInterval = 10L;





    public static File systemSaveDataDir = null;

    public static final File dckerfilesDir = new File("/runtypes");
    public static final File saveDataDir   = new File("/save_data");
    public static final File runDir        = new File(saveDataDir, "run");
    public static final File saveDir       = new File(saveDataDir, "save");
    public static final File logDir        = new File(saveDataDir, "logs");
    public static final File buildHelpers  = new File(saveDataDir, "build_helpers");
    public static final File sendDir       = new File(saveDataDir, "save");

    // a dir with no children to use as build context
    public static final File buildHole     = new File(buildHelpers, "build_hole");

    private static boolean isSlave = false;


    public static File translateSaveDataFileToHostFile(File file){
        dbl.log("in path", file);
        String fp = file.getAbsolutePath().replaceFirst(saveDataDir.getAbsolutePath(),"");
        File osPath = new File(systemSaveDataDir, fp);

        dbl.log("out path", osPath);
        return osPath;
    }


    public DockerManager(){
        // makes the common dirs if they do not exist
        runDir.mkdir();
        saveDir.mkdir();
        logDir.mkdir();
        buildHelpers.mkdir();
        sendDir.mkdir();
        buildHole.mkdir();
        try {
            systemSaveDataDir = new File(System.getenv("SAVE_DATA_SYS_PATH"));
        } catch (Exception e){}



        // builds the ticket network if it does not exist
        DockerGenericCommand command = new DockerGenericCommand("docker network create ticketNetwork");
        command.run();


        if (System.getenv("IS_SLAVE") != null){
            isSlave = System.getenv("IS_SLAVE").equals("TRUE");
        } else {
            isSlave = false;
        }
    }

    private void startLoops(){
        if (!isSlave){
            dbl.log("\n\n############################\n", "Running as master", "\n############################\n\n");
            new CompleteWatcher(completeWatcherInterval).startLoopThread();
            new ResourceWatcher(resourceWatcherInterval, checkInIntervalBuffer, checkInInterval).startLoopThread();
            new DeleteWatcher(deleteWatcherInterval).startLoopThread();
        }

        new RemoteRunWorker(workerLoopInterval, checkInInterval).startWorker();
    }






    public static void main( String[] args ) {
        while (!SystemDbFunctions.canConnectToDB()){
            System.out.println("Cant connect to db waiting...");
            try {
                Thread.sleep(5000);
            } catch (Exception e){}

        }

        PowerOnChecks.removeUnusedResourceKeys();
        PowerOnChecks.removeUnusedFiles();
        PowerOnChecks.removeUnusedResourceKeys();
        DockerManager manager = new DockerManager();
        manager.startLoops();
        /*ComputeResources.ResourceKey resources = null;
        try {
            InputStream inputStream = new FileInputStream("/home/trygve/Development/projects/Remote-run-server/Docker_manager/system_resources.yaml");
            Yaml yaml = new Yaml();
            ComputeResources.ResourceKey key = yaml.loadAs(inputStream, ComputeResources.ResourceKey.class);
            resources  = key;
        } catch (Exception e){
            e.printStackTrace();
        }*/

    }
}
