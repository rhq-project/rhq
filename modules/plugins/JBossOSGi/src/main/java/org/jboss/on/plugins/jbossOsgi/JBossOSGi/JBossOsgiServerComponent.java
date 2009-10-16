
package org.jboss.on.plugins.jbossOsgi.JBossOSGi;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

public class JBossOsgiServerComponent implements ResourceComponent
, OperationFacet
, CreateChildResourceFacet
{
    private final Log log = LogFactory.getLog(this.getClass());

    private static final int CHANGEME = 1; // TODO remove or change this

    private EmsConnection connection;

    ResourceContext resourceContext;

    public static final String EVENT_TYPE_LOG = "log"; // See plugin descriptor
    private EventContext eventContext;
    private OsgiEventPoller eventPoller;

    /**
     * Controls the dampening of connection error stack traces in an attempt to control spam to the log
     * file. Each time a connection error is encountered, this will be incremented. When the connection
     * is finally established, this will be reset to zero.
     */
    private int consecutiveConnectionErrors;



    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        try {
            EmsConnection connection = loadConnection(resourceContext.getPluginConfiguration());
            EmsBean bean = connection.getBean(JBossOsgiServerDiscovery.MANAGER_OBJECT);

            // this is necessary to prove that that not only the connection exists but is servicing requests.
            bean.getAttribute("bundles").refresh();
            return AvailabilityType.UP;
        } catch (Exception e) {
            closeConnection();
            return AvailabilityType.DOWN;
        }
    }



    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        resourceContext = context;

        Configuration conf = context.getPluginConfiguration();
        // TODO add code to start the resource / connection to it


        // Register getting remote log events
        eventContext = context.getEventContext();
        eventPoller = new OsgiEventPoller();
        eventContext.registerEventPoller(eventPoller, 60);

    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {

        if (eventPoller!=null)
            eventPoller.tearDown();
        eventContext.unregisterEventPoller(EVENT_TYPE_LOG);
    }





    public void startOperationFacet(OperationContext context) {

    }


    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("dummyOperation".equals(name)) {
            // TODO implement me

        }
        return res;
    }



    /**
     * Create a child resource
     * @see org.rhq.core.pluginapi.inventory.CreateChildResourceFacet
     */
    public CreateResourceReport createResource(CreateResourceReport report)
    {
        // TODO supply code to create a child resource

        return null; // TODO change this
    }

    public EmsConnection getEmsConnection() {
        EmsConnection emsConnection = null;

        try {
            emsConnection = loadConnection(resourceContext.getPluginConfiguration());
        } catch (Exception e) {
            log.error("Component attempting to access a connection that could not be loaded");
        }

        return emsConnection;
    }



    public EmsConnection getEmsConnection(Configuration config) {
        EmsConnection emsConnection = null;

        try {
            emsConnection = loadConnection(config);
        } catch (Exception e) {
            log.error("Component attempting to access a connection that could not be loaded");
        }

        return emsConnection;
    }

    /**
     * This is the preferred way to use a connection from within this class; methods should not access the connection
     * property directly as it may not have been instantiated if the connection could not be made.
     *
     * <p>If the connection has already been established, return the object reference to it. If not, attempt to make
     * a live connection to the JMX server.</p>
     *
     * <p>If the connection could not be made in the {@link #start(org.rhq.core.pluginapi.inventory.ResourceContext)}
     * method, this method will effectively try to load the connection on each attempt to use it. As such, multiple
     * threads may attempt to access the connection through this means at a time. Therefore, the method has been
     * made synchronized on instances of the class.</p>
     *
     * <p>If any errors are encountered, this method will log the error, taking into account logic to prevent spamming
     * the log file. Calling methods should take care to not redundantly log the exception thrown by this method.</p>
     *
     * @return live connection to the JMX server; this will not be <code>null</code>
     *
     * @throws Exception if there are any issues at all connecting to the server
     * @param pluginConfig
     */
    private synchronized EmsConnection loadConnection(Configuration pluginConfig) throws Exception {
        if (this.connection == null) {
            try {
                //Configuration pluginConfig = this.resourceContext.getPluginConfiguration();

                ConnectionSettings connectionSettings = new ConnectionSettings();

                String connectionTypeDescriptorClass = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
                    .getStringValue();
                PropertySimple serverUrl = pluginConfig
                    .getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY);

                connectionSettings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(
                    connectionTypeDescriptorClass).newInstance());
                // if not provided use the default serverUrl
                if (null != serverUrl) {
                    connectionSettings.setServerUrl(serverUrl.getStringValue());
                }

//                connectionSettings.setPrincipal(pluginConfig.getSimpleValue(PRINCIPAL_CONFIG_PROP, null));
//                connectionSettings.setCredentials(pluginConfig.getSimpleValue(CREDENTIALS_CONFIG_PROP, null));

                if (connectionSettings.getAdvancedProperties() == null) {
                    connectionSettings.setAdvancedProperties(new Properties());
                }

                ConnectionFactory connectionFactory = new ConnectionFactory();


                ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
                this.connection = connectionProvider.connect();

                this.connection.loadSynchronous(false); // this loads all the MBeans

                this.consecutiveConnectionErrors = 0;

                if (log.isDebugEnabled())
                    log.debug("Successfully made connection to the OSGiServer instance");
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
                    log.warn("Could not establish connection to the JBossOSGi instance ["
                        + (consecutiveConnectionErrors + 1) + "] times for resource ["
                        + resourceContext.getResourceKey() + "]", e);
                }

                if (log.isDebugEnabled())
                    log.debug("Could not connect to the JBoss instance for resource ["
                        + resourceContext.getResourceKey() + "]", e);

                consecutiveConnectionErrors++;

                throw e;
            }
        }

        return connection;
    }


        /**
     * If necessary attempt to close the EMS connection, then set this.connection null.  Synchronized ensure we play well
     * with loadConnection.
     */
    public synchronized void closeConnection() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                log.error("Error closing Tomcat EMS connection: " + e);
            }
            this.connection = null;
        }
    }
}