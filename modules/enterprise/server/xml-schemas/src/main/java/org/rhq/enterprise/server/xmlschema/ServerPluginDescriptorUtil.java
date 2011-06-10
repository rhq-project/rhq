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
package org.rhq.enterprise.server.xmlschema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Utilities for server-side plugin descriptors.
 *
 * @author John Mazzitelli
 */
public abstract class ServerPluginDescriptorUtil {
    private static final Log LOG = LogFactory.getLog(ServerPluginDescriptorUtil.class);

    // the path to the server plugin descriptor found in all server plugins 
    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-serverplugin.xml";

    // the names of all XSD schema files mapped to the package names where their generated classes are  
    private static final Map<String, String> PLUGIN_SCHEMA_PACKAGES;

    // a context path consisting of all the plugin descriptors' package names
    private static final String PLUGIN_CONTEXT_PATH;

    static {
        // maps all xsd files to their generated package names for all known server plugin types;
        // if a new plugin type is ever added, you must ensure you add the new plugin type's xsd/package here
        // See also: http://rhq-project.org/display/RHQ/Design-Server+Side+Plugins#Design-ServerSidePlugins-xmlschemas
        PLUGIN_SCHEMA_PACKAGES = new HashMap<String, String>();
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN, XmlSchemas.PKG_SERVERPLUGIN);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_GENERIC, XmlSchemas.PKG_SERVERPLUGIN_GENERIC);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_CONTENT, XmlSchemas.PKG_SERVERPLUGIN_CONTENT);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_PERSPECTIVE, XmlSchemas.PKG_SERVERPLUGIN_PERSPECTIVE);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_ALERT, XmlSchemas.PKG_SERVERPLUGIN_ALERT);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_ENTITLEMENT, XmlSchemas.PKG_SERVERPLUGIN_ENTITLEMENT);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_BUNDLE, XmlSchemas.PKG_SERVERPLUGIN_BUNDLE);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_PACKAGETYPE, XmlSchemas.PKG_SERVERPLUGIN_PACKAGETYPE);
        PLUGIN_SCHEMA_PACKAGES.put(XmlSchemas.XSD_SERVERPLUGIN_DRIFT, XmlSchemas.PKG_SERVERPLUGIN_DRIFT);

        // so we only have to do this once, build a ':' separated context path containing all schema package names
        StringBuilder packages = new StringBuilder();
        for (String packageName : PLUGIN_SCHEMA_PACKAGES.values()) {
            packages.append(packageName).append(':');
        }
        packages.setLength(packages.length() - 1); // delete the ending ':' so it isn't in our path
        PLUGIN_CONTEXT_PATH = packages.toString();
    }

    /**
     * Determines which of the two plugins is obsolete - in other words, this determines which
     * plugin is older. Each plugin must have the same logical name, but
     * one of which will be determined to be obsolete and should not be deployed.
     * If they have the same MD5, they are identical, so <code>null</code> will be returned.
     * Otherwise, the versions are compared and the one with the oldest version is obsolete.
     * If they have the same versions, the one with the oldest timestamp is obsolete.
     * If they have the same timestamp too, we have no other way to determine obsolescence so plugin1
     * will be picked arbitrarily and a message will be logged when this occurs.
     *
     * @param plugin1
     * @param plugin2
     * @return a reference to the obsolete plugin (plugin1 or plugin2 reference will be returned)
     *         <code>null</code> is returned if they are the same (i.e. they have the same MD5)
     * @throws IllegalArgumentException if the two plugins have different logical names or different types
     */
    public static ServerPlugin determineObsoletePlugin(ServerPlugin plugin1, ServerPlugin plugin2) {
        if (!plugin1.getName().equals(plugin2.getName())) {
            throw new IllegalArgumentException("The two plugins don't have the same name:" + plugin1 + ":" + plugin2);
        }

        if (!plugin1.getType().equals(plugin2.getType())) {
            throw new IllegalArgumentException("The two plugins don't have the same type:" + plugin1 + ":" + plugin2);
        }

        if (plugin1.getMd5().equals(plugin2.getMd5())) {
            return null;
        } else {
            String version1Str = plugin1.getVersion();
            String version2Str = plugin2.getVersion();
            ComparableVersion plugin1Version = new ComparableVersion((version1Str != null) ? version1Str : "0");
            ComparableVersion plugin2Version = new ComparableVersion((version2Str != null) ? version2Str : "0");
            if (plugin1Version.equals(plugin2Version)) {
                if (plugin1.getMtime() == plugin2.getMtime()) {
                    LOG.info("Plugins [" + plugin1 + ", " + plugin2
                        + "] are the same logical plugin but have different content. The plugin [" + plugin1
                        + "] will be considered obsolete.");
                    return plugin1;
                } else if (plugin1.getMtime() < plugin2.getMtime()) {
                    return plugin1;
                } else {
                    return plugin2;
                }
            } else if (plugin1Version.compareTo(plugin2Version) < 0) {
                return plugin1;
            } else {
                return plugin2;
            }
        }
    }

    /**
     * Returns the version for the plugin represented by the given descriptor/file.
     * If the descriptor defines a version, that is considered the version of the plugin.
     * However, if the plugin descriptor does not define a version, the plugin jar's manifest
     * is searched for an implementation version string and if one is found that is the version
     * of the plugin. If the manifest entry is also not found, the plugin does not have a version
     * associated with it, which causes this method to throw an exception.
     * 
     * @param pluginFile the plugin jar
     * @param descriptor the plugin descriptor as found in the plugin jar (if <code>null</code>,
     *                   the plugin file will be read and the descriptor parsed from it)
     * @return the version of the plugin
     * @throws Exception if the plugin is invalid, there is no version for the plugin or the version string is invalid
     */
    public static ComparableVersion getPluginVersion(File pluginFile, ServerPluginDescriptorType descriptor)
        throws Exception {

        if (descriptor == null) {
            descriptor = loadPluginDescriptorFromUrl(pluginFile.toURI().toURL());
            if (descriptor == null) {
                throw new Exception("Plugin is missing a descriptor: " + pluginFile);
            }
        }

        String version = descriptor.getVersion();
        if (version == null) {
            Manifest manifest = getManifest(pluginFile);
            if (manifest != null) {
                version = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
        }

        if (version == null) {
            throw new Exception("No version is defined for plugin jar [" + pluginFile
                + "]. A version must be defined either via the MANIFEST.MF [" + Attributes.Name.IMPLEMENTATION_VERSION
                + "] attribute or via the plugin descriptor 'version' attribute.");
        }

        try {
            return new ComparableVersion(version);
        } catch (RuntimeException e) {
            throw new Exception("Version [" + version + "] for [" + pluginFile + "] did not parse", e);
        }
    }

    /**
     * Loads a plugin descriptor from the given plugin jar and returns it. If the given jar does not
     * have a server plugin descriptor, <code>null</code> will be returned, meaning this is not
     * a server plugin jar.
     *  
     * @param pluginJarFileUrl URL to a plugin jar file
     * @return the plugin descriptor found in the given plugin jar file, or <code>null</code> if there
     *         is no plugin descriptor in the jar file
     * @throws Exception if failed to parse the descriptor file found in the plugin jar
     */
    public static ServerPluginDescriptorType loadPluginDescriptorFromUrl(URL pluginJarFileUrl) throws Exception {

        final Log logger = LogFactory.getLog(ServerPluginDescriptorUtil.class);

        if (pluginJarFileUrl == null) {
            throw new Exception("A valid plugin JAR URL must be supplied.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Loading plugin descriptor from plugin jar at [" + pluginJarFileUrl + "]...");
        }

        testPluginJarIsReadable(pluginJarFileUrl);

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

            ServerPluginDescriptorType pluginDescriptor = null;

            if (descriptorEntry != null) {
                Unmarshaller unmarshaller = null;
                try {
                    unmarshaller = getServerPluginDescriptorUnmarshaller();
                    Object jaxbElement = unmarshaller.unmarshal(jis);
                    pluginDescriptor = ((JAXBElement<? extends ServerPluginDescriptorType>) jaxbElement).getValue();
                } finally {
                    if (unmarshaller != null) {
                        for (ValidationEvent ev : ((ValidationEventCollector) unmarshaller.getEventHandler())
                            .getEvents()) {
                            logger.debug("Plugin [" + pluginJarFileUrl + "] descriptor event {Severity: "
                                + ev.getSeverity() + ", Message: " + ev.getMessage() + ", Exception: "
                                + ev.getLinkedException() + "}");
                        }
                    }
                }
            }

            return pluginDescriptor;

        } catch (Exception e) {
            throw new Exception("Could not successfully parse the plugin descriptor [" + PLUGIN_DESCRIPTOR_PATH
                + "] found in plugin jar at [" + pluginJarFileUrl + "]", e);
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

    /**
     * This will return a JAXB unmarshaller that will enable the caller to parse a server plugin
     * descriptor. The returned unmarshaller will have a {@link ValidationEventCollector}
     * installed as an {@link Unmarshaller#getEventHandler() event handler} which can be used
     * to obtain error messages if the unmarshaller fails to parse an XML document.
     * 
     * @return a JAXB unmarshaller enabling the caller to parse server plugin descriptors
     * 
     * @throws Exception if an unmarshaller could not be created
     */
    public static Unmarshaller getServerPluginDescriptorUnmarshaller() throws Exception {

        // create the JAXB context with all the generated plugin packages in it
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(PLUGIN_CONTEXT_PATH);
        } catch (Exception e) {
            throw new Exception("Failed to create JAXB Context.", e);
        }

        // create the unmarshaller that can be used to parse XML documents containing server plugin descriptors
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        // enable schema validation to ensure the XML documents parsed by the unmarshaller are valid descriptors
        ClassLoader cl = ServerPluginDescriptorUtil.class.getClassLoader();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        StreamSource[] schemaSources = new StreamSource[PLUGIN_SCHEMA_PACKAGES.size()];
        int i = 0;
        for (String schemaPath : PLUGIN_SCHEMA_PACKAGES.keySet()) {
            URL schemaURL = cl.getResource(schemaPath);
            schemaSources[i++] = new StreamSource(schemaURL.toExternalForm());
        }

        Schema pluginSchema = schemaFactory.newSchema(schemaSources);
        unmarshaller.setSchema(pluginSchema);

        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);

        return unmarshaller;
    }

    private static void testPluginJarIsReadable(URL pluginJarFileUrl) throws Exception {
        InputStream inputStream = null;
        try {
            inputStream = pluginJarFileUrl.openStream();
        } catch (IOException e) {
            throw new Exception("Unable to open plugin jar at [" + pluginJarFileUrl + "] for reading.");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Obtains the manifest of the plugin file represented by the given deployment info.
     * Use this method rather than calling deploymentInfo.getManifest()
     * (workaround for https://jira.jboss.org/jira/browse/JBAS-6266).
     * 
     * @param pluginFile the plugin file
     * @return the deployed plugin's manifest
     */
    private static Manifest getManifest(File pluginFile) {
        try {
            JarFile jarFile = new JarFile(pluginFile);
            try {
                Manifest manifest = jarFile.getManifest();
                return manifest;
            } finally {
                jarFile.close();
            }
        } catch (Exception ignored) {
            return null; // this is OK, it just means we do not have a manifest
        }
    }
}
