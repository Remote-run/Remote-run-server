package no.ntnu.enums;

/**
 * The statuses of the incoming tickets
 */
public enum TicketStatus {
    /**
     * The ticket is received, waiting but not built
     */
    WAITING,

    INSTALLING,

    /**
     * The ticket has been built and is ready to be run
     */
    READY,

    /**
     * The ticket is currently running
     */
    RUNNING,

    // maby remove
    STOPPED,

    /**
     * The ticket is complete and is waiting to be removed
     */
    DONE,

    /**
     * Some error has occurred. The error wil be sent and the ticket wil be discarded
     */
    VOIDED
}
