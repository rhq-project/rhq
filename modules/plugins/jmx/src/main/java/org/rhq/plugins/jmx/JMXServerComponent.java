/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.jmx;

import java.util.ArrayList;
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
 * @author Greg Hinkle
 */
public class JMXServerComponent implements JMXComponent {
    private static Log log = LogFactory.getLog(JMXServerComponent.class);

    private EmsConnection connection;
    private ConnectionProvider connectionProvider;

    public void start(ResourceContext context) throws Exception {
        log.info("Starting connection to JMX Server " + context.getResourceKey());

        Configuration configuration = context.getPluginConfiguration();

        String connectionTypeDescriptorClass = configuration.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
            .getStringValue();

        try {
            if (LocalVMTypeDescriptor.class.getName().equals(connectionTypeDescriptorClass)) {
                String commandLine = configuration.getSimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY)
                    .getStringValue();

                List<Resource> found = new ArrayList<Resource>();

                Map<Integer, LocalVirtualMachine> vms = LocalVMFinder.getManageableVirtualMachines();
                if (vms != null) {
                    for (LocalVirtualMachine vm : vms.values()) {
                        if (vm.getCommandLine().equals(commandLine)) {
                            connectLocal(vm.getVmid(), context);
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

                ConnectionSettings cs = new ConnectionSettings();
                J2SE5ConnectionTypeDescriptor desc = new J2SE5ConnectionTypeDescriptor();
                cs.setConnectionType(desc);
                cs.setServerUrl(configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY)
                    .getStringValue());
                if (principal != null)
                    cs.setPrincipal(principal);
                if (credentials != null)
                    cs.setCredentials(credentials);
                ConnectionFactory cf = new ConnectionFactory();
                this.connection = cf.connect(cs);
                this.connectionProvider = this.connection.getConnectionProvider();

            } else {
                // This can handle internal connections (within the same vm as the plugin container) as well as
                // any remote connections
                ConnectionSettings settings = new ConnectionSettings();

                settings.setConnectionType((ConnectionTypeDescriptor) Class.forName(connectionTypeDescriptorClass)
                    .newInstance());
                settings.setServerUrl(configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY)
                    .getStringValue());
                settings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
                    context.getTemporaryDirectory().getAbsolutePath());
                ConnectionFactory cf = new ConnectionFactory();
                this.connectionProvider = cf.getConnectionProvider(settings);
                this.connection = connectionProvider.connect();
            }

            this.connection.loadSynchronous(false);
        } catch (Exception e) {
            log.warn("JMX Plugin connection failure", e);
            throw new Exception("Unable to connect to Java VM ["
                + configuration.getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY).getStringValue()
                + "]", e);
        }

        if (connection == null) {
            log.warn("Unable to connect to JMX Server " + context.getResourceKey());
        }
    }

    public void stop() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    protected void connectLocal(int vmid, ResourceContext context) {
        // TODO GH: Refactor ems to also accept the vm itself
        ConnectionSettings settings = new ConnectionSettings();
        settings.setConnectionType(new LocalVMTypeDescriptor());
        settings.setServerUrl(String.valueOf(vmid));
        ConnectionFactory connectionFactory = new ConnectionFactory();

        settings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP, String.valueOf(Boolean.TRUE));
        settings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
            context.getTemporaryDirectory().getAbsolutePath());
        this.connection = connectionFactory.connect(settings);
        this.connectionProvider = this.connection.getConnectionProvider();
    }

    public EmsConnection getEmsConnection() {
        return this.connection;
    }

    public AvailabilityType getAvailability() {
        return ((connectionProvider != null) && connectionProvider.isConnected()) ? AvailabilityType.UP
            : AvailabilityType.DOWN;
    }

    public List<Resource> discoverServices(ResourceType type, Configuration defaultPluginConfiguration) {
        defaultPluginConfiguration.getSimple("objectName").getStringValue();

        return null;
    }

    /*
     * public void initialize() {   connection = ConnectionFactory
     *
     * String url = config.getProperty(Context.PROVIDER_URL);
     *
     * ConnectionFactory connectionFactory = new ConnectionFactory();
     *
     * ConnectionSettings connectionSettings = new ConnectionSettings();
     * connectionSettings.initializeConnectionType(new J2SE5ConnectionTypeDescriptor());
     *
     * //connectionFactory.discoverServerClasses(connectionSettings);
     *
     * connectionSettings.setServerUrl(url);
     *
     * String principal = config.getProperty(Context.SECURITY_PRINCIPAL);   if (principal != null)
     * connectionSettings.setPrincipal(principal);
     *
     * String credentials = config.getProperty(Context.SECURITY_CREDENTIALS);   if (credentials != null)
     * connectionSettings.setCredentials(credentials);
     *
     * log.info("Loading JBoss connection [" +     config.getProperty(Context.PROVIDER_URL) +     "] with install path
     * [" +     config.getProperty("installpath"));   try   {      Class.forName("javax.management.MBeanServer");
     * ClassLoader cl = Thread.currentThread().getContextClassLoader();      if (cl != null)      {
     * Class.forName("javax.management.MBeanServer", false, cl);      }      log.warn("An MBeanServer class has be found
     * at the plugin level. MBeanServer classes should not be found here " +        "unless you are running JDK 5+.");
     * }   catch (ClassNotFoundException e)   {   }
     *
     * EmsConnection connection = connectionFactory.connect(connectionSettings);   // Do a *,* query
     * connection.loadSynchronous(false);
     *
     * connectionCache.put(key, connection);   return connection; }
     */
}