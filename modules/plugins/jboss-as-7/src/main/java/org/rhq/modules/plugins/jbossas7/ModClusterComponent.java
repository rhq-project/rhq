package org.rhq.modules.plugins.jbossas7;

import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
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
        Result result = getASConnection().execute(op);

        Map<String, Property> propertyMap = parameters.getAllProperties();
        for (Map.Entry<String, Property> entry : propertyMap.entrySet()) {
            PropertySimple ps = (PropertySimple) entry.getValue();
            op.addAdditionalProperty(entry.getKey(), ps.getStringValue());
        }

        OperationResult operationResult = new OperationResult();
        if (result.isSuccess()) {
            operationResult.setSimpleResult(result.getResult().toString());
        } else {
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
}
