/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.clientapi.descriptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Utilities for agent plugin descriptors.
 *
 * @author John Mazzitelli
 */
public abstract class AgentPluginDescriptorUtil {

    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";
    private static final String PLUGIN_SCHEMA_PATH = "rhq-plugin.xsd";

    /**
     * Loads a plugin descriptor from the given plugin jar and returns it.
     * 
     * This is a static method to provide a convienence method for others to be able to use.
     *  
     * @param pluginJarFileUrl URL to a plugin jar file
     * @return the plugin descriptor found in the given plugin jar file
     * @throws PluginContainerException if failed to find or parse a descriptor file in the plugin jar
     */
    public static PluginDescriptor loadPluginDescriptorFromUrl(URL pluginJarFileUrl) throws PluginContainerException {

        final Log logger = LogFactory.getLog(AgentPluginDescriptorUtil.class);

        if (pluginJarFileUrl == null) {
            throw new PluginContainerException("A valid plugin JAR URL must be supplied.");
        }
        logger.debug("Loading plugin descriptor from plugin jar at [" + pluginJarFileUrl + "]...");

        testPluginJarIsReadable(pluginJarFileUrl);

        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        } catch (Exception e) {
            throw new PluginContainerException("Failed to create JAXB Context.", new WrappedRemotingException(e));
        }

        JarInputStream jis = null;
        JarEntry descriptorEntry = null;

        try {
            jis = new JarInputStream(pluginJarFileUrl.openStream());
            JarEntry nextEntry = jis.getNextJarEntry();
            while (nextEntry != null && descriptorEntry == null) {
                if (PLUGIN_DESCRIPTOR_PATH.equals(nextEntry.getName())) {
                    descriptorEntry = nextEntry;
                } else {
                    jis.closeEntry();
                    nextEntry = jis.getNextJarEntry();
                }
            }

            if (descriptorEntry == null) {
                throw new Exception("The plugin descriptor does not exist");
            }

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation
            URL pluginSchemaURL = AgentPluginDescriptorUtil.class.getClassLoader().getResource(PLUGIN_SCHEMA_PATH);
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                pluginSchemaURL);
            unmarshaller.setSchema(pluginSchema);

            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);

            PluginDescriptor pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(jis);

            for (ValidationEvent event : vec.getEvents()) {
                logger.debug("Plugin [" + pluginDescriptor.getName() + "] descriptor messages {Severity: "
                    + event.getSeverity() + ", Message: " + event.getMessage() + ", Exception: "
                    + event.getLinkedException() + "}");
            }

            return pluginDescriptor;
        } catch (Exception e) {
            throw new PluginContainerException("Could not successfully parse the plugin descriptor ["
                + PLUGIN_DESCRIPTOR_PATH + " found in plugin jar at [" + pluginJarFileUrl + "]",
                new WrappedRemotingException(e));
        } finally {
            if (jis != null) {
                try {
                    jis.close();
                } catch (Exception e) {
                    logger.warn("Cannot close jar stream [" + pluginJarFileUrl + "]. Cause: " + e);
                }
            }
        }
    }

    private static void testPluginJarIsReadable(URL pluginJarFileUrl) throws PluginContainerException {
        InputStream inputStream = null;
        try {
            inputStream = pluginJarFileUrl.openStream();
        } catch (IOException e) {
            throw new PluginContainerException("Unable to open plugin jar at [" + pluginJarFileUrl + "] for reading.");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
}
