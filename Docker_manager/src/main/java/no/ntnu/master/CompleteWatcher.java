package no.ntnu.master;

import no.ntnu.Main;
import no.ntnu.sql.TicketDbFunctions;
import no.ntnu.ticket.TicketExitReason;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class CompleteWatcher extends Watcher {



    public CompleteWatcher(Long waitTime) {
        super(waitTime);
    }

    /**
     * The actions the watcher needs to preform at every time step
     */
    @Override
    public void act() {
        HashMap<UUID, String[]> mailList = TicketDbFunctions.getCompleteButNotMailList();

        mailList.forEach((uuid, strings) -> {
            String retMail = strings[0];
            TicketExitReason exitReason = TicketExitReason.valueOf(strings[1]);
            //Mail.sendTicketDoneMail(uuid, retMail, exitReason);
            dbl.log("\n\n DUMMY MAIL \n\n");
            TicketDbFunctions.setMailSent(uuid);
        });
    }

}
