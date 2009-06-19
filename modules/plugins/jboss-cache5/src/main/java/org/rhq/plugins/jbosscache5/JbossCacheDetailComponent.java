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

public class JbossCacheDetailComponent implements MeasurementFacet,
		OperationFacet, ProfileServiceComponent<ProfileServiceComponent> {

	public static String JMX_NAME = "jmx-name";
	public static String CACHE_DETAIL_BEAN_NAME = "bean-name";
	public ProfileServiceComponent parentComponent;
	public EmsBean detailComponent;
	private final Log log = LogFactory.getLog(this.getClass());

	public void start(ResourceContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		parentComponent = context.getParentResourceComponent();
		EmsConnection connection = getEmsConnection();

		String beanName = context.getPluginConfiguration().getSimple(
				CACHE_DETAIL_BEAN_NAME).getStringValue();
		detailComponent = connection.getBean(beanName);
	}

	public void stop() {
		// TODO Auto-generated method stub

	}

	public AvailabilityType getAvailability() {
		return AvailabilityType.UP;
	}

	public EmsConnection getEmsConnection() {
		return parentComponent.getEmsConnection();
	}

	public ProfileServiceConnection getConnection() {
		return parentComponent.getConnection();
	}

	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> metrics) throws Exception {

		if (detailComponent == null)
			return;

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

		if (detailComponent == null)
			return null;

		EmsOperation operation = detailComponent.getOperation(name);

		if (operation == null)
			return null;

		Object obj = operation.invoke(new Object[] {});

		if (obj != null)
			return new OperationResult(String.valueOf(obj));

		return null;
	}

}
