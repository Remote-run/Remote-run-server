package no.ntnu.exeptions;


/**
 * Exemption indicating there are some kind of error with the ticket which makes it not runnable
 */
public class TicketErrorException extends Exception {


    public TicketErrorException(String message) {
        super(message);
    }
}

