package no.ntnu.sql;

import no.ntnu.dockerComputeRecources.ComputeResources;
import no.ntnu.dockerComputeRecources.ResourceManager;
import no.ntnu.dockerComputeRecources.ResourceTypes.ComputeResource;
import no.ntnu.dockerComputeRecources.WorkerNodeResourceManager;
import no.ntnu.enums.TicketStatus;
import no.ntnu.ticket.Ticket;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class SystemDbFunctions extends PsqlDb{


    public static ComputeResources.ResourceKey[] getDbTicketResourceKeys() {
        Vector<ComputeResources.ResourceKey> tmpList = new Vector<>();
        try {
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = "SELECT * FROM resource_keys WHERE id NOT IN (SELECT resource_key FROM compute_nodes)";
            ResultSet resultSet = statement.executeQuery(query);
            dbl.log("making SQL query:\n", query);


            while (resultSet.next()){
                tmpList.add(new ComputeResources.ResourceKey(
                        resultSet.getString("id"),
                        resultSet.getInt("gpu"),
                        resultSet.getInt("cpu"),
                        resultSet.getInt("gig_ram")));
            }

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e){
            e.printStackTrace();
        }

        return tmpList.toArray(ComputeResources.ResourceKey[]::new);
    }

    public static HashMap<UUID,Long> getComputeNodesWithCheckIn() {
        HashMap<UUID,Long> result = new HashMap<>();
        try {
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = "SELECT id, last_check_in FROM compute_nodes";
            ResultSet resultSet = statement.executeQuery(query);
            dbl.log("making SQL query:\n", query);


            while (resultSet.next()){
                result.put(UUID.fromString(resultSet.getString("id")),resultSet.getLong("last_check_in"));
            }

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e ){
            e.printStackTrace();
        }

        return result;
    }

    public static WorkerNodeResourceManager getWorkerResourceManagerById(UUID workerId) {
        WorkerNodeResourceManager resourceManager = null;
        try {
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = String.format(
                    "SELECT k.gpu, k.cpu, k.gig_ram " +
                    "FROM compute_nodes " +
                    "INNER JOIN resource_keys k ON compute_nodes.resource_key = k.id " +
                    "WHERE compute_nodes.id LIKE '%s';",workerId);
            ResultSet resultSet = statement.executeQuery(query);
            dbl.log("making SQL query:\n", query);


            while (resultSet.next()){
                ComputeResources.ResourceKey resourceKey = new ComputeResources.ResourceKey(
                        resultSet.getString("id"),
                        resultSet.getInt("gpu"),
                        resultSet.getInt("cpu"),
                        resultSet.getInt("gig_ram"));
                resourceManager = new WorkerNodeResourceManager(ComputeResources.mapUnitToComputeResource(resourceKey), workerId);
            }

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e){
            e.printStackTrace();
        }

        return resourceManager;
    }


    public static void activateTicket(UUID ticketId, UUID workerId){
        try {
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = String.format("INSERT INTO active_ticket (id,runner) "
                    + "VALUES ('%s', '%s');", ticketId, workerId);
            statement.executeUpdate(query);
            dbl.log("making SQL query:\n", query);

            statement.close();
            connection.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public static UUID[] getActiveWorkerTickets(UUID workerId) {
        Vector<UUID> tmpList = new Vector<>();
        try {
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = String.format(
                    "SELECT ticket_id " +
                            "FROM active_ticket " +
                            "WHERE runner LIKE '%s';",workerId);
            ResultSet resultSet = statement.executeQuery(query);
            dbl.log("making SQL query:\n", query);

            while (resultSet.next()){
                tmpList.add(UUID.fromString(resultSet.getString("ticket_id")));
            }

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e){
            e.printStackTrace();
        }

        return tmpList.toArray(UUID[]::new);
    }

    public static Vector<UUID> getWorkerIdsSortedByBacklog(UUID ...workerIds) {
        Vector<UUID> sortedWorkerIds = null;
        try {
            String workerFilter = "";
            if (workerIds.length > 0){
                workerFilter = "WHERE runner IN (" +
                        Arrays.stream(workerIds).map(UUID::toString).collect(Collectors.joining(", ")) +
                                ")";
            }
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = "SELECT compute_nodes.id, count(t.id) " +
                    "FROM compute_nodes " +
                    "LEFT JOIN active_ticket a ON compute_nodes.id = a.runner " +
                    "LEFT JOIN tickets t ON t.id = a.ticket_id " +
                    "WHERE status LIKE 'WAITING' " +
                    workerFilter +
                    "GROUP BY compute_nodes.id " +
                    "ORDER BY count(t.id);";


            ResultSet resultSet = statement.executeQuery(query);
            dbl.log("making SQL query:\n", query);


            while (resultSet.next()){
                sortedWorkerIds.add(UUID.fromString(resultSet.getString(1)));
            }

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e){
            e.printStackTrace();
        }

        return sortedWorkerIds;
    }

    public static void cleanOutWorker(UUID workerId){
        try {
            Connection connection = tryConnectToDB();
            Statement statement = connection.createStatement();

            String query = "UPDATE tickets " +
                    "SET status='WAITING' " +
                    "WHERE id IN (SELECT ticket_id FROM active_ticket " +
                    String.format("where runner='%s');", workerId);

            statement.executeUpdate(query);
            dbl.log("making SQL query:\n", query);

            query = String.format("DELETE FROM compute_nodes WHERE id='%s';", workerId);

            statement.close();
            connection.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }



























}
