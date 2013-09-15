package org.rhq.core.pc.inventory.discoverycallback;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;

public class DiscoveryCallbackAbortCallback2 implements ResourceDiscoveryCallback{
    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails details) throws Exception {
        details.setResourceName("Callback2");
        return DiscoveryCallbackResults.PROCESSED;
    }
}
