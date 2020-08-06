package no.ntnu.ticket;

import no.ntnu.DockerInterface.DockerFunctions;
import no.ntnu.DockerInterface.DockerRunCommand;
import no.ntnu.DockerManager;
import no.ntnu.config.ApiConfig;
import no.ntnu.config.ConfigError;
import no.ntnu.config.configBuilder.ConfigParam;
import no.ntnu.dockerComputeRecources.ComputeResources;
import no.ntnu.dockerComputeRecources.ResourceManager;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.exeptions.TicketErrorException;
import no.ntnu.sql.TicketDbFunctions;

import no.trygvejw.debugLogger.DebugLogger;
import no.trygvejw.util.Compression;
import org.json.JSONObject;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

/**
 * The representation of a run request's data an general functions this is manly used to organize where the ticket
 * should be run. To actually run the ticket the RunnableTicket class should be used
 */
public class Ticket {

    protected static DebugLogger dbl = new DebugLogger(false);

    /**
     * The prefix to be used in common naming of ticket related things like images and dirs
     * to allow for easy distinction of what is system stuff and ticket stuff
     */
    public static final String commonPrefix = "ticket_";

    /**
     * Simple way of ensuring the container wont have 2 build opperations
     * or a build and an run opperation at the same time.
     *
     * The ticket only have one thread slot to do stuff
     */
    public Thread buildThread = null;

    protected UUID ticketId;
    protected String commonName;

    protected File runDir;
    protected File saveDir;
    protected File logDir;
    protected File outDir;
    protected File outFile;
    protected File apiConfigFile;


    protected TicketStatus state = TicketStatus.WAITING;
    protected ResourceManager usedResourceManager;


    private ComputeResources.ResourceKey resourceKey = ComputeResources.defaultKey;


    /**
     * Creates a ticket
     * @param ticketId the id to give the ticket
     */
    public Ticket(UUID ticketId) {
        dbl.log("NEW TICKET ", ticketId);
        this.ticketId = ticketId;
        this.commonName = Ticket.commonPrefix + ticketId;
        runDir = new File(DockerManager.runDir, commonName);
        saveDir = new File(DockerManager.saveDir, commonName);
        outFile = new File(DockerManager.sendDir,commonName + ".zip");
        logDir = new File(DockerManager.logDir, commonName);
        apiConfigFile = new File(runDir, ApiConfig.commonConfigName);
        saveDir.mkdir();
        logDir.mkdir();

        try{
            this.state = TicketDbFunctions.getTicketStatus(ticketId);
        }catch (Exception e){
            voidTicket(TicketExitReason.buildError);
            e.printStackTrace();
        }
    }

    /**
     * Som error has occurred making the execution of the ticket not possible.
     *
     * the ticket wil be removed and the owner notified
     */
    protected void voidTicket(TicketExitReason voidReason) {

        System.out.println("\n// ############### TICKET VOIDED ############### //");
        System.out.println("ticket id: " + this.ticketId);
        System.out.println("void reason: " + voidReason.name());
        System.out.println("// ############################################# //\n");

        try {
            compressResults(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setState(TicketStatus.VOIDED);
        this.cleanTicket(voidReason);

    }

    protected void compressResults(boolean includeLogs) throws IOException {
        if (this.logDir.exists() && includeLogs){
            Compression.zip(logDir, new File(saveDir, "logs.zip"));
        }

        if (saveDir.exists()){
            Compression.zip(saveDir, outFile);
        }
    }

    protected void cleanTicket(TicketExitReason exitReason){
        DockerFunctions.cleanTicket(this.ticketId);
        TicketDbFunctions.setTicketComplete(this.ticketId, exitReason);
        if (this.usedResourceManager != null){
            this.usedResourceManager.freeTicketResources(this);
        }
    }


    public ComputeResources.ResourceKey getResourceKey() {
        if (this.resourceKey.resourceId.equals(ComputeResources.defaultKey.resourceId)){
            try {
                resourceKey = ComputeResources.TranslateComputeResourceKey(ApiConfig.getResourceKey(this.apiConfigFile));
            } catch (Exception e){
                e.printStackTrace();
                dbl.log("error getting the resource key ");
            }

        }
        dbl.log("Returning resource key: ", resourceKey.resourceId);

        return resourceKey;
    }

    /**
     *  -- tbd -- Have to think about whether or not to use the sql or only local
     */
    public TicketStatus getState() {
        // TODO: do a sql call here to avoid a potential sync issue
        return state;
    }

    /**
     * Returns the common name of the ticket, that is the common prfix + the ticket id
     * @return The common name of the ticket
     */
    public String getCommonName() {
        return commonName;
    }

    /**
     * Sets the the new state of the ticket and updates the database
     * @param state the new state of the ticket
     */
    public void setState(TicketStatus state) {
        dbl.log(state);
        this.state = state;
        TicketDbFunctions.updateTicketStatus(ticketId, state);

    }

    /**
     * Return whether or not the ticket is don executing that is if it's done or voided
     * @return Whether or not the ticket is don executing.
     */
    public boolean isDone() {
        return this.state.equals(TicketStatus.DONE) || this.state.equals(TicketStatus.VOIDED) ;
    }

    /**
     * Returns the ticket id.
     * @return The ticket id.
     */
    public UUID getTicketId() {
        return ticketId;
    }

}
