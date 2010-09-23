package org.rhq.enterprise.server.plugins.cloud;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
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
//        CloudManagerLocal cloudMgr = LookupUtil.getCloudManager();
//        List<Server> servers = cloudMgr.getAllServers();
//        Map<String, String> addresses = getAddresses(context);

//        for (Server server : servers) {
//            if (!addresses.containsKey(server.getName())) {
//                addresses.put(server.getName(), server.getAddress());
//            } else if (addressChanged(addresses, server)) {
//                addresses.put(server.getName(), server.getAddress());
//                notifyAgents(server);
//            }
//        }

        if (!context.containsKey("counter")) {
            context.put("counter", "1");
            return;
        }

        Integer counter = Integer.parseInt(context.get("counter"));
        System.out.println("CURRENT COUNT: " + counter);
        context.put("counter", Integer.toString(counter + 1));
    }

//    @SuppressWarnings("unchecked")
//    private Map<String, String> getAddresses(ScheduledJobInvocationContext context) {
//        Map<String, Serializable> jobData = context.getProperties();
//        if (!jobData.containsKey("serverAddresses")) {
//            jobData.put("serverAddresses", new HashMap<String, String>());
//        }
//        return (Map<String, String>) jobData.get("serverAddresses");
//    }
//
//    private boolean addressChanged(Map<String, String> addresses, Server server) {
//        String lastKnownAddr = addresses.get(server.getName());
//        return !server.getAddress().endsWith(lastKnownAddr);
//    }
//
//    private void notifyAgents(Server server) {
//        EntityManager entityMgr = LookupUtil.getEntityManager();
//    }
}
