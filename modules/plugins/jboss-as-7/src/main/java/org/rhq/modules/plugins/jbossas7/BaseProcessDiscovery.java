/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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

    private final Log log = LogFactory.getLog(this.getClass());

    // Auto-discover running AS7 instances.
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        List<ProcessScanResult> processScanResults = discoveryContext.getAutoDiscoveredProcesses();
        for (ProcessScanResult processScanResult : processScanResults) {
            try {
                ProcessInfo process = processScanResult.getProcessInfo();
                AS7CommandLine commandLine = new AS7CommandLine(process);
                DiscoveredResourceDetails details = buildResourceDetails(discoveryContext, process, commandLine);
                discoveredResources.add(details);
                log.debug("Discovered new " + discoveryContext.getResourceType().getName() + " Resource (key=["
                        + details.getResourceKey() + "], name=[" + details.getResourceName() + "], version=["
                        + details.getResourceVersion() + "]).");
            } catch (RuntimeException e) {
                // Only barf a stack trace for runtime exceptions.
                log.error("Discovery of a " + discoveryContext.getResourceType().getName()
                        + " Resource failed for " + processScanResult.getProcessInfo() + ".", e);
            } catch (Exception e) {
                log.error("Discovery of a " + discoveryContext.getResourceType().getName()
                        + " Resource failed for " + processScanResult.getProcessInfo() + " - cause: " + e);
            }
        }

        return discoveredResources;
    }

    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext discoveryContext,
                                                             ProcessInfo process, AS7CommandLine commandLine)
            throws Exception {
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
        pluginConfig.setSimpleValue("domainHost", domainHost);

        File logDir = getLogDir(process, commandLine, baseDir);
        serverPluginConfig.setLogDir(logDir);

        File logFile = getLogFile(logDir);
        initLogEventSourcesConfigProp(logFile.getPath(), pluginConfig);

        HostPort managementHostPort = hostConfig.getManagementHostPort(commandLine, getMode());
        serverPluginConfig.setHostname(managementHostPort.host);
        serverPluginConfig.setPort(managementHostPort.port);
        pluginConfig.setSimpleValue("realm", hostConfig.getManagementSecurityRealm());
        String apiVersion = hostConfig.getDomainApiVersion();
        JBossProductType productType = JBossProductType.determineJBossProductType(homeDir, apiVersion);
        serverPluginConfig.setProductType(productType);
        pluginConfig.setSimpleValue("expectedRuntimeProductName", productType.PRODUCT_NAME);
        pluginConfig.setSimpleValue("hostXmlFileName", getHostXmlFileName(commandLine));

        ProcessInfo agentProcess = discoveryContext.getSystemInformation().getThisProcess();
        setStartScriptPluginConfigProps(process, commandLine, pluginConfig, agentProcess);
        setUserAndPasswordPluginConfigProps(serverPluginConfig, hostConfig, baseDir);

        String key = createKeyForLocalResource(serverPluginConfig);
        HostPort hostPort = hostConfig.getDomainControllerHostPort(commandLine);
        String name = buildDefaultResourceName(hostPort, managementHostPort, productType);
        String description = buildDefaultResourceDescription(hostPort, productType);
        String version = getVersion(homeDir, productType);

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
            if (processInfo.getExecutable() == null) {
                throw new RuntimeException(HOME_DIR_SYSPROP + " for AS7 process " + processInfo
                        + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
            }
            String cwd = processInfo.getExecutable().getCwd();
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
                    log.warn("Failed to find start script file for AS7 server with command line [" + commandLine
                            + "] - defaulting 'startScripFile' plugin config prop to [" + startScript + "].");
                }
            }
        }
        startScriptConfig.setStartScript(startScript);

        String startScriptPrefix = ServerStartScriptDiscoveryUtility.getStartScriptPrefix(process, agentProcess);
        startScriptConfig.setStartScriptPrefix(startScriptPrefix);

        Map<String, String> startScriptEnv = ServerStartScriptDiscoveryUtility.getStartScriptEnv(process, parentProcess,
                START_SCRIPT_ENV_VAR_NAMES);
        startScriptConfig.setStartScriptEnv(startScriptEnv);

        List<String> startScriptArgs = ServerStartScriptDiscoveryUtility.getStartScriptArgs(parentProcess,
                commandLine.getAppServerArguments(), START_SCRIPT_OPTION_EXCLUDES);
        startScriptConfig.setStartScriptArgs(startScriptArgs);
    }

    protected abstract ProcessInfo getPotentialStartScriptProcess(ProcessInfo process);

    private void setUserAndPasswordPluginConfigProps(ServerPluginConfiguration serverPluginConfig, HostConfiguration hostConfig,
                                                     File baseDir) {
        Properties mgmtUsers = getManagementUsers(hostConfig, getMode(), baseDir);
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
                if (process.getExecutable() == null) {
                    baseDir = new File(homeDir, baseDirString);
                    if (!baseDir.exists()) {
                        throw new RuntimeException(getBaseDirSystemPropertyName() + " for AS7 process " + process
                                + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
                    }
                } else {
                    String cwd = process.getExecutable().getCwd();
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
                if (process.getExecutable() == null) {
                    throw new RuntimeException(getConfigDirSystemPropertyName() + " for AS7 process " + process
                            + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
                }
                String cwd = process.getExecutable().getCwd();
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
                if (process.getExecutable() == null) {
                    throw new RuntimeException(getLogDirSystemPropertyName() + " for AS7 process " + process
                            + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
                }
                String cwd = process.getExecutable().getCwd();
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

    protected abstract String buildDefaultResourceName(HostPort hostPort, HostPort managementHostPort, JBossProductType productType);

    protected abstract String buildDefaultResourceDescription(HostPort hostPort, JBossProductType productType);

    // Manually add a (remote) AS7 instance.
    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext context) throws InvalidPluginConfigurationException {
        ServerPluginConfiguration serverPluginConfig = new ServerPluginConfiguration(pluginConfig);

        String hostname = serverPluginConfig.getHostname();
        Integer port = serverPluginConfig.getPort();
        String user = serverPluginConfig.getUser();
        String pass = serverPluginConfig.getPassword();

        if (hostname == null || port == null) {
            throw new InvalidPluginConfigurationException("Hostname and port must both be set.");
        }

        ProductInfo productInfo = new ProductInfo(hostname, user, pass, port).getFromRemote();
        JBossProductType productType = productInfo.getProductType();

        if (productType==null) {
            throw new InvalidPluginConfigurationException("Can not connect to [" + hostname + ":" + port + "] as user [" + user +"]. Did you provide the correct credentials?");
        }

        HostPort hostPort = new HostPort(false);
        HostPort managementHostPort = new HostPort(false);
        managementHostPort.host = hostname;
        managementHostPort.port = port;
        String key = createKeyForRemoteResource(hostname + ":" + port);
        String name = buildDefaultResourceName(hostPort, managementHostPort, productType);
        String version = productInfo.getProductVersion();
        String description = buildDefaultResourceDescription(hostPort, productType);

        pluginConfig.put(new PropertySimple("manuallyAdded", true));
        pluginConfig.put(new PropertySimple("productType",productType.name()));
        pluginConfig.setSimpleValue("expectedRuntimeProductName", productType.PRODUCT_NAME);

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfig, null);

        return detail;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        String currentResourceKey = inventoriedResource.getResourceKey();
        Configuration pluginConfiguration = inventoriedResource.getPluginConfiguration();
        ServerPluginConfiguration serverPluginConfiguration = new ServerPluginConfiguration(pluginConfiguration);

        ResourceUpgradeReport report = new ResourceUpgradeReport();
        Boolean upgraded = FALSE;

        if (!currentResourceKey.startsWith(LOCAL_RESOURCE_KEY_PREFIX)
            && !currentResourceKey.startsWith(REMOTE_RESOURCE_KEY_PREFIX)) {
            // Resource key in wrong format 
            upgraded = TRUE;
            if (new File(currentResourceKey).isDirectory()) {
                // Old key format for a local resource (key is base dir)
                report.setNewResourceKey(createKeyForLocalResource(serverPluginConfiguration));
            } else if (currentResourceKey.contains(":")) {
                // Old key format for a remote (manually added) resource (key is base dir)
                report.setNewResourceKey(createKeyForRemoteResource(currentResourceKey));
            } else {
                upgraded = FALSE;
                log.warn("Unknown format, cannot upgrade resource key [" + currentResourceKey + "]");
            }
        }

        if (pluginConfiguration.getSimpleValue("expectedRuntimeProductName") == null) {
            upgraded = TRUE;
            pluginConfiguration.setSimpleValue("expectedRuntimeProductName",
                serverPluginConfiguration.getProductType().PRODUCT_NAME);
            report.setNewPluginConfiguration(pluginConfiguration);
        }

        if (upgraded == TRUE) {
            return report;
        }
        return null;
    }

    private String createKeyForRemoteResource(String hostPort) {
        return REMOTE_RESOURCE_KEY_PREFIX + hostPort; 
    }

    private String createKeyForLocalResource(ServerPluginConfiguration serverPluginConfiguration) {
        return LOCAL_RESOURCE_KEY_PREFIX
            + serverPluginConfiguration.getHostConfigFile().getAbsolutePath();
    }

    private <T>T getServerAttribute(ASConnection connection, String attributeName) {
        Operation op = new ReadAttribute(null, attributeName);
        Result res = connection.execute(op);
        if (!res.isSuccess()) {
            throw new InvalidPluginConfigurationException("Could not connect to remote server ["
                + res.getFailureDescription() + "]. Did you enable management?");
        }
        return (T) res.getResult();
    }

    // never returns null
    private Properties getManagementUsers(HostConfiguration hostConfig, AS7Mode mode, File baseDir) {
        String realm = hostConfig.getManagementSecurityRealm();
        File mgmUsersPropsFile = hostConfig.getSecurityPropertyFile(baseDir, mode, realm);

        Properties props = new Properties();

        if (!mgmUsersPropsFile.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("Management user properties file not found at [" + mgmUsersPropsFile + "].");
            }
            return props;
        }

        if (!mgmUsersPropsFile.canRead()) {
            log.warn("Management user properties at [" + mgmUsersPropsFile + "] is not readable.");
            return props;
        }

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(mgmUsersPropsFile);
        } catch (FileNotFoundException e) {
            log.debug("Management user properties file not found at [" + mgmUsersPropsFile + "].");
            return props;
        }

        try {
            props.load(inputStream);
        } catch (IOException e) {
            log.error("Failed to parse management users properties file at [" + mgmUsersPropsFile + "]: "
                    + e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("Failed to close management users properties file at [" + mgmUsersPropsFile + "]: "
                        + e.getMessage());
            }
        }

        return props;
    }

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
            log.error(e.getMessage());
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
                return new StringBuilder(productType.SHORT_NAME).append(" ")
                    .append(versionLine.substring(versionLine.lastIndexOf(versionPrefix) + versionPrefix.length()))
                    .toString();
            }
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not read file " + file.getAbsolutePath(), e);
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

    private class ProductInfo {
        private String hostname;
        private String user;
        private String pass;
        private int port;
        private String productVersion;
        private JBossProductType productType;
        private String releaseVersion;
        private String releaseCodeName;
        private boolean fromRemote = false;
        private String serverName;

        public ProductInfo(String hostname, String user, String pass, int port) {
            this.hostname = hostname;
            this.user = user;
            this.pass = pass;
            this.port = port;
        }

        public String getProductVersion() {
            return productVersion;
        }

        public JBossProductType getProductType() {
            return productType;
        }

        public ProductInfo getFromRemote() {
            ASConnection connection = new ASConnection(hostname, port, user, pass);
            try {
                String productName = getServerAttribute(connection, "product-name");
                if ((productName != null) && !productName.isEmpty())
                    productType = JBossProductType.getValueByProductName(productName);
                else {
                    Integer apiVersion = getServerAttribute(connection,"management-major-version");
                    if (apiVersion==1) {
                        productType = JBossProductType.AS;
                    } else {
                        // In the future also check for other versions of WildFly via the release-version
                        productType = JBossProductType.WILDFLY8;
                    }
                }
                releaseVersion = getServerAttribute(connection, "release-version");
                releaseCodeName = getServerAttribute(connection, "release-codename");
                serverName = getServerAttribute(connection, "name");
                productVersion = getServerAttribute(connection, "product-version");
                if (productVersion == null) {
                    productVersion = releaseVersion;
                }
                fromRemote = true;
            } catch (InvalidPluginConfigurationException e) {
                log.debug("Could not get the product info from [" + hostname + ":" + port
                    + "] - probably a connection failure");
            } finally {
                connection.shutdown();
            }
            return this;
        }

        @Override
        public String toString() {
            return "ProductInfo{" + "hostname='" + hostname + '\'' + ", port=" + port + ", productVersion='"
                + productVersion + '\'' + ", productType='" + productType + '\'' + ", releaseVersion='"
                + releaseVersion + '\'' + ", releaseCodeName='" + releaseCodeName + '\'' + ", fromRemote=" + fromRemote
                + '}';
        }
    }

}
