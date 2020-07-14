package no.ntnu.ticket;

import no.ntnu.enums.TicketStatus;
import no.ntnu.ticket.Ticket;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;

public class TicketDoneMail {

    public static void sendMail(String to, Ticket ticket){
        String dlLink = "localhost:8080/download/" + ticket.getCommonName();

        // Sender's email ID needs to be mentioned
        String from = "noreply@edu.com.io.hvasomhelst";

        // Assuming you are sending email from localhost
        String host = "mail";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            if (ticket.getState().equals(TicketStatus.VOIDED)){
                message.setSubject("Your run crashed");

                message.setText("To recive the pices (if any) click here:\n" + dlLink);
            } else {
                message.setSubject("Your run is done");

                message.setText("To recive the stuff:\n" + dlLink);
            }


            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
