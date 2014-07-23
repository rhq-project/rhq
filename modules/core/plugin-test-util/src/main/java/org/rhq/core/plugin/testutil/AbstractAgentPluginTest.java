/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.plugin.testutil;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.strategy.AcceptScopesStrategy;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.MeasurementDefinitionFilter;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.maven.MavenArtifactNotFoundException;
import org.rhq.core.util.maven.MavenArtifactProperties;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * The base class for an Agent plugin integration test class.
 *
 * @author Ian Springer
 */
public abstract class AbstractAgentPluginTest extends Arquillian {

    private Log log = LogFactory.getLog(this.getClass());

    @ArquillianResource
    protected MockingServerServices serverServices;

    @ArquillianResource
    protected PluginContainerConfiguration pluginContainerConfiguration;

    @ArquillianResource
    protected PluginContainer pluginContainer;

    private FakeServerInventory serverInventory;

    private FakeServerInventory.CompleteDiscoveryChecker discoveryCompleteChecker;

    @Deployment(name = "platform", order = 1)
    public static RhqAgentPluginArchive getPlatformPlugin() throws Exception {
        MavenResolverSystem mavenDependencyResolver = Maven.resolver();
        String platformPluginArtifact = "org.rhq:rhq-platform-plugin:jar:" + getPlatformPluginVersion();

        return mavenDependencyResolver.offline().loadPomFromFile("pom.xml").resolve(platformPluginArtifact)
            .withoutTransitivity().asSingle(RhqAgentPluginArchive.class);
    }

    @Deployment(name = "pluginUnderTest", order = 2)
    public static RhqAgentPluginArchive getPluginUnderTest() throws Exception {
        // This is the jar that was just built during the Maven package phase, just prior to this test getting run
        // during the Maven integration-test phase. This is exactly what we want, because it's the real Maven-produced
        // jar, freshly assembled from the classes being tested.
        File pluginJarFile = getPluginJarFile();
        System.out.println("Using plugin jar [" + pluginJarFile + "]...");
        MavenResolverSystem mavenDependencyResolver = Maven.resolver();
        // Pull in any required plugins from our pom's dependencies.

        Collection<RhqAgentPluginArchive> requiredPlugins = Arrays.asList(mavenDependencyResolver
            .loadPomFromFile("pom.xml").importRuntimeAndTestDependencies(new AcceptScopesStrategy(ScopeType.PROVIDED))
            .as(RhqAgentPluginArchive.class));

        return ShrinkWrap.create(ZipImporter.class, pluginJarFile.getName()).importFrom(pluginJarFile)
            .as(RhqAgentPluginArchive.class).withRequiredPluginsFrom(requiredPlugins);
    }

    protected static File getPluginJarFile() {
        File targetDir = new File("target").getAbsoluteFile();
        File[] files = targetDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                boolean locatedPlugin = name.endsWith("-" + getRhqVersion() + ".jar");
                if (!locatedPlugin) {//try again to look for -redhat-*.jar
                    if (((name.indexOf(getRhqVersion() + "-redhat-")) > -1) && (name.endsWith("*.jar"))) {
                        locatedPlugin = true;
                    }
                }
                return locatedPlugin;
            }
        });
        return files[0];
    }

    /**
     * Set up our fake server discovery ServerService, which will auto-import all Resources in reports it receives.
     *
     * @throws Exception if an error occurs
     */
    @BeforeDiscovery
    public void resetServerServices() throws Exception {
        System.out.println("\n=== Resetting fake Server prior to running discovery scan...");

        this.serverInventory = new FakeServerInventory();
        System.out.println("\n====== Waiting for discovery to complete...");
        // TODO: Calculate the expected depth by recursively descending the types defined by the plugin.
        this.discoveryCompleteChecker = serverInventory.createAsyncDiscoveryCompletionChecker(getTypeHierarchyDepth());

        try {
            this.serverServices.resetMocks();
            Mockito.when(
                this.serverServices.getDiscoveryServerService()
                    .mergeInventoryReport(Mockito.any(InventoryReport.class))).then(
                serverInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract int getTypeHierarchyDepth();

    @AfterDiscovery
    public void waitForAsyncDiscoveries() throws Exception {
        try {
            discoveryCompleteChecker.waitForDiscoveryComplete(12000);
            System.out.println("\n====== Discovery completed.");
        } catch (InterruptedException e) {
            throw new RuntimeException("Discovery did not complete within 12 seconds.");
        }
        // Wait a while longer to give all Resource components a chance to start.
        // TODO: Do this more intelligently so we don't sleep longer than needed.
        Thread.sleep(10000);
    }
    /**
     * Note - this is stronger than {@link #waitForAsyncDiscoveries()} but can be slower. It waits until the
     * discovered tree size stabilizes, which may take longer than hitting a target tree depth.
     * Tree depth may be sufficient for many simple test hierarchies but for a large scale integration
     * test, requiring full discovery of an AS-7 server (for example), it can reach the target depth well before
     * the entire tree is discovered and populated.
     * </p>
     * This is equivalent to {{waitForAsyncDiscoveryToStabilize(root, 5000L, 8)}}. As such it will wait
     * for at least 40s.
     *
     * @throws Exception
     */
    protected void waitForAsyncDiscoveryToStabilize(Resource root) throws Exception {
        waitForAsyncDiscoveryToStabilize(root, 5000L, 8);
    }

    /**
     * @param root
     * @param checkInterval how long between checks of the tree size
     * @param stableCount how many checks must be the same before we're convinced we're stable
     * @throws Exception
     */
    protected void waitForAsyncDiscoveryToStabilize(Resource root, long checkInterval, int stableCount)
        throws Exception {
        int startResCount = 0;
        int endResCount = getResCount(root);
        int numStableChecks = 0;
        log.info("waitForAsyncDiscoveryToStabilize: ResourceCount Start=" + endResCount);
        do {
            startResCount = endResCount;
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                //
            }
            endResCount = getResCount(root);

            if (startResCount == endResCount) {
                ++numStableChecks;
            } else {
                numStableChecks = 0;
            }
        } while (startResCount < endResCount || numStableChecks < stableCount);
        log.info("waitForAsyncDiscoveryToStabilize: ResourceCount Stable at=" + endResCount);
    }

    static protected int getResCount(Resource resource) {
        int size = 1;
        Set<Resource> children = resource.getChildResources();
        if (null != children && !children.isEmpty()) {
            HashSet<Resource> safeChildren = new HashSet<Resource>(children);
            for (Resource r : safeChildren) {
                size += getResCount(r);
            }
        }
        return size;
    }
    /**
     * Get availability for a Resource synchronously, with a 5 second timeout.
     *
     * @param resource the Resource
     *
     * @return the report containing the collected data
     */
    @NotNull
    protected AvailabilityType getAvailability(Resource resource) throws PluginContainerException {
        ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(resource);
        long timeoutMillis = 5000;
        AvailabilityFacet availFacet = resourceContainer.createResourceComponentProxy(AvailabilityFacet.class,
            FacetLockType.READ, timeoutMillis, false, false, false);
        AvailabilityType avail;
        try {
            avail = availFacet.getAvailability();
        } catch (Exception e) {
            System.out.println("====== Error occurred during availability check on " + resource + ": " + e);
            throw new RuntimeException("Error occurred during availability check on " + resource + ": " + e);
        }
        return avail;
    }

    /**
     * Execute an operation on a Resource synchronously, with the same timeout that the PC would use.
     *
     * @param resource the Resource
     * @param operationName the name of the operation
     * @param params parameters to pass to the operation; may be null if the operation does not define any parameters
     *
     * @return the result of the operation
     */
    @NotNull
    protected OperationResult invokeOperation(Resource resource, String operationName, @Nullable
    Configuration params) throws PluginContainerException {
        ResourceType resourceType = resource.getResourceType();
        OperationDefinition operationDefinition = ResourceTypeUtility.getOperationDefinition(resourceType,
            operationName);
        assertNotNull(operationDefinition, "No operation named [" + operationName + "] is defined for ResourceType {"
            + resourceType.getPlugin() + "}" + resourceType.getName() + ".");

        long timeout = getDefaultTimeout(resource.getResourceType(), operationName);
        System.out.println("=== Invoking operation [" + operationName + "] with parameters ["
            + ((params != null) ? params.toString(true) : params) + "] on " + resource + "...");
        ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(resource);
        long timeoutMillis = timeout * 1000;
        OperationFacet operationFacet = resourceContainer.createResourceComponentProxy(OperationFacet.class,
            FacetLockType.WRITE, timeoutMillis, false, false, false);
        OperationResult operationResult;
        try {
            operationResult = operationFacet.invokeOperation(operationName, params);
        } catch (Exception e) {
            String paramsString = (params != null) ? params.toString(true) : String.valueOf(params);
            System.out.println("====== Error occurred during invocation of operation [" + operationName
                + "] with parameters [" + paramsString + "] on " + resource + ": " + e);
            e.printStackTrace(System.out);
            throw new RuntimeException("Error occurred during invocation of operation [" + operationName
                + "] with parameters [" + paramsString + "] on " + resource + ".", e);
        }
        return operationResult;
    }

    @NotNull
    protected Configuration loadResourceConfiguration(Resource resource) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        ConfigurationDefinition resourceConfigDef = resourceType.getResourceConfigurationDefinition();
        assertNotNull(resourceConfigDef, "No resource config is defined for ResourceType " + resourceType + ".");
        System.out.println("=== Loading Resource config for " + resource + "...");
        ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(resource);
        long timeoutMillis = 5000;
        ConfigurationFacet configurationFacet = resourceContainer.createResourceComponentProxy(
            ConfigurationFacet.class, FacetLockType.READ, timeoutMillis, false, false, false);
        return configurationFacet.loadResourceConfiguration();
    }

    @NotNull
    protected ConfigurationUpdateReport updateResourceConfiguration(Resource resource, Configuration resourceConfig)
        throws Exception {
        ResourceType resourceType = resource.getResourceType();
        ConfigurationDefinition resourceConfigDef = resourceType.getResourceConfigurationDefinition();
        assertNotNull(resourceConfigDef, "No resource config is defined for ResourceType " + resourceType + ".");
        System.out.println("=== Updating Resource config for " + resource + "...");
        ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(resource);
        long timeoutMillis = 5000;
        ConfigurationFacet configurationFacet = resourceContainer.createResourceComponentProxy(
            ConfigurationFacet.class, FacetLockType.WRITE, timeoutMillis, false, false, false);
        ConfigurationUpdateReport report = new ConfigurationUpdateReport(resourceConfig);
        configurationFacet.updateResourceConfiguration(report);
        return report;
    }

    protected void assertAllResourceComponentsStarted() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Map<ResourceType, ResourceContainer> nonStartedResourceContainersByType = new LinkedHashMap<ResourceType, ResourceContainer>();
        findNonStartedResourceComponentsRecursively(platform, nonStartedResourceContainersByType);
        assertTrue(nonStartedResourceContainersByType.isEmpty(),
            "Resource containers with non-started Resource components by type: " + nonStartedResourceContainersByType);
    }

    private void findNonStartedResourceComponentsRecursively(Resource resource,
        Map<ResourceType, ResourceContainer> nonStartedResourceContainersByType) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        if (!nonStartedResourceContainersByType.containsKey(resourceType)) {
            ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(
                resource);
            if (resourceContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
                nonStartedResourceContainersByType.put(resourceType, resourceContainer);
            } else if (resourceContainer.getResourceComponent() == null) {
                System.err.println("****** Resource container " + resourceContainer
                    + " says its Resource component is started, but the component is null. ******");
                nonStartedResourceContainersByType.put(resourceType, resourceContainer);
            }
        }

        // Recurse.
        for (Resource childResource : resource.getChildResources()) {
            findNonStartedResourceComponentsRecursively(childResource, nonStartedResourceContainersByType);
        }
    }

    protected void assertAllResourceConfigsLoad() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Map<ResourceType, Exception> resourceConfigLoadExceptionsByType = new LinkedHashMap<ResourceType, Exception>();
        findResourceConfigsThatFailToLoadRecursively(platform, resourceConfigLoadExceptionsByType);
        assertTrue(resourceConfigLoadExceptionsByType.isEmpty(), "Resource configs that failed to load by type: "
            + resourceConfigLoadExceptionsByType);
    }

    private void findResourceConfigsThatFailToLoadRecursively(Resource resource,
        Map<ResourceType, Exception> resourceConfigLoadExceptionsByType) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        // Only check resource configs on types of Resources from the plugin under test.
        if (resourceType.getPlugin().equals(getPluginName())
            && (resourceType.getResourceConfigurationDefinition() != null)
            && !resourceConfigLoadExceptionsByType.containsKey(resourceType)) {
            ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(
                resource);
            if (resourceContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
                return;
            }

            Exception exception = null;
            try {
                Configuration resourceConfig = loadResourceConfiguration(resource);
                List<String> validationErrors = ConfigurationUtility.validateConfiguration(resourceConfig,
                    resourceType.getResourceConfigurationDefinition());
                if (!validationErrors.isEmpty()) {
                    exception = new Exception("Resource config is not valid: " + validationErrors.toString());
                }
            } catch (Exception e) {
                exception = e;
            }
            if (exception != null) {
                resourceConfigLoadExceptionsByType.put(resourceType, exception);
            }
        }

        // Recurse.
        for (Resource childResource : resource.getChildResources()) {
            findResourceConfigsThatFailToLoadRecursively(childResource, resourceConfigLoadExceptionsByType);
        }
    }

    protected void assertAllNumericMetricsAndTraitsHaveNonNullValues(
        Map<ResourceType, String[]> excludedMetricNamesByType) throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        LinkedHashMap<ResourceType, Set<String>> metricsWithNullValuesByType = new LinkedHashMap<ResourceType, Set<String>>();
        findNumericMetricsAndTraitsWithNullValuesRecursively(platform, metricsWithNullValuesByType);
        removeExcludedMetricNames(metricsWithNullValuesByType, excludedMetricNamesByType);
        assertTrue(metricsWithNullValuesByType.isEmpty(), "Metrics with null values by type: "
            + metricsWithNullValuesByType);
    }

    private void removeExcludedMetricNames(LinkedHashMap<ResourceType, Set<String>> metricsWithNullValuesByType,
        Map<ResourceType, String[]> excludedMetricNamesByType) {
        for (Iterator<ResourceType> mapIterator = metricsWithNullValuesByType.keySet().iterator(); mapIterator
            .hasNext();) {
            ResourceType resourceType = mapIterator.next();
            if (excludedMetricNamesByType.get(resourceType) == null) {
                continue;
            }

            Set<String> namesOfMetricsWithNullValues = metricsWithNullValuesByType.get(resourceType);
            List<String> excludedMetricNames = Arrays.asList(excludedMetricNamesByType.get(resourceType));
            for (Iterator<String> setIterator = namesOfMetricsWithNullValues.iterator(); setIterator.hasNext();) {
                String nameOfMetricWithNullValue = setIterator.next();
                if (excludedMetricNames.contains(nameOfMetricWithNullValue)) {
                    setIterator.remove();
                }
            }
            if (namesOfMetricsWithNullValues.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private void findNumericMetricsAndTraitsWithNullValuesRecursively(Resource resource,
        Map<ResourceType, Set<String>> metricsWithNullValuesByType) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        // Only check metrics on types of Resources from the plugin under test.
        if (resourceType.getPlugin().equals(getPluginName())) {
            ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(
                resource);
            if (resourceContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
                return;
            }

            Set<String> metricsWithNullValues = getNumericMetricsAndTraitsWithNullValues(resource);
            if (!metricsWithNullValues.isEmpty()) {
                Set<String> metricsWithNullValuesForType = metricsWithNullValuesByType.get(resourceType);
                if (metricsWithNullValuesForType != null) {
                    metricsWithNullValuesForType.addAll(metricsWithNullValues);
                } else {
                    metricsWithNullValuesByType.put(resourceType, metricsWithNullValues);
                }
            }
        }

        // Recurse.
        for (Resource childResource : resource.getChildResources()) {
            findNumericMetricsAndTraitsWithNullValuesRecursively(childResource, metricsWithNullValuesByType);
        }
    }

    protected Set<String> getNumericMetricsAndTraitsWithNullValues(Resource resource) throws Exception {
        ResourceType type = resource.getResourceType();
        Set<MeasurementDefinition> numericMetricAndTraitDefs = ResourceTypeUtility.getMeasurementDefinitions(type,
            new MeasurementDefinitionFilter() {
                private final Set<DataType> acceptableDataTypes = EnumSet.of(DataType.MEASUREMENT, DataType.TRAIT);

                public boolean accept(MeasurementDefinition metricDef) {
                    return acceptableDataTypes.contains(metricDef.getDataType());
                }
            });
        Set<String> metricsWithNullValues = getMetricsWithNullValues(resource, numericMetricAndTraitDefs);
        return metricsWithNullValues;
    }

    protected Set<String> getMetricsWithNullValues(Resource resource, Set<MeasurementDefinition> metricDefs)
        throws Exception {
        Set<String> metricsWithNullValues = new TreeSet<String>();
        for (MeasurementDefinition metricDef : metricDefs) {
            if (!metricDef.getResourceType().equals(resource.getResourceType())) {
                throw new IllegalArgumentException(metricDef + " is not defined by " + resource.getResourceType());
            }
            Object value;
            switch (metricDef.getDataType()) {
            case MEASUREMENT:
                value = collectNumericMetric(resource, metricDef.getName());
                break;
            case TRAIT:
                value = collectTrait(resource, metricDef.getName());
                break;
            default:
                throw new IllegalArgumentException("Unsupported metric type: " + metricDef.getDataType());
            }
            if (value == null) {
                metricsWithNullValues.add(metricDef.getName());
            }
        }
        return metricsWithNullValues;
    }

    @Nullable
    protected Double collectNumericMetric(Resource resource, String metricName) throws Exception {
        System.out.println("=== Collecting numeric metric [" + metricName + "] for " + resource + "...");
        MeasurementReport report = collectMetric(resource, metricName);

        Double value;
        if (report.getNumericData().isEmpty()) {
            assertEquals(
                report.getTraitData().size(),
                0,
                "Metric [" + metricName + "] for Resource type " + resource.getResourceType()
                    + " is defined as a numeric metric, but the plugin returned one or more traits!: "
                    + report.getTraitData());
            assertEquals(report.getCallTimeData().size(), 0,
                "Metric [" + metricName + "] for Resource type " + resource.getResourceType()
                    + " is defined as a numeric metric, but the plugin returned one or more call-time metrics!: "
                    + report.getCallTimeData());
            value = null;
        } else {
            assertEquals(report.getNumericData().size(), 1,
                "Requested a single metric, but plugin returned more than one datum: " + report.getNumericData());
            MeasurementDataNumeric datum = report.getNumericData().iterator().next();
            assertEquals(datum.getName(), metricName, "Numeric metric [" + metricName + "] for Resource type "
                + resource.getResourceType() + " was requested, but the plugin returned a numeric metric with name ["
                + datum.getName() + "] and value [" + datum.getValue() + "]!");
            // Normalize NaN or infinite to null, as the PC does.
            value = (datum.getValue().isNaN() || datum.getValue().isInfinite()) ? null : datum.getValue();
        }
        System.out.println("====== Collected numeric metric [" + metricName + "] with value of [" + value + "] for "
            + resource + ".");

        return value;
    }

    @Nullable
    protected String collectTrait(Resource resource, String traitName) throws Exception {
        System.out.println("=== Collecting trait [" + traitName + "] for " + resource + "...");
        MeasurementReport report = collectMetric(resource, traitName);

        String value;
        if (report.getTraitData().isEmpty()) {
            assertEquals(
                report.getNumericData().size(),
                0,
                "Metric [" + traitName + "] for Resource type " + resource.getResourceType()
                    + " is defined as a trait, but the plugin returned one or more numeric metrics!: "
                    + report.getNumericData());
            assertEquals(
                report.getCallTimeData().size(),
                0,
                "Metric [" + traitName + "] for Resource type " + resource.getResourceType()
                    + " is defined as a trait, but the plugin returned one or more call-time metrics!: "
                    + report.getCallTimeData());
            value = null;
        } else {
            assertEquals(report.getTraitData().size(), 1,
                "Requested a single trait, but plugin returned more than one datum: " + report.getTraitData());
            MeasurementDataTrait datum = report.getTraitData().iterator().next();
            assertEquals(datum.getName(), traitName,
                "Trait [" + traitName + "] for Resource type " + resource.getResourceType()
                    + " was requested, but the plugin returned a trait with name [" + datum.getName() + "] and value ["
                    + datum.getValue() + "]!");
            value = datum.getValue();
        }
        System.out.println("====== Collected trait [" + traitName + "] with value of [" + value + "] for " + resource
            + ".");

        return value;
    }

    /**
     * Collect a metric for a Resource synchronously, with a 7 second timeout.
     *
     * @param resource the Resource
     * @param metricName the name of the metric
     *
     * @return the report containing the collected data
     */
    @NotNull
    private MeasurementReport collectMetric(Resource resource, String metricName) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        MeasurementDefinition measurementDefinition = ResourceTypeUtility.getMeasurementDefinition(resourceType,
            metricName);
        assertNotNull(measurementDefinition, "No metric named [" + metricName + "] is defined for ResourceType {"
            + resourceType.getPlugin() + "}" + resourceType.getName() + ".");

        ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(resource);
        long timeoutMillis = 5000;
        if (resourceContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
            throw new IllegalStateException("Resource component for " + resource + " has not yet been started.");
        }
        MeasurementFacet measurementFacet = resourceContainer.createResourceComponentProxy(MeasurementFacet.class,
            FacetLockType.READ, timeoutMillis, false, false, false);
        MeasurementReport report = new MeasurementReport();
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(-1, metricName, -1, true,
            measurementDefinition.getDataType(), measurementDefinition.getRawNumericType());
        Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>();
        requests.add(request);
        try {
            measurementFacet.getValues(report, requests);
        } catch (Exception e) {
            System.out.println("====== Error occurred during collection of metric [" + metricName + "] on " + resource
                + ": " + e);
            throw new RuntimeException("Error occurred during collection of metric [" + metricName + "] on " + resource
                + ": " + e);
        }
        return report;
    }

    protected OperationResult invokeOperationAndAssertSuccess(Resource resource, String operationName, @Nullable
    Configuration params) throws PluginContainerException {
        OperationResult result = invokeOperation(resource, operationName, params);
        assertOperationSucceeded(operationName, params, result);
        return result;
    }

    private long getDefaultTimeout(ResourceType resourceType, String operationName) {
        OperationDefinition operationDefinition = ResourceTypeUtility.getOperationDefinition(resourceType,
            operationName);
        // Note: The PC's default timeout is 10 minutes.
        return (operationDefinition.getTimeout() != null) ? operationDefinition.getTimeout()
            : this.pluginContainerConfiguration.getOperationInvocationTimeout();
    }

    private static String getRhqVersion() {
        MavenArtifactProperties rhqPluginContainerPom = null;
        try {
            rhqPluginContainerPom = MavenArtifactProperties.getInstance("org.rhq", "rhq-parent");
        } catch (MavenArtifactNotFoundException e) {
            throw new RuntimeException(e);
        }
        return rhqPluginContainerPom.getVersion();
    }

    private static String getPlatformPluginVersion() {
        MavenArtifactProperties rhqPluginContainerPom = null;
        try {
            rhqPluginContainerPom = MavenArtifactProperties.getInstance("org.rhq", "rhq-platform-plugin");
        } catch (MavenArtifactNotFoundException e) {
            throw new RuntimeException(e);
        }
        return rhqPluginContainerPom.getVersion();
    }

    protected void assertOperationSucceeded(String operationName, Configuration params, OperationResult result) {
        String paramsString = (params != null) ? params.toString(true) : String.valueOf(params);
        assertNull(result.getErrorMessage(), "Operation [" + operationName + "] with parameters " + paramsString
            + " returned an error: " + result.getErrorMessage());
    }

    protected abstract String getPluginName();

    protected FakeServerInventory getServerInventory() {
        return serverInventory;
    }

    /**
     * Test that loads a resource configuration and then immediately updates the resource
     * with the exact same loaded settings.
     *
     * Notes:
     * 1) load/update is not executed on the root resource provided.
     * 2) if a resource is ignored then all of subresource of that resources are ignored
     *
     * @param rootResource root resource
     * @param ignoredResources resources to be ignored
     * @return number of errors
     * @throws InterruptedException
     * @throws PluginContainerException
     */
    protected int loadUpdateConfigChildResources(Resource rootResource, List<String> ignoredResources)
        throws InterruptedException, PluginContainerException {

        ignoredResources = (ignoredResources == null) ? new ArrayList<String>() : ignoredResources;

        ConfigurationManager configManager = this.pluginContainer.getConfigurationManager();
        configManager.initialize();
        Thread.sleep(10 * 1000L);

        Queue<Resource> unparsedResources = new LinkedList<Resource>();
        addCommitedChildrenToCollection(unparsedResources, rootResource, ignoredResources);

        int errorCount = 0;

        while (!unparsedResources.isEmpty()) {
            Resource resourceUnderTest = unparsedResources.poll();

            addCommitedChildrenToCollection(unparsedResources, resourceUnderTest, ignoredResources);

            if (resourceUnderTest.getResourceType().getResourceConfigurationDefinition() != null) {
                Configuration configUnderTest = configManager.loadResourceConfiguration(resourceUnderTest.getId());

                ConfigurationUpdateRequest updateRequest = new ConfigurationUpdateRequest(1, configUnderTest,
                    resourceUnderTest.getId());
                ConfigurationUpdateResponse updateResponse = configManager
                    .executeUpdateResourceConfigurationImmediately(updateRequest);

                if (updateResponse == null) {
                    errorCount++;
                    log.error("------------------------------");
                    log.error(resourceUnderTest);
                    log.error("Update Response is NULL!!!!");
                    log.error("------------------------------\n");
                }
                if (updateResponse.getErrorMessage() != null) {
                    errorCount++;
                    log.error("------------------------------");
                    log.error(resourceUnderTest);
                    log.error(updateResponse.getErrorMessage());
                    log.error("------------------------------\n");
                }
            }
        }

        return errorCount;
    }

    /**
     * Test that executes all the no arg operations for all the subresources of a provided resource.
     * Notes:
     * 1) no operations are executed on the root resource provided.
     * 2) if a resource is ignored then all of subresource of that resources are ignored
     *
     * @param rootResource root resource
     * @param ignoredResources resources to be ignored
     * @param ignoredOperations operations to be ignored
     * @throws PluginContainerException
     */
    protected void executeNoArgOperations(Resource rootResource, List<String> ignoredResources,
        List<String> ignoredOperations) throws PluginContainerException {

        ignoredResources = (ignoredResources == null) ? new ArrayList<String>() : ignoredResources;
        ignoredOperations = (ignoredOperations == null) ? new ArrayList<String>() : ignoredOperations;

        Queue<Resource> unparsedResources = new LinkedList<Resource>();
        addCommitedChildrenToCollection(unparsedResources, rootResource, ignoredResources);

        while (!unparsedResources.isEmpty()) {
            Resource resourceUnderTest = unparsedResources.poll();

            addCommitedChildrenToCollection(unparsedResources, resourceUnderTest, ignoredResources);

            for (OperationDefinition operationUnderTest : resourceUnderTest.getResourceType().getOperationDefinitions()) {
                if (!ignoredOperations.contains(operationUnderTest.getName())) {
                    if (operationUnderTest.getParametersConfigurationDefinition() == null
                        || operationUnderTest.getParametersConfigurationDefinition().getPropertyDefinitions().isEmpty()) {
                        this.invokeOperationAndAssertSuccess(resourceUnderTest, operationUnderTest.getName(),
                            new Configuration());
                    }
                }
            }
        }
    }

    /**
     * Adds direct subresources of resources to a collection.
     *
     * @param accumulatorCollection accumulator collection
     * @param rootResource root resource
     * @param ignoredResources resources to be ignored
     */
    private void addCommitedChildrenToCollection(Collection<Resource> accumulatorCollection, Resource rootResource,
        List<String> ignoredResources) {
        for (Resource childResource : rootResource.getChildResources()) {
            if (childResource.getInventoryStatus().equals(InventoryStatus.COMMITTED)) {
                if (!ignoredResources.contains(childResource.getResourceType().getName())) {
                    accumulatorCollection.add(childResource);
                }
            } else {
                log.info("Resource NOT COMMITTED --> not added to collection!! - " + childResource + " - "
                    + childResource.getInventoryStatus());
            }
        }
    }

}
