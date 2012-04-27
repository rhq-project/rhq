/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.helpers.rtfilter.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * A JBoss AS7 extension that configures the RHQ response-time servlet filter for all deployed webapps.
 *
 * @author Ian Springer
 */
public class RtFilterExtension implements Extension {

    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:rhq:rtfilter:1.0";

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "rhq-rtfilter";

    private final SubsystemReader reader = new SubsystemReader();
    private final SubsystemWriter writer = new SubsystemWriter();

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = RtFilterExtension.class.getPackage().getName() + ".LocalDescriptions";

    public static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();

    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, RtFilterExtension.class.getClassLoader(),
                true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, reader);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration =
                subsystem.registerSubsystemModel(RtFilterSubsystemDefinition.INSTANCE);
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE,
                GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(writer);
    }

    /**
     * The subsystem reader, which uses STAX to read the subsystem XML.
     */
    private static class SubsystemReader implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            INIT_PARAMS.clear();

            // Parse the 'subsystem' element...
            ParseUtils.requireNoAttributes(reader);

            list.add(createAddSubsystemOperation());

            // Read the child elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (reader.getLocalName().equals("init-param")) {
                    readInitParam(reader);
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
                // TODO: Add support for configuring filter mappings.
            }
        }

        private void readInitParam(XMLExtendedStreamReader reader) throws XMLStreamException {
            String id = null;
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if (reader.getAttributeLocalName(i).equals("id")) {
                    if (id != null) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    id = reader.getAttributeValue(i);
                } else {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }

            String description = null;
            String paramName = null;
            String paramValue = null;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                if (reader.isStartElement()) {
                    if (reader.getLocalName().equals("description")) {
                        if (description != null) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        description = reader.getElementText();
                    } else if (reader.getLocalName().equals("param-name")) {
                        if (paramName != null) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        paramName = reader.getElementText();
                    } else if (reader.getLocalName().equals("param-value")) {
                        if (paramValue != null) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        paramValue = reader.getElementText();
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }

            Set<String> missingRequiredElements = new HashSet<String>();
            if (paramName == null) {
                missingRequiredElements.add("param-name");
            }
            if (paramValue == null) {
                missingRequiredElements.add("param-value");
            }
            if (!missingRequiredElements.isEmpty()) {
                throw ParseUtils.missingRequiredElement(reader, missingRequiredElements);
            }

            // There's no need for us to expose the init params via the AS7 management model. Just store them in a
            // static Map that RtFilterDeploymentUnitProcessor will be able to access.
            INIT_PARAMS.put(paramName, paramValue);
        }

        private static ModelNode createAddSubsystemOperation() {
            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
            return subsystem;
        }

    }

    /**
     * The subsystem writer, which uses STAX to write the subsystem XML.
     */
    private static class SubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(RtFilterExtension.NAMESPACE, false);
            writer.writeEndElement();
        }

    }

}
