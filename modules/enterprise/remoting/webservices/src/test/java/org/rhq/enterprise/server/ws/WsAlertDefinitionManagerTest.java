package org.rhq.enterprise.server.ws;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;

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
public class WsAlertDefinitionManagerTest extends AssertJUnit implements
		TestPropertiesInterface {

	private static ObjectFactory WS_OBJECT_FACTORY ;
	private static WebservicesRemote WEBSERVICE_REMOTE;
	private static Subject subject;

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
	void testFindAlertDefinitionsWithoutFiltering() {
		List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE
				.findAlertDefinitionsByCriteria(subject, WS_OBJECT_FACTORY
						.createAlertDefinitionCriteria());

		assertNotNull(
				"Expected to get back non-null results when fetch alert definitions without filtering",
				alertDefs);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindSingleAlertDefinitionWithFiltering() {
		Resource service = findService("service-alpha-0", "server-omega-0");

		AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY
				.createAlertDefinitionCriteria();
		criteria.setFilterName("service-alpha-0-alert-def-1");
		criteria
				.setFilterDescription("Test alert definition 1 for service-alpha-0");
		criteria.setFilterPriority(AlertPriority.MEDIUM);
		criteria.setFilterEnabled(true);
		// criteria.addFilterResourceIds([service.id]);
		List<Integer> filterResourceIds = new ArrayList<Integer>();
		filterResourceIds.add(service.getId());
		criteria.filterResourceIds = filterResourceIds;
		criteria.setFilterDeleted(false);

		List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE
				.findAlertDefinitionsByCriteria(subject, criteria);

		assertEquals("Expected to get back one alert definition.", alertDefs
				.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testGetAlertDefinitionById() {
		Resource service = findService("service-alpha-0", "server-omega-0");

		AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
		criteria.setFilterName("service-alpha-0-alert-def-1");
		criteria
				.setFilterDescription("Test alert definition 1 for service-alpha-0");
		criteria.setFilterPriority(AlertPriority.MEDIUM);
		criteria.setFilterEnabled(true);
		// criteria.addFilterResourceIds(service.getId());
		List<Integer> filterResourceIds = new ArrayList<Integer>();
		filterResourceIds.add(service.getId());
		criteria.filterResourceIds = filterResourceIds;
		criteria.setFilterDeleted(false);

		List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE
				.findAlertDefinitionsByCriteria(subject, criteria);
		assertTrue("Unable to locate alert definitions.", alertDefs.size() > 0);
		AlertDefinition alertDef = alertDefs.get(0);

		assertNotNull("Expected to get back an alert " + "definition for id "
				+ alertDef.id, WEBSERVICE_REMOTE.getAlertDefinition(subject,
				alertDef.id));
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindMultipleAlertDefinitionsWithFiltering() {
		Resource serviceAlpha = findService("service-alpha-0", "server-omega-0");
		Resource serviceBeta =  findService("service-alpha-1", "server-omega-0");

		AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY
				.createAlertDefinitionCriteria();
		criteria.setFilterPriority(AlertPriority.MEDIUM);
		List<Integer> filterResourceIds = new ArrayList<Integer>();
		filterResourceIds.add(serviceAlpha.getId());
		filterResourceIds.add(serviceBeta.getId());
		criteria.filterResourceIds = filterResourceIds;
		criteria.setFilterDeleted(false);

		List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE
				.findAlertDefinitionsByCriteria(subject, criteria);

		assertEquals("Expected to get back two alert definitions but got "
				+ alertDefs.size(), alertDefs.size(), 2);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindAlertDefinitionWithFilteringAndFetchingAssociations() {
		AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY
				.createAlertDefinitionCriteria();
		criteria.setFilterName("service-alpha-0");
		criteria
				.setFilterDescription("Test alert definition 1 for service-alpha-0");

		criteria.setFetchAlerts(true);
		criteria.setFetchConditions(true);
		criteria.setFetchAlertNotifications(true);
		criteria.setFilterDeleted(false);

		List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE
				.findAlertDefinitionsByCriteria(subject, criteria);

		assertEquals(
				"Expected to get back one alert when filtering and fetching associations",
				alertDefs.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindAlertDefinitionsWithFilteringAndSortingAndFetchingAssociations() {
		Resource serviceAlpha = findService("service-alpha-0", "server-omega-0");
		Resource serviceBeta = findService("service-alpha-1", "server-omega-0");

		AlertDefinitionCriteria criteria = WS_OBJECT_FACTORY
				.createAlertDefinitionCriteria();
		criteria.setFilterPriority(AlertPriority.MEDIUM);
		// criteria.addFilterResourceIds([serviceAlpha.id, serviceBeta.id]);
		List<Integer> filterResourceIds = new ArrayList<Integer>();
		filterResourceIds.add(serviceAlpha.getId());
		filterResourceIds.add(serviceBeta.getId());
		criteria.filterResourceIds = filterResourceIds;
		criteria.setFilterDeleted(false);

		criteria.setSortName(PageOrdering.ASC);
		criteria.setSortPriority(PageOrdering.DESC);

		List<AlertDefinition> alertDefs = WEBSERVICE_REMOTE
				.findAlertDefinitionsByCriteria(subject, criteria);

		assertEquals(
				"Expected to get back two alert definitions when filtering, "
						+ "sorting, and fetching associations", alertDefs
						.size(), 2);

		// TODO verify sort order
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindAlertsWithoutFiltering() {
		AlertCriteria criteria = WS_OBJECT_FACTORY.createAlertCriteria();

		List<Alert> alerts = WEBSERVICE_REMOTE.findAlertsByCriteria(subject,
				criteria);

		assertNotNull(
				"Expected to get back non-null results when fetching alerts without filtering",
				alerts);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindAlertsWithFiltering() throws InterruptedException,
			MalformedURLException, SecurityException, IllegalArgumentException,
			ClassNotFoundException, NoSuchMethodException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, LoginException_Exception, FileNotFoundException {

		// find resource
		Resource service = findService("service-alpha-0", "server-omega-0");
		assertNotNull("Specified resource was not located.", service);
		String eventDetails = new java.util.Date() + " >> events created for "
				+ service.name;
		String severity = "WARN";
		int numberOfEvents = 1;

		// opSchedule = fireEvents(service, severity, numberOfEvents,
		// eventDetails);
		// ResourceOperationSchedule opSchedule = fireEvents(service, severity,
		// numberOfEvents);
		ResourceOperationSchedule opSchedule = fireEvents(service, severity,
				numberOfEvents, eventDetails);

		WsEventManagerTest.waitForScheduledOperationToComplete(opSchedule);

		int pauseLength = 1000; // in milliseconds
		int numberOfIntervals = 30;

		EventCriteria eventCriteria = WS_OBJECT_FACTORY.createEventCriteria();
		eventCriteria.caseSensitive = true;
		eventCriteria.setFilterResourceId(service.id);
		eventCriteria.setFilterDetail(eventDetails);
		eventCriteria.setFetchSource(true);

		List<Event> events = waitForEventsToBeCommitted(pauseLength,
				numberOfIntervals, eventCriteria, numberOfEvents);

		assertEquals(
				"Failed to find all fired events when finding alerts "
						+ "with filtering. This could just be a timeout. You may want to check your database and server logs to be sure though",
				numberOfEvents,events.size());

		String alertDef1Name = "service-alpha-0-alert-def-1";

		AlertCriteria alertCriteria = WS_OBJECT_FACTORY.createAlertCriteria();
		alertCriteria.setFilterName(alertDef1Name);
		alertCriteria
				.setFilterDescription("Test alert definition 1 for service-alpha-0");
		alertCriteria.getFilterPriorities().add(AlertPriority.MEDIUM);
		alertCriteria.setFilterResourceTypeName("service-alpha");

		List<Alert> alerts = WEBSERVICE_REMOTE.findAlertsByCriteria(subject,
				alertCriteria);

		assertEquals("Expected to get back one alert for alert definition '"
				+ alertDef1Name + "'", 1, alerts.size());
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

		return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria)
				.get(0);
	}

	ResourceOperationSchedule fireEvents(Resource resource, String severity,
			int numberOfEvents) {
//		SimpleDateFormat sdf = new SimpleDateFormat();
		String details = new java.util.Date()
				+ " >> events created for " + resource.name;
		String operationName = "createEvents";
		int delay = 0;
		int repeatInterval = 0;
		int repeatCount = 0;
		int timeout = 0;
//		 Configuration parameters = createParameters(resource, severity,
//		 numberOfEvents, details);
		WsConfiguration parameters = createWsConfigurationParameters(resource, severity,
				numberOfEvents, details);

		String description = "Test script event for " + resource.name;

		return WEBSERVICE_REMOTE.scheduleResourceOperation(subject,
				resource.id, operationName, delay, repeatInterval, repeatCount,
				timeout, parameters, description);
	}

	ResourceOperationSchedule fireEvents(Resource resource, String severity,
			int numberOfEvents, String details) throws MalformedURLException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, LoginException_Exception {
		String operationName = "createEvents";
		int delay = 0;
		int repeatInterval = 0;
		int repeatCount = 0;
		int timeout = 0;
//		Configuration parameters = createParameters(resource, severity,
//				numberOfEvents, details);
		WsConfiguration parameters = createWsConfigurationParameters(resource, severity,
				numberOfEvents, details);
		String description = "Test script event for " + resource.name;

		return WEBSERVICE_REMOTE.scheduleResourceOperation(subject,
				resource.id, operationName, delay, repeatInterval, repeatCount,
				timeout, parameters, description);
	}

	Configuration createParameters(Resource resource, String severity,
			int numberOfEvents, String details) {
		Configuration params = WS_OBJECT_FACTORY.createConfiguration();
		// params.propertyListOrPropertySimpleOrPropertyMap.
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
		prop4.setName("count");
		prop4.setStringValue(Integer.valueOf(numberOfEvents) + "");
		List<Property> wsProps = new ArrayList<Property>();
		wsProps.add(prop1);
		wsProps.add(prop2);
		wsProps.add(prop3);
		wsProps.add(prop4);
		// params.propertyListOrPropertySimpleOrPropertyMap = wsProps;
		params.propertyListOrPropertySimpleOrPropertyMap = params
				.getPropertyListOrPropertySimpleOrPropertyMap();
		params.propertyListOrPropertySimpleOrPropertyMap.add(prop1);
		params.propertyListOrPropertySimpleOrPropertyMap.add(prop2);
		params.propertyListOrPropertySimpleOrPropertyMap.add(prop3);
		params.propertyListOrPropertySimpleOrPropertyMap.add(prop4);

		return params;
	}

	public static WsConfiguration createWsConfigurationParameters(Resource resource,
			String severity, int numberOfEvents, String details) {
		// Configuration params = WS_OBJECT_FACTORY.createConfiguration();
		WsConfiguration params = new WsConfiguration();
		// params.propertyListOrPropertySimpleOrPropertyMap.
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
		prop4.setName("count");
		prop4.setStringValue(Integer.valueOf(numberOfEvents) + "");
//		params.
		JaxbUtilities.addProperty(params, prop1);
		JaxbUtilities.addProperty(params, prop2);
		JaxbUtilities.addProperty(params, prop3);
		JaxbUtilities.addProperty(params, prop4);

		
		return params;
	}

	List<Event> findEventsByResource(Resource resource) {
		EventCriteria criteria = WS_OBJECT_FACTORY.createEventCriteria();
		criteria.setFilterResourceId(resource.id);

		return WEBSERVICE_REMOTE.findEventsByCriteria(subject, criteria);
	}

	List<Event> waitForEventsToBeCommitted(int intervalLength,
			int numberOfIntervals, EventCriteria eventCriteria,
			int numberOfEvents) throws InterruptedException {
		List<Event> events = null;
		for (int i = 0; i < numberOfIntervals; ++i) {
			events = WEBSERVICE_REMOTE.findEventsByCriteria(subject,
					eventCriteria);
			java.lang.System.out.println("SIZE = " + events.size()
					+ ", NUM_EVENTS = " + numberOfEvents);
			if (events.size() == numberOfEvents) {
				return events;
			}
			java.lang.Thread.sleep(intervalLength);
		}
		return events;
	}
}
