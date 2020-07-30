package no.ntnu.sql;

import no.ntnu.dockerComputeRecources.ComputeResources;
import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.ticket.TicketExitReason;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class TicketDbFunctions extends PsqlDb {



    public static boolean setTicketResource(UUID uid, ComputeResources.ResourceKey resource) {
        boolean keyAdded = false;

        try{
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            // try to get the current resource key at the id
            String query = String.format("SELECT * FROM resource_keys WHERE id = '%s'", resource.resourceId);
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()){
                // the key exist just give it to the ticket
                resultSet.close();

                dbl.log("making SQL query:\n", query);
                statement.executeUpdate(query);
                keyAdded = true;
            }

            statement.close();
            connection.close();

        } catch (Exception e){
            e.printStackTrace();
        }

        return keyAdded;

    }


    public static void insertNewTicket(UUID uid, RunType runType, String returnMail, int priority) throws SQLException {
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        dbl.log(uid.toString(), "<- skal se ca slik ut : 0e37df36-f698-11e6-8dd4-cb9ced3df976");

        String query = String.format("INSERT INTO tickets (id, return_mail,run_type,run_priority) "
                + "VALUES ('%s', '%s', '%s', %o );", uid.toString(), returnMail, runType.name(), priority);

        dbl.log("making SQL query:\n", query);
        statement.executeUpdate(query);

        statement.close();
        connection.close();
    }



    public static void updateTicketStatus(UUID ticketNmr, TicketStatus ticketStatus) {
        try{
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = String.format("UPDATE tickets SET status = '%s' WHERE id='%s';", ticketStatus.name(), ticketNmr);
            dbl.log("making SQL query:\n", query);
            statement.executeUpdate(query);


            statement.close();
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static void setTicketComplete(UUID ticketId, TicketExitReason exitReason) {
        try{
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = String.format("DELETE FROM active_ticket WHERE ticket_id='%s';", ticketId);
            dbl.log("making SQL query:\n", query);
            statement.executeUpdate(query);

            query = String.format("INSERT INTO out (id, exit_reason) VALUES ('%s', '%s');", ticketId, exitReason.name());
            dbl.log("making SQL query:\n", query);
            statement.executeUpdate(query);


            statement.close();
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static void cleanTicket(UUID ticketNmr) throws SQLException {
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = String.format("DELETE FROM out WHERE id='%s';", ticketNmr);
        dbl.log("making SQL query:\n", query);
        statement.executeUpdate(query);

        statement.close();
        connection.close();
    }

    public static TicketStatus getTicketStatus(UUID ticketId) throws SQLException{


        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = String.format("SELECT status FROM tickets WHERE id = '%s'", ticketId.toString());
        ResultSet resultSet = statement.executeQuery(query);
        resultSet.next();

        String statusStr = resultSet.getString("status");
        TicketStatus ticketStatus = null;

        try {
            ticketStatus = TicketStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e){}


        resultSet.close();
        statement.close();
        connection.close();

        return ticketStatus;

    }



    public static UUID[] getTicketsWithStatus(TicketStatus status) throws SQLException{
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = String.format("SELECT id FROM tickets WHERE status = '%s'", status.name());
        ResultSet resultSet = statement.executeQuery(query);

        ArrayList<UUID> tmpList = new ArrayList<>();
        while (resultSet.next()){
            tmpList.add(UUID.fromString(resultSet.getString("id")));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return tmpList.toArray(UUID[]::new);

    }

    public static HashMap<UUID, Long> __getCompleteList() throws SQLException{
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = "SELECT * FROM out;";
        ResultSet resultSet = statement.executeQuery(query);

        HashMap<UUID, Long> tmpList = new HashMap<>();
        while (resultSet.next()){
            tmpList.put(UUID.fromString(resultSet.getString("id")), resultSet.getLong("kill_at"));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return tmpList;
    }

    public static HashMap<UUID, Long> getCompleteList(){

        String query = "SELECT * FROM out;";
        HashMap<UUID, Long> tmpList = new HashMap<>();
        query(query, resultSet -> tmpList.put(UUID.fromString(resultSet.getString("id")), resultSet.getLong("kill_at")));

        return tmpList;
    }



    public static HashMap<UUID, RunType> getRuntypes(UUID[] ids) throws SQLException{
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String idList = Arrays.stream(ids).map(UUID::toString).collect(Collectors.joining("','"));

        String query = String.format("SELECT id, run_type FROM tickets WHERE id IN ('%s')", idList);
        ResultSet resultSet = statement.executeQuery(query);

        HashMap<UUID, RunType> tmpList = new HashMap<>();
        while (resultSet.next()){
            tmpList.put(UUID.fromString(resultSet.getString("id")), RunType.valueOf(resultSet.getString("id")));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return tmpList;
    }


    /**
     * Returns the id off every ticket that is not in the out or staging table
     * @return
     * @throws SQLException
     */
    public static UUID[] getTicketsByPriority() throws SQLException{

        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = "SELECT id " +
                "FROM tickets, s " +
                "WHERE id IN (SELECT ticket_id FROM active_ticket UNION SELECT id FROM out) ORDER BY run_priority ,timestamp ;";
        ResultSet resultSet = statement.executeQuery(query);

        ArrayList<UUID> tmpList = new ArrayList<>();
        while (resultSet.next()){
            tmpList.add(UUID.fromString(resultSet.getString("id")));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return tmpList.toArray(UUID[]::new);
    }

    public static UUID[] getAllTicketUUID() throws SQLException{
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = "SELECT id FROM tickets;";
        ResultSet resultSet = statement.executeQuery(query);

        ArrayList<UUID> tmpList = new ArrayList<>();
        while (resultSet.next()){
            tmpList.add(UUID.fromString(resultSet.getString("id")));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return tmpList.toArray(UUID[]::new);
    }


}
