package org.rhq.plugins.mock.jboss;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.mock.jboss.configuration.ConfigurationHandler;
import org.rhq.plugins.mock.jboss.metrics.MetricValueCalculator;
import org.rhq.plugins.mock.jboss.operations.OperationHandler;
import org.rhq.plugins.mock.jboss.scenario.ScenarioArtifact;
import org.rhq.plugins.mock.jboss.scenario.ScenarioMetric;
import org.rhq.plugins.mock.jboss.scenario.ScenarioOperationResult;
import org.rhq.plugins.mock.jboss.scenario.ScenarioProperty;
import org.rhq.plugins.mock.jboss.scenario.ScenarioResource;
import org.rhq.plugins.mock.jboss.scenario.ScenarioResourceConfigurationError;

/**
 * Plugin discovery component representing any resource in the mock that uses the JNDI name as the resource name and
 * key.
 *
 * In order for a service to utilize this class as its discovery component, it must define a property called "jndiName"
 * that will be used. An example of this is the no-tx-datasource service can be found in the rhq-plugin.xml in this
 * module.
 *
 * Author: Jason Dobies
 */
public class JndiResourceDiscoveryComponent implements ResourceDiscoveryComponent<JBossServerComponent> {
    /**
     * Property name used to retrieve the JNDI name, which will act as the resource name and key.
     */
    private static final String JNDI_NAME_PROPERTY = "jndiName";

    private final Log LOG = LogFactory.getLog(JndiResourceDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JBossServerComponent> context) {
        LOG.info("Initializing to Service Type: " + context.getResourceType());

        if (!ResourceCache.resourceMapExists(context.getResourceType(), context.getParentResourceComponent()))
            initializeFromScenario(context);

        // Pull the resources from the cache so updates from components are taken into account
        Map<String, DiscoveredResourceDetails> cacheMap = ResourceCache.getResourceMap(context.getResourceType(),
            context.getParentResourceComponent());

        // Copy the list 
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        for (DiscoveredResourceDetails r : cacheMap.values()) {
            discoveredResources.add(new DiscoveredResourceDetails(r.getResourceType(), r.getResourceKey(), r
                .getResourceName(), r.getResourceVersion(), r.getResourceDescription(), null, null));
        }

        return discoveredResources;
    }

    // Private  --------------------------------------------

    /**
     * Parses each resource that should be discovered by this configuration into domain objects and populates the cached
     * resource map for this combination.
     *
     * @param context specific server to load the resources for.
     */
    private void initializeFromScenario(ResourceDiscoveryContext<JBossServerComponent> context) {
        ScenarioLoader scenarioLoader = ScenarioLoader.getInstance();

        // Convert each scenario resource into a domain Resource
        List<ScenarioResource> scenarioResources = scenarioLoader.getResources(context.getParentResourceComponent()
            .getResourceContext().getResourceKey(), context.getResourceType().getName());
        Map<String, DiscoveredResourceDetails> scenarioResourceMap = new HashMap<String, DiscoveredResourceDetails>(
            scenarioResources.size());
        for (ScenarioResource scenarioResource : scenarioResources) {
            // Find the JNDI name from the datasource's properties to use for the name and key
            String jndiName = null;
            List<ScenarioProperty> properties = scenarioResource.getProperty();
            for (ScenarioProperty property : properties) {
                if (property.getName().equals(JNDI_NAME_PROPERTY)) {
                    jndiName = property.getValue();
                    break;
                }
            }

            // Create and cache
            DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), jndiName,
                jndiName, "", "", null, null);

            scenarioResourceMap.put(resource.getResourceKey(), resource);

            // Read, convert, and cache resource's configuration
            List<ScenarioProperty> scenarioProperties = scenarioResource.getProperty();
            Configuration configuration = ScenarioLoader.propertiesToConfiguration(scenarioProperties);
            ResourceCache.putConfiguration(resource.getResourceKey(), configuration);

            // Store the availability
            AvailabilityType availabilityType = ScenarioLoader.convertAvailability(scenarioResource.getAvailability());
            ResourceCache.putAvailability(resource.getResourceKey(), availabilityType);

            // Create the cache of metric calculators
            List<ScenarioMetric> scenarioMetrics = scenarioResource.getMetric();
            Map<String, MetricValueCalculator> valueCalculators = ScenarioLoader
                .createMetricValueCalculators(scenarioMetrics);
            ResourceCache.putMetricCalculators(resource.getResourceKey(), valueCalculators);

            // Create the cache of operation handlers
            List<ScenarioOperationResult> scenarioOperations = scenarioResource.getOperationResults();
            Map<String, OperationHandler> operationHandlers = ScenarioLoader
                .createOperationHandlers(scenarioOperations);
            ResourceCache.putOperationHandlers(resource.getResourceKey(), operationHandlers);

            // Create the cache of artifacts
            List<ScenarioArtifact> scenarioArtifacts = scenarioResource.getArtifact();
            //Map<String, ArtifactHolder> artifacts = ScenarioLoader.createArtifacts(scenarioArtifacts);
            //ResourceCache.putArtifacts(resource.getResourceKey(), artifacts);

            // Create the cache of errors
            ConfigurationHandler errorHolder = new ConfigurationHandler();
            errorHolder.addPropertyErrors(scenarioProperties);
            // Check for error message to put into configuration
            ScenarioResourceConfigurationError resourceConfigurationError = scenarioResource.getConfigurationError();
            if (resourceConfigurationError != null) {
                String message = resourceConfigurationError.getMessage();
                if (message != null && !message.equals("")) {
                    errorHolder.setConfigurationError(message);
                }
            }
            ResourceCache.putErrors(resource.getResourceKey(), errorHolder);

        }

        LOG.info("Loaded " + scenarioResourceMap.size() + " resources from the scenario");

        // Load the scenario resources into the cached resource map
        Map<String, DiscoveredResourceDetails> cacheMap = ResourceCache.getResourceMap(context.getResourceType(),
            context.getParentResourceComponent());
        cacheMap.putAll(scenarioResourceMap);
    }
}
