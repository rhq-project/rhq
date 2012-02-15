package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle domain deployments
 * @author Heiko W. Rupp
 */
public class DomainDeploymentComponent extends DeploymentComponent{

    @Override
    public AvailabilityType getAvailability() {
        // Domain deployments have no 'enabled' attribute

        Operation op = new ReadResource(getAddress());
        Result res = getASConnection().execute(op);

        return (res!=null && res.isSuccess()) ? AvailabilityType.UP: AvailabilityType.DOWN;
    }
}
