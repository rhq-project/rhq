package org.rhq.modules.plugins.jbossas7;

import java.util.ArrayList;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for ModCluster
 * @author Heiko W. Rupp
 */
public class ModClusterComponent extends BaseComponent implements OperationFacet, ConfigurationFacet {

    //Ex. "/subsystem=modcluster/mod-cluster-config=configuration/" or following is valid.
    private static String CONFIG_ADDRESS = ",mod-cluster-config=configuration";

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {

        Operation op = new Operation(name, getAddress());
        OperationResult operationResult = new OperationResult();
        Result result = null;

        if ("list-proxies".equals(name)) {
            result = getASConnection().execute(op);
            if ((result != null) && (result.isSuccess())) {
                ArrayList container = (ArrayList) result.getResult();
                if ((container != null) && !container.isEmpty()) {
                    Object type = container.get(0);
                    String values = "";
                    if (type instanceof String) {
                        for (int i = 0; i < container.size(); i++) {
                            values += container.get(i) + ",";
                        }
                        values = values.substring(0, values.length() - 1);
                    } else {
                        values = container.toString();
                    }
                    operationResult.getComplexResults().put(new PropertySimple("proxy-list", values));

                } else {//return empty value.
                    operationResult.getComplexResults().put(new PropertySimple("proxy-list", ""));
                }
            }
        } else if ("add-proxy".equals(name)) {
            addAdditionalToOp(op, parameters, "host", false);
            addAdditionalToOp(op, parameters, "port", false);
            result = getASConnection().execute(op);
            if (result.isSuccess()) {
                operationResult.setSimpleResult("Success");
            }
        } else if ("remove-proxy".equals(name)) {
            addAdditionalToOp(op, parameters, "host", false);
            addAdditionalToOp(op, parameters, "port", false);
            result = getASConnection().execute(op);
            if (result.isSuccess()) {
                operationResult.setSimpleResult("Success");
            }
        } else if ("read-proxies-configuration".equals(name)) {
            //spinder 3/25/12: Can we do better than displaying all content as massive string?
            //                 Content is unstructured/variable from httpd server.
            result = getASConnection().execute(op);
            if ((result != null) && (result.isSuccess())) {
                ArrayList container = (ArrayList) result.getResult();
                if ((container != null) && !container.isEmpty()) {
                    Object type = container.get(0);
                    String values = "";
                    if (type instanceof String) {
                        for (int i = 0; i < container.size(); i++) {
                            values += container.get(i) + ",";
                        }
                        values = values.substring(0, values.length() - 1);
                    } else {
                        values = container.toString();
                    }
                    operationResult.getComplexResults().put(new PropertySimple("current-proxy-config", values));

                } else {//return empty value.
                    operationResult.getComplexResults().put(new PropertySimple("current-proxy-config", ""));
                }
            }
        } else if ("read-proxies-info".equals(name)) {
            //spinder 3/25/12: Can we do better than displaying all content as massive string?
            //                 Content is unstructured/variable from httpd server.
            result = getASConnection().execute(op);
            if ((result != null) && (result.isSuccess())) {
                ArrayList container = (ArrayList) result.getResult();
                if ((container != null) && !container.isEmpty()) {
                    Object type = container.get(0);
                    String values = "";
                    if (type instanceof String) {
                        for (int i = 0; i < container.size(); i++) {
                            values += container.get(i) + ",";
                        }
                        values = values.substring(0, values.length() - 1);
                    } else {
                        values = container.toString();
                    }
                    operationResult.getComplexResults().put(new PropertySimple("current-proxy-info", values));

                } else {//return empty value.
                    operationResult.getComplexResults().put(new PropertySimple("current-proxy-info", ""));
                }
            }
        } else {
            /*
             * This is a catch all for operations that are not explicitly treated above.
             */
            op = new Operation(name, address);
        }

        if (!result.isSuccess()) {
            operationResult.setErrorMessage(result.getFailureDescription());
        }

        return operationResult;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {

        //retrieve config definition
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        //default address is not right. Update it
        Address modClusterConfigAddress = new Address(key + CONFIG_ADDRESS);
        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef, connection,
            modClusterConfigAddress);
        Configuration config = delegate.loadResourceConfiguration();

        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        //default address is not right. Update it
        Address modClusterConfigAddress = new Address(key + CONFIG_ADDRESS);
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, connection,
            modClusterConfigAddress);
        delegate.updateResourceConfiguration(report);

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
}
