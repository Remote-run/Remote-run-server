package no.ntnu.ticket;


/**
 * All the different reasons for a ticket to be marked as complete
 */
public enum TicketExitReason {

    /**
     * The ticket has completed whatever it was suposed to do without error
     */
    complete,

    /**
     * The ticket has used more than its allowed time slice and where terminated
     */
    timeout,

    /**
     * The ticket encontered an error while building and where terminated
     */
    buildError,

    /**
     * The ticket encontered an error while running and where terminated
     */
    runError,

    /**
     * The ticket encontered an error while installing the maven dependencies and where terminated
     */
    mavenInstallError,

}
