package org.rhq.modules.plugins.wildfly10;

import java.util.ArrayList;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Component class for ModCluster
 * @author Heiko W. Rupp
 */
public class ModClusterComponent extends BaseComponent implements OperationFacet, ConfigurationFacet {

    static String DYNAMIC_PROVIDER = ",dynamic-load-provider=configuration";

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        Operation op = new Operation(name, getAddress());
        OperationResult operationResult = new OperationResult();
        Result result = null;

        String modClusterKeyMesg = "Modcluster resource keys are not in correct format.";
        modClusterKeyMesg += " Should be {modcluster address}:{jvmRoute}:{virtual-host}:{context} but instead ";
        modClusterKeyMesg += " was '" + key + "'";

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
            if (result != null && result.isSuccess()) {
                operationResult.setSimpleResult("Success");
            }
        } else if ("remove-proxy".equals(name)) {
            addAdditionalToOp(op, parameters, "host", false);
            addAdditionalToOp(op, parameters, "port", false);
            result = getASConnection().execute(op);
            if (result != null && result.isSuccess()) {
                operationResult.setSimpleResult("Success");
            }
        } else if ("disable-context".equals(name)) {//disable handled by base case
            //update the operation components with details from the resource being operated on.
            //Ex. {modcluster address}:{jvmRoute}:{virtual-host}:{context}
            String[] keyComponents = key.split(":");
            if (keyComponents.length == 4) {
                op.addAdditionalProperty("virtualhost", keyComponents[2]);
                op.addAdditionalProperty("context", keyComponents[3]);
                result = getASConnection().execute(op);
                if ((result != null) && (result.isSuccess())) {
                    operationResult.setSimpleResult("Success");
                }
            } else {
                operationResult.setErrorMessage(modClusterKeyMesg);
                return operationResult;
            }
        } else if ("enable-context".equals(name)) {//enable handled by base case
            String currentAddress = getAddress().getPath();
            //update the operation components with details from the resource being operated on.
            //Ex. {modcluster address}:{jvmRoute}:{virtual-host}:{context}
            String[] keyComponents = key.split(":");
            if (keyComponents.length == 4) {
                op.addAdditionalProperty("virtualhost", keyComponents[2]);
                op.addAdditionalProperty("context", keyComponents[3]);
                result = getASConnection().execute(op);
                if ((result != null) && (result.isSuccess())) {
                    operationResult.setSimpleResult("Success");
                }
            } else {
                operationResult.setErrorMessage(modClusterKeyMesg);
                return operationResult;
            }
        } else if ("stop-context".equals(name)) {
            String currentAddress = getAddress().getPath();
            //Ex. {modcluster address}:{jvmRoute}:{virtual-host}:{context}
            String[] keyComponents = key.split(":");
            if (keyComponents.length == 4) {
                op.addAdditionalProperty("virtualhost", keyComponents[2]);
                op.addAdditionalProperty("context", keyComponents[3]);
                addAdditionalToOp(op, parameters, "waittime", true);
                result = getASConnection().execute(op);
                if ((result != null) && (result.isSuccess())) {
                    operationResult.setSimpleResult("Success");
                }
            } else {
                operationResult.setErrorMessage(modClusterKeyMesg);
                return operationResult;
            }
        } else if ("add-custom-metric".equals(name)) {
            //update the address and operation name. Use class name as identifier.
            String newOperationDestination = getAddress().getPath() + DYNAMIC_PROVIDER
                + ",custom-load-metric,custom-load-metric=" + retrieveNewIdentifier(parameters, "class");
            op = new Operation("add", new Address(newOperationDestination));
            addAdditionalToOp(op, parameters, "class", false);
            addAdditionalToOp(op, parameters, "weight", false);
            addAdditionalToOp(op, parameters, "capacity", true);
            result = getASConnection().execute(op);
            if ((result != null) && (result.isSuccess())) {
                operationResult.setSimpleResult("Success");
            }
        } else if ("remove-custom-metric".equals(name)) {
            //update the address and operation name. Use class name as identifier.
            String newOperationDestination = getAddress().getPath() + DYNAMIC_PROVIDER
                + ",custom-load-metric,custom-load-metric=" + retrieveNewIdentifier(parameters, "class");
            op = new Operation("remove", new Address(newOperationDestination));
            addAdditionalToOp(op, parameters, "class", false);
            result = getASConnection().execute(op);
            if ((result != null) && (result.isSuccess())) {
                operationResult.setSimpleResult("Success");
            }
        } else if ("add-metric".equals(name)) {
            //update the address and operation name. Use class name as identifier.
            String newOperationDestination = getAddress().getPath() + DYNAMIC_PROVIDER
                + ",custom-load-metric,load-metric=" + retrieveNewIdentifier(parameters, "type");
            op = new Operation("add", new Address(newOperationDestination));
            addAdditionalToOp(op, parameters, "weight", false);
            addAdditionalToOp(op, parameters, "capacity", true);
            addAdditionalToOp(op, parameters, "type", false);
            result = getASConnection().execute(op);
            if ((result != null) && (result.isSuccess())) {
                operationResult.setSimpleResult("Success");
            }
        } else if ("remove-metric".equals(name)) {
            //update the address and operation name. Use class name as identifier.
            String newOperationDestination = getAddress().getPath() + DYNAMIC_PROVIDER
                + ",custom-load-metric,load-metric=" + retrieveNewIdentifier(parameters, "type");
            op = new Operation("remove", new Address(newOperationDestination));
            addAdditionalToOp(op, parameters, "type", false);
            result = getASConnection().execute(op);
            if ((result != null) && (result.isSuccess())) {
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
            result = getASConnection().execute(op);
            if (result.isSuccess()) {
                operationResult.setSimpleResult("Success");
            }
        }

        if (!result.isSuccess()) {
            operationResult.setErrorMessage(result.getFailureDescription());
        }

        return operationResult;
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

    /** Parses the Configuration passed in and retrieves the value of the parameterName
     *  to be used as the resource key.
     * 
     * @param parameters  Configuration
     * @param parameterName specific property value to retrieve.
     * @return
     */
    String retrieveNewIdentifier(Configuration parameters, String parameterName) {
        String identifier = "";
        //retrieve the value of the specific property identified by parameterName
        if ((parameters != null) && (parameterName != null) && !parameterName.isEmpty()) {
            identifier = parameters.getSimpleValue(parameterName, null);
        }
        return identifier;
    }

    @Override
    public Address getAddress() {
        return new Address(key);
    }

}
