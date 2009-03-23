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

package org.jboss.on.plugins.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.on.plugins.tomcat.helper.TomcatConfig;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * Discovers JBoss EWS and Apache Tomcat5, Tomcat6 server instances.
 *
 * @author Jay Shaughnessy
 */
public class TomcatDiscoveryComponent implements ResourceDiscoveryComponent<PlatformComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Indicates the version information could not be determined.
     */
    public static final String UNKNOWN_PORT = "Unknown Port";
    public static final String UNKNOWN_VERSION = "Unknown Version";

    public static final String PROPERTY_CATALINA_BASE = "-Dcatalina.base=";
    public static final String PROPERTY_CATALINA_HOME = "-Dcatalina.home=";

    /**
     * Formal name used to identify the server.
     */
    private static final String PRODUCT_NAME_EWS = "JBoss EWS Tomcat";
    private static final String PRODUCT_NAME_APACHE = "Apache Tomcat";

    /**
     * Formal description of the product passed into discovered resources.
     */
    private static final String PRODUCT_DESCRIPTION_EWS = "JBoss Enterprise Web Application Server";
    private static final String PRODUCT_DESCRIPTION_APACHE = "Apache Tomcat Web Application Server";

    /**
     * Patterns used to parse out the Tomcat server version from the version script output. For details on which of these
     * patterns will be used, check {@link #determineVersion(String, org.rhq.core.system.SystemInfo)}.
     */
    private static final Pattern TOMCAT_5_5_AND_LATER_VERSION_PATTERN = Pattern.compile(".*Server number:.*");
    private static final Pattern TOMCAT_5_0_AND_EARLIER_VERSION_PATTERN = Pattern.compile(".*Version:.*");

    /**
     * Pattern to parse out host/port for manual add of remote server 
     */
    private static final Pattern TOMCAT_MANAGER_URL_PATTERN = Pattern.compile(".*//(.*):(\\d+)/.*");

    /** 
     * EWS Install path pattern used to distinguish from standalone Apache Tomcat installs. Good up through a TC V9
     */
    private static final Pattern EWS_PATTERN = Pattern.compile(".*ews.*tomcat[5-9]");

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PlatformComponent> context) {
        log.debug("Discovering Tomcat servers...");

        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();

        // For each Tomcat process found in the context, create a resource details instance
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            if (log.isDebugEnabled()) {
                log.debug("Discovered potential Tomcat process: " + autoDiscoveryResult);
            }

            try {
                DiscoveredResourceDetails resource = parseTomcatProcess(context, autoDiscoveryResult);
                if (resource != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Verified Tomcat process: " + autoDiscoveryResult);
                    }

                    resources.add(resource);
                }
            } catch (Exception e) {
                log.error("Error creating discovered resource for process: " + autoDiscoveryResult, e);
            }
        }

        // Process any manually-added resources.
        List<Configuration> contextPluginConfigurations = context.getPluginConfigurations();
        for (Configuration pluginConfiguration : contextPluginConfigurations) {
            ProcessInfo processInfo = null;
            DiscoveredResourceDetails resource = parsePluginConfig(context, pluginConfiguration);
            if (resource != null)
                if (log.isDebugEnabled()) {
                    log.debug("Verified Tomcat configuration: " + pluginConfiguration);
                }

            resources.add(resource);
        }

        return resources;
    }

    /**
     * Processes a process that has been detected to be a Tomcat server process. If a standalone
     * Apache or EWS Tomcat instance return a resource ready to be returned as part of the discovery report.
     *
     * @param  context             discovery context making this call
     * @param  autoDiscoveryResult process scan being parsed for an EWS resource
     *
     * @return resource object describing the Tomcat server running in the specified process
     */
    private DiscoveredResourceDetails parseTomcatProcess(ResourceDiscoveryContext<PlatformComponent> context,
        ProcessScanResult autoDiscoveryResult) {

        ProcessInfo processInfo = autoDiscoveryResult.getProcessInfo();
        String[] commandLine = processInfo.getCommandLine();

        if (null == processInfo.getExecutable()) {
            log.debug("Ignoring Tomcat instance (agent may not be owner) with following command line: "
                + Arrays.toString(commandLine));
            return null;
        }
        if (!isStandalone(commandLine)) {
            log.debug("Ignoring embedded Tomcat instance with following command line: " + Arrays.toString(commandLine));
            return null;
        }

        String installationPath = determineInstallationPath(processInfo);

        // Pull out data from the discovery call
        SystemInfo systemInfo = context.getSystemInformation();
        String hostname = systemInfo.getHostname();
        TomcatConfig tomcatConfig = parseTomcatConfig(installationPath);

        // Create pieces necessary for the resource creation
        String resourceVersion = determineVersion(installationPath, systemInfo);
        boolean isEWS = isEWS(installationPath);
        String productName = isEWS ? PRODUCT_NAME_EWS : PRODUCT_NAME_APACHE;
        String productDescription = (isEWS ? PRODUCT_DESCRIPTION_EWS : PRODUCT_DESCRIPTION_APACHE)
            + ((hostname == null) ? "" : (" (" + hostname + ")"));
        String resourceName = productName + " ("
            + ((tomcatConfig.getAddress() == null) ? "" : (tomcatConfig.getAddress() + ":")) + tomcatConfig.getPort()
            + ")";
        String resourceKey = installationPath;

        Configuration pluginConfiguration = new Configuration();
        populatePluginConfiguration(pluginConfiguration, installationPath, commandLine);

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), resourceKey,
            resourceName, resourceVersion, productDescription, pluginConfiguration, processInfo);

        return resource;
    }

    /**
     * Processes a manually specified plugin configuration. If a standalone Apache or EWS Tomcat instance
     * return a resource ready to be returned as part of the discovery report.
     *
     * @param  context             discovery context making this call
     * @param  pluginConfiguration The manually specified plugin config
     *
     * @return resource object describing the Tomcat server running in the specified process
     */
    private DiscoveredResourceDetails parsePluginConfig(ResourceDiscoveryContext<PlatformComponent> context,
        Configuration pluginConfiguration) {

        String installationPath = pluginConfiguration.getSimpleValue(
            TomcatServerComponent.PLUGIN_CONFIG_INSTALLATION_PATH, "invalid");
        File installDir = new File(installationPath);
        SystemInfo systemInfo = context.getSystemInformation();
        String hostname = systemInfo.getHostname();
        String version = UNKNOWN_VERSION;
        String port = UNKNOWN_PORT;
        String address = null;

        // if the specified install dir does not exist locally assume this is a remote Tomcat server
        // We can't determine version. Try to get the hostname from the connect url
        if (!installDir.isDirectory()) {
            log.info("Manually added Tomcat Server directory does not exist locally. Assuming remote Tomcat Server: "
                + installationPath);

            Matcher matcher = TOMCAT_MANAGER_URL_PATTERN.matcher(pluginConfiguration.getSimpleValue(
                JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY, null));
            if (matcher.find()) {
                hostname = matcher.group(1);
                address = hostname;
                port = matcher.group(2);
            }
        } else {
            TomcatConfig tomcatConfig = parseTomcatConfig(installationPath);
            version = determineVersion(installationPath, systemInfo);
            address = tomcatConfig.getAddress();
            hostname = systemInfo.getHostname();
            port = tomcatConfig.getPort();
        }

        boolean isEWS = isEWS(installationPath);
        String productName = isEWS ? PRODUCT_NAME_EWS : PRODUCT_NAME_APACHE;
        String productDescription = (isEWS ? PRODUCT_DESCRIPTION_EWS : PRODUCT_DESCRIPTION_APACHE)
            + ((hostname == null) ? "" : (" (" + hostname + ")"));
        String resourceName = productName + " (" + ((address == null) ? "" : (address + ":")) + port + ")";
        String resourceKey = installationPath;

        populatePluginConfiguration(pluginConfiguration, installationPath, null);

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), resourceKey,
            resourceName, version, productDescription, pluginConfiguration, null);

        return resource;
    }

    /**
     * Check from the command line if this is a standalone tomcat
     *
     * @param  commandLine
     *
     * @return
     */
    private boolean isStandalone(String[] commandLine) {
        for (String item : commandLine) {
            if (item.contains("catalina.home")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check from the command line if this is an EWS tomcat
     *
     * @param  commandLine
     *
     * @return
     */
    private static boolean isEWS(String installationPath) {
        boolean isEws = ((null != installationPath) && EWS_PATTERN.matcher(installationPath.toLowerCase()).matches());

        return isEws;
    }

    /**
     * Looks for tomcat home on the assumption that the process working directory is home/bin.  This may or may not
     * be a valid assumption, if cwd is not /bin will try {@link determineInstallationPath(String[])}.
     * 
     * @param ProcessInfo for the standalone tomcat process. Null if it can't be determined.
     *
     * @return
     */
    private String determineInstallationPath(ProcessInfo processInfo) {
        String cwdPath = null;
        String result = null;

        if (null != processInfo.getExecutable()) {
            cwdPath = processInfo.getCurrentWorkingDirectory();
            if (cwdPath.endsWith("bin")) {
                result = new File(cwdPath).getParent();
            }
        }

        if (null == result) {
            result = determineInstallationPath(processInfo.getCommandLine());
        }

        return result;
    }

    /**
     * Looks for tomcat home in the command line properties.
     * 
     * This can be called if we are guaranteed to have an absolute path for the catalina.home property. Otherwise, call
     * {@link determineInstallationPath(ProcessInfo)}  
     *
     * @param startup command line
     *
     * @return
     */
    private String determineInstallationPath(String[] cmdLine) {
        String result = null;

        for (int i = 0; i < cmdLine.length; ++i) {
            String line = cmdLine[i];
            if (line.startsWith(PROPERTY_CATALINA_HOME)) {
                result = line.substring(PROPERTY_CATALINA_HOME.length());
                break;
            }
            // older versions may have only this property defined
            if (line.startsWith(PROPERTY_CATALINA_BASE)) {
                result = line.substring(PROPERTY_CATALINA_BASE.length());
                break;
            }
        }

        if (null != result) {
            try {
                result = new File(result).getCanonicalPath();
            } catch (IOException e) {
                log.warn("Unexpected standalone Tomcat installation path: " + result);
            }
        }

        return result;
    }

    /**
     * Parses the tomcat config file (server.xml) and returns a value object with access to its relevant contents.
     *
     * @param  installationPath installation path of the tomcat instance
     *
     * @return value object; <code>null</code> if the config file cannot be found
     */
    private TomcatConfig parseTomcatConfig(String installationPath) {
        String configFileName = installationPath + File.separator + "conf" + File.separator + "server.xml";
        File configFile = new File(configFileName);
        TomcatConfig config = TomcatConfig.getConfig(configFile);
        return config;
    }

    /**
     * Executes the necessary script to determine the Tomcat version number.
     *
     * @param  installationPath path to the Tomcat instance being checked
     * @param  systemInfo       used to make the script call
     *
     * @return version of the tomcat instance; unknown version message if it cannot be determined
     */
    private String determineVersion(String installationPath, SystemInfo systemInfo) {
        boolean isNix = File.separatorChar == '/';
        String versionScriptFileName = installationPath + File.separator + "bin" + File.separator + "version."
            + (isNix ? "sh" : "bat");
        File versionScriptFile = new File(versionScriptFileName);

        if (!versionScriptFile.exists()) {
            log.warn("Version script file not found in expected location: " + versionScriptFile);
            return UNKNOWN_VERSION;
        }

        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(versionScriptFile);
        TomcatServerOperationsDelegate.setProcessExecutionEnvironment(processExecution, installationPath);

        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(5000L);
        processExecution.setKillOnTimeout(true);

        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        String versionOutput = results.getCapturedOutput();

        String version = getVersionFromVersionScriptOutput(versionOutput);

        if (UNKNOWN_VERSION.equals(version)) {
            log.warn("Failed to determine Tomcat Server Version Given:\nVersionInfo:" + versionOutput
                + "\ninstallationPath: " + installationPath + "\nScript:" + versionScriptFileName + "\ntimeout=5000L");
        }

        return version;
    }

    private String getVersionFromVersionScriptOutput(String versionOutput) {
        String version = UNKNOWN_VERSION;
        // Try more recent Tomcat version string format first.
        Matcher matcher = TOMCAT_5_5_AND_LATER_VERSION_PATTERN.matcher(versionOutput);
        if (matcher.find()) {
            String serverNumberString = matcher.group();
            String[] serverNumberParts = serverNumberString.split(":");
            version = serverNumberParts[1].trim();
        } else {
            matcher = TOMCAT_5_0_AND_EARLIER_VERSION_PATTERN.matcher(versionOutput);
            if (matcher.find()) {
                String serverNumberString = matcher.group();
                String[] serverNumberParts = serverNumberString.split("/");
                version = serverNumberParts[1].trim();
            }
        }
        return version;
    }

    private void populatePluginConfiguration(Configuration configuration, String installationPath, String[] commandLine) {

        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_INSTALLATION_PATH, null)) {
            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_INSTALLATION_PATH,
                installationPath));
        }

        String binPath = installationPath + File.separator + "bin" + File.separator;
        String scriptExtension = (File.separatorChar == '/') ? ".sh" : ".bat";

        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_START_SCRIPT, null)) {
            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_START_SCRIPT, binPath + "startup"
                + scriptExtension));
        }
        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_SHUTDOWN_SCRIPT, null)) {
            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_SHUTDOWN_SCRIPT, binPath
                + "shutdown" + scriptExtension));
        }

        populateJMXConfiguration(configuration, commandLine);
    }

    private void populateJMXConfiguration(Configuration configuration, String[] commandLine) {
        // null for manual add, properties will already be set
        if (null == commandLine) {
            return;
        }

        String portProp = "com.sun.management.jmxremote.port";

        String port = null;
        for (String argument : commandLine) {
            String cmdLineArg = "-D" + portProp + "=";
            if (argument.startsWith(cmdLineArg)) {
                port = argument.substring(cmdLineArg.length());
                break;
            }
        }

        configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
            "org.mc4j.ems.connection.support.metadata.Tomcat55ConnectionTypeDescriptor"));
        // this should be set but just in case we'll use the default connection server url later on 
        if (null != port) {
            configuration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
                "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi"));
        }
    }

}
