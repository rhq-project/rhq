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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";
    private static final String PLUGIN_SCHEMA_PATH = "rhq-plugin.xsd";

    private final Log log = LogFactory.getLog(this.getClass());

    private final URL pluginJarUrl;
    private final ClassLoader pluginClassLoader;

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
    public PluginDescriptorLoader(URL pluginJarUrl, ClassLoader classLoader, ClassLoader parentClassLoader,
        boolean unpackJars, File tmpDir) throws PluginContainerException {

        this.pluginJarUrl = pluginJarUrl;

        if (classLoader != null) {
            this.pluginClassLoader = classLoader;
        } else {
            this.pluginClassLoader = createPluginClassLoader(pluginJarUrl, parentClassLoader, unpackJars, tmpDir);
        }
    }

    private ClassLoader createPluginClassLoader(URL jarUrl, ClassLoader parentClassLoader, boolean unpackJars,
        File tmpDir) throws PluginContainerException {

        ClassLoader classLoader;
        if (parentClassLoader == null) {
            parentClassLoader = this.getClass().getClassLoader();
        }

        if (jarUrl != null) {
            classLoader = PluginClassLoader.create(new File(jarUrl.getPath()).getName(), jarUrl, unpackJars,
                parentClassLoader, tmpDir);
            log.debug("Created classloader for plugin [" + jarUrl + "]");
        } else {
            // this is mainly to support tests
            classLoader = parentClassLoader;
        }

        return classLoader;
    }

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
        return loadPluginDescriptorFromUrl(this.pluginJarUrl);
    }

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

        final Log logger = LogFactory.getLog(PluginDescriptorLoader.class);

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

        JarFile jarFile = null;

        try {
            jarFile = new JarFile(new File(pluginJarFileUrl.toURI()));
            JarEntry descriptorEntry = jarFile.getJarEntry(PLUGIN_DESCRIPTOR_PATH);
            InputStream is = jarFile.getInputStream(descriptorEntry);
            if (is == null) {
                throw new PluginContainerException("Could not load plugin descriptor [" + PLUGIN_DESCRIPTOR_PATH
                    + "] from plugin jar at [" + pluginJarFileUrl + "].");
            }

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation. (see http://jira.jboss.com/jira/browse/JBNADM-1539)
            URL pluginSchemaURL = PluginDescriptorLoader.class.getClassLoader().getResource(PLUGIN_SCHEMA_PATH);
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                pluginSchemaURL);
            unmarshaller.setSchema(pluginSchema);

            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);

            PluginDescriptor pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(is);

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
            if (jarFile != null) {
                try {
                    jarFile.close(); // closes the input stream we created for us
                } catch (Exception e) {
                    logger.warn("Cannot close jar file [" + pluginJarFileUrl + "]. Cause: " + e);
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
        return this.getClass().getSimpleName() + "[pluginJarUrl=" + this.pluginJarUrl + ", pluginClassLoader="
            + this.pluginClassLoader + "]";
    }

    private static void testPluginJarIsReadable(URL pluginJarFileUrl) throws PluginContainerException {
        InputStream inputStream = null;
        try {
            inputStream = pluginJarFileUrl.openStream();
        } catch (IOException e) {
            throw new PluginContainerException("Unable to open plugin jar at [" + pluginJarFileUrl + "] for reading.");
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException ignore) {
            }
        }
    }
}
