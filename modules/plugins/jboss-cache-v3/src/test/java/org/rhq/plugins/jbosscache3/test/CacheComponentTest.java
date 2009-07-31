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

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "JBossCache3-test", enabled = true)
public class CacheComponentTest {

	private static final long MEASUREMENT_FACET_METHOD_TIMEOUT = 10000;
	private static final long OPERATION_FACET_METHOD_TIMEOUT = 10000;
	private Log log = LogFactory.getLog(CacheComponentTest.class);

	private static final String CACHE_PLUGIN_NAME = "JBossCache3";
	private static final String CACHE_RESOURCE_TYPE_NAME = "Cache";
	private static final String CACHE_EXAMPLE_FILE = "test-service.xml";
	private static final String ADDED_RESOURCE = "foo:config=test,testName=testValue,Dname=testName,service=ExampleCacheJmxWrapper";
	private static final String REMOTE_NAME = "TestOperationBean/remote";
	private static final String JBOSS_HOME = "homeDir";
	private static final String JBOSS_NAME = "serverName";
	private EmsConnection connection;
	private String testJarPath;
	private ResourceType cacheResourceType;

	private String[] nullableMetrics = { "ClusterConfig",
			"CacheLoaderConfiguration", "CacheLoaderConfig",
			"BuddyReplicationConfig", "EvictionPolicyConfig",
			"ClusterProperties", "TransactionManagerLookupClass" };

	private String[] components = { "Interceptor", "Data Container",
			"RPC Manager", "RegionManager", "Transaction Table",
			"Tx Interceptor", "Lock Manager" };

	private String[] jmxComponents = { "CacheMgmtInterceptor", "DataContainer",
			"RPCManager", "RegionManager", "TransactionTable", "TxInterceptor",
			"MVCCLockManager" };

	private String searchString = "*:*,jmx-resource=";

	@Test
	public void testDiscovery() throws Exception {

		Set<Resource> resources = TestHelper.getResources(cacheResourceType);
		ObjectName testResource = new ObjectName(ADDED_RESOURCE);

		for (int i = 0; i < components.length; i++) {
			System.out.println(components[i]);
			ResourceType type = TestHelper.getResourceType(components[i],
					CACHE_PLUGIN_NAME);
			if (type != null) {
				Set<Resource> res = TestHelper.getResources(type);
				List<EmsBean> beans = connection.queryBeans(searchString
						+ jmxComponents[i]);
				assert (res.size() == beans.size());
				resources.addAll(res);
			} else {
				assert (connection.queryBeans(searchString + jmxComponents[i])
						.size() == 0);
			}
		}

		boolean isTestExamplePresent = false;

		for (Resource resource : resources) {
			ObjectName resourceName = new ObjectName(resource.getResourceKey());

			if (resourceName.equals(testResource)) {
				isTestExamplePresent = true;
			}
		}

		assert !resources.isEmpty();
		assert isTestExamplePresent;
	}

	@Test
	public void testMetrics() throws Exception {
		// assert (cacheResourceType != null);
		cacheResourceType = TestHelper.getResourceType(
				CACHE_RESOURCE_TYPE_NAME, CACHE_PLUGIN_NAME);
		Set<MeasurementDefinition> metricDefinitions = cacheResourceType
				.getMetricDefinitions();

		Set<Resource> resources = TestHelper.getResources(cacheResourceType);

		for (Resource resource : resources) {
			System.out.println("Validating metrics for " + resource + "...");

			MeasurementFacet measurementFacet = ComponentUtil.getComponent(
					resource.getId(), MeasurementFacet.class,
					FacetLockType.READ, MEASUREMENT_FACET_METHOD_TIMEOUT, true,
					true);
			EmsBean relatedBean = connection.getBean(resource.getResourceKey());

			for (MeasurementDefinition metricDefinition : metricDefinitions) {

				String compValue = getMetricValue(metricDefinition,
						measurementFacet);
				EmsAttribute atr = relatedBean.getAttribute(metricDefinition
						.getName());
				log.info("            Validating Metric "
						+ metricDefinition.getName());
				String beanValue = String.valueOf(atr.getValue());

				assert (compareValues(compValue, atr));
			}
		}

		return;
	}

	protected boolean compareValues(String value, EmsAttribute atr) {
		String type = atr.getType();
		Object obj = atr.getValue();

		if (value == null & obj == null)
			return true;

		if (type.equals("int")) {
			Integer val = Double.valueOf(value).intValue();
			return obj.equals(val);
		}
		if (type.equals("double")) {
			Double val = Double.valueOf(value);
			return obj.equals(val);
		}
		if (type.equals("long")) {
			Long val = Double.valueOf(value).longValue();
			return obj.equals(val);
		}

		return value.equals(obj.toString());
	}

	protected String getMetricValue(MeasurementDefinition metricDefinition,
			MeasurementFacet measurementFacet) throws Exception {

		String name = metricDefinition.getName();
		DataType dataType = metricDefinition.getDataType();

		if (dataType == DataType.MEASUREMENT || dataType == DataType.TRAIT) {

			MeasurementReport report = new MeasurementReport();
			Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>();

			MeasurementScheduleRequest request = new MeasurementScheduleRequest(
					1, name, 0, true, dataType);
			requests.add(request);

			measurementFacet.getValues(report, requests);
			System.out.println("Getting metrics from " + name);
			if (dataType == DataType.MEASUREMENT) {
				assert report.getNumericData().isEmpty()
						|| report.getNumericData().size() == 1;
				assert report.getCallTimeData().isEmpty();

				MeasurementDataNumeric dataNumeric = (report.getNumericData()
						.isEmpty()) ? null : report.getNumericData().iterator()
						.next();
				Double dValue = (dataNumeric != null) ? dataNumeric.getValue()
						: null;

				return String.valueOf(dValue);
			} else if (dataType == DataType.TRAIT) {

				boolean isNullable = isNullableMetric(name);

				if (!isNullable)
					assert report.getTraitData().isEmpty()
							|| report.getTraitData().size() == 1;

				MeasurementDataTrait dataTrait = (report.getTraitData().size() == 1) ? report
						.getTraitData().iterator().next()
						: null;
				String value = (dataTrait != null) ? dataTrait.getValue()
						: null;
				return value;
			}
		}
		return null;
	}

	protected boolean isNullableMetric(String name) {
		for (int i = 0; i < nullableMetrics.length; i++) {
			if (name.equals(nullableMetrics[i]))
				return true;
		}
		return false;
	}

	@Test
	public void testOperations() throws Exception {
		Set<OperationDefinition> operationDefinitions = cacheResourceType
				.getOperationDefinitions();

		Set<Resource> resources = TestHelper.getResources(cacheResourceType);
		for (Resource resource : resources) {
			System.out.println("Validating operations for " + resource + "...");
			OperationFacet operationFacet = ComponentUtil.getComponent(resource
					.getId(), OperationFacet.class, FacetLockType.WRITE,
					OPERATION_FACET_METHOD_TIMEOUT, true, true);

			for (OperationDefinition operationDefinition : operationDefinitions) {
				String name = operationDefinition.getName();
				OperationResult result = operationFacet.invokeOperation(name,
						new Configuration());
				log.info("Validating operation '" + name + "' result ("
						+ result + ")...");
			}
		}
		return;
	}

	@BeforeSuite(alwaysRun = true)
	@Parameters( { "principal", "credentials", "testJarPath" })
	public void start(@Optional String principal, @Optional String credentials,
			@Optional String testJarPath) {
		try {
			File pluginDir = new File("target/itest/plugins");
			PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
			pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
			pcConfig.setPluginDirectory(pluginDir);
			pcConfig.setInsideAgent(false);
			PluginContainer container = PluginContainer.getInstance();
			PluginContainer.getInstance().setConfiguration(pcConfig);
			System.out.println("Starting PC...");
			PluginContainer.getInstance().initialize();
			Set<String> pluginNames = PluginContainer.getInstance()
					.getPluginManager().getMetadataManager().getPluginNames();
			System.out.println("PC started with the following plugins: "
					+ pluginNames);
			PluginContainer.getInstance().getInventoryManager()
					.executeServerScanImmediately();

			Configuration newConfig = AppServerUtils.getASResource()
					.getPluginConfiguration();
			newConfig.put(new PropertySimple("principal", principal));
			newConfig.put(new PropertySimple("credentials", credentials));

			int asResourceId = AppServerUtils.getASResource().getId();

			PluginContainer.getInstance().getInventoryManager()
					.updatePluginConfiguration(asResourceId, newConfig);

			deployCacheExample(testJarPath);
			runTest();

			PluginContainer.getInstance().getInventoryManager()
					.executeServiceScanImmediately();

			PluginContainer.getInstance().getInventoryManager()
					.executeServerScanImmediately();
			ProfileServiceComponent serverComp = (ProfileServiceComponent) AppServerUtils
					.getASComponentProxy(ProfileServiceComponent.class);

			connection = serverComp.getEmsConnection();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to start PC...");
		}
	}

	@AfterSuite(alwaysRun = true)
	public void stop() {
		try {
			System.out.println("Stopping PC...");
			AppServerUtils.undeployFromAS(testJarPath);
			PluginContainer.getInstance().shutdown();
			System.out.println("PC stopped.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to stop PC...");
		}
	}

	protected void deployCacheExample(String jarPath) throws Exception {
		testJarPath = jarPath;
		File jarFile = new File(jarPath);
		AppServerUtils.deployFileToAS(jarFile.getName(), jarFile, false);

		String sourcePath = "target" + File.separator + "test-classes"
				+ File.separator + CACHE_EXAMPLE_FILE;

		File sourceFile = new File(sourcePath);

		Resource res = AppServerUtils.getASResource();
		Configuration config = res.getPluginConfiguration();
		PropertySimple propHome = config.getSimple(JBOSS_HOME);
		PropertySimple propName = config.getSimple(JBOSS_NAME);
		assert (propHome != null);
		assert (propName != null);

		String name = propHome.getStringValue() + File.separatorChar + "server"
				+ File.separatorChar + propName.getStringValue()
				+ File.separatorChar + "deploy";
		log.info(name);
		File destDir = new File(name);
		assert (destDir.exists());
		assert (destDir.isDirectory());

		File destFile = File.createTempFile("tmp", "-service.xml", destDir);
		destFile.deleteOnExit();

		TestHelper.copyFile(destFile, sourceFile);

	}

	protected void runTest() {

		try {
			Object test = AppServerUtils.getRemoteObject(REMOTE_NAME,
					Object.class);
			// InitialContext ctx= AppServerUtils.getAppServerInitialContext();

			AppServerUtils.invokeMethod("test", test, (MethodArgDef[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}