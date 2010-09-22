package org.rhq.enterprise.server.plugins.cloud;

import java.io.Serializable;
import java.util.Map;

import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

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
        Map<String, Serializable> jobProperties = context.getProperties();
        
        if (!jobProperties.containsKey("counter")) {
            jobProperties.put("counter", 1);
            return;
        }

        Integer counter = (Integer)jobProperties.get("counter");
        System.out.println("CURRENT COUNT: " + counter);
        jobProperties.put("counter", counter + 1);
    }
}
