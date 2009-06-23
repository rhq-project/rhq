package org.rhq.plugins.jbosscache5;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

public class JBossCacheComponent implements MeasurementFacet, OperationFacet,
		ProfileServiceComponent<ProfileServiceComponent> {

	private final Log log = LogFactory.getLog(this.getClass());

	public static String CACHE_SEARCH_STRING = "searchString";

	private ProfileServiceComponent parentComp;

	private String beanName;

	public void start(ResourceContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		parentComp = context.getParentResourceComponent();

		Configuration configuration = context.getPluginConfiguration();

		if (configuration.get(JbossCacheComponent.CACHE_SEARCH_STRING) != null)
			beanName = configuration.getSimple(
					JbossCacheComponent.CACHE_SEARCH_STRING).getStringValue();
		else
			throw new InvalidPluginConfigurationException(
					"Invalid plugin configuration in JbossCache component.");
	}

	public void stop() {
		// TODO Auto-generated method stub

	}

	public AvailabilityType getAvailability() {
		try {
			EmsConnection connection = parentComp.getEmsConnection();
			if (connection == null)
				return AvailabilityType.DOWN;

			boolean up = connection.getBean(beanName).isRegistered();
			return up ? AvailabilityType.UP : AvailabilityType.DOWN;
		} catch (Exception e) {
			if (log.isDebugEnabled())
				log.debug("Can not determine availability for " + beanName
						+ ": " + e.getMessage());
			return AvailabilityType.DOWN;
		}
	}

	public ProfileServiceConnection getConnection() {
		return parentComp.getConnection();
	}

	public EmsConnection getEmsConnection() {
		return parentComp.getEmsConnection();
	}

	public OperationResult invokeOperation(String name, Configuration parameters)
			throws InterruptedException, Exception {

		OperationResult result = null;

		try {
			EmsBean cacheBean = getEmsConnection().getBean(beanName);

			EmsOperation operation = cacheBean.getOperation(name);

			String res = String.valueOf(operation.invoke(new Object[] {}));

			result = new OperationResult(res);
		} catch (Exception e) {
			log.error(" Failure to invoke operation " + name + " on bean "
					+ beanName, e);
		}
		return result;
	}

	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> metrics) throws Exception {

		EmsBean cacheBean = getEmsConnection().getBean(beanName);

		for (MeasurementScheduleRequest request : metrics) {
			String metricName = request.getName();
			try {

				EmsAttribute atribute = cacheBean.getAttribute(metricName);

				Object value = atribute.getValue();

				if (request.getDataType() == DataType.MEASUREMENT) {
					Number number = (Number) value;
					report.addData(new MeasurementDataNumeric(request, number
							.doubleValue()));
				} else if (request.getDataType() == DataType.TRAIT) {
					report.addData(new MeasurementDataTrait(request, value
							.toString()));
				}
			} catch (Exception e) {
				log.error(" Failure to collect measurements data from metric "
						+ metricName + " from bean "
						+ cacheBean.getBeanName().toString(), e);
			}

		}
	}
}
