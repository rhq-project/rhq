package org.rhq.plugins.mysql;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.testng.annotations.Test;

/**
 * Tests MySql Server.
 */
@Test
public class PluginTest extends ComponentTest {

    String host = System.getProperty("host", "localhost");
    String principal = System.getProperty("principal", "mysql");
    String credentials = System.getProperty("credentials", "");
    String rtn = "MySql Server";

    {
        setProcessScan(false);
    }

    @Override
    protected void setConfiguration(Configuration c, ResourceType resourceType) {
        if (resourceType.getName().equals(rtn)) {
            c.getSimple("host").setStringValue(host);
            c.getSimple("principal").setStringValue(principal);
            c.getSimple("credentials").setStringValue(credentials);
        }
        if (resourceType.getName().equals("Database")) {
            c.getSimple("tableDiscovery").setBooleanValue(false);
        }
    }

    public void test() throws Exception {
        manuallyAdd("MySql Server");
        ResourceComponent resource = getComponent("MySql Server");
        assertUp(resource);
    }

}
