package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * These tests can not be executed in our standard unit test fashion as they
 * require a running RHQ Server with our web services deployed.
 * 
 * This is still in development and has the current restrictions: - add
 * [dev_root
 * ]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices
 * -{version}.jar to TOP of eclipse classpath to run from your IDE(actually need
 * to use classpath setup from bin/jbossas/bin/wsrunclient.sh to take advantage
 * of type substitution correctly) - Server running on localhost. - ws-test user
 * defined in database with full permissions - Non RHQ Server JBossAS in
 * inventory. - The -Ptest-ws profile specified when running mvn test from
 * webservices dir - Perftest plugin installed and agent started as described in
 * modules/enterprise/remoting/scripts/README.txt
 * 
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
public class WsConfigurationManagerTest extends AssertJUnit implements TestPropertiesInterface {

    private static ObjectFactory WS_OBJECT_FACTORY;
    private static WebservicesRemote WEBSERVICE_REMOTE;
    private static Subject subject = null;

    @BeforeClass
    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
        LoginException_Exception {

        // build reference variable bits
        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);

        WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
        WS_OBJECT_FACTORY = new ObjectFactory();
        WsSubjectTest.checkForWsTestUserAndRole();
        subject = WEBSERVICE_REMOTE.login(credentials, credentials);
    }

    @Test(enabled = TESTS_ENABLED)
    void testConfigurationManager() {
        // get config for JBossAS server
        String desc = "JBoss Application Server";
        ResourceCriteria resourceCriteria = WS_OBJECT_FACTORY.createResourceCriteria();
        resourceCriteria.setFilterDescription(desc);
        resourceCriteria.setFetchResourceConfiguration(true);
        resourceCriteria.setFetchPluginConfiguration(true);

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, resourceCriteria);
        assertNotNull("Unable to locate JBoss AS instances reference.", resources);
        assertTrue("Unable to find instances of JBoss AS.", resources.size() > 0);

        Resource resource = resources.get(0);
        //        Configuration configuration = WEBSERVICE_REMOTE
        WsConfiguration configuration = WEBSERVICE_REMOTE
        // JaxbConfiguration configuration = WEBSERVICE_REMOTE
            .getResourceConfiguration(subject, resource.getId());
        assertNotNull("Configuration was not located.", configuration);

        // TODO: verify configuration details

        boolean isUpdating = WEBSERVICE_REMOTE.isResourceConfigurationUpdateInProgress(subject, resource.getId());
        assertFalse("Config should not be in process of modification.", isUpdating);

        // Get plugin configuration information
        //        Configuration pluginConfig = WEBSERVICE_REMOTE.getPluginConfiguration(
        WsConfiguration pluginConfig = WEBSERVICE_REMOTE.getPluginConfiguration(
        // JaxbConfiguration pluginConfig =
            // WEBSERVICE_REMOTE.getPluginConfiguration(
            subject, resource.getId());
        assertNotNull("Configuration was not located.", pluginConfig);
        assertNotNull("The property definition map should not be null.", pluginConfig.getProperties());
        //            pluginConfig.getPropertyListOrPropertySimpleOrPropertyMap());
    }

    Resource findResource(String name, String parentName) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setFilterName(name);
        criteria.setFilterParentResourceName(parentName);

        return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria).get(0);
        //        WsResourceListWrapper ret = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
        //        ret.
    }

    Resource findService(String name, String parentName) {
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName(name);
        criteria.setFilterParentResourceName(parentName);

        return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria).get(0);
    }

    List<Resource> findBetaServices(String parentName) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setFilterParentResourceName(parentName);
        criteria.setFilterResourceTypeName("service-beta");
        criteria.caseSensitive = true;
        criteria.strict = true;

        return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
    }

    private Property locateProperty(List<Property> properties, String name) {
        Property located = null;
        if ((properties != null) && (name != null) && (name.trim().length() > 0)) {
            // List<Entry> list = properties.getEntry();
            for (int i = 0; (located == null) && i < properties.size(); i++) {
                Property entry = properties.get(i);
                if (entry.getName().equals(name)) {
                    located = entry;
                }
            }

        }
        return located;
    }

    private PropertySimple locatePropertySimple(List<PropertySimple> properties, String name) {
        PropertySimple located = null;
        if ((properties != null) && (name != null) && (name.trim().length() > 0)) {
            // List<Entry> list = properties.getEntry();
            for (int i = 0; (located == null) && i < properties.size(); i++) {
                PropertySimple entry = properties.get(i);
                if (entry.getName().equals(name)) {
                    located = entry;
                }
            }

        }
        return located;
    }

    private PropertySimple getSimple(List<PropertySimple> list, String name) {
        Property property = locatePropertySimple(list, name);
        return (PropertySimple) property;
    }

    private PropertySimple getSimpleConfig(List<Property> list, String name) {
        Property property = locateProperty(list, name);
        return (PropertySimple) property;
    }

    @Test(enabled = TESTS_ENABLED)
    void testUpdateResourceConfiguration() throws InterruptedException {

        Resource resource = findService("service-beta-1", "server-omega-1");
        //        Configuration config = WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id);
        WsConfiguration config = WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id);

        String propertyName = "beta-config0";
        String propertyValue = "updated property value -- " + new java.util.Date();

        //        property = config.getSimple(propertyName);
        //        PropertySimple property = getSimple(config.getPropertyListOrPropertySimpleOrPropertyMap(), propertyName);
        PropertySimple property = getSimple(config.getPropertySimpleContainer(), propertyName);

        property.setStringValue(propertyValue);
        //        config.put(property);
        //        config.getPropertyListOrPropertySimpleOrPropertyMap().add(property);
        config.getProperties().add(property);
        config.getPropertySimpleContainer().add(property);

        ResourceConfigurationUpdate configUpdate = WEBSERVICE_REMOTE.updateResourceConfiguration(subject, resource.id,
            config);

        while (WEBSERVICE_REMOTE.isResourceConfigurationUpdateInProgress(subject, resource.id)) {
            java.lang.Thread.sleep(1000);
        }

        config = WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id);
        //        updatedProperty = config.getSimple(propertyName);
        //        PropertySimple updatedProperty = getSimple(config.getPropertyListOrPropertySimpleOrPropertyMap(), propertyName);
        PropertySimple updatedProperty = getSimple(config.getPropertySimpleContainer(), propertyName);

        assertEquals("Failed to update resource configuration", updatedProperty.stringValue, propertyValue);
    }

    private Configuration convert(WsConfiguration config) {
        Configuration cfg = new Configuration();
        if (config != null) {
            cfg.ctime = config.ctime;
            cfg.id = config.id;
            cfg.mtime = config.mtime;
            cfg.notes = config.notes;
            cfg.version = config.version;
            cfg.propertyListOrPropertySimpleOrPropertyMap = new ArrayList<Property>();
            //            for (Property p : config.properties) {
            //                cfg.propertyListOrPropertySimpleOrPropertyMap.add(p);
            //            }
            if (config.propertySimpleContainer != null) {
                for (PropertySimple p : config.propertySimpleContainer) {
                    cfg.propertyListOrPropertySimpleOrPropertyMap.add(p);
                }
            }
            if (config.propertyListContainer != null) {
                for (PropertyList p : config.propertyListContainer) {
                    cfg.propertyListOrPropertySimpleOrPropertyMap.add(p);
                }
            }
            if (config.propertyMapContainer != null) {
                for (PropertyMap p : config.propertyMapContainer) {
                    cfg.propertyListOrPropertySimpleOrPropertyMap.add(p);
                }
            }

        } else {
            throw new IllegalArgumentException("WsConfiguration can not be null.");
        }
        return cfg;
    }

    @Test(enabled = TESTS_ENABLED)
    void testUpdatePluginConfiguration() {

        Resource resource = findService("service-beta-0", "server-omega-0");
        WsConfiguration pluginConfig = WEBSERVICE_REMOTE.getPluginConfiguration(subject, resource.id);
        //        Configuration pluginConfig = WEBSERVICE_REMOTE.getPluginConfiguration(subject, resource.id);

        String propertyName = "beta-property0";
        String propertyValue = "updated property value -- " + new java.util.Date();

        //        property = pluginConfig.getSimple(propertyName);
        //        PropertySimple property = getSimple(pluginConfig.getPropertyListOrPropertySimpleOrPropertyMap(), propertyName);
        //        PropertySimple property = getSimple(pluginConfig.getProperties(), propertyName);
        PropertySimple property = getSimple(pluginConfig.getPropertySimpleContainer(), propertyName);

        property.setStringValue(propertyValue);
        pluginConfig.propertySimpleContainer.add(property);
        pluginConfig.properties.add(property);

        PluginConfigurationUpdate configUpdate = WEBSERVICE_REMOTE.updatePluginConfiguration(subject, resource.id,
            pluginConfig);

        pluginConfig = WEBSERVICE_REMOTE.getPluginConfiguration(subject, resource.id);
        //        updatedProperty = pluginConfig.getSimple(propertyName);
        //        PropertySimple updatedProperty = getSimple(pluginConfig.getPropertyListOrPropertySimpleOrPropertyMap(),
        PropertySimple updatedProperty = getSimple(pluginConfig.getPropertySimpleContainer(), propertyName);

        assertEquals("Failed to update plugin configuration", updatedProperty.stringValue, propertyValue);

    }

    // This test is failing I think due to the asynchronous nature of the operations involved. I have verified through the
    // web UI that the configuration updates are actually happening. I just need to figure out how to make the test wait
    // so that it can (consistently) get the udpated values.
    // - jsanda
    @Test(enabled = TESTS_ENABLED)
    void testUpdateResourceGroupConfiguration() {
        String groupName = "service-beta-group -- " + new java.util.Date();
        ResourceGroup group = createResourceGroup(groupName);

        List<Resource> services = findBetaServices("server-omega-0");

        assertEquals("Failed to find beta services", services.size(), 10);

        addServicesToGroup(services, group);

        List<Configuration> configs = loadConfigs(services);

        assertEquals("Failed to load all resource configurations", configs.size(), 10);

        String propertyName = "beta-config0";
        String propertyValue = "updated property value -- " + new java.util.Date();

        updateResourceConfigs(configs, propertyValue);

        HashMap<Integer, Configuration> configMap = toMap(services, configs);

        //     updateId = WEBSERVICE_REMOTE.scheduleGroupResourceConfigurationUpdate(group.id, configMap);
        //
        //     while (WEBSERVICE_REMOTE.isGroupResourceConfigurationUpdateInProgress(updateId)) {
        //         java.lang.Thread.sleep(1000);
        //     }
        //
        //     var updatedConfigs = loadConfigs(services);
        //
        //     for (i in updatedConfigs) {
        //         var updatedProperty = updatedConfigs[i].getSimple(propertyName);
        //
        //         Assert.assertEquals(updatedProperty.stringValue, propertyValue, "Failed to update resource group configuration");
        //     }
    }

    ResourceGroup createResourceGroup(String name) {
        ResourceType resourceType = getResourceType("service-beta");
        assertNotNull("Failed to find resource type for new resource group.", resourceType);

        //        return WEBSERVICE_REMOTE.createResourceGroup(ResourceGroup(name, resourceType));
        ResourceGroup rg = new ResourceGroup();
        rg.setName(name);
        rg.setResourceType(resourceType);
        WsResourceGroupWrapper wsRg = WS_OBJECT_FACTORY.createWsResourceGroupWrapper();
        wsRg.setName(name);
        wsRg.setResourceType(resourceType);
        ResourceGroup resGroup = WEBSERVICE_REMOTE.createResourceGroup(subject, wsRg);
        return resGroup;
    }

    void addServicesToGroup(List<Resource> services, ResourceGroup group) {
        //        var serviceIds = [];
        List<Integer> serviceIds = new ArrayList<Integer>();

        for (Resource resource : services) {
            serviceIds.add(resource.getId());
        }

        WEBSERVICE_REMOTE.addResourcesToGroup(subject, group.id, serviceIds);
    }

    ResourceType getResourceType(String resourceTypeName) {
        String pluginName = "PerfTest";

        return WEBSERVICE_REMOTE.getResourceTypeByNameAndPlugin(subject, resourceTypeName, pluginName);
    }

    List<Configuration> loadConfigs(List<Resource> resources) {
        List<Configuration> configs = new ArrayList<Configuration>();

        for (Resource resource : resources) {
            //            configs.add(WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id));
            WsConfiguration wsCfg = WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id);
            Configuration cfg = convert(wsCfg);
            configs.add(cfg);
        }

        return configs;
    }

    void updateResourceConfigs(List<Configuration> configs, String propertyValue) {
        String propertyName = "beta-config0";
        PropertySimple property = null;

        for (Configuration config : configs) {
            //            property = configs[i].getSimple(propertyName);
            //            property = locateProperty(config.getPropertyListOrPropertySimpleOrPropertyMap(), propertyName);
            //            config.propertyListOrPropertySimpleOrP
            //            property = getSimple(config.getPropertyListOrPropertySimpleOrPropertyMap(), propertyName);
            property = getSimpleConfig(config.propertyListOrPropertySimpleOrPropertyMap, propertyName);
            //            property.stringValue = propertyValue;
            assertNotNull("Retrieved property should not be null.", property);
            property.stringValue = propertyValue;
            //            configs[i].put(property);
            config.getPropertyListOrPropertySimpleOrPropertyMap().add(property);
        }
    }

    HashMap<Integer, Configuration> toMap(List<Resource> services, List<Configuration> configs) {
        HashMap<Integer, Configuration> map = new java.util.HashMap<Integer, Configuration>();

        //        for (i = 0; i < services.size(); ++i) {
        //            map.put(java.lang.Integer(services.get(i).id), configs[i]);
        //        }
        for (int i = 0; i < services.size(); ++i) {
            map.put(services.get(i).id, configs.get(i));
        }

        return map;
    }
}
