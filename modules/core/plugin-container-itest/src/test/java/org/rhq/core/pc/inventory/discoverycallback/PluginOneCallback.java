package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

import java.util.Set;

public class PluginOneCallback implements ResourceDiscoveryCallback{
    @Override
    public void discoveredResources(Set<DiscoveredResourceDetails> discoveredDetails) throws Exception {
        // we know our test discovery component detects one singleton server, let's tweek the details.
        // note that plugin 2 (the one that depends our plugin) will also have a callback and will
        // have a chance to overwrite what we do here. Our test code will test that the child plugin
        // callback in that plugin's callback (PluginTwoCallback1) can tweek what we do here.
        DiscoveredResourceDetails details = discoveredDetails.iterator().next();
        details.setResourceName("This will be overwritten by plugin two's test callback, so this string doesn't matter");
        details.setResourceVersion("This will be overwritten by plugin two's test callback, so this string doesn't matter");
        details.getPluginConfiguration().put(new PropertySimple("TestServerOne.prop1", "PluginOneCallback:prop1"));
        System.out.println("!!!!!!!!!!!!!!" + this.getClass().getName() + "==>" + discoveredDetails);
    }
}
