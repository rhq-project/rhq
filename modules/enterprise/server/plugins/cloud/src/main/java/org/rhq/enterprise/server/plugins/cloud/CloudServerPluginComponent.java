package org.rhq.enterprise.server.plugins.cloud;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.util.LookupUtil;

public class CloudServerPluginComponent implements ServerPluginComponent {

    public void initialize(ServerPluginContext context) throws Exception {
    }

    public void start() {
    }

    public void stop() {
    }

    public void shutdown() {
    }

    public void syncServerEndpoints(ScheduledJobInvocationContext context) {
        CloudManagerLocal cloudMgr = LookupUtil.getCloudManager();
        List<Server> servers = cloudMgr.getAllServers();

        for (Server server : servers) {
            if (!context.containsKey("server:" + server.getName())) {
                context.put("server:" + server.getName(), server.getAddress());
            } else if (addressChanged(context, server)) {
                context.put("server:" + server.getName(), server.getAddress());
                notifyAgents(server);
            }
        }
    }

    private boolean addressChanged(ScheduledJobInvocationContext context, Server server) {
        String lastKnownAddr = context.get("server:" + server.getName());
        return !server.getAddress().endsWith(lastKnownAddr);
    }

    @SuppressWarnings("unchecked")
    private void notifyAgents(Server server) {
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

        for (Resource agent : agents) {
            updateAgent(agent, server);
        }
    }

    private void updateAgent(Resource agent, Server server) {
        OperationManagerLocal operationMgr = LookupUtil.getOperationManager();
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();

        Configuration params = new Configuration();
        params.put(new PropertySimple("server", server.getAddress()));

        operationMgr.scheduleResourceOperation(subjectMgr.getOverlord(), agent.getId(), "switchToServer", 0, 0, 0, 0,
            params, "Server endpoint has changed. Sending new address to agent.");
    }
}
