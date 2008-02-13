/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc.plugin;

import java.io.File;
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
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Manages the context of a loaded plugin. This includes a classloader created specifically for the plugin and access to
 * its XML descriptor file.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class PluginEnvironment {
    private static final Log log = LogFactory.getLog(PluginEnvironment.class);

    private static final String DEFAULT_PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";

    // make these protected mainly to allow unit tests to extend this class to customize these
    protected URL pluginUrl;
    protected String pluginName;
    protected ClassLoader pluginClassLoader;
    protected PluginDescriptor pluginDescriptor;
    protected String pluginDescriptorPath;

    /**
     * Creates a new plugin environment. If <code>classLoader</code> is specified, it will be used as the environment's
     * classloader and <code>parentClassLoader</code> will be ignored. If <code>classLoader</code> is <code>null</code>,
     * then a {@link PluginClassLoader} will be created with the given parent classloader and it will unpack all
     * embedded jars found in the given plugin URL's <code>/lib</code> directory.
     *
     * @param  pluginUrl         where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpdir            a directory where the jars are placed when unpacked
     *
     * @throws PluginContainerException
     */
    public PluginEnvironment(URL pluginUrl, ClassLoader classLoader, ClassLoader parentClassLoader, File tmpdir)
        throws PluginContainerException {
        initialize(pluginUrl, DEFAULT_PLUGIN_DESCRIPTOR_PATH, classLoader, parentClassLoader, true, tmpdir);
    }

    /**
     * This creates a {@link PluginEnvironment} that will allow you to specify the plugin's new class loader to not
     * unpack its embedded jars, if they exist. You normally only pass in <code>false</code> for <code>unpackJars</code>
     * if you only want to process/parse the plugin descriptor but do not plan on using this environment instance for
     * actual runtime of the plugin (because it won't work since you haven't unpacked the jars).
     *
     * <p>If you pass in a non-<code>null</code> <code>classLoader</code>, then both <code>parentClassLoader</code> and
     * <code>unpackJars</code> is ignored since you are saying you already know how to find the classes for this
     * environment.</p>
     *
     * @param  pluginUrl         where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  unpackJars        if <code>true</code> and plugin jar has embedded jars, they will be unpacked - this is
     *                           ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpdir            a directory where the jars are placed if they are to be unpacked; only meaningful when
     *                           <code>unpackJars</code> is <code>true</code>
     *
     * @throws PluginContainerException
     */
    public PluginEnvironment(URL pluginUrl, ClassLoader classLoader, ClassLoader parentClassLoader, boolean unpackJars,
        File tmpdir) throws PluginContainerException {
        initialize(pluginUrl, DEFAULT_PLUGIN_DESCRIPTOR_PATH, classLoader, parentClassLoader, unpackJars, tmpdir);
    }

    public void destroy() {
        // if its our own plugin classloader, let's tell it to clean up its directory where the jars were unpacked
        if (pluginClassLoader instanceof PluginClassLoader) {
            ((PluginClassLoader) pluginClassLoader).destroy();
        }
    }

    /**
     * This will create the plugin's {@link #getClassLoader() classloader}, then read and parse the
     * {@link #getDescriptor() XML plugin descriptor} to make sure its valid.
     *
     * @param  pluginJarUrl      where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  descriptorPath    the path within the plugin classloader where the XML descriptor is found
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  unpackJars        if <code>true</code> and plugin jar has embedded jars, they will be unpacked - this is
     *                           ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpDirectory      a directory where the jars are placed if they are to be unpacked; only meaningful when
     *                           <code>unpackJars</code> is <code>true</code>
     *
     * @throws PluginContainerException
     */
    protected void initialize(URL pluginJarUrl, String descriptorPath, ClassLoader classLoader,
        ClassLoader parentClassLoader, boolean unpackJars, File tmpDirectory) throws PluginContainerException {
        if (classLoader == null) {
            if (parentClassLoader == null) {
                parentClassLoader = this.getClass().getClassLoader();
            }

            if (pluginJarUrl != null) {
                classLoader = PluginClassLoader.create(new File(pluginJarUrl.getPath()).getName(), pluginJarUrl,
                    unpackJars, parentClassLoader, tmpDirectory);
            } else {
                // this is mainly to support tests
                classLoader = parentClassLoader;
            }
        }

        initializeDescriptor(classLoader, descriptorPath, pluginJarUrl);

        this.pluginUrl = pluginJarUrl;
        this.pluginDescriptorPath = descriptorPath;
        this.pluginClassLoader = classLoader;
        this.pluginName = this.pluginDescriptor.getName();
    }

    private void initializeDescriptor(ClassLoader classloader, String descriptorPath, URL pluginJarUrl)
        throws PluginContainerException {
        // purpose of this method is to set the this.pluginDescriptor field and verify the XML is valid

        if (pluginJarUrl == null) {
            throw new PluginContainerException("A valid plugin JAR URL must be supplied");
        }

        JAXBContext jaxbContext;

        try {
            jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        } catch (JAXBException e) {
            throw new PluginContainerException("Could not instantiate the JAXB Context",
                new WrappedRemotingException(e));
        }

        InputStream is = null;
        try {
            // A classloader with just the primary plugin in it... no dependencies or libraries
            ClassLoader pluginOnlyClassloader = new URLClassLoader(new URL[] { pluginJarUrl });

            is = pluginOnlyClassloader.getResourceAsStream(descriptorPath);
            if (is == null) {
                log.warn("Could not load plugin descriptor [" + descriptorPath + "] from URL: " + pluginJarUrl);
            }

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation. (see http://jira.jboss.com/jira/browse/JBNADM-1539)
            URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-plugin.xsd");
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                pluginSchemaURL);
            unmarshaller.setSchema(pluginSchema);

            ValidationEventCollector vec = new ValidationEventCollector();
            unmarshaller.setEventHandler(vec);

            this.pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(is);

            for (ValidationEvent event : vec.getEvents()) {
                log.debug("Plugin [" + this.pluginDescriptor.getName() + "] descriptor messages {Severity: "
                    + event.getSeverity() + ", Message: " + event.getMessage() + ", Exception: "
                    + event.getLinkedException() + "}");
            }
        } catch (Exception e) {
            throw new PluginContainerException("Could not successfully parse the plugin descriptor [" + descriptorPath
                + " found in URL [" + pluginJarUrl + "]", new WrappedRemotingException(e));
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    // Nothing more we can do here
                }
            }
        }

        return;
    }

    public String getPluginName() {
        return pluginName;
    }

    public URL getPluginUrl() {
        return pluginUrl;
    }

    public ClassLoader getClassLoader() {
        return pluginClassLoader;
    }

    public PluginDescriptor getDescriptor() {
        return pluginDescriptor;
    }

    public String getPluginDescriptorPath() {
        return pluginDescriptorPath;
    }

    @Override
    public String toString() {
        return pluginName + ": url=[" + pluginUrl + "], descriptor-path=[" + pluginDescriptorPath + "]";
    }
}