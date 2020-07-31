package no.ntnu.master;

import no.ntnu.config.ApiConfig;
import no.ntnu.enums.TicketStatus;
import no.ntnu.ticket.Ticket;
import no.ntnu.ticket.TicketExitReason;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;


/*

TODO: lag en mailing liste hvor maskina sende mail til folk med system feil


 */




public class Mail {
    private static final String senderMail = System.getenv("MAIL_USERNAME");
    private static final String senderPass = System.getenv("MAIL_PASSWORD");


    public static void sendGmail(String to, String subject, String contents){
        String host="smtp.gmail.com";
        final String user=senderMail;
        final String password=senderPass;


        Properties properties = new Properties();

        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user,password);
                    }
                });



        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
            message.setSubject(subject);
            message.setText(contents);

            Transport.send(message);

            //System.out.println("message sent successfully...");

        } catch (MessagingException e) {e.printStackTrace();}
    }

    public static void sendTicketDoneMail(UUID ticketId, String returnMail, TicketExitReason exitReason){
        String subject = "";
        String contents = "";
        String dlLink = "https://remote-run.uials.no/download/" + Ticket.commonPrefix + ticketId;
        switch (exitReason){
            case complete:
                subject = "Your run ticket is complete";
                contents = "your results can be downloaded from " + dlLink;
                break;
            case runError:
                subject = "Your run ticket encountered an error";
                contents = "your results (if any) and the error logs can be downloaded from " + dlLink;
                break;
            case buildError:
                subject = "Your run ticket encountered an error";
                contents = "your results (if any) and the error logs can be downloaded from " + dlLink;
                break;
            case mavenInstallError:
                subject = "Your run ticket encountered an error";
                contents = "your results (if any) and the error logs can be downloaded from " + dlLink;
                break;
            case timeout:
                subject = "";
                contents = "";
                break;
        }

        Mail.sendGmail(returnMail, subject,contents);

    }

}
