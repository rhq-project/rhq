package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
public class WsMeasurementScheduleManagerTest extends AssertJUnit implements
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
	void testFindWithFiltering() {
		MeasurementDefinition measurementDef = findMeasurementDefinition();
		Resource resource = findAlphaService();

		MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
		// criteria.addFilterDefinitionIds([measurementDef.id]);
		List<Integer> filterDefinitionIds = new ArrayList<Integer>();
		filterDefinitionIds.add(measurementDef.getId());
		criteria.filterDefinitionIds = filterDefinitionIds;

		criteria.setFilterResourceId(resource.id);

		List<MeasurementSchedule> measurementSchedules = WEBSERVICE_REMOTE
				.findSchedulesByCriteria(subject, criteria);

		assertEquals("Failed to find measurement schedules when filtering",
				measurementSchedules.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithFetchingAssociations() {
		MeasurementDefinition measurementDef = findMeasurementDefinition();
		Resource resource = findAlphaService();

		MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
		// criteria.addFilterDefinitionIds([measurementDef.id])
		List<Integer> filterDefinitionIds = new ArrayList<Integer>();
		filterDefinitionIds.add(measurementDef.getId());
		criteria.filterDefinitionIds = filterDefinitionIds;

		criteria.setFilterResourceId(resource.id);
		criteria.setFetchBaseline(true);
		criteria.setFetchDefinition(true);
		criteria.setFetchResource(true);

		List<MeasurementSchedule> measurementSchedules = WEBSERVICE_REMOTE
				.findSchedulesByCriteria(subject, criteria);

		assertEquals(
				"Failed to find measurement schedules when fetching associations",
				measurementSchedules.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithSorting() {
		MeasurementDefinition measurementDef = findMeasurementDefinition();
		Resource resource = findAlphaService();

		MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
		criteria.setSortName(PageOrdering.ASC);

		List<MeasurementSchedule> measurementSchedules = WEBSERVICE_REMOTE
				.findSchedulesByCriteria(subject, criteria);

		assertTrue("Failed to find measurement schedules when sorting",
				measurementSchedules.size() > 0);
	}

	MeasurementDefinition findMeasurementDefinition() {
		MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
		criteria.setFilterName("alpha-metric0");
		criteria.setFilterResourceTypeName("service-alpha");

		List<MeasurementDefinition> measurementDefs = WEBSERVICE_REMOTE
				.findMeasurementDefinitionsByCriteria(subject, criteria);

		return measurementDefs.get(0);
	}

	Resource findAlphaService() {
		ResourceCriteria criteria = new ResourceCriteria();
		criteria.setFilterName("service-alpha-0");
		criteria.setFilterParentResourceName("server-omega-0");

		return WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria)
				.get(0);
	}
}
