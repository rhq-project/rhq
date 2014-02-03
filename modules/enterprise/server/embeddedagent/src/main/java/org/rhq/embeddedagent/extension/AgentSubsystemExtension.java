package org.rhq.embeddedagent.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import org.rhq.enterprise.agent.AgentConfigurationConstants;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;

public class AgentSubsystemExtension implements Extension {

    private final Logger log = Logger.getLogger(AgentSubsystemExtension.class);

    public static final String NAMESPACE = "urn:org.rhq:embeddedagent:1.0";
    public static final String SUBSYSTEM_NAME = "embeddedagent";

    private final SubsystemParser parser = new SubsystemParser();

    private static final String RESOURCE_NAME = AgentSubsystemExtension.class.getPackage().getName()
        + ".LocalDescriptions";

    protected static final String PLUGINS_ELEMENT = "plugins";
    protected static final String PLUGIN_ELEMENT = "plugin";
    protected static final String PLUGIN_NAME = "name";
    protected static final String PLUGIN_ENABLED = "enabled";
    protected static final String AGENT_ENABLED = "enabled";
    protected static final boolean AGENT_ENABLED_DEFAULT = false;
    protected static final boolean PLUGIN_ENABLED_DEFAULT = true;
    protected static final String AGENT_CONFIG_ELEMENT = "extra-configuration";
    protected static final String PREFERENCE_ELEMENT = "preference";
    protected static final String SERVER_ENDPOINT_ELEMENT = "server-endpoint";
    protected static final String SERVER_ENDPOINT_ADDRESS_XML = "address";
    protected static final String SERVER_ENDPOINT_PORT_XML = "port";
    protected static final String SERVER_ENDPOINT_TRANSPORT_XML = "transport";
    protected static final String SERVER_ENDPOINT_TRANSPORT_PARAMS_XML = "transport-params";
    protected static final String AGENT_ENDPOINT_ELEMENT = "agent-endpoint";
    protected static final String AGENT_ENDPOINT_SOCKET_BINDING_XML = "socket-binding";
    protected static final String AGENT_ENDPOINT_TRANSPORT_XML = "transport";
    protected static final String AGENT_ENDPOINT_TRANSPORT_PARAMS_XML = "transport-params";

    protected static final String AGENT_RESTART_OP = "restart";
    protected static final String AGENT_STOP_OP = "stop";
    protected static final String AGENT_STATUS_OP = "status";

    protected static final String ATTRIB_AGENT_NAME = AgentConfigurationConstants.NAME;
    protected static final String ATTRIB_DISABLE_NATIVE = AgentConfigurationConstants.DISABLE_NATIVE_SYSTEM;
    protected static final String ATTRIB_SERVER_TRANSPORT = AgentConfigurationConstants.SERVER_TRANSPORT;
    protected static final String ATTRIB_SERVER_BIND_PORT = AgentConfigurationConstants.SERVER_BIND_PORT;
    protected static final String ATTRIB_SERVER_BIND_ADDRESS = AgentConfigurationConstants.SERVER_BIND_ADDRESS;
    protected static final String ATTRIB_SERVER_TRANSPORT_PARAMS = AgentConfigurationConstants.SERVER_TRANSPORT_PARAMS;
    protected static final String ATTRIB_SERVER_ALIAS = AgentConfigurationConstants.SERVER_ALIAS;
    protected static final String ATTRIB_SOCKET_BINDING = AGENT_ENDPOINT_SOCKET_BINDING_XML;
    protected static final String ATTRIB_AGENT_TRANSPORT = ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT;
    protected static final String ATTRIB_AGENT_TRANSPORT_PARAMS = ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT_PARAMS;

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME,
            AgentSubsystemExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        log.info("Initializing embedded agent subsystem");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration = subsystem
            .registerSubsystemModel(AgentSubsystemDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(parser);
    }

    /**
     * The subsystem parser, which uses stax to read and write to and from xml
     */
    private static class SubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // The agent "enabled" attribute is required
            ParseUtils.requireSingleAttribute(reader, AGENT_ENABLED);

            // Add the main subsystem 'add' operation
            final ModelNode opAdd = new ModelNode();
            opAdd.get(OP).set(ADD);
            opAdd.get(OP_ADDR).set(PathAddress.pathAddress(SUBSYSTEM_PATH).toModelNode());
            String agentEnabledValue = reader.getAttributeValue(null, AGENT_ENABLED);
            if (agentEnabledValue != null) {
                opAdd.get(AGENT_ENABLED).set(agentEnabledValue);
            }

            // Read the children elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                String elementName = reader.getLocalName();
                if (elementName.equals(PLUGINS_ELEMENT)) {
                    ModelNode pluginsAttributeNode = opAdd.get(PLUGINS_ELEMENT);
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        if (reader.isStartElement()) {
                            readPlugin(reader, pluginsAttributeNode);
                        }
                    }
                } else if (elementName.equals(AGENT_CONFIG_ELEMENT)) {
                    ModelNode configAttributeNode = opAdd.get(AGENT_CONFIG_ELEMENT);
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        if (reader.isStartElement()) {
                            readPreference(reader, configAttributeNode);
                        }
                    }
                } else if (elementName.equals(SERVER_ENDPOINT_ELEMENT)) {
                    String val = reader.getAttributeValue(null, SERVER_ENDPOINT_ADDRESS_XML);
                    if (val != null) {
                        opAdd.get(ATTRIB_SERVER_BIND_ADDRESS).set(val);
                    }
                    val = reader.getAttributeValue(null, SERVER_ENDPOINT_PORT_XML);
                    if (val != null) {
                        opAdd.get(ATTRIB_SERVER_BIND_PORT).set(val);
                    }
                    val = reader.getAttributeValue(null, SERVER_ENDPOINT_TRANSPORT_XML);
                    if (val != null) {
                        opAdd.get(ATTRIB_SERVER_TRANSPORT).set(val);
                    }
                    val = reader.getAttributeValue(null, SERVER_ENDPOINT_TRANSPORT_PARAMS_XML);
                    if (val != null) {
                        opAdd.get(ATTRIB_SERVER_TRANSPORT_PARAMS).set(val);
                    }
                    ParseUtils.requireNoContent(reader);
                } else if (elementName.equals(AGENT_ENDPOINT_ELEMENT)) {
                    String val = reader.getAttributeValue(null, AGENT_ENDPOINT_SOCKET_BINDING_XML);
                    if (val != null) {
                        opAdd.get(ATTRIB_SOCKET_BINDING).set(val);
                    }
                    val = reader.getAttributeValue(null, AGENT_ENDPOINT_TRANSPORT_XML);
                    if (val != null) {
                        opAdd.get(ATTRIB_AGENT_TRANSPORT).set(val);
                    }
                    val = reader.getAttributeValue(null, AGENT_ENDPOINT_TRANSPORT_PARAMS_XML);
                    if (val != null) {
                        opAdd.get(ATTRIB_AGENT_TRANSPORT_PARAMS).set(val);
                    }
                    ParseUtils.requireNoContent(reader);
                } else if (elementName.equals(ATTRIB_AGENT_NAME)) {
                    opAdd.get(ATTRIB_AGENT_NAME).set(reader.getElementText());
                } else if (elementName.equals(ATTRIB_DISABLE_NATIVE)) {
                    opAdd.get(ATTRIB_DISABLE_NATIVE).set(reader.getElementText());
                } else if (elementName.equals(ATTRIB_SERVER_ALIAS)) {
                    opAdd.get(ATTRIB_SERVER_ALIAS).set(reader.getElementText());
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

            list.add(opAdd);
        }

        private void readPlugin(XMLExtendedStreamReader reader, ModelNode pluginsAttributeNode)
            throws XMLStreamException {

            if (!reader.getLocalName().equals(PLUGIN_ELEMENT)) {
                throw ParseUtils.unexpectedElement(reader);
            }

            String pluginName = null;
            boolean pluginEnabled = PLUGIN_ENABLED_DEFAULT;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attr = reader.getAttributeLocalName(i);
                String value = reader.getAttributeValue(i);
                if (attr.equals(PLUGIN_ENABLED)) {
                    pluginEnabled = Boolean.parseBoolean(value);
                } else if (attr.equals(PLUGIN_NAME)) {
                    pluginName = value;
                } else {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
            ParseUtils.requireNoContent(reader);
            if (pluginName == null) {
                throw ParseUtils.missingRequiredElement(reader, Collections.singleton(PLUGIN_NAME));
            }

            // Add the plugin to the plugins attribute node
            pluginsAttributeNode.add(pluginName, pluginEnabled);
        }

        private void readPreference(XMLExtendedStreamReader reader, ModelNode configAttributeNode)
            throws XMLStreamException {

            if (!reader.getLocalName().equals(PREFERENCE_ELEMENT)) {
                throw ParseUtils.unexpectedElement(reader);
            }

            ParseUtils.requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.VALUE.getLocalName());
            String attr = reader.getAttributeValue(null, Attribute.NAME.getLocalName());
            String val = reader.getAttributeValue(null, Attribute.VALUE.getLocalName());
            ParseUtils.requireNoContent(reader);

            configAttributeNode.add(attr, val);
        }

        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
            throws XMLStreamException {
            ModelNode node = context.getModelNode();

            // <subsystem>
            context.startSubsystemElement(AgentSubsystemExtension.NAMESPACE, false);
            writer.writeAttribute(AGENT_ENABLED,
                String.valueOf(node.get(AGENT_ENABLED).asBoolean(AGENT_ENABLED_DEFAULT)));

            // our config elements
            writeElement(writer, node, ATTRIB_AGENT_NAME);
            writeElement(writer, node, ATTRIB_DISABLE_NATIVE);
            writeElement(writer, node, ATTRIB_SERVER_ALIAS);

            // <server-endpoint>
            writer.writeStartElement(SERVER_ENDPOINT_ELEMENT);
            ModelNode serverAddressNode = node.get(ATTRIB_SERVER_BIND_ADDRESS);
            ModelNode serverPortNode = node.get(ATTRIB_SERVER_BIND_PORT);
            ModelNode serverTransportNode = node.get(ATTRIB_SERVER_TRANSPORT);
            ModelNode serverTransportParamsNode = node.get(ATTRIB_SERVER_TRANSPORT_PARAMS);

            if (serverPortNode.isDefined()) {
                writer.writeAttribute(SERVER_ENDPOINT_PORT_XML, serverPortNode.asString());
            }
            if (serverAddressNode.isDefined()) {
                writer.writeAttribute(SERVER_ENDPOINT_ADDRESS_XML, serverAddressNode.asString());
            }
            if (serverTransportNode.isDefined()) {
                writer.writeAttribute(SERVER_ENDPOINT_TRANSPORT_XML, serverTransportNode.asString());
            }
            if (serverTransportParamsNode.isDefined()) {
                writer.writeAttribute(SERVER_ENDPOINT_TRANSPORT_PARAMS_XML, serverTransportParamsNode.asString());
            }
            // </server-endpoint>
            writer.writeEndElement();

            // <agent-endpoint>
            writer.writeStartElement(AGENT_ENDPOINT_ELEMENT);
            ModelNode agentSocketBindingNode = node.get(ATTRIB_SOCKET_BINDING);
            ModelNode agentTransportNode = node.get(ATTRIB_AGENT_TRANSPORT);
            ModelNode agentTransportParamsNode = node.get(ATTRIB_AGENT_TRANSPORT_PARAMS);

            if (agentSocketBindingNode.isDefined()) {
                writer.writeAttribute(AGENT_ENDPOINT_SOCKET_BINDING_XML, agentSocketBindingNode.asString());
            }
            if (agentTransportNode.isDefined()) {
                writer.writeAttribute(AGENT_ENDPOINT_TRANSPORT_XML, agentTransportNode.asString());
            }
            if (agentTransportParamsNode.isDefined()) {
                writer.writeAttribute(AGENT_ENDPOINT_TRANSPORT_PARAMS_XML, agentTransportParamsNode.asString());
            }
            // </agent-endpoint>
            writer.writeEndElement();

            // <extra-configuration>
            writer.writeStartElement(AGENT_CONFIG_ELEMENT);
            ModelNode configNode = node.get(AGENT_CONFIG_ELEMENT);
            if (configNode != null && configNode.isDefined()) {
                for (Property property : configNode.asPropertyList()) {
                    // <preference>
                    writer.writeStartElement(PREFERENCE_ELEMENT);
                    writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), property.getValue().asString());
                    // </preference>
                    writer.writeEndElement();
                }
            }
            // </extra-configuration>
            writer.writeEndElement();

            // <plugins>
            writer.writeStartElement(PLUGINS_ELEMENT);
            ModelNode plugins = node.get(PLUGINS_ELEMENT);
            if (plugins != null && plugins.isDefined()) {
                for (Property property : plugins.asPropertyList()) {
                    // <plugin>
                    writer.writeStartElement(PLUGIN_ELEMENT);
                    writer.writeAttribute(PLUGIN_NAME, property.getName());
                    writer.writeAttribute(PLUGIN_ENABLED, property.getValue().asString());
                    // </plugin>
                    writer.writeEndElement();
                }
            }
            // </plugins>
            writer.writeEndElement();
            // </subsystem>
            writer.writeEndElement();
        }

        private void writeElement(final XMLExtendedStreamWriter writer, ModelNode node, String attribName)
            throws XMLStreamException {
            ModelNode attribNode = node.get(attribName);
            if (attribNode.isDefined()) {
                writer.writeStartElement(attribName);
                writer.writeCharacters(attribNode.asString());
                writer.writeEndElement();
            }
        }
    }
}
