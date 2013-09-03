package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

import java.util.Set;

public class PluginTwoCallback2 implements ResourceDiscoveryCallback{
    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails details) throws Exception {
        details.setResourceName("PluginTwoCallback2:name");
        details.setResourceVersion("PluginTwoCallback2:1.0");
        // notice we do not touch plugin config property TestServerTwo.prop1, our test code will check that it is null
        details.getPluginConfiguration().put(new PropertySimple("TestServerTwo.prop2", "PluginTwoCallback2:prop2"));
        System.out.println("!!!!!!!!!!!!!!" + this.getClass().getName() + "==>" + details);
        return DiscoveryCallbackResults.PROCESSED;
    }
}
