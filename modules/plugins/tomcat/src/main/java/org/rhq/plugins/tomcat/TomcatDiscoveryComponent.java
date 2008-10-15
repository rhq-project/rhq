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
package org.rhq.plugins.tomcat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;

/**
 * Discovers standalone Tomcat servers.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public class TomcatDiscoveryComponent implements ResourceDiscoveryComponent {
    // Constants  --------------------------------------------

    /**
     * Indicates the Tomcat version information could not be determined.
     */
    public static final String UNKNOWN_VERSION = "Unknown Version";

    /**
     * Formal name used to identify the Tomcat server.
     */
    private static final String PRODUCT_NAME = "Apache Tomcat";

    /**
     * Formal description of the Tomcat prodcut passed into discovered resources.
     */
    private static final String PRODUCT_DESCRIPTION = "Apache Tomcat Web Application Server";

    /**
     * Pattern used to parse out the Tomcat server version from the version script output. For details on which of these
     * patterns will be used, check {@link #determineVersion(String, org.rhq.core.system.SystemInfo)}.
     */
    private static final Pattern SERVER_6X_VERSION_PATTERN = Pattern.compile(".*Server number:.*");

    /**
     * Pattern used to parse out the Tomcat server version from the version script output. For details on which of these
     * patterns will be used, check {@link #determineVersion(String, org.rhq.core.system.SystemInfo)}.
     */
    private static final Pattern SERVER_50_VERSION_PATTERN = Pattern.compile(".*Version:.*");

    /**
     * Plugin configuration property name.
     */
    private static final String PROP_START_SCRIPT = "startScript";

    /**
     * Plugin configuration property name.
     */
    private static final String PROP_SHUTDOWN_SCRIPT = "shutdownScript";

    /**
     * Plugin configuration property name.
     */
    private static final String PROP_INSTALLATION_PATH = "installationPath";

    /**
     * Plugin configuration property name.
     */
    private static final String PROP_JMX_URL = "jmxUrl";

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(TomcatDiscoveryComponent.class);

    // ResourceDiscoveryComponent  --------------------------------------------

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.info("Discovering Tomcat servers");

        Set<DiscoveredResourceDetails> resources = new HashSet<DiscoveredResourceDetails>();

        // For each Tomcat process found in the context, process and create a resource details instance
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            log.debug("Discovered Tomcat process: " + autoDiscoveryResult);

            try {
                DiscoveredResourceDetails resource = parseTomcatProcess(context, autoDiscoveryResult);
                if (resource != null) {
                    resources.add(resource);
                }
            } catch (Exception e) {
                log.error("Error creating discovered resource for process: " + autoDiscoveryResult, e);
            }
        }

        return resources;
    }

    // Private  --------------------------------------------

    /**
     * Processes a process that has been detected to be a Tomcat server. The result will be a JON resource ready to be
     * returned as part of the discovery report.
     *
     * @param  context             discovery context making this call
     * @param  autoDiscoveryResult process scan being parsed for a Tomcat resource
     *
     * @return resource object describing the Tomcat servre running in the specified process
     */
    private DiscoveredResourceDetails parseTomcatProcess(ResourceDiscoveryContext context,
        ProcessScanResult autoDiscoveryResult) {
        // Pull out data from the discovery call
        ProcessInfo processInfo = autoDiscoveryResult.getProcessInfo();
        SystemInfo systemInfo = context.getSystemInformation();
        String[] commandLine = processInfo.getCommandLine();

        if (isStandalone(commandLine)) {
            log.info("Detected a standalone tomcat instance with following command line, ignoring: "
                + Arrays.toString(commandLine));
            return null;
        }

        String[] classpath = findClassPath(commandLine);
        String installationPath = determineInstallationPath(classpath);
        TomcatConfig config = parseTomcatConfig(installationPath);

        // Create pieces necessary for the resource creation
        String resourceVersion = determineVersion(installationPath, systemInfo);
        String hostname = systemInfo.getHostname();
        String resourceName = ((hostname == null) ? "" : (hostname + " ")) + PRODUCT_NAME + " " + resourceVersion
            + " (" + ((config.getAddress() == null) ? "" : (config.getAddress() + ":")) + config.getPort() + ")";
        String resourceKey = installationPath;

        Configuration pluginConfiguration = populatePluginConfiguration(installationPath);

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), resourceKey,
            resourceName, resourceVersion, PRODUCT_DESCRIPTION, pluginConfiguration, processInfo);
        return resource;
    }

    /**
     * Check from the commandline if this is a standalone tomcat
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
     * Searches through the command line arguments for the classpath setting.
     *
     * @param  arguments command line arguments passed to the java process
     *
     * @return array of entries in the classpath; <code>null</code> if the classpath is not specified using -cp or
     *         -classpath
     */
    private String[] findClassPath(String[] arguments) {
        for (int ii = 0; ii < (arguments.length - 1); ii++) {
            String arg = arguments[ii];
            if ("-cp".equals(arg) || "-classpath".equals(arg)) {
                String[] classpath = arguments[ii + 1].split(File.pathSeparator);
                return classpath;
            }
        }

        return null;
    }

    /**
     * Looks for a known JAR in the classpath to determine the installation path of the Tomcat instance.
     *
     * @param  classpath classpath of the java call
     *
     * @return
     */
    private String determineInstallationPath(String[] classpath) {
        for (String classpathEntry : classpath) {
            if (classpathEntry.endsWith("bootstrap.jar")) {
                // Directory of bootstrap.jar
                String installationPath = classpathEntry.substring(0, classpathEntry.lastIndexOf(File.separatorChar));

                // bootstrap.jar is in the /bin directory, so move one directory up
                installationPath = installationPath.substring(0, installationPath.lastIndexOf(File.separatorChar));

                return installationPath;
            }
        }

        return null;
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
        /* Sample output from version script in Tomcat 5.5 and higher
         *
         * Server version: Apache Tomcat/5.5.23 Server built:   Mar 5 2007 08:25:04 Server number:  5.5.23.0 OS Name:
         *   Linux OS Version:     2.6.20-16-generic Architecture:   i386 JVM Version:    1.5.0_12-b04 JVM Vendor:
         * Sun Microsystems Inc.
         *
         * Sample output from version script in Tomcat 5.0
         *
         * Using CATALINA_BASE:   /opt/tomcat/jakarta-tomcat-5.0.30 Using CATALINA_HOME:
         * /opt/tomcat/jakarta-tomcat-5.0.30 Using CATALINA_TMPDIR: /opt/tomcat/jakarta-tomcat-5.0.30/temp Using
         * JAVA_HOME:       /opt/jdk/jdk1.5.0_12 Version: Apache Tomcat/5.0.30
         */

        boolean isNix = File.separatorChar == '/';

        // Execute the version script included with Tomcat 5.0 and higher
        String versionScriptFileName = installationPath + File.separator + "bin" + File.separator + "version."
            + (isNix ? "sh" : "bat");

        ProcessExecution processExecution = new ProcessExecution(versionScriptFileName);
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(500);
        processExecution.setKillOnTimeout(true);

        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);
        String versionOutput = results.getCapturedOutput();

        // The tomcat .tar.gz files do not come with the version.sh marked as executable. Rather than making every
        // customer change this, try again using an assumed location for the shell.
        if (versionOutput.equals("") && isNix) {
            processExecution = new ProcessExecution("/bin/sh");
            processExecution.setArguments(new String[] { versionScriptFileName });
            processExecution.setCaptureOutput(true);
            processExecution.setWaitForCompletion(500);
            processExecution.setKillOnTimeout(true);

            results = systemInfo.executeProcess(processExecution);
            versionOutput = results.getCapturedOutput();
        }

        // Check for both the 5.5/6.x format and the 5.0 format
        Matcher matcher6x = SERVER_6X_VERSION_PATTERN.matcher(versionOutput);
        Matcher matcher5x = SERVER_50_VERSION_PATTERN.matcher(versionOutput);

        String version = UNKNOWN_VERSION;
        if (matcher6x.find()) {
            // Matcher returns:       Server number:  5.5.23.0
            String serverNumberString = matcher6x.group();
            String[] serverNumberParts = serverNumberString.split(":");
            version = serverNumberParts[1].trim();
        } else if (matcher5x.find()) {
            // Matcher returns:       Version: Apache Tomcat/5.0.30
            String serverNumberString = matcher5x.group();
            String[] serverNumberParts = serverNumberString.split("/");
            version = serverNumberParts[1].trim();
        }

        return version;
    }

    /**
     * Populates the plugin configuration for the tomcat instance being discovered.
     *
     * @param  installationPath identifies the Tomcat server being discovered
     *
     * @return populated plugin configuration instance
     */
    private Configuration populatePluginConfiguration(String installationPath) {
        Configuration configuration = new Configuration();

        configuration.put(new PropertySimple(PROP_INSTALLATION_PATH, installationPath));

        String scriptExtension = (File.separatorChar == '/') ? "sh" : "bat";

        configuration.put(new PropertySimple(PROP_START_SCRIPT, installationPath + File.separator + scriptExtension));
        configuration
            .put(new PropertySimple(PROP_SHUTDOWN_SCRIPT, installationPath + File.separator + scriptExtension));

        return configuration;
    }
}