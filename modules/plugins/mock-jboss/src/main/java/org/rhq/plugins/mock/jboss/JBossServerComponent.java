package org.rhq.plugins.mock.jboss;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Author: Jason Dobies
 */
public class JBossServerComponent implements ResourceComponent, ConfigurationFacet, DeleteResourceFacet,
    CreateChildResourceFacet {
    private static final Log LOG = LogFactory.getLog(JBossServerComponent.class);

    /**
     * Resource types that use the JNDI name as the resource name and key. This is used during service creation.
     */
    private static final List<String> JNDI_KEYED_RESOURCES = new ArrayList<String>();

    static {
        JNDI_KEYED_RESOURCES.add("no-tx-datasource");
        JNDI_KEYED_RESOURCES.add("local-datasource");
        JNDI_KEYED_RESOURCES.add("xa-datasource");
        JNDI_KEYED_RESOURCES.add("jms-topic");
        JNDI_KEYED_RESOURCES.add("jms-queue");
    }

    private ResourceContext resourceContext;

    public void start(ResourceContext context) {
        this.resourceContext = context;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public Configuration loadResourceConfiguration() {
        return null;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        // Currently, there is no support for artifact based creations.
        // jdobies, May 29, 2007

        ResourceType resourceType = report.getResourceType();
        Configuration configuration = report.getResourceConfiguration();

        LOG.info("Creating new resource of type: " + resourceType);

        switch (resourceType.getCreationDataType()) {
        case CONFIGURATION:
            if (isJndiKeyedResource(resourceType)) {
                String jndiName = configuration.getSimple("jndiName").getStringValue();
                DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, jndiName, jndiName,
                    null, null, null, null);

                Map<String, DiscoveredResourceDetails> resourceMap = ResourceCache.getResourceMap(resourceType, this);
                resourceMap.put(jndiName, resource);

                // Initialize the new resource with an availability
                ResourceCache.putAvailability(jndiName, AvailabilityType.UP);
                ResourceCache.putConfiguration(jndiName, configuration);

                report.setResourceKey(jndiName);
            }
            break;

        case CONTENT:
            // The plugin is supposed to assign the resource key, so in this case just generate it based on the resource name
            String resourceName = report.getResourceName();
            String resourceKey = resourceName + "-key";

            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey, resourceName,
                null, null, null, null);
            Map<String, DiscoveredResourceDetails> resourceMap = ResourceCache.getResourceMap(resourceType, this);
            resourceMap.put(resourceKey, resource);

            ResourceCache.putAvailability(resourceKey, AvailabilityType.UP);
            ResourceCache.putConfiguration(resourceKey, new Configuration());

            report.setResourceKey(resourceKey);
            break;

        }

        report.setStatus(CreateResourceStatus.SUCCESS);
        return report;
    }

    public void deleteResource() {
        // No Op
    }

    @Override
    public String toString() {
        return "JBossServer: " + resourceContext.getResourceKey();
    }

    /**
     * Needed for the mock code - plugins normally do not want or need to expose this
     */
    ResourceContext getResourceContext() {
        return this.resourceContext;
    }

    private boolean isJndiKeyedResource(ResourceType resourceType) {
        return JNDI_KEYED_RESOURCES.contains(resourceType.getName());
    }
}
