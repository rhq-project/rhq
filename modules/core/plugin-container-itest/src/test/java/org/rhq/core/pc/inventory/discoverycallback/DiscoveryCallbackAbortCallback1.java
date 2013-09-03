package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

public class DiscoveryCallbackAbortCallback1 implements ResourceDiscoveryCallback {
    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails details) throws Exception {
        if (details.getResourceKey().contains("abort")) {
            // if the key has "abort" in it - process it - our other callback will do the same, causing the abort
            details.setResourceName("Callback1");
            return DiscoveryCallbackResults.PROCESSED;
        } else {
            // any other discovered details will be skipped by this callback
            return DiscoveryCallbackResults.UNPROCESSED;
        }
    }
}
