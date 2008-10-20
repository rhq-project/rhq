 /*
  * Jopr Management Platform
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
package org.rhq.plugins.jbossas;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.JBossConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jbossas.helper.JBossInstallationInfo;
import org.rhq.plugins.jbossas.helper.JBossInstanceInfo;
import org.rhq.plugins.jbossas.helper.JBossProperties;
import org.rhq.plugins.jbossas.util.JBossMBeanUtility;
import org.rhq.plugins.jbossas.util.JnpConfig;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * Discovers JBossAS 3.2.x and 4.x server instances.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class JBossASDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(JBossASDiscoveryComponent.class);

    private static final String JBOSS_SERVICE_XML = "conf" + File.separator + "jboss-service.xml";
    private static final String JBOSS_NAMING_SERVICE_XML = "deploy" + File.separator + "naming-service.xml";
    private static final String JAVA_HOME_ENV_VAR = "JAVA_HOME";
    private static final String ANY_ADDRESS = "0.0.0.0";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String CHANGE_ME = "***CHANGE_ME***";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.debug("Discovering JBossAS 3.2.x and 4.x servers...");
        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        DiscoveredResourceDetails jbossPcIsEmbeddedIn = discoverJBossPcIsEmbeddedIn(context);
        if (jbossPcIsEmbeddedIn != null) {
            resources.add(jbossPcIsEmbeddedIn);
        }
        processAutoDiscoveredProcesses(context, resources, jbossPcIsEmbeddedIn);
        processManuallyAddedResources(context, resources);
        return resources;
    }

    private void processAutoDiscoveredProcesses(ResourceDiscoveryContext context,
        Set<DiscoveredResourceDetails> resources, DiscoveredResourceDetails jbossPcIsEmbeddedIn) {
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            ProcessInfo processInfo = autoDiscoveryResult.getProcessInfo();
            if (log.isDebugEnabled())
                log.debug("Discovered JBossAS process: " + processInfo);

            JBossInstanceInfo cmdLine;
            try {
                cmdLine = new JBossInstanceInfo(processInfo);
            } catch (Exception e) {
                log.error("Failed to process JBossAS command line: " + Arrays.asList(processInfo.getCommandLine()), e);
                continue;
            }

            // See if we have an AS 5 - if so, skip it.
            JBossInstallationInfo installInfo = cmdLine.getInstallInfo();
            if (installInfo.getVersion().startsWith("5")) {
                if (log.isDebugEnabled())
                    log.debug("Found an AS 5, which is not supported by this plugin - skipping...");
                continue;
            }

            File installHome = new File(cmdLine.getSystemProperties().getProperty(JBossProperties.HOME_DIR));
            File configDir = new File(cmdLine.getSystemProperties().getProperty(JBossProperties.SERVER_HOME_DIR));

            if ((jbossPcIsEmbeddedIn != null)
                && jbossPcIsEmbeddedIn.getPluginConfiguration().getSimple(
                    JBossASServerComponent.CONFIGURATION_PATH_CONFIG_PROP).getStringValue().equals(
                    configDir.getAbsolutePath())) {
                // We're running embedded, and the JBossAS server we're embedded in has already been found.
                continue;
            }

            // The config dir might be a symlink - call getCanonicalFile() to resolve it if so, before
            // calling isDirectory() (isDirectory() returns false for a symlink, even if it points at
            // a directory).
            try {
                if (!configDir.getCanonicalFile().isDirectory()) {
                    log.warn("Skipping discovery for process " + processInfo + ", because JBAS configuration dir '"
                        + configDir + "' does not exist or is not a directory.");
                    continue;
                }
            }
            catch (IOException e) {
                log.error("Skipping discovery for process " + processInfo + ", because JBAS configuration dir '"
                        + configDir + "' could not be canonicalized.", e);
                continue;
            }

            Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

            String jnpURL = getJnpURL(cmdLine, installHome, configDir);

            // Set the connection type (used by JMX plugin to connect to the MBean server).
            pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                JBossConnectionTypeDescriptor.class.getName()));

            // Set the required props...
            pluginConfiguration.put(new PropertySimple(JBossASServerComponent.NAMING_URL_CONFIG_PROP, jnpURL));
            pluginConfiguration.put(new PropertySimple(JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP, installHome
                .getAbsolutePath()));
            pluginConfiguration
                .put(new PropertySimple(JBossASServerComponent.CONFIGURATION_PATH_CONFIG_PROP, configDir));

            // Set the optional props...
            pluginConfiguration.put(new PropertySimple(JBossASServerComponent.CONFIGURATION_SET_CONFIG_PROP, cmdLine
                .getSystemProperties().getProperty(JBossProperties.SERVER_NAME)));
            pluginConfiguration.put(new PropertySimple(JBossASServerComponent.BINDING_ADDRESS_CONFIG_PROP, cmdLine
                .getSystemProperties().getProperty(JBossProperties.BIND_ADDRESS)));

            String javaHome = processInfo.getEnvironmentVariable(JAVA_HOME_ENV_VAR);
            if (javaHome == null && log.isDebugEnabled()) {
                log.debug("JAVA_HOME environment variable not set in JBossAS process - defaulting "
                    + JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP
                    + "connection property to the plugin container JRE dir.");
            }

            pluginConfiguration.put(new PropertySimple(JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP, javaHome));

            initLogEventSourcesConfigProp(configDir, pluginConfiguration);

            // Init props that have static defaults.
            setPluginConfigurationDefaults(pluginConfiguration);

            DiscoveredResourceDetails resourceDetails = createResourceDetails(context, pluginConfiguration,
                processInfo, installInfo);
            resources.add(resourceDetails);
        }
    }

    private void processManuallyAddedResources(ResourceDiscoveryContext context,
        Set<DiscoveredResourceDetails> resources) {
        // NOTE: The PC will never actually pass in more than one plugin config...
        List<Configuration> pluginConfigurations = context.getPluginConfigurations();
        for (Configuration pluginConfiguration : pluginConfigurations) {
            // Set the connection type (used by JMX plugin to connect to the MBean server).
            pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                JBossConnectionTypeDescriptor.class.getName()));

            // Set default values on any props that are not set.
            setPluginConfigurationDefaults(pluginConfiguration);

            ProcessInfo processInfo = null;
            String jbossHomeDir = pluginConfiguration.getSimpleValue(JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP,
                null);// this will never be null
            JBossInstallationInfo installInfo;
            try {
                installInfo = new JBossInstallationInfo(new File(jbossHomeDir));
            } catch (IOException e) {
                throw new InvalidPluginConfigurationException(e);
            }
            DiscoveredResourceDetails resourceDetails = createResourceDetails(context, pluginConfiguration,
                processInfo, installInfo);
            resources.add(resourceDetails);
        }
    }

    private void initLogEventSourcesConfigProp(File configDir, Configuration pluginConfiguration) {
        File logDir = new File(configDir, "log");
        File serverLogFile = new File(logDir, "server.log");
        if (serverLogFile.exists() && !serverLogFile.isDirectory()) {
            PropertyMap serverLogEventSource = new PropertyMap("serverLog");
            serverLogEventSource.put(new PropertySimple(
                JBossASServerComponent.LogEventSourcePropertyNames.LOG_FILE_PATH, serverLogFile));
            serverLogEventSource.put(new PropertySimple(JBossASServerComponent.LogEventSourcePropertyNames.ENABLED,
                Boolean.FALSE));
            PropertyList logEventSources = pluginConfiguration
                .getList(JBossASServerComponent.LOG_EVENT_SOURCES_CONFIG_PROP);
            logEventSources.add(serverLogEventSource);
        }
    }

    private static JnpConfig getJnpConfig(File installHome, File configDir, Properties props) {
        File serviceXML = new File(configDir, JBOSS_SERVICE_XML);
        JnpConfig config = JnpConfig.getConfig(installHome, serviceXML, props);
        if ((config == null) || (config.getJnpPort() == null)) {
            File namingServiceFile = new File(configDir, JBOSS_NAMING_SERVICE_XML);
            if (namingServiceFile.exists()) {
                config = JnpConfig.getConfig(installHome, namingServiceFile, props);
            }
        }
        return config;
    }

    private void setPluginConfigurationDefaults(Configuration pluginConfiguration) {
        setPropertySimpleIfNotAlreadySet(pluginConfiguration, JBossASServerComponent.START_SCRIPT_CONFIG_PROP,
            JBossASServerComponent.DEFAULT_START_SCRIPT);
        setPropertySimpleIfNotAlreadySet(pluginConfiguration, JBossASServerComponent.SHUTDOWN_SCRIPT_CONFIG_PROP,
            JBossASServerComponent.DEFAULT_SHUTDOWN_SCRIPT);
        setPropertySimpleIfNotAlreadySet(pluginConfiguration, JBossASServerComponent.JAVA_HOME_PATH_CONFIG_PROP,
            JBossASServerComponent.DEFAULT_JAVA_HOME);
        setPropertySimpleIfNotAlreadySet(pluginConfiguration, JBossASServerComponent.BINDING_ADDRESS_CONFIG_PROP,
            JBossASServerComponent.DEFAULT_BIND_ADDRESS);
    }

    private void setPropertySimpleIfNotAlreadySet(Configuration pluginConfiguration, String propName, String propValue) {
        PropertySimple prop = pluginConfiguration.getSimple(propName);
        if ((prop == null) || (prop.getStringValue() == null)) {
            pluginConfiguration.put(new PropertySimple(propName, propValue));
        }
    }

    private DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfiguration, @Nullable
        ProcessInfo processInfo, JBossInstallationInfo installInfo) {
        String configPath = pluginConfiguration.getSimple(JBossASServerComponent.CONFIGURATION_PATH_CONFIG_PROP)
            .getStringValue();
        File absoluteConfigPath = JBossASServerComponent.resolvePathRelativeToHomeDir(pluginConfiguration, configPath);

        // Canonicalize the config path, so it's consistent no matter how it's entered.
        // This prevents two servers with different forms of the same config path, but
        // that are actually the same server, from ending up in inventory.
        // JON: fix for JBNADM-2634 - do not resolve symlinks (ips, 12/18/07)
        String key = FileUtils.getCanonicalPath(absoluteConfigPath.getPath());

        String bindingAddress = pluginConfiguration.getSimple(JBossASServerComponent.BINDING_ADDRESS_CONFIG_PROP)
            .getStringValue();
        String namingUrl = pluginConfiguration.getSimple(JBossASServerComponent.NAMING_URL_CONFIG_PROP)
            .getStringValue();

        // Only include the JNP port in the Resource name if its value is not "***CHANGE_ME***".
        String namingPort = null;
        //noinspection ConstantConditions
        int colonIndex = namingUrl.lastIndexOf(':');
        if ((colonIndex != -1) && (colonIndex != (namingUrl.length() - 1))) {
            // NOTE: We assume the JNP URL does not have a trailing slash.
            String port = namingUrl.substring(colonIndex + 1);
            if (!port.equals(CHANGE_ME)) {
                namingPort = port;
            }
        }

        String configName = absoluteConfigPath.getName();
        String baseName = discoveryContext.getSystemInformation().getHostname();
        String description = installInfo.getProductType().DESCRIPTION;
        File deployDir = new File(absoluteConfigPath, "deploy");
        File rhqInstallerWar = new File(deployDir, "rhq-installer.war");
        boolean isRhqServer = rhqInstallerWar.exists();
        if (isRhqServer) {
            baseName += " RHQ Server, ";
            description += " hosting the RHQ Server";

            // RHQ-633 : We know this is an RHQ Server. Let's auto configure for tracking its log file
            File rhqLogFile = JBossASServerComponent.resolvePathRelativeToHomeDir(pluginConfiguration,
                "../logs/rhq-server-log4j.log");
            if (rhqLogFile.exists() && !rhqLogFile.isDirectory()) {
                try {
                    PropertyMap serverLogEventSource = new PropertyMap("serverLog");
                    serverLogEventSource
                        .put(new PropertySimple(JBossASServerComponent.LogEventSourcePropertyNames.LOG_FILE_PATH,
                            rhqLogFile.getCanonicalPath()));
                    serverLogEventSource.put(new PropertySimple(
                        JBossASServerComponent.LogEventSourcePropertyNames.ENABLED, Boolean.TRUE));
                    serverLogEventSource.put(new PropertySimple(
                        JBossASServerComponent.LogEventSourcePropertyNames.MINIMUM_SEVERITY, "info"));
                    PropertyList logEventSources = pluginConfiguration
                        .getList(JBossASServerComponent.LOG_EVENT_SOURCES_CONFIG_PROP);
                    logEventSources.add(serverLogEventSource);
                } catch (IOException e) {
                    log.warn("Unable to setup rhq server log monitoring", e);
                }

            }

        }
        String name = formatServerName(baseName, bindingAddress, namingPort, configName, installInfo);

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, installInfo.getVersion(),
            description, pluginConfiguration, processInfo);
    }

    @Nullable
    private DiscoveredResourceDetails discoverJBossPcIsEmbeddedIn(ResourceDiscoveryContext context) {
        MBeanServer server = JBossMBeanUtility.getJBossMBeanServer();

        try {
            String jnpAddress = null;
            String jnpPort = null;
            ObjectName namingObjectName = new ObjectName("jboss:service=Naming");
            Set namingSet = server.queryNames(namingObjectName, null);
            if (namingSet.iterator().hasNext()) {
                jnpAddress = (String) server.getAttribute(namingObjectName, "BindAddress");
                jnpPort = String.valueOf(server.getAttribute(namingObjectName, "Port"));
            }

            String bindAddress = null;
            ObjectName systemPropertiesObjectName = new ObjectName("jboss:name=SystemProperties,type=Service");
            Set systemPropertiesSet = server.queryNames(systemPropertiesObjectName, null);
            if (systemPropertiesSet.iterator().hasNext()) {
                bindAddress = (String) server.invoke(systemPropertiesObjectName, "get",
                    new Object[] { JBossProperties.BIND_ADDRESS }, new String[] { String.class.getName() });
            }
            if (bindAddress == null) {
                bindAddress = jnpAddress;
            }

            ObjectName configObjectName = new ObjectName("jboss.system:type=ServerConfig");
            Set set = server.queryNames(configObjectName, null);
            if (set.iterator().hasNext()) {
                // ServerConfig MBean found
                File homeDir = (File) server.getAttribute(configObjectName, "HomeDir");
                JBossInstallationInfo installInfo;
                try {
                    installInfo = new JBossInstallationInfo(homeDir);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                File configDir = (File) server.getAttribute(configObjectName, "ServerHomeDir");
                String configName = (String) server.getAttribute(configObjectName, "ServerName");
                String version = (String) server.getAttribute(configObjectName, "SpecificationVersion");

                Configuration pluginConfiguration = context.getDefaultPluginConfiguration();

                // Set the connection type (used by JMX plugin to connect to the MBean server).
                pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                    InternalVMTypeDescriptor.class.getName()));

                pluginConfiguration.put(new PropertySimple(JBossASServerComponent.JBOSS_HOME_DIR_CONFIG_PROP, homeDir));
                pluginConfiguration.put(new PropertySimple(JBossASServerComponent.CONFIGURATION_PATH_CONFIG_PROP,
                    configDir));
                pluginConfiguration.put(new PropertySimple(JBossASServerComponent.CONFIGURATION_SET_CONFIG_PROP,
                    configName));

                // Now set default values on any props that are still not set.
                setPluginConfigurationDefaults(pluginConfiguration);

                String resourceName = formatServerName(context.getSystemInformation().getHostname(), bindAddress,
                    jnpPort, configName, installInfo);
                DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), configDir
                    .getAbsolutePath(), resourceName, version,
                    "JBossAS server that RHQ Plugin Container is running within", pluginConfiguration, null);

                return resource;
            }
        } catch (Exception e) {
            /* JBoss MBean doesn't exist - not a JBoss server. */
            if (log.isDebugEnabled())
                log.debug("Not detected to be embedded in a JBossAS Server", e);
        }

        return null;
    }

    public String formatServerName(String baseName, String bindingAddress, String jnpPort, String configName,
        JBossInstallationInfo installInfo) {
        baseName = baseName + " " + installInfo.getProductType().NAME + " " + installInfo.getVersion() + " "
            + configName;

        if ((bindingAddress != null) && bindingAddress.equals(LOCALHOST)) {
            bindingAddress = null;
        }

        String details = null;
        if ((bindingAddress != null) && (jnpPort != null && !jnpPort.equals(CHANGE_ME))) {
            details = bindingAddress + ":" + jnpPort;
        } else if ((bindingAddress == null) && (jnpPort != null && !jnpPort.equals(CHANGE_ME))) {
            details = jnpPort;
        } else if (bindingAddress != null) {
            details = bindingAddress;
        }

        return baseName + ((details != null) ? (" (" + details + ")") : "");
    }

    private static String getJnpURL(JBossInstanceInfo cmdLine, File installHome, File configDir) {
        JnpConfig jnpConfig = getJnpConfig(installHome, configDir, cmdLine.getSystemProperties());
        String jnpAddress = (jnpConfig.getJnpAddress() != null) ? jnpConfig.getJnpAddress() : CHANGE_ME;
        if (jnpAddress.equals(ANY_ADDRESS)) {
            jnpAddress = LOCALHOST;
        }
        String jnpPort = (jnpConfig.getJnpPort() != null) ? String.valueOf(jnpConfig.getJnpPort()) : CHANGE_ME;
        return "jnp://" + jnpAddress + ":" + jnpPort;
    }
}
