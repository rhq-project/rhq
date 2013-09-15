package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

public class DiscoveryCallbackVetoCallbackOK implements ResourceDiscoveryCallback{
    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails details) throws Exception {
        if (details.getResourceKey().contains("abort")) {
            // if the key has "abort" in it - this is the one our other callback will veto
            details.setResourceName("Should have been vetoed"); // does nothing; just ensuring it has no effect on other resources
            return DiscoveryCallbackResults.PROCESSED;
        } else {
            details.setResourceName("CallbackOK");
            return DiscoveryCallbackResults.PROCESSED;
        }
    }
}
