package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
 * [dev_root]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices
 * -{version}.jar to TOP of eclipse classpath to run from your IDE(actually need to use 
 *  classpath setup from bin/jbossas/bin/wsrunclient.sh to take advantage of type
 *  substitution correctly) 
 * - Server running on localhost. 
 * - ws-test user defined in database with full permissions 
 * - Non RHQ Server JBossAS in inventory. 
 * - The -Ptest-ws profile specified when running mvn test from webservices dir 
 * - Perftest plugin installed and agent started as described in 
 *    modules/enterprise/remoting/scripts/README.txt
 * 
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
public class WsResourceGroupManagerTest extends AssertJUnit implements
		TestPropertiesInterface {

	// Test variables
	// private static final boolean TESTS_ENABLED = true;
	// protected static String credentials = "ws-test";
	// protected static String host = "127.0.0.1";
	// protected static int port = 7080;
	// protected static boolean useSSL = false;
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
	void testCreateAndDeleteResourceGroup()
			throws ResourceGroupDeleteException_Exception {
		ResourceGroup resourceGroup = createResourceGroup();

		assertFalse("Failed to create resource group", resourceGroup.id == 0);

		WEBSERVICE_REMOTE.deleteResourceGroup(subject, resourceGroup.id);

		java.lang.Exception exception = null;
		try {
			WEBSERVICE_REMOTE.getResourceGroup(subject, resourceGroup.id);
		} catch (java.lang.Exception e) {
			exception = e;
		}
		assertNotNull("Failed to delete resource group", exception);
	}

	@Test(enabled = TESTS_ENABLED)
	void testAddResourcesToGroup() {
		ResourceGroup resourceGroup = createResourceGroup();

		assertFalse(
				"Cannot add resources to group. Failed to create resource group.",
				resourceGroup.id == 0);

		List<Resource> resources = findAlphaServices();
		assertEquals(
				"Cannot add resources to group. Failed to find the correct number of resources.",
				resources.size(), 10);

		addResourcesToGroup(resourceGroup, resources);

		ResourceGroupCriteria criteria = new ResourceGroupCriteria();
		criteria.setFilterId(resourceGroup.id);
		criteria.setFetchExplicitResources(true);

		resourceGroup = WEBSERVICE_REMOTE.findResourceGroupsByCriteria(subject,
				criteria).get(0);

		assertEquals(
				"Failed to find resources in group. Resources may not have been added.",
				resourceGroup.explicitResources.size(), 10);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithFiltering() {
		ResourceGroup resourceGroup = createResourceGroup();
		List<Resource> resources = findAlphaServices();
		Resource resource = resources.get(0);

		addResourcesToGroup(resourceGroup, resources);

		ResourceGroupCriteria criteria = new ResourceGroupCriteria();
		criteria.setFilterId(resourceGroup.id);
		criteria.setFilterPluginName("PerfTest");
		criteria.setFilterResourceTypeId(resource.resourceType.id);
		criteria.setFilterResourceTypeName(resource.resourceType.name);
		criteria.setFilterName(resourceGroup.name);
		criteria.setFilterGroupCategory(GroupCategory.COMPATIBLE);
		// criteria.addFilterExplicitResourceIds(getIds(resources));
		criteria.filterExplicitResourceIds = getIds(resources);

		List<ResourceGroup> resourceGroups = WEBSERVICE_REMOTE
				.findResourceGroupsByCriteria(subject, criteria);

		assertEquals("Failed to find resource groups when applying filters.",
				resourceGroups.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithFetchingAssociations() {
		ResourceGroup resourceGroup = createResourceGroup();
		List<Resource> resources = findAlphaServices();

		addResourcesToGroup(resourceGroup, resources);

		ResourceGroupCriteria criteria = new ResourceGroupCriteria();
		criteria.setFilterId(resourceGroup.id);
		criteria.setFetchExplicitResources(true);
		criteria.setFetchImplicitResources(true);
		criteria.setFetchOperationHistories(true);
		criteria.setFetchConfigurationUpdates(true);
		criteria.setFetchGroupDefinition(true);
		criteria.setFetchResourceType(true);

		List<ResourceGroup> resourceGroups = WEBSERVICE_REMOTE
				.findResourceGroupsByCriteria(subject, criteria);

		assertEquals(
				"Failed to find resource groups when fetching associations.",
				resourceGroups.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithSorting() {
		ResourceGroup resourceGroup = createResourceGroup();
		List<Resource> resources = findAlphaServices();

		addResourcesToGroup(resourceGroup, resources);

		ResourceGroupCriteria criteria = new ResourceGroupCriteria();
		criteria.setSortName(PageOrdering.ASC);
		criteria.setSortResourceTypeName(PageOrdering.DESC);

		List<ResourceGroup> resourceGroups = WEBSERVICE_REMOTE
				.findResourceGroupsByCriteria(subject, criteria);

		assertTrue("Failed to find resource groups when sorting",
				resourceGroups.size() > 0);
	}

	ResourceGroup createResourceGroup() {
		ResourceType resourceType = getResourceType();
		assertNotNull("Failed to find resource type for new resource group.",
				resourceType);

		String groupName = "test-group-" + new java.util.Date().getTime();
		ResourceGroup resGroup = new ResourceGroup();
		resGroup.setName(groupName);
		resGroup.setResourceType(resourceType);

		return WEBSERVICE_REMOTE.createResourceGroup(subject, resGroup);
	}

	ResourceType getResourceType() {
		String resourceTypeName = "service-alpha";
		String pluginName = "PerfTest";

		return WEBSERVICE_REMOTE.getResourceTypeByNameAndPlugin(subject,
				resourceTypeName, pluginName);
	}

	List<Resource> findAlphaServices() {
		ResourceCriteria criteria = new ResourceCriteria();
		criteria.caseSensitive = true;
		criteria.strict = true;
		criteria.setFilterParentResourceName("server-omega-0");
		criteria.setFilterResourceTypeName("service-alpha");
		criteria.setFetchResourceType(true);

		return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
	}

	void addResourcesToGroup(ResourceGroup group, List<Resource> resources) {
		List<Integer> resourceIds = getIds(resources);
		WEBSERVICE_REMOTE.addResourcesToGroup(subject, group.id, resourceIds);
	}

	List<Integer> getIds(List<Resource> resources) {
		// var ids = [];
		List<Integer> ids = new ArrayList<Integer>();

		for (int i = 0; i < resources.size(); ++i) {
			// ids.push(resources.get(i).id);
			ids.add(resources.get(i).getId());
		}

		return ids;
	}

	// public void testResourceGroup() throws java.lang.Exception {
	//    	
	// assertNotNull("Webservice Remote is null.",WEBSERVICE_REMOTE);
	// assertNotNull("JAXB ObjectFactory is null.",WS_OBJECT_FACTORY);
	// assertNotNull("You have not logged in successfully.",subject);
	//    	
	// //locate group
	// ResourceGroupCriteria groupCriteria =
	// WS_OBJECT_FACTORY.createResourceGroupCriteria();
	// groupCriteria.setFilterName("All Perf Test Servers");
	// List<ResourceGroup> groups =
	// WEBSERVICE_REMOTE.findResourceGroupsByCriteria(subject, groupCriteria);
	// assertNotNull("The ResourceGroup reference was null.",groups);
	// assertTrue("Group was not located.",groups.size()>0);
	// //
	//    	
	// }
}
