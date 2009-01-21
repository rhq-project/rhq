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
package org.rhq.core.pc.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * This class handles parsing and validating an RHQ plugin's descriptor.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class PluginDescriptorLoader {
    /**
     * The path to the plugin descriptor file within the plugin jar.
     */
    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";

    private static final String PLUGIN_SCHEMA_PATH = "rhq-plugin.xsd";

    private final Log log = LogFactory.getLog(this.getClass());

    private URL pluginJarUrl;
    private ClassLoader pluginClassLoader;

    /**
     * This will create the plugin's {@link #getPluginClassLoader() classloader}. If <code>classLoader</code> is
     * specified, it will be used as the environment's classloader, and <code>parentClassLoader</code> will be ignored.
     * If <code>classLoader</code> is <code>null</code>, then a {@link PluginClassLoader} will be created with the given
     * parent classloader and it will unpack all embedded jars found in the given plugin URL's <code>/lib</code>
     * directory.The {@link #loadPluginDescriptor()} method can then be called to parse ands validate the XML plugin
     * descriptor.
     *
     * @param  pluginJarUrl      where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  unpackJars        if <code>true</code> and plugin jar has embedded jars, they will be unpacked - this is
     *                           ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpDir            a directory where the jars are placed if they are to be unpacked; only meaningful when
     *                           <code>unpackJars</code> is <code>true</code>
     *
     * @throws PluginContainerException on failure to load the descriptor
     */
    public PluginDescriptorLoader(URL pluginJarUrl, ClassLoader classLoader, ClassLoader parentClassLoader, boolean unpackJars, File tmpDir) throws PluginContainerException {
        this.pluginJarUrl = pluginJarUrl;
        this.pluginClassLoader = (classLoader != null) ? classLoader : createPluginClassLoader(pluginJarUrl, parentClassLoader, unpackJars, tmpDir);
    }

    private ClassLoader createPluginClassLoader(URL pluginJarUrl, ClassLoader parentClassLoader, boolean unpackJars, File tmpDir) throws PluginContainerException {
        ClassLoader classLoader;
        if (parentClassLoader == null) {
            parentClassLoader = this.getClass().getClassLoader();
        }
        if (pluginJarUrl != null) {
            classLoader = PluginClassLoader.create(new File(pluginJarUrl.getPath()).getName(), pluginJarUrl, unpackJars, parentClassLoader, tmpDir);
        } else {
            // this is mainly to support tests
            classLoader = parentClassLoader;
        }
        return classLoader;
    }

    @NotNull
    public ClassLoader getPluginClassLoader() {
        return this.pluginClassLoader;
    }

    /**
     * Loads and validates the plugin descriptor from the plugin jar associated with this loader.
     *
     * @return the JAXB plugin descriptor object
     * @throws PluginContainerException on failure to load the descriptor
     */
    public PluginDescriptor loadPluginDescriptor() throws PluginContainerException {
        if (this.pluginJarUrl == null) {
            throw new PluginContainerException("A valid plugin JAR URL must be supplied.");
        }
        log.debug("Loading plugin descriptor from plugin jar at [" + this.pluginJarUrl + "]...");

        testPluginJarIsReadable();

        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        } catch (JAXBException e) {
            //noinspection ThrowableInstanceNeverThrown
            throw new PluginContainerException("Could not instantiate the JAXB Context.", new WrappedRemotingException(e));
        }

        InputStream is = null;
        try {
            // A classloader with just the primary plugin in it... no dependencies or libraries
            // Note that we use a parent classloader of null in case more than one plugin happens
            // to be in our thread context classloader - we don't want any other plugins getting picked up
            ClassLoader pluginOnlyClassloader = new URLClassLoader(new URL[] { this.pluginJarUrl }, null);

            is = pluginOnlyClassloader.getResourceAsStream(PLUGIN_DESCRIPTOR_PATH);
            if (is == null) {
                throw new PluginContainerException("Could not load plugin descriptor [" + PLUGIN_DESCRIPTOR_PATH + "] from plugin jar at [" + this.pluginJarUrl + "].");
            }

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation. (see http://jira.jboss.com/jira/browse/JBNADM-1539)
            URL pluginSchemaURL = getClass().getClassLoader().getResource(PLUGIN_SCHEMA_PATH);
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);
            unmarshaller.setSchema(pluginSchema);

            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);

            PluginDescriptor pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(is);

            for (ValidationEvent event : vec.getEvents()) {
                log.debug("Plugin [" + pluginDescriptor.getName() + "] descriptor messages {Severity: " + event.getSeverity() + ", Message: " + event.getMessage() + ", Exception: "
                    + event.getLinkedException() + "}");
            }

            return pluginDescriptor;
        } catch (Exception e) {
            throw new PluginContainerException("Could not successfully parse the plugin descriptor [" + PLUGIN_DESCRIPTOR_PATH + " found in plugin jar at [" + this.pluginJarUrl + "]",
                new WrappedRemotingException(e));
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // Nothing more we can do here
                }
            }
        }
    }

    public void destroy() {
        // If it's our own plugin classloader, let's tell it to clean up its directory where the jars were unpacked.
        if (this.pluginClassLoader instanceof PluginClassLoader) {
            ((PluginClassLoader) this.pluginClassLoader).destroy();
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pluginJarUrl=" + this.pluginJarUrl + ", pluginClassLoader=" + this.pluginClassLoader + "]";
    }

    private void testPluginJarIsReadable() throws PluginContainerException {
        InputStream inputStream = null;
        try {
            inputStream = this.pluginJarUrl.openStream();
        } catch (IOException e) {
            throw new PluginContainerException("Unable to open plugin jar at [" + this.pluginJarUrl + "] for reading.");
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                log.error("Failed to close input stream for plugin jar at [" + this.pluginJarUrl + "].");
            }
        }
    }
}
