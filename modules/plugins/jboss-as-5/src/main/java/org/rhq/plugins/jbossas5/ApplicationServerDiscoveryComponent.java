/*
* Jopr Management Platform
* Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jbossas5.connection.LocalProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.helper.JBossInstallationInfo;
import org.rhq.plugins.jbossas5.helper.JBossInstanceInfo;
import org.rhq.plugins.jbossas5.helper.JBossProperties;
import org.rhq.plugins.jbossas5.util.JnpConfig;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

/**
 * A Resource discovery component for JBoss AS, 5.1.0.CR1 or later, and JBoss EAP, 5.0.0.Beta or later, Servers.
 *
 * @author Ian Springer
 * @author Mark Spritzler
 */
public class ApplicationServerDiscoveryComponent implements ResourceDiscoveryComponent {
    private static final String DEFAULT_RESOURCE_DESCRIPTION_AS = "JBoss Application Server (AS)";
    private static final String DEFAULT_RESOURCE_DESCRIPTION_EAP = "JBoss Enterprise Application Platform (EAP)";
    private static final String JBMANCON_DEBUG_SYSPROP = "jbmancon.debug";
    private static final String CHANGE_ME = "***CHANGE_ME***";
    private static final String JBOSS_SERVICE_XML = "conf" + File.separator + "jboss-service.xml";
    private static final String JBOSS_NAMING_SERVICE_XML = "deploy" + File.separator + "naming-service.xml";
    private static final String ANY_ADDRESS = "0.0.0.0";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String JAVA_HOME_ENV_VAR = "JAVA_HOME";
    private static final ComparableVersion AS_MINIMUM_VERSION = new ComparableVersion("5.1.0.CR1");
    private static final ComparableVersion EAP_MINIMUM_VERSION = new ComparableVersion("5.0.0.Beta");

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) {
        log.trace("Discovering " + discoveryContext.getResourceType().getName() + " Resources...");

        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        // NOTE: The PC will never actually pass in more than one plugin config...
        List<Configuration> manuallyAddedJBossAsPluginConfigs = discoveryContext.getPluginConfigurations();
        if (!manuallyAddedJBossAsPluginConfigs.isEmpty()) {
            Configuration pluginConfig = manuallyAddedJBossAsPluginConfigs.get(0);
            DiscoveredResourceDetails manuallyAddedJBossAS = createDetailsForManuallyAddedJBossAS(discoveryContext,
                pluginConfig);
            resources.add(manuallyAddedJBossAS);
        } else {
            DiscoveredResourceDetails inProcessJBossAS = discoverInProcessJBossAS(discoveryContext);
            if (inProcessJBossAS != null) {
                // If we're running inside a JBoss AS JVM, that's the only AS instance we want to discover.
                resources.add(inProcessJBossAS);
            } else {
                // Otherwise, scan the process table for external AS instances.
                resources.addAll(discoverExternalJBossAsProcesses(discoveryContext));
            }
        }
        log
            .trace("Discovered " + resources.size() + " " + discoveryContext.getResourceType().getName()
                + " resources.");

        return resources;
    }

    private Set<DiscoveredResourceDetails> discoverExternalJBossAsProcesses(ResourceDiscoveryContext discoveryContext) {
        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> autoDiscoveryResults = discoveryContext.getAutoDiscoveredProcesses();

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

            // See if this JBAS instance's version is less than 5.1.0.CR1 - if so, skip it.
            JBossInstallationInfo installInfo = cmdLine.getInstallInfo();
            ComparableVersion version = new ComparableVersion(installInfo.getVersion());

            String productType = installInfo.getProductType().name();

            // Check if this is a compatible JBoass AS instance
            if (productType.equals("AS") && version.compareTo(AS_MINIMUM_VERSION) < 0) {
                if (log.isDebugEnabled())
                    log.debug("JBAS version " + version + " is not supported by this plugin (minimum version is "
                        + AS_MINIMUM_VERSION + ") - skipping...");
                continue;
            }
            // Check if this is a compatible JBoass EAP instance
            if (productType.equals("EAP") && version.compareTo(EAP_MINIMUM_VERSION) < 0) {
                if (log.isDebugEnabled())
                    log.debug("JBEAP version " + version + " is not supported by this plugin (minimum version is "
                        + EAP_MINIMUM_VERSION + ") - skipping...");
                continue;
            }
            File installHome = new File(cmdLine.getSystemProperties().getProperty(JBossProperties.HOME_DIR));
            File configDir = new File(cmdLine.getSystemProperties().getProperty(JBossProperties.SERVER_HOME_DIR));

            // The config dir might be a symlink - call getCanonicalFile() to resolve it if so, before
            // calling isDirectory() (isDirectory() returns false for a symlink, even if it points at
            // a directory).
            try {
                if (!configDir.getCanonicalFile().isDirectory()) {
                    log.warn("Skipping discovery for process " + processInfo + ", because JBAS configuration dir '"
                        + configDir + "' does not exist or is not a directory.");
                    continue;
                }
            } catch (IOException e) {
                log.error("Skipping discovery for process " + processInfo + ", because JBAS configuration dir '"
                    + configDir + "' could not be canonicalized.", e);
                continue;
            }

            Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();

            String jnpURL = getJnpURL(cmdLine, installHome, configDir);

            // TODO? Set the connection type - local or remote

            // Set the required props...
            pluginConfiguration.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.NAMING_URL,
                jnpURL));
            pluginConfiguration.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.HOME_DIR,
                installHome.getAbsolutePath()));
            pluginConfiguration.put(new PropertySimple(
                ApplicationServerComponent.PluginConfigPropNames.SERVER_HOME_DIR, configDir));

            // Set the optional props...
            pluginConfiguration.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.SERVER_NAME,
                cmdLine.getSystemProperties().getProperty(JBossProperties.SERVER_NAME)));
            pluginConfiguration.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.BIND_ADDRESS,
                cmdLine.getSystemProperties().getProperty(JBossProperties.BIND_ADDRESS)));

            String javaHome = processInfo.getEnvironmentVariable(JAVA_HOME_ENV_VAR);
            if (javaHome == null && log.isDebugEnabled()) {
                log.debug("JAVA_HOME environment variable not set in JBossAS process - defaulting "
                    + ApplicationServerComponent.PluginConfigPropNames.JAVA_HOME
                    + "connection property to the plugin container JRE dir.");
                javaHome = System.getenv(JAVA_HOME_ENV_VAR);
            }

            pluginConfiguration.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.JAVA_HOME,
                javaHome));

            initLogEventSourcesConfigProp(configDir, pluginConfiguration);

            // TODO: Init props that have static defaults.
            //setPluginConfigurationDefaults(pluginConfiguration);

            DiscoveredResourceDetails resourceDetails = createResourceDetails(discoveryContext, pluginConfiguration,
                processInfo, installInfo);
            resources.add(resourceDetails);
        }
        return resources;
    }

    private DiscoveredResourceDetails createDetailsForManuallyAddedJBossAS(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfig) {
        // Set default values on any props that are not set.
        //setPluginConfigurationDefaults(pluginConfiguration);

        ProcessInfo processInfo = null;
        String jbossHomeDir = pluginConfig.getSimple(ApplicationServerComponent.PluginConfigPropNames.HOME_DIR)
            .getStringValue();
        JBossInstallationInfo installInfo;
        try {
            installInfo = new JBossInstallationInfo(new File(jbossHomeDir));
        } catch (IOException e) {
            throw new InvalidPluginConfigurationException(e);
        }
        DiscoveredResourceDetails resourceDetails = createResourceDetails(discoveryContext, pluginConfig, processInfo,
            installInfo);
        return resourceDetails;
    }

    @Nullable
    private DiscoveredResourceDetails discoverInProcessJBossAS(ResourceDiscoveryContext discoveryContext) {
        ProfileServiceConnectionProvider connectionProvider = new LocalProfileServiceConnectionProvider();
        ProfileServiceConnection connection;
        try {
            connection = connectionProvider.connect();
        } catch (Exception e) {
            // This most likely just means we're not embedded inside a JBoss AS 5.x instance.
            log.debug("Unable to connect to in-process ProfileService: " + e);
            return null;
        }

        ManagementView managementView = connection.getManagementView();
        ManagedComponent serverConfigComponent = ManagedComponentUtils.getSingletonManagedComponent(managementView,
            new ComponentType("MCBean", "ServerConfig"));
        String serverName = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent, "serverName");

        // serverHomeDir is the full path to the instance's configuration dir, e.g. "/opt/jboss-5.1.0.GA/server/default";
        // That's guaranteed to be unique among JBAS instances on the same machine, so we'll use it as the Resource key.
        String serverHomeDir = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent,
            "serverHomeDir");
        String resourceKey = serverHomeDir;

        // homeDir is the full path to the JBoss installation dir used by this instance, e.g. "/opt/jboss-5.1.0.GA".
        String homeDir = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent, "homeDir");
        // Figure out if the instance is AS or EAP, and reflect that in the Resource name.
        JBossInstallationInfo installInfo;
        try {
            installInfo = new JBossInstallationInfo(new File(homeDir));
        } catch (IOException e) {
            throw new InvalidPluginConfigurationException(e);
        }
        String resourceName = "JBoss ";
        resourceName += installInfo.isEap() ? "EAP " : "AS ";
        resourceName += installInfo.getMajorVersion();
        resourceName += " (" + serverName + ")";

        String description = installInfo.isEap() ? DEFAULT_RESOURCE_DESCRIPTION_EAP : DEFAULT_RESOURCE_DESCRIPTION_AS;

        String version = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent,
            "specificationVersion");

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        pluginConfig.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.HOME_DIR, homeDir));
        pluginConfig.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.SERVER_HOME_DIR,
            serverHomeDir));
        pluginConfig.put(new PropertySimple(ApplicationServerComponent.PluginConfigPropNames.SERVER_NAME, serverName));

        boolean debug = Boolean.getBoolean(JBMANCON_DEBUG_SYSPROP);
        if (debug) {
            //new UnitTestRunner().runUnitTests(connection);
        }

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), resourceKey, resourceName, version,
            description, pluginConfig, null);
    }

    private DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfiguration, @Nullable ProcessInfo processInfo, JBossInstallationInfo installInfo) {
        String serverHomeDir = pluginConfiguration.getSimple(
            ApplicationServerComponent.PluginConfigPropNames.SERVER_HOME_DIR).getStringValue();
        File absoluteConfigPath = ApplicationServerComponent.resolvePathRelativeToHomeDir(pluginConfiguration,
            serverHomeDir);

        // Canonicalize the config path, so it's consistent no matter how it's entered.
        // This prevents two servers with different forms of the same config path, but
        // that are actually the same server, from ending up in inventory.
        // JON: fix for JBNADM-2634 - do not resolve symlinks (ips, 12/18/07)
        String key = FileUtils.getCanonicalPath(absoluteConfigPath.getPath());

        String bindAddress = pluginConfiguration.getSimple(
            ApplicationServerComponent.PluginConfigPropNames.BIND_ADDRESS).getStringValue();
        String namingUrl = pluginConfiguration.getSimple(ApplicationServerComponent.PluginConfigPropNames.NAMING_URL)
            .getStringValue();

        // Only include the JNP port in the Resource name if its value is not "***CHANGE_ME***".
        String namingPort = null;
        //noinspection ConstantConditions
        int colonIndex = namingUrl.lastIndexOf(':');
        if ((colonIndex != -1) && (colonIndex != (namingUrl.length() - 1))) {
            // NOTE: We assume the JNP URL does not have a trailing slash.
            String port = namingUrl.substring(colonIndex + 1);
            if (!port.equals(CHANGE_ME))
                namingPort = port;
        }

        String configName = absoluteConfigPath.getName();
        String baseName = discoveryContext.getSystemInformation().getHostname();
        String description = installInfo.getProductType().DESCRIPTION;
        File deployDir = new File(absoluteConfigPath, "deploy");
        File rhqInstallerWar = new File(deployDir, "rhq-installer.war");
        File rhqInstallerWarUndeployed = new File(deployDir, "rhq-installer.war.rej");
        boolean isRhqServer = rhqInstallerWar.exists() || rhqInstallerWarUndeployed.exists();
        if (isRhqServer) {
            baseName += " Jopr Server, ";
            description += " hosting the Jopr Server";
            // We know this is an RHQ Server. Let's add an event source for its server log file, but disable it by default.
            configureEventSourceForServerLogFile(pluginConfiguration);
        }
        String name = formatServerName(baseName, bindAddress, namingPort, configName, installInfo);

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, installInfo.getVersion(),
            description, pluginConfiguration, processInfo);
    }

    private String formatServerName(String baseName, String bindingAddress, String jnpPort, String configName,
        JBossInstallationInfo installInfo) {
        baseName = baseName + " " + installInfo.getProductType().NAME + " " + installInfo.getVersion() + " "
            + configName;
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

    private void configureEventSourceForServerLogFile(Configuration pluginConfiguration) {
        File rhqLogFile = ApplicationServerComponent.resolvePathRelativeToHomeDir(pluginConfiguration,
            "../logs/rhq-server-log4j.log");
        if (rhqLogFile.exists() && !rhqLogFile.isDirectory()) {
            try {
                PropertyMap serverLogEventSource = new PropertyMap("serverLog");
                serverLogEventSource.put(new PropertySimple(
                    LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.LOG_FILE_PATH, rhqLogFile
                        .getCanonicalPath()));
                serverLogEventSource.put(new PropertySimple(
                    LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.ENABLED, Boolean.FALSE));
                serverLogEventSource.put(new PropertySimple(
                    LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.MINIMUM_SEVERITY, "info"));
                PropertyList logEventSources = pluginConfiguration
                    .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);
                logEventSources.add(serverLogEventSource);
            } catch (IOException e) {
                log.warn("Unable to setup RHQ Server log file monitoring.", e);
            }
        }
    }

    private String getJnpURL(JBossInstanceInfo cmdLine, File installHome, File configDir) {
        File urlStore = new File(configDir, "data/jnp-service.url");
        if (urlStore.exists() && urlStore.canRead()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(urlStore));
                String jnpUrl = br.readLine();
                if (jnpUrl != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Read JNP URL from jnp-service.url file: " + jnpUrl);
                    }
                    return jnpUrl;
                }
            } catch (IOException ioe) {
                // Nothing to do
            }
        }

        log.warn("Failed to read jnp-service.url from " + configDir + "/data");

        // Above did not work, so fall back to our previous scheme
        JnpConfig jnpConfig = getJnpConfig(installHome, configDir, cmdLine.getSystemProperties());
        String jnpAddress = (jnpConfig.getJnpAddress() != null) ? jnpConfig.getJnpAddress() : CHANGE_ME;
        if (ANY_ADDRESS.equals(jnpAddress)) {
            jnpAddress = LOCALHOST;
        }
        String jnpPort = (jnpConfig.getJnpPort() != null) ? String.valueOf(jnpConfig.getJnpPort()) : CHANGE_ME;
        return "jnp://" + jnpAddress + ":" + jnpPort;
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

    private void initLogEventSourcesConfigProp(File configDir, Configuration pluginConfig) {
        File logDir = new File(configDir, "log");
        File serverLogFile = new File(logDir, "server.log");
        if (serverLogFile.exists() && !serverLogFile.isDirectory()) {
            PropertyMap serverLogEventSource = new PropertyMap("serverLog");
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.LOG_FILE_PATH, serverLogFile));
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.ENABLED, Boolean.FALSE));
            PropertyList logEventSources = pluginConfig
                .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);
            logEventSources.add(serverLogEventSource);
        }
    }
}
