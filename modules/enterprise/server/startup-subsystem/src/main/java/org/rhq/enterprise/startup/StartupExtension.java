/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.startup;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

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

/**
 * A JBoss AS7 subsystem extension that starts up the RHQ deployment applications.
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */

public class StartupExtension implements Extension {

    // The namespace used for the subsystem XML element
    public static final String NAMESPACE = "urn:org.rhq:startup:1.0";
    // The name of our subsystem within the model
    public static final String SUBSYSTEM_NAME = "rhq-startup";
    // The path of our subsystem within the model
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    // the deployments we are managing
    public static final String DEPLOYMENT_APP_EAR = "rhq.ear";

    // our management API version
    private static final int API_MAJOR_VERSION = 1;
    private static final int API_MINOR_VERSION = 0;

    // location in the classloader of the description messages
    private static final String RESOURCE_NAME = StartupExtension.class.getPackage().getName() + ".LocalDescriptions";

    // used to read and write the XML of our subsystem config
    private static final StartupSubsystemParser parser = new StartupSubsystemParser();

    private static final String RHQ_SERVER_HOME_ENVVAR = "RHQ_SERVER_HOME";
    private static final String RHQ_SERVER_HOME_SYSPROP = "rhq.server.home";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
            StartupExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        // we know our app needs to know where RHQ is installed, so setup our sys prop now
        String serverHome = System.getProperty(RHQ_SERVER_HOME_SYSPROP);
        if (serverHome == null) {
            serverHome = System.getenv(RHQ_SERVER_HOME_ENVVAR);
            if (serverHome == null) {
                // assume we are running our own embedded AS, so RHQ home is the parent dir of the AS home
                try {
                    serverHome = new File(System.getProperty("jboss.home.dir"), "..").getCanonicalPath();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to locate server home", e);
                }
            }
            System.setProperty(RHQ_SERVER_HOME_SYSPROP, serverHome);
        }

        File modulesDir = new File(serverHome, "modules"); // if this is a correct home dir, RHQ's modules directory should be here
        if (!modulesDir.isDirectory()) {
            throw new IllegalStateException("Invalid RHQ server home dir: " + serverHome);
        }

        // register subsystem with its model version
        final SubsystemRegistration subsystem;
        subsystem = context.registerSubsystem(SUBSYSTEM_NAME, API_MAJOR_VERSION, API_MINOR_VERSION);

        // register subsystem model with subsystem definition that defines all attributes and operations
        final ManagementResourceRegistration registration;
        registration = subsystem.registerSubsystemModel(StartupSubsystemDefinition.INSTANCE);

        // register describe operation
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE,
            GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // register the object that persists our subsystem XML config
        subsystem.registerXMLElementWriter(parser);
    }

    /**
     * The subsystem reader/writer, which uses STAX to read and write the subsystem XML.
     */
    private static class StartupSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // Parse the 'subsystem' element...
            ParseUtils.requireNoAttributes(reader);

            // (I think we are required to add this)
            list.add(createAddSubsystemOperation());

            // // Read the child elements
            // while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            //     if (reader.getLocalName().equals("some-xml-element-name")) {
            //         status = reader.getElementText();
            //     } else {
            //         throw ParseUtils.unexpectedElement(reader);
            //     }
            // }
            ParseUtils.requireNoContent(reader);

            return;
        }

        private static ModelNode createAddSubsystemOperation() {
            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
            return subsystem;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            //TODO seems to be a problem with empty elements
            //context.startSubsystemElement(StartupExtension.NAMESPACE, true); // <subsystem/>
            context.startSubsystemElement(StartupExtension.NAMESPACE, false); // <subsystem>

            // writer.writeStartElement("some-xml-element-name"); // <xml-element-name>
            // writer.writeCharacters(something);
            // writer.writeEndElement(); // </xml-element-name>

            writer.writeEndElement(); // </subsystem>
        }
    }

}
