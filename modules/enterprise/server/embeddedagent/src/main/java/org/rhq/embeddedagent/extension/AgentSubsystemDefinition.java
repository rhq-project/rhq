package org.rhq.embeddedagent.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class AgentSubsystemDefinition extends SimpleResourceDefinition {

    public static final AgentSubsystemDefinition INSTANCE = new AgentSubsystemDefinition();

    protected static final SimpleAttributeDefinition AGENT_ENABLED_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.AGENT_ENABLED, ModelType.BOOLEAN).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.AGENT_ENABLED).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setDefaultValue(new ModelNode(AgentSubsystemExtension.AGENT_ENABLED_DEFAULT)).setAllowNull(false).build();

    protected static final PluginsAttributeDefinition PLUGINS_ATTRIBDEF = new PluginsAttributeDefinition();

    private AgentSubsystemDefinition() {
        super(AgentSubsystemExtension.SUBSYSTEM_PATH, AgentSubsystemExtension.getResourceDescriptionResolver(null),
            AgentSubsystemAdd.INSTANCE, AgentSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration rr) {
        rr.registerReadWriteAttribute(AGENT_ENABLED_ATTRIBDEF, null, AgentEnabledAttributeHandler.INSTANCE);
        rr.registerReadWriteAttribute(PLUGINS_ATTRIBDEF, null, PluginsAttributeHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration rr) {
        super.registerOperations(rr);

        // We always need to add a 'describe' operation
        rr.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE,
            GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        rr.registerOperationHandler(
            AgentSubsystemExtension.AGENT_RESTART_OP,
            AgentSubsystemRestart.INSTANCE,
            new DefaultOperationDescriptionProvider(AgentSubsystemExtension.AGENT_RESTART_OP, AgentSubsystemExtension
                .getResourceDescriptionResolver(null)), false, OperationEntry.EntryType.PUBLIC);

        rr.registerOperationHandler(
            AgentSubsystemExtension.AGENT_STOP_OP,
            AgentSubsystemStop.INSTANCE,
            new DefaultOperationDescriptionProvider(AgentSubsystemExtension.AGENT_STOP_OP, AgentSubsystemExtension
                .getResourceDescriptionResolver(null)), false, OperationEntry.EntryType.PUBLIC);

        rr.registerOperationHandler(
            AgentSubsystemExtension.AGENT_STATUS_OP,
            AgentSubsystemStatus.INSTANCE,
            new DefaultOperationDescriptionProvider(AgentSubsystemExtension.AGENT_STATUS_OP, AgentSubsystemExtension
                .getResourceDescriptionResolver(null), ModelType.STRING), false, OperationEntry.EntryType.PUBLIC);

        return;
    }
}
