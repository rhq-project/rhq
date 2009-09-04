package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlSeeAlso;
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
@XmlSeeAlso( { PropertyDefinitionSimple.class, PropertyDefinitionList.class, PropertyDefinitionMap.class,
    ObjectFactory.class })
public class WsEventManagerTest extends AssertJUnit implements TestPropertiesInterface{

    //Test variables
    private static ObjectFactory WS_OBJECT_FACTORY;
    private static WebservicesRemote WEBSERVICE_REMOTE;
    private static Subject subject = null;

    @BeforeClass
    public static void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
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

    static String alphaService0Details;
    static String alphaService1Details;
    static String betaService0Details;
    static ResourceOperationSchedule operationSchedule;
    static Resource alphaService0;
    static Resource alphaService1;
    static Resource betaService0;

    @BeforeClass
    static void setUp() throws MalformedURLException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, LoginException_Exception {
    	init();
        Resource parentServer = findServer("server-omega-0");
        alphaService0 = findService("service-alpha-0", parentServer);
        alphaService1 = findService("service-alpha-1", parentServer);
        betaService0 = findService("service-beta-0", parentServer);

        alphaService0Details = new java.util.Date() + " >> events created for " + alphaService0.name;
        alphaService1Details = new java.util.Date() + " >> events created for " + alphaService1.name;
        betaService0Details = new java.util.Date() + " >> events created for " + betaService0.name;

        operationSchedule = fireEvent(alphaService0, "WARN", 1, alphaService0Details);
        fireEvent(alphaService1, "ERROR", 1, alphaService1Details);
        fireEvent(betaService0, "FATAL", 1, betaService0Details);
    }

    @Test(enabled = TESTS_ENABLED)
    void testFilterByResource() throws InterruptedException, JAXBException, MalformedURLException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, LoginException_Exception {

    	EventCriteria criteria = WS_OBJECT_FACTORY.createEventCriteria();
        criteria.caseSensitive = true;
        //criteria.addFilterResourceId(alphaService0.id);
        //criteria.addFilterSeverity(EventSeverity.WARN);
        criteria.setFilterDetail(alphaService0Details);
        //criteria.addFilterSourceName(alphaService0.name);
        criteria.setFilterSourceName("service-alpha-event");

        ResourceOperationHistory result = waitForScheduledOperationToComplete(operationSchedule);

        assertNotNull("Failed to get result for scheduled operation", result);

        java.lang.Thread.sleep(15000);

        List<Event> events = WEBSERVICE_REMOTE.findEventsByCriteria(subject, criteria);
        //var events = findEventsByResource(alphaService0);

        assertEquals("Event count not correct.", 1, events.size());

        events = findEventsByResource(alphaService1);
        assertTrue("Expected to find events when filtering by resource id for " + alphaService1, events.size() > 0);

        events = findEventsByResource(betaService0);
        assertTrue("Expected to find events when filtering by resource id for " + betaService0, events.size() > 0);
    }

    static Resource findServer(String name) {
//        if (WS_OBJECT_FACTORY == null) {
//            setUp();
//        }
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName(name);

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertEquals("Expected to find only one resource named " + name + "'", resources.size(), 1);

        return resources.get(0);
    }

    static Resource findService(String name, Resource parentServer) {
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        criteria.setFilterName(name);
        criteria.setFilterParentResourceId(parentServer.id);

        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        assertEquals("Expected to find only one service named '" + name + "' having parent, '" + parentServer.name
            + "'", 1, resources.size());

        return resources.get(0);
    }

	static ResourceOperationSchedule fireEvent(Resource resource,
			String severity, int numberOfEvents, String details)
			throws MalformedURLException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, LoginException_Exception {
        String operationName = "createEvents";
        int delay = 0;
        int repeatInterval = 0;
        int repeatCount = 0;
        int timeout = 0;
//        Configuration parameters = createParameters(resource, severity, numberOfEvents, details);
         WsConfiguration parameters = 
        	 WsAlertManagerTest.createWsConfigurationParameters(resource, severity, numberOfEvents, details);
        String description = "Test script event for " + resource.name;

        return WEBSERVICE_REMOTE.scheduleResourceOperation(subject, resource.id, operationName, delay, repeatInterval,
            repeatCount, timeout, parameters, description);
    }

    static Configuration createParameters(Resource resource, String severity, int numberOfEvents, String details) {
        Configuration params = WS_OBJECT_FACTORY.createConfiguration();
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

        params.getPropertyListOrPropertySimpleOrPropertyMap().add(prop1);
        params.getPropertyListOrPropertySimpleOrPropertyMap().add(prop2);
        params.getPropertyListOrPropertySimpleOrPropertyMap().add(prop3);
        params.getPropertyListOrPropertySimpleOrPropertyMap().add(prop4);

        return params;
    }

    List<Event> findEventsByResource(Resource resource) {
        EventCriteria criteria = WS_OBJECT_FACTORY.createEventCriteria();
        criteria.setFilterResourceId(resource.id);

        return WEBSERVICE_REMOTE.findEventsByCriteria(subject, criteria);
    }

    public static ResourceOperationHistory waitForScheduledOperationToComplete(ResourceOperationSchedule schedule)
        throws InterruptedException, MalformedURLException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, LoginException_Exception {

        return waitForScheduledOperationToComplete(schedule, 1000L, 10);
    }

    public static ResourceOperationHistory waitForScheduledOperationToComplete(ResourceOperationSchedule schedule,
        long intervalDuration, int maxIntervals) throws InterruptedException, MalformedURLException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, LoginException_Exception {
        //        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
        if (WS_OBJECT_FACTORY == null) {
            init();
        }
        ResourceOperationHistoryCriteria criteria = WS_OBJECT_FACTORY.createResourceOperationHistoryCriteria();
        //TODO: doc below on wiki
        //        criteria.setFilt
        //        criteria.addFilterJobId(schedule.getJobId());
        //### same as setting jobId and Job Group        criteria.addFilterJobId(schedule.getJobId());
        criteria.setFilterJobGroup(schedule.getJobGroup());
        criteria.setFilterJobName(schedule.getJobName());
        //###??        criteria.addFilterResourceIds(schedule.getResource().getId());
        ArrayList<Integer> filterResIds = new ArrayList<Integer>();
        filterResIds.add(schedule.getResource().getId());
        //TODO: doc below for wiki
        criteria.filterResourceIds = filterResIds;
        criteria.setSortStartTime(PageOrdering.DESC);
        //TODO: doc below for wiki.
        //        criteria.setPaging(0, 1);  AKA. setPageNumber and setPageSize in that order
        criteria.setPageNumber(0);
        criteria.setPageSize(1);
        criteria.setFetchOperationDefinition(true);
        criteria.setFetchParameters(true);
        criteria.setFetchResults(true);

        ResourceOperationHistory history = null;

        int i = 0;

        while (history == null && i < maxIntervals) {
            Thread.sleep(intervalDuration);
            List<ResourceOperationHistory> histories = WEBSERVICE_REMOTE.findResourceOperationHistoriesByCriteria(
                subject, criteria);
            if (histories.size() > 0 && histories.get(0).getStatus() != OperationRequestStatus.INPROGRESS) {
                history = histories.get(0);
            }
            ++i;
        }

        return history;
    }

}
