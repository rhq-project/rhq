package org.rhq.modules.plugins.jbossas7;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle Datasorces (possibly jdbc-driver) related stuff
 * @author Heiko W. Rupp
 */
public class DatasourceComponent extends BaseComponent<BaseComponent<?>> implements OperationFacet, ConfigurationFacet,
        CreateChildResourceFacet
{

    private static final String NOTSET = "-notset-";
    private final Log log = LogFactory.getLog(DatasourceComponent.class);

    @Override
    public OperationResult invokeOperation(String operationName, Configuration parameters) throws Exception { // TODO still needed ? Check with plugin descriptor

        OperationResult result = new OperationResult();
        ASConnection connection = getASConnection();
        Operation op;

        if (operationName.equals("addDriver")) { // TODO decide if we need this at all. See also the plugin-descriptor
            String drivername = parameters.getSimpleValue("driver-name", NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("jdbc-driver", drivername);

            op = new Operation("add", theAddress);
            op.addAdditionalProperty("driver-name", drivername);
            op.addAdditionalProperty("deployment-name", parameters.getSimpleValue("deployment-name", NOTSET));
            op.addAdditionalProperty("driver-class-name", parameters.getSimpleValue("driver-class-name", NOTSET));
        } else {
            /*
             * This is a catch all for operations that are not explicitly treated above.
             */
            op = new Operation(operationName, address);
        }

        Result res = connection.execute(op);
        if (res.isSuccess()) {
            result.setSimpleResult("Success");

            if ("enable".equals(operationName)) {
                context.getAvailabilityContext().enable();
            } else if ("disable".equals(operationName)) {
                context.getAvailabilityContext().disable();
            }

        } else {
            result.setErrorMessage(res.getFailureDescription());
        }

        return result;
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        CreateResourceReport resourceReport = super.createResource(report);

        // No success -> no point in continuing
        if (resourceReport.getStatus() != CreateResourceStatus.SUCCESS)
            return resourceReport;

        // outer create resource did not cater for the xa properties, so lets add them now
        if (report.getResourceType().getName().toLowerCase().contains("xa")) {
            PropertyList listPropertyWrapper = report.getResourceConfiguration().getList("*2");
            List<Property> listProperty = listPropertyWrapper.getList();

            String baseAddress = resourceReport.getResourceKey();

            if (!listProperty.isEmpty()) {
                CompositeOperation cop = new CompositeOperation();
                for (Property p : listProperty) {
                    PropertyMap map = (PropertyMap) p;
                    String key = map.getSimpleValue("key", null);
                    String value = map.getSimpleValue("value", null);
                    if (key == null || value == null)
                        continue;

                    Address propertyAddress = new Address(baseAddress);
                    propertyAddress.add("xa-datasource-properties", key);
                    Operation op = new Operation("add", propertyAddress);
                    op.addAdditionalProperty("value", value);
                    cop.addStep(op);

                }

                Result res = getASConnection().execute(cop);
                if (!res.isSuccess()) {
                    resourceReport.setErrorMessage("Datasource was added, but setting xa-properties failed: "
                        + res.getFailureDescription());
                    resourceReport.setStatus(CreateResourceStatus.FAILURE);
                }
            }
        }
        return resourceReport;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

        Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request : requests) {
            if (request.getName().equals("connectionAvailable")) {
                report.addData(getConnectionAvailable(request));
            } else if (request.getName().equals("max-pool-size")) {
                getRCAsMetric(report, request);
            } else if (request.getName().equals("min-pool-size")) {
                getRCAsMetric(report, request);
            } else {
                metrics.add(request);
            }
        }

        /*
         * Remainder here are metrics that can be read from the resource.
         * Those
         */
        ReadResource op = new ReadResource(address);
        op.includeRuntime(true);
        op.recursive(true);
        ComplexResult res = getASConnection().executeComplex(op);
        if (!res.isSuccess())
            return;

        Map<String, Object> results = new HashMap<String, Object>();
        Map<String, Object> statistics = (Map<String, Object>) res.getResult().get("statistics");
        if (statistics!=null) {
            results.putAll((Map<? extends String, ? extends Object>) statistics.get("pool"));
            results.putAll((Map<? extends String, ? extends Object>) statistics.get("jdbc"));

            for (MeasurementScheduleRequest metric : metrics) {
                String name = metric.getName();

                Object o = results.get(name);
                if (o != null) {
                    String tmp = (String) o;
                    Double val = Double.valueOf(tmp);
                    MeasurementDataNumeric data = new MeasurementDataNumeric(metric, val);
                    report.addData(data);
                }
            }
        }
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration config = report.getConfiguration();
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();

        //These properties cannot be undefined once set.
        //Also the AS7 server does not accept null values even if the properties are still unset.
        replaceWithDefaultIfNull("max-pool-size", config, configDef);
        replaceWithDefaultIfNull("min-pool-size", config, configDef);
        replaceWithDefaultIfNull("pool-prefill", config, configDef);
        replaceWithDefaultIfNull("pool-use-strict-min", config, configDef);
        replaceWithDefaultIfNull("blocking-timeout-wait-millis", config, configDef);
        replaceWithDefaultIfNull("idle-timeout-minutes", config, configDef);
        replaceWithDefaultIfNull("background-validation-millis", config, configDef);
        replaceWithDefaultIfNull("background-validation-minutes", config, configDef);
        replaceWithDefaultIfNull("background-validation", config, configDef);

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }

    private void getRCAsMetric(MeasurementReport report, MeasurementScheduleRequest request) {
        Operation op = new ReadAttribute(getAddress(), request.getName());
        Result res = getASConnection().execute(op);

        if (res.isSuccess()) {
            Integer tmp = (Integer) res.getResult();
            if (tmp == null) { // server
                if (request.getName().equals("max-pool-size"))
                    tmp = 20; // The default value
                else if (request.getName().equals("min-pool-size"))
                    tmp = 0; // The default value
                else
                    tmp =-1; // Fallback for unknown requests
            }
            Double val = Double.valueOf(tmp);
            MeasurementDataNumeric data = new MeasurementDataNumeric(request, val);
            report.addData(data);
        } else {
            log.warn("Could not read [" + request.getName() + "] on " + getAddress() + ": " + res.getFailureDescription());
        }
    }

    private MeasurementDataTrait getConnectionAvailable(MeasurementScheduleRequest request) {
        Operation op = new Operation("test-connection-in-pool", getAddress());
        Result res = getASConnection().execute(op);

        MeasurementDataTrait trait = new MeasurementDataTrait(request, String.valueOf(res.isSuccess()));

        return trait;
    }

    /**
     * Replace the value configured by the user with the default value from the resource descriptor if
     * the value to be sent to the server is null or empty.
     *
     * @param propertyName property name
     * @param config configuration update
     * @param configDef configuration definition
     */
    private void replaceWithDefaultIfNull(String propertyName, Configuration config, ConfigurationDefinition configDef) {
        PropertyDefinitionSimple propertyDefinition = configDef.getPropertyDefinitionSimple(propertyName);

        if (propertyDefinition != null) {
            String propertyValue = config.getSimpleValue(propertyName);
            if (propertyValue == null || propertyValue.isEmpty()) {
                config.put(new PropertySimple(propertyName, propertyDefinition.getDefaultValue()));
            }
        }
    }
}
