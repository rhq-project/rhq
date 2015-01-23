/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.jmx;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.util.ConnectionProviderFactory;
import org.rhq.plugins.jmx.util.JvmResourceKey;
import org.rhq.plugins.jmx.util.JvmUtility;

/**
 * This component will discover JVM processes that appear to be long-running (i.e. "servers"). Specifically, it will
 * discover java processes that:
 * <ul>
 *  <li>have enabled JMX Remoting (JSR-160) via com.sun.management.jmxremote* system properties on their command lines,
 *      or</li>
 *  <li>are Sun/Oracle-compatible java processes accessible via the com.sun.tools.attach API AND specify the
 *      org.rhq.resourceKey system property on their command lines (e.g. -Dorg.rhq.resourceKey=FOO); the attach API uses IPC
 *      under the covers, so for a process to be accessible, it must be running as the same user as the RHQ Agent; even
 *      if the Agent is running as root, processes running as other users cannot be accessed via the attach API</li>
 * </ul>
 * Some other java processes that do not meet these criteria can be manually added if they expose JMX remotely in
 * another supported form (WebLogic, WebSphere, etc.).
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class JMXDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet, ResourceUpgradeFacet { //, ClassLoaderFacet {

    private static final Log log = LogFactory.getLog(JMXDiscoveryComponent.class);

    public static final String COMMAND_LINE_CONFIG_PROPERTY = "commandLine";

    public static final String CONNECTOR_ADDRESS_CONFIG_PROPERTY = "connectorAddress";

    public static final String INSTALL_URI = "installURI";

    public static final String CONNECTION_TYPE = "type";

    public static final String PARENT_TYPE = "PARENT";

    public static final String ADDITIONAL_CLASSPATH_ENTRIES = "additionalClassPathEntries";

    private static final String SYSPROP_JMXREMOTE_PORT = "com.sun.management.jmxremote.port";
    private static final String SYSPROP_RHQ_JMXPLUGIN_PROCESS_FILTERS = "rhq.jmxplugin.process-filters";
    public static final String SYSPROP_RHQ_RESOURCE_KEY = "org.rhq.resourceKey";
    private static final String SYSPROP_JAVA_VERSION = "java.version";

    /*
     * Ignore certain java processes that are managed by their own plugin. For example, the Tomcat plugin will handle
     * Tomcat processes configured for JMX management.
     */
    private static final String[] DEFAULT_PROCESS_EXCLUDES = new String[] {
        "org.rhq.enterprise.agent.AgentMain",             // RHQ Agent
        "org.jboss.Main",                                 // JBoss AS 3.x-6.x
        "catalina.startup.Bootstrap",                     // Tomcat
        "org.apache.cassandra.thrift.CassandraDaemon",    // Cassnadra 1.1.x
        "org.apache.cassandra.service.CassandraDaemon"    // Cassandra 1.2.x
    };

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet<DiscoveredResourceDetails>();
        Map<String, List<DiscoveredResourceDetails>> duplicatesByKey = new LinkedHashMap<String, List<DiscoveredResourceDetails>>();

        // Filter out JBoss, Tomcat, etc. processes, which will be represented by more specific types of Resources
        // discovered by other plugins.
        List<ProcessScanResult> nonExcludedProcesses = getNonExcludedJavaProcesses(context);

        for (ProcessScanResult process : nonExcludedProcesses) {
            try {
                ProcessInfo processInfo = process.getProcessInfo();
                DiscoveredResourceDetails details = discoverResourceDetails(context, processInfo);
                if (details != null) {
                    //detect discovered jmx resources that are erroneously using the same key
                    if (discoveredResources.contains(details)) {
                        List<DiscoveredResourceDetails> duplicates = duplicatesByKey.get(details.getResourceKey());
                        if (duplicates == null) {
                            duplicates = new ArrayList<DiscoveredResourceDetails>();
                            duplicatesByKey.put(details.getResourceKey(), duplicates);
                        }
                        duplicates.add(details);
                    }
                    discoveredResources.add(details);
                }
            } catch (RuntimeException re) {
                // Don't let a runtime exception for a particular ProcessInfo cause the entire discovery scan to fail.
                if (log.isDebugEnabled()) {
                    log.debug("Error when trying to discover JVM process [" + process + "].", re);
                } else {
                    log.warn("Error when trying to discover JVM process [" + process + "] (enable DEBUG for stack trace): " + re);
                }
            }
        }

        //Log the erroneous collisions and take them out of the discoveredResource list.
        for (String duplicateKey : duplicatesByKey.keySet()) {
            List<DiscoveredResourceDetails> duplicates = duplicatesByKey.get(duplicateKey);
            log.error("Multiple Resources with the same key (" + duplicateKey
                + ") were discovered - none will be reported to the plugin container! This most likely means that there are multiple java processes running with the same value for the "
                + SYSPROP_RHQ_RESOURCE_KEY + " system property specified on their command lines. Here is the list of Resources: "
                + duplicates);
            discoveredResources.remove(duplicates.get(0));
        }

        return discoveredResources;
    }

    private List<ProcessScanResult> getNonExcludedJavaProcesses(ResourceDiscoveryContext context) {
        // This is the list of all currently running java processes.
        List<ProcessScanResult> javaProcesses = context.getAutoDiscoveredProcesses();

        List<ProcessScanResult> nonExcludedJavaProcesses = new ArrayList<ProcessScanResult>();
        Set<String> processExcludes = getProcessExcludes();
        for (ProcessScanResult javaProcess : javaProcesses) {
            String[] args = javaProcess.getProcessInfo().getCommandLine();
            StringBuilder buffer = new StringBuilder();
            for (String arg : args) {
                buffer.append(arg).append(" ");
            }
            String commandLine = buffer.toString();
            if (!isExcluded(commandLine, processExcludes)) {
                nonExcludedJavaProcesses.add(javaProcess);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Process [" + javaProcess.getProcessInfo()
                        + "] excluded since its command line contains one of the following: " + processExcludes);
                }
            }
        }
        return nonExcludedJavaProcesses;
    }

    private boolean isExcluded(String commandLine, Set<String> processExcludes) {
        for (String processExclude : processExcludes) {
            if (commandLine.contains(processExclude)) {
                return true;
            }
        }
        return false;
    }

    protected Set<String> getProcessExcludes() {
        Set<String> processExcludes;
        String overrideProcessExcludes = System.getProperty(SYSPROP_RHQ_JMXPLUGIN_PROCESS_FILTERS);
        if (overrideProcessExcludes != null) {
            processExcludes = new HashSet<String>(Arrays.asList(overrideProcessExcludes.split(",")));
        } else {
            processExcludes = new HashSet<String>(Arrays.asList(DEFAULT_PROCESS_EXCLUDES));
        }
        return processExcludes;
    }

    // MANUAL ADD
    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext discoveryContext)
            throws InvalidPluginConfigurationException {

        String type = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE).getStringValue();
        if (type.equals(PARENT_TYPE)) {
            throw new InvalidPluginConfigurationException("'" + PARENT_TYPE + "' is not a valid type for a manually added JVM.");
        }

        String connectorAddress = pluginConfig.getSimpleValue(CONNECTOR_ADDRESS_CONFIG_PROPERTY, null);
        if (connectorAddress == null) {
            throw new InvalidPluginConfigurationException("A connector address must be specified when manually adding a JVM.");
        }

        ConnectionProvider connectionProvider;
        EmsConnection connection = null;
        try {
            connectionProvider = ConnectionProviderFactory.createConnectionProvider(pluginConfig, null,
                discoveryContext.getParentResourceContext().getTemporaryDirectory());
            connection = connectionProvider.connect();
            connection.loadSynchronous(false);
            String key = connectorAddress;
            String name = connectorAddress;

            String version = getJavaVersion(connection);
            if (version == null) {
                log.warn("Unable to determine version of JVM with connector address [" + connectorAddress + "].");
            }

            String connectionType = pluginConfig.getSimpleValue(CONNECTION_TYPE, null);
            String description = connectionType + " JVM (" + connectorAddress + ")";

            DiscoveredResourceDetails resourceDetails = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    key, name, version, description, pluginConfig, null);
            return resourceDetails;
        } catch (Exception e) {
            if (e.getCause() instanceof SecurityException) {
                throw new InvalidPluginConfigurationException("Failed to authenticate to JVM with connector address ["
                        + connectorAddress + "] - principal and/or credentials connection properties are not set correctly.");
            }
            throw new RuntimeException("Failed to connect to JVM with connector address [" + connectorAddress + "].", e);
        } finally {
            if(connection != null) {
                connection.close();
            }
        }

    }

    private String getJavaVersion(EmsConnection connection) {
        String version = null;
        try {
            EmsBean runtimeMXBean = connection.getBean(ManagementFactory.RUNTIME_MXBEAN_NAME);
            if (runtimeMXBean != null) {
                EmsAttribute systemPropertiesAttribute = runtimeMXBean.getAttribute("systemProperties");
                TabularData systemProperties = (TabularData) systemPropertiesAttribute.getValue();
                CompositeData compositeData = systemProperties.get(new String[]{"java.version"});
                if (compositeData != null) {
                    version = (String) compositeData.get("value");
                }
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to determine Java version of remote JVM at ["
                    + connection.getConnectionProvider().getConnectionSettings().getServerUrl() + "].", e);
        }
        return version;
    }

    // For now, this method is not used. This method is the ClassLoaderFacet method, but I commented
    // out the fact that this class implements that interface. As of today, 7/20/2009, I'm not sure we really
    // need to implement the classloader facet since EMS's ability to use additionalClasspathEntries in its
    // classloaders is all we need today to support JMX Server resource types. But I have a feeling it may be
    // necessary in the future to allow plugins to extend the JMX Server resource type and have it specify
    // "classLoaderType='instance'" in its plugin descriptor - if that use-case is ever needed, we'll want to
    // have this class implement ClassLoaderFacet which then brings this method in use (nothing would need to
    // be changed other than to add "implements ClassLoaderFacet" to the class definition).
    // For now, since we don't want the additional overhead of calling into this method when we currently have
    // no need for it, we do not implement the ClassLoaderFacet.
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext<ResourceComponent<?>> context,
                                                DiscoveredResourceDetails details) throws Exception {
        List<File> jars = ConnectionProviderFactory.getAdditionalJarsFromConfig(details.getPluginConfiguration());
        if (jars == null || jars.isEmpty()) {
            return null;
        }

        List<URL> urls = new ArrayList<URL>(jars.size());
        for (File jar : jars) {
            urls.add(jar.toURI().toURL());
        }

        return urls;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        JvmResourceKey oldKey = JvmResourceKey.valueOf(inventoriedResource.getResourceKey());
        JvmResourceKey.Type oldKeyType = oldKey.getType();
        if (oldKeyType == JvmResourceKey.Type.Legacy || oldKeyType == JvmResourceKey.Type.JmxRemotingPort) {
            if (!inventoriedResource.getSystemInformation().isNative()) {
                log.warn("Cannot attempt to upgrade Resource key [" + inventoriedResource.getResourceKey()
                    + "] of JVM Resource, because this Agent is not running with native system info support (i.e. SIGAR).");
                return null;
            }

            Configuration pluginConfig = inventoriedResource.getPluginConfiguration();
            String connectorAddress = pluginConfig.getSimpleValue(CONNECTOR_ADDRESS_CONFIG_PROPERTY, null);
            JMXServiceURL jmxServiceURL;
            try {
                jmxServiceURL = new JMXServiceURL(connectorAddress);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to parse connector address: " + connectorAddress, e);
            }

            JMXConnector jmxConnector = null;
            Long pid;
            try {
                jmxConnector = connect(jmxServiceURL);
                MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();
                RuntimeMXBean runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(mbeanServerConnection,
                    ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
                pid = getJvmPid(runtimeMXBean);
                if (pid == null) {
                    throw new RuntimeException("Failed to determine JVM pid by parsing JVM name.");
                }
            } catch (SecurityException e) {
                // Authentication failed, which most likely means the username and password are not set correctly in
                // the Resource's plugin config. This is not an error, so return null.
                log.info("Unable to upgrade key of JVM Resource with key [" + inventoriedResource.getResourceKey()
                        + "], since authenticating to its JMX service URL [" + jmxServiceURL + "] failed: "
                        + e.getMessage());
                return null;
            } catch (IOException e) {
                // The JVM's not currently running, which means we won't be able to figure out its main class name,
                // which is needed to upgrade its key. This is not an error, so return null.
                log.debug("Unable to upgrade key of JVM Resource with key [" + inventoriedResource.getResourceKey()
                        + "], since connecting to its JMX service URL [" + jmxServiceURL + "] failed: " + e);
                return null;
            } finally {
                close(jmxConnector);
            }

            List<ProcessInfo> processes = inventoriedResource.getSystemInformation().getProcesses(
                "process|pid|match=" + pid);
            if (processes.size() != 1) {
                throw new IllegalStateException("Failed to find process with PID [" + pid + "].");
            }
            ProcessInfo process = processes.get(0);
            String mainClassName = getJavaMainClassName(process);
            String explicitKeyValue = getSystemPropertyValue(process, SYSPROP_RHQ_RESOURCE_KEY);
            if (oldKeyType == JvmResourceKey.Type.Legacy || explicitKeyValue != null) {
                // We need to upgrade the key.
                JvmResourceKey newKey;
                if (explicitKeyValue != null) {
                    newKey = JvmResourceKey.fromExplicitValue(mainClassName, explicitKeyValue);
                } else {
                    newKey = JvmResourceKey.fromJmxRemotingPort(mainClassName, oldKey.getJmxRemotingPort());
                }

                ResourceUpgradeReport resourceUpgradeReport = new ResourceUpgradeReport();
                resourceUpgradeReport.setNewResourceKey(newKey.toString());
                return resourceUpgradeReport;
            }
        }

        return null;
    }

    private void close(JMXConnector jmxConnector) {
        try {
            if (jmxConnector != null)
                jmxConnector.close();
        } catch (Exception e) {}
    }

    private static Long getJvmPid(RuntimeMXBean runtimeMXBean) {
        Long pid;
        String jvmName = runtimeMXBean.getName();
        int atIndex = jvmName.indexOf('@');
        pid = (atIndex != -1) ? Long.valueOf(jvmName.substring(0, atIndex)) : null;
        return pid;
    }

    protected DiscoveredResourceDetails discoverResourceDetails(ResourceDiscoveryContext context, ProcessInfo process) {
        Integer jmxRemotingPort = getJmxRemotingPort(process);
        JMXServiceURL jmxServiceURL = null;
        if (jmxRemotingPort != null) {
            // Use JMX Remoting when possible, since it doesn't require the RHQ Agent to have OS-level permissions to
            // communicate with the remote JVM via IPC.
            try {
                jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxRemotingPort +  "/jmxrmi");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            // If JMX Remoting is not enabled, it's required that a Resource key is explicitly specified via the org.rhq.resourceKey sysprop.
            String keyString = getSystemPropertyValue(process, SYSPROP_RHQ_RESOURCE_KEY);
            if (keyString != null && !keyString.equals("")) {
                // Start up a JMX agent within the JVM via the Sun Attach API, and return a URL that can be used to connect
                // to that agent.
                // Note, this will only work if the remote JVM is Java 6 or later, and maybe some 64 bit Java 5 - see
                // JBNADM-3332. Also, the RHQ Agent will have to be running on a JDK, not a JRE, so that we can access
                // the JDK's tools.jar, which contains the Sun JVM Attach API classes.
                jmxServiceURL = JvmUtility.extractJMXServiceURL(process);
                if (jmxServiceURL == null) {
                    return null;
                }
            }
        }
        log.debug("JMX service URL for java process [" + process + "] is [" + jmxServiceURL + "].");

        return buildResourceDetails(context, process, jmxServiceURL, jmxRemotingPort);
    }

    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext context, ProcessInfo process,
                                                             JMXServiceURL jmxServiceURL, Integer jmxRemotingPort) {
        JvmResourceKey key = buildResourceKey(process, jmxRemotingPort);
        if (key == null) {
            return null;
        }
        String name = buildResourceName(key);
        String version = getJavaVersion(process, jmxServiceURL);
        String description = "JVM, monitored via " + ((jmxRemotingPort != null) ? "JMX Remoting" : "Sun JVM Attach API");

        Configuration pluginConfig = context.getDefaultPluginConfiguration();
        pluginConfig.put(new PropertySimple(CONNECTION_TYPE, J2SE5ConnectionTypeDescriptor.class.getName()));
        if (jmxRemotingPort != null) {
            pluginConfig.put(new PropertySimple(CONNECTOR_ADDRESS_CONFIG_PROPERTY, jmxServiceURL));
        }

        return new DiscoveredResourceDetails(context.getResourceType(), key.toString(), name, version, description,
            pluginConfig, process);
    }

    private JvmResourceKey buildResourceKey(ProcessInfo process, Integer jmxRemotingPort) {
        JvmResourceKey key;
        String mainClassName = getJavaMainClassName(process);
        String keyString = getSystemPropertyValue(process, SYSPROP_RHQ_RESOURCE_KEY);
        if (keyString != null && !keyString.equals("")) {
            log.debug("Using explicitly specified Resource key: [" + keyString + "]...");
            key = JvmResourceKey.fromExplicitValue(mainClassName, keyString);
        } else {
            if (jmxRemotingPort != null) {
                log.debug("Using JMX remoting port [" + jmxRemotingPort + "] as Resource key...");
                key = JvmResourceKey.fromJmxRemotingPort(mainClassName, jmxRemotingPort);
            } else {
                log.debug("Process [" + process.getPid() + "] with command line ["
                    + Arrays.asList(process.getCommandLine())
                    + "] cannot be discovered, because it does not specify either of the following system properties: "
                    + "-D" + SYSPROP_JMXREMOTE_PORT + "=12345, -D" + SYSPROP_RHQ_RESOURCE_KEY + "=UNIQUE_KEY");
                key = null;
            }
        }
        return key;
    }

    protected String getJavaVersion(ProcessInfo process, JMXServiceURL jmxServiceURL) {
        JMXConnector jmxConnector = null;
        try {
            jmxConnector = connect(jmxServiceURL);
            return getJavaVersion(jmxConnector);
        } catch (SecurityException e) {
            log.warn("Unable to to authenticate to JMX service URL [" + jmxServiceURL + "]: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to connect to JMX service URL [" + jmxServiceURL + "].", e);
        } catch (Exception e) {
            log.error("Failed to determine JVM version for process [" + process.getPid() + "] with command line [" +
                Arrays.asList(process.getCommandLine()) + "].", e);
        } finally {
            close(jmxConnector);
        }
        // TODO: We could exec "java -version" here.
        return null;
    }

    protected String getJavaVersion(JMXConnector jmxConnector) throws Exception {
        String version;
        MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();
        RuntimeMXBean runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(mbeanServerConnection,
                ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
        version = runtimeMXBean.getSystemProperties().get(SYSPROP_JAVA_VERSION);
        if (version == null) {
            throw new IllegalStateException("System property [" + SYSPROP_JAVA_VERSION + "] is not defined.");
        }
        return version;
    }

    private static JMXConnector connect(JMXServiceURL jmxServiceURL) throws IOException {
        JMXConnector jmxConnector;
        try {
            jmxConnector = JMXConnectorFactory.connect(jmxServiceURL);
        } catch (IOException e) {
            throw new IOException("Failed to connect to JMX service URL [" + jmxServiceURL + "].");
        }
        return jmxConnector;
    }

    private String buildResourceName(JvmResourceKey key) {
        StringBuilder name = new StringBuilder();
        String mainClassName = key.getMainClassName();
        if (mainClassName != null) {
            if (mainClassName.length() <= 200) {
                name.append(mainClassName);
            } else {
                // Truncate it if it's really long for a more palatable Resource name.
                name.append(mainClassName.substring(mainClassName.length() - 200));
            }
        }

        //build the resource names from supported JvmResourceKey instances. See JvmResourceKey.Type for more details.
        switch (key.getType()) {
        case Legacy: // implies main classname was not found. Include earlier naming format as well.
            name.append("JMX Server (" + key.getJmxRemotingPort() + ")");
            break;
        case ConnectorAddress:
            name.append(key.getConnectorAddress());
            break;
        case JmxRemotingPort:
            name.append(':').append(key.getJmxRemotingPort());
            break;
        case Explicit:
            name.append(' ').append(key.getExplicitValue());
            break;
        default:
            throw new IllegalStateException("Unsupported key type: " + key.getType());
        }

        return name.toString();
    }

    protected String getJavaMainClassName(ProcessInfo process) {
        // TODO (ips, 04/02/12): If command line contains "-jar foo.jar", pull the main class name out of foo.jar's
        //                       MANIFEST.MF.
        String className = null;
        for (int i = 1; i < process.getCommandLine().length; i++) {
            String arg = process.getCommandLine()[i];

            if (!arg.startsWith("-")) {
                className = arg;
                break;
            } else if (arg.equals("-cp") || arg.equals("-classpath")) {
                // The next arg is the classpath - skip it.
                i++;
            }
        }
        return className;
    }

    protected Integer getJmxRemotingPort(ProcessInfo process) {
        String value = getSystemPropertyValue(process, SYSPROP_JMXREMOTE_PORT);
        if (value != null) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    protected String getSystemPropertyValue(ProcessInfo process, String systemPropertyName) {
        for (String argument : process.getCommandLine()) {
            String prefix = "-D" + systemPropertyName + "=";
            if (argument.startsWith(prefix)) {
                return argument.substring(prefix.length());
            }
        }
        return null;
    }

}
