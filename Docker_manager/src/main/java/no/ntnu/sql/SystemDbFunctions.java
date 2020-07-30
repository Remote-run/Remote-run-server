package no.ntnu.sql;

import no.ntnu.dockerComputeRecources.ComputeResources;
import no.ntnu.dockerComputeRecources.WorkerNodeResourceManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class SystemDbFunctions extends PsqlDb{


    public static ComputeResources.ResourceKey[] getDbTicketResourceKeys() {
        Vector<ComputeResources.ResourceKey> tmpList = new Vector<>();
        String query = "SELECT * FROM resource_keys WHERE id NOT IN (SELECT resource_key FROM compute_nodes)";

        sqlQuery(query,resultSet -> {
            tmpList.add(new ComputeResources.ResourceKey(
                    resultSet.getString("id"),
                    resultSet.getInt("gpu"),
                    resultSet.getInt("cpu"),
                    resultSet.getInt("gig_ram")));
        });

        return tmpList.toArray(ComputeResources.ResourceKey[]::new);
    }

    public static HashMap<UUID,Long> getComputeNodesWithCheckIn() {
        HashMap<UUID,Long> result = new HashMap<>();
        String query = "SELECT id, last_check_in FROM compute_nodes";
        sqlQuery(query, resultSet -> result.put(UUID.fromString(resultSet.getString("id")),resultSet.getLong("last_check_in")));

        return result;
    }

    public static WorkerNodeResourceManager getWorkerResourceManagerById(UUID workerId) {
        AtomicReference<WorkerNodeResourceManager> resourceManager = null;
        String query = String.format(
                "SELECT k.gpu, k.cpu, k.gig_ram " +
                "FROM compute_nodes " +
                "INNER JOIN resource_keys k ON compute_nodes.resource_key = k.id " +
                "WHERE compute_nodes.id LIKE '%s';",workerId);
        sqlQuery(query, resultSet -> {
            ComputeResources.ResourceKey resourceKey = new ComputeResources.ResourceKey(
                    resultSet.getString("id"),
                    resultSet.getInt("gpu"),
                    resultSet.getInt("cpu"),
                    resultSet.getInt("gig_ram"));
            resourceManager.set(new WorkerNodeResourceManager(ComputeResources.mapUnitToComputeResource(resourceKey), workerId));
        });


        return resourceManager.get();
    }


    public static void activateTicket(UUID ticketId, UUID workerId){
        String query = String.format("INSERT INTO active_ticket (id,runner) "
                + "VALUES ('%s', '%s');", ticketId, workerId);
        sqlUpdate(query);
    }


    public static UUID[] getActiveWorkerTickets(UUID workerId) {
        Vector<UUID> tmpList = new Vector<>();
        String query = String.format(
                "SELECT ticket_id " +
                "FROM active_ticket " +
                "WHERE runner LIKE '%s';",workerId);
        sqlQuery(query,resultSet ->  tmpList.add(UUID.fromString(resultSet.getString("ticket_id"))));

        return tmpList.toArray(UUID[]::new);
    }

    public static Vector<UUID> getWorkerIdsSortedByBacklog(UUID ...workerIds) {
        Vector<UUID> sortedWorkerIds = null;

        String workerFilter = "";
        if (workerIds.length > 0){
            workerFilter = "WHERE runner IN (" +
                    Arrays.stream(workerIds).map(UUID::toString).collect(Collectors.joining(", ")) +
                            ")";
        }

        String query = "SELECT compute_nodes.id, count(t.id) " +
                "FROM compute_nodes " +
                "LEFT JOIN active_ticket a ON compute_nodes.id = a.runner " +
                "LEFT JOIN tickets t ON t.id = a.ticket_id " +
                "WHERE status LIKE 'WAITING' " +
                workerFilter +
                "GROUP BY compute_nodes.id " +
                "ORDER BY count(t.id);";

        sqlQuery(query, resultSet -> sortedWorkerIds.add(UUID.fromString(resultSet.getString(1))));


        return sortedWorkerIds;
    }

    public static void cleanOutWorker(UUID workerId){

        String query = "UPDATE tickets " +
                "SET status='WAITING' " +
                "WHERE id IN (SELECT ticket_id FROM active_ticket " +
                String.format("where runner='%s');", workerId);

        sqlUpdate(query);

        query = String.format("DELETE FROM compute_nodes WHERE id='%s';", workerId);
        sqlUpdate(query);
    }



























}
