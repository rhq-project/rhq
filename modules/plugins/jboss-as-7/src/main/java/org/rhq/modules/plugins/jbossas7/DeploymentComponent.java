package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Deal with deployments
 * @author Heiko W. Rupp
 */
public class DeploymentComponent extends BaseComponent implements OperationFacet{

    @Override
    public AvailabilityType getAvailability() {
        Operation op = new ReadAttribute(getAddress(),"enabled");
        Result res = getASConnection().execute(op);
        if (!res.isSuccess())
            return AvailabilityType.DOWN;

        if (!(Boolean)(res.getResult()))
            return AvailabilityType.DOWN;

        return AvailabilityType.UP;
    }

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        String action;
        if (name.equals("enable")) {
            action = "deploy";
        } else if (name.equals("disable")) {
            action = "undeploy";
        } else {
            return super.invokeOperation(name, parameters);
        }

        Operation op = new Operation(action,getAddress());
        Result res = getASConnection().execute(op);
        OperationResult result = new OperationResult();
        if (res.isSuccess())
            result.setSimpleResult("Success");
        else
            result.setErrorMessage(res.getFailureDescription());

        return result;
    }
}
