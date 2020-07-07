package no.ntnu.sql;

import no.ntnu.enums.RunType;
import no.ntnu.enums.TicketStatus;
import no.ntnu.util.DebugLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class PsqlInterface {
    private static final String url = System.getenv("SQLURL");
    private static final String dbUser = System.getenv("POSGRESS_USER");
    private static final String dbPassword = System.getenv("POSGRESS_PASSWORD");

    private static final DebugLogger dbl = new DebugLogger(true);



    private static Connection tryConnectToDB(){
        System.out.println("try connect to db");
        Connection connection = null;
        try{
            Class.forName("org.postgresql.Driver"); // i think this is to chek if the class exists
            connection = DriverManager.getConnection(url, dbUser, dbPassword);
        } catch (Exception e){
            e.printStackTrace();
        }

        return connection;
    }

    public static enum  dbView {
        priority_run_que,
        running,
        done,
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

        String query = String.format("UPDATE tickets SET status = '%s' WHERE id=%s;", ticketStatus.name(), ticketNmr);
        dbl.log("making SQL query:\n", query);
        statement.executeUpdate(query);

        if (ticketStatus == TicketStatus.DONE){
            // the ticket is completed and is moved to the kill list
            String killQuery = String.format("INSERT INTO out (id) VALUES (%s );", ticketNmr);
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

    public static Integer[] getSortedWaitingQue() throws SQLException {
        return getViewIdCol(dbView.priority_run_que);
    }

    public static Integer[] getRunning() throws SQLException {
        return getViewIdCol(dbView.running);
    }



    private static Integer[] getViewIdCol(dbView view) throws SQLException{
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        String query = String.format("SELECT id FROM %s", view.name());
        ResultSet resultSet = statement.executeQuery(query);

        ArrayList<Integer> tmpList = new ArrayList<>();
        while (resultSet.next()){
            tmpList.add(resultSet.getInt("id"));
        }

        resultSet.close();
        statement.close();
        connection.close();

        return tmpList.toArray(Integer[]::new);

    }


}
