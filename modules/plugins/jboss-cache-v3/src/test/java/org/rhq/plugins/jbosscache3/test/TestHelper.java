package org.rhq.plugins.jbosscache3.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

public class TestHelper {

	public static Set<Resource> getResources(ResourceType resourceType) {
		InventoryManager inventoryManager = PluginContainer.getInstance()
				.getInventoryManager();
		Set<Resource> resources = inventoryManager
				.getResourcesWithType(resourceType);
		return resources;
	}

	public static ResourceType getResourceType(String resourceTypeName,
			String pluginName) {
		PluginManager pluginManager = PluginContainer.getInstance()
				.getPluginManager();
		PluginMetadataManager pluginMetadataManager = pluginManager
				.getMetadataManager();
		return pluginMetadataManager.getType(resourceTypeName, pluginName);
	}

	public static void copyFile(File fileA, File fileB) throws Exception {

		InputStream in = new FileInputStream(fileB);

		OutputStream out = new FileOutputStream(fileA);

		byte[] buf = new byte[1024];
		int length;
		while ((length = in.read(buf)) > 0) {
			out.write(buf, 0, length);
		}
		in.close();
		out.close();
	}

	public static boolean compareValues(String value, String type, Object obj) {

		if (value == null & obj == null)
			return true;

		if (type.equals("int")) {
			Integer val = Integer.valueOf(Double.valueOf(value).intValue());
			return obj.equals(val);
		}
		if (type.equals("double")) {
			Double val = Double.valueOf(value);
			Double orig = (Double) obj;
			orig = round(orig, 1);

			return obj.equals(val);
		}
		if (type.equals("long")) {
			Long val = Long.valueOf(Double.valueOf(value).longValue());
			return obj.equals(val);
		}

		String val2 = obj.toString();
		if (value.length() != val2.length()) {
			val2 = val2.substring(0, value.length());
		}
		return value.equals(val2);
	}

	private static double round(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	public static String getMetricValue(Resource resource, String metricName)
			throws Exception {

		MeasurementFacet measurementFacet = ComponentUtil
				.getComponent(resource.getId(), MeasurementFacet.class,
						FacetLockType.READ,
						CacheComponentTest.MEASUREMENT_FACET_METHOD_TIMEOUT,
						true, true);
		ResourceType resourceType = resource.getResourceType();
		Set<MeasurementDefinition> metricDefinitions = resourceType
				.getMetricDefinitions();

		for (MeasurementDefinition metricDefinition : metricDefinitions) {
			if (metricDefinition.getName().equals(metricName))
				return getMetricValue(metricDefinition, measurementFacet);
		}

		throw new Exception("Metric Name " + metricName + " not found.");
	}

	public static String getMetricValue(MeasurementDefinition metricDefinition,
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

	public static void startContainer(String principal, String credentials)
			throws Exception {
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

		PluginContainer.getInstance().getInventoryManager()
				.executeServerScanImmediately();
	}

}
