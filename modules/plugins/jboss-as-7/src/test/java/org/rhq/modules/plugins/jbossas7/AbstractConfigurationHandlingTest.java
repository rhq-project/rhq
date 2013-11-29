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

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.modules.plugins.jbossas7.json.Result.FAILURE;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Base class for configuration handling tests
 * @author Heiko W. Rupp
 */
public abstract class AbstractConfigurationHandlingTest {
    private static final Log LOG = LogFactory.getLog(AbstractConfigurationHandlingTest.class);

    private static final String DESCRIPTOR_FILENAME = "test-plugin.xml";

    private PluginDescriptor pluginDescriptor;

    void loadPluginDescriptor() throws Exception {
        try {
            URL descriptorUrl = this.getClass().getClassLoader().getResource(DESCRIPTOR_FILENAME);
            LOG.info("Loading plugin descriptor at: " + descriptorUrl);

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

    /* Attempts to load a service descriptor with name passed in.
     */
    protected ConfigurationDefinition loadServiceDescriptorElement(String serviceName)
        throws InvalidPluginDescriptorException {

        //locate the services
        List<ServiceDescriptor> services = pluginDescriptor.getServices();

        //locate the specific entry
        ServiceDescriptor serviceDescriptor = findServiceEntry(serviceName, services);

        assert serviceDescriptor != null : "Service descriptor not found in test plugin descriptor";

        //? Validate the returned value?
        return ConfigurationMetadataParser.parse("null", serviceDescriptor.getResourceConfiguration());
    }

    private ServerDescriptor findServer(String name, List<ServerDescriptor> servers) {
        for (ServerDescriptor server : servers) {
            if (server.getName().equals(name)) {
                return server;
            }
        }

        return null;
    }

    /* Search for Service entries by name specified.
     */
    private ServiceDescriptor findServiceEntry(String name, List<ServiceDescriptor> services) {
        for (ServiceDescriptor service : services) {
            if (service.getName().equals(name)) {
                return service;
            }
        }

        return null;
    }

    protected String loadJsonFromFile(String fileName) throws Exception {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Provide a fake connection, that will return the
     * content we provide via #setContent
     *
     */
    protected class FakeConnection extends ASConnection {
        private ObjectMapper mapper = new ObjectMapper();

        @Override
        public Result execute(Operation op) {
            JsonNode json = executeRaw(op);
            Result result;
            try {
                result = mapper.readValue(json, Result.class);
            } catch (Exception e) {
                LOG.warn("Could not read jsonValue", e);
                result = new Result();
                result.setOutcome(FAILURE);
                result.setFailureDescription(e.getMessage());
                result.setRhqThrowable(e);
            }
            return result;
        }

        JsonNode content;

        public FakeConnection() {
            super("localhost", 1234, "fake", "fake");
        }

        public void setContent(JsonNode content) {
            this.content = content;
        }

        public JsonNode executeRaw(Operation operation, int timeoutSec) {
            if (content == null)
                throw new IllegalStateException("Content not yet set");

            Address address = operation.getAddress();
            if (address != null && !address.isEmpty()) {
                // we need to clone the content and then for the result find the right sub-content to put into result and
                // return this one.

                // find the sub-content we want
                String[] parts = address.getPath().split("=");
                String key = parts[0];
                String val = parts[1];
                JsonNode result = content.get("result");
                JsonNode keyNode = result.get(key);
                JsonNode valNode = keyNode.get(val);

                // clone the original content
                ObjectMapper tmpMapper = new ObjectMapper();
                JsonNode tmp = tmpMapper.createObjectNode();
                ((ObjectNode) tmp).putAll(((ObjectNode) content));

                // replace the result with the sub-content
                ((ObjectNode) tmp).put("result", valNode);

                return tmp;
            }
            return content;
        }
    }
}
