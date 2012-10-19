package org.rhq.plugins.database;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

public class H2DatabaseDiscovery implements ResourceDiscoveryComponent<ResourceComponent<?>>, ManualAddFacet<ResourceComponent<?>> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ResourceComponent<?>> context) {
        return Collections.emptySet();
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
            ResourceDiscoveryContext<ResourceComponent<?>> context)
            throws InvalidPluginConfigurationException {

        String version = "";
        DiscoveredResourceDetails details = createResourceDetails(context, pluginConfiguration,
            version, null);
        return details;

    }

    private static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
            Configuration pluginConfig, String version, @Nullable
            ProcessInfo processInfo) {
            String key = pluginConfig.getSimpleValue("url", "");
            String name = key;
            String description = "Database " + version + " (" + key + ")";
            return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, description,
                pluginConfig, processInfo);
        }

}
