package org.rhq.embeddedagent.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;

class AgentSubsystemRestart implements OperationStepHandler {

    static final AgentSubsystemRestart INSTANCE = new AgentSubsystemRestart();

    private final Logger log = Logger.getLogger(AgentSubsystemRestart.class);

    private AgentSubsystemRestart() {
    }

    @Override
    public void execute(OperationContext opContext, ModelNode model) throws OperationFailedException {
        try {
            ServiceName name = AgentService.SERVICE_NAME;
            AgentService service = (AgentService) opContext.getServiceRegistry(true).getRequiredService(name)
                .getValue();
            log.info("Asked to restart the embedded agent");
            service.stopAgent();
            service.startAgent();
        } catch (ServiceNotFoundException snfe) {
            throw new OperationFailedException("Cannot restart embedded agent - the agent is disabled", snfe);
        } catch (StartException se) {
            throw new OperationFailedException("Cannot restart embedded agent", se);
        }

        opContext.completeStep();
        return;
    }
}
