package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
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
 * This is still in development and has the current restrictions:
 * - add [dev_root]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices-{version}.jar 
 *    to TOP of IDE classpath for development/testing. 
 * - Server running on localhost. 
 * - ws-test user defined in database with full permissions 
 * - Non RHQ Server JBossAS in inventory. 
 * - The ws.test.package-path and ws.test.package-version environment 
 *   variables must be defined to a test .war file.
 * 
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
public class WsOperationManagerTest extends AssertJUnit implements TestPropertiesInterface {

    //Test variables
    //    private static final boolean TESTS_ENABLED = true;
    //    protected static String credentials = "ws-test";
    //    protected static String host = "127.0.0.1";
    //    protected static int port = 7080;
    //    protected static boolean useSSL = false;
    private static ObjectFactory WS_OBJECT_FACTORY;
    private static WebservicesRemote WEBSERVICE_REMOTE;
    private static Subject subject = null;

    @BeforeClass
    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
        LoginException_Exception {

        //build reference variable bits
        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);

        WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
        WS_OBJECT_FACTORY = new ObjectFactory();
        WsSubjectTest.checkForWsTestUserAndRole();
        subject = WEBSERVICE_REMOTE.login(credentials, credentials);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindOperationDefinitionsUnfiltered() {
        OperationDefinitionCriteria opDefCrit = WS_OBJECT_FACTORY.createOperationDefinitionCriteria();
        List<OperationDefinition> operationDefinitions = WEBSERVICE_REMOTE.findOperationDefinitionsByCriteria(subject,
            opDefCrit);

        assertNotNull("Expected non-null results for criteria search of operation definitions", operationDefinitions);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindSingleOperationDefinitionsWithFiltering() {
        OperationDefinitionCriteria criteria = WS_OBJECT_FACTORY.createOperationDefinitionCriteria();
        criteria.strict = true;
        criteria.setFilterName("start");
        criteria.setFilterDisplayName("Start");
        criteria
            .setFilterDescription("Start this application server. The script used is specified in the Operations group of connection properties.");
        criteria.setFilterPluginName("JBossAS");
        criteria.setFilterResourceTypeName("JBossAS Server");

        List<OperationDefinition> opDefinitions = WEBSERVICE_REMOTE.findOperationDefinitionsByCriteria(subject,
            criteria);

        assertEquals("Expected to get back a single operation definition that "
            + "corresponds to the Start operation but got back, '" + getNames(opDefinitions) + "'", opDefinitions
            .size(), 1);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindOperationDefinitionsWithOptionalFiltering() {
        OperationDefinitionCriteria criteria = WS_OBJECT_FACTORY.createOperationDefinitionCriteria();
        criteria.filtersOptional = true;

        criteria.setFilterDisplayName("Start");
        criteria.setFilterDescription("_non-existent description_");
        criteria.setFilterPluginName("JBossAS");
        criteria.setFilterResourceTypeName("JBossAS Server");

        List<OperationDefinition> opDefinitions = WEBSERVICE_REMOTE.findOperationDefinitionsByCriteria(subject,
            criteria);

        assertTrue("Expected non-empty result list for criteria search with optional "
            + "filters for operation definitions", opDefinitions.size() > 0);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindSingleOperationDefinitionWithFilteringAndFetchingAssociations() {
        OperationDefinitionCriteria criteria = WS_OBJECT_FACTORY.createOperationDefinitionCriteria();
        criteria.strict = true;
        criteria.setFilterPluginName("JBossAS");
        criteria.setFilterName("start");
        criteria.setFilterDisplayName("Start");
        criteria.setFilterResourceTypeName("JBossAS Server");
        criteria.setFetchParametersConfigurationDefinition(true);
        criteria.setFetchParametersConfigurationDefinition(true);

        List<OperationDefinition> opDefinitions = WEBSERVICE_REMOTE.findOperationDefinitionsByCriteria(subject,
            criteria);

        assertEquals("Expected to get back one operation definition when "
            + "filtering and fetching associations but got back, '" + getNames(opDefinitions) + "'", opDefinitions
            .size(), 1);

        OperationDefinition opDefinition = opDefinitions.get(0);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindMultipleOperationDefinitionsWithFilteringAndFetchingAssociations() {
        OperationDefinitionCriteria criteria = WS_OBJECT_FACTORY.createOperationDefinitionCriteria();
        criteria.strict = true;
        criteria.setFilterPluginName("JBossAS");
        criteria.setFilterResourceTypeName("JBossAS Server");
        criteria.setFetchParametersConfigurationDefinition(true);
        criteria.setFetchParametersConfigurationDefinition(true);

        List<OperationDefinition> opDefinitions = WEBSERVICE_REMOTE.findOperationDefinitionsByCriteria(subject,
            criteria);

        assertEquals("Expected to get back three operation definitions when "
            + "filtering and fetching associations but got back, '" + getNames(opDefinitions) + "'", opDefinitions
            .size(), 3);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindMultipleOperationDefinitionsWithSorting() {
        OperationDefinitionCriteria criteria = WS_OBJECT_FACTORY.createOperationDefinitionCriteria();
        criteria.setFilterPluginName("JBossAS");
        criteria.setFilterResourceTypeName("JBossAS Server");
        criteria.setFetchParametersConfigurationDefinition(true);
        criteria.setFetchResultsConfigurationDefinition(true);
        criteria.setSortName(PageOrdering.DESC);

        List<OperationDefinition> opDefinitions = WEBSERVICE_REMOTE.findOperationDefinitionsByCriteria(subject,
            criteria);

        assertTrue("Expected to get back operation definitions when sorting", opDefinitions.size() > 0);

        // TODO verify sorting
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindResourceOperationHistoriesUnfiltered() {
        ResourceOperationHistoryCriteria resOpHisCrit = WS_OBJECT_FACTORY.createResourceOperationHistoryCriteria();
        List<ResourceOperationHistory> histories = WEBSERVICE_REMOTE.findResourceOperationHistoriesByCriteria(subject,
            resOpHisCrit);

        assertNotNull("Expected non-null results for criteria search of resource operation histories", histories);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindResourceOperationHistoriesWithFiltering() throws InterruptedException {
        Resource serviceAlpha = findResource("service-alpha-0", "server-omega-0");
        Resource serviceBeta = findResource("service-beta-0", "server-omega-0");

        assertNotNull("Failed to find service-alpha-0", serviceAlpha);
        assertNotNull("Failed to find service-beta-0", serviceBeta);

        int numberOfEvents = 3;

        ResourceOperationSchedule serviceAlphaOperationSchedule = fireEvents(serviceAlpha, "WARN", numberOfEvents);
        ResourceOperationSchedule serviceBetaOperationSchedule = fireEvents(serviceBeta, "DEBUG", numberOfEvents);

        ResourceOperationHistory serviceAlphaOpHistory = WsEventManagerTest
            .waitForScheduledOperationToComplete(serviceAlphaOperationSchedule);

        assertNotNull("Expected to get back operation history for '"
            + serviceAlphaOperationSchedule.operationDisplayName + "'", serviceAlphaOpHistory);

        ResourceOperationHistory serviceBetaOpHistory = WsEventManagerTest
            .waitForScheduledOperationToComplete(serviceBetaOperationSchedule);

        ResourceOperationHistoryCriteria criteria = WS_OBJECT_FACTORY.createResourceOperationHistoryCriteria();
        criteria.setFilterOperationName("createEvents");

        List<ResourceOperationHistory> histories = WEBSERVICE_REMOTE.findResourceOperationHistoriesByCriteria(subject,
            criteria);

        assertTrue("Expected to find at least two resource operation histories for the "
            + "createEvents operations that were executed for service-alpha-0 and for service-beta-0",
            histories.size() > 1);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindGroupOperationHistoriesUnfiltered() {
        List<GroupOperationHistory> histories = WEBSERVICE_REMOTE.findGroupOperationHistoriesByCriteria(subject,
            WS_OBJECT_FACTORY.createGroupOperationHistoryCriteria());

        assertNotNull("Expected non-null results for criteria search of group operation histories", histories);
    }

    String[] getNames(List<OperationDefinition> list) {
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            names[i] = (list.get(i).getName());
        }
        return names;
    }

    Resource findResource(String name, String parentName) {
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName(name);
        criteria.setFilterParentResourceName(parentName);

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertEquals("Expected to find only one '" + name + "' having parent, '" + parentName + "'", 1, resources
            .size());

        return resources.get(0);
    }

    ResourceOperationSchedule fireEvents(Resource resource, String severity, int numberOfEvents) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        String details = sdf.format(new java.util.Date()) + " >> events created for " + resource.name;
        String operationName = "createEvents";
        int delay = 0;
        int repeatInterval = 0;
        int repeatCount = 0;
        int timeout = 0;
        Configuration parameters = createParameters(resource, severity, numberOfEvents, details);
        String description = "Test script event for " + resource.name;

        return WEBSERVICE_REMOTE.scheduleResourceOperation(subject, resource.id, operationName, delay, repeatInterval,
            repeatCount, timeout, parameters, description);
    }

    Configuration createParameters(Resource resource, String severity, int numberOfEvents, String details) {
        Configuration params = WS_OBJECT_FACTORY.createConfiguration();
        //	    params.propertyListOrPropertySimpleOrPropertyMap.
        PropertySimple prop1 = WS_OBJECT_FACTORY.createPropertySimple();
        prop1.setName("source");
        prop1.setStringValue(resource.getName());
        //        params.put(WS_OBJECT_FACTORY.createPropertySimple("source", resource.name));
        //        params.put(new PropertySimple("details", details));
        PropertySimple prop2 = WS_OBJECT_FACTORY.createPropertySimple();
        prop2.setName("details");
        prop2.setStringValue(details);
        //        params.put(new PropertySimple("severity", severity));
        PropertySimple prop3 = WS_OBJECT_FACTORY.createPropertySimple();
        prop3.setName("severity");
        prop3.setStringValue(severity);
        //        params.put(new PropertySimple("count", java.lang.Integer(numberOfEvents)));
        PropertySimple prop4 = WS_OBJECT_FACTORY.createPropertySimple();
        prop1.setName("count");
        prop4.setStringValue(Integer.valueOf(numberOfEvents).toString());

        params.propertyListOrPropertySimpleOrPropertyMap = new ArrayList<Property>();
        params.propertyListOrPropertySimpleOrPropertyMap.add(prop1);
        params.propertyListOrPropertySimpleOrPropertyMap.add(prop2);
        params.propertyListOrPropertySimpleOrPropertyMap.add(prop3);
        params.propertyListOrPropertySimpleOrPropertyMap.add(prop4);

        return params;
    }

}
