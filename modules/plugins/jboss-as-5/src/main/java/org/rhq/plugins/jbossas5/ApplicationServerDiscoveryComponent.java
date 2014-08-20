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

package org.rhq.plugins.jbossas5;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.on.common.jbossas.JBossASDiscoveryUtils;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.core.pluginapi.util.CommandLineOption;
import org.rhq.core.pluginapi.util.JavaCommandLine;
import org.rhq.core.pluginapi.util.ServerStartScriptDiscoveryUtility;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.jbossas5.helper.JBossInstallationInfo;
import org.rhq.plugins.jbossas5.helper.JBossInstanceInfo;
import org.rhq.plugins.jbossas5.helper.JBossProductType;
import org.rhq.plugins.jbossas5.helper.JBossProperties;
import org.rhq.plugins.jbossas5.util.JnpConfig;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

/**
 * A Resource discovery component for JBoss application server Resources, which include the following:
 *
 *   JBoss AS, 5.2.0.Beta1 and later
 *   JBoss EAP, 5.0.0.Beta and later
 *   JBoss EWP, 5.0.0.CR1 and later
 *   JBoss SOA-P, 5.0.0.Beta and later
 *
 * @author Ian Springer
 * @author Mark Spritzler
 */
@SuppressWarnings({ "UnusedDeclaration" })
public class ApplicationServerDiscoveryComponent implements ResourceDiscoveryComponent, ClassLoaderFacet,
    ManualAddFacet, ResourceUpgradeFacet {

    private static final Log LOG = LogFactory.getLog(ApplicationServerDiscoveryComponent.class);

    private static final String JBOSS_SERVICE_XML = "conf" + File.separator + "jboss-service.xml";
    private static final String JBOSS_NAMING_SERVICE_XML = "deploy" + File.separator + "naming-service.xml";
    private static final String ANY_ADDRESS = "0.0.0.0";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String JAVA_HOME_ENV_VAR = "JAVA_HOME";

    private static final Map<JBossProductType, ComparableVersion> MINIMUM_PRODUCT_VERSIONS = new HashMap<JBossProductType, ComparableVersion>(
        4);
    static {
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.AS, new ComparableVersion("5.2.0.Beta1"));
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.EAP, new ComparableVersion("5.0.0.Beta"));
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.EWP, new ComparableVersion("5.0.0.CR1"));
        MINIMUM_PRODUCT_VERSIONS.put(JBossProductType.SOA, new ComparableVersion("5.0.0.Beta"));
    }

    private static final String[] CLIENT_JAR_URLS = new String[] {
        // NOTE: The jbossall-client.jar aggregates a whole bunch of other jars from the client dir via its
        // MANIFEST.MF Class-Path.
        "%clientUrl%/jbossall-client.jar", //
        "%clientUrl%/trove.jar", //
        "%clientUrl%/javassist.jar", //
        "%commonLibUrl%/jboss-security-aspects.jar", //
        "%libUrl%/jboss-managed.jar", //
        "%libUrl%/jboss-metatype.jar", //
        "%libUrl%/jboss-dependency.jar", //
        "%libUrl%/jboss-reflect.jar", //
        // AS 6.0 M1 and later
        "%libUrl%/jboss-classpool.jar", //
        "%libUrl%/jboss-classpool-scoped.jar", //
        // AS 6.0 M4 and later
        "%commonLibUrl%/jboss-as-profileservice.jar", //
        "%libUrl%/jboss-profileservice-spi.jar" //
    };

    // The set of env vars that are actually relevant to AS5.  Include only these to reduce the amount
    // of noise in the property value and to potentially avoid stale values.
    private static final Set<String> START_SCRIPT_ENV_VAR_NAMES = new LinkedHashSet<String>();

    // Script options that are set by the script itself and that we don't want. Currently none.
    private static final Set<CommandLineOption> START_SCRIPT_OPTION_EXCLUDES = new HashSet<CommandLineOption>();

    static {
        // Note that JAVA_OPTS is not included on purpose.  If present the setting will override that
        // which is set in run.conf or possibly in a custom script.  We opt to let run.conf provide the setting
        // as that is quite likely what users will expect.  JAVA_OPTS can be added manually to the
        // startScriptEnv settings at which time it would provide an override. Not discovering JAVA_OPTS also
        // avoids issues with duplicate settings, due to manipulations made to it by run.conf.
        START_SCRIPT_ENV_VAR_NAMES.addAll(Arrays.asList( //
            "JAVA_HOME", //
            "JAVA", //
            "JAVAC_JAR", //
            "JBOSS_HOME", //
            "JBOSS_BASE_DIR", //
            "JBOSS_LOG_DIR", //
            "JBOSS_CONFIG_DIR", //
            "RUN_CONF", //
            "MAX_FD", //
            "PROFILER" //
        ));

        // If OS is Windows, add env vars that are only used by the batch files.
        if (File.separatorChar == '\\') {
            START_SCRIPT_ENV_VAR_NAMES.add("ECHO");
            START_SCRIPT_ENV_VAR_NAMES.add("NOPAUSE");
        }
    }

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) {
        LOG.trace("Discovering JBoss AS 5.x and 6.x Resources...");
        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        DiscoveredResourceDetails inProcessJBossAS = discoverInProcessJBossAS(discoveryContext);
        if (inProcessJBossAS != null) {
            // If we're running inside a JBoss AS JVM, that's the only AS instance we want to discover.
            resources.add(inProcessJBossAS);
        } else {
            // Otherwise, scan the process table for external AS instances.
            resources.addAll(discoverExternalJBossAsProcesses(discoveryContext));
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Discovered " + resources.size() + " JBossAS 5.x and 6.x Resources.");
        }
        return resources;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException {
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
        setValuesForUnsetClientJarUrlProperties(pluginConfig);

        List<URL> clientJarUrls = new ArrayList<URL>();
        for (String clientJarUrlString : CLIENT_JAR_URLS) {
            // Substitute values in for any templated plugin config props.
            clientJarUrlString = ResourceComponentUtils.replacePropertyExpressionsInTemplate(clientJarUrlString,
                pluginConfig);
            URL clientJarUrl = new URL(clientJarUrlString);
            if (isReadable(clientJarUrl)) {
                clientJarUrls.add(clientJarUrl);
            } else {
                LOG.debug("Client JAR [" + clientJarUrl + "] does not exist or is not readable (note, this JAR "
                    + " may not be required for some app server versions).");
            }
        }

        return clientJarUrls;
    }

    private boolean isReadable(URL url) {
        try {
            InputStream inputStream = url.openStream();
            try {
                inputStream.close();
            } catch (IOException e) {
                LOG.error("Failed to close input stream for URL [" + url + "].", e);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void setValuesForUnsetClientJarUrlProperties(Configuration pluginConfig) throws MalformedURLException {
        String homeDir = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.HOME_DIR)
            .getStringValue();
        URL homeUrl = new File(homeDir).toURI().toURL();

        String clientUrlString = pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.CLIENT_URL,
            null);
        if (clientUrlString == null) {
            URL clientUrl = new URL(homeUrl, "client");
            pluginConfig.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.CLIENT_URL, clientUrl));
        }

        String libUrlString = pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.LIB_URL, null);
        if (libUrlString == null) {
            URL libUrl = new URL(homeUrl, "lib");
            pluginConfig.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.LIB_URL, libUrl));
        }

        String commonLibUrlString = pluginConfig.getSimpleValue(
            ApplicationServerPluginConfigurationProperties.COMMON_LIB_URL, null);
        if (commonLibUrlString == null) {
            URL commonLibUrl = new URL(homeUrl, "common/lib");
            pluginConfig.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.COMMON_LIB_URL,
                commonLibUrl));
        }
    }

    private Set<DiscoveredResourceDetails> discoverExternalJBossAsProcesses(ResourceDiscoveryContext discoveryContext) {
        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> autoDiscoveryResults = discoveryContext.getAutoDiscoveredProcesses();

        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            ProcessInfo processInfo = autoDiscoveryResult.getProcessInfo();
            if (LOG.isDebugEnabled())
                LOG.debug("Discovered JBoss AS process: " + processInfo);

            JBossInstanceInfo jbossInstanceInfo;
            try {
                jbossInstanceInfo = new JBossInstanceInfo(processInfo);
            } catch (Exception e) {
                LOG.error("Failed to process JBoss AS command line: " + Arrays.asList(processInfo.getCommandLine()), e);
                continue;
            }

            // Skip it if it's an AS/EAP/SOA-P version we don't support.
            JBossInstallationInfo installInfo = jbossInstanceInfo.getInstallInfo();
            if (!isSupportedProduct(installInfo)) {
                continue;
            }

            File installHome = new File(jbossInstanceInfo.getSystemProperties().getProperty(JBossProperties.HOME_DIR));
            File configDir = new File(jbossInstanceInfo.getSystemProperties().getProperty(
                JBossProperties.SERVER_HOME_DIR));

            // The config dir might be a symlink - call getCanonicalFile() to resolve it if so, before
            // calling isDirectory() (isDirectory() returns false for a symlink, even if it points at
            // a directory).
            try {
                if (!configDir.getCanonicalFile().isDirectory()) {
                    LOG.warn("Skipping discovery for JBoss AS process " + processInfo + ", because configuration dir '"
                        + configDir + "' does not exist or is not a directory.");
                    continue;
                }
            } catch (IOException e) {
                LOG.error("Skipping discovery for JBoss AS process " + processInfo + ", because configuration dir '"
                    + configDir + "' could not be canonicalized.", e);
                continue;
            }

            Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();

            // TODO? Set the connection type - local or remote

            // Set the required props...
            String jnpURL = getJnpURL(jbossInstanceInfo, installHome, configDir);
            PropertySimple namingUrlProp = new PropertySimple(
                ApplicationServerPluginConfigurationProperties.NAMING_URL, jnpURL);
            if (jnpURL == null) {
                namingUrlProp.setErrorMessage("RHQ failed to discover the naming provider URL.");
            }
            pluginConfiguration.put(namingUrlProp);
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.HOME_DIR,
                installHome.getAbsolutePath()));
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR,
                configDir));

            // Set the optional props...
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.SERVER_NAME,
                jbossInstanceInfo.getSystemProperties().getProperty(JBossProperties.SERVER_NAME)));
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.BIND_ADDRESS,
                jbossInstanceInfo.getSystemProperties().getProperty(JBossProperties.BIND_ADDRESS)));

            JBossASDiscoveryUtils.UserInfo userInfo = JBossASDiscoveryUtils.getJmxInvokerUserInfo(configDir);
            if (userInfo != null) {
                pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.PRINCIPAL,
                    userInfo.getUsername()));
                pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.CREDENTIALS,
                    userInfo.getPassword()));
            }

            String javaHome = processInfo.getEnvironmentVariable(JAVA_HOME_ENV_VAR);
            if (javaHome == null && LOG.isDebugEnabled()) {
                LOG.warn("Unable to determine the JAVA_HOME environment variable for the JBoss AS process - "
                    + " the Agent is probably running as a user that does not have access to the AS process's "
                    + " environment.");
            }
            pluginConfiguration.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.JAVA_HOME,
                javaHome));

            initLogEventSourcesConfigProp(configDir, pluginConfiguration);

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
            LOG.debug("In-process JBoss AS discovery failed - we are probably not running embedded within JBoss AS.", t);
            return null;
        }
    }

    private DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfig, @Nullable ProcessInfo processInfo, JBossInstallationInfo installInfo) {

        String serverHomeDir = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR)
            .getStringValue();
        File absoluteConfigPath = resolvePathRelativeToHomeDir(pluginConfig, serverHomeDir);

        // Canonicalize the config path, so it's consistent no matter how it's entered.
        // This prevents two servers with different forms of the same config path, but
        // that are actually the same server, from ending up in inventory.
        String key;
        try {
            key = absoluteConfigPath.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(
                "Unexpected IOException while converting config file path to its canonical form", e);
        }

        String bindAddress = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.BIND_ADDRESS)
            .getStringValue();
        String namingUrl = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.NAMING_URL)
            .getStringValue();

        // If we were able to discover the JNP URL, include the JNP port in the Resource name.
        String namingPort = null;
        //noinspection ConstantConditions
        if (namingUrl != null) {
            URI uri = URI.create(namingUrl);
            if (uri.getPort() != -1) {
                namingPort = String.valueOf(uri.getPort());
            }
        }

        final JBossProductType productType = installInfo.getProductType();
        String description = productType.DESCRIPTION + " " + installInfo.getMajorVersion();
        File deployDir = new File(absoluteConfigPath, "deploy");

        File rhqInstallerWar = new File(deployDir, "rhq-installer.war");
        File rhqInstallerWarUndeployed = new File(deployDir, "rhq-installer.war.rej");
        boolean isRhqServer = rhqInstallerWar.exists() || rhqInstallerWarUndeployed.exists();
        if (isRhqServer) {
            description += " hosting the RHQ Server";
            // We know this is an RHQ Server. Let's add an event source for its server log file, but disable it by default.
            configureEventSourceForServerLogFile(pluginConfig);
        }
        final String PRODUCT_PREFIX = productType.name() + " ";
        String name = PRODUCT_PREFIX
            + formatServerName(bindAddress, namingPort, discoveryContext.getSystemInformation().getHostname(),
                absoluteConfigPath.getName(), isRhqServer);

        // If we are discovering plugin config via processInfo, then in addition to everything we discover above,
        // we'll now see if we can determine more robust startup environment information to be used for start/restart
        // operations if possible. If this information is not discovered, and left unset by the user or a remote client
        // update, then  the start script will fall back to the minimal information captured above.
        if (processInfo != null) {
            setStartScriptPluginConfigProps(processInfo, new JavaCommandLine(processInfo.getCommandLine()),
                pluginConfig);
        }

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, PRODUCT_PREFIX
            + installInfo.getVersion(), description, pluginConfig, processInfo);
    }

    private void setStartScriptPluginConfigProps(ProcessInfo process, JavaCommandLine commandLine,
        Configuration pluginConfig) {
        StartScriptConfiguration startScriptConfig = new StartScriptConfiguration(pluginConfig);
        ProcessInfo parentProcess = process.getParentProcess();

        File startScript = ServerStartScriptDiscoveryUtility.getStartScript(parentProcess);
        // if we don't discover the start script then leave it empty and the operation code
        // will determine the OS-specific default when needed.
        if (startScript != null) {
            boolean exists = startScript.exists();

            if (!exists && !startScript.isAbsolute()) {
                File homeDir = new File(
                    pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.HOME_DIR));
                File startScriptAbsolute = new File(homeDir, startScript.getPath());
                exists = startScriptAbsolute.exists();
            }
            if (!exists) {
                LOG.warn("Discovered startScriptFile ["
                    + startScript
                    + "] but failed to find it on disk. The start script may not be correct. The command line used for discovery is ["
                    + commandLine + "]");

            }
            startScriptConfig.setStartScript(startScript);
        }

        Map<String, String> startScriptEnv = ServerStartScriptDiscoveryUtility.getStartScriptEnv(process,
            parentProcess, START_SCRIPT_ENV_VAR_NAMES);
        startScriptConfig.setStartScriptEnv(startScriptEnv);

        List<String> startScriptArgs = ServerStartScriptDiscoveryUtility.getStartScriptArgs(parentProcess,
            commandLine.getClassArguments(), START_SCRIPT_OPTION_EXCLUDES);
        startScriptConfig.setStartScriptArgs(startScriptArgs);
    }

    public String formatServerName(String bindingAddress, String jnpPort, String hostname, String configurationName,
        boolean isRhq) {

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
                    LOG.warn("Unknown hostname passed in as the binding address for JBoss AS server discovery: "
                        + bindingAddress);
                }
            }

            if (jnpPort != null) {
                hostnameToUse += ":" + jnpPort;
            }

            return hostnameToUse + " " + configurationName;
        }
    }

    private void configureEventSourceForServerLogFile(Configuration pluginConfig) {
        File rhqLogFile = resolvePathRelativeToHomeDir(pluginConfig, "../logs/rhq-server-log4j.log");
        if (rhqLogFile.exists() && !rhqLogFile.isDirectory()) {
            try {
                PropertyMap serverLogEventSource = new PropertyMap("logEventSource");
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
                LOG.warn("Unable to setup RHQ Server log file monitoring.", e);
            }
        }
    }

    private String getJnpURL(JBossInstanceInfo cmdLine, File installHome, File configDir) {
        ArrayList<File> possibleJnpServiceUrlFiles = new ArrayList<File>(2);
        possibleJnpServiceUrlFiles.add(new File(configDir, "data/jnp-service.url"));
        // if the app server was told to go somewhere else to store its data files, look in there too (BZ 699893)
        if (cmdLine.getSystemProperties() != null) {
            String dataDir = cmdLine.getSystemProperties().getProperty("jboss.server.data.dir");
            if (dataDir != null) {
                possibleJnpServiceUrlFiles.add(new File(dataDir, "jnp-service.url"));
            }
        }
        for (File jnpServiceUrlFile : possibleJnpServiceUrlFiles) {
            if (jnpServiceUrlFile.exists() && jnpServiceUrlFile.canRead()) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(jnpServiceUrlFile));
                    String jnpUrl = br.readLine();
                    if (jnpUrl != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Read JNP URL from jnp-service.url file: " + jnpUrl);
                        }
                        return jnpUrl;
                    }
                } catch (IOException ioe) {
                    // Nothing to do
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            // nada
                        }
                    }
                }
            }
        }

        LOG.warn("Failed to read JNP URL from: " + possibleJnpServiceUrlFiles);

        // Above did not work, so fall back to our previous scheme
        JnpConfig jnpConfig = getJnpConfig(installHome, configDir, cmdLine.getSystemProperties());

        String jnpAddress = (jnpConfig.getJnpAddress() != null) ? jnpConfig.getJnpAddress() : null;
        Integer jnpPort = (jnpConfig.getJnpPort() != null) ? jnpConfig.getJnpPort() : null;

        if (jnpAddress == null || jnpPort == null) {
            LOG.warn("Failed to discover JNP URL for JBoss instance with configuration directory [" + configDir + "].");
            return null;
        }

        if (ANY_ADDRESS.equals(jnpAddress)) {
            jnpAddress = LOCALHOST;
        }

        return "jnp://" + jnpAddress + ":" + jnpPort;
    }

    private static JnpConfig getJnpConfig(File installHome, File configDir, Properties props) {
        File serviceXML = new File(configDir, JBOSS_SERVICE_XML);
        JnpConfig config = JnpConfig.getConfig(serviceXML, props);
        if ((config == null) || (config.getJnpPort() == null)) {
            File namingServiceFile = new File(configDir, JBOSS_NAMING_SERVICE_XML);
            if (namingServiceFile.exists()) {
                config = JnpConfig.getConfig(namingServiceFile, props);
            }
        }
        return config;
    }

    private void initLogEventSourcesConfigProp(File configDir, Configuration pluginConfig) {
        File logDir = new File(configDir, "log");
        File serverLogFile = new File(logDir, "server.log");
        if (serverLogFile.exists() && !serverLogFile.isDirectory()) {
            PropertyMap serverLogEventSource = new PropertyMap("logEventSource");
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
        boolean supported;
        if (minimumVersion != null) {
            // The product is supported if the version is greater than or equal to the minimum version.
            supported = (version.compareTo(minimumVersion) >= 0);
            if (!supported) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(productType + " version " + version + " is not supported by this plugin (minimum "
                        + productType + " version is " + minimumVersion + ") - skipping...");
                }
            }
        } else {
            supported = true;
        }
        return supported;
    }

    @NotNull
    private static File resolvePathRelativeToHomeDir(Configuration pluginConfig, @NotNull String path) {
        File configDir = new File(path);
        if (!FileUtil.isAbsolutePath(path)) {
            String homeDir = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.HOME_DIR)
                .getStringValue();
            configDir = new File(homeDir, path);
        }

        // BZ 903402 - get the real absolute path - under most conditions, it's the same thing, but if on windows
        //             the drive letter might not have been specified - this makes sure the drive letter is specified.
        return configDir.getAbsoluteFile();
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        ResourceUpgradeReport report = new ResourceUpgradeReport();
        boolean upgraded = false;

        String configDirPath = inventoriedResource.getResourceKey();
        File configDir = new File(configDirPath);
        try {
            String configDirCanonicalPath = configDir.getCanonicalPath();
            if (!configDirCanonicalPath.equals(configDirPath)) {
                upgraded = true;
                report.setNewResourceKey(configDirCanonicalPath);
            }
        } catch (IOException e) {
            LOG.warn("Unexpected IOException while converting host config file path to its canonical form", e);
        }

        if (upgraded) {
            return report;
        }
        return null;
    }
}
