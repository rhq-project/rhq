package org.rhq.embeddedagent.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * Handler responsible for adding the subsystem resource to the model
 */
class AgentSubsystemAdd extends AbstractAddStepHandler {

    static final AgentSubsystemAdd INSTANCE = new AgentSubsystemAdd();

    private final Logger log = Logger.getLogger(AgentSubsystemAdd.class);

    private AgentSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        AgentSubsystemDefinition.AGENT_ENABLED_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.PLUGINS_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.AGENT_NAME_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.DISABLE_NATIVE_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.SERVER_TRANSPORT_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.SERVER_BIND_PORT_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.SERVER_BIND_ADDRESS_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.SERVER_TRANSPORT_PARAMS_ATTRIBDEF.validateAndSet(operation, model);
        AgentSubsystemDefinition.SERVER_ALIAS_ATTRIBDEF.validateAndSet(operation, model);
        log.info("Populating the embedded agent subsystem model: " + operation + "=" + model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        boolean enabled = AgentSubsystemDefinition.AGENT_ENABLED_ATTRIBDEF.resolveModelAttribute(context, model)
                .asBoolean(AgentSubsystemExtension.AGENT_ENABLED_DEFAULT);

        if (!enabled) {
            log.info("Embedded agent is not enabled and will not be deployed");
            return;
        }

        log.info("Embedded agent is enabled and will be deployed");

        // figure out what plugins we are to support
        HashMap<String, Boolean> pluginsWithEnableFlag = new HashMap<String, Boolean>();
        ModelNode pluginsNode = AgentSubsystemDefinition.PLUGINS_ATTRIBDEF.resolveModelAttribute(context, model);
        if (pluginsNode != null && pluginsNode.isDefined()) {
            List<Property> pluginsList = pluginsNode.asPropertyList();
            for (Property pluginsItem : pluginsList) {
                String pluginName = pluginsItem.getName();
                boolean pluginEnabled = pluginsItem.getValue().asBoolean();
                pluginsWithEnableFlag.put(pluginName, pluginEnabled);
            }
        }

        // set up our runtime configuration overrides that should be used instead of the out-of-box config
        Map<String, String> overrides = new HashMap<String, String>();
        addOverrideProperty(context, model, overrides, AgentSubsystemDefinition.AGENT_NAME_ATTRIBDEF);
        addOverrideProperty(context, model, overrides, AgentSubsystemDefinition.DISABLE_NATIVE_ATTRIBDEF);
        addOverrideProperty(context, model, overrides, AgentSubsystemDefinition.SERVER_TRANSPORT_ATTRIBDEF);
        addOverrideProperty(context, model, overrides, AgentSubsystemDefinition.SERVER_BIND_PORT_ATTRIBDEF);
        addOverrideProperty(context, model, overrides, AgentSubsystemDefinition.SERVER_BIND_ADDRESS_ATTRIBDEF);
        addOverrideProperty(context, model, overrides, AgentSubsystemDefinition.SERVER_TRANSPORT_PARAMS_ATTRIBDEF);
        addOverrideProperty(context, model, overrides, AgentSubsystemDefinition.SERVER_ALIAS_ATTRIBDEF);

        // create our service
        AgentService service = new AgentService();
        service.setPlugins(pluginsWithEnableFlag);
        service.setConfigurationOverrides(overrides);

        // install the service
        ServiceName name = AgentService.SERVICE_NAME;
        ServiceController<AgentService> controller = context.getServiceTarget() //
            .addService(name, service) //
            .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.envServiceValue) //
            .addListener(verificationHandler) //
            .setInitialMode(Mode.ACTIVE) //
            .install();
        newControllers.add(controller);
        return;
    }

    private void addOverrideProperty(OperationContext context, ModelNode model, Map<String, String> overrides,
        AttributeDefinition attribDef)
        throws OperationFailedException {
        ModelNode node = attribDef.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            overrides.put(attribDef.getName(), node.asString());
        }
    }
}
