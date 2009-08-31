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
public class WsMeasurementDefinitionManagerTest extends AssertJUnit implements
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
		ResourceType resourceType = findResourceType();

		MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
		criteria.setFilterName("alpha-metric0");
		criteria.setFilterDisplayName("Alpha Metric 0");
		criteria.setFilterDescription("Alpha Metric 0");
		criteria.setFilterResourceTypeName(resourceType.name);
		criteria.setFilterResourceTypeId(resourceType.id);
		criteria.setFilterCategory(MeasurementCategory.PERFORMANCE);
		criteria.setFilterNumericType(NumericType.DYNAMIC);
		criteria.setFilterDataType(DataType.MEASUREMENT);
		criteria.setFilterDisplayType(DisplayType.DETAIL);
		criteria.setFilterDefaultOn(false);
		// criteria.setFilterDefaultInterval(1200000);
		criteria.setFilterDefaultInterval(1200000L);

		List<MeasurementDefinition> measurementDefs = WEBSERVICE_REMOTE
				.findMeasurementDefinitionsByCriteria(subject, criteria);

		assertEquals("Failed to find measurement definition when filtering",
				measurementDefs.size(), 1);
	}

	@Test(enabled = TESTS_ENABLED)
	void testFindWithSorting() {
		MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
		criteria.setFilterResourceTypeName("service-alpha");
		criteria.setSortName(PageOrdering.ASC);
		criteria.setSortDisplayName(PageOrdering.DESC);
		criteria.setSortResourceTypeName(PageOrdering.ASC);
		criteria.setSortCategory(PageOrdering.DESC);
		criteria.setSortUnits(PageOrdering.ASC);
		criteria.setSortNumericType(PageOrdering.DESC);
		criteria.setSortDataType(PageOrdering.ASC);
		criteria.setSortDisplayType(PageOrdering.DESC);
		criteria.setSortDefaultOn(PageOrdering.ASC);
		criteria.setSortDefaultInterval(PageOrdering.DESC);

		List<MeasurementDefinition> measurementDefs = WEBSERVICE_REMOTE
				.findMeasurementDefinitionsByCriteria(subject, criteria);

		assertTrue("Failed to find measurement definitions when sorting",
				measurementDefs.size() > 0);
	}

	ResourceType findResourceType() {
		ResourceTypeCriteria criteria = new ResourceTypeCriteria();
		criteria.setFilterName("service-alpha");
		criteria.setFilterPluginName("PerfTest");

		return WEBSERVICE_REMOTE.findResourceTypesByCriteria(subject, criteria)
				.get(0);
	}
}
