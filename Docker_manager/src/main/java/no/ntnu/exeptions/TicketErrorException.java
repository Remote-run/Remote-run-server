package no.ntnu.exeptions;

/*
hva kan gå galt:

## alle:
- return mailen kan være noe som ikke er støttet (dette må implementeres)
- timeout. har stått og ventet/kjørt så lenge at den skrues av
- run error. kresjer av ukontrolerbare grunner  når den kjøres


## java
- pom ikke funnet
- Classpath ikke sottet
- mvn exec error

## python / andre
- executable filen finnes ikke
- image er tomm

 */


public class TicketErrorException extends Exception {


    public TicketErrorException(String message) {
        super(message);
    }
}

