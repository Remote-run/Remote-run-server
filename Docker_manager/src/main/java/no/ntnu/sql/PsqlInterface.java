package no.ntnu.sql;

import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.ticket.Ticket;
import no.ntnu.util.DebugLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class PsqlInterface {
    private static final String url = System.getenv("SQLURL");
    private static final String dbUser = System.getenv("POSGRESS_USER");
    private static final String dbPassword = System.getenv("POSGRESS_PASSWORD");

    private static final DebugLogger dbl = new DebugLogger(false);


    private enum ticketsColumns{
        id,
        return_mail,
        run_priority,
        timestamp,
        status
    }


    private static Connection tryConnectToDB(){
        dbl.log("try connect to db", "url", url, "user", dbUser, "passwd", dbPassword);
        Connection connection = null;
        try{
            Class.forName("org.postgresql.Driver"); // i think this is to chek if the class exists
            connection = DriverManager.getConnection(url, dbUser, dbPassword);
        } catch (Exception e){
            e.printStackTrace();
        }

        return connection;
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


    public static void updateTicketStatus(UUID ticketNmr, TicketStatus ticketStatus) throws SQLException {
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = String.format("UPDATE tickets SET status = '%s' WHERE id='%s';", ticketStatus.name(), ticketNmr);
        dbl.log("making SQL query:\n", query);
        statement.executeUpdate(query);

        if (ticketStatus == TicketStatus.DONE){
            // the ticket is completed and is moved to the kill list
            String killQuery = String.format("INSERT INTO out (id) VALUES (%s);", ticketNmr);
            dbl.log("making SQL query:\n", query);
            statement.executeUpdate(killQuery);
        }

        statement.close();
        connection.close();

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



    public static UUID[] getTicketsByPriority() throws SQLException{
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = "SELECT id FROM tickets ORDER BY run_priority ,timestamp;";
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
