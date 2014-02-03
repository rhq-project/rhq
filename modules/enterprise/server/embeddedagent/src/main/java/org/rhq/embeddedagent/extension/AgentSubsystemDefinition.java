package org.rhq.embeddedagent.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;

public class AgentSubsystemDefinition extends SimpleResourceDefinition {

    public static final AgentSubsystemDefinition INSTANCE = new AgentSubsystemDefinition();

    protected static final SimpleAttributeDefinition AGENT_ENABLED_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.AGENT_ENABLED, ModelType.BOOLEAN).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.AGENT_ENABLED).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setDefaultValue(new ModelNode(AgentSubsystemExtension.AGENT_ENABLED_DEFAULT)).setAllowNull(false).build();

    protected static final PluginsAttributeDefinition PLUGINS_ATTRIBDEF = new PluginsAttributeDefinition();

    protected static final SimpleAttributeDefinition AGENT_NAME_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_AGENT_NAME, ModelType.STRING).setAllowExpression(true)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition DISABLE_NATIVE_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_DISABLE_NATIVE, ModelType.BOOLEAN).setAllowExpression(true)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).setAllowNull(true).build();

    protected static final SimpleAttributeDefinition SERVER_BIND_ADDRESS_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_SERVER_BIND_ADDRESS, ModelType.STRING).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.SERVER_ENDPOINT_ADDRESS_XML)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).setAllowNull(true).build();

    protected static final SimpleAttributeDefinition SERVER_BIND_PORT_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_SERVER_BIND_PORT, ModelType.STRING).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.SERVER_ENDPOINT_PORT_XML)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setDefaultValue(new ModelNode(AgentConfigurationConstants.DEFAULT_SERVER_BIND_PORT)).setAllowNull(false)
        .build();

    protected static final SimpleAttributeDefinition SERVER_TRANSPORT_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_SERVER_TRANSPORT, ModelType.STRING).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.SERVER_ENDPOINT_TRANSPORT_XML)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setDefaultValue(new ModelNode(AgentConfigurationConstants.DEFAULT_SERVER_TRANSPORT)).setAllowNull(false)
        .build();

    protected static final SimpleAttributeDefinition SERVER_TRANSPORT_PARAMS_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_SERVER_TRANSPORT_PARAMS, ModelType.STRING).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.SERVER_ENDPOINT_TRANSPORT_XML)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setDefaultValue(new ModelNode(AgentConfigurationConstants.DEFAULT_SERVER_TRANSPORT_PARAMS))
        .setAllowNull(false).build();

    protected static final SimpleAttributeDefinition SERVER_ALIAS_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_SERVER_ALIAS, ModelType.STRING).setAllowExpression(true)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).setAllowNull(true).build();

    protected static final SimpleAttributeDefinition SOCKET_BINDING_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_SOCKET_BINDING, ModelType.STRING)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).setDefaultValue(new ModelNode("embeddedagent"))
        .setValidator(new StringLengthValidator(1)).setAllowNull(false).build();

    protected static final SimpleAttributeDefinition AGENT_TRANSPORT_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_AGENT_TRANSPORT, ModelType.STRING).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.AGENT_ENDPOINT_TRANSPORT_XML)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setDefaultValue(new ModelNode(ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_TRANSPORT))
        .setAllowNull(true).build();

    protected static final SimpleAttributeDefinition AGENT_TRANSPORT_PARAMS_ATTRIBDEF = new SimpleAttributeDefinitionBuilder(
        AgentSubsystemExtension.ATTRIB_AGENT_TRANSPORT_PARAMS, ModelType.STRING).setAllowExpression(true)
        .setXmlName(AgentSubsystemExtension.AGENT_ENDPOINT_TRANSPORT_PARAMS_XML)
        .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        .setDefaultValue(new ModelNode(ServiceContainerConfigurationConstants.DEFAULT_CONNECTOR_TRANSPORT_PARAMS))
        .setAllowNull(true).build();

    private AgentSubsystemDefinition() {
        super(AgentSubsystemExtension.SUBSYSTEM_PATH, AgentSubsystemExtension.getResourceDescriptionResolver(null),
            AgentSubsystemAdd.INSTANCE, AgentSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration rr) {
        rr.registerReadWriteAttribute(AGENT_ENABLED_ATTRIBDEF, null, AgentEnabledAttributeHandler.INSTANCE);
        rr.registerReadWriteAttribute(PLUGINS_ATTRIBDEF, null, PluginsAttributeHandler.INSTANCE);
        registerReloadRequiredWriteAttributeHandler(rr, AGENT_NAME_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, DISABLE_NATIVE_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, SERVER_TRANSPORT_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, SERVER_BIND_PORT_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, SERVER_BIND_ADDRESS_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, SERVER_TRANSPORT_PARAMS_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, SERVER_ALIAS_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, SOCKET_BINDING_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, AGENT_TRANSPORT_ATTRIBDEF);
        registerReloadRequiredWriteAttributeHandler(rr, AGENT_TRANSPORT_PARAMS_ATTRIBDEF);
    }

    private void registerReloadRequiredWriteAttributeHandler(ManagementResourceRegistration rr, AttributeDefinition def) {
        rr.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
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
