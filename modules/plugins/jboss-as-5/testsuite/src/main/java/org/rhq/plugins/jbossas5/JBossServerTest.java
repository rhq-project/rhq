/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.plugins.jbossas5;

import junit.extensions.TestSetup;
import junit.framework.Test;
import org.apache.cactus.ServletTestCase;
import org.apache.cactus.ServletTestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.KnownComponentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedParameter;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.types.CollectionMetaType;
import org.jboss.metatype.api.types.CompositeMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
//import org.rhq.plugins.jbossas5.util.ConversionUtil;
//import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Cactus Unit Test class to test interactivity between the PluginContainer and the AS ProfileService
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 */
public class JBossServerTest extends ServletTestCase
{
    //CONSTANTS
    private static final Log LOG = LogFactory.getLog(JBossServerTest.class);

    private static final String SERVER_NAME = "Jboss App Server:default";
    private static final String B_SERVER_NAME = "Application Server:default";

    // Test Cases  --------------------------------------------

    public static Test suite()
    {
        return new TestSetup(new ServletTestSuite(JBossServerTest.class))
        {
            public void setUp() throws Exception
            {
                PluginContainer.getInstance().initialize();
            }

            public void tearDown() throws Exception
            {
                shutdownPluginContainer();
            }
        };

    }

    public void testManagedPropertiesByType()
    {

        ManagementView mgtView = getCurrentProfileView();
        try
        {
            DeploymentTemplateInfo noTXinfo = mgtView.getTemplate("NoTxDataSourceTemplate");
            DeploymentTemplateInfo xaInfo = mgtView.getTemplate("XADataSourceTemplate");
            //Set<String> templateNames = mgtView.getTemplateNames();
            //Set<String> deploymentNames = mgtView.getDeploymentNames();

            ComponentType localType = new ComponentType("DataSource", "Local Transaction");
            Set<ManagedComponent> localDS = mgtView.getComponentsForType(localType);
            if (localDS != null)
            {
                for (ManagedComponent component : localDS)
                {
                    LOG.info("***********************************************");
                    LOG.info("******Dumping a LocalTX component values******");
                    LOG.info("***********************************************");
                    Map<String, ManagedProperty> properties = component.getProperties();
                    LOG.info("\t******Properties******");
                    for (ManagedProperty managedProperty : properties.values())
                    {
                        String name = managedProperty.getName();
                        MetaType propType = managedProperty.getMetaType();
                        LOG.info("\t\tProperty Name: " + name);
                        LOG.info("\t\tProperty Type: " + propType.getTypeName());
                        if (!propType.isGeneric() && !propType.isSimple())
                        {
                            if (propType.isCollection())
                            {
                                CollectionMetaType collMetaType = (CollectionMetaType) propType;
                                LOG.info("\t\t\tCollection Sub Type: " + collMetaType.getTypeName());
                            }
                            if (propType.isComposite())
                            {
                                CompositeMetaType compositeType = (CompositeMetaType) propType;
                                Set<String> items = compositeType.itemSet();
                                for (String itemName : items)
                                {
                                    LOG.info("\t\t\tComposite Sub Type: " + compositeType.getType(itemName).getTypeName());
                                }
                            }
                        }
                    }
                    LOG.info("***********************************************");
                    LOG.info("\t******Operations******");
                    Set<ManagedOperation> operations = component.getOperations();
                    for (ManagedOperation operation : operations)
                    {
                        String name = operation.getName();
                        MetaType returnType = operation.getReturnType();
                        LOG.info("\t\tOperation Name: " + name);
                        LOG.info("\t\tReturn Type: " + returnType.getTypeName());
                        ManagedParameter[] parameters = operation.getParameters();
                        LOG.info("\t\tParameters:");
                        for (ManagedParameter parameter : parameters)
                        {
                            String paramName = parameter.getName();
                            MetaType paramType = parameter.getMetaType();
                            LOG.info("\t\t\tParameter Name: " + paramName + " Type: " + paramType);
                        }
                    }
                }
            }
            LOG.info("queues: " + localDS);
            //assertNotNull("Null Local DS", localDS);
            //assertTrue("queues.size", localDS.size() > 0);

            ComponentType noType = new ComponentType("DataSource", "No Transaction");
            Set<ManagedComponent> noDS = mgtView.getComponentsForType(noType);
            if (noDS != null)
            {
                for (ManagedComponent component : noDS)
                {
                    Set<ManagedOperation> operations = component.getOperations();
                    for (ManagedOperation operation : operations)
                    {
                        String name = operation.getName();
                        ManagedParameter[] parameters = operation.getParameters();
                        for (ManagedParameter parameter : parameters)
                        {
                            String paramName = parameter.getName();
                        }
                    }
                }
            }
            LOG.info("queues: " + noDS);
            //assertNotNull("Null NO TX DS", noDS);
            //assertTrue("queues.size", noDS.size() == 0);

            ComponentType xaType = new ComponentType("DataSource", "XA Transaction");
            Set<ManagedComponent> xaDS = mgtView.getComponentsForType(xaType);
            if (xaDS != null)
            {
                for (ManagedComponent component : xaDS)
                {
                    Set<ManagedOperation> operations = component.getOperations();
                    for (ManagedOperation operation : operations)
                    {
                        String name = operation.getName();
                        ManagedParameter[] parameters = operation.getParameters();
                        for (ManagedParameter parameter : parameters)
                        {
                            String paramName = parameter.getName();
                        }
                    }
                }
            }
            LOG.info("queues: " + xaDS);
            //assertNotNull("Null XA DS", xaDS);
            //assertTrue("queues.size", xaDS.size() == 0);

            ComponentType queueType = new ComponentType("JMSDestination", "Queue");
            Set<ManagedComponent> queues = mgtView.getComponentsForType(queueType);
            LOG.info("queues: " + queues);
            //assertNotNull("Null JMS queues", queues);
            //assertTrue("queues.size", queues.size() > 0);

            ComponentType topicType = new ComponentType("JMSDestination", "Topic");
            Set<ManagedComponent> topics = mgtView.getComponentsForType(topicType);
            LOG.info("topics: " + topics);
            //assertNotNull(topics);
            //assertTrue("topics.size", topics.size() > 0);

        }
        catch (NoSuchDeploymentException e)
        {
            LOG.error(e.getMessage(), e);
            //fail();
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage(), e);
            //fail();
        }
    }

    public void testPluginLoad()
    {
        LOG.info("########################  Running testPluginLoad()");
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();

        PluginEnvironment pluginEnvironment = pluginManager.getPlugin("ProfileService");
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        //System.out.println("Plugin package: " + pluginEnvironment.getDescriptor().getPackage());
    }

    public void testServerDiscovery() throws Exception
    {
        LOG.info("########################  Running testServerDiscovery()");
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

        Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
        Set<Resource> servers = platform.getChildResources();
        assert servers.size() != 0;
        System.out.println("Found " + servers.size() + " servers");
    }

    public void testServiceDiscovery() throws Exception
    {
        LOG.info("########################  Running testServiceDiscovery()");
        try
        {

            PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
            Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

            System.out.println("RUNTIME SERVERS: " + platform.getChildResources().size());
            for (Resource server : platform.getChildResources())
            {
                System.out.println("Server: " + server.toString());
                System.out.println("Found with " + server.getChildResources().size() + " child services");
                assert server.getChildResources().size() > 0 : "There should be child resources discovered";
            }
        }
        catch (Exception e)
        {
            LOG.error("Failure to run discovery", e);
            throw e;
        }
    }

    public void testDump()
            throws Exception
    {
        LOG.info("########################  Running testDump()");
        PluginContainer pc = PluginContainer.getInstance();
        ConfigurationAgentService configService = pc.getConfigurationAgentService();

        Resource platform = discoverPlatform();

        LOG.info("Loaded Servers");
        for (Resource server : platform.getChildResources())
        {
            String serverName = server.getName();
            LOG.info("Server: " + serverName);

            for (Resource service : server.getChildResources())
            {
                LOG.info("   Service: " + service.getName());

                Configuration serviceConfig = configService.loadResourceConfiguration(service.getId());
                logConfiguration(serviceConfig, "     ");
            }
        }
    }

    public void testInventory()
            throws Exception
    {
        LOG.info("########################  Running testInventory()");
        Resource platform = discoverPlatform();

        // There should only be one server
        assert platform.getChildResources().size() == 1 : "Incorrect number of servers loaded";

        recursiveInventory(platform.getChildResources());
        for (Resource server : platform.getChildResources())
        {
            String serverName = server.getName();
            assertEquals("Server Name should be: " + SERVER_NAME, SERVER_NAME, serverName);
            Set<Resource> childResources = server.getChildResources();
            assert childResources.size() > 0 : "There should be more than one service found";
        }
    }

    private void recursiveInventory(Set<Resource> childResources)
    {
        for (Resource resource : childResources)
        {
            //String resourceName = resource.getName();
            Set<Resource> children = resource.getChildResources();
            recursiveInventory(children);
        }
    }

    public void testConfigurationDefinition()
            throws Exception
    {
        LOG.info("########################  Running testConfigurationDefinition()");
        PluginContainer pluginContainer = PluginContainer.getInstance();
        PluginMetadataManager metadataManager = pluginContainer.getPluginManager().getMetadataManager();
        ResourceType noTxDatasourceResourceType =
                metadataManager.getType("No Transaction", ResourceCategory.SERVICE);


        ConfigurationDefinition noTxConfigurationDefinition = noTxDatasourceResourceType.getResourceConfigurationDefinition();

        assert noTxConfigurationDefinition != null : "Configuration definition could not be read for no-tx-datasource";

        assert noTxDatasourceResourceType.getResourceConfigurationDefinition().getDefaultTemplate() != null :
                "Default template could not be read for no-tx-datasource";
    }

    public void testReadDataSourceConfiguration()
            throws Exception
    {
        LOG.info("########################  Running testReadConfiguration()");
        Resource platform = discoverPlatform();
        // This only tests a datasource, will need to test other service types
        Resource defaultDs = findService("DefaultDS", SERVER_NAME, platform);


        PluginContainer pc = PluginContainer.getInstance();
        ConfigurationAgentService configService = pc.getConfigurationAgentService();

        Configuration configuration = configService.loadResourceConfiguration(defaultDs.getId());

        assert configuration != null : "Configuration for DefaultDS was null";

        assert configuration.getSimple("jndi-name").getStringValue().equals("DefaultDS") : "Property jndi-name was incorrect";
        assert configuration.getSimple("connection-url").getStringValue().equals("jdbc:hsqldb:hsql://${jboss.bind.address}:1701")
                : "Property connection-url was incorrect";
        assert configuration.getSimple("user-name").getStringValue().equals("sa") : "Property user-name was incorrect";
        assert configuration.getSimple("password").getStringValue().equals("") : "Property password was incorrect";
        assert configuration.getSimple("security-domain").getStringValue().equals("") : "Property security-domain was incorrect";
        assert configuration.getSimple("min-pool-size").getStringValue().equals("5") : "Property min-pool-size was incorrect";
        assert configuration.getSimple("max-pool-size").getStringValue().equals("20") : "Property max-pool-size was incorrect";
    }

    public void testUpdateDataSourceConfiguration()
            throws Exception
    {
        LOG.info("########################  Running testUpdateConfiguration()");
        // Not sure how well this actually works, cannot see the change
        // .being made into the DefaultDS.xml file
        Resource platform = discoverPlatform();
        Resource defaultDs = findService("DefaultDS", SERVER_NAME, platform);

        PluginContainer pc = PluginContainer.getInstance();
        ConfigurationAgentService configService = pc.getConfigurationAgentService();

        Configuration configuration = configService.loadResourceConfiguration(defaultDs.getId());

        // Sanity check, to make sure jndi name is as we expect it
        assert configuration != null : "Configuration for Default DS was null";
        assert configuration.getSimple("jndi-name").getStringValue().equals("DefaultDS") : "Property jndiName was incorrect";

        // Change a value and update
        //configuration = configuration.deepCopy();
        configuration.put(new PropertySimple("jndi-name", "TestDS"));
        configuration.put(new PropertySimple("max-pool-size", "100"));
        ConfigurationUpdateRequest cur = new ConfigurationUpdateRequest(0, configuration, defaultDs.getId());

        configService.updateResourceConfiguration(cur);

        // Verify changes are in place
        configuration = configService.loadResourceConfiguration(defaultDs.getId());

        assert configuration.getSimple("jndi-name").getStringValue().equals("TestDS") : "Updated property jndiName was incorrect";
        assert configuration.getSimple("max-pool-size").getStringValue().equals("100") : "Updated property max-pool-size was incorrect";
        // Rollback changes (find a better way of doing this)
        configuration.put(new PropertySimple("jndi-name", "DefaultDS"));
        configuration.put(new PropertySimple("max-pool-size", "20"));
        cur.setConfiguration(configuration);
        configService.updateResourceConfiguration(cur);
    }

    public void testCreateDataSourceService()
            throws Exception
    {
        LOG.info("########################  Running testCreateService()");
        createDatasource("AnotherTestDS", "3", "10");
    }

    public void testDeleteDataSourceService()
            throws Exception
    {
        LOG.info("########################  Running testDeleteService()");
        deleteDataSource("DeleteDS");
    }

    public void testCreateThenDeleteDataSourceThroughProfileService()
            throws Exception
    {
        LOG.info("########################  Running testCreateService()");
        createDatasource("CreateDeleteTestDS", "3", "10");
        deleteDataSource("CreateDeleteTestDS");
    }

    public void testDataSourceOperationService()
            throws Exception
    {
        Resource platform = discoverPlatform();
        Resource defaultDS = findService("DefaultDS", SERVER_NAME, platform);
        String operationName = "flush";

        PluginContainer pc = PluginContainer.getInstance();
        OperationAgentService operationAgentService = pc.getOperationAgentService();
        operationAgentService.invokeOperation("1", defaultDS.getId(), operationName, null);
    }

    public void testDataSourceMetrics()
            throws Exception
    {
        Resource platform = discoverPlatform();
        Resource defaultDS = findService("DefaultDS", SERVER_NAME, platform);

        PluginContainer pc = PluginContainer.getInstance();
        Set<MeasurementDefinition> definitions = defaultDS.getResourceType().getMetricDefinitions();

        List<MeasurementDataRequest> requests = new ArrayList<MeasurementDataRequest>();
        for (MeasurementDefinition definition : definitions) {
            requests.add(new MeasurementDataRequest(definition));
        }
        Set<MeasurementData> data = pc.getMeasurementAgentService().getRealTimeMeasurementValue(defaultDS.getId(),
            requests);
        assert data.size() > 0 : "We should be getting some kind of measurement data back";
    }


    // @TODO Make this a test when Availability is part of the Profile Service
    public void availability()
            throws Exception
    {
        Resource platform = discoverPlatform();
        // @TODO change to get other Datasources, String would be different.
        Resource jonDs = null;//findService("RHQDS", SERVER_JON, platform);
        Resource defaultDs = null;//findService("DefaultDS", SERVER_JON, platform);

        DiscoveryAgentService discoveryService = PluginContainer.getInstance().getDiscoveryAgentService();

        Availability availability;

        availability = discoveryService.getAvailability(jonDs);
        assert availability != null : "Availability was returned as null";
        assert availability.getResource().getId() == jonDs.getId() : "Incorrect resource ID associated with availability";
        assert availability.getAvailabilityType() == AvailabilityType.UP : "Mock default value for availability was set incorrectly";

        availability = discoveryService.getAvailability(defaultDs);
        assert availability != null : "Availability was returned as null";
        assert availability.getResource().getId() == defaultDs.getId() : "Incorrect resource ID associated with availability";
        assert availability.getAvailabilityType() == AvailabilityType.DOWN : "Mock did not set availability to down correctly";

    }

    public void testReadDatasourceFromManagementView()
            throws Exception
    {
        ManagementView mgtView = getCurrentProfileView();
        Set<ManagedComponent> components = mgtView.getComponentsForType(KnownComponentTypes.DataSourceTypes.LocalTx.getType());
        assert components != null : "Components was returned as null";
        assert components.size() > 0;

        for (ManagedComponent comp : components)
        {
            LOG.info("**** Local Tx found named: " + comp.getName());
        }
    }

    public void testReadJMSFromManagementView()
            throws Exception
    {
        ManagementView mgtView = getCurrentProfileView();
        Set<ManagedComponent> components = mgtView.getComponentsForType(KnownComponentTypes.JMSDestination.Queue.getType());
        assert components != null : "Components was returned as null";
        assert components.size() > 0;

        for (ManagedComponent comp : components)
        {
            LOG.info("**** JMS Destination found named: " + comp.getName());
        }
    }

    public void testReadDLQJMSFromManagementView()
            throws Exception
    {
        ManagementView mgtView = getCurrentProfileView();
        ManagedComponent comp = mgtView.getComponent("/queue/DLQ", KnownComponentTypes.JMSDestination.Queue.getType());
        assert comp != null : "Comp was returned as null";
        assert comp.getName().equals("/queue/DLQ") : "Did not find DLQ Destintion";

    }

    // Private  --------------------------------------------

    private Resource discoverPlatform()
            throws Exception
    {
        PluginContainer pc = PluginContainer.getInstance();

        DiscoveryAgentService discovery = pc.getDiscoveryAgentService();

        discovery.executeServerScanImmediately();
        discovery.executeServiceScanImmediately();

        return discovery.getPlatform();
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
                PropertySimple simple = (PropertySimple) property;
                LOG.info(prefix + "Property: " + name + " -> " + simple.getStringValue());
            }
        }
    }

    private void createDatasource(String dsName, String min, String max)
            throws Exception
    {
        LOG.info("########################  Running testCreateService()");
        PluginContainer pc = PluginContainer.getInstance();
        ResourceFactoryAgentService resourceFactoryAgentService = pc.getResourceFactoryAgentService();

        Resource platform = discoverPlatform();
        // There should only be one server.
        Resource as5Server = findServer(SERVER_NAME, platform);
        ResourceType dsType =
                pc.getPluginManager().getMetadataManager().getType("Local Transaction", ResourceCategory.SERVICE);

        //Puts a String, Long String, Integer, Boolean, and Map into the DS config
        Configuration config = new Configuration();
        config.put(new PropertySimple("jndi-name", dsName));
        config.put(new PropertySimple("connection-url", "jdbc:hsqldb:."));
        config.put(new PropertySimple("user-name", "userJoe"));
        config.put(new PropertySimple("password", "password"));
        // Min and Max are not being put into the xml file.
        config.put(new PropertySimple("min-pool-size", min));
        config.put(new PropertySimple("max-pool-size", max));
        config.put(new PropertySimple("driver-class", "org.hsqldb.jdbcDriver"));
        config.put(new PropertySimple("share-prepared-statements", false));
        config.put(new PropertySimple("prepared-statement-cache-size", max));
        config.put(new PropertySimple("new-connection-sql", "Select count(*) from dual;"));

        PropertyMap connectionProperies = new PropertyMap("connectionProperties");
        PropertySimple simple1 = new PropertySimple("ConnProp1", "FirstValue");
        PropertySimple simple2 = new PropertySimple("ConnProp2", "SecondValue");
        connectionProperies.put(simple1);
        connectionProperies.put(simple2);
        config.put(connectionProperies);

        CreateResourceRequest request = new CreateResourceRequest(1, as5Server.getId(), "newResource", dsType.getName(), dsType.getPlugin(), null, config);

        resourceFactoryAgentService.executeCreateResourceImmediately(request);

        platform = discoverPlatform();

        Resource testDs = findService(dsName, SERVER_NAME, platform);

        pc = PluginContainer.getInstance();
        ConfigurationAgentService configService = pc.getConfigurationAgentService();

        Configuration configuration = configService.loadResourceConfiguration(testDs.getId());

        assert configuration.getSimple("jndi-name").getStringValue().equals(dsName) : "Property jndi-name was incorrect";
        assert configuration.getSimple("connection-url").getStringValue().equals("jdbc:hsqldb:hsql://${jboss.bind.address}:1701")
                : "Property connection-url was incorrect";
        assert configuration.getSimple("user-name").getStringValue().equals("userJoe") : "Property user-name was incorrect";
        assert configuration.getSimple("password").getStringValue().equals("password") : "Property password was incorrect";
        assert configuration.getSimple("min-pool-size").getStringValue().equals(min) : "Property min-pool-size was incorrect";
        assert configuration.getSimple("max-pool-size").getStringValue().equals(max) : "Property max-pool-size was incorrect";

        // @todo add assertions for new properties like the Integer and Boolean and Map properties
    }

    private void deleteDataSource(String dsName)
            throws Exception
    {
        Resource platform = discoverPlatform();
        Resource deleteMe = findService(dsName, SERVER_NAME, platform);

        assert deleteMe != null : "Could not find datasource to be deleted";

        PluginContainer pc = PluginContainer.getInstance();
        ResourceFactoryAgentService resourceFactoryAgentService = pc.getResourceFactoryAgentService();

        DeleteResourceRequest deleteRequest = new DeleteResourceRequest(1, deleteMe.getId());

        try
        {
            resourceFactoryAgentService.executeDeleteResourceImmediately(deleteRequest);
        }
        catch (PluginContainerException e)
        {
            fail();
        }

        InventoryManager inventoryManager = pc.getInventoryManager();
        assert inventoryManager.getResourceComponent(deleteMe) == null : "Resource component was not deleted from inventory manager";

        Resource deleteMe2 = findService(dsName, SERVER_NAME, platform);

        assert deleteMe2 == null : "found datasource that was supposed to be deleted";
    }

    private static void shutdownPluginContainer()
    {
        PluginContainer.getInstance().shutdown();
    }

    private static ProfileService getProfileService()
    {
        ProfileService profileService = null;
        InitialContext initialContext;
        try
        {
            initialContext = new InitialContext();
        }
        catch (NamingException e)
        {
            LOG.error("Unable to get an InitialContext to JBoss AS 5", e);
            return null;
        }

        try
        {
            profileService = (ProfileService) initialContext.lookup("ProfileService");

            //ManagementView view = getCurrentProfileView();
            //ComponentType type = new ComponentType("DataSource", "Local Transaction");
            //Set<ManagedComponent> components = view.getComponentsForType(type);
        }
        catch (NamingException e)
        {
            LOG.error("Could not find ProfileService Name on JBoss AS 5", e);
        }
        catch (Exception e)
        {
            LOG.error("Exception thrown when looking up ProfileService on JBoss AS 5", e);
        }
        return profileService;
    }

    private static ManagementView getCurrentProfileView()
    {
        ProfileService profileService = getProfileService();
        ManagementView currentProfileView = profileService.getViewManager();
        String[] domains = profileService.getDomains();

        ProfileKey defaultKey = new ProfileKey(domains[0]);

        try
        {
            currentProfileView.loadProfile(defaultKey);
        }
        catch (Exception e)
        {
            LOG.error("Could not find default Profile in Current Profile View", e);
        }

        return currentProfileView;
    }

}
