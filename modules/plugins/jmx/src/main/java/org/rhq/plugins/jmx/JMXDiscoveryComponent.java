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
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
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
import org.rhq.plugins.jmx.util.Socket;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;

/**
 * This component will discover JVM processes that appear to be long-running (i.e. "servers"). Specifically, it will
 * discover java processes that:
 * <ul>
 *  <li>have enabled JMX Remoting (JSR-160) via com.sun.management.jmxremote* system properties on their command lines, or</li>
 *  <li>are Sun/Oracle-compatible java processes accessible via the com.sun.tools.attach API AND specify the
 *      org.rhq.resourceKey system property on their command lines (e.g. -Dorg.rhq.resourceKey=FOO); the attach API uses IPC
 *      under the covers, so for a process to be accessible, it either must be running as the same user as the RHQ Agent,
 *      or the Agent must be running as root<li>
 * </ul>
 * Some other java processes that do not meet these criteria can be manually added if they expose JMX remotely in
 * another supported form (WebLogic, WebSphere, etc.).
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class JMXDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet, ResourceUpgradeFacet { //, ClassLoaderFacet {

    private static final Log log = LogFactory.getLog(JMXDiscoveryComponent.class);

    // our own private SIGAR
    // TODO (ips, 01/05/12): enhance native-system API, then use that instead of using SIGAR directly
    private static Sigar SIGAR;
    static {
        try {
            SIGAR = new Sigar();
        } catch (RuntimeException re) {
            SIGAR = null;
        }
    }

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
     * Ignore certain processes that are managed by their own plugin. For example, the Tomcat plugin will
     * handle Tomcat processes configured for JMX management.
     */
    private static final String[] DEFAULT_PROCESS_EXCLUDES = new String[]{"org.jboss.Main", "catalina.startup.Bootstrap"};

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
            if (javaProcess.getProcessInfo().equals(context.getSystemInformation().getThisProcess())) {
                // If the process is our own process (i.e. the RHQ Agent JVM), then skip it, since the rhq-agent
                // plugin will handle discovering that.
                continue;
            }
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
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext discoveryContext)
            throws InvalidPluginConfigurationException {
        
        String connectorAddress = pluginConfig.getSimpleValue(CONNECTOR_ADDRESS_CONFIG_PROPERTY, null);
        if (connectorAddress == null) {
            throw new InvalidPluginConfigurationException("A connector address must be specified when manually adding a JMX Server.");
        }
        
        ConnectionProvider connectionProvider;
        EmsConnection connection;
        try {
            connectionProvider = ConnectionProviderFactory.createConnectionProvider(pluginConfig, null,
                discoveryContext.getParentResourceContext().getTemporaryDirectory());
            connection = connectionProvider.connect();
            connection.loadSynchronous(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connection to connector address [" + connectorAddress + "].", e);
        }
                        
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
    }

    private String getJavaVersion(EmsConnection connection) {
        String version = null;
        EmsBean runtimeMXBean = connection.getBean(ManagementFactory.RUNTIME_MXBEAN_NAME);
        if (runtimeMXBean != null) {
            EmsAttribute systemPropertiesAttribute = runtimeMXBean.getAttribute("systemProperties");
            if (systemPropertiesAttribute != null) {
                Map<String, String> systemProperties = (Map<String, String>) systemPropertiesAttribute.getValue();
                version = systemProperties.get("java.version");
            }
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

            Long pid;
            try {
                JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL);
                MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();                
                RuntimeMXBean runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(mbeanServerConnection,
                    ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
                pid = getJvmPid(runtimeMXBean);
                if (pid == null) {
                    throw new RuntimeException("Failed to determine JVM pid by parsing JVM name.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to determine JVM pid.", e);
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

    private static Long getJvmPid(RuntimeMXBean runtimeMXBean) {
        Long pid;
        String jvmName = runtimeMXBean.getName();
        int atIndex = jvmName.indexOf('@');
        pid = (atIndex != -1) ? Long.valueOf(jvmName.substring(0, atIndex)) : null;
        return pid;
    }

    protected DiscoveredResourceDetails discoverResourceDetails(ResourceDiscoveryContext context, ProcessInfo process) {
        Integer jmxRemotingPort = getJmxRemotingPort(process);
        JMXServiceURL jmxServiceURL;
        Set<Socket> serverSockets;
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
            if (keyString == null || keyString.equals("")) {
                serverSockets = getServerSockets(process);
                if (!serverSockets.isEmpty()) {
                    log.info("Server JVM process [" + process.getPid() + "] with command line ["
                        + Arrays.asList(process.getCommandLine())
                        + "] cannot be discovered, because it does not specify either of the following system properties: -D"
                        + SYSPROP_JMXREMOTE_PORT + "=JMX_REMOTING_PORT, -D" + SYSPROP_RHQ_RESOURCE_KEY + "=UNIQUE_KEY");
                    return null;
                }
            }

            // Start up a JMX agent within the JVM via the Sun Attach API, and return a URL that can be used to connect
            // to that agent.
            // Note, this will only work if the remote JVM is Java 6 or later, and maybe some 64 bit Java 5 - see
            // JBNADM-3332. Also, the RHQ Agent will have to be running on a JDK, not a JRE, so that we can access
            // the JDK's tools.jar, which contains the Sun JVM Attach API classes.
            jmxServiceURL = JvmUtility.extractJMXServiceURL(process.getPid());
        }

        if (jmxServiceURL != null) {
            log.debug("JMX service URL for process [" + process + "] is [" + jmxServiceURL + "].");
            return buildResourceDetails(context, process, jmxServiceURL, jmxRemotingPort);
        } else {
            return null;
        }
    }

    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext context, ProcessInfo process,
                                                             JMXServiceURL jmxServiceURL, Integer jmxRemotingPort) {
        JvmResourceKey key;                        
        String mainClassName = getJavaMainClassName(process);
        String value = getSystemPropertyValue(process, SYSPROP_RHQ_RESOURCE_KEY);
        if (value != null && !value.equals("")) {
            log.debug("Using explicitly specified Resource key: [" + value + "]...");
            key = JvmResourceKey.fromExplicitValue(mainClassName, value);
        } else {
            if (jmxRemotingPort != null) {
                log.debug("Using JMX remoting port [" + jmxRemotingPort + "] as Resource key...");
                key = JvmResourceKey.fromJmxRemotingPort(mainClassName, jmxRemotingPort);
            } else {
                log.info("Process [" + process.getPid() + "] with command line [" + Arrays.asList(process.getCommandLine()) + "] cannot be discovered, because it does not specify either of the following system properties: -Dcom.sun.management.jmxremote, -D" + SYSPROP_RHQ_RESOURCE_KEY + "=UNIQUE_KEY");
                return null;
            }
        }

        String name = buildResourceName(key);

        String version;
        try {
            JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL);
            MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();
            RuntimeMXBean runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(mbeanServerConnection,
                ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
            version = runtimeMXBean.getSystemProperties().get(SYSPROP_JAVA_VERSION);
            if (version == null) {
                throw new IllegalStateException("System property " + SYSPROP_JAVA_VERSION + " is not defined.");
            }
        } catch (Exception e) {
            log.error("Failed to determine JVM version.", e);
            version = null;
        }

        String description = "JVM, monitored via " + ((jmxRemotingPort != null) ? "JMX Remoting" : "Sun JVM Attach API");

        Configuration pluginConfig = context.getDefaultPluginConfiguration();
        pluginConfig.put(new PropertySimple(CONNECTION_TYPE, J2SE5ConnectionTypeDescriptor.class.getName()));
        if (jmxRemotingPort != null) {
            pluginConfig.put(new PropertySimple(CONNECTOR_ADDRESS_CONFIG_PROPERTY, jmxServiceURL));
        }        
        
        return new DiscoveredResourceDetails(context.getResourceType(), key.toString(), name, version, description,
            pluginConfig, process);
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

        switch (key.getType()) {
            case JmxRemotingPort:
                name.append(':').append(key.getJmxRemotingPort()); break;
            case Explicit:
                name.append(' ').append(key.getExplicitValue()); break;
            default:
                throw new IllegalStateException("Unsupported key type: " + key.getType());
        }

        return name.toString();
    }

    private Set<Socket> getServerSockets(ProcessInfo process) {
        Set<Socket> serverSockets = new HashSet<Socket>();
        if (SIGAR != null) {
            // Only request the type of connections we are interested in - TCP and UDP listen sockets.
            int netFlags = (NetFlags.CONN_TCP | NetFlags.CONN_UDP | NetFlags.CONN_SERVER);
            NetConnection[] connections;
            try {
                connections = SIGAR.getNetConnectionList(netFlags);
            } catch (SigarException e) {
                log.debug("Failed to get net connections.", e);
                connections = new NetConnection[0];
            }
            for (NetConnection connection : connections) {
                if (connection.getState() == NetFlags.TCP_LISTEN) {
                    try {
                        long pid = SIGAR.getProcPort(connection.getType(), connection.getLocalPort());
                        if (pid == process.getPid()) {
                            Socket.Protocol protocol = (connection.getType() == NetFlags.CONN_UDP) ?
                                Socket.Protocol.UDP : Socket.Protocol.TCP;
                            serverSockets.add(new Socket(protocol, connection.getLocalAddress(),
                                connection.getLocalPort()));
                        }
                    } catch (SigarException e) {
                        log.debug("Failed to get pid for connection [" + connection + "].", e);
                    }
                }
            }
        }
        return serverSockets;
    }

    protected String getJavaMainClassName(ProcessInfo process) {
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