/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.jbossas7jmx;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.JSR160ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Provides a class you can subclass to discover JMX MBeans from within an AS7 app server.
 *
 * @author Jay Shaughnessy
 */
public abstract class AbstractDiscoveryComponent<T extends ResourceComponent<?>> implements
    ResourceDiscoveryComponent<T> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context) {

        RemoteJMXComponent<T> jmxParentComponent = new RemoteJMXComponent<T>(context);

        MBeanResourceDiscoveryComponent<RemoteJMXComponent<T>> discoveryComponent = new MBeanResourceDiscoveryComponent<RemoteJMXComponent<T>>();

        return discoveryComponent.performDiscovery(context.getDefaultPluginConfiguration(), jmxParentComponent,
            context.getResourceType());
    }

    private static class RemoteJMXComponent<T extends ResourceComponent<?>> implements JMXComponent<T> {

        private final Log log = LogFactory.getLog(RemoteJMXComponent.class);

        private ResourceDiscoveryContext<T> context;
        private T component;
        private EmsConnection connection;

        /**
         * Controls the dampening of connection error stack traces in an attempt to control spam to the log
         * file. Each time a connection error is encountered, this will be incremented. When the connection
         * is finally established, this will be reset to zero.
         */
        private int consecutiveConnectionErrors;

        public RemoteJMXComponent(ResourceDiscoveryContext<T> context) {
            this.context = context;
            this.component = context.getParentResourceComponent();
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
            this.component.start(context);
        }

        @Override
        public void stop() {
            this.component.stop();
        }

        @Override
        public AvailabilityType getAvailability() {
            return this.component.getAvailability();
        }

        @Override
        public EmsConnection getEmsConnection() {
            EmsConnection emsConnection = null;

            try {
                emsConnection = loadConnection();
            } catch (Exception e) {
                log.error("Component attempting to access a connection that could not be loaded");
            }

            return emsConnection;
        }

        public EmsConnection loadConnection() throws Exception {
            if (connection != null) {
                return connection;
            }

            try {

                Configuration pluginConfig = context.getParentResourceContext().getPluginConfiguration();
                String hostname = pluginConfig.getSimpleValue("hostname", "localhost");
                String port = pluginConfig.getSimpleValue("port", "9999");
                if (!"9999".equals(port)) {
                    port = String.valueOf(Integer.valueOf(port) + 9);
                }
                String username = pluginConfig.getSimpleValue("username", "rhqadmin");
                String password = pluginConfig.getSimpleValue("password", "rhqadmin");

                ConnectionSettings connectionSettings = new ConnectionSettings();
                connectionSettings.initializeConnectionType(new JSR160ConnectionTypeDescriptor());
                connectionSettings.setServerUrl("service:jmx:remoting-jmx://" + hostname + ":" + port);
                connectionSettings.setPrincipal(username);
                connectionSettings.setCredentials(password);
                //String connectionTypeDescriptorClass = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
                //    .getStringValue();

                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.discoverServerClasses(connectionSettings);

                if (connectionSettings.getAdvancedProperties() == null) {
                    connectionSettings.setAdvancedProperties(new Properties());
                }

                // Tell EMS to make copies of jar files so that the ems classloader doesn't lock
                // application files (making us unable to update them)  Bug: JBNADM-670
                // TODO GH: turn this off in the embedded case
                //connectionSettings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP,
                //    String.valueOf(Boolean.TRUE));

                // But tell it to put them in a place that we clean up when shutting down the agent
                //connectionSettings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
                //    context.getParentResourceContext().getTemporaryDirectory().getAbsolutePath());

                //connectionSettings.getAdvancedProperties().setProperty(InternalVMTypeDescriptor.DEFAULT_DOMAIN_SEARCH,
                //    "jboss");

                log.info("Loading AS7 connection [" + connectionSettings.getServerUrl() + "] with install path ["
                    + connectionSettings.getLibraryURI() + "]...");

                ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
                this.connection = connectionProvider.connect();

                //connectionSettings.getAdvancedProperties().setProperty(JNP_DISABLE_DISCOVERY_JNP_INIT_PROP, "true");

                // Make sure the timeout always happens, even if the JBoss server is hung.
                //connectionSettings.getAdvancedProperties().setProperty("jnp.timeout", String.valueOf(JNP_TIMEOUT));
                //connectionSettings.getAdvancedProperties().setProperty("jnp.sotimeout", String.valueOf(JNP_SO_TIMEOUT));

                /*
                try {
                    org.mc4j.ems.impl.JMXRemotingConnectionProvider foo;
                    JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:remoting-jmx://" + hostname + ":" + port);
                    Map<String, String[]> env = new HashMap<String, String[]>();
                    env.put(JMXConnector.CREDENTIALS, new String[] { username, password });
                    RemotingConnectorProvider provider = new RemotingConnectorProvider();
                    JMXConnector connector = provider.newJMXConnector(serviceURL, env);
                    MBeanServerConnection conn = connector.getMBeanServerConnection();

                } catch (Exception e) {
                }
                */
                if (log.isDebugEnabled())
                    log.debug("Successfully made connection to the AS7 instance for resource ["
                        + context.getParentResourceContext().getResourceKey() + "]");

                return connection;

            } catch (Exception e) {

                // The connection will be established even in the case that the principal cannot be authenticated,
                // but the connection will not work. That failure seems to come from the call to loadSynchronous after
                // the connection is established. If we get to this point that an exception was thrown, close any
                // connection that was made and null it out so we can try to establish it again.
                if (connection != null) {
                    if (log.isDebugEnabled())
                        log.debug("Connection created but an exception was thrown. Closing the connection.", e);
                    connection.close();
                    connection = null;
                }

                // Since the connection is attempted each time it's used, failure to connect could result in log
                // file spamming. Log it once for every 10 consecutive times it's encountered.
                if (consecutiveConnectionErrors % 10 == 0) {
                    log.warn("Could not establish connection to the JBoss AS instance ["
                        + (consecutiveConnectionErrors + 1) + "] times for resource ["
                        + context.getParentResourceContext().getResourceKey() + "]", e);
                }

                if (log.isDebugEnabled())
                    log.debug("Could not connect to the JBoss AS instance for resource ["
                        + context.getParentResourceContext().getResourceKey() + "]", e);

                consecutiveConnectionErrors++;

                throw e;
            }
        }
    }
}