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

    protected static final DebugLogger dbl = new DebugLogger(false);


    protected enum ticketsColumns{
        id,
        return_mail,
        run_priority,
        timestamp,
        status
    }


    protected static Connection tryConnectToDB(){
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

    protected static void sqlQuery(String query, ThrowingConsumer<ResultSet, SQLException> rowHandler){
        try{
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery(query);
            dbl.log("making SQL query:\n", query);

            HashMap<UUID, RunType> tmpList = new HashMap<>();
            while (resultSet.next()){
                rowHandler.accept(resultSet);
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    protected static void sqlUpdate(String query){
        try{
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            statement.executeUpdate(query);
            dbl.log("making SQL update:\n", query);

            statement.close();
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
}
