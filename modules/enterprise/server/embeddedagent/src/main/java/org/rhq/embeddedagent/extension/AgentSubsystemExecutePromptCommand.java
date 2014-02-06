package org.rhq.embeddedagent.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;

class AgentSubsystemExecutePromptCommand implements OperationStepHandler {

    static final AgentSubsystemExecutePromptCommand INSTANCE = new AgentSubsystemExecutePromptCommand();

    private AgentSubsystemExecutePromptCommand() {
        }

    @Override
    public void execute(OperationContext opContext, ModelNode model) throws OperationFailedException {
        try {
            ServiceName name = AgentService.SERVICE_NAME;
            AgentService service = (AgentService) opContext.getServiceRegistry(true).getRequiredService(name)
                .getValue();

            String command = model.get(AgentSubsystemDefinition.EXECUTE_PROMPT_COMMAND_PARAM_COMMAND.getName())
                .asString();

            String results = service.executePromptCommand(command);
            opContext.getResult().set(results);
        } catch (ServiceNotFoundException snfe) {
            throw new OperationFailedException(
                "Cannot execute prompt command because the embedded agent is not enabled");
        } catch (Exception e) {
            throw new OperationFailedException("Failed to execute prompt command [" + model + "]", e);
        }
        opContext.completeStep();
    }
}
