package org.rhq.plugins.mock.jboss;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.mock.jboss.configuration.ConfigurationHandler;
import org.rhq.plugins.mock.jboss.metrics.MetricValueCalculator;
import org.rhq.plugins.mock.jboss.operations.OperationHandler;

/**
 * Plugin component representing any resource in the mock that uses the JNDI property as the resource name and key. For
 * example, both datasources and JMS destinations can be represented as generic resources keyed by their JNDI property
 * name.
 *
 * In order for a service to utilize this class as its component, it must define a property called "jndiName" that will
 * be used. An example of this is the no-tx-datasource service can be found in the rhq-plugin.xml in this module.
 *
 * @author Jason Dobies
 */
public class JndiResourceComponent implements ResourceComponent, ConfigurationFacet, DeleteResourceFacet,
    MeasurementFacet, OperationFacet, CreateChildResourceFacet {
    //Used to implement ContentFacet, but this is commenting that out
    /**
     * Property name used to retrieve the JNDI name, which will act as the resource name and key.
     */
    private static final String JNDI_NAME_PROPERTY = "jndiName";

    private final Log log = LogFactory.getLog(JndiResourceComponent.class);

    private ResourceContext resourceContext;

    // ResourceComponent Implementation  --------------------------------------------

    public void start(ResourceContext context) {
        this.resourceContext = context;
    }

    public void stop() {
        this.resourceContext = null;
    }

    public AvailabilityType getAvailability() {
        return ResourceCache.getAvailability(resourceContext.getResourceKey());
    }

    // ConfigurationFacet Implementation  --------------------------------------------

    public Configuration loadResourceConfiguration() {
        return ResourceCache.getConfiguration(resourceContext.getResourceKey());
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration configuration = report.getConfiguration();

        log.info("Received call to update configuration: " + configuration);

        ConfigurationHandler errorHolder = ResourceCache.getErrors(resourceContext.getResourceKey());

        if (errorHolder != null) {
            errorHolder.handleUpdate(report);
        } else {

            /* Since there are no scenario that has errors for this resource, we just set the
             * status to success to pass the next if statment and to put the configuration update
             * into the ResourceCache.
             */
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        }

        if (report.getStatus().equals(ConfigurationUpdateStatus.SUCCESS)) {
            ResourceCache.putConfiguration(resourceContext.getResourceKey(), configuration.deepCopy());

            // If the jndiName changed, update the resource name and key
            String jndiName = configuration.getSimple(JNDI_NAME_PROPERTY).getStringValue();
            if (!resourceContext.getResourceKey().equals(jndiName)) {
                Map<String, DiscoveredResourceDetails> resourceMap = ResourceCache.getResourceMap(resourceContext
                    .getResourceType(), resourceContext.getParentResourceComponent());

                DiscoveredResourceDetails resource = resourceMap.remove(resourceContext.getResourceKey());
                if (resource != null) {
                    resource.setResourceName(jndiName);
                    resource.setResourceKey(jndiName);
                    resourceMap.put(jndiName, resource);
                }
            }
        }
    }

    // MeasurementFacet Implementation  --------------------------------------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        Map<String, MetricValueCalculator> valueCalculators = ResourceCache.getMetricCalculators(resourceContext
            .getResourceKey());

        if (valueCalculators == null) {
            log.debug("No map of metric calculators found for resource key: " + resourceContext.getResourceKey());
            return;
        }

        for (MeasurementScheduleRequest request : metrics) {
            String metricName = request.getName();
            MetricValueCalculator calculator = valueCalculators.get(metricName);

            if (calculator == null) {
                log.debug("No metric calculator found for resource key: " + resourceContext.getResourceKey()
                    + " metric name: " + metricName);
                continue;
            }

            MeasurementDataNumeric measurementData = (MeasurementDataNumeric) calculator.nextValue(request);
            report.addData(measurementData);
        }

    }

    // OperationFacet Implementation  --------------------------------------------

    public OperationResult invokeOperation(String name, Configuration configuration) throws Exception {
        Map<String, OperationHandler> operationHandlers = ResourceCache.getOperationHandlers(resourceContext
            .getResourceKey());

        if (operationHandlers == null) {
            log.debug("No map of operation handlers found for resource key: " + resourceContext.getResourceKey());
            return null;
        }

        OperationHandler operationHandler = operationHandlers.get(name);

        if (operationHandler == null) {
            log.debug("No operation handler found for resource key: " + resourceContext.getResourceKey()
                + " operation: " + name);
            return null;
        }

        /* The operation handler will react accordingly if the desired result is to throw an exception or time out,
           depending on the way the scenario is configured. */
        return operationHandler.handleOperation(configuration);
    }

    // ResourceFactoryFacet Implementation  --------------------------------------------

    public CreateResourceReport createResource(CreateResourceReport report) {
        // No-op
        return report;
    }

    public void deleteResource() {
        log.debug("Deleting resource: " + resourceContext.getResourceKey());

        Map<String, DiscoveredResourceDetails> resourceMap = ResourceCache.getResourceMap(resourceContext
            .getResourceType(), resourceContext.getParentResourceComponent());
        resourceMap.remove(resourceContext.getResourceKey());
    }

    // ContentFacet Implementation  --------------------------------------------

    /*public Set<InstalledPackageDetail> getInstalledPackages(PackageType type)
    {
       Set<InstalledPackageDetails> results = new HashSet<InstalledPackageDetails>();

       Map<String, ArtifactHolder> allResourceArtifacts = ResourceCache.getArtifacts(resourceContext.getResourceKey());

       if (allResourceArtifacts == null)
          return results;

       String typeName = type.getName();
       for (ArtifactHolder artifact : allResourceArtifacts.values())
       {
          if (typeName.equals(artifact.getType()))
          {
             InstalledPackageDetails details = new InstalledPackageDetails(artifact.getName(), "1", "1", "noarch");
             results.add(details);
          }
       }

       return results;
    }

    public void installPackages(Set<InstalledPackage> packages, ContentServices contentServices)
    {
       for (InstalledPackage installedPackage : packages)
       {
          Map<String, ArtifactHolder> allArtifactsForResource = ResourceCache.getArtifacts(resourceContext.getResourceKey());
    
          // Create the new artifact holder to handle future discoveries against this artifact
          ArtifactHolder holder = new ArtifactHolder(artifactDetails.getArtifactKey(), artifactDetails.getName(),
                                                     artifactDetails.getType().getName(), factory);
    
          // Add holder to cache so the "resource" knows about the artifact
          allArtifactsForResource.put(artifactDetails.getArtifactKey(), holder);
       }

       return;
    }

    public void removePackages(Set<InstalledPackage> packages)
    {
       for (InstalledPackage installedPackage : packages)
       {
          Map<String, ArtifactHolder> allArtifactsForResource = ResourceCache.getArtifacts(resourceContext.getResourceKey());
          allArtifactsForResource.remove(artifactDetails.getArtifactKey());         
       }
    }

    public InputStream retrievePackageBits(InstalledPackage pkg)
    {
       Map<String, ArtifactHolder> allArtifactsForResource = ResourceCache.getArtifacts(resourceContext.getResourceKey());

       ArtifactHolder artifactHolder = allArtifactsForResource.get(artifactDetails.getArtifactKey());

       if (artifactHolder == null)
       {
          log.error("Could not find artifact holder for artifact key: " + artifactDetails.getArtifactKey());
          return null;
       }
    }*/
}
