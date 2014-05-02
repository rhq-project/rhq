 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas.test;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.InventoryPrinter;

/**
 * @author Greg Hinkle
 */
@Test(groups = "jboss.plugin")
public class JBossASPluginTest {
    private Log log = LogFactory.getLog(JBossASPluginTest.class);

    private static final String SERVER_AS = "JBoss Application Server 4.0.5.GA";
    private static final String PLUGIN_NAME = "JBossAS";

    private MockConfigurationServerService configurationCallback = new MockConfigurationServerService();

    @BeforeSuite
    public void start() {
        try {
            File pluginDir = new File("target/itest/plugins");
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);
            pcConfig.setInsideAgent(false);

            PluginContainer.getInstance().setConfiguration(pcConfig);
            PluginContainer.getInstance().initialize();
            System.out.println("Plugin container started with the following plugins:");
            Set<String> pluginNames = PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames();
            for (String pluginName : pluginNames) {
                System.out.println("\t* " + pluginName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterSuite
    public void stop() {
        PluginContainer.getInstance().shutdown();
    }

    @Test
    public void testPluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));
    }

    @Test(dependsOnMethods = "testPluginLoad")
    public void testServerDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        Set<Resource> servers = platform.getChildResources();

        // assert servers.size() != 0; Don't require a running app server for testing at this point
        System.out.println("Found " + servers.size() + " servers");
    }

    @Test(dependsOnMethods = "testServerDiscovery")
    public void testServiceDiscovery() throws Exception {
        try {
            InventoryReport report = PluginContainer.getInstance().getInventoryManager()
                .executeServiceScanImmediately();
            Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

            /*System.out.println("RUNTIME SERVERS: " + platform.getChildResources().size());
             * for (Resource server : platform.getChildResources()) { System.out.println("Server: " +
             * server.toString()); System.out.println("Found with " + server.getChildResources().size() + " child
             * services");}*/
        } catch (Exception e) {
            log.error("Failure to run discovery", e);
            throw e;
        }

        InventoryPrinter.outputInventory(new PrintWriter(System.out), false);
    }

    /*//@Test(dependsOnMethods = "testServiceDiscovery")
     * // Test disabled public void datasourceConfigurationRead() throws Exception {
     *
     * for (Resource datasource : findResources(PluginContainer.getInstance().getInventoryManager().getPlatform(),
     * "Datasource", new ArrayList<Resource>())) {   //ResourceComponent serviceComponent   //       =
     * PluginContainer.getInstance().getInventoryManager().getResourceComponent(datasource);
     *
     * //Configuration config = ((ConfigurationFacet)serviceComponent).loadResourceConfiguration();
     * System.out.println("printing datasource config for " + datasource);   //if (config != null)    //
     * ConfigurationPrinter.prettyPrintConfiguration(config); }}*/

    /*// @TODO when we can run integration testing with the AS, the asserts on the next few tests should be uncommented
     * out
     * //@Test(dependsOnMethods = "testServiceDiscovery") public void dataSourceCreate() throws Exception { try {
     * PluginContainer pc = PluginContainer.getInstance();   ConfigurationAgentService configService =
     * pc.getConfigurationAgentService();   ResourceFactoryAgentService resourceFactoryAgentService =
     * pc.getResourceFactoryAgentService();   DiscoveryAgentService discoveryService =
     * PluginContainer.getInstance().getDiscoveryAgentService();
     *
     * Resource platform = discoverPlatform();   // TODO now how to actually find the AS 4.x server   Resource jonServer
     * = findServer("JON", platform);   ResourceType dsType =
     * pc.getPluginManager().getMetadataManager().getType("Datasource", "JBossAS");   assert dsType != null : "Couldn't
     * find datasource type to test with.";
     *
     * Configuration config = new Configuration();   config.put(new PropertySimple("jndiName", "NewDS"));
     *
     * CreateResourceRequest request =         new CreateResourceRequest(1, jonServer.getId(), "createdResource",
     * dsType.getName(), dsType.getPlugin(), config);
     *
     * resourceFactoryAgentService.createResource(request);
     *
     * Thread.sleep(3000);
     *
     * platform = discoverPlatform();   Resource newDs = findService("NewDS", SERVER_AS, platform);   //assert newDs !=
     * null : "Created service cannot be found";
     *
     * Availability newDsAvailability = discoveryService.getAvailability(newDs);   //assert
     * newDsAvailability.getAvailabilityType().equals(AvailabilityType.DOWN) : "Availability not set for newly created
     * service: " + newDsAvailability;
     *
     * Configuration newDsConfiguration = configService.loadResourceConfiguration(newDs.getId());   //assert
     * newDsConfiguration != null : "Null configuration found for newly created resource"; } catch (Exception e) {
     * e.printStackTrace(); }
     *
     *
     *}*/

    /*//@Test(dependsOnMethods = "testDataSourceCreate")
     * //Test is disabled public void dataSourceUpdate() throws Exception { try {   Resource platform =
     * discoverPlatform();   Resource jonDs = findService("RHQDS", SERVER_AS, platform);
     *
     * PluginContainer pc = PluginContainer.getInstance();   ConfigurationAgentService configService =
     * pc.getConfigurationAgentService();
     *
     * Configuration configuration = configService.loadResourceConfiguration(jonDs.getId());
     *
     * // Sanity check, to make sure jndi name is as we expect it   //assert configuration != null : "Configuration for
     * JON DS was null";   //assert configuration.getSimple("jndiName").getStringValue().equals("RHQDS") : "Property
     * jndiName was incorrect";
     *
     * // Change a value and update   configuration = configuration.deepCopy();   configuration.put(new
     * PropertySimple("jndiName", "Test DS"));   configurationCallback.reset();
     * configService.updateResourceConfiguration(new ConfigurationUpdateRequest(0, configuration, jonDs.getId()));
     * Thread.sleep(2000); // this should not take longer than a couple of seconds   //assert
     * configurationCallback.getLastResult() != null : "didn't get configuration update yet - do we have to wait
     * longer?";   //assert configurationCallback.getLastResult().getStatus() == ConfigurationUpdateStatus.SUCCESS;
     *
     * // Verify changes are in place   configuration = configService.loadResourceConfiguration(jonDs.getId());
     *
     * //assert configuration.getSimple("jndiName").getStringValue().equals("Test DS") : "Updated property jndiName was
     * incorrect";
     *
     * // Rollback changes (find a better way of doing this)   configuration.put(new PropertySimple("jndiName",
     * "RHQDS"));   configService.updateResourceConfiguration(new ConfigurationUpdateRequest(0, configuration,
     * jonDs.getId())); } catch (Exception e) {   e.printStackTrace(); }
     *
     *}*/

    // Test is disabled
    /*public void dataSourceDelete() throws Exception
     * { try {   PluginContainer pc = PluginContainer.getInstance();   ResourceFactoryAgentService
     * resourceFactoryAgentService = pc.getResourceFactoryAgentService();
     *
     * Resource platform = discoverPlatform();   // Rediscover to pick up new resource   Resource deleteMe =
     * findService("NewDS", SERVER_AS, platform); //         assert deleteMe != null : "Created service cannot be
     * found";
     *
     * // Delete the newly created resource   DeleteResourceRequest deleteRequest = new DeleteResourceRequest(1,
     * deleteMe.getId());   resourceFactoryAgentService.deleteResource(deleteRequest);
     *
     * Thread.sleep(1000);
     *
     * platform = discoverPlatform();   // Rediscover to pick up deletion   deleteMe = findService("NewDS", SERVER_AS,
     * platform);   //assert deleteMe == null : "Created service was not deleted"; } catch (Exception e) {
     * e.printStackTrace(); }}*/

    /*
     * Private helper methods.
     */

    private Resource discoverPlatform() throws Exception {
        PluginContainer pc = PluginContainer.getInstance();

        DiscoveryAgentService discovery = pc.getDiscoveryAgentService();

        discovery.executeServerScanImmediately();
        discovery.executeServiceScanImmediately();

        Resource platform = discovery.getPlatform();

        return platform;
    }

    private Resource findServer(String name, Resource platform) {
        Set<Resource> servers = platform.getChildResources();

        for (Resource s : servers) {
            if (name.equals(s.getName())) {
                return s;
            }
        }

        return null;
    }

    private Resource findService(String serviceName, String serverName, Resource platform) {
        Resource server = findServer(serverName, platform);

        if (server == null) {
            return null;
        }

        Set<Resource> services = server.getChildResources();

        for (Resource s : services) {
            if (serviceName.equals(s.getName())) {
                return s;
            }
        }

        return null;
    }

    private List<Resource> findResources(Resource parent, String typeName, List<Resource> foundResources) {
        if (parent.getResourceType().getName().equals(typeName)) {
            foundResources.add(parent);
        }

        for (Resource resource : parent.getChildResources()) {
            for (Resource child : resource.getChildResources()) {
                findResources(child, typeName, foundResources);
            }
        }

        return foundResources;
    }

    /*
     * @Test(dependsOnMethods = "testServiceDiscovery") public void testDatabaseMeasurement() throws Exception {
     * Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform(); for (Resource server :
     * platform.getChildResources()) {   List<Resource> services = new ArrayList<Resource>(server.getChildResources());
     *  Collections.sort(services);   for (Resource service : services)   {      ResourceComponent serviceComponent
     *    = PluginContainer.getInstance().getInventoryManager().getResourceComponent(service);      if (serviceComponent
     * instanceof MeasurementFacet)      {         Set<MeasurementScheduleRequest> metricList = new
     * HashSet<MeasurementScheduleRequest>();         metricList.add(new MeasurementScheduleRequest(1, "numbackends",
     * 1000, true));         MeasurementReport report = new MeasurementReport();         ((MeasurementFacet)
     * serviceComponent).getValues(report, metricList);         for (MeasurementData value : report.getNumericData())
     *      {            System.out.println(value.getValue() + ":" + service.getName());         }      }   } }}*/

    private Set<Resource> getDiscoveredServers() {
        Set<Resource> found = new HashSet<Resource>();
        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        for (Resource resource : platform.getChildResources()) {
            if (resource.getResourceType().getName().equals("Postgres Server")) {
                found.add(resource);
            }
        }

        return found;
    }

    private class MockConfigurationServerService implements ConfigurationServerService {
        private ConfigurationUpdateResponse lastResult;

        public void reset() {
            lastResult = null;
        }

        public ConfigurationUpdateResponse getLastResult() {
            return lastResult;
        }

        public void completeConfigurationUpdate(ConfigurationUpdateResponse response) {
            lastResult = response;
        }

      public void persistUpdatedResourceConfiguration(int resourceId,
               Configuration resourceConfiguration)
      {
         // TODO Auto-generated method stub

      }

        @Override
        @LimitedConcurrency("rhq.server.concurrency-limit.configuration-update")
        public Configuration persistUpdatedPluginConfiguration(int resourceId, Configuration pluginConfiguration) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}