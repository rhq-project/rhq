package org.rhq.embeddedagent.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

class AgentSubsystemStop implements OperationStepHandler {

	static final AgentSubsystemStop INSTANCE = new AgentSubsystemStop();

	private final Logger log = Logger.getLogger(AgentSubsystemStop.class);

	private AgentSubsystemStop() {
    }

    @Override
    public void execute(OperationContext opContext, ModelNode model) throws OperationFailedException {
        try {
            ServiceName name = AgentService.SERVICE_NAME;
            AgentService service = (AgentService) opContext.getServiceRegistry(true).getRequiredService(name)
                    .getValue();
            log.info("Asked to stop the embedded agent");
            service.stopAgent();
        } catch (Exception e) {
            // the agent service isn't deployed, so obviously, the agent is already stopped. just keep going
		}

        opContext.completeStep();
        return;
	}
}
