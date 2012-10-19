package org.rhq.plugins.jmx.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.local.LocalVMFinder;
import org.mc4j.ems.connection.local.LocalVirtualMachine;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.ConnectionTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.LocalVMTypeDescriptor;

/**
 * A factory that can construct an EMS {@link ConnectionProvider} for a JMX Server Resource from that Resource's plugin
 * configuration.
 *
 * @author Ian Springer
 */
public class ConnectionProviderFactory {

    public static ConnectionProvider createConnectionProvider(Configuration pluginConfig, ProcessInfo process,
                                                              File tempDir) throws Exception {
        String connectionTypeDescriptorClassName = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
                    .getStringValue();

        if (connectionTypeDescriptorClassName.equals("LocalVMTypeDescriptor")) {
            connectionTypeDescriptorClassName = LocalVMTypeDescriptor.class.getName();
        }

        Class<?> connectionTypeDescriptorClass;
        try {
            connectionTypeDescriptorClass = Class.forName(connectionTypeDescriptorClassName);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Invalid connection type - class [" + connectionTypeDescriptorClassName
                            + "] not found.");
        }
        if (!(ConnectionTypeDescriptor.class.isAssignableFrom(connectionTypeDescriptorClass))) {
            throw new InvalidPluginConfigurationException("Invalid connection type - class [" + connectionTypeDescriptorClassName
                + "] does not implement the " + ConnectionTypeDescriptor.class.getName() + " interface.");
        }
        ConnectionTypeDescriptor connectionTypeDescriptor;
        try {
            connectionTypeDescriptor = (ConnectionTypeDescriptor) connectionTypeDescriptorClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate connection type descriptor of type [" + connectionTypeDescriptorClassName + "].", e);
        }

        ConnectionSettings settings = new ConnectionSettings();
        settings.initializeConnectionType(connectionTypeDescriptor);

        // Set principal and credentials.
        String principal = pluginConfig.getSimpleValue(JMXComponent.PRINCIPAL_CONFIG_PROP, null);
        settings.setPrincipal(principal);
        String credentials = pluginConfig.getSimpleValue(JMXComponent.CREDENTIALS_CONFIG_PROP, null);
        settings.setCredentials(credentials);

        if (connectionTypeDescriptor instanceof LocalVMTypeDescriptor) {
            // NOTE (ips, 01/19/12): This is not very reliable for long-term management of a JVM, since it uses the
            //                       command line from the time the JVM was originally discovered, which may have changed.
            String commandLine = pluginConfig.getSimpleValue(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY, null);
            if (commandLine == null) {
                throw new InvalidPluginConfigurationException("A command line is required for the "
                    + connectionTypeDescriptorClassName + " connection type.");
            }

            Map<Integer, LocalVirtualMachine> vms = LocalVMFinder.getManageableVirtualMachines();
            LocalVirtualMachine targetVm = null;
            if (vms != null) {
                for (LocalVirtualMachine vm : vms.values()) {
                    if (vm.getCommandLine().equals(commandLine)) {
                        targetVm = vm;
                        break;
                    }
                }
            }
            if (targetVm == null) {
                // This could just be because the JVM is not currently running.
                throw new Exception("JVM with command line [" + commandLine + "] not found.");
            }
            String vmId = String.valueOf(targetVm.getVmid());
            settings.setServerUrl(vmId);
        } else if (connectionTypeDescriptor instanceof J2SE5ConnectionTypeDescriptor) {
            // Connect via JMX Remoting, using the JVM Attach API to start up a JMX Remoting Agent if necessary.
            String jmxConnectorAddress = getJmxConnectorAddress(pluginConfig, process);
            settings.setServerUrl(jmxConnectorAddress);
        } else {
            // Handle internal connections (InternalVMTypeDescriptor) (i.e. the RHQ plugin container's own JVM), as
            // well as miscellaneous types of remote connections - WebSphere, WebLogic, etc.
            String connectorAddress = pluginConfig.getSimpleValue(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
                        null);
            if (connectorAddress == null) {
                throw new InvalidPluginConfigurationException("A connector address is required for the "
                    + connectionTypeDescriptorClassName + " connection type.");
            }
            settings.setServerUrl(connectorAddress);
            String installURI = pluginConfig.getSimpleValue(JMXDiscoveryComponent.INSTALL_URI, null);
            settings.setLibraryURI(installURI);
        }

        addAdditionalJarsToConnectionSettings(settings, pluginConfig);

        return createConnectionProvider(settings, tempDir);
    }

    private static ConnectionProvider createConnectionProvider(ConnectionSettings settings, File tempDir) {
        settings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP, String.valueOf(Boolean.TRUE));
        settings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
            tempDir.getAbsolutePath());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.discoverServerClasses(settings);

        return connectionFactory.getConnectionProvider(settings);
    }

    private static void addAdditionalJarsToConnectionSettings(ConnectionSettings settings, Configuration pluginConfig) {

        List<File> additionalEntries = getAdditionalJarsFromConfig(pluginConfig);
        if (additionalEntries == null || additionalEntries.size() == 0) {
            return; // nothing to do, there are no additional entries to add
        }

        // get the setting's current list of classpath entries - we are going to add to these
        List<File> settingsEntries = settings.getClassPathEntries();
        if (settingsEntries == null) {
            settingsEntries = new ArrayList<File>();
        }

        // append the additional entries to the end of the setting's current entries
        settingsEntries.addAll(additionalEntries);

        // now that we've appended our additional jars, tell the connection settings about the new list
        settings.setClassPathEntries(settingsEntries);
    }

    private static String getJmxConnectorAddress(Configuration pluginConfig, ProcessInfo process) throws Exception {
        String connectorAddress = pluginConfig.getSimpleValue(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
            null);
        if (connectorAddress == null) {
            // No JMX connector address defined - try to connect via Attach API.
            if (process == null) {
                throw new Exception("Could not find java process for JVM.");
            }
            JMXServiceURL jmxServiceURL = JvmUtility.extractJMXServiceURL(process);
            if (jmxServiceURL == null) {
                throw new Exception("Could not obtain JMX service URL via Attach API for JVM [" + process + "].");
            }
            connectorAddress = jmxServiceURL.toString();
        }
        return connectorAddress;
    }

    /**
     * Examines the plugin configuration and if it defines additional classpath entries, this
     * will return a list of files that point to all the jars that need to be added to a classloader
     * to support the managed JMX resource.
     *
     * Note: this is package static scoped so the resource component can use this method.
     *
     * @param pluginConfiguration
     *
     * @return list of files pointing to additional jars; will be empty if no additional jars are to be added
     */
    public static List<File> getAdditionalJarsFromConfig(Configuration pluginConfiguration) {
        List<File> jarFiles = new ArrayList<File>();

        // get the plugin config setting that contains comma-separated list of files/dirs to additional jars
        // if no additional classpath entries are specified, we'll return an empty list
        PropertySimple prop = pluginConfiguration.getSimple(JMXDiscoveryComponent.ADDITIONAL_CLASSPATH_ENTRIES);
        if (prop == null || prop.getStringValue() == null || prop.getStringValue().trim().length() == 0) {
            return jarFiles;
        }
        String[] paths = prop.getStringValue().trim().split(",");
        if (paths == null || paths.length == 0) {
            return jarFiles;
        }

        // Get all additional classpath entries which can be listed as jar file names or directories.
        // If a directory has ".jar" at the end, all jar files found in that directory will be added
        // as class path entries.
        final class JarFilenameFilter implements FilenameFilter {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        }

        for (String path : paths) {
            path = path.trim();
            if (path.length() > 0) {
                if (path.endsWith("*.jar")) {
                    path = path.substring(0, path.length() - 5);
                    File dir = new File(path);
                    File[] jars = dir.listFiles(new JarFilenameFilter());
                    if (jars != null && jars.length > 0) {
                        jarFiles.addAll(Arrays.asList(jars));
                    }
                } else {
                    File pathFile = new File(path);
                    jarFiles.add(pathFile);
                }
            }
        }

        return jarFiles;
    }

}
