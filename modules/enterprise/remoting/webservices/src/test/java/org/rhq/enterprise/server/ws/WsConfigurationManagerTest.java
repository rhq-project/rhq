package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
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
public class WsConfigurationManagerTest extends AssertJUnit implements
		TestPropertiesInterface {

	private static ObjectFactory WS_OBJECT_FACTORY;
	private static WebservicesRemote WEBSERVICE_REMOTE;
	private static Subject subject = null;

	@BeforeClass
	public void init() throws ClassNotFoundException, MalformedURLException,
			SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, LoginException_Exception {

		// build reference variable bits
		URL gUrl = WsUtility.generateRemoteWebserviceURL(
				WebservicesManagerBeanService.class, host, port, useSSL);
		QName gQName = WsUtility
				.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
		WebservicesManagerBeanService jws = new WebservicesManagerBeanService(
				gUrl, gQName);

		WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
		WS_OBJECT_FACTORY = new ObjectFactory();
		WsSubjectTest.checkForWsTestUserAndRole();
		subject = WEBSERVICE_REMOTE.login(credentials, credentials);
	}

	@Test(enabled = TESTS_ENABLED)
	void testConfigurationManager() {
		// get config for JBossAS server
		String desc = "JBoss Application Server";
		ResourceCriteria resourceCriteria = WS_OBJECT_FACTORY
				.createResourceCriteria();
		resourceCriteria.setFilterDescription(desc);
		resourceCriteria.setFetchResourceConfiguration(true);
		resourceCriteria.setFetchPluginConfiguration(true);

		List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(
				subject, resourceCriteria);
		assertNotNull("Unable to locate JBoss AS instances reference.",
				resources);
		assertTrue("Unable to find instances of JBoss AS.",
				resources.size() > 0);

		Resource resource = resources.get(0);
		Configuration configuration = WEBSERVICE_REMOTE
		// JaxbConfiguration configuration = WEBSERVICE_REMOTE
				.getResourceConfiguration(subject, resource.getId());
		assertNotNull("Configuration was not located.", configuration);

		// TODO: verify configuration details

		// Test get configuration
		Configuration configRetrieved = WEBSERVICE_REMOTE.getConfiguration(
		// JaxbConfiguration configRetrieved =
		// WEBSERVICE_REMOTE.getConfiguration(
				subject, configuration.getId());
		assertNotNull("Configuration was not located.", configRetrieved);
		assertEquals("Configuration information was not correct.",
				configuration.getVersion(), configRetrieved.getVersion());

		boolean isUpdating = WEBSERVICE_REMOTE
				.isResourceConfigurationUpdateInProgress(subject, resource
						.getId());
		assertFalse("Config should not be in process of modification.",
				isUpdating);

		// Get plugin configuration information
		Configuration pluginConfig = WEBSERVICE_REMOTE.getPluginConfiguration(
		// JaxbConfiguration pluginConfig =
		// WEBSERVICE_REMOTE.getPluginConfiguration(
				subject, resource.getId());
		assertNotNull("Configuration was not located.", configRetrieved);
		assertNotNull("The property definition map should not be null.",
		// pluginConfig.getProperties());
				pluginConfig.getPropertyListOrPropertySimpleOrPropertyMap());
	}

	// @Test(enabled = TESTS_ENABLED)
	// void testUpdateResourceConfiguration() {
	// Resource resource = findService("service-beta-1", "server-omega-1");
	// Configuration config =
	// WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id);
	//
	// String propertyName = "beta-config0";
	// String propertyValue = "updated property value -- " + new
	// java.util.Date();
	//
	// // property = config.getSimple(propertyName);
	// Property property =
	// locateProperty(config.getPropertyListOrPropertySimpleOrPropertyMap(),
	// propertyName);
	//
	// property = (PropertySimple)property;
	// property.setStringValue(propertyValue);
	// config.put(property);
	//
	// var configUpdate =
	// ConfigurationManager.updateResourceConfiguration(resource.id, config);
	//
	// while
	// (ConfigurationManager.isResourceConfigurationUpdateInProgress(resource.id))
	// {
	// java.lang.Thread.sleep(1000);
	// }
	//
	// config = ConfigurationManager.getResourceConfiguration(resource.id);
	// var updatedProperty = config.getSimple(propertyName);
	//
	// Assert.assertEquals(updatedProperty.stringValue, propertyValue,
	// "Failed to update resource configuration");
	// }
	//
	// @Test(enabled = TESTS_ENABLED)
	// void testUpdatePluginConfiguration() {
	// var resource = findService('service-beta-0', 'server-omega-0');
	// var pluginConfig =
	// ConfigurationManager.getPluginConfiguration(resource.id);
	//
	// var propertyName = 'beta-property0';
	// var propertyValue = 'updated property value -- ' + java.util.Date();
	//
	// var property = pluginConfig.getSimple(propertyName);
	//
	// property.setStringValue(propertyValue);
	//
	// var configUpdate =
	// ConfigurationManager.updatePluginConfiguration(resource.id,
	// pluginConfig);
	//
	// pluginConfig = ConfigurationManager.getPluginConfiguration(resource.id);
	// var updatedProperty = pluginConfig.getSimple(propertyName);
	//
	// Assert.assertEquals(updatedProperty.stringValue, propertyValue, 'Failed
	// to update plugin configuration');
	// }
	//
	// // This test is failing I think due to the asynchronous nature of the
	// operations involved. I have verified through the
	// // web UI that the configuration updates are actually happening. I just
	// need to figure out how to make the test wait
	// // so that it can (consistently) get the udpated values.
	// // - jsanda
	// @Test(enabled = TESTS_ENABLED)
	// void testUpdateResourceGroupConfiguration() {
	// var groupName = 'service-beta-group -- ' + java.util.Date();
	// var group = createResourceGroup(groupName);
	//
	// var services = findBetaServices('server-omega-0');
	//
	// Assert.assertNumberEqualsJS(services.size(), 10, 'Failed to find beta
	// services');
	//
	// addServicesToGroup(services, group);
	//
	// var configs = loadConfigs(services);
	//
	// Assert.assertNumberEqualsJS(configs.length, 10, 'Failed to load all
	// resource configurations');
	//
	// var propertyName = 'beta-config0';
	// var propertyValue = 'updated property value -- ' + java.util.Date();
	//
	// updateResourceConfigs(configs, propertyValue);
	//
	// var configMap = toMap(services, configs);
	//
	// var updateId =
	// ConfigurationManager.scheduleGroupResourceConfigurationUpdate(group.id,
	// configMap);
	//
	// while
	// (ConfigurationManager.isGroupResourceConfigurationUpdateInProgress(updateId))
	// {
	// java.lang.Thread.sleep(1000);
	// }
	//
	// var updatedConfigs = loadConfigs(services);
	//
	// for (i in updatedConfigs) {
	// var updatedProperty = updatedConfigs[i].getSimple(propertyName);
	//
	// Assert.assertEquals(updatedProperty.stringValue, propertyValue,
	// "Failed to update resource group configuration");
	// }
	// }
	//
	// findService(name, parentName) {
	// var criteria = ResourceCriteria();
	// criteria.addFilterName(name);
	// criteria.addFilterParentResourceName(parentName);
	//
	// return ResourceManager.findResourcesByCriteria(criteria).get(0);
	// }
	//
	// findBetaServices(parentName) {
	// var criteria = ResourceCriteria();
	// criteria.addFilterParentResourceName(parentName);
	// criteria.addFilterResourceTypeName('service-beta');
	// criteria.caseSensitive = true;
	// criteria.strict = true;
	//
	// return ResourceManager.findResourcesByCriteria(criteria);
	// }
	//
	// createResourceGroup(name) {
	// var resourceType = getResourceType('service-beta');
	// Assert.assertNotNull(resourceType, 'Failed to find resource type for new
	// resource group.');
	//
	// return ResourceGroupManager.createResourceGroup(ResourceGroup(name,
	// resourceType));
	// }
	//
	// addServicesToGroup(services, group) {
	// var serviceIds = [];
	//
	// for (i = 0; i < services.size(); ++i) {
	// serviceIds.push(services.get(i).id);
	// }
	//
	// ResourceGroupManager.addResourcesToGroup(group.id, serviceIds);
	// }
	//
	// getResourceType(resourceTypeName) {
	// var pluginName = 'PerfTest';
	//
	// return
	// ResourceTypeManager.getResourceTypeByNameAndPlugin(resourceTypeName,
	// pluginName);
	// }
	//
	// loadConfigs(resources) {
	// var configs = [];
	//
	// for (i = 0; i < resources.size(); ++i) {
	// configs.push(ConfigurationManager.getResourceConfiguration(resources.get(i).id));
	// }
	//
	// return configs;
	// }
	//
	// // updateResourceConfigs(configs, propertyValue) {
	// // var propertyName = "beta-config0";
	// // var property = null;
	// //
	// // for (int i = 0; in configs) {
	// // property = configs[i].getSimple(propertyName);
	// // property.stringValue = propertyValue;
	// // configs[i].put(property);
	// // }
	// // }
	//
	// toMap(services, configs) {
	// var map = java.util.HashMap();
	//
	// for (i = 0; i < services.size(); ++i) {
	// map.put(java.lang.Integer(services.get(i).id), configs[i]);
	// }
	//
	// return map;
	// }

	// //#################################################

	// @Test(enabled = TESTS_ENABLED)
	// void testUpdateResourceConfiguration() {
	// Resource resource = findResource("service-beta-0", "server-omega-0");
	// Configuration config =
	// WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id);
	//
	// String propertyName = "beta-config0";
	// String propertyValue = "updated property value -- " + new
	// java.util.Date();
	//
	// List<Property> bag =
	// config.getPropertyListOrPropertySimpleOrPropertyMap();
	//        	
	// property = config.getSimple(propertyName);
	//
	// property.setStringValue(propertyValue);
	//
	// ResourceConfigurationUpdate configUpdate =
	// WEBSERVICE_REMOTE.updateResourceConfiguration(subject, resource.id,
	// config);
	//
	// while (WEBSERVICE_REMOTE.isResourceConfigurationUpdateInProgress(subject,
	// resource.id)) {
	// java.lang.Thread.sleep(1000);
	// }
	//
	// config = WEBSERVICE_REMOTE.getResourceConfiguration(subject,
	// resource.id);
	// var updatedProperty = config.getSimple(propertyName);
	//
	// assertEquals("Failed to update resource configuration",updatedProperty.stringValue,
	// propertyValue);
	// }

	// @Test(enabled = TESTS_ENABLED)
	// Resource findResource(String name, String parentName) {
	// ResourceCriteria criteria = new ResourceCriteria();
	// criteria.setFilterName(name);
	// criteria.setFilterParentResourceName(parentName);
	//
	// return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria)
	// .get(0);
	// }

	// @Test(enabled = TESTS_ENABLED)
	// void testUpdateResourceConfiguration() {
	// Resource resource = findService("service-beta-1", "server-omega-1");
	// Configuration config = WEBSERVICE_REMOTE.getResourceConfiguration(
	// subject, resource.id);
	//
	// // var interfaces = config.getClass().interfaces;
	// // for (i = 0; i < interfaces.length; ++i) {
	// // println(interfaces[i].getName());
	// // }
	// //
	// // if (true) return;
	//
	// String propertyName = "beta-config0";
	// String propertyValue = "updated property value -- "
	// + new java.util.Date();
	//
	// // property = config.getSimple(propertyName);
	// List<Property> properties = config
	// .getPropertyListOrPropertySimpleOrPropertyMap();
	// assertNotNull("PropertyDefinitions have not been populated.",
	// properties);
	// PropertySimple property = (PropertySimple) locateProperty(properties,
	// propertyName);
	// // property = getSimple(properties, propertyName);
	//
	// property.setStringValue(propertyValue);
	// // config.put(property);
	// List<Property> propertyContainer =
	// config.getPropertyListOrPropertySimpleOrPropertyMap();
	// assertNotNull("PropertyContainer was null.",propertyContainer);
	// propertyContainer.add(property);
	//
	// ResourceConfigurationUpdate configUpdate =
	// WEBSERVICE_REMOTE.updateResourceConfiguration(
	// subject, resource.id, config);
	//
	// while (WEBSERVICE_REMOTE
	// .isResourceConfigurationUpdateInProgress(subject, resource.id)) {
	// java.lang.Thread.sleep(1000);
	// }
	//
	// config = WEBSERVICE_REMOTE.getResourceConfiguration(subject,
	// resource.id);
	// // updatedProperty = config.getSimple(propertyName);
	// updatedProperty = config.getSimple(propertyName);
	//
	// Assert.assertEquals(updatedProperty.stringValue, propertyValue,
	// "Failed to update resource configuration");
	// }
	//
	// @Test(enabled = TESTS_ENABLED)
	// function testUpdatePluginConfiguration() {
	// var resource = findService('service-beta-0', 'server-omega-0');
	// var pluginConfig =
	// ConfigurationManager.getPluginConfiguration(resource.id);
	//
	// var propertyName = 'beta-property0';
	// var propertyValue = 'updated property value -- ' + java.util.Date();
	//
	// var property = pluginConfig.getSimple(propertyName);
	//
	// property.setStringValue(propertyValue);
	//
	// var configUpdate =
	// ConfigurationManager.updatePluginConfiguration(resource.id,
	// pluginConfig);
	//
	// pluginConfig = ConfigurationManager.getPluginConfiguration(resource.id);
	// var updatedProperty = pluginConfig.getSimple(propertyName);
	//
	// Assert.assertEquals(updatedProperty.stringValue, propertyValue, 'Failed
	// to update plugin configuration');
	// }
	//
	// @Test(enabled = TESTS_ENABLED)
	// function testUpdateResourceGroupConfiguration() {
	// var groupName = 'service-beta-group -- ' + java.util.Date();
	// var group = createResourceGroup(groupName);
	//
	// var services = findBetaServices('server-omega-0');
	//
	// Assert.assertNumberEqualsJS(services.size(), 10, 'Failed to find beta
	// services');
	//
	// var configs = loadConfigs(services);
	//
	// Assert.assertNumberEqualsJS(configs.length, 10, 'Failed to load all
	// resource configurations');
	// }

	// @Test(enabled = TESTS_ENABLED)
	// void testUpdateResourceConfiguration() {
	// Resource resource = findResource("service-beta-0", "server-omega-0");
	// Configuration config =
	// WEBSERVICE_REMOTE.getResourceConfiguration(subject, resource.id);
	//
	// String propertyName = "beta-config0";
	// String propertyValue = "updated property value -- " + new
	// java.util.Date();
	//
	// List<Property> bag =
	// config.getPropertyListOrPropertySimpleOrPropertyMap();
	//        	
	// property = config.getSimple(propertyName);
	//
	// property.setStringValue(propertyValue);
	//
	// ResourceConfigurationUpdate configUpdate =
	// WEBSERVICE_REMOTE.updateResourceConfiguration(subject, resource.id,
	// config);
	//
	// while (WEBSERVICE_REMOTE.isResourceConfigurationUpdateInProgress(subject,
	// resource.id)) {
	// java.lang.Thread.sleep(1000);
	// }
	//
	// config = WEBSERVICE_REMOTE.getResourceConfiguration(subject,
	// resource.id);
	// var updatedProperty = config.getSimple(propertyName);
	//
	// assertEquals("Failed to update resource configuration",updatedProperty.stringValue,
	// propertyValue);
	// }

	Resource findResource(String name, String parentName) {
		ResourceCriteria criteria = new ResourceCriteria();
		criteria.setFilterName(name);
		criteria.setFilterParentResourceName(parentName);

		return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria)
				.get(0);
	}

	Resource findService(String name, String parentName) {
		ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
		criteria.setFilterName(name);
		criteria.setFilterParentResourceName(parentName);

		return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria)
				.get(0);
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
		if ((properties != null) && (name != null)
				&& (name.trim().length() > 0)) {
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
}
