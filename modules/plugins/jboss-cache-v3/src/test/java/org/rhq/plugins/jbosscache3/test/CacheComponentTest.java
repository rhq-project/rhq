/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.jbosscache3.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "jbosscache3-test")
public class CacheComponentTest {

	public static final long MEASUREMENT_FACET_METHOD_TIMEOUT = 10000;
	public static final long OPERATION_FACET_METHOD_TIMEOUT = 10000;
	private Log log = LogFactory.getLog(CacheComponentTest.class);
	public static final String CACHE_PLUGIN_NAME = "JBossCache3";
	private static final String CACHE_RESOURCE_TYPE_NAME = "Cache";
	private static final String ADDED_RESOURCE = "foo:config=test,testName=testValue,Dname=testName,service=ExampleCacheJmxWrapper";
	private EmsConnection connection;
	private OperationTest operationTest;
	private RemoteClientTest remoteClientTest;

	public static String[] RESOURCE_TYPES = { "Interceptor", "Data Container",
			"RPC Manager", "RegionManager", "Transaction Table",
			"Tx Interceptor", "Lock Manager", "Cache" };

	public static String[] JMX_COMPONENT_NAMES = { "CacheMgmtInterceptor",
			"DataContainer", "RPCManager", "RegionManager", "TransactionTable",
			"TxInterceptor", "MVCCLockManager" };

	public static String searchString = "*:*,jmx-resource=";
	private String REGEX = "(,|:)jmx-resource=[^,]*(,|\\z)";

	@Test
	public void testDiscovery() throws Exception {
		PluginContainer.getInstance().getInventoryManager()
				.executeServiceScanImmediately();

		Set<Resource> resources = TestHelper.getResources(TestHelper
				.getResourceType(CACHE_RESOURCE_TYPE_NAME, CACHE_PLUGIN_NAME));

		ObjectName testResource = new ObjectName(ADDED_RESOURCE);

		for (int i = 0; i < RESOURCE_TYPES.length - 1; i++) {
			System.out.println(RESOURCE_TYPES[i]);
			ResourceType type = TestHelper.getResourceType(RESOURCE_TYPES[i],
					CACHE_PLUGIN_NAME);
			if (type != null) {
				Set<Resource> res = TestHelper.getResources(type);
				List<EmsBean> beans = connection.queryBeans(searchString
						+ JMX_COMPONENT_NAMES[i]);

				assert (res.size() == beans.size());
				resources.addAll(res);
			} else {
				assert (connection.queryBeans(
						searchString + JMX_COMPONENT_NAMES[i]).size() == 0);
			}
		}

		boolean isTestExamplePresent = false;
		Set cacheNames = new HashSet();
		Pattern p = Pattern.compile(REGEX);

		for (Resource resource : resources) {
			ObjectName resourceName = new ObjectName(resource.getResourceKey());

			if (resourceName.equals(testResource)) {
				isTestExamplePresent = true;
			}

			String beanName = resource.getResourceKey();

			Matcher m = p.matcher(beanName);
			if (m.find()) {
				beanName = m.replaceFirst(m.group(2).equals(",") ? m.group(1)
						: "");

				if (!cacheNames.contains(beanName)) {
					EmsBean bean = connection.getBean(beanName);
					if (bean != null)
						cacheNames.add(beanName);
				}
			}
		}

		ResourceType type = TestHelper.getResourceType(
				CACHE_RESOURCE_TYPE_NAME, CACHE_PLUGIN_NAME);
		Set<Resource> res = TestHelper.getResources(type);

		for (Resource resource : res) {
			assert (cacheNames.contains(resource.getResourceKey()));
		}

		assert (res.size() == cacheNames.size());
		assert !resources.isEmpty();
		assert isTestExamplePresent;
	}

	@Test
	public void testMetrics() {
		try {
			PluginContainer.getInstance().getInventoryManager()
					.executeServiceScanImmediately();

			for (String resourceTypeName : RESOURCE_TYPES) {
				ResourceType cacheResourceType = TestHelper.getResourceType(
						resourceTypeName, CACHE_PLUGIN_NAME);
				testResouceTypeMetrics(cacheResourceType);
			}

			ResourceType cacheResourceType = TestHelper.getResourceType(
					CACHE_RESOURCE_TYPE_NAME, CACHE_PLUGIN_NAME);
			testResouceTypeMetrics(cacheResourceType);

			return;
		} catch (Exception e) {
			log.error("Metric test failed", e);
			org.testng.Assert.fail("Metric test failed", e);
		}
	}

	private void testResouceTypeMetrics(ResourceType cacheResourceType)
			throws Exception {
		Set<MeasurementDefinition> metricDefinitions = cacheResourceType
				.getMetricDefinitions();

		Set<Resource> resources = TestHelper.getResources(cacheResourceType);

		for (Resource resource : resources) {
			log.info("Validating metrics for " + resource + "...");

			MeasurementFacet measurementFacet = ComponentUtil.getComponent(
					resource.getId(), MeasurementFacet.class,
					FacetLockType.READ, MEASUREMENT_FACET_METHOD_TIMEOUT, true,
					true);
			EmsBean relatedBean = connection.getBean(resource.getResourceKey());

			for (MeasurementDefinition metricDefinition : metricDefinitions) {

				String compValue = TestHelper.getMetricValue(metricDefinition,
						measurementFacet);
				EmsAttribute atr = relatedBean.getAttribute(metricDefinition
						.getName());
				log.info("            Validating Metric "
						+ metricDefinition.getName());
				String beanValue = String.valueOf(atr.getValue());

				assert (TestHelper.compareValues(compValue, atr.getType(), atr
						.getValue()));
			}
		}
	}

	@Test
	public void testOperations() {
		try {
			PluginContainer.getInstance().getInventoryManager()
					.executeServiceScanImmediately();
			operationTest = new OperationTest(connection);
			operationTest.testOperations();
		} catch (Exception e) {
			log.error("Operation test failed", e);
			org.testng.Assert.fail("Operation test failed", e);
		}
	}

	@BeforeSuite(groups = "jbosscache3-test")
	@Parameters( { "principal", "credentials", "testJarPath", "xmlFilePath" })
	public void start(@Optional String principal, @Optional String credentials,
			@Optional String testJarPath, String xmlFilePath) {
		try {
			TestHelper.startContainer(principal, credentials);
			ProfileServiceComponent serverComp = (ProfileServiceComponent) AppServerUtils
					.getASComponentProxy(ProfileServiceComponent.class);
			connection = serverComp.getEmsConnection();
			remoteClientTest = new RemoteClientTest();
			remoteClientTest.deployXmlExample(xmlFilePath);
			remoteClientTest.deployCacheExample(testJarPath);
			remoteClientTest.runTest();

			PluginContainer.getInstance().getInventoryManager()
					.executeServiceScanImmediately();

		} catch (java.lang.IllegalStateException e) {
			log.info("Object allready deployed.");
			remoteClientTest.runTest();
		} catch (Exception e) {
			org.testng.Assert.fail("Failed to start PC...", e);
			log.error("Failed to start PC...", e);
		}
	}

	@AfterSuite(groups = "jbosscache3-test")
	public void stop() {
		try {
			log.info("Stopping PC...");
			remoteClientTest.runClientClean();
			remoteClientTest.undeployCacheExample();
			PluginContainer.getInstance().shutdown();
			log.info("PC stopped.");
		} catch (Exception e) {

			log.error("Failed to stop PC...", e);
			org.testng.Assert.fail("Failed to stop PC..", e);
		}
	}

}