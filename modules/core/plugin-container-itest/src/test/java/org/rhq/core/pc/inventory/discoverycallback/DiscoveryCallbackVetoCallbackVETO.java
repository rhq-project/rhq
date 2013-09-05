package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

public class DiscoveryCallbackVetoCallbackVETO implements ResourceDiscoveryCallback {
    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails details) throws Exception {
        if (details.getResourceKey().contains("abort")) {
            // if the key has "abort" in it - we want to veto it.
            details.setResourceName("Callback1"); // does nothing; just ensuring it has no effect on other resources
            return DiscoveryCallbackResults.VETO;
        } else {
            // any other discovered details will be skipped by this callback
            return DiscoveryCallbackResults.UNPROCESSED;
        }
    }
}
