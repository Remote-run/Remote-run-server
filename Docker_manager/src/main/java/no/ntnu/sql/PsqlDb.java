package no.ntnu.sql;

import no.ntnu.enums.RunType;
import no.ntnu.util.DebugLogger;
import no.ntnu.util.ThrowingConsumer;

import java.sql.*;
import java.util.HashMap;
import java.util.UUID;

class PsqlDb {

    private static final String url = System.getenv("SQLURL");
    private static final String dbUser = System.getenv("POSGRESS_USER");
    private static final String dbPassword = System.getenv("POSGRESS_PASSWORD");

    protected static final DebugLogger allQueries = new DebugLogger(false);
    protected static final DebugLogger errorQueries = new DebugLogger(true);


    protected enum ticketsColumns{
        id,
        return_mail,
        run_priority,
        timestamp,
        status
    }


    protected static Connection tryConnectToDB(){
        allQueries.log("try connect to db", "url", url, "user", dbUser, "passwd", dbPassword);
        Connection connection = null;
        try{
            Class.forName("org.postgresql.Driver"); // i think this is to chek if the class exists

            connection = DriverManager.getConnection(url, dbUser, dbPassword);
        } catch (Exception e){
            e.printStackTrace();
        }

        return connection;
    }



    protected static void sqlQuery(String query, ThrowingConsumer<ResultSet, SQLException> rowHandler){
        try{
            sqlQueryUnCaught(query, rowHandler);
        } catch (SQLException e){
            errorQueries.log("ERROR QUERY:", query);
            e.printStackTrace();
        }
    }

    protected static void sqlQueryUnCaught(String query, ThrowingConsumer<ResultSet, SQLException> rowHandler) throws SQLException{

        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        allQueries.log("making SQL query:\n", query);
        ResultSet resultSet = statement.executeQuery(query);


        HashMap<UUID, RunType> tmpList = new HashMap<>();
        while (resultSet.next()){
            rowHandler.accept(resultSet);
        }

        resultSet.close();
        statement.close();
        connection.close();

    }



    protected static void sqlUpdate(String query){
        try{
            sqlUpdateUnCaught(query);
        } catch (SQLException e){
            errorQueries.log("ERROR QUERY:", query);
            e.printStackTrace();
        }
    }

    protected static void sqlUpdateUnCaught(String query) throws SQLException{
        Connection connection = tryConnectToDB();
        Statement statement = connection.createStatement();

        allQueries.log("making SQL update:\n", query);
        statement.executeUpdate(query);


        statement.close();
        connection.close();

    }
}
