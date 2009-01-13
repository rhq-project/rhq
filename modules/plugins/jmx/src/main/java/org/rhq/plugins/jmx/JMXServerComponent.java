/*
 * RHQ Management Platform
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
package org.rhq.plugins.jmx;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.local.LocalVMFinder;
import org.mc4j.ems.connection.local.LocalVirtualMachine;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.ConnectionTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.LocalVMTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * The generic JMX server component used to create and cache a connection to a local or
 * remote JMX MBeanServer. This component is responsible for building an isolated connection/classloader
 * to the managed resource's JMX MBeanServer. Each connection is isolated from other connections
 * created by other instances of this component. This allows for it to do things like manage
 * multiple JBossAS servers that are running on the same box, even if they are of different JBossAS
 * versions. The same holds true for Hibernate applications - multiple connections can be created
 * to different versions of the Hibernate MBean and due to the isolation of each connection, there
 * are no version incompatibility errors that will occur.
 *  
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class JMXServerComponent implements JMXComponent {
    private static Log log = LogFactory.getLog(JMXServerComponent.class);

    private EmsConnection connection;
    private ConnectionProvider connectionProvider;

    ResourceContext context;

    public void start(ResourceContext context) throws Exception {
        this.context = context;
        log.info("Starting connection to JMX Server " + context.getResourceKey());

        try {
            internalStart();
        } catch (Exception e) {
            log.warn("JMX Plugin connection failure", e);
            // The new model is to always succeed in starting, but warn about the errors (we only do this the first request)
            /*throw new Exception("Unable to connect to Java VM ["
                + configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY).getStringValue()
                + "]", e);*/
        }

        if (connection == null) {
            log.warn("Unable to connect to JMX Server " + context.getResourceKey());
        }
    }

    protected void internalStart() throws Exception {
        Configuration configuration = context.getPluginConfiguration();

        String connectionTypeDescriptorClass = configuration.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
            .getStringValue();

        if (LocalVMTypeDescriptor.class.getName().equals(connectionTypeDescriptorClass)) {
            String commandLine = configuration.getSimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY)
                .getStringValue();

            Map<Integer, LocalVirtualMachine> vms = LocalVMFinder.getManageableVirtualMachines();
            if (vms != null) {
                for (LocalVirtualMachine vm : vms.values()) {
                    if (vm.getCommandLine().equals(commandLine)) {
                        connectLocal(vm.getVmid());
                    }
                }
            }
        } else if (JMXDiscoveryComponent.PARENT_TYPE.equals(connectionTypeDescriptorClass)) {
            // We're embedded in another jmx server component without jmxremoting set so just use the parent's connection
            this.connection = ((JMXComponent) context.getParentResourceComponent()).getEmsConnection();
            this.connectionProvider = this.connection.getConnectionProvider();
        } else if (J2SE5ConnectionTypeDescriptor.class.getName().equals(connectionTypeDescriptorClass)) {
            // We're embedded in a J2SE VM with jmxremote defined (e.g. for jconsole usage)
            String principal = null;
            String credentials = null;
            PropertySimple o = configuration.getSimple(JMXComponent.PRINCIPAL_CONFIG_PROP);
            if (o != null) {
                principal = o.getStringValue();
            }
            o = configuration.getSimple(JMXComponent.CREDENTIALS_CONFIG_PROP);
            if (o != null) {
                credentials = o.getStringValue();
            }

            ConnectionSettings settings = new ConnectionSettings();
            J2SE5ConnectionTypeDescriptor desc = new J2SE5ConnectionTypeDescriptor();
            settings.setConnectionType(desc);
            settings.setServerUrl(configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY)
                .getStringValue());
            if (principal != null) {
                settings.setPrincipal(principal);
            }
            if (credentials != null) {
                settings.setCredentials(credentials);
            }

            prepareConnection(settings);

        } else {
            // This can handle internal connections (within the same vm as the plugin container) as well as
            // any remote connections
            ConnectionSettings settings = new ConnectionSettings();

            settings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(connectionTypeDescriptorClass)
                .newInstance());

            settings.setConnectionType((ConnectionTypeDescriptor) Class.forName(connectionTypeDescriptorClass)
                .newInstance());
            settings.setServerUrl(configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY)
                .getStringValue());

            String installPath = configuration.getSimpleValue(JMXDiscoveryComponent.INSTALL_URI, null);
            if (installPath != null) {
                settings.setLibraryURI(configuration.getSimple(JMXDiscoveryComponent.INSTALL_URI).getStringValue());
            }

            prepareConnection(settings);
        }

        this.connection.loadSynchronous(false);

    }

    public void stop() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    protected void connectLocal(int vmid) {
        // TODO GH: Refactor ems to also accept the vm itself
        ConnectionSettings settings = new ConnectionSettings();
        settings.setConnectionType(new LocalVMTypeDescriptor());
        settings.setServerUrl(String.valueOf(vmid));
        prepareConnection(settings);
    }

    protected void prepareConnection(ConnectionSettings settings) {
        settings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP, String.valueOf(Boolean.TRUE));
        settings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
            this.context.getTemporaryDirectory().getAbsolutePath());

        addAdditionalJarsToConnectionSettings(settings);

        ConnectionFactory cf = new ConnectionFactory();
        cf.discoverServerClasses(settings);

        this.connectionProvider = cf.getConnectionProvider(settings);
        this.connection = this.connectionProvider.connect();
    }

    public EmsConnection getEmsConnection() {
        return this.connection;
    }

    public AvailabilityType getAvailability() {
        if (connectionProvider == null || !connectionProvider.isConnected()) {
            try {
                internalStart();
            } catch (Exception e) {
                log.debug("Still unable to reconnect resource: " + context.getResourceKey() + " due to error: "
                    + e.getMessage());
            }
        }

        return ((connectionProvider != null) && connectionProvider.isConnected()) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    public List<Resource> discoverServices(ResourceType type, Configuration defaultPluginConfiguration) {
        defaultPluginConfiguration.getSimple("objectName").getStringValue();

        return null;
    }

    private void addAdditionalJarsToConnectionSettings(ConnectionSettings settings) {
        // get the plugin config setting that contains comma-separated list of files/dirs to additional jars
        // if no additional classpath entries are specified, we'll return immediately and not do anything to settings
        Configuration pc = context.getPluginConfiguration();
        PropertySimple prop = pc.getSimple(JMXDiscoveryComponent.ADDITIONAL_CLASSPATH_ENTRIES);
        if (prop == null || prop.getStringValue() == null || prop.getStringValue().trim().length() == 0) {
            return;
        }
        String[] paths = prop.getStringValue().trim().split(",");
        if (paths == null || paths.length == 0) {
            return;
        }

        // get the current set of class path entries - we are going to add to these
        List<File> entries = settings.getClassPathEntries();
        if (entries == null) {
            entries = new ArrayList<File>();
        }

        // Get all additional classpath entries which can be listed as jar filenames or directories.
        // If a directory has "/*.jar" at the end, all jar files found in that directory will be added
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
                        entries.addAll(Arrays.asList(jars));
                    }
                } else {
                    File pathFile = new File(path);
                    entries.add(pathFile);
                }
            }
        }

        // now that we've appended our additional jars, tell the connection settings about the new list
        settings.setClassPathEntries(entries);

        return;
    }
}