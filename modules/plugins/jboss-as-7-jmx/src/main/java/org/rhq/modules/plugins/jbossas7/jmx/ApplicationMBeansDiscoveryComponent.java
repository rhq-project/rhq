/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.jmx;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.rhq.modules.plugins.jbossas7.JBossProductType.WILDFLY8;
import static org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansDiscoveryComponent.PluginConfigProps.BEANS_QUERY_STRING;
import static org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansDiscoveryComponent.PluginConfigProps.NEW_RESOURCE_DESCRIPTION;
import static org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansDiscoveryComponent.PluginConfigProps.NEW_RESOURCE_NAME;
import static org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansDiscoveryComponent.PluginConfigProps.NEW_RESOURCE_VERSION;

import java.io.File;
import java.util.Collections;
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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.BaseServerComponent;
import org.rhq.modules.plugins.jbossas7.JBossProductType;
import org.rhq.modules.plugins.jbossas7.ManagedASComponent;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;

/**
 * Discovery class for the container {@link ApplicationMBeansComponent} component class.
 *
 * <h3>Custom plugin setup</h3>
 *
 * <p>This class is <strong>not</strong> declared in the AS7 plugin descriptor. It is a tool that that plugin developers
 * may use while writing their <strong>own</strong> plugin to monitor their application MBeans.</p>
 *
 * <p>The custom plugin <strong>must</strong>:</p>
 * <ul>
 *     <li>depend on the AS7 plugin and re-use its classes (<em>useClasses</em> attribute set to true in the plugin
 *     descriptor);</li>
 *     <li>embed the JMX plugin as a library.</li>
 * </ul>
 *
 * <p>The resource type using this class (or a subclass of this class) as a discovery component must indicate which type
 * of server it runs inside. It may be either a <em>JBossAS7 Standalone Server</em> or a <em>Managed Server</em>.</p>
 * 
 * <p>Plugins authors are <em>required</em> to provide a resource key and name and <em>may</em>provide a resource
 * description and version by declaring plugin configuration properties of the following names:</p>
 * 
 * <ul>
 *     <li>{@link PluginConfigProps#NEW_RESOURCE_KEY}</li>
 *     <li>{@link PluginConfigProps#NEW_RESOURCE_NAME}</li>
 *     <li>{@link PluginConfigProps#NEW_RESOURCE_DESCRIPTION}</li>
 *     <li>{@link PluginConfigProps#NEW_RESOURCE_VERSION}</li>
 * </ul>
 * 
 * <p>Alternatively, they can subclass create a subclass of this discovery component and override one/some/all of the
 * following methods:</p>
 * 
 * <ul>
 *     <li>{@link #getNewResourceKey(org.rhq.core.domain.configuration.Configuration)}</li>
 *     <li>{@link #getNewResourceName(org.rhq.core.domain.configuration.Configuration)}</li>
 *     <li>{@link #getNewResourceDescription(org.rhq.core.domain.configuration.Configuration)} </li>
 *     <li>{@link #getNewResourceVersion(org.rhq.core.domain.configuration.Configuration)}</li>
 * </ul>
 *
 * <h3>Auto-discovery</h3>
 *
 * <p>By default, application MBeans will be searched with an EMS query defined in the plugin descriptor by the
 * {@link PluginConfigProps#BEANS_QUERY_STRING} plugin-config property.
 * <br>
 * It is also possible to define the query string in a subclass overriding the
 * {@link #getBeansQueryString(org.rhq.core.domain.configuration.Configuration)} method.
 * </p>
 *
 * <p>Plugin developers can implement their custom MBeans lookup method in a subclass overriding the
 * {@link #hasApplicationMBeans(org.rhq.core.domain.configuration.Configuration, org.mc4j.ems.connection.EmsConnection)}
 * method.</p>
 *
 * <h3>JMX host and port discovery</h3>
 *
 * <p>The JMX server host will be detected by looking at the top level server resource plugin configuration
 * ({@link org.rhq.modules.plugins.jbossas7.StandaloneASComponent} and {@link org.rhq.modules.plugins.jbossas7.HostControllerComponent}). The JMX port detection mechanism depends on the
 * parent resource.</p>
 *
 * <h4>On standalone servers</h4>
 *
 * <p>For AS7 and EAP6 servers, the discovery component will look at the management port defined in the parent
 * {@link org.rhq.modules.plugins.jbossas7.StandaloneASComponent} plugin configuration and add the value of {@link #STANDALONE_REMOTING_PORT_OFFSET}.</p>
 * <p>For Wildfly8 servers, the discovery component will look at the management port defined in the parent
 * {@link org.rhq.modules.plugins.jbossas7.StandaloneASComponent} plugin configuration.</p>
 *
 * <h4>On managed servers</h4>
 *
 * <p>For AS7 and EAP6 servers, the discovery component will use {@link #DOMAIN_REMOTING_PORT_DEFAULT} <em>plus</em> the port offset of the managed server.</p>
 * <p>For Wildfly8 servers, the discovery component will use {@link #HTTP_PORT_DEFAULT} <em>plus</em> the port offset of the managed server.</p>
 *
 * <h3>Authentication</h3>
 *
 * <p>JMX connectivity on AS7 and EAP6 requires authentication and standalone and managed servers behaviors differ.</p>
 *
 * <h4>On standalone servers</h4>
 *
 * <p>In standalone mode, the server will use the ManagementRealm, just as for HTTP management interface authentication.
 * Consequently, this class will pick up the credentials of the management user defined in the plugin configuration of
 * the parent server resource ({@link org.rhq.modules.plugins.jbossas7.StandaloneASComponent}).</p>
 *
 * <p>In other words, plugin authors have nothing to do.</p>
 *
 * <h4>On managed servers</h4>
 *
 * <p>In domain mode, the server will use the ApplicationRealm. Consequently, there is no way for the discovery
 * component to discover credentials automatically and it will use "rhqadmin:rhqadmin" by default.</p>
 *
 * <p>When working with managed servers, plugin developers should subclass the discovery component and override the
 * {@link #getCredentialsForManagedAS()} method. For example, an implementation could lookup the credentials in a
 * text file.</p>
 *
 * <h3>Plugin descriptor example</h3>
 *
 * <pre>
 *     &lt;plugin
 *         name="MyappMBeansPlugin"
 *         displayName="Myapp MBeans Plugin"
 *         package="com.myapp.services.plugin"
 *         xmlns="urn:xmlns:rhq-plugin"
 *         xmlns:c="urn:xmlns:rhq-configuration"&gt;
 *
 *       &lt;depends plugin="JBossAS7" useClasses="true"/&gt;
 *
 *       &lt;service name="Myapp Services"
 *                discovery="org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansDiscoveryComponent"
 *                class="org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansComponent"
 *                description="Container for Myapp Services"
 *                singleton="true"&gt;
 *
 *         &lt;runs-inside&gt;
 *           &lt;!-- The type of the server the application is running on --&gt;
 *           &lt;parent-resource-type name="JBossAS7 Standalone Server" plugin="JBossAS7"/&gt;
 *           &lt;parent-resource-type name="Managed Server" plugin="JBossAS7"/&gt;
 *         &lt;/runs-inside&gt;
 *
 *         &lt;plugin-configuration&gt;
 *           &lt;c:simple-property name="beansQueryString" readOnly="true" default="myapp:service=*"/&gt;
 *           &lt;c:simple-property name="newResourceKey" readOnly="true" default="myappServices"/&gt;
 *           &lt;c:simple-property name="newResourceName" readOnly="true" default="Myapp Services"/&gt;
 *           &lt;c:simple-property name="newResourceDescription" readOnly="true" default="Container for Myapp Services"/&gt;
 *         &lt;/plugin-configuration&gt;
 *
 *         &lt;!--
 *           ApplicationMBeansComponent can be the parent of any JMXComponent (JMX plugin). For example, it's possible
 *           to monitor a MBean with no line of Java code thanks to the MBeanResourceComponent facility. Plugin authors
 *           only have to configure metrics (mapped to MBeans attributes) and operations (MBeans operations).
 *         --&gt;
 *         &lt;service name="HelloService" discovery="org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent"
 *                  class="org.rhq.plugins.jmx.MBeanResourceComponent" singleton="true"&gt;
 *           &lt;plugin-configuration&gt;
 *             &lt;c:simple-property name="objectName" default="myapp:service=HelloService" readOnly="true"/&gt;
 *           &lt;/plugin-configuration&gt;
 *           &lt;operation name="helloTo"&gt;
 *             &lt;parameters&gt;
 *               &lt;c:simple-property name="p1" displayName="somebody" type="string" required="true"/&gt;
 *             &lt;/parameters&gt;
 *             &lt;results&gt;
 *               &lt;c:simple-property name="operationResult" type="string"/&gt;
 *             &lt;/results&gt;
 *           &lt;/operation&gt;
 *         &lt;/service&gt;
 *
 *       &lt;/service&gt;
 *
 *     &lt;/plugin&gt;
 * </pre>
 *
 * @author Thomas Segismont
 * @see ApplicationMBeansComponent
 */
public class ApplicationMBeansDiscoveryComponent implements ResourceDiscoveryComponent<BaseComponent<?>> {
    private static final Log LOG = LogFactory.getLog(ApplicationMBeansDiscoveryComponent.class);

    public static final class PluginConfigProps {
        public static final String BEANS_QUERY_STRING = "beansQueryString";
        public static final String NEW_RESOURCE_KEY = "newResourceKey";
        public static final String NEW_RESOURCE_NAME = "newResourceName";
        public static final String NEW_RESOURCE_DESCRIPTION = "newResourceDescription";
        public static final String NEW_RESOURCE_VERSION = "newResourceVersion";

        private PluginConfigProps() {
            // Constants class
        }
    }

    private static final String HOSTNAME = "hostname";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CLIENT_JAR_LOCATION = "clientJarLocation";
    // "remoting-jmx" for AS7 and EAP6, "http-remoting-jmx" for Wildfly8;
    private static final String PROTOCOL = "protocol";

    private static final int STANDALONE_REMOTING_PORT_OFFSET = 9;
    private static final int DOMAIN_REMOTING_PORT_DEFAULT = 4447;
    private static final String MANAGED_SERVER_PORT_OFFSET_PROPERTY_NAME = "socket-binding-port-offset";
    private static final int HTTP_PORT_DEFAULT = 8080;

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {

        BaseComponent<?> parentComponent = context.getParentResourceComponent();
        BaseServerComponent baseServerComponent = parentComponent.getServerComponent();
        ServerPluginConfiguration serverPluginConfiguration = baseServerComponent.getServerPluginConfiguration();
        JBossProductType productType = serverPluginConfiguration.getProductType();

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        int port;
        String username, password;
        if (parentComponent instanceof ManagedASComponent) {
            ManagedASComponent managedASComponent = (ManagedASComponent) parentComponent;
            Configuration managedASConfig = managedASComponent.loadResourceConfiguration();
            PropertySimple offsetProp = managedASConfig.getSimple(MANAGED_SERVER_PORT_OFFSET_PROPERTY_NAME);
            if (offsetProp == null) {
                LOG.warn("Could not find Managed Server socket binding offset, skipping discovery");
                return Collections.emptySet();
            }
            if (productType != WILDFLY8) {
                port = DOMAIN_REMOTING_PORT_DEFAULT;
            } else {
                port = HTTP_PORT_DEFAULT;
            }
            port +=  offsetProp.getIntegerValue();
            String[] credentials = getCredentialsForManagedAS();
            username = credentials[0];
            password = credentials[1];
        } else if (parentComponent instanceof StandaloneASComponent) {
            port = serverPluginConfiguration.getPort();
            if (productType != WILDFLY8) {
                port += STANDALONE_REMOTING_PORT_OFFSET;
            }
            username = serverPluginConfiguration.getUser();
            password = serverPluginConfiguration.getPassword();
        } else {
            LOG.warn(parentComponent + " is not a supported parent component");
            return Collections.emptySet();
        }

        String clientJarPath = "bin" + File.separator + "client" + File.separator + "jboss-client.jar";
        File clientJarFile = new File(serverPluginConfiguration.getHomeDir(), clientJarPath);
        if (!clientJarFile.isFile()) {
            LOG.warn(clientJarFile + " does not exist.");
            return Collections.emptySet();
        }

        pluginConfig.setSimpleValue(HOSTNAME, serverPluginConfiguration.getHostname());
        pluginConfig.setSimpleValue(PORT, String.valueOf(port));
        pluginConfig.setSimpleValue(USERNAME, username);
        pluginConfig.setSimpleValue(PASSWORD, password);
        pluginConfig.setSimpleValue(CLIENT_JAR_LOCATION, clientJarFile.getAbsolutePath());
        pluginConfig.setSimpleValue(PROTOCOL, productType == WILDFLY8 ? "http-remoting-jmx" : "remoting-jmx");

        EmsConnection emsConnection = null;
        try {
            emsConnection = loadEmsConnection(pluginConfig, context.getParentResourceContext().getTemporaryDirectory());
            if (emsConnection == null) {
                // An error occured while creating the connection
                return Collections.emptySet();
            }
            if (!hasApplicationMBeans(pluginConfig, emsConnection)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No application MBeans found");
                }
                return Collections.emptySet();
            }

            return Collections.singleton(new DiscoveredResourceDetails(context.getResourceType(),
                getNewResourceKey(pluginConfig), getNewResourceName(pluginConfig), getNewResourceVersion(pluginConfig),
                getNewResourceDescription(pluginConfig), pluginConfig, null));

        } finally {
            if (emsConnection != null) {
                emsConnection.close();
            }
        }
    }

    /**
     * Indicates if a managed server or a standalone server has application MBeans. This implementation searches MBeans
     * with an EMS query string.
     *
     * @param pluginConfig - the plugin configuration object for the {@link ApplicationMBeansComponent} that may be
     *                     created 
     * @param emsConnection - an active emsConnection which can be used to communicate with the server
     * @return true if application MBeans were detected, false otherwise
     */
    protected boolean hasApplicationMBeans(Configuration pluginConfig, EmsConnection emsConnection) {
        String beansQueryString = getBeansQueryString(pluginConfig);
        if (emsConnection.queryBeans(beansQueryString).isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found no MBeans with query '" + beansQueryString + "'");
            }
            return false;
        }
        return true;
    }

    /**
     * Default implementation of the credentials lookup for JMX authentication against managed servers (domain mode).
     *
     * @return an array of two elements, the first being the username and the second the password
     */
    protected String[] getCredentialsForManagedAS() {
        return new String[] { "rhqadmin", "rhqadmin" };
    }

    /**
     * The EMS query string chosen when using the default implementation of
     * {@link #hasApplicationMBeans(org.rhq.core.domain.configuration.Configuration, org.mc4j.ems.connection.EmsConnection)}.
     *
     * @param pluginConfig - the plugin configuration object for the {@link ApplicationMBeansComponent} that will be
     *                     created
     * @return an EMS query string
     */
    protected String getBeansQueryString(Configuration pluginConfig) {
        return pluginConfig.getSimpleValue(BEANS_QUERY_STRING);
    }

    /**
     * @param pluginConfig - the plugin configuration object for the {@link ApplicationMBeansComponent} that will be
     *                     created
     * @return the new resource key
     */
    protected String getNewResourceKey(Configuration pluginConfig) {
        return pluginConfig.getSimpleValue(PluginConfigProps.NEW_RESOURCE_KEY);
    }

    /**
     * @param pluginConfig - the plugin configuration object for the {@link ApplicationMBeansComponent} that will be
     *                     created
     * @return the new resource name
     */
    protected String getNewResourceName(Configuration pluginConfig) {
        return pluginConfig.getSimpleValue(NEW_RESOURCE_NAME);
    }

    /**
     * @param pluginConfig - the plugin configuration object for the {@link ApplicationMBeansComponent} that will be
     *                     created
     * @return the new resource description
     */
    protected String getNewResourceDescription(Configuration pluginConfig) {
        return pluginConfig.getSimpleValue(NEW_RESOURCE_DESCRIPTION);
    }

    /**
     * @param pluginConfig - the plugin configuration object for the {@link ApplicationMBeansComponent} that will be
     *                     created
     * @return the new resource version
     */
    protected String getNewResourceVersion(Configuration pluginConfig) {
        return pluginConfig.getSimpleValue(NEW_RESOURCE_VERSION);
    }

    /**
     * Creates a new {@link EmsConnection} object.
     *
     * @param pluginConfig - a plugin configuration object of the {@link ApplicationMBeansComponent}
     * @return a new {@link EmsConnection} object or null if connecting failed
     * @deprecated as of RHQ4.12. Use
     * {@link #loadEmsConnection(org.rhq.core.domain.configuration.Configuration, java.io.File)} instead.
     */
    @Deprecated
    public static EmsConnection loadEmsConnection(Configuration pluginConfig) {
        return loadEmsConnection(pluginConfig, new File(System.getProperty("java.io.tmpdir")));
    }

    /**
     * Creates a new {@link EmsConnection} object.
     *
     * @param pluginConfig - a plugin configuration object of the {@link ApplicationMBeansComponent}
     * @param jarTempDir - the directory where the additional JARs for the JMX connection classloader should be copied
     * @return a new {@link EmsConnection} object or null if connecting failed
     */
    public static EmsConnection loadEmsConnection(Configuration pluginConfig, File jarTempDir) {
        EmsConnection emsConnection = null;
        try {
            File clientJarFile = new File(pluginConfig.getSimpleValue(CLIENT_JAR_LOCATION));

            ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.initializeConnectionType(new A7ConnectionTypeDescriptor(clientJarFile));
            connectionSettings.setLibraryURI(clientJarFile.getParent());
            String serverUrl = "service:jmx:" //
                + pluginConfig.getSimpleValue(PROTOCOL) //
                + "://" //
                + pluginConfig.getSimpleValue(HOSTNAME) //
                + ":" //
                + pluginConfig.getSimpleValue(PORT);
            connectionSettings.setServerUrl(serverUrl);

            connectionSettings.setPrincipal(pluginConfig.getSimpleValue(USERNAME));
            connectionSettings.setCredentials(pluginConfig.getSimpleValue(PASSWORD));

            if (connectionSettings.getControlProperties() == null) {
                connectionSettings.setControlProperties(new Properties());
            }
            connectionSettings.getControlProperties().setProperty(ConnectionFactory.COPY_JARS_TO_TEMP,
                String.valueOf(TRUE));
            connectionSettings.getControlProperties().setProperty(ConnectionFactory.JAR_TEMP_DIR,
                jarTempDir.getAbsolutePath());

            if (connectionSettings.getAdvancedProperties() == null) {
                connectionSettings.setAdvancedProperties(new Properties());
            }
            connectionSettings.getAdvancedProperties().setProperty(ConnectionFactory.USE_CONTEXT_CLASSLOADER,
                String.valueOf(FALSE));

            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.discoverServerClasses(connectionSettings);
            ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);

            return connectionProvider.connect();

        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not create EmsConnection", e);
            }
            if (emsConnection != null) {
                emsConnection.close();
            }
            return null;
        }
    }

    private static class A7ConnectionTypeDescriptor extends JSR160ConnectionTypeDescriptor {
        private final File clientJarFile;

        public A7ConnectionTypeDescriptor(File clientJarFile) {
            this.clientJarFile = clientJarFile;
        }

        @Override
        public String[] getConnectionClasspathEntries() {
            return new String[] { clientJarFile.getName() };
        }

        @Override
        public boolean isUseChildFirstClassLoader() {
            return true;
        }
    }
}
