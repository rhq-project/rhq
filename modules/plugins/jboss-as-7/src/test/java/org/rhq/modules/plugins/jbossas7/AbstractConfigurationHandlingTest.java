/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.testng.annotations.BeforeSuite;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.modules.plugins.jbossas7.json.Operation;

/**
 * Base class for configuration handling tests
 * @author Heiko W. Rupp
 */
public class AbstractConfigurationHandlingTest {

    private static final String DESCRIPTOR_FILENAME = "test-plugin.xml";
    private Log log = LogFactory.getLog(getClass());
    private PluginDescriptor pluginDescriptor;


    void loadPluginDescriptor() throws Exception {
        try {
            URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME);
            log.info("Loading plugin descriptor at: " + descriptorUrl);

            JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);
            pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    protected ConfigurationDefinition loadDescriptor(String serverName) throws InvalidPluginDescriptorException {
        List<ServerDescriptor> servers = pluginDescriptor.getServers();

        ServerDescriptor serverDescriptor = findServer(serverName, servers);
        assert serverDescriptor != null : "Server descriptor not found in test plugin descriptor";

        return ConfigurationMetadataParser.parse("null", serverDescriptor.getResourceConfiguration());
    }

    private ServerDescriptor findServer(String name, List<ServerDescriptor> servers) {
        for (ServerDescriptor server : servers) {
            if (server.getName().equals(name)) {
                return server;
            }
        }

        return null;
    }

    /**
     * Provide a fake connection, that will return the
     * content we provide via #setContent
     *
     */
    protected class FakeConnection extends ASConnection {

        JsonNode content;

        public FakeConnection() {
            super("localhost", 1234);
        }

        public void setContent(JsonNode content) {
            this.content = content;
        }

        @Override
        public JsonNode executeRaw(Operation operation) {
            if (content==null)
                throw new IllegalStateException("Content not yet set");
            return content;
        }
    }
}
