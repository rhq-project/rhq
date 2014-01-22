/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.itest.nonpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.ASConnectionParams;
import org.rhq.modules.plugins.jbossas7.ASConnectionParamsBuilder;
import org.rhq.modules.plugins.jbossas7.ASUploadConnection;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Abstract base class for integration tests that do not run against an RHQ plugin container.
 *
 * @author Heiko W. Rupp
 */
@Test(groups = { "integration", "nonpc" }, dependsOnGroups = "discovery")
public abstract class AbstractIntegrationTest {

    protected static final String PLUGIN_NAME = "JBossAS7";

    protected static final String DC_HOST = System.getProperty("jboss.domain.bindAddress");
    protected static final int DC_HTTP_PORT = Integer.valueOf(System.getProperty("jboss.domain.httpManagementPort"));
    protected static final String DC_USER = AbstractJBossAS7PluginTest.MANAGEMENT_USERNAME;
    protected static final String DC_PASS = AbstractJBossAS7PluginTest.MANAGEMENT_PASSWORD;
    protected static final String MAVEN_REPO_LOCAL = System.getProperty("settings.localRepository");

    String uploadToAs(String deploymentPath) throws IOException {
        String fileName = new File(deploymentPath).getName();
        ASConnectionParams asConnectionParams = new ASConnectionParamsBuilder() //
            .setHost(DC_HOST) //
            .setPort(DC_HTTP_PORT) //
            .setUsername(DC_USER) //
            .setPassword(DC_PASS) //
            .createASConnectionParams();
        ASUploadConnection conn = new ASUploadConnection(asConnectionParams, fileName);
        OutputStream os = conn.getOutputStream();

        //        URL url = getClass().getClassLoader().getResource(".");
        //        System.out.println(url);

        InputStream fis = getClass().getClassLoader().getResourceAsStream(deploymentPath);
        if (fis == null) {
            File inputFile = new File(deploymentPath);
            if (!inputFile.canRead()) {
                throw new FileNotFoundException("Input stream for path [" + deploymentPath
                    + "] could not be opened - does the file exist either in the test classpath or on the filesystem?");
            }
            fis = new FileInputStream(inputFile);
        }

        StreamUtil.copy(fis, os, false);
        try {
            fis.close();
        } catch (IOException e) {
            // ignore
        }

        JsonNode node = conn.finishUpload();
        //        System.out.println(node);
        assert node != null : "No result from upload - node was null";
        assert node.has("outcome") : "No outcome from upload";
        String outcome = node.get("outcome").getTextValue();
        assert outcome.equals("success") : "Upload was no success, but: [" + outcome + "]";

        JsonNode resultNode = node.get("result");
        return resultNode.get("BYTES_VALUE").getTextValue();
    }

    ASConnection getASConnection() {
        ASConnectionParams asConnectionParams = new ASConnectionParamsBuilder() //
            .setHost(DC_HOST) //
            .setPort(DC_HTTP_PORT) //
            .setUsername(DC_USER) //
            .setPassword(DC_PASS) //
            .createASConnectionParams();
        return new ASConnection(asConnectionParams);
    }

    Operation addDeployment(String deploymentName, String bytes_value) {
        Address deploymentsAddress = new Address("deployment", deploymentName);
        Operation op = new Operation("add", deploymentsAddress);
        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", bytes_value));
        content.add(contentValues);
        op.addAdditionalProperty("content", content);
        op.addAdditionalProperty("name", deploymentName); // this needs to be unique per upload
        op.addAdditionalProperty("runtime-name", deploymentName);

        return op;
    }

    private static final String DESCRIPTOR_FILENAME = "rhq-plugin.xml";
    private Log log = LogFactory.getLog(getClass());
    private PluginDescriptor pluginDescriptor;

    void loadPluginDescriptor() throws Exception {
        try {
            //ClassLoader classLoader = this.getClass().getClassLoader();
            //URL descriptorUrl = classLoader.getResource("META-INF" + File.separator + DESCRIPTOR_FILENAME);
            File descriptorFile = new File("src/main/resources/META-INF/" + DESCRIPTOR_FILENAME);
            URL descriptorUrl = descriptorFile.toURI().toURL();
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

    protected ConfigurationDefinition loadServerDescriptor(String serverName) throws InvalidPluginDescriptorException {
        List<ServerDescriptor> servers = pluginDescriptor.getServers();

        ServerDescriptor serverDescriptor = findServer(serverName, servers);
        assert serverDescriptor != null : "Server descriptor for server [" + serverName
            + "] not found in test plugin descriptor";

        return ConfigurationMetadataParser.parse("null", serverDescriptor.getResourceConfiguration());
    }

    protected ConfigurationDefinition loadServiceDescriptor(String serviceName) throws InvalidPluginDescriptorException {
        List<ServiceDescriptor> services = pluginDescriptor.getServices();

        ServiceDescriptor serviceDescriptor = findService(serviceName, services);
        assert serviceDescriptor != null : "Service descriptor for service [" + serviceName
            + "] not found in plugin descriptor";

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

    private ServiceDescriptor findService(String name, List<ServiceDescriptor> services) {
        for (ServiceDescriptor service : services) {
            if (service.getName().equals(name)) {
                return service;
            }
        }

        return null;
    }

}
