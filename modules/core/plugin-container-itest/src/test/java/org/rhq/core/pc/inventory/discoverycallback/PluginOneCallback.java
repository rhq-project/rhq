package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

import java.util.Set;

public class PluginOneCallback implements ResourceDiscoveryCallback{
    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails details) throws Exception {
        // note that plugin 2 (the one that depends our plugin) will also have a callback and will
        // have a chance to overwrite what we do here. Our test code will test that the child plugin
        // callback in that plugin's callback (PluginTwoCallback1) can tweek what we do here.
        details.setResourceName("This will be overwritten by plugin two's test callback, so this string doesn't matter");
        details.setResourceVersion("This will be overwritten by plugin two's test callback, so this string doesn't matter");
        details.getPluginConfiguration().put(new PropertySimple("TestServerOne.prop1", "PluginOneCallback:prop1"));
        System.out.println("!!!!!!!!!!!!!!" + this.getClass().getName() + "==>" + details);
        // this plugin isn't being nice - it changed details but said it didn't. Our tests will show that the other
        // callbacks will overwrite some of these changes. In the future, we could support "PROCESSED_BUT_KEEP_GOING"
        // to allow a callback to change details but allow other callbacks to further process the details.
        return DiscoveryCallbackResults.UNPROCESSED;
    }
}
