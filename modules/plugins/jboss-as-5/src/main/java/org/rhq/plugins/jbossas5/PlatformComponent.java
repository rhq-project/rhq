package org.rhq.plugins.jbossas5;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.adapter.api.MeasurementAdapter;
import org.rhq.plugins.jbossas5.adapter.impl.measurement.SimpleMetaValueMeasurementAdapter;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.ResourceTypeUtils;
import org.mc4j.ems.connection.EmsConnection;

public class PlatformComponent implements
		ProfileServiceComponent<ProfileServiceComponent>, MeasurementFacet,
		OperationFacet, ConfigurationFacet {

	private final Log log = LogFactory.getLog(this.getClass());
	private ResourceContext<ProfileServiceComponent> context;
	private String deployName;
	private String componentName;

	public PlatformComponent() {
	}

	public void start(ResourceContext<ProfileServiceComponent> context)
			throws InvalidPluginConfigurationException, Exception {
		this.context = context;
	}

	public void stop() {
		// TODO Auto-generated method stub

	}

	public ProfileServiceConnection getConnection() {
		ApplicationServerComponent component = (ApplicationServerComponent) context
				.getParentResourceComponent();
		return component.getConnection();

	}

    public EmsConnection getEmsConnection() {
        return context.getParentResourceComponent().getEmsConnection();
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
		// TODO Auto-generated method stub
		return null;
	}

	// ------------ MeasurementFacet Implementation ------------

	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> requests) {
		try {
			ManagedComponent component = loadComponent();
			String[] propertyName;

			for (MeasurementScheduleRequest request : requests) {
				String metricName = request.getName();

				propertyName = parsePropertyName(metricName);
				if (propertyName.length == 0)
					continue;

				ManagedProperty prop = component.getProperties().get(
						propertyName[0]);

				if (prop != null) {
					MetaType type = prop.getMetaType();
					MetaValue value = prop.getValue();

					if (value != null) {
						MeasurementDefinition measurementDefinition = ResourceTypeUtils
								.getMeasurementDefinition(context
										.getResourceType(), metricName);

						if (type.isSimple()) {
							MeasurementAdapter measurementAdapter = new SimpleMetaValueMeasurementAdapter();
							if (measurementDefinition != null
									& measurementAdapter != null)
								measurementAdapter.setMeasurementData(report,
										value, request, measurementDefinition);
						}
						if (type.isComposite()) {

							ManagedObject obj = prop.getManagedObject();
							Object attachment = obj.getAttachment();

							for (int i = 0; i < propertyName.length; i++) {
								attachment = getObjectProperty(attachment,
										propertyName[i]);
							}

							setMeasurementData(report, attachment, request,
									measurementDefinition);

						}

					}
				}
			}

		} catch (NoSuchDeploymentException e) {
			log.error("Loaded component does not exist: " + e);
		} catch (SecurityException e) {
			log.error("Security Exception: " + e);

		}
	}

	public Configuration loadResourceConfiguration() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateResourceConfiguration(ConfigurationUpdateReport report) {
		// TODO Auto-generated method stub

	}

	private ManagedComponent loadComponent() throws NoSuchDeploymentException {

		deployName = context.getPluginConfiguration().getSimple(
				PlatformDiscoveryComponent.PLATFORM_DEPLOYMENT_NAME)
				.getStringValue();

		componentName = context.getPluginConfiguration().getSimple(
				PlatformDiscoveryComponent.PLATFORM_COMPONENT_NAME)
				.getStringValue();

		ProfileServiceConnection connection = context
				.getParentResourceComponent().getConnection();

		ManagementView managementView = connection.getManagementView();

		ManagedDeployment deploy = managementView.getDeployment(deployName);
		if (deploy != null) {
			Map<String, ManagedComponent> components = deploy.getComponents();

			return components.get(componentName);
		}

		return null;
	}

	public AvailabilityType getAvailability() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] parsePropertyName(String name) {

		String[] ps;

		if (name.indexOf('{') == -1) {
			ps = new String[1];
			ps[0] = name;
			return ps;
		}
		name = name.substring(name.indexOf('{') + 1, name.indexOf('}'));
		ps = name.split("\\.");

		return ps;
	}

	protected Object getObjectProperty(Object value, String property) {

		try {
			PropertyDescriptor[] pds = Introspector.getBeanInfo(
					value.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				if (pd.getName().equals(property)) {
					pd.getReadMethod().setAccessible(true);
					value = pd.getReadMethod().invoke(value);
				}
			}
		} catch (Exception e) {
			log.error("Object " + value.toString()
					+ " does not contain property " + property, e);
		}

		return value;
	}

	public void setMeasurementData(MeasurementReport report, Object value,
			MeasurementScheduleRequest request,
			MeasurementDefinition measurementDefinition) {

		DataType dataType = measurementDefinition.getDataType();
		switch (dataType) {
		case MEASUREMENT:
			try {
				MeasurementDataNumeric dataNumeric = new MeasurementDataNumeric(
						request, new Double(value.toString()));
				report.addData(dataNumeric);
			} catch (NumberFormatException e) {
				log
						.warn(
								"Measurement request: "
										+ request.getName()
										+ " did not return a numeric value from the Profile Service",
								e);
			}
			break;
		case TRAIT:
			MeasurementDataTrait dataTrait = new MeasurementDataTrait(request,
					String.valueOf(value));
			report.addData(dataTrait);
			break;
		default:
			throw new IllegalStateException(
					"Unsupported measurement data type: " + dataType);
		}
	}

	public OperationResult invokeOperation(String name, Configuration parameters)
			throws InterruptedException, Exception {

		ResourceType resourceType = context.getResourceType();
		OperationDefinition operationDefinition = ResourceTypeUtils
				.getOperationDefinition(resourceType, name);

		ManagedComponent component = loadComponent();
		Set<ManagedOperation> operations = component.getOperations();

		for (ManagedOperation operation : operations) {
			if (operation.getName().equals(name)) {
				MetaValue[] params = ConversionUtils
						.convertOperationsParametersToMetaValues(operation,
								parameters, operationDefinition);
				MetaValue operationResult = operation.invoke(params);

				OperationResult result = new OperationResult();
				ConversionUtils.convertManagedOperationResults(operation,
						operationResult, result.getComplexResults(),
						operationDefinition);

				return result;
			}
		}
		return null;
	}

}
