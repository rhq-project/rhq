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

	public static String CACHE_SERVICE_NAME = "service";
	public static String CACHE_DOMAIN_NAME = "domain";
	public static String CACHE_CONFIG_NAME = "config";
	public static String CACHE_JMX_NAME = "jmx-resource";
	
    private final Log log = LogFactory.getLog(this.getClass());
	
    private String service;
	private String domain;
	private String config;

	private ProfileServiceComponent parentComp;
	private ResourceContext context;
	private EmsBean cacheBean;

	public void start(ResourceContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {
		this.context = context;

		parentComp = context.getParentResourceComponent();

		Configuration configuration = context.getPluginConfiguration();

		config = configuration.getSimple(CACHE_CONFIG_NAME).getStringValue();
		service = configuration.getSimple(CACHE_SERVICE_NAME).getStringValue();
		domain = configuration.getSimple(CACHE_DOMAIN_NAME).getStringValue();

		EmsConnection connection = getEmsConnection();
		String beanName = domain + ":" + config + "," + service;
		cacheBean = connection.getBean(beanName);
        
        return;
	}

	public void stop() {
		return;
	}

	public AvailabilityType getAvailability() {		
		return AvailabilityType.UP;
	}

	public ProfileServiceConnection getConnection() {
		return parentComp.getConnection();
	}

	public EmsConnection getEmsConnection() {
		return parentComp.getEmsConnection();
	}

	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> metrics) throws Exception {

		if (cacheBean == null) {
			return;
        }

		for (MeasurementScheduleRequest request : metrics) {
			String metricName = request.getName();
			try {
				EmsAttribute atribute = cacheBean.getAttribute(metricName);
				Object value = atribute.getValue();
                if (value == null) {
                    continue;
                }
                
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
						+ cacheBean.getBeanName(), e);
			}

		}

	}

	public OperationResult invokeOperation(String name, Configuration parameters)
			throws InterruptedException, Exception {

		if (cacheBean == null) {
			return null;		
        }
		EmsOperation operation = cacheBean.getOperation(name);
		String res = String.valueOf(operation.invoke(new Object[] {}));
		OperationResult result = new OperationResult(res);
		return result;
	}
}
