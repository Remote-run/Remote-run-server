package no.ntnu.master;


import no.trygvejw.debugLogger.DebugLogger;

public abstract class Watcher {

    private Long waitTime = 600L;// seconds
    private Thread loopThread;

    protected DebugLogger dbl = new DebugLogger(true);

    protected Watcher(Long waitTime) {
        this.waitTime = waitTime;
    }



    public void startLoopThread(){
        if (this.loopThread == null){
            dbl.log("NEW WATCHER GENERATED");
            this.loopThread = new Thread(this::watcherLoop);
            this.loopThread.start();
        }
    }

    private void watcherLoop(){
        dbl.log("LOOP IS STARTING");
        while (true){
            try {
                Thread.sleep( this.waitTime * 1000);
                act();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    /**
     * The actions the watcher needs to preform at every time step
     */
    public abstract void act();
}
