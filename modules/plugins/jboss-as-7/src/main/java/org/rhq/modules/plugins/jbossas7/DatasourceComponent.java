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
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
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
 * Handle JDBC-driver related stuff
 * @author Heiko W. Rupp
 */
public class DatasourceComponent extends BaseComponent<BaseComponent<?>> implements OperationFacet, ConfigurationFacet {

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
        } else if (operationName.equals("addDatasource")) {
            String name = parameters.getSimpleValue("name", NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("data-source", name);
            op = new Operation("add", theAddress);
            addRequiredToOp(op, parameters, "driver-name");
            addRequiredToOp(op, parameters, "jndi-name");
            addRequiredToOp(op, parameters, "connection-url");
            addOptionalToOp(op, parameters, "user-name");
            addOptionalToOp(op, parameters, "password");
        } else if (operationName.equals("addXADatasource")) {
            String name = parameters.getSimpleValue("name", NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("xa-data-source", name);
            op = new CompositeOperation();
            Operation step1 = new Operation("add", theAddress);
            addRequiredToOp(step1, parameters, "driver-name");
            addRequiredToOp(step1, parameters, "jndi-name");
            addOptionalToOp(step1, parameters, "user-name");
            addOptionalToOp(step1, parameters, "password");
            addRequiredToOp(step1, parameters, "xa-datasource-class");

            ((CompositeOperation) op).addStep(step1);

            // handling of xa-properties -- this is now a subresource in AS7 and at least needs a connection url
            String connectionUrl = parameters.getSimpleValue("connection-url", null);
            if (connectionUrl == null || connectionUrl.isEmpty())
                throw new IllegalArgumentException("Connection-url must not be empty");
            Address cuAddress = new Address(theAddress);
            cuAddress.add("xa-datasource-properties", "connection-url");
            Operation step2 = new Operation("add", cuAddress);
            step2.addAdditionalProperty("value", connectionUrl);
            ((CompositeOperation) op).addStep(step2);

            PropertyList xaPropList = parameters.getList("xa-properties");
            if (xaPropList != null) {
                List<Property> xaProps = xaPropList.getList();
                for (Property prop : xaProps) {
                    PropertyMap pMap = (PropertyMap) prop;
                    PropertySimple keyProp = pMap.getSimple("key");
                    PropertySimple valProp = pMap.getSimple("value");
                    Address propAddress = new Address(theAddress);
                    propAddress.add("xa-datasource-properties", keyProp.getStringValue());
                    Operation step = new Operation("add", propAddress);
                    step.addAdditionalProperty("value", valProp.getStringValue()); // TODO ??
                    ((CompositeOperation) op).addStep(step);
                }
            }
        } else {
            /*
             * This is a catch all for operations that are not explicitly treated above.
             */
            op = new Operation(operationName, address);
        }

        Result res = connection.execute(op);
        if (res.isSuccess()) {
            result.setSimpleResult("Success");
        } else {
            result.setErrorMessage(res.getFailureDescription());
        }

        return result;
    }

    void addAdditionalToOp(Operation op, Configuration parameters, String parameterName, boolean optional) {
        String value = parameters.getSimpleValue(parameterName, null);
        if (value == null) {
            if (!optional) {
                throw new IllegalArgumentException("Required parameter [" + parameterName + "] for operation ["
                    + op.getName() + "] is not defined.");
            }
        } else {
            op.addAdditionalProperty(parameterName, value);
        }
    }

    void addRequiredToOp(Operation op, Configuration parameters, String property) {
        addAdditionalToOp(op, parameters, property, false);
    }

    void addOptionalToOp(Operation op, Configuration parameters, String property) {
        addAdditionalToOp(op, parameters, property, true);
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Operation op = new Operation("disable", getAddress());
        Result res = getASConnection().execute(op);
        if (!res.isSuccess()) {
            report.setErrorMessage("Was not able to disable the datasource for config changes: "
                + res.getFailureDescription());
            return;
        }

        super.updateResourceConfiguration(report);

        op = new Operation("enable", getAddress());
        res = getASConnection().execute(op);

    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

        Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request : requests) {
            if (request.getName().equals("connectionAvailable")) {
                report.addData(getConnectionAvailable(request));
            } else if (request.getName().equals("max-pool-size")) {
                getMaxPoolSizeAsMetric(report, request);
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

    private void getMaxPoolSizeAsMetric(MeasurementReport report, MeasurementScheduleRequest request) {
        Operation op = new ReadAttribute(getAddress(), "max-pool-size");
        Result res = getASConnection().execute(op);

        if (res.isSuccess()) {
            String tmp = (String) res.getResult();
            if (tmp == null) { // server r
                tmp = "20"; // The default value
            }
            Double val = Double.valueOf(tmp);
            MeasurementDataNumeric data = new MeasurementDataNumeric(request, val);
            report.addData(data);
        } else {
            log.warn("Could not read max-pool-size on " + getAddress() + ": " + res.getFailureDescription());
        }
    }

    private MeasurementDataTrait getConnectionAvailable(MeasurementScheduleRequest request) {
        Operation op = new Operation("test-connection-in-pool", getAddress());
        Result res = getASConnection().execute(op);

        MeasurementDataTrait trait = new MeasurementDataTrait(request, String.valueOf(res.isSuccess()));

        return trait;
    }
}
