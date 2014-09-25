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

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.core.util.StringUtil.arrayToString;
import static org.rhq.core.util.StringUtil.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.domain.util.OSGiVersion;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.core.pluginapi.util.CommandLineOption;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.pluginapi.util.JavaCommandLine;
import org.rhq.core.pluginapi.util.ServerStartScriptDiscoveryUtility;
import org.rhq.core.pluginapi.util.StartScriptConfiguration;
import org.rhq.core.system.ProcessInfo;
import org.rhq.modules.plugins.jbossas7.helper.HostConfiguration;
import org.rhq.modules.plugins.jbossas7.helper.HostPort;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Abstract base discovery component for the two server types - "JBossAS7 Host Controller" and
 * "JBossAS7 Standalone Server".
 */
public abstract class BaseProcessDiscovery implements ResourceDiscoveryComponent, ManualAddFacet, ResourceUpgradeFacet {
    private static final Log LOG = LogFactory.getLog(BaseProcessDiscovery.class);

    private static final String JBOSS_AS_PREFIX = "jboss-as-";
    private static final String JBOSS_EAP_PREFIX = "jboss-eap-";
    private static final String WILDFLY_PREFIX = "wildfly-";

    private static final String LOCAL_RESOURCE_KEY_PREFIX = "hostConfig: ";
    private static final String REMOTE_RESOURCE_KEY_PREFIX = "hostPort: ";

    private static final String HOME_DIR_SYSPROP = "jboss.home.dir";

    private static final String RHQADMIN = "rhqadmin";
    private static final String RHQADMIN_ENCRYPTED = "35c160c1f841a889d4cda53f0bfc94b6";

    private static final boolean OS_IS_WINDOWS = (File.separatorChar == '\\');

    // The list of environment vars that the AS7 start script will use if they are set.
    private static final Set<String> START_SCRIPT_ENV_VAR_NAMES = new LinkedHashSet<String>();
    static {
        START_SCRIPT_ENV_VAR_NAMES.addAll(Arrays.asList( //
            "RUN_CONF", //
            "STANDALONE_CONF", //
            "DOMAIN_CONF", //
            "MAX_FD", //
            "PROFILER", //
            "JAVA_HOME", //
            "JAVA", //
            "PRESERVE_JAVA_OPTS", //
            "PROCESS_CONTROLLER_JAVA_OPTS", //
            "HOST_CONTROLLER_JAVA_OPTS", //
            "JAVAC_JAR", //
            "JBOSS_HOME", //
            "JBOSS_MODULES_SYSTEM_PKGS", //
            "JBOSS_MODULEPATH", //
            "JBOSS_BASE_DIR", //
            "JBOSS_LOG_DIR", //
            "JBOSS_CONFIG_DIR", //
            "JBOSS_PIDFILE", //
            "LAUNCH_JBOSS_IN_BACKGROUND" //
        ));

        // If OS is Windows, add env vars that are only used by the batch files.
        if (OS_IS_WINDOWS) {
            START_SCRIPT_ENV_VAR_NAMES.add("ECHO");
            START_SCRIPT_ENV_VAR_NAMES.add("NOPAUSE");
        }
    }

    // e.g.: -mp /opt/jboss-as-7.1.1.Final/modules
    //       --pc-address 127.0.0.1
    //       --pc-port 52624
    //       -default-jvm /usr/java/jdk1.6.0_30/jre/bin/java
    //       -Djboss.home.dir=/opt/jboss-as-7.1.1.Final
    private static final Set<CommandLineOption> START_SCRIPT_OPTION_EXCLUDES = new HashSet<CommandLineOption>();
    static {
        START_SCRIPT_OPTION_EXCLUDES.add(new CommandLineOption("mp", null));
        START_SCRIPT_OPTION_EXCLUDES.add(new CommandLineOption(null, "pc-address"));
        START_SCRIPT_OPTION_EXCLUDES.add(new CommandLineOption(null, "pc-port"));
        START_SCRIPT_OPTION_EXCLUDES.add(new CommandLineOption("default-jvm", null));
        START_SCRIPT_OPTION_EXCLUDES.add(new CommandLineOption("Djboss.home.dir", null));
    }

    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    private static final OSGiVersion OSGI_VERSION_6_2_0 = new OSGiVersion("6.2.0");
    private static final OSGiVersion OSGI_VERSION_7_0_0 = new OSGiVersion("7.0.0");
    private static final OSGiVersion OSGI_VERSION_6_3_0 = new OSGiVersion("6.3.0");

    // Auto-discover running AS7 instances.
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        List<ProcessScanResult> processScanResults = discoveryContext.getAutoDiscoveredProcesses();
        for (ProcessScanResult processScanResult : processScanResults) {
            try {
                ProcessInfo process = processScanResult.getProcessInfo();
                AS7CommandLine commandLine = new AS7CommandLine(process);
                DiscoveredResourceDetails details = buildResourceDetails(discoveryContext, process, commandLine);
                discoveredResources.add(details);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Discovered new " + discoveryContext.getResourceType().getName() + " Resource (key=["
                        + details.getResourceKey() + "], name=[" + details.getResourceName() + "], version=["
                        + details.getResourceVersion() + "]).");
                }
            } catch (RuntimeException e) {
                // Only barf a stack trace for runtime exceptions.
                LOG.error("Discovery of a " + discoveryContext.getResourceType().getName() + " Resource failed for "
                    + processScanResult.getProcessInfo() + ".", e);
            } catch (Exception e) {
                LOG.error("Discovery of a " + discoveryContext.getResourceType().getName() + " Resource failed for "
                    + processScanResult.getProcessInfo() + " - cause: " + e);
            }
        }

        return discoveredResources;
    }

    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext discoveryContext,
        ProcessInfo process, AS7CommandLine commandLine) throws Exception {
        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        ServerPluginConfiguration serverPluginConfig = new ServerPluginConfiguration(pluginConfig);

        File homeDir = getHomeDir(process, commandLine);
        serverPluginConfig.setHomeDir(homeDir);

        File baseDir = getBaseDir(process, commandLine, homeDir);
        serverPluginConfig.setBaseDir(baseDir);

        File configDir = getConfigDir(process, commandLine, baseDir);
        serverPluginConfig.setConfigDir(configDir);

        File hostXmlFile = getHostXmlFile(commandLine, configDir);
        if (!hostXmlFile.exists()) {
            throw new Exception("Server configuration file not found at the expected location (" + hostXmlFile + ").");
        }

        // This is a "hidden" plugin config prop, i.e. it is intentionally not defined in the plugin descriptor.
        serverPluginConfig.setHostConfigFile(hostXmlFile);

        // This method must be called before getHostConfiguration() can be called.
        HostConfiguration hostConfig = loadHostConfiguration(hostXmlFile);

        String domainHost = findHost(hostXmlFile);
        // this property is DEPRECATED we don't need it during discovery
        pluginConfig.setSimpleValue("domainHost", domainHost);

        File logDir = getLogDir(process, commandLine, baseDir);
        serverPluginConfig.setLogDir(logDir);

        File logFile = getLogFile(logDir);
        initLogEventSourcesConfigProp(logFile.getPath(), pluginConfig);

        HostPort managementHostPort = hostConfig.getManagementHostPort(commandLine, getMode());
        serverPluginConfig.setHostname(managementHostPort.host);
        serverPluginConfig.setPort(managementHostPort.port);
        serverPluginConfig.setSecure(managementHostPort.isSecure);
        HostPort nativeHostPort = hostConfig.getNativeHostPort(commandLine, getMode());
        serverPluginConfig.setNativeHost(nativeHostPort.host);
        serverPluginConfig.setNativePort(nativeHostPort.port);
        pluginConfig.setSimpleValue("realm", hostConfig.getManagementSecurityRealm());
        String apiVersion = hostConfig.getDomainApiVersion();
        JBossProductType productType = JBossProductType.determineJBossProductType(homeDir, apiVersion);
        serverPluginConfig.setProductType(productType);
        pluginConfig.setSimpleValue("expectedRuntimeProductName", productType.PRODUCT_NAME);
        pluginConfig.setSimpleValue("hostXmlFileName", getHostXmlFileName(commandLine));

        ProcessInfo agentProcess = discoveryContext.getSystemInformation().getThisProcess();
        setStartScriptPluginConfigProps(process, commandLine, pluginConfig, agentProcess);
        setUserAndPasswordPluginConfigProps(serverPluginConfig, hostConfig);

        String key = createKeyForLocalResource(serverPluginConfig);
        HostPort hostPort = hostConfig.getDomainControllerHostPort(commandLine);
        String name = buildDefaultResourceName(hostPort, managementHostPort, productType, hostConfig.getHostName());
        String description = buildDefaultResourceDescription(hostPort, productType);
        String version = getVersion(homeDir, productType);

        pluginConfig.setSimpleValue("supportsPatching", Boolean.toString(supportsPatching(productType, version)));

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, description,
            pluginConfig, process);
    }

    protected HostConfiguration loadHostConfiguration(File hostXmlFile) throws Exception {
        try {
            return new HostConfiguration(hostXmlFile);
        } catch (Exception e) {
            throw new Exception("Failed to load host configuration from [" + hostXmlFile + "].", e);
        }
    }

    protected File getHomeDir(ProcessInfo processInfo, JavaCommandLine javaCommandLine) {
        String home = javaCommandLine.getSystemProperties().get(HOME_DIR_SYSPROP);
        File homeDir = new File(home);
        if (!homeDir.isAbsolute()) {
            if (processInfo.priorSnaphot().getExecutable() == null) {
                throw new RuntimeException(HOME_DIR_SYSPROP + " for AS7 process " + processInfo
                    + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
            }
            String cwd = processInfo.priorSnaphot().getExecutable().getCwd();
            homeDir = new File(cwd, home);
        }

        return new File(FileUtils.getCanonicalPath(homeDir.getPath()));
    }

    private void setStartScriptPluginConfigProps(ProcessInfo process, AS7CommandLine commandLine,
        Configuration pluginConfig, ProcessInfo agentProcess) {
        StartScriptConfiguration startScriptConfig = new StartScriptConfiguration(pluginConfig);
        ProcessInfo parentProcess = getPotentialStartScriptProcess(process);

        File startScript = ServerStartScriptDiscoveryUtility.getStartScript(parentProcess);
        if (startScript == null) {
            // The parent process is not a script - fallback to the default value (e.g. "bin/standalone.sh").
            String startScriptFileName = getMode().getStartScriptFileName();
            startScript = new File("bin", startScriptFileName);
        }
        if (!startScript.exists()) {
            if (!startScript.isAbsolute()) {
                File homeDir = new File(pluginConfig.getSimpleValue("homeDir"));
                File startScriptAbsolute = new File(homeDir, startScript.getPath());
                if (!startScriptAbsolute.exists()) {
                    LOG.warn("Failed to find start script file for AS7 server with command line [" + commandLine
                        + "] - defaulting 'startScripFile' plugin config prop to [" + startScript + "].");
                }
            }
        }
        startScriptConfig.setStartScript(startScript);

        String startScriptPrefix = ServerStartScriptDiscoveryUtility.getStartScriptPrefix(process, agentProcess);
        startScriptConfig.setStartScriptPrefix(startScriptPrefix);

        Map<String, String> startScriptEnv = ServerStartScriptDiscoveryUtility.getStartScriptEnv(process,
            parentProcess, START_SCRIPT_ENV_VAR_NAMES);
        startScriptConfig.setStartScriptEnv(startScriptEnv);

        List<String> startScriptArgs = ServerStartScriptDiscoveryUtility.getStartScriptArgs(parentProcess,
            commandLine.getAppServerArguments(), START_SCRIPT_OPTION_EXCLUDES);
        startScriptConfig.setStartScriptArgs(startScriptArgs);
    }

    protected abstract ProcessInfo getPotentialStartScriptProcess(ProcessInfo process);

    private void setUserAndPasswordPluginConfigProps(ServerPluginConfiguration serverPluginConfig,
        HostConfiguration hostConfig) {
        Properties mgmtUsers = getManagementUsers(hostConfig, serverPluginConfig);
        String user;
        String password;
        if (!mgmtUsers.isEmpty()) {
            if (mgmtUsers.containsKey(RHQADMIN)) {
                user = RHQADMIN;
                String encryptedPassword = mgmtUsers.getProperty(user);
                if (RHQADMIN_ENCRYPTED.equals(encryptedPassword)) {
                    // If the password is "rhqadmin" encrypted, set the "password" prop to "rhqadmin".
                    password = RHQADMIN;
                } else {
                    password = null;
                }
            } else {
                // No "rhqadmin" user is defined - just grab an arbitrary user.
                user = (String) mgmtUsers.keySet().iterator().next();
                // Note, we don't set the "password" prop, since the password we've read from mgmt-users.properties is
                // encrypted, and we need to return an unencrypted value.
                password = null;
            }
        } else {
            // Either no users are defined, or we failed to read the mgmt-users.properties file - default both user and
            // password to "rhqadmin", so that if the end user runs the "createRhqUser" operation, their conn props will
            // already be ready to go.
            user = RHQADMIN;
            password = RHQADMIN;
        }
        serverPluginConfig.setUser(user);
        serverPluginConfig.setPassword(password);
    }

    protected File getBaseDir(ProcessInfo process, JavaCommandLine javaCommandLine, File homeDir) {
        String baseDirString = javaCommandLine.getSystemProperties().get(getBaseDirSystemPropertyName());
        File baseDir;
        if (baseDirString != null) {
            baseDir = new File(baseDirString);
            if (!baseDir.isAbsolute()) {
                if (process.priorSnaphot().getExecutable() == null) {
                    baseDir = new File(homeDir, baseDirString);
                    if (!baseDir.exists()) {
                        throw new RuntimeException(getBaseDirSystemPropertyName() + " for AS7 process " + process
                            + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
                    }
                } else {
                    String cwd = process.priorSnaphot().getExecutable().getCwd();
                    baseDir = new File(cwd, baseDirString);
                    if (!baseDir.exists()) {
                        baseDir = new File(homeDir, baseDirString);
                    }
                }
            }
            baseDir = new File(FileUtils.getCanonicalPath(baseDir.getPath()));
        } else {
            baseDir = new File(homeDir, getDefaultBaseDirName());
        }
        return baseDir;
    }

    protected File getConfigDir(ProcessInfo process, JavaCommandLine javaCommandLine, File baseDir) {
        String configDirString = javaCommandLine.getSystemProperties().get(getConfigDirSystemPropertyName());
        File configDir;
        if (configDirString != null) {
            configDir = new File(configDirString);
            if (!configDir.isAbsolute()) {
                if (process.priorSnaphot().getExecutable() == null) {
                    throw new RuntimeException(getConfigDirSystemPropertyName() + " for AS7 process " + process
                        + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
                }
                String cwd = process.priorSnaphot().getExecutable().getCwd();
                configDir = new File(cwd, configDirString);
            }
            configDir = new File(FileUtils.getCanonicalPath(configDir.getPath()));
        } else {
            configDir = new File(baseDir, getDefaultConfigDirName());
        }
        return configDir;
    }

    protected File getLogDir(ProcessInfo process, AS7CommandLine commandLine, File baseDir) {
        String logDirString = commandLine.getSystemProperties().get(getLogDirSystemPropertyName());
        File logDir;
        if (logDirString != null) {
            logDir = new File(logDirString);
            if (!logDir.isAbsolute()) {
                if (process.priorSnaphot().getExecutable() == null) {
                    throw new RuntimeException(getLogDirSystemPropertyName() + " for AS7 process " + process
                        + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
                }
                String cwd = process.priorSnaphot().getExecutable().getCwd();
                logDir = new File(cwd, logDirString);
            }
            logDir = new File(FileUtils.getCanonicalPath(logDir.getPath()));
        } else {
            logDir = new File(baseDir, getDefaultLogDirName());
        }
        return logDir;
    }

    // Returns the name of the host config xml file (domain controller) or server config xml file (standalone server),
    // e.g. "standalone.xml" or "host.xml".
    protected String getHostXmlFileName(AS7CommandLine commandLine) {
        AS7Mode mode = getMode();
        String optionValue = commandLine.getClassOption(mode.getHostConfigFileNameOption());
        if (optionValue == null) {
            optionValue = commandLine.getSystemProperties().get(mode.getDefaultHostConfigSystemPropertyName());
        }
        return (optionValue != null) ? optionValue : mode.getDefaultHostConfigFileName();
    }

    // Returns the host config xml file (domain controller) or server config xml file (standalone server).
    protected File getHostXmlFile(AS7CommandLine commandLine, File configDir) {
        return new File(configDir, getHostXmlFileName(commandLine));
    }

    protected String getDefaultConfigDirName() {
        return "configuration";
    }

    protected String getDefaultLogDirName() {
        return "log";
    }

    protected abstract AS7Mode getMode();

    protected File getLogFile(File logDir) {
        return new File(logDir, getLogFileName());
    }

    protected abstract String getBaseDirSystemPropertyName();

    protected abstract String getConfigDirSystemPropertyName();

    protected abstract String getLogDirSystemPropertyName();

    protected abstract String getDefaultBaseDirName();

    protected abstract String getLogFileName();

    protected abstract String buildDefaultResourceName(HostPort hostPort, HostPort managementHostPort,
        JBossProductType productType, String serverName);

    protected abstract String buildDefaultResourceDescription(HostPort hostPort, JBossProductType productType);

    /**
     * Deprecated due to changes requiring a server name to build resource name. If no name
     * is provided then the information is omitted from the resource name.
     *
     *  Please see [BZ 1080552] for more details.
     */
    @Deprecated
    protected String buildDefaultResourceName(HostPort hostPort, HostPort managementHostPort,
        JBossProductType productType) {
        return buildDefaultResourceName(hostPort, managementHostPort, productType, null);
    }

    // Manually add a (remote) AS7 instance.
    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig, ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException {
        ServerPluginConfiguration serverPluginConfig = new ServerPluginConfiguration(pluginConfig);

        String hostname = serverPluginConfig.getHostname();
        Integer port = serverPluginConfig.getPort();
        String user = serverPluginConfig.getUser();

        if (hostname == null || port == null) {
            throw new InvalidPluginConfigurationException("Hostname and port must both be set.");
        }

        ProductInfo productInfo = new ProductInfo(ASConnectionParams.createFrom(serverPluginConfig)).getFromRemote();
        JBossProductType productType = productInfo.getProductType();

        if (productType == null) {
            throw new InvalidPluginConfigurationException("Can not connect to [" + hostname + ":" + port
                + "] as user [" + user + "]. Did you provide the correct credentials?");
        }

        HostPort hostPort = new HostPort(false);
        HostPort managementHostPort = new HostPort(false);
        managementHostPort.host = hostname;
        managementHostPort.port = port;
        String key = createKeyForRemoteResource(hostname + ":" + port);
        String name = buildDefaultResourceName(hostPort, managementHostPort, productType, null);
        //FIXME this is inconsistent with how the version looks like when autodiscovered
        String version = productInfo.getProductVersion();
        String description = buildDefaultResourceDescription(hostPort, productType);

        pluginConfig.put(new PropertySimple("manuallyAdded", true));
        pluginConfig.put(new PropertySimple("productType", productType.name()));
        pluginConfig.put(new PropertySimple("supportsPatching", supportsPatching(productType, version)));
        pluginConfig.setSimpleValue("expectedRuntimeProductName", productType.PRODUCT_NAME);

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), key, name, version,
            description, pluginConfig, null);

        return detail;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        ResourceUpgradeReport report = new ResourceUpgradeReport();
        boolean upgraded = false;

        String currentResourceKey = inventoriedResource.getResourceKey();
        Configuration pluginConfiguration = inventoriedResource.getPluginConfiguration();
        ServerPluginConfiguration serverPluginConfiguration = new ServerPluginConfiguration(pluginConfiguration);

        boolean hasLocalResourcePrefix = currentResourceKey.startsWith(LOCAL_RESOURCE_KEY_PREFIX);
        boolean hasRemoteResourcePrefix = currentResourceKey.startsWith(REMOTE_RESOURCE_KEY_PREFIX);
        if (!hasLocalResourcePrefix && !hasRemoteResourcePrefix) {
            // Resource key in wrong format
            upgraded = true;
            if (new File(currentResourceKey).isDirectory()) {
                // Old key format for a local resource (key is base dir)
                report.setNewResourceKey(createKeyForLocalResource(serverPluginConfiguration));
            } else if (currentResourceKey.contains(":")) {
                // Old key format for a remote (manually added) resource (key is base dir)
                report.setNewResourceKey(createKeyForRemoteResource(currentResourceKey));
            } else {
                upgraded = false;
                LOG.warn("Unknown format, cannot upgrade resource key [" + currentResourceKey + "]");
            }
        } else if (hasLocalResourcePrefix) {
            String configFilePath = currentResourceKey.substring(LOCAL_RESOURCE_KEY_PREFIX.length());
            File configFile = new File(configFilePath);
            try {
                String configFileCanonicalPath = configFile.getCanonicalPath();
                if (!configFileCanonicalPath.equals(configFilePath)) {
                    upgraded = true;
                    report.setNewResourceKey(LOCAL_RESOURCE_KEY_PREFIX + configFileCanonicalPath);
                }
            } catch (IOException e) {
                LOG.warn("Unexpected IOException while converting host config file path to its canonical form", e);
            }
        }

        if (pluginConfiguration.getSimpleValue("expectedRuntimeProductName") == null) {
            upgraded = true;
            pluginConfiguration.setSimpleValue("expectedRuntimeProductName",
                serverPluginConfiguration.getProductType().PRODUCT_NAME);
            report.setNewPluginConfiguration(pluginConfiguration);
        }

        String supportsPatching = pluginConfiguration.getSimpleValue("supportsPatching");
        if (supportsPatching == null || supportsPatching.startsWith("__UNINITIALIZED_")) {
            upgraded = true;

            JBossProductType productType = JBossProductType.valueOf(pluginConfiguration.getSimpleValue("productType"));
            pluginConfiguration
                .setSimpleValue("supportsPatching", Boolean.toString(supportsPatching(productType, inventoriedResource.getVersion())));

            report.setNewPluginConfiguration(pluginConfiguration);
        }

        if (upgraded) {
            return report;
        }
        return null;
    }

    private String createKeyForRemoteResource(String hostPort) {
        return REMOTE_RESOURCE_KEY_PREFIX + hostPort;
    }

    private String createKeyForLocalResource(ServerPluginConfiguration serverPluginConfiguration) {
        // Canonicalize the config path, so it's consistent no matter how it's entered.
        // This prevents two servers with different forms of the same config path, but
        // that are actually the same server, from ending up in inventory.
        try {
            return LOCAL_RESOURCE_KEY_PREFIX + serverPluginConfiguration.getHostConfigFile().getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(
                "Unexpected IOException while converting host config file path to its canonical form", e);
        }
    }

    private <T> T getServerAttribute(ASConnection connection, String attributeName) {
        Operation op = new ReadAttribute(null, attributeName);
        Result res = connection.execute(op);
        if (!res.isSuccess()) {
            throw new InvalidPluginConfigurationException("Could not connect to remote server ["
                + res.getFailureDescription() + "]. Did you enable management?");
        }
        @SuppressWarnings("unchecked")
        T result = (T) res.getResult();
        return result;
    }

    // never returns null
    private Properties getManagementUsers(HostConfiguration hostConfig, ServerPluginConfiguration pluginConfig) {
        String realm = hostConfig.getManagementSecurityRealm();
        File mgmUsersPropsFile = hostConfig.getSecurityPropertyFile(pluginConfig, realm);

        Properties props = new Properties();

        if (!mgmUsersPropsFile.exists()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Management user properties file not found at [" + mgmUsersPropsFile + "].");
            }
            return props;
        }

        if (!mgmUsersPropsFile.canRead()) {
            // BZ 1118061 Log only at debug because JBoss EAP's users, groups, and roles configuration files
            // are read/write by owner only. Meaning that the jbossadmin user owns these files and the
            // jbossonadmin user may not have permission to read these files.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Management user properties at [" + mgmUsersPropsFile + "] is not readable.");
            }
            return props;
        }

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(mgmUsersPropsFile);
        } catch (FileNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Management user properties file not found at [" + mgmUsersPropsFile + "].");
            }
            return props;
        }

        try {
            props.load(inputStream);
        } catch (IOException e) {
            LOG.error("Failed to parse management users properties file at [" + mgmUsersPropsFile + "]: "
                + e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOG.error("Failed to close management users properties file at [" + mgmUsersPropsFile + "]: "
                    + e.getMessage());
            }
        }

        return props;
    }

    /**
     * reads <host name= attribute from host.xml file
     * @param hostXmlFile file
     * @return name attribute from host.xml
     * @Deprecated as of RHQ 4.12. domainHost pluginConfig property is deprecated as well, so this method will no longer be needed.
     */
    private String findHost(File hostXmlFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String hostName = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            try {
                Document document = builder.parse(is); // TODO keep this around
                hostName = document.getDocumentElement().getAttribute("name");
            } finally {
                is.close();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        if (hostName == null)
            hostName = "local"; // Fallback to the installation default
        return hostName;
    }

    private void initLogEventSourcesConfigProp(String fileName, Configuration pluginConfiguration) {
        PropertyList logEventSources = pluginConfiguration
            .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);

        if (logEventSources == null)
            return;

        File serverLogFile = new File(fileName);

        if (serverLogFile.exists() && !serverLogFile.isDirectory()) {
            PropertyMap serverLogEventSource = new PropertyMap("logEventSource");
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.LOG_FILE_PATH, serverLogFile));
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.ENABLED, Boolean.FALSE));
            logEventSources.add(serverLogEventSource);
        }
    }

    private String getVersion(File homeDir, JBossProductType productType) {
        // Products should have a version.txt file at root dir
        File versionFile = new File(homeDir, "version.txt");
        String version = getProductVersionInFile(versionFile, " - Version ", productType);
        if (version == null && productType != JBossProductType.AS && productType != JBossProductType.WILDFLY8) {
            // No version.txt file. Try modules/system/layers/base/org/jboss/as/product/slot/dir/META-INF/MANIFEST.MF
            String layeredProductManifestFilePath = arrayToString(
                new String[] { "modules", "system", "layers", "base", "org", "jboss", "as", "product",
                    productType.SHORT_NAME.toLowerCase(), "dir", "META-INF", "MANIFEST.MF" }, File.separatorChar);
            File productManifest = new File(homeDir, layeredProductManifestFilePath);
            version = getProductVersionInFile(productManifest, "JBoss-Product-Release-Version: ", productType);
            if (version == null) {
                // Try modules/org/jboss/as/product/slot/dir/META-INF/MANIFEST.MF
                String productManifestFilePath = arrayToString(new String[] { "modules", "org", "jboss", "as",
                    "product", productType.SHORT_NAME.toLowerCase(), "dir", "META-INF", "MANIFEST.MF" },
                    File.separatorChar);
                productManifest = new File(homeDir, productManifestFilePath);
                version = getProductVersionInFile(productManifest, "JBoss-Product-Release-Version: ", productType);
            }
        }
        if (version == null) {
            // Fallback
            version = determineServerVersionFromHomeDir(homeDir);
        }
        return version;
    }

    private String getProductVersionInFile(File file, String versionPrefix, JBossProductType productType) {
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        try {
            String versionLine = FileUtils.findString(file.getAbsolutePath(), versionPrefix);
            if (isNotBlank(versionLine)) {
                return productType.SHORT_NAME + " "
                    + versionLine.substring(versionLine.lastIndexOf(versionPrefix) + versionPrefix.length());
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not read file " + file.getAbsolutePath(), e);
            }
        }
        return null;
    }

    protected String determineServerVersionFromHomeDir(File homeDir) {
        String version;
        String homeDirName = homeDir.getName();
        if (homeDirName.startsWith(WILDFLY_PREFIX)) {
            version = homeDirName.substring(WILDFLY_PREFIX.length());
        } else if (homeDirName.startsWith(JBOSS_AS_PREFIX)) {
            version = homeDirName.substring(JBOSS_AS_PREFIX.length());
        } else if (homeDirName.startsWith(JBOSS_EAP_PREFIX)) {
            version = homeDirName.substring(JBOSS_EAP_PREFIX.length());
        } else if (homeDirName.indexOf('-') >= 0) {
            version = homeDirName.substring(homeDirName.lastIndexOf('-') + 1);
        } else {
            version = "";
        }
        return version;
    }

    private boolean supportsPatching(JBossProductType productType, String version) {
        if (version.startsWith(productType.SHORT_NAME)) {
            //version of the resource is SHORT_NAME space VERSION
            version = version.substring(productType.SHORT_NAME.length() + 1);
        }
        // EAP 6.1.0 version.txt file content is: "JBoss Enterprise Application Platform - Version 6.0.1 GA"
        // As a consequence, the version detected will be "6.0.1 GA" (notice the space instead of a dot)
        // Give such version strings a chance to make a valid OSGiVersion instance (avoid IllegalArgumentException)
        version = SPACE_PATTERN.matcher(version).replaceAll(".");
        OSGiVersion osgiVersion;
        try {
            osgiVersion = new OSGiVersion(version);
        } catch (IllegalArgumentException e) {
            // If the version is still not matching the expected pattern, default to false
            if (LOG.isDebugEnabled()) {
                LOG.debug("Defaulting to supportsPatching = false", e);
            }
            return false;
        }

        switch (productType) {
        case AS:
            return false;
        case EAP:
            return OSGI_VERSION_6_2_0.compareTo(osgiVersion) <= 0;
        case WILDFLY8:
            return true;
        case JPP:
            return false; //as of now
        case SOA:
            return false; //as of now
        case ISPN:
            return OSGI_VERSION_7_0_0.compareTo(osgiVersion) <= 0;
        case JDG:
            return OSGI_VERSION_6_3_0.compareTo(osgiVersion) <= 0;
        }

        //let's default to true for the other cases. Most servers support this and additionally plugins
        //can override this in the plugin config using DiscoveryCallback or ResourceUpgradeCallback.
        return true;
    }

    private class ProductInfo {
        private ASConnectionParams asConnectionParams;
        private String productVersion;
        private JBossProductType productType;
        private String releaseVersion;
        private String releaseCodeName;
        private boolean fromRemote = false;

        public ProductInfo(ASConnectionParams asConnectionParams) {
            this.asConnectionParams = asConnectionParams;
        }

        public String getProductVersion() {
            return productVersion;
        }

        public JBossProductType getProductType() {
            return productType;
        }

        public ProductInfo getFromRemote() {
            ASConnection connection = new ASConnection(asConnectionParams);
            try {
                String productName = getServerAttribute(connection, "product-name");
                if ((productName != null) && !productName.isEmpty())
                    productType = JBossProductType.getValueByProductName(productName);
                else {
                    Integer apiVersion = getServerAttribute(connection, "management-major-version");
                    if (apiVersion == 1) {
                        productType = JBossProductType.AS;
                    } else {
                        // In the future also check for other versions of WildFly via the release-version
                        productType = JBossProductType.WILDFLY8;
                    }
                }
                releaseVersion = getServerAttribute(connection, "release-version");
                releaseCodeName = getServerAttribute(connection, "release-codename");
                productVersion = getServerAttribute(connection, "product-version");
                if (productVersion == null) {
                    productVersion = releaseVersion;
                }
                fromRemote = true;
            } catch (InvalidPluginConfigurationException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not get the product info from [" + asConnectionParams.getHost() + ":"
                        + asConnectionParams.getPort() + "] - probably a connection failure");
                }
            } finally {
                connection.shutdown();
            }
            return this;
        }

        @Override
        public String toString() {
            return "ProductInfo{" + "hostname='" + asConnectionParams.getHost() + '\'' + ", port="
                + asConnectionParams.getPort() + ", productVersion='" + productVersion + '\'' + ", productType='"
                + productType + '\'' + ", releaseVersion='" + releaseVersion + '\'' + ", releaseCodeName='"
                + releaseCodeName + '\'' + ", fromRemote=" + fromRemote + '}';
        }
    }

}
