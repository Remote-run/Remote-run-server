package no.ntnu.sql;

import no.ntnu.dockerComputeRecources.ComputeResources;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.ticket.TicketExitReason;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public class TicketDbFunctions extends PsqlDb {



    public static boolean setTicketResource(UUID uid, ComputeResources.ResourceKey resource) {
        boolean keyAdded = false;
        AtomicBoolean keyValid = new AtomicBoolean(false);

        String query = String.format("SELECT * FROM resource_keys WHERE id = '%s'", resource.resourceId);
        sqlQuery(query, resultSet -> {
            keyValid.set(true);
        });

        if (keyValid.get()){
            query = String.format("UPDATE tickets SET resource_key = '%s' WHERE id='%s';", resource.resourceId, uid);
            sqlUpdate(query);
        }

        return keyAdded;

    }


    public static void insertNewTicket(UUID uid, RunType runType, String returnMail, int priority){
        String query = String.format("INSERT INTO tickets (id, return_mail,run_type,run_priority) "
                + "VALUES ('%s', '%s', '%s', %o );", uid.toString(), returnMail, runType.name(), priority);

        sqlUpdate(query);
    }



    public static void updateTicketStatus(UUID ticketNmr, TicketStatus ticketStatus) {
        String query = String.format("UPDATE tickets SET status = '%s' WHERE id='%s';", ticketStatus.name(), ticketNmr);
        sqlUpdate(query);

    }

    public static void setTicketComplete(UUID ticketId, TicketExitReason exitReason) {
        String query = String.format("DELETE FROM active_ticket WHERE ticket_id='%s';", ticketId);
        sqlUpdate(query);

        query = String.format("INSERT INTO out (id, exit_reason) VALUES ('%s', '%s');", ticketId, exitReason.name());
        sqlUpdate(query);
    }

    public static void cleanTicket(UUID ticketNmr){
        String query = String.format("DELETE FROM out WHERE id='%s';", ticketNmr);
        sqlUpdate(query);
    }

    public static TicketStatus getTicketStatus(UUID ticketId) throws SQLException{
        String query = String.format("SELECT status FROM tickets WHERE id = '%s'", ticketId.toString());

        AtomicReference<String> statusStr = null;
        sqlQuery(query,resultSet -> statusStr.set(resultSet.getString("status")));
        return TicketStatus.valueOf(statusStr.get());
    }



    public static UUID[] getTicketsWithStatus(TicketStatus status){
        String query = String.format("SELECT id FROM tickets WHERE status = '%s'", status.name());
        Vector<UUID> tmpList = new Vector<>();
        sqlQuery(query,resultSet -> tmpList.add(UUID.fromString(resultSet.getString("id"))));

        return tmpList.toArray(UUID[]::new);
    }

    public static HashMap<UUID, Long> getCompleteList(){

        String query = "SELECT * FROM out;";
        HashMap<UUID, Long> tmpList = new HashMap<>();
        sqlQuery(query, resultSet -> tmpList.put(UUID.fromString(resultSet.getString("id")), resultSet.getLong("kill_at")));

        return tmpList;
    }



    public static HashMap<UUID, RunType> getRuntypes(UUID[] ids){

        String idList = Arrays.stream(ids).map(UUID::toString).collect(Collectors.joining("','"));
        String query = String.format("SELECT id, run_type FROM tickets WHERE id IN ('%s')", idList);

        HashMap<UUID, RunType> tmpList = new HashMap<>();
        sqlQuery(query,resultSet -> tmpList.put(UUID.fromString(resultSet.getString("id")), RunType.valueOf(resultSet.getString("id"))));

        return tmpList;
    }


    /**
     * Returns the id off every ticket that is not in the out or staging table
     * @return
     * @throws SQLException
     */
    public static UUID[] getTicketsByPriority(){


        String query = "SELECT id " +
                "FROM tickets, s " +
                "WHERE id IN (SELECT ticket_id FROM active_ticket UNION SELECT id FROM out) ORDER BY run_priority ,timestamp ;";

        ArrayList<UUID> tmpList = new ArrayList<>();
        sqlQuery(query,resultSet -> tmpList.add(UUID.fromString(resultSet.getString("id"))));

        return tmpList.toArray(UUID[]::new);
    }

    public static UUID[] getAllTicketUUID(){

        String query = "SELECT id FROM tickets;";
        ArrayList<UUID> tmpList = new ArrayList<>();
        sqlQuery(query, resultSet -> tmpList.add(UUID.fromString(resultSet.getString("id"))));

        return tmpList.toArray(UUID[]::new);
    }


}
