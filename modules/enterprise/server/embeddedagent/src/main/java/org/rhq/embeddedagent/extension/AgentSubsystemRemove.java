package org.rhq.embeddedagent.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

class AgentSubsystemRemove extends AbstractRemoveStepHandler {

    static final AgentSubsystemRemove INSTANCE = new AgentSubsystemRemove();

    private AgentSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
        throws OperationFailedException {

        ServiceName name = AgentService.SERVICE_NAME;
        context.removeService(name);
    }
}
