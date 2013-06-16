package org.rhq.core.pc.upgrade.plugins.v2;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

public class DiscComponent<T extends ResourceComponent<?>> implements ResourceDiscoveryComponent<T>,
    ResourceUpgradeFacet<T> {
    private static final String V1_RESOURCE_KEY = "resource-key-v1";
    private static final String RESOURCE_KEY = "resource-key-v2";
    private static final String RESOURCE_NAME = "resource-name-v2";
    private static final String RESOURCE_DESCRIPTION = "resource-description-v2";
    private static final String TEST_PROPERTY = "test-property";
    private static final String RESOURCE_TEST_PROPERTY = "test-property-v2";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context)
        throws InvalidPluginConfigurationException, Exception {
        Configuration pluginConfig = context.getDefaultPluginConfiguration();
        pluginConfig.put(new PropertySimple(TEST_PROPERTY, RESOURCE_TEST_PROPERTY));
        return Collections.singleton(new DiscoveredResourceDetails(context.getResourceType(), RESOURCE_KEY,
            RESOURCE_NAME, null, RESOURCE_DESCRIPTION, pluginConfig, null));
    }

    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<T> inventoriedResource) {
        if (V1_RESOURCE_KEY.equals(inventoriedResource.getResourceKey())) {
            ResourceUpgradeReport report = new ResourceUpgradeReport();
            report.setNewResourceKey(RESOURCE_KEY);
            report.setNewName(RESOURCE_NAME);
            report.setNewDescription(RESOURCE_DESCRIPTION);
            Configuration newPluginConfiguration = new Configuration();
            newPluginConfiguration.put(new PropertySimple(TEST_PROPERTY, RESOURCE_TEST_PROPERTY));
            report.setNewPluginConfiguration(newPluginConfiguration);

            File dataDir = inventoriedResource.getDataDirectory();

            if (dataDir != null) {
                if (!(dataDir.exists())) {
                    dataDir.mkdir();
                }
                File marker = new File(dataDir, "upgrade-succeeded");
                try {
                    marker.createNewFile();
                } catch (IOException localIOException) {
                }
            }
            return report;
        }

        return null;
    }
}
