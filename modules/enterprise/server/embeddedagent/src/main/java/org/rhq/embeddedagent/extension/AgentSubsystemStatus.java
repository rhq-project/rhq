package org.rhq.embeddedagent.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;

class AgentSubsystemStatus implements OperationStepHandler {

	static final AgentSubsystemStatus INSTANCE = new AgentSubsystemStatus();

	private AgentSubsystemStatus() {
    }

    @Override
    public void execute(OperationContext opContext, ModelNode model) throws OperationFailedException {
        boolean isStarted = false;
        try {
            ServiceName name = AgentService.SERVICE_NAME;
            AgentService service = (AgentService) opContext.getServiceRegistry(true).getRequiredService(name)
                    .getValue();
            isStarted = service.isAgentStarted();
        } catch (ServiceNotFoundException snfe) {
            // the agent just isn't deployed, so obviously, is isn't started
            isStarted = false;
		}
        opContext.getResult().set(isStarted ? "STARTED" : "STOPPED");
        opContext.completeStep();
	}
}
