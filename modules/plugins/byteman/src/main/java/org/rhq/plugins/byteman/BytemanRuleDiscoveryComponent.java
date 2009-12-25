package org.rhq.plugins.byteman;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class BytemanRuleDiscoveryComponent implements ResourceDiscoveryComponent<BytemanScriptComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BytemanScriptComponent> context)
        throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        List<String> rules = context.getParentResourceComponent().getRules();
        if (rules != null && !rules.isEmpty()) {
            for (String ruleName : rules) {
                details.add(new DiscoveredResourceDetails(context.getResourceType(), ruleName, ruleName, "unversioned",
                    "A rule defined in a Byteman script", null, null));
            }
        }

        return details;
    }

}
