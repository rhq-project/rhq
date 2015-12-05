/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.xml.sax.SAXException;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph.PluginDependency;
import org.rhq.core.clientapi.descriptor.group.expressions.CannedGroupExpressions;
import org.rhq.core.clientapi.descriptor.plugin.Bundle;
import org.rhq.core.clientapi.descriptor.plugin.ParentResourceType;
import org.rhq.core.clientapi.descriptor.plugin.PlatformDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.RunsInsideType;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Utilities for agent plugin descriptors.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public abstract class AgentPluginDescriptorUtil {
    private static final Log LOG = LogFactory.getLog(AgentPluginDescriptorUtil.class);

    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";
    private static final String PLUGIN_SCHEMA_PATH = "rhq-plugin.xsd";
    private static final String CANNED_GROUP_EXPRESSION_SCHEMA_PATH = "rhq-canned-groups.xsd";
    private static final String CANNED_GROUP_EXPRESSION_DESCRIPTOR_PATH = "META-INF/rhq-group-expressions.xml";

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
     * @throws IllegalArgumentException if the two plugins have different logical names
     */
    public static Plugin determineObsoletePlugin(Plugin plugin1, Plugin plugin2) {
        if (!plugin1.getName().equals(plugin2.getName())) {
            throw new IllegalArgumentException("The two plugins don't have the same name:" + plugin1 + ":" + plugin2);
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
    public static ComparableVersion getPluginVersion(File pluginFile, PluginDescriptor descriptor) throws Exception {

        if (descriptor == null) {
            descriptor = loadPluginDescriptorFromUrl(pluginFile.toURI().toURL());
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

    /**
     * Given an existing dependency graph and a plugin descriptor, this will add that plugin and its dependencies
     * to the dependency graph.
     *
     * @param dependencyGraph
     * @param descriptor
     */
    public static void addPluginToDependencyGraph(PluginDependencyGraph dependencyGraph, PluginDescriptor descriptor) {
        String pluginName = descriptor.getName();
        List<PluginDependencyGraph.PluginDependency> dependencies = new ArrayList<PluginDependencyGraph.PluginDependency>();
        for (PluginDescriptor.Depends dependency : descriptor.getDepends()) {
            String dependencyName = dependency.getPlugin();
            boolean useClasses = dependency.isUseClasses(); // TODO this may not be used anymore
            boolean required = true; // all <depends> plugins are implicitly required
            dependencies.add(new PluginDependencyGraph.PluginDependency(dependencyName, useClasses, required));
        }

        List<PlatformDescriptor> platforms = descriptor.getPlatforms();
        List<ServerDescriptor> servers = descriptor.getServers();
        List<ServiceDescriptor> services = descriptor.getServices();

        for (PlatformDescriptor platform : platforms) {
            addOptionalDependency(platform, dependencies);
        }
        for (ServerDescriptor server : servers) {
            addOptionalDependency(server, dependencies);
        }
        for (ServiceDescriptor service : services) {
            addOptionalDependency(service, dependencies);
        }

        dependencyGraph.addPlugin(pluginName, dependencies);
        return;
    }

    private static void addOptionalDependency(PlatformDescriptor platform,
        List<PluginDependencyGraph.PluginDependency> dependencies) {
        for (ServerDescriptor childServer : platform.getServers()) {
            addOptionalDependency(childServer, dependencies);
        }
        for (ServiceDescriptor childService : platform.getServices()) {
            addOptionalDependency(childService, dependencies);
        }

        addOptionalDependency(platform.getRunsInside(), dependencies);

        addOptionalBundleDependency(platform, dependencies);
        return;
    }

    private static void addOptionalDependency(ServerDescriptor server,
        List<PluginDependencyGraph.PluginDependency> dependencies) {
        for (ServerDescriptor childServer : server.getServers()) {
            addOptionalDependency(childServer, dependencies);
        }
        for (ServiceDescriptor childService : server.getServices()) {
            addOptionalDependency(childService, dependencies);
        }

        addOptionalDependency(server.getRunsInside(), dependencies);
        addOptionalDependency(server.getSourcePlugin(), dependencies);

        addOptionalBundleDependency(server, dependencies);
        return;
    }

    private static void addOptionalDependency(ServiceDescriptor service,
        List<PluginDependencyGraph.PluginDependency> dependencies) {
        for (ServiceDescriptor childService : service.getServices()) {
            addOptionalDependency(childService, dependencies);
        }

        addOptionalDependency(service.getRunsInside(), dependencies);
        addOptionalDependency(service.getSourcePlugin(), dependencies);

        addOptionalBundleDependency(service, dependencies);
    }

    private static void addOptionalDependency(RunsInsideType runsInside,
        List<PluginDependencyGraph.PluginDependency> dependencies) {

        if (runsInside != null) {
            List<ParentResourceType> parents = runsInside.getParentResourceType();
            for (ParentResourceType parent : parents) {
                addOptionalDependency(parent.getPlugin(), dependencies);
            }
        }
        return;
    }

    private static void addOptionalBundleDependency(ResourceDescriptor resource, List<PluginDependency> dependencies) {
        if (resource.getBundle() != null && resource.getBundle().getTargets() != null) {
            for (Bundle.Targets.ResourceType t : resource.getBundle().getTargets().getResourceType()) {
                addOptionalDependency(t.getPlugin(), dependencies);
            }
        }
    }

    private static void addOptionalDependency(String pluginName,
        List<PluginDependencyGraph.PluginDependency> dependencies) {

        if (pluginName != null) {
            boolean useClasses = false;
            boolean required = false;
            PluginDependency dep = new PluginDependencyGraph.PluginDependency(pluginName, useClasses, required);
            if (!dependencies.contains(dep)) {
                // only add it if it doesn't exist yet - this is so we don't override a required dep with an optional one
                dependencies.add(dep);
            }
        }
        return;
    }

    /**
     * Retrieves file content as string from given jar
     * @param pluginJarFileUrl  URL to a plugin jar file
     * @return content of additionPath file as String, or null if file does not exist in JAR
     * @throws PluginContainerException if we fail to read content
     */
    public static CannedGroupExpressions loadCannedExpressionsFromUrl(URL pluginJarFileUrl)
        throws PluginContainerException {
        final Log logger = LogFactory.getLog(AgentPluginDescriptorUtil.class);

        if (pluginJarFileUrl == null) {
            throw new PluginContainerException("A valid plugin JAR URL must be supplied.");
        }
        logger.debug("Loading plugin additions from plugin jar at [" + pluginJarFileUrl + "]...");
        ValidationEventCollector validationEventCollector = new ValidationEventCollector();
        testPluginJarIsReadable(pluginJarFileUrl);

        JarInputStream jis = null;
        JarEntry descriptorEntry = null;
        try {
            jis = new JarInputStream(pluginJarFileUrl.openStream());
            JarEntry nextEntry = jis.getNextJarEntry();
            while (nextEntry != null && descriptorEntry == null) {
                if (CANNED_GROUP_EXPRESSION_DESCRIPTOR_PATH.equals(nextEntry.getName())) {
                    descriptorEntry = nextEntry;
                } else {
                    jis.closeEntry();
                    nextEntry = jis.getNextJarEntry();
                }
            }

            if (descriptorEntry == null) {
                logger.debug("Plugin additions not found");
                // plugin additions are optional thing
                return null;
            }
            return parseCannedGroupExpressionsDescriptor(jis, validationEventCollector);
        } catch (Exception e) {
            throw new PluginContainerException("Could not parse the plugin additions ["
                + CANNED_GROUP_EXPRESSION_DESCRIPTOR_PATH + "] found in plugin jar at [" + pluginJarFileUrl + "].",
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

    /**
     * Loads a plugin descriptor from the given plugin jar and returns it.
     *
     * This is a static method to provide a convenience method for others to be able to use.
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

        PluginDescriptor pluginDescriptor = null;
        ValidationEventCollector validationEventCollector = null;

        try {
            validationEventCollector = new ValidationEventCollector();
            pluginDescriptor = loadPluginDescriptorFromUrl(pluginJarFileUrl, validationEventCollector, false, logger);

        } catch (PluginContainerException e) {
            if (skipValidation()) {
                // retry without validation, this is a jvm version with known issues
                validationEventCollector = new ValidationEventCollector();
                pluginDescriptor = loadPluginDescriptorFromUrl(pluginJarFileUrl, validationEventCollector, true, logger);

            } else {
                // probably a valid schema issue
                logValidationEvents(pluginJarFileUrl, validationEventCollector, logger);
                throw e;
            }
        }

        return pluginDescriptor;
    }

    // BZ 1264395 On certain JDKs there are bugs running the schema validation. Only add validation on approved JDKs.
    private static boolean skipValidation() {
        String vm = System.getProperty("java.vm.name", "unknown");
        String version = System.getProperty("java.version", "unknown");
        String java = vm + " " + version;
        String skipPattern = System.getProperty("org.rhq.xsl.validation.skip", "OpenJDK.*1\\.6.*");

        if (java.matches(skipPattern)) {
            LOG.debug("Skipping Agent Plugin XSL Validation because of known issues with [" + java + "]");
            return true;
        }

        return false;
    }

    private static PluginDescriptor loadPluginDescriptorFromUrl(URL pluginJarFileUrl,
        ValidationEventCollector validationEventCollector, boolean skipValidation, final Log logger)
        throws PluginContainerException {

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

            return (PluginDescriptor) parsePluginDescriptor(jis, validationEventCollector, PLUGIN_SCHEMA_PATH,
                DescriptorPackages.PC_PLUGIN, skipValidation);

        } catch (Exception e) {
            throw new PluginContainerException("Could not successfully parse the plugin descriptor ["
                + PLUGIN_DESCRIPTOR_PATH + "] found in plugin jar at [" + pluginJarFileUrl + "].",
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

    /**
     * @param is input to check
     * @return parsed PluginDescriptor
     * @throws PluginContainerException if validation fails
     */
    public static PluginDescriptor parsePluginDescriptor(InputStream is) throws PluginContainerException {
        return parsePluginDescriptor(is, new ValidationEventCollector());
    }

    /**
     * @param is input to check
     * @return parsed PluginDescriptor
     * @throws PluginContainerException if validation fails
     */
    public static PluginDescriptor parsePluginDescriptor(InputStream is,
        ValidationEventCollector validationEventCollector) throws PluginContainerException {
        JAXBContext jaxbContext;
        return (PluginDescriptor) parsePluginDescriptor(is, validationEventCollector, PLUGIN_SCHEMA_PATH,
            DescriptorPackages.PC_PLUGIN, false);
    }

    /**
     * @param is input to check
     * @return parsed PluginDescriptor
     * @throws PluginContainerException if validation fails
     */
    public static CannedGroupExpressions parseCannedGroupExpressionsDescriptor(InputStream is,
        ValidationEventCollector validationEventCollector) throws PluginContainerException {
        JAXBContext jaxbContext;
        return (CannedGroupExpressions) parsePluginDescriptor(is, validationEventCollector,
            CANNED_GROUP_EXPRESSION_SCHEMA_PATH, DescriptorPackages.CANNED_EXPRESSIONS, false);
    }

    /**
     * Parses a descriptor from InputStream without a validator.
     * @param is input to check
     * @return parsed PluginDescriptor
     * @throws PluginContainerException if validation fails
     */
    private static Object parsePluginDescriptor(InputStream is, ValidationEventCollector validationEventCollector,
        String xsd, String jaxbPackage, boolean skipValidation) throws PluginContainerException {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(jaxbPackage);
        } catch (Exception e) {
            throw new PluginContainerException("Failed to create JAXB Context.", new WrappedRemotingException(e));
        }

        Unmarshaller unmarshaller;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();

            if (!skipValidation) {
                URL pluginSchemaURL = AgentPluginDescriptorUtil.class.getClassLoader().getResource(xsd);
                Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                    pluginSchemaURL);
                unmarshaller.setSchema(pluginSchema);
            }

            unmarshaller.setEventHandler(validationEventCollector);
            return unmarshaller.unmarshal(is);

        } catch (JAXBException e) {
            throw new PluginContainerException(e);
        } catch (SAXException e) {
            throw new PluginContainerException(e);
        }
    }

    private static void logValidationEvents(URL pluginJarFileUrl, ValidationEventCollector validationEventCollector,
        Log logger) {
        for (ValidationEvent event : validationEventCollector.getEvents()) {
            // First build the message to be logged. The message will look something like this:
            //
            //   Validation fatal error while parsing [jopr-jboss-as-plugin-4.3.0-SNAPSHOT.jar:META-INF/rhq-plugin.xml]
            //   at line 221, column 94: cvc-minInclusive-valid: Value '20000' is not facet-valid with respect to
            //   minInclusive '30000' for type '#AnonType_defaultIntervalmetric'.
            //
            StringBuilder message = new StringBuilder();
            String severity = null;
            switch (event.getSeverity()) {
            case ValidationEvent.WARNING:
                severity = "warning";
                break;
            case ValidationEvent.ERROR:
                severity = "error";
                break;
            case ValidationEvent.FATAL_ERROR:
                severity = "fatal error";
                break;
            }
            message.append("Validation ").append(severity);
            File pluginJarFile = new File(pluginJarFileUrl.getPath());
            message.append(" while parsing [").append(pluginJarFile.getName()).append(":")
                .append(PLUGIN_DESCRIPTOR_PATH).append("]");
            ValidationEventLocator locator = event.getLocator();
            message.append(" at line ").append(locator.getLineNumber());
            message.append(", column ").append(locator.getColumnNumber());
            message.append(": ").append(event.getMessage());

            // Now write the message to the log at an appropriate level.
            switch (event.getSeverity()) {
            case ValidationEvent.WARNING:
            case ValidationEvent.ERROR:
                logger.warn(message);
                break;
            case ValidationEvent.FATAL_ERROR:
                logger.error(message);
                break;
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
