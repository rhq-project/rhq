package org.rhq.plugins.byteman;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class BytemanScriptDiscoveryComponent implements ResourceDiscoveryComponent<BytemanAgentComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BytemanAgentComponent> context)
        throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        Map<String, String> allScripts = context.getParentResourceComponent().getAllKnownScripts();
        if (allScripts != null && !allScripts.isEmpty()) {
            for (String scriptName : allScripts.keySet()) {
                int startShortName = scriptName.lastIndexOf("\\");
                if (startShortName == -1) {
                    startShortName = scriptName.lastIndexOf("/");
                }
                String shortScriptName = scriptName.substring(startShortName + 1); // script names never end with \ or /, so this is OK
                details.add(new DiscoveredResourceDetails(context.getResourceType(), scriptName, shortScriptName,
                    "unversioned", "A script with Byteman rules", null, null));
            }
        }

        return details;
    }

}
