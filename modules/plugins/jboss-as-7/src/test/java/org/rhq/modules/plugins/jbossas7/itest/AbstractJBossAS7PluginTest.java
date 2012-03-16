/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.modules.plugins.jbossas7.itest;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.Assert;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.maven.MavenArtifactNotFoundException;
import org.rhq.core.util.maven.MavenArtifactProperties;
import org.rhq.modules.plugins.jbossas7.itest.domain.DomainServerComponentTest;
import org.rhq.modules.plugins.jbossas7.itest.standalone.StandaloneServerComponentTest;
import org.rhq.modules.plugins.jbossas7.util.ResourceTypeUtility;
import org.rhq.modules.plugins.jbossas7.util.ResourceUtility;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * The base class for all jboss-as-7 plugin integration tests.
 *
 * @author Ian Springer
 */
public abstract class AbstractJBossAS7PluginTest extends Arquillian {

    protected static final String PLUGIN_NAME = "jboss-as-7";

    private static final String MANAGEMENT_USERNAME = "test";
    private static final String MANAGEMENT_PASSWORD = "test";

    private static boolean createdManagementUsers;

    @ArquillianResource
    protected MockingServerServices serverServices;

    @ArquillianResource
    protected PluginContainerConfiguration pluginContainerConfiguration;

    @ArquillianResource
    protected PluginContainer pluginContainer;

    @ArquillianResource
    protected Deployer pluginDeployer;

    protected FakeServerInventory fakeServerInventory = new FakeServerInventory();

    @Deployment(name = "platform", order = 1)
    public static RhqAgentPluginArchive getPlatformPlugin() throws Exception {
        MavenDependencyResolver mavenDependencyResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        String platformPluginArtifact = "org.rhq:rhq-platform-plugin:jar:" + getRhqVersion();
        Collection<RhqAgentPluginArchive> plugins = mavenDependencyResolver.loadEffectivePom("pom.xml")
            .artifact(platformPluginArtifact).resolveAs(RhqAgentPluginArchive.class);
        return plugins.iterator().next();
    }

    @Deployment(name = "jboss-as-7", order = 2)
    public static RhqAgentPluginArchive getJBossAS7Plugin() throws Exception {
        // This is the jar that was just built during the Maven package phase, just prior to this test getting run
        // during the Maven integration-test phase. This is exactly what we want, because it's the real Maven-produced
        // jar, freshly assembled from the classes being tested.
        File pluginJarFile = new File("target/rhq-jboss-as-7-plugin-" + getRhqVersion() + ".jar");
        MavenDependencyResolver mavenDependencyResolver = DependencyResolvers.use(MavenDependencyResolver.class);
        // Pull in any required plugins from our pom's dependencies.
        Collection<RhqAgentPluginArchive> requiredPlugins = mavenDependencyResolver.loadEffectivePom("pom.xml")
            .importAllDependencies().resolveAs(RhqAgentPluginArchive.class);
        return ShrinkWrap.create(ZipImporter.class, pluginJarFile.getName()).importFrom(pluginJarFile)
            .as(RhqAgentPluginArchive.class).withRequiredPluginsFrom(requiredPlugins);
    }

    /**
     * Set up our fake server discovery ServerService, which will auto-import all Resources in reports it receives.
     *
     * @throws Exception if an error occurs
     */
    @BeforeDiscovery
    public void resetServerServices() throws Exception {
        try {
            this.serverServices.resetMocks();
            when(this.serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
                    this.fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Make sure a management user is configured on both discovered AS7 test servers.     
     */
    @AfterDiscovery
    public void installManagementUsers() throws PluginContainerException {
        if (!createdManagementUsers) {
            Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
            Resource domainServer = ResourceUtility.getChildResource(platform,
                    DomainServerComponentTest.RESOURCE_TYPE, DomainServerComponentTest.RESOURCE_KEY);
            if (domainServer != null) {
                installManagementUser(domainServer);
            }
            Resource standaloneServer = ResourceUtility.getChildResource(platform,
                    StandaloneServerComponentTest.RESOURCE_TYPE, StandaloneServerComponentTest.RESOURCE_KEY);
            if (standaloneServer != null) {
                installManagementUser(standaloneServer);
            }            
            createdManagementUsers = true;
        }
    }

    /**
     * Execute an operation on a Resource synchronously.
     * 
     * @param resource the Resource
     * @param operationName the name of the operation
     * @param params parameters to pass to the operation; may be null if the operation does not define any parameters
     *
     * @return the result of the operation
     */
    @NotNull
    protected OperationResult invokeOperation(Resource resource, String operationName, @Nullable Configuration params) 
            throws PluginContainerException {
        ResourceType resourceType = resource.getResourceType();
        OperationDefinition operationDefinition = ResourceTypeUtility.getOperationDefinition(resourceType,
                operationName);
        Assert.assertNotNull(operationDefinition, "No operation named [" + operationName
                + "] is defined for ResourceType {" + resourceType.getPlugin() + "}" + resourceType.getName() + ".");

        long timeout = getDefaultTimeout(resource.getResourceType(), operationName);        
        System.out.println("=== Invoking operation [" + operationName + "] with parameters ["
                + ((params != null) ? params.toString(true) : params) + "] on " + resource + "...");
        ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(resource);
        long timeoutMillis = timeout * 1000;
        OperationFacet operationFacet = resourceContainer.createResourceComponentProxy(OperationFacet.class, 
                FacetLockType.WRITE, timeoutMillis, false, false);
        OperationResult operationResult;
        try {
            operationResult = operationFacet.invokeOperation(operationName, params);
        } catch (Exception e) {
            String paramsString = (params != null) ? params.toString(true) : String.valueOf(params);
            System.out.println("====== Error occurred during invocation of operation [" + operationName
                                + "] with parameters [" + paramsString + "] on " + resource + ": " + e);
            throw new RuntimeException("Error occurred during invocation of operation [" + operationName
                    + "] with parameters [" + paramsString + "] on " + resource + ": " + e);
        }
        return operationResult;
    }

    protected double collectNumericMetricAndAssertNotNull(Resource resource, String metricName)
            throws PluginContainerException {
        Double value = collectNumericMetric(resource, metricName);
        Assert.assertNotNull(value, "Collected null value for numeric metric [" + metricName + "] for " + resource + "");
        return value;
    }

    protected String collectTraitAndAssertNotNull(Resource resource, String traitName) throws PluginContainerException {
        String value = collectTrait(resource, traitName);
        Assert.assertNotNull(value, "Collected null value for trait [" + traitName + "] for " + resource + ".");
        return value;
    }
    
    @Nullable
    protected Double collectNumericMetric(Resource resource, String metricName) throws PluginContainerException {
        System.out.println("=== Collecting numeric metric [" + metricName + "] for " + resource + "...");
        MeasurementReport report = collectMetric(resource, metricName);
        if (report.getNumericData().isEmpty()) {
            return null;
        }
        Assert.assertEquals(report.getNumericData().size(), 1,
                "Requested a single metric but plugin returned more than one datum: " + report.getNumericData());
        MeasurementDataNumeric datum = report.getNumericData().iterator().next();
        // Normalize NaN or infinite to null, as the PC does.
        Double value = (datum.getValue().isNaN() || datum.getValue().isInfinite()) ? null : datum.getValue();
        System.out.println("====== Collected numeric metric [" + metricName + "] with value of [" + value + "] for "
                + resource + ".");
        return value;
    }

    @Nullable
    protected String collectTrait(Resource resource, String traitName) throws PluginContainerException {
        System.out.println("=== Collecting trait [" + traitName + "] for " + resource + "...");
        MeasurementReport report = collectMetric(resource, traitName);
        if (report.getTraitData().isEmpty()) {
            return null;
        }
        Assert.assertEquals(report.getTraitData().size(), 1,
                "Requested a single trait but plugin returned more than one datum: " + report.getTraitData());
        MeasurementDataTrait datum = report.getTraitData().iterator().next();
        String value = datum.getValue();
        System.out.println("====== Collected trait [" + traitName + "] with value of [" + value + "] for "
                + resource + ".");
        return value;
    }

    /**
     * Collect a metric for a Resource synchronously.
     *
     * @param resource the Resource
     * @param metricName the name of the metric                 
     *
     * @return the report containing the collected data
     */
    @NotNull
    private MeasurementReport collectMetric(Resource resource, String metricName)
            throws PluginContainerException {
        ResourceType resourceType = resource.getResourceType();
        MeasurementDefinition measurementDefinition = ResourceTypeUtility.getMeasurementDefinition(resourceType,
                metricName);
        Assert.assertNotNull(measurementDefinition, "No metric named [" + metricName
                + "] is defined for ResourceType {" + resourceType.getPlugin() + "}" + resourceType.getName() + ".");
               
        ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(resource);
        int timeoutMillis = 5000;
        MeasurementFacet measurementFacet = resourceContainer.createResourceComponentProxy(MeasurementFacet.class,
                FacetLockType.READ, timeoutMillis, false, false);
        MeasurementReport report = new MeasurementReport();
        MeasurementScheduleRequest request = new MeasurementScheduleRequest(-1, metricName, -1, true,
                measurementDefinition.getDataType(), measurementDefinition.getRawNumericType());
        Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>();
        requests.add(request);
        try {
            measurementFacet.getValues(report, requests);
        } catch (Exception e) {
            System.out.println("====== Error occurred during collection of metric [" + metricName
                                + "] on " + resource + ": " + e);
            throw new RuntimeException("Error occurred during collection of metric [" + metricName
                    + "] on " + resource + ": " + e);
        }        
        return report;
    }

    protected void invokeOperationAndAssertSuccess(Resource resource, String operationName, @Nullable Configuration params)
            throws PluginContainerException {
        OperationResult result = invokeOperation(resource, operationName, params);
        assertOperationSucceeded(operationName, params, result);
    }    

    private long getDefaultTimeout(ResourceType resourceType, String operationName) {
        OperationDefinition operationDefinition = ResourceTypeUtility.getOperationDefinition(resourceType,
                operationName);
        return (long) ((operationDefinition.getTimeout() != null) ? operationDefinition.getTimeout() : 
                        this.pluginContainerConfiguration.getOperationInvocationTimeout());
    }

    private void installManagementUser(Resource resource) throws PluginContainerException {
        System.out.println("Installing management user [" + MANAGEMENT_USERNAME + "] for " + resource + "...");

        // Invoke the "installRhqUser" operation on the ResourceComponent - this will update the mgmt-users.properties
        // file in the AS7 server's configuration directory.
        Configuration params = new Configuration();
        params.put(new PropertySimple("user", MANAGEMENT_USERNAME));
        params.put(new PropertySimple("password", MANAGEMENT_PASSWORD));

        String operationName = "installRhqUser";
        OperationResult result = invokeOperation(resource, operationName, params);
        System.out.println("Installed management user [" + MANAGEMENT_USERNAME + "] for " + resource + ".");
        assertOperationSucceeded(operationName, params, result);

        // Update the username and password in the plugin config.
        Configuration pluginConfig = resource.getPluginConfiguration();
        pluginConfig.getSimple("user").setStringValue(MANAGEMENT_USERNAME);
        pluginConfig.getSimple("password").setStringValue(MANAGEMENT_PASSWORD);

        // Restart the ResourceComponent, so it will start using the new plugin config.
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        inventoryManager.deactivateResource(resource);
        ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resource);
        Assert.assertNotNull(resourceContainer, "No ResourceContainer exists for " + resource + ".");
        try {
            inventoryManager.activateResource(resource, resourceContainer, true);
        } catch (PluginContainerException e) {
            throw new RuntimeException("Failed to activate ResourceComponent for " + resource + ".", e);
        }
    }

    private static String getRhqVersion() throws MavenArtifactNotFoundException {
        MavenArtifactProperties rhqPluginContainerPom = MavenArtifactProperties.getInstance("org.rhq",
            "rhq-core-plugin-container");
        return rhqPluginContainerPom.getVersion();
    }

    protected void assertOperationSucceeded(String operationName, Configuration params, OperationResult result) {
        String paramsString = (params != null) ? params.toString(true) : String.valueOf(params);
        Assert.assertNull(result.getErrorMessage(), "Operation [" + operationName + "] with parameters " 
                + paramsString + " returned an error: " + result.getErrorMessage());
    }

}
