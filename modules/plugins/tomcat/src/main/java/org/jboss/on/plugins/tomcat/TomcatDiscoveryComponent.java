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
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * Discovers JBoss EWS and Apache Tomcat5, Tomcat6 server instances.
 *
 * @author Jay Shaughnessy
 */
@SuppressWarnings("unchecked")
public class TomcatDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Indicates the version information could not be determined.
     */
    public static final String UNKNOWN_PORT = "Unknown Port";
    public static final String UNKNOWN_VERSION = "Unknown Version";

    public static final String PROPERTY_CATALINA_BASE = "-Dcatalina.base=";
    public static final String PROPERTY_CATALINA_HOME = "-Dcatalina.home=";

    /**
     * Formal name used to identify the server. Today we name all servers the same. 
     */
    private static final String PRODUCT_NAME = "Tomcat";

    /**
     * Formal description of the product passed into discovered resources. Today we do not distinguish between EWS and Apache installs.
     */
    private static final String PRODUCT_DESCRIPTION = "Tomcat Web Application Server";

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
     * There is no real good way to distinguish an EWS install from an Apache install.  The default unzip
     * directory structure for EWS will contain "ews" in the path.  But this can be changed.  For rpms the
     * rpm itself has a slightly different format than the non-ews rpm. Since these mechanisms are weak
     * we don't currently fork any behavior based on EWS vs Apache.
     */
    // untested private static final Pattern EWS_RPM_INSTALL_PATTERN = Pattern.compile("tomcat[5\\-5|6\\-6].*\\.ep5\\.*");
    private static final Pattern EWS_ZIP_INSTALL_PATTERN = Pattern.compile(".*ews.*tomcat[5-9]");

    /**
     * EWS RPM Install path substrings used to identify EWS tomcat version
     */
    public static final String EWS_TOMCAT_6 = "tomcat6";
    public static final String EWS_TOMCAT_5 = "tomcat5";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
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
     * @param  autoDiscoveryResult process scan being parsed for an tomcat resource
     *
     * @return resource object describing the Tomcat server running in the specified process
     */
    private DiscoveredResourceDetails parseTomcatProcess(ResourceDiscoveryContext context,
        ProcessScanResult autoDiscoveryResult) {

        ProcessInfo processInfo = autoDiscoveryResult.getProcessInfo();
        String[] commandLine = processInfo.getCommandLine();

        if (null == processInfo.getExecutable()) {
            log.debug("Ignoring Tomcat instance (agent may not be owner) with following command line: "
                + Arrays.toString(commandLine));
            return null;
        }
        if (!isStandalone(commandLine)) {
            log.debug("Ignoring embedded Tomcat instance (catalina.home not found) with following command line: "
                + Arrays.toString(commandLine));
            return null;
        }

        String catalinaHome = determineCatalinaHome(commandLine);
        if (null == catalinaHome) {
            log.error("Ignoring Tomcat instance due to invalid setting of catalina.home in command line: "
                + Arrays.toString(commandLine));
            return null;
        }

        String catalinaBase = determineCatalinaBase(commandLine, catalinaHome);
        if (null == catalinaBase) {
            log.error("Ignoring Tomcat instance due to invalid setting of catalina.base in command line: "
                + Arrays.toString(commandLine));
            return null;
        }

        // Pull out data from the discovery call
        SystemInfo systemInfo = context.getSystemInformation();
        String hostname = systemInfo.getHostname();
        TomcatConfig tomcatConfig = parseTomcatConfig(catalinaBase);

        // Create pieces necessary for the resource creation
        String resourceVersion = determineVersion(catalinaHome, catalinaBase, systemInfo);
        String productName = PRODUCT_NAME;
        String productDescription = PRODUCT_DESCRIPTION + ((hostname == null) ? "" : (" (" + hostname + ")"));
        String resourceName = productName + " ("
            + ((tomcatConfig.getAddress() == null) ? "" : (tomcatConfig.getAddress() + ":")) + tomcatConfig.getPort()
            + ")";
        String resourceKey = catalinaBase;

        Configuration pluginConfiguration = new Configuration();
        populatePluginConfiguration(pluginConfiguration, catalinaHome, catalinaBase, commandLine);

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
    private DiscoveredResourceDetails parsePluginConfig(ResourceDiscoveryContext context,
        Configuration pluginConfiguration) {

        String catalinaHome = pluginConfiguration.getSimpleValue(
            TomcatServerComponent.PLUGIN_CONFIG_CATALINA_HOME_PATH, "invalid");
        try {
            catalinaHome = FileUtils.getCanonicalPath(catalinaHome);
        } catch (Exception e) {
            // leave as is
        }
        File catalinaHomeDir = new File(catalinaHome);

        String catalinaBase = pluginConfiguration.getSimpleValue(
            TomcatServerComponent.PLUGIN_CONFIG_CATALINA_HOME_PATH, catalinaHome);
        try {
            catalinaBase = FileUtils.getCanonicalPath(catalinaBase);
        } catch (Exception e) {
            // leave as is
        }

        SystemInfo systemInfo = context.getSystemInformation();
        String hostname = systemInfo.getHostname();
        String version = UNKNOWN_VERSION;
        String port = UNKNOWN_PORT;
        String address = null;

        // TODO : Should we even allow this remote server stuff? I think we risk a resourceKey collsion with a local
        // server.

        // if the specified home dir does not exist locally assume this is a remote Tomcat server
        // We can't determine version. Try to get the hostname from the connect url
        if (!catalinaHomeDir.isDirectory()) {
            log.info("Manually added Tomcat Server directory does not exist locally. Assuming remote Tomcat Server: "
                + catalinaHome);

            Matcher matcher = TOMCAT_MANAGER_URL_PATTERN.matcher(pluginConfiguration.getSimpleValue(
                JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY, null));
            if (matcher.find()) {
                hostname = matcher.group(1);
                address = hostname;
                port = matcher.group(2);
            }
        } else {
            TomcatConfig tomcatConfig = parseTomcatConfig(catalinaBase);
            version = determineVersion(catalinaHome, catalinaBase, systemInfo);
            address = tomcatConfig.getAddress();
            hostname = systemInfo.getHostname();
            port = tomcatConfig.getPort();
        }

        String productName = PRODUCT_NAME;
        String productDescription = PRODUCT_DESCRIPTION + ((hostname == null) ? "" : (" (" + hostname + ")"));
        String resourceName = productName + " (" + ((address == null) ? "" : (address + ":")) + port + ")";
        String resourceKey = catalinaBase;

        populatePluginConfiguration(pluginConfiguration, catalinaHome, catalinaBase, null);

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
            if (item.toLowerCase().contains("catalina.home")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Looks for tomcat home in the command line properties. Requires a full path for the catalina.home
     * property. The path may be a symbolic link.
     * 
     * @param startup command line
     *
     * @return A canonical form of the catalina home path set in the command line.  Symbolic links
     * are not resolved to ensure that we discover the same resource repeatedly for the same symlink
     * despite changes in the physical path. Null if invalid. Null if invalid.
     */
    private String determineCatalinaHome(String[] cmdLine) {
        String result = null;

        for (int i = 0; i < cmdLine.length; ++i) {
            String line = cmdLine[i];
            if (line.startsWith(PROPERTY_CATALINA_HOME)) {
                result = line.substring(PROPERTY_CATALINA_HOME.length());
                break;
            }
        }

        if (null != result) {
            try {
                result = FileUtils.getCanonicalPath(result);
            } catch (Exception e) {
                result = null;
            }
        }

        return result;
    }

    /**
     * Looks for tomcat instance base in the command line properties. Requires a full path for the catalina.base, if
     * specified.  The path may be a symbolic link.
     * 
     * @param startup command line
     *
     * @return A canonical form of the catalina base path if set in the command line.  Symbolic links
     * are not resolved to ensure that we discover the same resource repeatedly for the same symlink
     * despite changes in the physical path. Returns supplied catalinaHome if not found. Null if found but invalid. 
     */
    private String determineCatalinaBase(String[] cmdLine, String catalinaHome) {
        String result = null;

        for (int i = 0; i < cmdLine.length; ++i) {
            String line = cmdLine[i];

            if (line.startsWith(PROPERTY_CATALINA_BASE)) {
                result = line.substring(PROPERTY_CATALINA_BASE.length());
                break;
            }
        }

        if (null == result) {
            result = catalinaHome;
        } else {
            try {
                result = FileUtils.getCanonicalPath(result);
            } catch (Exception e) {
                result = null;
            }
        }

        return result;
    }

    /**
     * Parses the tomcat config file (server.xml) and returns a value object with access to its relevant contents.
     *
     * @param  catalinaBase base dir for the tomcat instance
     *
     * @return value object; <code>null</code> if the config file cannot be found
     */
    private TomcatConfig parseTomcatConfig(String catalinaBase) {
        String configFileName = catalinaBase + File.separator + "conf" + File.separator + "server.xml";
        File configFile = new File(configFileName);
        TomcatConfig config = TomcatConfig.getConfig(configFile);
        return config;
    }

    /**
     * Executes the necessary script to determine the Tomcat version number.
     *
     * @param  catalinaHome path to the Tomcat instance being checked
     * @param  systemInfo       used to make the script call
     *
     * @return version of the tomcat instance; unknown version message if it cannot be determined
     */
    private String determineVersion(String catalinaHome, String catalinaBase, SystemInfo systemInfo) {
        boolean isNix = File.separatorChar == '/';
        String versionScriptFileName = catalinaHome + File.separator + "bin" + File.separator + "version."
            + (isNix ? "sh" : "bat");
        File versionScriptFile = new File(versionScriptFileName);

        if (!versionScriptFile.exists()) {
            log.warn("Version script file not found in expected location: " + versionScriptFile);
            return UNKNOWN_VERSION;
        }

        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(versionScriptFile);
        TomcatServerOperationsDelegate.setProcessExecutionEnvironment(processExecution, catalinaHome, catalinaBase);

        long timeout = 10000L;
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(timeout);
        processExecution.setKillOnTimeout(true);

        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        String versionOutput = results.getCapturedOutput();

        String version = getVersionFromVersionScriptOutput(versionOutput);

        if (UNKNOWN_VERSION.equals(version)) {
            log.warn("Failed to determine Tomcat Server Version Given:\nVersionInfo:" + versionOutput
                + "\ncatalinaHome: " + catalinaHome + "\nScript:" + versionScriptFileName + "\ntimeout=" + timeout);
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

    private void populatePluginConfiguration(Configuration configuration, String catalinaHome, String catalinaBase,
        String[] commandLine) {

        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_CATALINA_HOME_PATH, null)) {
            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_CATALINA_HOME_PATH, catalinaHome));
        }

        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_CATALINA_BASE_PATH, null)) {
            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_CATALINA_BASE_PATH, catalinaBase));
        }

        String binPath = catalinaHome + File.separator + "bin" + File.separator;
        String scriptExtension = (File.separatorChar == '/') ? ".sh" : ".bat";

        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_START_SCRIPT, null)) {
            String script = binPath + "startup" + scriptExtension;
            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_START_SCRIPT, script));
        }
        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_SHUTDOWN_SCRIPT, null)) {
            String script = binPath + "shutdown" + scriptExtension;
            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_SHUTDOWN_SCRIPT, script));
        }
        if (null == configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_CONTROL_METHOD, null)) {
            // if the specified startup script does not exist and the installation path looks indicative, assume an RPM install.
            TomcatServerComponent.ControlMethod controlMethod = TomcatServerComponent.ControlMethod.SCRIPT;

            // The script should exist unless this is an rpm install, which uses System V init.
            if (!new File(configuration.getSimpleValue(TomcatServerComponent.PLUGIN_CONFIG_START_SCRIPT, null))
                .exists()) {
                if (isEWS(catalinaHome)) {
                    controlMethod = TomcatServerComponent.ControlMethod.RPM;
                }
            }

            configuration.put(new PropertySimple(TomcatServerComponent.PLUGIN_CONFIG_CONTROL_METHOD, controlMethod
                .name()));
        }

        populateJMXConfiguration(configuration, commandLine);
    }

    /**
     * Check from the command line if this is an EWS tomcat
     *
     * @param  commandLine
     *
     * @return
     */
    private static boolean isEWS(String catalinaHome) {
        boolean isEws = ((null != catalinaHome) && EWS_ZIP_INSTALL_PATTERN.matcher(catalinaHome.toLowerCase())
            .matches());

        if (!isEws) {
            // TODO Check for rpm name to distinguish EWS rpm from standard apache rpm.  Note - this isn't necessary until
            // (if) we ever do anything differently based on whether this is an EWS or Apache install.
        }

        return isEws;
    }

    public static boolean isEWSTomcat5(String catalinaHome) {
        return (isEWS(catalinaHome) && catalinaHome.endsWith(EWS_TOMCAT_5));
    }

    public static boolean isEWSTomcat6(String catalinaHome) {
        return (isEWS(catalinaHome) && catalinaHome.endsWith(EWS_TOMCAT_6));
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
