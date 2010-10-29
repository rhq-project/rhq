/*
* Jopr Management Platform
* Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jbossas5.helper.JBossInstallationInfo;
import org.rhq.plugins.jbossas5.helper.JBossInstanceInfo;
import org.rhq.plugins.jbossas5.helper.JBossProperties;
import org.rhq.plugins.jbossas5.helper.JBossProductType;
import org.rhq.plugins.jbossas5.util.JnpConfig;

import org.jboss.on.common.jbossas.JBossASDiscoveryUtils;

/**
 * A Resource discovery component for JBoss AS Server Resources, which include the following:
 *
 *   JBoss AS, 5.2.0.Beta1 and later
 *   JBoss EAP, 5.0.0.Beta and later
 *   JBoss EWP, 5.0.0.CR1 and later
 *   JBoss SOA-P, 5.0.0.Beta and later
 *
 * @author Ian Springer
 * @author Mark Spritzler
 */
public class ApplicationServerDiscoveryComponent implements ResourceDiscoveryComponent, ClassLoaderFacet,
        ManualAddFacet {
    private static final String CHANGE_ME = "***CHANGE_ME***";
    private static final String JBOSS_SERVICE_XML = "conf" + File.separator + "jboss-service.xml";
    private static final String JBOSS_NAMING_SERVICE_XML = "deploy" + File.separator + "naming-service.xml";
    private static final String ANY_ADDRESS = "0.0.0.0";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String JAVA_HOME_ENV_VAR = "JAVA_HOME";

    private static final Map<JBossProductType, ComparableVersion> MINIMUM_PRODUCT_VERSIONS = new HashMap(3);
    static {
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.AS, new ComparableVersion("5.2.0.Beta1"));
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.EAP, new ComparableVersion("5.0.0.Beta"));
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.EWP, new ComparableVersion("5.0.0.CR1"));
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.SOA, new ComparableVersion("5.0.0.Beta"));
    }

    private static final List<String> CLIENT_JARS = Arrays.asList(
        // NOTE: The jbossall-client.jar aggregates a whole bunch of other jars from the client dir via its
        // MANIFEST.MF Class-Path.
        "client/jbossall-client.jar",
        "client/trove.jar",
        "client/javassist.jar",
        "common/lib/jboss-security-aspects.jar",
        "lib/jboss-managed.jar",
        "lib/jboss-metatype.jar",
        "lib/jboss-dependency.jar",
        "lib/jboss-reflect.jar"
    );

    private static final List<String> AS6_CLIENT_JARS = new ArrayList<String>(CLIENT_JARS);
    static {
        // The below jars are required for JBoss AS 6.0 M1, M2, and M3.
        AS6_CLIENT_JARS.add("lib/jboss-classpool.jar");
        AS6_CLIENT_JARS.add("lib/jboss-classpool-scoped.jar");
    }

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) {
        log.trace("Discovering JBoss AS 5.x and 6.x Resources...");
        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        DiscoveredResourceDetails inProcessJBossAS = discoverInProcessJBossAS(discoveryContext);
        if (inProcessJBossAS != null) {
            // If we're running inside a JBoss AS JVM, that's the only AS instance we want to discover.
            resources.add(inProcessJBossAS);
        } else {
            // Otherwise, scan the process table for external AS instances.
            resources.addAll(discoverExternalJBossAsProcesses(discoveryContext));
        }
        log.trace("Discovered " + resources.size() + " JBossAS 5.x and 6.x Resources.");
        return resources;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext discoveryContext)
            throws InvalidPluginConfigurationException {
        // Set default values on any props that are not set.
        //setPluginConfigurationDefaults(pluginConfiguration);

        ProcessInfo processInfo = null;
        String jbossHomeDir = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.HOME_DIR)
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

    @SuppressWarnings("unchecked")
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext context, DiscoveredResourceDetails details) 
            throws Exception {
        Configuration pluginConfig = details.getPluginConfiguration();
        String homeDir = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.HOME_DIR).getStringValue();

        List<URL> clientJars = new ArrayList<URL>();

        for (String jarFileName : getClientJars(pluginConfig)) {
            File clientJar = new File(homeDir, jarFileName);
            if (!clientJar.exists()) {
                throw new FileNotFoundException("Cannot find [" + clientJar + "] - unable to manage server.");
            }
            if (!clientJar.canRead()) {
                throw new IOException("Cannot read [" + clientJar + "] - unable to manage server.");
            }
            clientJars.add(clientJar.toURI().toURL());
        }

        return clientJars;
    }

    private List<String> getClientJars(Configuration pluginConfig) throws IOException {
        PropertySimple jbossHomeDir = pluginConfig.getSimple("homeDir");
        JBossInstallationInfo installationInfo = new JBossInstallationInfo(new File(jbossHomeDir.getStringValue()));

        if (installationInfo.getMajorVersion().equals("6")) {
            return AS6_CLIENT_JARS;
        }

        return CLIENT_JARS;
    }

    private Set<DiscoveredResourceDetails> discoverExternalJBossAsProcesses(ResourceDiscoveryContext discoveryContext) {
        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> autoDiscoveryResults = discoveryContext.getAutoDiscoveredProcesses();

        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            ProcessInfo processInfo = autoDiscoveryResult.getProcessInfo();
            if (log.isDebugEnabled())
                log.debug("Discovered JBoss AS process: " + processInfo);

            JBossInstanceInfo cmdLine;
            try {
                cmdLine = new JBossInstanceInfo(processInfo);
            } catch (Exception e) {
                log.error("Failed to process JBoss AS command line: " + Arrays.asList(processInfo.getCommandLine()), e);
                continue;
            }

            // Skip it if it's an AS/EAP/SOA-P version we don't support.
            JBossInstallationInfo installInfo = cmdLine.getInstallInfo();
            if (!isSupportedProduct(installInfo)) {
                continue;
            }

            File installHome = new File(cmdLine.getSystemProperties().getProperty(JBossProperties.HOME_DIR));
            File configDir = new File(cmdLine.getSystemProperties().getProperty(JBossProperties.SERVER_HOME_DIR));

            // The config dir might be a symlink - call getCanonicalFile() to resolve it if so, before
            // calling isDirectory() (isDirectory() returns false for a symlink, even if it points at
            // a directory).
            try {
                if (!configDir.getCanonicalFile().isDirectory()) {
                    log.warn("Skipping discovery for JBoss AS process " + processInfo + ", because configuration dir '"
                        + configDir + "' does not exist or is not a directory.");
                    continue;
                }
            } catch (IOException e) {
                log.error("Skipping discovery for JBoss AS process " + processInfo + ", because configuration dir '"
                    + configDir + "' could not be canonicalized.", e);
                continue;
            }

            Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();

            String jnpURL = getJnpURL(cmdLine, installHome, configDir);

            // TODO? Set the connection type - local or remote

            // Set the required props...
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.NAMING_URL,
                jnpURL));
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.HOME_DIR,
                installHome.getAbsolutePath()));
            pluginConfiguration.put(new PropertySimple(
                ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR, configDir));

            // Set the optional props...
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.SERVER_NAME,
                cmdLine.getSystemProperties().getProperty(JBossProperties.SERVER_NAME)));
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.BIND_ADDRESS,
                cmdLine.getSystemProperties().getProperty(JBossProperties.BIND_ADDRESS)));

            JBossASDiscoveryUtils.UserInfo userInfo = JBossASDiscoveryUtils.getJmxInvokerUserInfo(configDir);
            if (userInfo != null) {
                pluginConfiguration.put(
                        new PropertySimple(ApplicationServerPluginConfigurationProperties.PRINCIPAL,
                                userInfo.getUsername()));
                pluginConfiguration.put(
                        new PropertySimple(ApplicationServerPluginConfigurationProperties.CREDENTIALS,
                                userInfo.getPassword()));
            }

            String javaHome = processInfo.getEnvironmentVariable(JAVA_HOME_ENV_VAR);
            if (javaHome == null && log.isDebugEnabled()) {
                log.warn("Unable to determine the JAVA_HOME environment variable for the JBoss AS process - "
                    + " the Agent is probably running as a user that does not have access to the AS process's "
                    + " environment.");                
            }
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.JAVA_HOME,
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

    @Nullable
    private DiscoveredResourceDetails discoverInProcessJBossAS(ResourceDiscoveryContext discoveryContext) {
        try {
            return new InProcessJBossASDiscovery().discoverInProcessJBossAS(discoveryContext);
        } catch (Throwable t) {
            log.debug("In-process JBoss AS discovery failed - we are probably not running embedded within JBoss AS.", t);
            return null;
        }
    }

    private DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfig, @Nullable ProcessInfo processInfo, JBossInstallationInfo installInfo) {
        String serverHomeDir = pluginConfig.getSimple(
            ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR).getStringValue();
        File absoluteConfigPath = resolvePathRelativeToHomeDir(pluginConfig, serverHomeDir);

        // Canonicalize the config path, so it's consistent no matter how it's entered.
        // This prevents two servers with different forms of the same config path, but
        // that are actually the same server, from ending up in inventory.
        // JON: fix for JBNADM-2634 - do not resolve symlinks (ips, 12/18/07)
        String key = FileUtils.getCanonicalPath(absoluteConfigPath.getPath());

        String bindAddress = pluginConfig.getSimple(
            ApplicationServerPluginConfigurationProperties.BIND_ADDRESS).getStringValue();
        String namingUrl = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.NAMING_URL)
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

        String description = installInfo.getProductType().DESCRIPTION;
        File deployDir = new File(absoluteConfigPath, "deploy");
        
        File rhqInstallerWar = new File(deployDir, "rhq-installer.war");
        File rhqInstallerWarUndeployed = new File(deployDir, "rhq-installer.war.rej");
        boolean isRhqServer = rhqInstallerWar.exists() || rhqInstallerWarUndeployed.exists();
        if (isRhqServer) {
            description += " hosting the RHQ Server";
            // We know this is an RHQ Server. Let's add an event source for its server log file, but disable it by default.
            configureEventSourceForServerLogFile(pluginConfig);
        }
        String name = formatServerName(bindAddress, namingPort, discoveryContext.getSystemInformation().getHostname(), absoluteConfigPath.getName(), isRhqServer);

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, installInfo.getVersion(),
            description, pluginConfig, processInfo);
    }

    public String formatServerName(String bindingAddress, String jnpPort, String hostname, String configurationName, boolean isRhq) {

        if (isRhq) {
            return hostname + " RHQ Server";
        } else {
            String hostnameToUse = hostname;

            if (bindingAddress != null) {
                try {
                    InetAddress bindAddr = InetAddress.getByName(bindingAddress);
                    if (!bindAddr.isAnyLocalAddress()) {
                        //if the binding address != 0.0.0.0
                        hostnameToUse = bindAddr.getHostName();
                    }
                } catch (UnknownHostException e) {
                    //this should not happen?
                    log.warn("Unknown hostname passed in as the binding address for JBoss AS server discovery: "
                        + bindingAddress);
                }
            }

            if (jnpPort != null && !jnpPort.equals(CHANGE_ME)) {
                hostnameToUse += ":" + jnpPort;
            }

            return hostnameToUse + " " + configurationName;
        }
    }
   
    private void configureEventSourceForServerLogFile(Configuration pluginConfig) {
        File rhqLogFile = resolvePathRelativeToHomeDir(pluginConfig, "../logs/rhq-server-log4j.log");
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
                PropertyList logEventSources = pluginConfig
                    .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);
                logEventSources.add(serverLogEventSource);
            } catch (IOException e) {
                log.warn("Unable to setup RHQ Server log file monitoring.", e);
            }
        }
    }

    private String getJnpURL(JBossInstanceInfo cmdLine, File installHome, File configDir) {
        File jnpServiceUrlFile = new File(configDir, "data/jnp-service.url");
        if (jnpServiceUrlFile.exists() && jnpServiceUrlFile.canRead()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(jnpServiceUrlFile));
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

        log.warn("Failed to read JNP URL from '" + jnpServiceUrlFile + "'.");

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

    private boolean isSupportedProduct(JBossInstallationInfo installInfo) {
        ComparableVersion version = new ComparableVersion(installInfo.getVersion());
        JBossProductType productType = installInfo.getProductType();
        ComparableVersion minimumVersion = MINIMUM_PRODUCT_VERSIONS.get(productType);
        // The product is supported if the version is greater than or equal to the minimum version.
        boolean supported = (version.compareTo(minimumVersion) >= 0);
        if (!supported) {
            log.debug(productType + " version " + version + " is not supported by this plugin (minimum " + productType
                    + " version is " + minimumVersion + ") - skipping...");
        }
        return supported;
    }

    @NotNull
    private static File resolvePathRelativeToHomeDir(Configuration pluginConfig, @NotNull String path) {
        File configDir = new File(path);
        if (!configDir.isAbsolute()) {
            String homeDir = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.HOME_DIR).getStringValue();
            configDir = new File(homeDir, path);
        }
        return configDir;
    }
}
