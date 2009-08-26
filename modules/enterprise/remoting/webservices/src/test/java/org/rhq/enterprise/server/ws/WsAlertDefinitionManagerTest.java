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
public class WsAlertDefinitionManagerTest extends AssertJUnit implements TestPropertiesInterface {

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
    void testFindAlertDefinitionsWithoutFiltering() {
        List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE.findAlertDefinitionsByCriteria(subject, WS_OBJECT_FACTORY
            .createAlertDefinitionCriteria());

        assertNotNull("Expected to get back non-null results when fetch alert definitions without filtering", alertDefs);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindSingleAlertDefinitionWithFiltering() {
        Resource service = findService("service-alpha-0", "server-omega-0");

        AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY.createAlertDefinitionCriteria();
        criteria.setFilterName("service-alpha-0-alert-def-1");
        criteria.setFilterDescription("Test alert definition 1 for service-alpha-0");
        criteria.setFilterPriority(AlertPriority.MEDIUM);
        criteria.setFilterEnabled(true);
        //	    criteria.addFilterResourceIds([service.id]);
        List<Integer> filterResourceIds = new ArrayList<Integer>();
        filterResourceIds.add(service.getId());
        criteria.filterResourceIds = filterResourceIds;
        criteria.setFilterDeleted(false);

        List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE.findAlertDefinitionsByCriteria(subject, criteria);

        assertEquals("Expected to get back one alert definition.", alertDefs.size(), 1);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindMultipleAlertDefinitionsWithFiltering() {
        Resource serviceAlpha = findService("service-alpha-0", "server-omega-0");
        Resource serviceBeta = findService("service-alpha-1", "server-omega-0");

        AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY.createAlertDefinitionCriteria();
        criteria.setFilterPriority(AlertPriority.MEDIUM);
        List<Integer> filterResourceIds = new ArrayList<Integer>();
        filterResourceIds.add(serviceAlpha.getId());
        filterResourceIds.add(serviceBeta.getId());
        criteria.filterResourceIds = filterResourceIds;
        criteria.setFilterDeleted(false);

        List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE.findAlertDefinitionsByCriteria(subject, criteria);

        assertEquals("Expected to get back two alert definitions but got " + alertDefs.size(), alertDefs.size(), 2);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindAlertDefinitionWithFilteringAndFetchingAssociations() {
        AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY.createAlertDefinitionCriteria();
        criteria.setFilterName("service-alpha-0");
        criteria.setFilterDescription("Test alert definition 1 for service-alpha-0");

        criteria.setFetchAlerts(true);
        criteria.setFetchConditions(true);
        criteria.setFetchAlertNotifications(true);
        criteria.setFilterDeleted(false);

        List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE.findAlertDefinitionsByCriteria(subject, criteria);

        assertEquals("Expected to get back one alert when filtering and fetching associations", alertDefs.size(), 1);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindAlertDefinitionsWithFilteringAndSortingAndFetchingAssociations() {
        Resource serviceAlpha = findService("service-alpha-0", "server-omega-0");
        Resource serviceBeta = findService("service-alpha-1", "server-omega-0");

        AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY.createAlertDefinitionCriteria();
        criteria.setFilterPriority(AlertPriority.MEDIUM);
        //	    criteria.addFilterResourceIds([serviceAlpha.id, serviceBeta.id]);
        List<Integer> filterResourceIds = new ArrayList<Integer>();
        filterResourceIds.add(serviceAlpha.getId());
        filterResourceIds.add(serviceBeta.getId());
        criteria.filterResourceIds = filterResourceIds;
        criteria.setFilterDeleted(false);

        criteria.setSortName(PageOrdering.ASC);
        criteria.setSortPriority(PageOrdering.DESC);

        List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE.findAlertDefinitionsByCriteria(subject, criteria);

        assertEquals("Expected to get back two alert definitions when filtering, "
            + "sorting, and fetching associations", alertDefs.size(), 2);

        // TODO verify sort order
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindAlertsWithoutFiltering() {
        AlertCriteria criteria = WS_OBJECT_FACTORY.createAlertCriteria();

        List<Alert> alerts = WEBSERVICE_REMOTE.findAlertsByCriteria(subject, criteria);

        assertNotNull("Expected to get back non-null results when fetching alerts without filtering", alerts);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFindAlertsWithFiltering() throws InterruptedException {
        Resource service = findService("service-alpha-0", "server-omega-0");
        SimpleDateFormat sdf = new SimpleDateFormat();
        String eventDetails = sdf.format(new java.util.Date()) + " >> events created for " + service.name;
        String severity = "WARN";
        int numberOfEvents = 1;

        //	    opSchedule = fireEvents(service, severity, numberOfEvents, eventDetails);
        ResourceOperationSchedule opSchedule = fireEvents(service, severity, numberOfEvents);

        WsEventManagerTest.waitForScheduledOperationToComplete(opSchedule);

        int pauseLength = 1000; // in milliseconds
        int numberOfIntervals = 10;

        EventCriteria eventCriteria = WS_OBJECT_FACTORY.createEventCriteria();
        eventCriteria.caseSensitive = true;
        eventCriteria.setFilterResourceId(service.id);
        //eventCriteria.addFilterDetail(eventDetails);

        List<Event> events = waitForEventsToBeCommitted(pauseLength, numberOfIntervals, eventCriteria, numberOfEvents);

        assertEquals(
            "Failed to find all fired events when finding alerts "
                + "with filtering. This could just be a timeout. You may want to check your database and server logs to be sure though",
            events.size(), numberOfEvents);

        String alertDef1Name = "service-alpha-0-alert-def-1";

        AlertCriteria alertCriteria = WS_OBJECT_FACTORY.createAlertCriteria();
        alertCriteria.setFilterName(alertDef1Name);
        alertCriteria.setFilterDescription("Test alert definition 1 for service-alpha-0");
        alertCriteria.setFilterPriority(AlertPriority.MEDIUM);
        alertCriteria.setFilterResourceTypeName("service-alpha-0");

        List<Alert> alerts = WEBSERVICE_REMOTE.findAlertsByCriteria(subject, alertCriteria);

        assertEquals("Expected to get back one alert for alert definition '" + alertDef1Name + "'", alerts.size(), 1);
    }

    String[] getNames(List<OperationDefinition> list) {
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            names[i] = (list.get(i).getName());
        }
        return names;
    }

    Resource findService(String name, String parentName) {
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName(name);
        criteria.setFilterParentResourceName(parentName);

        return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria).get(0);
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
        PropertySimple prop2 = WS_OBJECT_FACTORY.createPropertySimple();
        prop2.setName("details");
        prop2.setStringValue(details);
        PropertySimple prop3 = WS_OBJECT_FACTORY.createPropertySimple();
        prop3.setName("severity");
        prop3.setStringValue(severity);
        PropertySimple prop4 = WS_OBJECT_FACTORY.createPropertySimple();
        prop1.setName("count");
        prop4.setStringValue(Integer.valueOf(numberOfEvents).toString());
        List<Property> wsProps = new ArrayList<Property>();
        wsProps.add(prop1);
        wsProps.add(prop2);
        wsProps.add(prop3);
        wsProps.add(prop4);
        params.propertyListOrPropertySimpleOrPropertyMap = wsProps;

        return params;
    }

    List<Event> findEventsByResource(Resource resource) {
        EventCriteria criteria = WS_OBJECT_FACTORY.createEventCriteria();
        criteria.setFilterResourceId(resource.id);

        return WEBSERVICE_REMOTE.findEventsByCriteria(subject, criteria);
    }

    List<Event> waitForEventsToBeCommitted(int intervalLength, int numberOfIntervals, EventCriteria eventCriteria,
        int numberOfEvents) throws InterruptedException {
        for (int i = 0; i < numberOfIntervals; ++i) {
            List<Event> events = WEBSERVICE_REMOTE.findEventsByCriteria(subject, eventCriteria);
            java.lang.System.out.println("SIZE = " + events.size() + ", NUM_EVENTS = " + numberOfEvents);
            if (events.size() == numberOfEvents) {
                return events;
            }
            java.lang.Thread.sleep(intervalLength);
        }
        return null;
    }
}
