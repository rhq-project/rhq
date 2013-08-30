package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

import java.util.Set;

public class PluginTwoCallback1 implements ResourceDiscoveryCallback{
    @Override
    public void discoveredResources(Set<DiscoveredResourceDetails> discoveredDetails) throws Exception {
        // the resource was discovered, and our plugin one's callback has tweeked these details, but now we can
        // further tweek the details here
        DiscoveredResourceDetails details = discoveredDetails.iterator().next();
        details.setResourceName("PluginTwoCallback1:name");
        details.setResourceVersion("PluginTwoCallback1:1.0");
        // notice we do not touch plugin config property TestServerOne.prop1, let the first plugin's callback touch it
        details.getPluginConfiguration().put(new PropertySimple("TestServerOne.prop2", "PluginTwoCallback1:prop2"));
        System.out.println("!!!!!!!!!!!!!!" + this.getClass().getName() + "==>" + discoveredDetails);
    }
}
