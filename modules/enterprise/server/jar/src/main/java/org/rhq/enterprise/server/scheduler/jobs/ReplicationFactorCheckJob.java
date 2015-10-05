/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.server.metrics.StorageSession;

/**
 * This job checks replication factor of storage cluster. In case the factor is not correct according to cluster size, ERROR log message 
 * is produced
 * @author lzoubek@redhat.com
 *
 */
public class ReplicationFactorCheckJob extends AbstractStatefulJob {
    private static final Log log = LogFactory.getLog(ReplicationFactorCheckJob.class);

    private static final List<String> KEYSPACES = Arrays.asList("rhq", "system_auth");

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        debug(getClass().getName() + " job starting");
        StorageNodeManagerLocal storageNodeManager = LookupUtil.getStorageNodeManager();
        SystemSettings settings = LookupUtil.getSystemManager().getObfuscatedSystemSettings(true);

        String username = settings.get(SystemSetting.STORAGE_USERNAME);
        String password = settings.get(SystemSetting.STORAGE_PASSWORD);

        List<StorageNode> storageNodes = storageNodeManager.getStorageNodes();
        List<String> hostNames = new ArrayList<String>();

        for (StorageNode storageNode : storageNodes) {
            // We only want clustered nodes here because we won't be able to connect to
            // node that is not part of the cluster. The filtering here on the operation
            // mode is somewhat convservative because we could also include ADD_MAINTENANCE
            // and REMOVE_MAINTENANCE, but this errors on the side of being safe. Lastly,
            // if a storage node does not have a resource, then that means it was was
            // deployed prior to installing the server.
            if (storageNode.getOperationMode() == StorageNode.OperationMode.NORMAL
                || storageNode.getOperationMode() == StorageNode.OperationMode.MAINTENANCE
                || storageNode.getResource() == null) {
                hostNames.add(storageNode.getAddress());
            }
        }
        if (hostNames.isEmpty()) {
            log.error("There is not storage node in relational database to connect! Please re-install at least 1 storage node");
            return;
        }
        debug("Initiating connection to cluster hosts=" + Arrays.toString(hostNames.toArray()));
        int port = storageNodes.get(0).getCqlPort();

        Cluster cluster = new ClusterBuilder().addContactPoints(hostNames.toArray(new String[hostNames.size()]))
            .withCredentialsObfuscated(username, password).withPort(port)
            .withLoadBalancingPolicy(new RoundRobinPolicy())
            .withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE))
            .withCompression(ProtocolOptions.Compression.NONE).build();

        // need to connect to system keyspace as it contains data about other keyspaces (and replication factor)
        Session session = null;
        try {
            session = cluster.connect("system");
            debug("Querying system keyspaces for strategy_options");
            Map<String, Integer> replicationFactors = getReplicationFactors(session);

            if (replicationFactors.size() != KEYSPACES.size()) {
                log.error("Failed to query storage cluster for keyspaces " + Arrays.toString(KEYSPACES.toArray())
                    + " for replication_factor, expected to retrieve " + KEYSPACES.size() + " but got "
                    + replicationFactors.size());

                return;
            }
            Map<String, Integer> factorsToSet = new HashMap<String, Integer>(replicationFactors.size());
            String inconsistency = "";
            for (Entry<String,Integer> factorEntry : replicationFactors.entrySet()) {
                int healthy = getHealthyReplicationFactor(factorEntry.getKey(), storageNodes.size());
                int current = factorEntry.getValue().intValue();

                if (current != healthy) {
                    inconsistency += "keyspace [" + factorEntry.getKey() + "] has replication_factor=" + current
                        + " but it should be " + healthy + ", ";
                    factorsToSet.put(factorEntry.getKey(), healthy);
                }
            }

            if (!factorsToSet.isEmpty()) {
                // strip last ", "
                inconsistency = inconsistency.substring(0, inconsistency.length() - 2);
                log.warn("Storage Cluster is not consistent! There are "
                    + storageNodes.size()
                    + " StorageNodes in RDBMS and "
                    + inconsistency
                    + ". This can happen in case StorageNode deployment/undeployment fails. ");

                log.info("Updating replication_factor for keyspaces "
                    + Arrays.toString(factorsToSet.keySet().toArray()));

                for (Entry<String, Integer> factor : factorsToSet.entrySet()) {
                    updateReplicationFactor(session, factor.getKey(), factor.getValue().intValue());
                }
                log.info("Replication factor(s) have been fixed, data in cluster wil be made consistent the next time storage maintenance job"
                    + " finishes or can be started manually via CLI using StorageNodeManager.runClusterMaintenance()");

            } else {
                debug("Storage Cluster replication_factor check finished, replication_factor is correct");
            }
        } catch (NoHostAvailableException ex) {
            log.error("Failed to connect to storage cluster", ex);

        } catch (Exception ex) {
            log.error("Failed to connect to storage cluster", ex);
        } finally {
            if (session != null) {
                session.shutdown();
            }
            cluster.shutdown();
        }
    }

    private Map<String, Integer> getReplicationFactors(Session session) {
        Map<String, Integer> factors = new HashMap<String, Integer>();
        List<Row> result = session.execute("select keyspace_name, strategy_options from schema_keyspaces").all();
        for (Row row : result) {
            String keyspace = row.getString("keyspace_name");
            // we're only interested in those 2 keypsaces
            if (KEYSPACES.contains(keyspace)) {
                Integer replicationFactor = readReplicationFactor(row.getString("strategy_options"));
                if (replicationFactor == null) {
                    // exception has been logged in readReplicationFactor
                    continue;
                }
                factors.put(keyspace, replicationFactor);
            }

        }
        return factors;
    }

    private static void debug(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
    }

    private Integer readReplicationFactor(String text) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> map = new ObjectMapper().readValue(text, Map.class);
            String factor = map.get("replication_factor");
            return Integer.parseInt(factor);
        } catch (Exception e) {
            log.error("Unable to parse strategy_options column from " + text, e);
        }
        return null;
    }

    public static ResultSet updateReplicationFactor(Session session, String keyspace, int replicationFactor) {
        debug("Updating replication_factor=" + replicationFactor + " for keyspace " + keyspace);
        return session.execute(createUpdateReplicationFactorStatement(keyspace, replicationFactor));
    }

    public static ResultSet updateReplicationFactor(StorageSession session, String keyspace, int replicationFactor) {
        debug("Updating replication_factor=" + replicationFactor + " for keyspace " + keyspace);
        return session.execute(createUpdateReplicationFactorStatement(keyspace, replicationFactor));
    }

    private static String createUpdateReplicationFactorStatement(String keyspace, int replicationFactor) {
        return "ALTER KEYSPACE " + keyspace + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': "
            + replicationFactor + "}";
    }

    static int getHealthyReplicationFactor(String keyspace, int clusterSize) {
        if ("system_auth".equals(keyspace)) {
            return clusterSize;
        }
        if (clusterSize == 2) {
            return 2;
        }
        if (clusterSize > 2) {
            return 3;
        }
        return 1;
    }

}
