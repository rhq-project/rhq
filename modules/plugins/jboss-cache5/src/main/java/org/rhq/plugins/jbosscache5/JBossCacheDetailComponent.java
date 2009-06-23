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

public class JBossCacheDetailComponent implements MeasurementFacet,
		OperationFacet, ProfileServiceComponent<ProfileServiceComponent> {

	public static String JMX_NAME = "jmx-name";
	public static String CACHE_DETAIL_BEAN_NAME = "bean-name";
	public ProfileServiceComponent parentComponent;
	private String beanName;

	private final Log log = LogFactory.getLog(this.getClass());

	public void start(ResourceContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		parentComponent = context.getParentResourceComponent();
		EmsConnection connection = getEmsConnection();

		beanName = context.getPluginConfiguration().getSimple(
				CACHE_DETAIL_BEAN_NAME).getStringValue();

	}

	public void stop() {
		// TODO Auto-generated method stub

	}

	public AvailabilityType getAvailability() {
		try {
			EmsConnection connection = parentComponent.getEmsConnection();
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

	public EmsConnection getEmsConnection() {
		return parentComponent.getEmsConnection();
	}

	public ProfileServiceConnection getConnection() {
		return parentComponent.getConnection();
	}

	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> metrics) throws Exception {

		EmsBean detailComponent = getEmsConnection().getBean(beanName);

		for (MeasurementScheduleRequest request : metrics) {

			String metricName = request.getName();
			try {
				EmsAttribute atribute = detailComponent
						.getAttribute(metricName);

				Object value = atribute.refresh();

				if (request.getDataType() == DataType.MEASUREMENT) {
					Number number = (Number) value;
					report.addData(new MeasurementDataNumeric(request, number
							.doubleValue()));
				} else if (request.getDataType() == DataType.TRAIT) {
					report.addData(new MeasurementDataTrait(request, value
							.toString()));
				}
			} catch (Exception e) {
				log.error(" Failure to collect measurement data from metric "
						+ metricName + " from bean "
						+ detailComponent.getBeanName().toString(), e);
			}

		}

	}

	public OperationResult invokeOperation(String name, Configuration parameters)
			throws InterruptedException, Exception {
		OperationResult result = null;

		try {
			EmsBean detailComponent = getEmsConnection().getBean(beanName);

			EmsOperation operation = detailComponent.getOperation(name);

			Object obj = operation.invoke(new Object[] {});

			if (obj != null)
				result = new OperationResult(String.valueOf(obj));

		} catch (Exception e) {
			log.error(" Failure to invoke operation " + name + " on bean "
					+ beanName, e);
		}
		return result;
	}

}
