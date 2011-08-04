package org.rhq.enterprise.server.plugins.cloud;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.util.LookupUtil;

public class CloudServerPluginComponent implements ServerPluginComponent, ControlFacet {

    private static Log log = LogFactory.getLog(CloudServerPluginComponent.class);

    public void initialize(ServerPluginContext context) throws Exception {
    }

    public void start() {
    }

    public void stop() {
    }

    public void shutdown() {
    }

    public ControlResults invoke(String name, Configuration parameters) {
        if ("syncServerEndpoint".equals(name)) {
            String serverName = parameters.getSimpleValue("name", null);
            String serverAddr = parameters.getSimpleValue("address", null);

            if (log.isDebugEnabled()) {
                log.debug("Invoked syncServerEndpoint with [name: " + serverName + ", address: " + serverAddr + "]");
            }

            ControlResults results = new ControlResults();

            CloudManagerLocal cloudMgr = LookupUtil.getCloudManager();
            Server server = cloudMgr.getServerByName(serverName);

            if (server == null) {
                log.warn("Failed to locate server. No address sync will be performed.");
                results.setError("No update performed. Failed to find server " + server.getName());
                return results;
            }

            if (serverAddr != null) {
                SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();

                server.setAddress(serverAddr);
                cloudMgr.updateServer(subjectMgr.getOverlord(), server);
            }

            int updateCount = notifyAgents(server);

            Configuration complexResults = results.getComplexResults();
            complexResults.put(new PropertySimple("results", updateCount + " agents have been updated."));

            if (log.isDebugEnabled()) {
                log.debug("Notified " + updateCount + " agents of the address change.");
            }

            return results;
        }

        return null;
    }

    public void syncServerEndpoints(ScheduledJobInvocationContext context) {
        log.debug("Preparing to sync server endpoints.");

        CloudManagerLocal cloudMgr = LookupUtil.getCloudManager();
        List<Server> servers = cloudMgr.getAllServers();

        purgeStaleServers(context, servers);

        for (Server server : servers) {
            if (!context.containsKey("server:" + server.getName())) {
                log.debug("Adding server [" + server.getName() + "] to sync list.");
                context.put("server:" + server.getName(), server.getAddress());
            } else if (addressChanged(context, server)) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected address change for " + server);
                    log.debug("Old address was " + context.get("server:" + server.getName() + ", new address is " +
                        server.getAddress()));
                }
                context.put("server:" + server.getName(), server.getAddress());
                notifyAgents(server);
            }
        }
    }

    private void purgeStaleServers(ScheduledJobInvocationContext context, List<Server> servers) {
        List<String> purgeList = new ArrayList<String>();

        Set<String> serverNames = new HashSet<String>();
        for (Server server : servers) {
            serverNames.add(server.getName());
        }

        for (String key : context.getJobData().keySet()) {
            if (key.startsWith("server:")) {
                String serverName = parseServerName(key);
                if (!serverNames.contains(serverName)) {
                    log.debug("Detected a stale server: " + serverName);
                    log.debug(serverName + " will be removed from the sync list.");
                    purgeList.add(key);
                }
            }
        }

        for (String staleServer : purgeList) {
            context.remove(staleServer);
        }
    }

    private String parseServerName(String key) {
        return key.substring("server:".length());
    }

    private boolean addressChanged(ScheduledJobInvocationContext context, Server server) {
        String lastKnownAddr = context.get("server:" + server.getName());
        return !server.getAddress().endsWith(lastKnownAddr);
    }

    @SuppressWarnings("unchecked")
    private int notifyAgents(Server server) {
        EntityManager entityMgr = LookupUtil.getEntityManager();
        String queryString = "select r " +
                             "from Resource r " +
                             "where r.resourceType.plugin = :pluginName and " +
                             "r.resourceType.name = :resourceTypeName and " +
                             "r.agent in (select a " +
                                         "from Agent a " +
                                         "where a.server = :server)";

        List<Resource> agents = entityMgr.createQuery(queryString)
            .setParameter("pluginName", "RHQAgent")
            .setParameter("resourceTypeName", "RHQ Agent")
            .setParameter("server", server)
            .getResultList();

        if (log.isDebugEnabled()) {
            log.debug("Found " + agents.size() + " to be updated with new server endpoint for " + server);
        }

        int numUpdated = 0;
        for (Resource agent : agents) {
            updateAgent(agent, server);
            numUpdated++;
        }

        return numUpdated;
    }

    private void updateAgent(Resource agent, Server server) {
        OperationManagerLocal operationMgr = LookupUtil.getOperationManager();
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();

        Configuration params = new Configuration();
        params.put(new PropertySimple("server", server.getAddress()));

        ResourceOperationSchedule schedule = operationMgr.scheduleResourceOperation(subjectMgr.getOverlord(),
            agent.getId(), "switchToServer", 0, 0, 0, 0, params, "Cloud Plugin: syncing server endpoint address");

        if (log.isDebugEnabled()) {
            log.debug("Schedule address sync for agent [name: " + agent.getName() + "].");
            log.debug("Operation schedule is " + schedule);
        }
    }
}
