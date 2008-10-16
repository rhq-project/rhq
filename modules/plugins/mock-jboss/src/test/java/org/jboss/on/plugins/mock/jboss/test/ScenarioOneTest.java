package org.jboss.on.plugins.mock.jboss.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.content.transfer.ContentDiscoveryReport;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.domain.content.transfer.DeletePackagesRequest;
import org.rhq.core.domain.content.transfer.DeployPackagesRequest;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.RetrievePackageBitsRequest;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.jboss.on.plugins.mock.jboss.ResourceCache;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.util.exception.ExceptionPackage;

/**
 * Author: Jason Dobies
 */
public class ScenarioOneTest
{
   // Constants  --------------------------------------------

   private static final Log LOG = LogFactory.getLog(ScenarioOneTest.class);

   private static final Map<String, Integer> SERVER_SERVICE_COUNTS = new HashMap<String, Integer>();

   private static final String SERVER_AS = "JBoss Application Server 4.0.4.GA";
   private static final String SERVER_JON = "JON Server";

   private static final int OPERATION_CALLBACK_SLEEP = 2000;

   // Attributes  --------------------------------------------

   private MockOperationServerService operationCallback = new MockOperationServerService();
   private MockConfigurationServerService configurationCallback = new MockConfigurationServerService();
   private MockContentServerService contentCallback = new MockContentServerService();

   // Static  --------------------------------------------

   static
   {
      SERVER_SERVICE_COUNTS.put(SERVER_JON, 5);
      SERVER_SERVICE_COUNTS.put(SERVER_AS, 9);
   }

   // Setup  --------------------------------------------

   @BeforeSuite
   public void initializePluginContainer()
   {
      System.setProperty("on.mock.jboss.scenario", "scenario1.xml");

      File pluginDir = new File("target/itest/plugins");
      PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
      pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
      pcConfig.setPluginDirectory(pluginDir);
      pcConfig.setInsideAgent(false);

      pcConfig.setOperationInvocationTimeout(2); // in seconds
      pcConfig.setAvailabilityScanInitialDelay(2); // in seconds
      pcConfig.setAvailabilityScanPeriod(2); // in seconds

      ServerServices servicesConfig = new ServerServices();
      servicesConfig.setOperationServerService(operationCallback);
      servicesConfig.setConfigurationServerService(configurationCallback);
      servicesConfig.setContentServerService(contentCallback);
      pcConfig.setServerServices(servicesConfig);

      PluginContainer pc = PluginContainer.getInstance();
      pc.setConfiguration(pcConfig);
      pc.initialize();
   }

   @AfterSuite
   public void shutdownPluginContainer()
   {
      ResourceCache.reset();
      PluginContainer.getInstance().shutdown();
   }

   @AfterMethod
   public void resetCallbackListeners()
   {
      operationCallback.reset();
      configurationCallback.reset();
      contentCallback.reset();
   }

   // Tests  --------------------------------------------

   @Test
   public void dump()
      throws Exception
   {
      Resource platform = discoverPlatform();

      LOG.info("Loaded Servers");
      logResource(platform, 0);
   }

   //@Test
   public void inventory()
      throws Exception
   {
      Resource platform = discoverPlatform();

      assert platform.getChildResources().size() == 2 : "Incorrect number of servers loaded";

      for (Resource server : platform.getChildResources())
      {
         String serverName = server.getName();
         Set<Resource> childResources = server.getChildResources();

         assert childResources.size() == SERVER_SERVICE_COUNTS.get(serverName) :
            "Services not correctly loaded for " + serverName + ". Expected " + SERVER_SERVICE_COUNTS.get(serverName) +
               " found " + childResources.size();
      }
   }

   @Test
   public void configurationDefinition()
      throws Exception
   {
      /* Strictly speaking, there is no code in the mock plugin that handles this. This
         test is to illustrate usage of the PC by the embedded console and to exercise the
         APIs.
         jdobies, Jan 8, 2007
       */
      PluginContainer pluginContainer = PluginContainer.getInstance();
      ResourceType noTxDatasourceResourceType =
         pluginContainer.getPluginManager().getMetadataManager().getType("no-tx-datasource", ResourceCategory.SERVICE);


      ConfigurationDefinition noTxConfigurationDefinition = noTxDatasourceResourceType.getResourceConfigurationDefinition();

      assert noTxConfigurationDefinition != null : "Configuration definition could not be read for no-tx-datasource";

      assert noTxDatasourceResourceType.getResourceConfigurationDefinition().getDefaultTemplate() != null :
         "Default template could not be read for no-tx-datasource";
   }

   //@Test
   public void readConfiguration()
      throws Exception
   {
      Resource platform = discoverPlatform();
      Resource jonDs = findService("DefaultDS", SERVER_JON, platform);

      PluginContainer pc = PluginContainer.getInstance();
      ConfigurationAgentService configService = pc.getConfigurationAgentService();

      Configuration configuration = configService.loadResourceConfiguration(jonDs.getId());

      assert configuration != null : "Configuration for RHQDS was null";

      assert configuration.getSimple("jndiName").getStringValue().equals("DefaultDS") : "Property jndiName was incorrect";
      assert configuration.getSimple("connectionUrl").getStringValue().equals("jdbc:oracle:thin:@dev33.qa.atl.jboss.com:1521:jon") : "Property connectionUrl was incorrect";
      assert configuration.getSimple("username").getStringValue().equals("rhqadmin") : "Property username was incorrect";
      assert configuration.getSimple("password").getStringValue().equals("rhqadmin") : "Property password was incorrect";
   }

   //@Test
   public void updateConfiguration()
      throws Exception
   {
      Resource platform = discoverPlatform();
      Resource jonDs = findService("RHQDS", SERVER_JON, platform);

      PluginContainer pc = PluginContainer.getInstance();
      ConfigurationAgentService configService = pc.getConfigurationAgentService();

      Configuration configuration = configService.loadResourceConfiguration(jonDs.getId());

      // Sanity check, to make sure jndi name is as we expect it
      assert configuration != null : "Configuration for JON DS was null";
      assert configuration.getSimple("jndiName").getStringValue().equals("RHQDS") : "Property jndiName was incorrect";

      // Change a value and update
      configuration = configuration.deepCopy();
      configuration.put(new PropertySimple("jndiName", "Test DS"));
      configurationCallback.reset();
      configService.updateResourceConfiguration(new ConfigurationUpdateRequest(0, configuration, jonDs.getId()));
      Thread.sleep(2000); // this should not take longer than a couple of seconds
      assert configurationCallback.getLastResult() != null : "didn't get configuration update yet - do we have to wait longer?";
      assert configurationCallback.getLastResult().getStatus() == ConfigurationUpdateStatus.SUCCESS;

      // Verify changes are in place
      configuration = configService.loadResourceConfiguration(jonDs.getId());

      assert configuration.getSimple("jndiName").getStringValue().equals("Test DS") : "Updated property jndiName was incorrect";

      // Rollback changes (find a better way of doing this)
      configuration.put(new PropertySimple("jndiName", "RHQDS"));
      configService.updateResourceConfiguration(new ConfigurationUpdateRequest(0, configuration, jonDs.getId()));
   }

   //@Test
   public void createServiceViaConfiguration()
      throws Exception
   {
      PluginContainer pc = PluginContainer.getInstance();
      ConfigurationAgentService configService = pc.getConfigurationAgentService();
      ResourceFactoryAgentService resourceFactoryAgentService = pc.getResourceFactoryAgentService();
      DiscoveryAgentService discoveryService = PluginContainer.getInstance().getDiscoveryAgentService();

      Resource platform = discoverPlatform();
      Resource jonServer = findServer(SERVER_JON, platform);
      ResourceType dsType =
         pc.getPluginManager().getMetadataManager().getType("no-tx-datasource", ResourceCategory.SERVICE);

      Configuration config = new Configuration();
      config.put(new PropertySimple("jndiName", "New DS"));

      CreateResourceRequest request =
         new CreateResourceRequest(1, jonServer.getId(), "createdResource", dsType.getName(), dsType.getPlugin(), null, config);

      resourceFactoryAgentService.createResource(request);

      Thread.sleep(3000);

      // Rediscover to pick up new resource
      platform = discoverPlatform();

      Resource newDs = findService("New DS", SERVER_JON, platform);
      assert newDs != null : "Created service cannot be found";

      Availability newDsAvailability = discoveryService.getAvailability(newDs);
      assert newDsAvailability.getAvailabilityType() == null : "Availability was not supposed to have been set for newly created service: " + newDsAvailability;
      Configuration newDsConfiguration = configService.loadResourceConfiguration(newDs.getId());
      assert newDsConfiguration != null : "Null configuration found for newly created resource";
   }

   //@Test
   public void deleteService()
      throws Exception
   {
      Resource platform = discoverPlatform();
      Resource deleteMe = findService("mcsoa-pubsub-db", SERVER_AS, platform);

      assert deleteMe != null : "Could not find datasource to be deleted";

      PluginContainer pc = PluginContainer.getInstance();
      ResourceFactoryAgentService resourceFactoryAgentService = pc.getResourceFactoryAgentService();

      DeleteResourceRequest deleteRequest = new DeleteResourceRequest(1, deleteMe.getId());
      resourceFactoryAgentService.deleteResource(deleteRequest);

      Thread.sleep(1000);

      InventoryManager inventoryManager = pc.getInventoryManager();
      assert inventoryManager.getResourceComponent(deleteMe) == null : "Resource component was not deleted from inventory manager";
   }

   //@Test
   public void deleteCreatedResource()
      throws Exception
   {
      // Create Resource
      PluginContainer pc = PluginContainer.getInstance();
      ResourceFactoryAgentService resourceFactoryAgentService = pc.getResourceFactoryAgentService();

      Resource platform = discoverPlatform();
      Resource jonServer = findServer(SERVER_JON, platform);
      ResourceType dsType =
         pc.getPluginManager().getMetadataManager().getType("no-tx-datasource", ResourceCategory.SERVICE);

      Configuration config = new Configuration();
      config.put(new PropertySimple("jndiName", "DeleteMeDS"));

      CreateResourceRequest request =
         new CreateResourceRequest(1, jonServer.getId(), "createdResource", dsType.getName(), dsType.getPlugin(), null, config);

      resourceFactoryAgentService.createResource(request);

      Thread.sleep(1000);

      // Rediscover to pick up new resource
      platform = discoverPlatform();

      Resource deleteMe = findService("DeleteMeDS", SERVER_JON, platform);
      assert deleteMe != null : "Created service cannot be found";

      // Delete the newly created resource
      DeleteResourceRequest deleteRequest = new DeleteResourceRequest(1, deleteMe.getId());
      resourceFactoryAgentService.deleteResource(deleteRequest);

      Thread.sleep(1000);

      // Rediscover to pick up deletion
      platform = discoverPlatform();

      deleteMe = findService("DeleteMeDS", SERVER_JON, platform);
      assert deleteMe == null : "Created service was not deleted";
   }

   //@Test
   public void availability()
      throws Exception
   {
      Resource platform = discoverPlatform();
      Resource defaultDs = findService("DefaultDS", SERVER_JON, platform);

      DiscoveryAgentService discoveryService = PluginContainer.getInstance().getDiscoveryAgentService();

      Availability availability;

      availability = discoveryService.getAvailability(defaultDs);
      assert availability != null : "Availability was returned as null";
      assert availability.getResource().getId() == defaultDs.getId() : "Incorrect resource ID associated with availability";
      assert availability.getAvailabilityType() != AvailabilityType.DOWN;

   }

   //@Test
   public void metrics()
      throws Exception
   {
      Resource platform = discoverPlatform();
      Resource eventQueue = findService("EventQueue", SERVER_JON, platform);

      MeasurementAgentService measurementService = PluginContainer.getInstance().getMeasurementAgentService();

      Set<MeasurementData> values = measurementService.getRealTimeMeasurementValue(eventQueue.getId(),DataType.MEASUREMENT, "consumer-count");

      assert values != null && values.size() > 0 : "Measurement service returned a null list of values";

      for (MeasurementData value : values)
      {
         assert value.getName().equals("consumer-count");

         MeasurementDataNumeric numeric = (MeasurementDataNumeric)value;
         assert numeric.getValue().equals(6.0);
      }
   }

   //@Test
   public void operations()
      throws Exception
   {
      Resource platform = discoverPlatform();
      Resource successResource = findService("dms-db", SERVER_AS, platform);
      Resource failureResource = findService("mcsoa-core-db", SERVER_AS, platform);
      Resource timeOutResource = findService("mcsoa-pubsub-db", SERVER_AS, platform);

      OperationAgentService operationService = PluginContainer.getInstance().getOperationAgentService();

      /* The request for operation invocation is sent to the plugin on a separate thread. As such, a sleep is added
         after each test case to ensure the actual invocation thread has enough time to execute and make the call
         to the callback object.
       */

      // Success Case
      operationService.invokeOperation("0", successResource.getId(), "start", null);
      Thread.sleep(OPERATION_CALLBACK_SLEEP);

      assert operationCallback.getLastJobId().equals("0") : "Incorrect job ID returned for success case. Expected: 0  Received: " + operationCallback.getLastJobId();
      assert operationCallback.getLastException() == null : "Exception returned for success case";

      Configuration result = operationCallback.getLastResult(); // if its defined, it'll have a single simple string property
      String successMessage = (result == null) ? "ok" : result.getSimpleProperties().values().iterator().next().getStringValue();

      assert successMessage != null : "Null result message received from success case";
      assert successMessage.endsWith("started correctly.") : "Incorrect result message received for success case: " + successMessage;

      operationCallback.reset();

      // Failure Case
      operationService.invokeOperation("1", failureResource.getId(), "start", null);
      Thread.sleep(OPERATION_CALLBACK_SLEEP);

      assert operationCallback.getLastJobId().equals("1") : "Incorrect job ID returned for failure case. Expected: 1  Received: " + operationCallback.getLastJobId();
      assert operationCallback.getLastResult() == null : "Result returned for failure case";

      ExceptionPackage failureException = operationCallback.getLastException();

      assert failureException != null : "Null exception message received from failure case";
      assert failureException.getMessage().startsWith("Error occurred") : "Incorrect message received for failure case: " + failureException.getMessage();
      assert failureException.getStackTraceString() != null : "Stack trace was not present in ExceptionPackage";

      operationCallback.reset();

      // Time Out Case
      operationService.invokeOperation("2", timeOutResource.getId(), "start", null);
      Thread.sleep(3000);

      assert operationCallback.isTimedOut() : "Time out response never received";

      assert operationCallback.getLastJobId().equals("2") : "Incorrect job ID returned for time out case. Expected 2  Received: " + operationCallback.getLastJobId();

      if (operationCallback.getLastException() != null)
      {
         LOG.error(operationCallback.getLastException().getStackTraceString());
         assert false : "Exception returned from time out case";
      }

      assert operationCallback.getLastResult() == null : "Result returned from time out case";
   }

//   @Test
//   public void discoverArtifacts()
//      throws Exception
//   {
//      // Setup
//      Resource platform = discoverPlatform();
//      Resource resource = findService("xswtp-db", SERVER_AS, platform);
//      Set<PackageType> packageTypes = resource.getResourceType().getPackageTypes();
//      PackageType type = null;
//      for (PackageType def : packageTypes)
//      {
//         if (def.getName().equals("datasourceDescriptor"))
//         {
//            type = def;
//            break;
//         }
//      }
//
//      assert type != null : "Could not find type for datasourceDescriptor";
//
//      ContentAgentService contentAgentService = PluginContainer.getInstance().getContentAgentService();
//
//      // Test
//      ContentDiscoveryReport discoveredPackages =
//         contentAgentService.executeResourcePackageDiscoveryImmediately(resource.getId(), type.getName());
//
//      assert discoveredPackages != null : "Discovered artifacts were returned as null";
//      assert discoveredPackages.getDeployedPackages() != null : "Discovered artifacts were returned as null";
//      assert discoveredPackages.getDeployedPackages().size() == 1 : "Incorrect number of artifacts returned. Expected 1, Found: " + discoveredPackages.size();
//
//      InstalledPackage artifact = discoveredPackages.getDeployedPackages().iterator().next();
//      assert artifact.getArtifactKey().equals("descriptor") : "Incorrect artifact key returned from discovered artifact";
//   }
//
//   @Test
//   public void createArtifact()
//      throws Exception
//   {
//      // Setup
//      Resource platform = discoverPlatform();
//      Resource resource = findService("mcsoa-pubsub-db", SERVER_AS, platform);
//
//      Set<PackageType> packageTypes = resource.getResourceType().getPackageTypes();
//      PackageType type = null;
//      for (PackageType def : packageTypes)
//      {
//         if (def.getName().equals("datasourceDescriptor"))
//         {
//            type = def;
//            break;
//         }
//      }
//
//      assert type != null : "Could not find type for datasourceDescriptor";
//
//      // Assemble Request
//      DeployPackagesRequest request = new DeployPackagesRequest(1, resource.getId(), "New Artifact", type.getName());
//
//      URL contentUrl = this.getClass().getClassLoader().getResource("scenario1.xml");
//
//      assert contentUrl != null : "Could not find content";
//
//      // Test
//      ContentAgentService contentAgentService = PluginContainer.getInstance().getContentAgentService();
//      contentAgentService.deployPackages(request);
//
//      // Create is on a separate thread, give it a bit to complete 
//      Thread.sleep(2000);
//
//      // Verify
//      ContentServiceResponse result = (ContentServiceResponse)contentCallback.getLastResult();
//
//      assert result != null : "Server service was never contacted with a result";
//      assert result.getStatus() == ContentRequestStatus.SUCCESS : "Incorrect request status on result. Found: " + result.getStatus();
//
//      ContentDiscoveryReport discoveredPackages =
//         contentAgentService.executeResourcePackageDiscoveryImmediately(resource.getId(), type.getName());
//
//      assert discoveredPackages != null : "Discovered artifacts were returned as null";
//      assert discoveredPackages.getDeployedPackages() != null : "Discovered artifacts were returned as null";
//      assert discoveredPackages.getDeployedPackages().size() == 1 : "Incorrect number of artifacts were discovered. Expected: 1, Found: " + discoveredPackages.size();
//   }
//
//   @Test
//   public void deleteArtifact()
//      throws Exception
//   {
//      // Setup
//      Resource platform = discoverPlatform();
//      Resource resource = findService("xswtp-db", SERVER_AS, platform);
//      Set<PackageType> packageTypes = resource.getResourceType().getPackageTypes();
//      PackageType type = null;
//      for (PackageType def : packageTypes)
//      {
//         if (def.getName().equals("datasourceArtifact2"))
//         {
//            type = def;
//            break;
//         }
//      }
//
//      assert type != null : "Could not find type for datasourceDescriptor";
//
//      ContentAgentService contentAgentService = PluginContainer.getInstance().getContentAgentService();
//
//      Set<InstalledPackage> discoveredPackages =
//         contentAgentService.executeResourcePackageDiscoveryImmediately(resource.getId(), type.getName());
//
//      assert discoveredPackages != null : "Artifacts were not initially found in resource";
//      assert discoveredPackages.size() == 1 : "Incorrect number of artifacts initially found in resource. Expected: 1, Found: " + discoveredPackages.size();
//
//      // Assemble Request
//      InstalledPackage doomedArtifact = discoveredPackages.iterator().next();
//      DeletePackagesRequest request = new DeletePackagesRequest(1, resource.getId(), doomedArtifact.getArtifactKey(), doomedArtifact.getArtifactType().getName());
//
//      // Test
//      contentAgentService.deletePackages(request);
//
//      // Delete is on a separate thread, give it a bit to complete 
//      Thread.sleep(10000);
//
//      // Verify
//      ContentServiceResponse result = (ContentServiceResponse)contentCallback.getLastResult();
//
//      assert result != null : "Server service was never contacted with a result";
//      assert result.getStatus() == ContentRequestStatus.SUCCESS : "Incorrect request status on result. Found: " + result.getStatus();
//
//      discoveredPackages = contentAgentService.executeResourcePackageDiscoveryImmediately(resource.getId(), type.getName());
//
//      assert discoveredPackages != null : "Null set of artifacts received after delete call";
//      assert discoveredPackages.size() == 0 : "Incorrect number of artifacts received after delete call. Expected: 1, Found: " + discoveredPackages.size();
//   }
//
//   @Test
//   public void getContent()
//      throws Exception
//   {
//      // Setup
//      Resource platform = discoverPlatform();
//      Resource resource = findService("xswtp-db", SERVER_AS, platform);
//      Set<PackageType> packageTypes = resource.getResourceType().getPackageTypes();
//      PackageType type = null;
//      for (PackageType def : packageTypes)
//      {
//         if (def.getName().equals("datasourceDescriptor"))
//         {
//            type = def;
//            break;
//         }
//      }
//
//      assert type != null : "Could not find type for datasourceDescriptor";
//
//      ContentAgentService contentAgentService = PluginContainer.getInstance().getContentAgentService();
//
//      Set<InstalledPackage> discoveredPackages =
//         contentAgentService.executeResourcePackageDiscoveryImmediately(resource.getId(), type.getName());
//
//      assert discoveredPackages != null : "Artifacts were not initially found in resource";
//      assert discoveredPackages.size() == 1 : "Incorrect number of artifacts initially found in resource. Expected: 1, Found: " + discoveredPackages.size();
//
//      // Assemble Request
//      InstalledPackage installedPackage = discoveredPackages.iterator().next();
//      RetrievePackageBitsRequest request = new RetrievePackageBitsRequest(1, resource.getId(), installedPackage.getArtifactKey(), installedPackage.getArtifactType().getName());
//
//      // Test
//      contentAgentService.retrievePackageBits(request);
//
//      // Get is on a separate thread, give it a bit to complete 
//      Thread.sleep(1000);
//
//      // Verify
//      ContentServiceResponse result = (ContentServiceResponse)contentCallback.getLastResult();
//      InputStream contentStream = contentCallback.inputStream;
//
//      assert result != null : "Server service was never contacted with a result";
//      assert result.getStatus() == ContentRequestStatus.SUCCESS : "Incorrect request status on result. Found: " + result.getStatus();
//      assert contentStream != null : "Content stream was not found in result";
//
//      InputStreamReader isr = new InputStreamReader(contentStream);
//      BufferedReader br = new BufferedReader(isr);
//
//      String firstLine = br.readLine();
//      assert firstLine.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>") : "Incorrect first line of content: " + firstLine;
//
//      br.close();
//   }

   // Private  --------------------------------------------

   private void logResource(Resource resource, int level) throws PluginContainerException
   {
      PluginContainer pc = PluginContainer.getInstance();
      ConfigurationAgentService configService = pc.getConfigurationAgentService();
      StringBuilder spacer = new StringBuilder();

      for (int j = 0; j < level; j++)
         spacer.append("  ");

      LOG.info(spacer + resource.getResourceType().getName() + ": " + resource.getName());

      try
      {
         Configuration resourceConfiguration = configService.loadResourceConfiguration(resource.getId());
         logConfiguration(resourceConfiguration, spacer.append("  ").toString());
      }
      catch (PluginContainerException e)
      {
         // configuration isn't supported on that resource
      }

      level++;
      for (Resource childResource : resource.getChildResources())
      {
         logResource(childResource, level);
      }
   }

   private Resource discoverPlatform()
      throws Exception
   {
      PluginContainer pc = PluginContainer.getInstance();

      DiscoveryAgentService discovery = pc.getDiscoveryAgentService();

      discovery.executeServerScanImmediately();
      discovery.executeServiceScanImmediately();

      Resource platform = discovery.getPlatform();

      return platform;
   }

   private Resource findServer(String name, Resource platform)
   {
      Set<Resource> servers = platform.getChildResources();

      for (Resource s : servers)
      {
         if (name.equals(s.getName()))
         {
            return s;
         }
      }

      return null;
   }

   private Resource findService(String serviceName, String serverName, Resource platform)
   {
      Resource server = findServer(serverName, platform);

      if (server == null)
      {
         return null;
      }

      Set<Resource> services = server.getChildResources();

      for (Resource s : services)
      {
         if (serviceName.equals(s.getName()))
         {
            return s;
         }
      }

      return null;
   }

   private void logConfiguration(Configuration config, String prefix)
   {
      if (config == null)
      {
         return;
      }

      Collection<String> names = config.getNames();

      for (String name : names)
      {
         Property property = config.get(name);

         if (property instanceof PropertySimple)
         {
            PropertySimple simple = (PropertySimple)property;
            LOG.info(prefix + "Property: " + name + " -> " + simple.getStringValue());
         }
      }
   }

   // Inner Classes  --------------------------------------------

   private class MockOperationServerService implements OperationServerService
   {
      // Attributes  --------------------------------------------

      private String lastJobId = "-1";
      private Configuration lastResult;
      private ExceptionPackage lastException;
      private boolean timedOut;

      // Public  --------------------------------------------

      public void reset()
      {
         lastJobId = "-1";
         lastResult = null;
         lastException = null;
         timedOut = false;
      }

      public String getLastJobId()
      {
         return lastJobId;
      }

      public Configuration getLastResult()
      {
         return lastResult;
      }

      public ExceptionPackage getLastException()
      {
         return lastException;
      }

      public boolean isTimedOut()
      {
         return timedOut;
      }

      // TestOperationServerService  --------------------------------------------

      public void operationSucceeded(String jobId, Configuration result, long invocationTime, long completionTime)
      {
         lastJobId = jobId;
         lastResult = result;
      }

      public void operationFailed(String jobId, ExceptionPackage error, long invocationTime, long completionTime)
      {
         lastJobId = jobId;
         lastException = error;
      }

      public void operationTimedOut(String jobId, long invocationTime, long timeoutTime)
      {
         lastJobId = jobId;
         timedOut = true;
      }
   }

   private class MockConfigurationServerService implements ConfigurationServerService
   {
      private ConfigurationUpdateResponse lastResult;

      public void reset()
      {
         lastResult = null;
      }

      public ConfigurationUpdateResponse getLastResult()
      {
         return lastResult;
      }

      public void completeConfigurationUpdate(ConfigurationUpdateResponse response)
      {
         lastResult = response;
      }

   }

   private class MockContentServerService implements ContentServerService {
		private Object lastResult;
		private InputStream inputStream;

		public void reset() {
			lastResult = null;
			inputStream = null;
		}

		public Object getLastResult() {
			return lastResult;
		}

		public InputStream getInputStream() {
			return inputStream;
		}

//		public void completeDeletePackageRequest(ContentServiceResponse response) {
//			lastResult = response;
//		}
//
//		public void completeDeployPackageRequest(ContentServiceResponse response) {
//			lastResult = response;
//		}
//
		public void completeRetrievePackageBitsRequest(
				ContentServiceResponse response, InputStream contentStream) {
			lastResult = response;
			inputStream = contentStream;
		}

		public void mergeDiscoveredPackages(ContentDiscoveryReport report) {
		}

		public void completeDeletePackageRequest(RemovePackagesResponse response) {
			// TODO Auto-generated method stub

		}

		public void completeDeployPackageRequest(DeployPackagesResponse response) {
			// TODO Auto-generated method stub

		}

		public long downloadPackageBitsForChildResource(int parentResourceId,
				String resourceTypeName, PackageDetailsKey packageDetailsKey,
				OutputStream outputStream) {
			// TODO Auto-generated method stub
			return 0;
		}

		public long downloadPackageBitsGivenResource(int resourceId,
				PackageDetailsKey packageDetailsKey, OutputStream outputStream) {
			// TODO Auto-generated method stub
			return 0;
		}

		public long downloadPackageBitsRangeGivenResource(int resourceId,
				PackageDetailsKey packageDetailsKey, OutputStream outputStream,
				long startByte, long endByte) {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getPackageBitsLength(int resourceId,
				PackageDetailsKey packageDetailsKey) {
			// TODO Auto-generated method stub
			return 0;
		}

		public PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(
				int resourceId, PageControl pc) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getResourceSubscriptionMD5(int resourceId) {
			// TODO Auto-generated method stub
			return null;
		}

		public Set<ResourcePackageDetails> loadDependencies(int requestId,
				Set<PackageDetailsKey> dependencyPackages) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
