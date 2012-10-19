package org.rhq.modules.plugins.jbossas7;

import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for ModCluster
 * @author Heiko W. Rupp
 */
public class ModClusterComponent extends BaseComponent implements OperationFacet {

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws Exception {

        Operation op = new Operation(name,getAddress());
        Result result = getASConnection().execute(op);

        Map<String,Property> propertyMap = parameters.getAllProperties();
        for (Map.Entry<String,Property> entry : propertyMap.entrySet()) {
            PropertySimple ps = (PropertySimple) entry.getValue();
            op.addAdditionalProperty(entry.getKey(),ps.getStringValue());
        }

        OperationResult operationResult = new OperationResult();
        if (result.isSuccess()) {
            operationResult.setSimpleResult(result.getResult().toString());
        }
        else {
            operationResult.setErrorMessage(result.getFailureDescription());
        }
        return operationResult;
    }
}
