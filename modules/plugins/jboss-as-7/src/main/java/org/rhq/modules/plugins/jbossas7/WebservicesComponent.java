package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Support for Webservices subsystem.
 *  
 * @author Simeon Pinder
 */
public class WebservicesComponent extends BaseComponent implements OperationFacet, ConfigurationFacet {

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = super.loadResourceConfiguration();

        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        super.updateResourceConfiguration(report);
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        Operation op = new Operation(name, getAddress());
        OperationResult operationResult = new OperationResult();
        Result result = null;

        if ("list-proxies".equals(name)) {

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
}
