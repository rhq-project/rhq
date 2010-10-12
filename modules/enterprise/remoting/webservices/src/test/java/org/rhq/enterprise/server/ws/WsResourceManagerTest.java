package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
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
public class WsResourceManagerTest extends AssertJUnit implements TestPropertiesInterface {

    // Test variables
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
    public void testResourceManager() throws java.lang.Exception {
        // define search term
        // String searchTerm = "RHQ AGENT";
        String searchTerm = "server-omega";

        // build criteria
        // Subject subject = WEBSERVICE_REMOTE.login(credentials, credentials);
        ResourceCriteria resourceCriteria = WS_OBJECT_FACTORY.createResourceCriteria();
        List<Resource> results = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, resourceCriteria);
        assertNotNull("Results not located correctly", results);

        // without filter term, should be get *
        int totalResourcesLocated = results.size();
        // check for uninitialized server
        assertTrue("Your server does not appear to be initialized. Resource count == 0.", (totalResourcesLocated > 0));

        // add criterion .. and resubmit
        resourceCriteria.setFilterName(searchTerm);
        results = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, resourceCriteria);
        assertNotNull("Results not located correctly", results);
        assertTrue("Criteria did not filter properly.", (totalResourcesLocated > results.size()));

        // Test getResource
        Resource resource = WEBSERVICE_REMOTE.getResource(subject, results.get(0).getId());
        assertNotNull("Resource by id was null.", resource);
        assertEquals("Resource ids not matching.", resource.getId(), results.get(0).getId());
        assertEquals("Resource names not matching.", resource.getName(), results.get(0).getName());

    }

    // function testFindUnfiltered() {
    @Test(enabled = TESTS_ENABLED)
    public void testUnfilteredFind() {

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, WS_OBJECT_FACTORY
            .createResourceCriteria());

        assertNotNull("Expected non-null results for criteria search.", resources);
        assertTrue("Expected non-empty result list.", resources.size() > 0);
    }

    // function testFindWithFiltering() {
    @Test(enabled = TESTS_ENABLED)
    public void testFindWithFiltering() throws SecurityException, IllegalArgumentException, NoSuchMethodException,
        IllegalAccessException, InvocationTargetException {
        ResourceCriteria criteria = createCriteria();
        //         TODO: fix default criteria
        criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName("service-alpha-0");
        criteria.setFilterParentResourceName("server-omega-2");
        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertEquals("Expected to get back a single resource", 1, resources.size());

    }

    // function testFindWithOptionalFiltering() {
    @Test(enabled = TESTS_ENABLED)
    public void testFindWithOptionalFiltering() throws SecurityException, IllegalArgumentException,
        NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        ResourceCriteria criteria = createCriteria();
        criteria.filtersOptional = true;
        criteria.setFilterParentResourceName("_does_not_exist_");

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertTrue("Expected to find resources when filtering made optional", resources.size() > 0);

    }

    // function testFindWithFilteringAndFetchingAssociations() {
    public void testFindWithFilteringAndFetchingAssociations() {
        // var criteria = createCriteria();
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.strict = true;
        criteria.setFilterParentResourceName("server-omega-0");
        criteria.setFilterResourceTypeName("service-beta");
        criteria.setFetchAgent(true);
        criteria.setFetchAlertDefinitions(true);
        criteria.setFetchResourceType(true);
        criteria.setFetchChildResources(true);
        criteria.setFetchParentResource(true);
        criteria.setFetchResourceConfiguration(true);
        criteria.setFetchResourceErrors(true);
        criteria.setFetchPluginConfigurationUpdates(true);
        criteria.setFetchImplicitGroups(true);
        criteria.setFetchExplicitGroups(true);
        criteria.setFetchOperationHistories(true);

        criteria.setSortName(PageOrdering.DESC);
        criteria.setSortResourceTypeName(PageOrdering.ASC);
        criteria.setSortInventoryStatus(PageOrdering.DESC);
        criteria.setSortVersion(PageOrdering.DESC);
        criteria.setSortResourceCategory(PageOrdering.ASC);

        // var resources = ResourceManager.findResourcesByCriteria(criteria);
        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        //        assertEquals("Expected to get back a single resource", 1, resources.size());
        assertTrue("Expected to get back resource list", (resources.size() > 0));
        Resource resource = resources.get(0);
        assertNotNull("resource.agent should have been loaded", resource.agent);
        //        assertNotNull("resource.alertDefinitions should have been loaded", resource.alertDefinitions);
        assertNotNull("resource.resourceType should have been loaded", resource.resourceType);
        //        assertNotNull("resource.childResources should have been loaded", resource.childResources);
        // TODO: following resource removed because it caused cycle, need to replace with wrapper approach
        //        assertNotNull("resource.parentResource should have been loaded", resource.parentResource);
        assertNotNull("resource.resourceConfiguration should have been loaded", resource.resourceConfiguration);
        //        TODO: following resource removed because it caused cycle, need to replace with wrapper
        //        assertNotNull("resource.resourceErrors should have been loaded", resource.resourceErrors);
        //        assertNotNull("resource.pluginConfigurationUpdates should have been loaded",
        //            resource.pluginConfigurationUpdates);
        //        assertNotNull("resource.implicitGroups should have been loaded", resource.implicitGroups);
        //        assertNotNull("reosurce.explicitGroups should have been loaded", resource.explicitGroups);
        //        assertNotNull("resource.operationHistories should have been loaded", resource.operationHistories);
    }

    // function testSortBySingleProperty() {
    @Test(enabled = TESTS_ENABLED)
    public void testSortBySingleProperty() {

        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.caseSensitive = true;
        criteria.setFilterParentResourceName("server-omega-0");
        // criteria.setFilterResourceTypeName("service-beta");
        criteria.setSortName(PageOrdering.DESC);

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertTrue("Expected to get back resources when sorting by a single property, resource.name",
            resources.size() > 0);

        // TODO verify resources are actually sorted

    }

    @Test(enabled = TESTS_ENABLED)
    public void testSortByMultipleProperties() {

        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.caseSensitive = true;
        criteria.setFilterParentResourceName("server-omega-0");
        // TODO: ?? why works when disabling resourceType?
        // criteria.setFilterResourceTypeName("service-beta");

        criteria.setSortName(PageOrdering.DESC);
        criteria.setSortResourceTypeName(PageOrdering.DESC);
        criteria.setSortInventoryStatus(PageOrdering.DESC);
        criteria.setSortVersion(PageOrdering.DESC);
        criteria.setSortResourceCategory(PageOrdering.DESC);

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertTrue("Expected to get resources when sorting by multiple proerties.", resources.size() > 0);

    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindResourceLineage2() {

        // create criteria
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName("RHQ AGENT JVM");
        criteria.setFilterParentResourceName("RHQ AGENT");

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
        assertNotNull("Unable to locate resources.", resources);
        assertTrue("Unable to locate correct number of resources.", resources.size() > 0);
        Resource resource = resources.get(0);

        // resources = WEBSERVICE_REMOTE.findResourceLineage(subject,
        // resource.id);
        WsResourceListWrapper resourcesList = WEBSERVICE_REMOTE.findResourceLineage(subject, resource.id);

        // System.out.println("NAME:"+resources.get(0).getName()+":"+resources.get(0).getName()+":"+resources.get(0).getUuid());
        // System.out.println("NAME:"+resources.get(1).getName()+":"+resources.get(1).getName()+":"+resources.get(1).getUuid());
        // System.out.println("NAME:"+resources.get(2).getName()+":"+resources.get(2).getName()+":"+resources.get(2).getUuid());

    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindResourceLineage() {

        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName("service-alpha-0");
        criteria.setFilterParentResourceName("server-omega-0");

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);
        Resource resource = resources.get(0);

        WsResourceListWrapper resourcesList = WEBSERVICE_REMOTE.findResourceLineage(subject, resource.id);

        resources = resourcesList.getLineageList();

        assertEquals("The wrong resource lineage returned for resource " + resource, resources.size(), 3);
        // assertEquals("The wrong root resource was returned",
        // "localhost.localdomain", resources.get(0).name);
        // assertEquals("The wrong root resource was returned", "Vital-AGENT",
        // resources.get(0).name);
        // lookup the first platform resource and use that name. More stable
        // than hard coding. Almost always one.
        ResourceCriteria platCriteria = new ResourceCriteria();
        platCriteria.setFilterResourceCategory(ResourceCategory.PLATFORM);
        List<Resource> platformList = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, platCriteria);
        assertNotNull("Platform list should not be null.", platformList);
        assertEquals("Should be only one in list for testing.", 1, platformList.size());
        assertEquals("The wrong root resource was returned", platformList.get(0).getName(), resources.get(0).name);
        assertTrue("The root resource was null.", (resources.get(0).getName() != null));
        assertEquals("The wrong parent resource was returned", resources.get(1).name, "server-omega-0");
        assertEquals("The last resource in the lineage is wrong", resources.get(2).name, "service-alpha-0");

    }

    ResourceCriteria createCriteria() throws SecurityException, IllegalArgumentException, NoSuchMethodException,
        IllegalAccessException, InvocationTargetException {
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.caseSensitive = true;
        addFilters(criteria);

        return criteria;
    }

    void addFilters(ResourceCriteria criteria) throws SecurityException, NoSuchMethodException,
        IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        HashMap<String, String> filters = getFilters();
        String[] keyArray = new String[filters.size()];
        keyArray = filters.keySet().toArray(keyArray);
        for (int i = 0; i < filters.size(); ++i) {
            String key = keyArray[i];
            Method method = criteria.getClass().getMethod("setFilter" + key, String.class);
            method.invoke(criteria, filters.get(key));
        }
    }

    HashMap<String, String> getFilters() {
        String resourceName = "service-alpha-0";

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Name", resourceName);
        map.put("ParentResourceName", "server-omega-0");
        map.put("ResourceKey", resourceName);
        map.put("Description", resourceName + " description");
        map.put("PluginName", "PerfTest");
        map.put("Version", "1.0");
        map.put("AgentName", "Vital-AGENT");
        // No setting agent name as this is too variable to put into a test
        // setup? I set mine on agent startup.
        // map.put("AgentName", "localhost.localdomain");
        return map;
    }

}
