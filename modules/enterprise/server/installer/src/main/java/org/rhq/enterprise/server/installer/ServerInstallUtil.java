/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.installer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.common.jbossas.client.controller.Address;
import org.rhq.common.jbossas.client.controller.CoreJBossASClient;
import org.rhq.common.jbossas.client.controller.DatasourceJBossASClient;
import org.rhq.common.jbossas.client.controller.FailureException;
import org.rhq.common.jbossas.client.controller.InfinispanJBossASClient;
import org.rhq.common.jbossas.client.controller.JBossASClient;
import org.rhq.common.jbossas.client.controller.MessagingJBossASClient;
import org.rhq.common.jbossas.client.controller.SecurityDomainJBossASClient;
import org.rhq.common.jbossas.client.controller.SocketBindingJBossASClient;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.setup.DBSetup;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * Provides utility methods necessary to complete the server installation.
 *
 * @author John Mazzitelli
 */
public class ServerInstallUtil {
    private static final Log LOG = LogFactory.getLog(ServerInstallUtil.class);

    public enum ExistingSchemaOption {
        OVERWRITE, KEEP, SKIP
    };

    public enum SupportedDatabaseType {
        POSTGRES, ORACLE
    };

    private static class SocketBindingInfo {
        public String name;
        public String sysprop;
        public int port;

        public SocketBindingInfo(String n, String s, int p) {
            name = n;
            sysprop = s;
            port = p;
        }
    }

    private static final ArrayList<SocketBindingInfo> defaultSocketBindings;
    static {
        // all ports are -1000 from out-of-box AS7 defaults
        // except for the jboss.management ones - those are -3000 from their out-of-box defaults
        defaultSocketBindings = new ArrayList<SocketBindingInfo>();
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_AJP,
            "rhq.server.socket.binding.port.ajp", 7009));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_HTTP,
            "rhq.server.socket.binding.port.http", 7080));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_HTTPS,
            "rhq.server.socket.binding.port.https", 7443));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_JACORB,
            "rhq.server.socket.binding.port.jacorb", 2528));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_JACORB_SSL,
            "rhq.server.socket.binding.port.jacorb-ssl", 2529));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MESSAGING,
            "rhq.server.socket.binding.port.messaging", 4445));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MESSAGING_THRUPUT,
            "rhq.server.socket.binding.port.messaging-throughput", 4455));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MGMT_HTTP,
            "jboss.management.http.port", 6990));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MGMT_HTTPS,
            "jboss.management.https.port", 6443));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MGMT_NATIVE,
            "jboss.management.native.port", 6999));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_OSGI_HTTP,
            "rhq.server.socket.binding.port.osgi-http", 7090));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_REMOTING,
            "rhq.server.socket.binding.port.remoting", 3447));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_TXN_RECOVERY_ENV,
            "rhq.server.socket.binding.port.txn-recovery-environment", 3712));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_TXN_STATUS_MGR,
            "rhq.server.socket.binding.port.txn-status-manager", 3713));
    }

    private static final String RHQ_DATASOURCE_NAME_NOTX = "NoTxRHQDS";
    private static final String RHQ_DATASOURCE_NAME_XA = "RHQDS";
    private static final String CASSANDRA_DATASOURCE_NAME = "CassandraDS";
    private static final String RHQ_DS_SECURITY_DOMAIN = "RHQDSSecurityDomain";
    private static final String RHQ_REST_SECURITY_DOMAIN = "RHQRESTSecurityDomain";
    private static final String JDBC_DRIVER_POSTGRES = "postgres";
    private static final String JDBC_DRIVER_ORACLE = "oracle";
    private static final String JDBC_DRIVER_CASSANDRA = "cassandra";
    private static final String JMS_ALERT_CONDITION_QUEUE = "AlertConditionQueue";
    private static final String JMS_DRIFT_CHANGESET_QUEUE = "DriftChangesetQueue";
    private static final String JMS_DRIFT_FILE_QUEUE = "DriftFileQueue";
    private static final String RHQ_CACHE_CONTAINER = "rhq";
    private static final String RHQ_CACHE = "rhqCache";

    /**
     * Configure the deployment scanner to get ready to deploy the application.
     * @param mcc JBossAS management client
     * @throws Exception
     */
    public static void configureDeploymentScanner(ModelControllerClient mcc) throws Exception {
        CoreJBossASClient client = new CoreJBossASClient(mcc);

        // we do not want our RHQ Server to support hot deployments via the scanner
        client.setAppServerDefaultDeploymentScanEnabled(false);
    }

    /**
     * Prepares the mail service by configuring the SMTP settings.
     * 
     * @param mcc JBossAS management client
     * @param serverProperties the server's properties
     * @throws Exception
     */
    public static void setupMailService(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {

        String fromAddressExpr = "${" + ServerProperties.PROP_EMAIL_FROM_ADDRESS + ":rhqadmin@localhost.com}";
        //String smtpHostExpr = "${" + ServerProperties.PROP_EMAIL_SMTP_HOST + ":localhost}";
        String smtpPortExpr = "${" + ServerProperties.PROP_EMAIL_SMTP_PORT + ":25}";

        // Tweek the mail configuration that comes out of box. Setup a batch request to write the proper attributes.

        // First, the from address (TODO: there is also a "ssl", "username" and "password" attribute we could set for authz)
        Address addr = Address.root().add(JBossASClient.SUBSYSTEM, "mail", "mail-session", "java:jboss/mail/Default");
        ModelNode writeFromAddr = JBossASClient.createRequest(JBossASClient.WRITE_ATTRIBUTE, addr);
        writeFromAddr.get(JBossASClient.NAME).set("from");
        writeFromAddr.get(JBossASClient.VALUE).setExpression(fromAddressExpr);

        // now the SMTP host
        addr = Address.root().add("socket-binding-group", "standard-sockets",
            "remote-destination-outbound-socket-binding", "mail-smtp");
        ModelNode writeHost = JBossASClient.createRequest(JBossASClient.WRITE_ATTRIBUTE, addr);
        writeHost.get(JBossASClient.NAME).set("host");
        // TODO: see https://issues.jboss.org/browse/AS7-5321 - that must be fixed before supporting expressions
        //writeHost.get(JBossASClient.VALUE).setExpression(smtpHostExpr);
        writeHost.get(JBossASClient.VALUE).set(serverProperties.get(ServerProperties.PROP_EMAIL_SMTP_HOST)); // remove when AS7-5321 is fixed

        // now the SMTP port
        addr = Address.root().add("socket-binding-group", "standard-sockets",
            "remote-destination-outbound-socket-binding", "mail-smtp");
        ModelNode writePort = JBossASClient.createRequest(JBossASClient.WRITE_ATTRIBUTE, addr);
        writePort.get(JBossASClient.NAME).set("port");
        writePort.get(JBossASClient.VALUE).setExpression(smtpPortExpr);

        ModelNode batch = JBossASClient.createBatchRequest(writeFromAddr, writeHost, writePort);
        JBossASClient client = new JBossASClient(mcc);
        ModelNode response = client.execute(batch);
        if (!JBossASClient.isSuccess(response)) {
            throw new FailureException(response, "Failed to setup mail service");
        }
        LOG.info("Mail service has been configured.");
        return;
    }

    /**
     * Give the server properties, this returns the type of database that will be connected to.
     * 
     * @param serverProperties
     * @return the type of DB
     */
    public static SupportedDatabaseType getSupportedDatabaseType(HashMap<String, String> serverProperties) {
        return getSupportedDatabaseType(serverProperties.get(ServerProperties.PROP_DATABASE_TYPE));
    }

    /**
     * Give the database type string, this returns the type of database that it refers to.
     * 
     * @param dbType the database type string
     * @return the type of DB
     */
    public static SupportedDatabaseType getSupportedDatabaseType(String dbType) {
        if (dbType == null) {
            return null;
        }
        if (dbType.toLowerCase().indexOf("postgres") > -1) {
            return SupportedDatabaseType.POSTGRES;
        } else if (dbType.toLowerCase().indexOf("oracle") > -1) {
            return SupportedDatabaseType.ORACLE;
        }
        return null;
    }

    /**
     * Creates the security domain for the datasources. This is needed to support
     * obfuscation of the password in the configuration file.
     *
     * @param mcc the JBossAS management client
     * @param serverProperties contains the obfuscated password to store in the security domain
     * @throws Exception
     */
    public static void createDatasourceSecurityDomain(ModelControllerClient mcc,
        HashMap<String, String> serverProperties) throws Exception {

        final String dbUsername = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
        final String obfuscatedPassword = serverProperties.get(ServerProperties.PROP_DATABASE_PASSWORD);
        final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);
        final String securityDomain = RHQ_DS_SECURITY_DOMAIN;
        if (!client.isSecurityDomain(securityDomain)) {
            client.createNewSecureIdentitySecurityDomainRequest(securityDomain, dbUsername, obfuscatedPassword);
            LOG.info("Security domain [" + securityDomain + "] created");
        } else {
            LOG.info("Security domain [" + securityDomain + "] already exists, skipping the creation request");
        }
    }

    /**
     * Creates the JMS Queues required for Drift and Alerting.
     *
     * @param mcc the JBossAS management client
     * @param serverProperties contains the obfuscated password to store in the security domain
     * @throws Exception
     */
    public static void createNewJMSQueues(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {

        final MessagingJBossASClient client = new MessagingJBossASClient(mcc);
        final List<String> entryNames = new ArrayList<String>();

        // TODO (jshaughn): Prior to HornetQ we set recoveryRetries to 0: "don't redeliver messages on failure. It
        // just causes more failures. just go straight to the dead messages by setting recoveryRetries to 0.
        // This is equivalent to setting the dLQMaxResent property to 0 in the MessageDriven annotation in
        // the class definition."
        // HornetQ has different semantics, and may behave well with default settings. If not, we'll
        // likely need to add specific <address-setting> elements for our queues, which set
        // max-delivery-attempts to 0.  The documented default is 10.

        String queueName = JMS_ALERT_CONDITION_QUEUE;
        if (!client.isQueue(queueName)) {
            entryNames.clear();
            entryNames.add("queue/" + queueName);
            ModelNode request = client.createNewQueueRequest(queueName, true, entryNames);
            ModelNode results = client.execute(request);
            if (!MessagingJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create JMS Queue [" + queueName + "]");
            } else {
                LOG.info("JMS queue [" + queueName + "] created");
            }
        } else {
            LOG.info("JMS Queue [" + queueName + "] already exists, skipping the creation request");
        }

        queueName = JMS_DRIFT_CHANGESET_QUEUE;
        if (!client.isQueue(queueName)) {
            entryNames.clear();
            entryNames.add("queue/" + queueName);
            ModelNode request = client.createNewQueueRequest(queueName, true, entryNames);
            ModelNode results = client.execute(request);
            if (!MessagingJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create JMS Queue [" + queueName + "]");
            } else {
                LOG.info("JMS queue [" + queueName + "] created");
            }
        } else {
            LOG.info("JMS Queue [" + queueName + "] already exists, skipping the creation request");
        }

        queueName = JMS_DRIFT_FILE_QUEUE;
        if (!client.isQueue(queueName)) {
            entryNames.clear();
            entryNames.add("queue/" + queueName);
            ModelNode request = client.createNewQueueRequest(queueName, true, entryNames);
            ModelNode results = client.execute(request);
            if (!MessagingJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create JMS Queue [" + queueName + "]");
            } else {
                LOG.info("JMS queue [" + queueName + "] created");
            }
        } else {
            LOG.info("JMS Queue [" + queueName + "] already exists, skipping the creation request");
        }

    }

    /**
     * Creates the security domain for REST.
     *
     * @param mcc the JBossAS management client
     * @param serverProperties contains the obfuscated password to store in the security domain
     * @throws Exception
     */
    public static void createRESTSecurityDomain(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {

        final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);
        final String securityDomain = RHQ_REST_SECURITY_DOMAIN;
        if (!client.isSecurityDomain(securityDomain)) {
            client.createNewDatabaseServerSecurityDomainRequest(securityDomain, "java:jboss/datasources/RHQDS",
                "SELECT PASSWORD FROM RHQ_PRINCIPAL WHERE principal=?",
                "SELECT 'all', 'Roles' FROM RHQ_PRINCIPAL WHERE principal=?", null, null);
            LOG.info("Security domain [" + securityDomain + "] created");
        } else {
            LOG.info("Security domain [" + securityDomain + "] already exists, skipping the creation request");
        }
    }

    /**
     * Creates the Infinispan caches for RHQ.
     *
     * @param mcc the JBossAS management client
     * @param serverProperties contains the obfuscated password to store in the security domain
     * @throws Exception
     */
    public static void createNewCaches(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {

        final InfinispanJBossASClient client = new InfinispanJBossASClient(mcc);
        final String cacheContainerName = RHQ_CACHE_CONTAINER;
        final String localCacheName = RHQ_CACHE;
        if (!client.isCacheContainer(cacheContainerName)) {
            ModelNode request = client.createNewCacheContainerRequest(cacheContainerName, localCacheName);
            ModelNode results = client.execute(request);
            if (!MessagingJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create Cache container [" + cacheContainerName + "]");
            } else {
                LOG.info("Cache container [" + cacheContainerName + "] created");
            }

        } else {
            LOG.info("Cache container [" + cacheContainerName + "] already exists, skipping the creation request");
        }

        if (!client.isLocalCache(cacheContainerName, localCacheName)) {
            ModelNode request = client.createNewLocalCacheRequest(cacheContainerName, localCacheName, null, null, null,
                null, null);
            ModelNode results = client.execute(request);
            if (!MessagingJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create Local Cache [" + localCacheName + "]");
            } else {
                LOG.info("Local Cache [" + localCacheName + "] created");
            }

        } else {
            LOG.info("Local Cache [" + localCacheName + "] already exists, skipping the creation request");
        }
    }

    /**
     * Creates JDBC driver configurations so the datasources can properly connect to the backend databases.
     * This will attempt to create drivers for all supported databases, not just for the database type that
     * is currently configured.
     *
     * @param mcc
     * @param serverProperties
     * @throws Exception
     */
    public static void createNewJdbcDrivers(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {

        final DatasourceJBossASClient client = new DatasourceJBossASClient(mcc);

        final ModelNode postgresDriverRequest = client.createNewJdbcDriverRequest(JDBC_DRIVER_POSTGRES,
            "org.rhq.postgres", "org.postgresql.xa.PGXADataSource");
        final ModelNode oracleDriverRequest = client.createNewJdbcDriverRequest(JDBC_DRIVER_ORACLE, "org.rhq.oracle",
            "oracle.jdbc.xa.client.OracleXADataSource");
//        final ModelNode cassandraDriverRequest = client.createNewJdbcDriverRequest(JDBC_DRIVER_CASSANDRA,
//            "org.apache-extras.cassandra-jdbc", null);

        // if we are to use Oracle, we throw an exception if we can't create the Oracle datasource. We also try to
        // create the Postgres datasource but because it isn't needed, we don't throw exceptions if that fails, we
        // just log a warning.
        // The reverse is true if we are to use Postgres (that is, we ensure Postgres driver is created, but not Oracle).
        ModelNode results;
        final SupportedDatabaseType supportedDbType = getSupportedDatabaseType(serverProperties);
        switch (supportedDbType) {
        case POSTGRES: {
            if (client.isJDBCDriver(JDBC_DRIVER_POSTGRES)) {
                LOG.info("Postgres JDBC driver is already deployed");
            } else {
                results = client.execute(postgresDriverRequest);
                if (!DatasourceJBossASClient.isSuccess(results)) {
                    throw new FailureException(results, "Failed to create postgres database driver");
                } else {
                    LOG.info("Deployed Postgres JDBC driver");
                }
            }

            if (client.isJDBCDriver(JDBC_DRIVER_ORACLE)) {
                LOG.info("Oracle JDBC driver is already deployed");
            } else {
                results = client.execute(oracleDriverRequest);
                if (!DatasourceJBossASClient.isSuccess(results)) {
                    LOG.warn("Could not create Oracle JDBC Driver - you will not be able to switch to an Oracle DB later: "
                        + JBossASClient.getFailureDescription(results));
                } else {
                    LOG.info("Deployed Oracle JDBC driver for future use");
                }
            }
            break;
        }
        case ORACLE: {
            if (client.isJDBCDriver(JDBC_DRIVER_ORACLE)) {
                LOG.info("Oracle JDBC driver is already deployed");
            } else {
                results = client.execute(oracleDriverRequest);
                if (!DatasourceJBossASClient.isSuccess(results)) {
                    throw new FailureException(results, "Failed to create oracle database driver");
                } else {
                    LOG.info("Deployed Oracle JDBC driver");
                }
            }
            if (client.isJDBCDriver(JDBC_DRIVER_POSTGRES)) {
                LOG.info("Postgres JDBC driver is already deployed");
            } else {
                results = client.execute(postgresDriverRequest);
                if (!DatasourceJBossASClient.isSuccess(results)) {
                    LOG.warn("Could not create Postgres JDBC Driver - you will not be able to switch to a Postgres DB later: "
                        + JBossASClient.getFailureDescription(results));
                } else {
                    LOG.info("Deployed Postgres JDBC driver for future use");
                }
            }
            break;
        }
        default:
            throw new RuntimeException("bad db type"); // this should never happen; should have never gotten to this point with a bad type
        }

//        ModelNode cassandraResults = client.execute(cassandraDriverRequest);
//        if (!DatasourceJBossASClient.isSuccess(cassandraResults)) {
//            throw new FailureException(cassandraResults, "Failed to create Cassandra database driver");
//        } else {
//            LOG.info("Deployed Cassandra JDBC driver");
//        }
    }

    /**
     * Creates the datasources needed by the RHQ Server.
     *
     * @param mcc the JBossAS management client
     * @param serverProperties properties to help determine the properties of the datasources to be created
     * @throws Exception
     */
    public static void createNewDatasources(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {

        final SupportedDatabaseType supportedDbType = getSupportedDatabaseType(serverProperties);
        switch (supportedDbType) {
        case POSTGRES: {
            createNewDatasources_Postgres(mcc);
            break;
        }
        case ORACLE: {
            createNewDatasources_Oracle(mcc);
            break;
        }
        default:
            throw new RuntimeException("bad db type"); // this should never happen; should have never gotten to this point with a bad type
        }
//        createNewDatasources_Cassandra(mcc);

        LOG.info("Created datasources");

        final DatasourceJBossASClient client = new DatasourceJBossASClient(mcc);
        client.enableDatasource(RHQ_DATASOURCE_NAME_NOTX);
        client.enableXADatasource(RHQ_DATASOURCE_NAME_XA);
        LOG.info("Enabled datasources");

    }

    private static void createNewDatasources_Postgres(ModelControllerClient mcc) throws Exception {
        final HashMap<String, String> props = new HashMap<String, String>(4);
        final DatasourceJBossASClient client = new DatasourceJBossASClient(mcc);

        ModelNode noTxDsRequest = null;
        ModelNode xaDsRequest = null;

        if (!client.isDatasource(RHQ_DATASOURCE_NAME_NOTX)) {
            props.put("char.encoding", "UTF-8");

            noTxDsRequest = client.createNewDatasourceRequest(RHQ_DATASOURCE_NAME_NOTX, 30000,
                "${rhq.server.database.connection-url:jdbc:postgres://127.0.0.1:5432/rhq}", JDBC_DRIVER_POSTGRES,
                "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter", 15, false, 2, 5, 75,
                RHQ_DS_SECURITY_DOMAIN, "-unused-stale-conn-checker-", "TRANSACTION_READ_COMMITTED",
                "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker", props);
            noTxDsRequest.get("steps").get(0).remove("stale-connection-checker-class-name"); // we don't have one of these for postgres            
        } else {
            LOG.info("Postgres datasource [" + RHQ_DATASOURCE_NAME_NOTX + "] already exists");
        }

        if (!client.isXADatasource(RHQ_DATASOURCE_NAME_XA)) {
            props.clear();
            props.put("ServerName", "${rhq.server.database.server-name:127.0.0.1}");
            props.put("PortNumber", "${rhq.server.database.port:5432}");
            props.put("DatabaseName", "${rhq.server.database.db-name:rhq}");

            xaDsRequest = client.createNewXADatasourceRequest(RHQ_DATASOURCE_NAME_XA, 30000, JDBC_DRIVER_POSTGRES,
                "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter", 15, 5, 50, (Boolean) null,
                (Boolean) null, 75, (String) null, RHQ_DS_SECURITY_DOMAIN, (String) null, "TRANSACTION_READ_COMMITTED",
                "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker", props);

        } else {
            LOG.info("Postgres XA datasource [" + RHQ_DATASOURCE_NAME_XA + "] already exists");
        }

        if (noTxDsRequest != null || xaDsRequest != null) {
            ModelNode batch = DatasourceJBossASClient.createBatchRequest(noTxDsRequest, xaDsRequest);
            ModelNode results = client.execute(batch);
            if (!DatasourceJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create Postgres datasources");
            }
        }
    }

//    private static void createNewDatasources_Cassandra(ModelControllerClient mcc) throws Exception {
//        DatasourceJBossASClient client = new DatasourceJBossASClient(mcc);
//        String connectionURL = "jdbc:cassandra://localhost:9160/system?version=3.0.0";
//        ModelNode dsRequest = client.createNewDatasourceRequest(CASSANDRA_DATASOURCE_NAME, connectionURL,
//            JDBC_DRIVER_CASSANDRA, false, new HashMap<String, String>());
//
//        ModelNode batch = DatasourceJBossASClient.createBatchRequest(dsRequest);
//        ModelNode results = client.execute(batch);
//        if (!DatasourceJBossASClient.isSuccess(results)) {
//            throw new FailureException(results, "Failed to create Cassandra data source");
//        } else {
//            LOG.info("Created Cassandra data source");
//        }
//    }

    private static void createNewDatasources_Oracle(ModelControllerClient mcc) throws Exception {
        final HashMap<String, String> props = new HashMap<String, String>(2);
        final DatasourceJBossASClient client = new DatasourceJBossASClient(mcc);

        ModelNode noTxDsRequest = null;
        ModelNode xaDsRequest = null;

        if (!client.isDatasource(RHQ_DATASOURCE_NAME_NOTX)) {
            props.put("char.encoding", "UTF-8");

            noTxDsRequest = client.createNewDatasourceRequest(RHQ_DATASOURCE_NAME_NOTX, 30000,
                "${rhq.server.database.connection-url:jdbc:oracle:thin:@127.0.0.1:1521:rhq}", JDBC_DRIVER_ORACLE,
                "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter", 15, false, 2, 5, 75,
                RHQ_DS_SECURITY_DOMAIN, "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker",
                "TRANSACTION_READ_COMMITTED",
                "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker", props);
        } else {
            LOG.info("Oracle datasource [" + RHQ_DATASOURCE_NAME_NOTX + "] already exists");
        }

        if (!client.isDatasource(RHQ_DATASOURCE_NAME_XA)) {
            props.clear();
            props.put("URL", "${rhq.server.database.connection-url:jdbc:oracle:thin:@127.0.0.1:1521:rhq}");

            xaDsRequest = client.createNewXADatasourceRequest(RHQ_DATASOURCE_NAME_XA, 30000, JDBC_DRIVER_ORACLE,
                "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter", 15, 5, 50, (Boolean) null,
                Boolean.TRUE, 75, (String) null, RHQ_DS_SECURITY_DOMAIN,
                "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker",
                "TRANSACTION_READ_COMMITTED",
                "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker", props);
        } else {
            LOG.info("Oracle XA datasource [" + RHQ_DATASOURCE_NAME_XA + "] already exists");
        }

        if (noTxDsRequest != null || xaDsRequest != null) {
            ModelNode batch = DatasourceJBossASClient.createBatchRequest(noTxDsRequest, xaDsRequest);
            ModelNode results = client.execute(batch);
            if (!DatasourceJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create Oracle datasources");
            }
        }
    }

    /**
     * Determines if we are in auto-install mode. This means the properties file is
     * fully configured and the installation can begin without asking the user
     * for more input.
     *
     * @param serverProperties the full set of server properties
     *
     * @return true if we are in auto-install mode; false if the user must give us more
     *         information before we can complete the installation.
     */
    public static boolean isAutoinstallEnabled(HashMap<String, String> serverProperties) {
        String enableProp = serverProperties.get(ServerProperties.PROP_AUTOINSTALL_ENABLE);
        if (enableProp != null) {
            return Boolean.parseBoolean(enableProp);
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the database already has the database schema created for it. It will not be known
     * what version of schema or if its the latest, all this method tells you is that some RHQ database schema exists.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return <code>true</code> if the database can be connected to
     *
     * @throws Exception if failed to communicate with the database
     */
    public static boolean isDatabaseSchemaExist(String connectionUrl, String username, String password)
        throws Exception {

        Connection conn = getDatabaseConnection(connectionUrl, username, password);
        DatabaseType db = DatabaseTypeFactory.getDatabaseType(conn);

        try {
            return db.checkTableExists(conn, "RHQ_PRINCIPAL");
        } catch (IllegalStateException e) {
            return false;
        } finally {
            db.closeConnection(conn);
        }
    }

    /**
     * Get the list of existing servers from an existing schema.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return List of server names registered in the database. Empty list if the table does not exist or there are no entries in the table.
     *
     * @throws Exception if failed to communicate with the database
     */
    public static ArrayList<String> getServerNames(String connectionUrl, String username, String password)
        throws Exception {
        DatabaseType db = null;
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        ArrayList<String> result = new ArrayList<String>();

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            if (db.checkTableExists(conn, "rhq_server")) {

                stm = conn.createStatement();
                rs = stm.executeQuery("SELECT name FROM rhq_server ORDER BY name asc");

                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        } catch (IllegalStateException e) {
            // table does not exist
        } catch (SQLException e) {
            LOG.info("Unable to fetch existing server info: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeJDBCObjects(conn, stm, rs);
            }
        }

        return result;
    }

    /**
     * Returns information on the server as found in the database (port numbers, affinity group, etc).
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @param serverName the server whose details are to be returned
     * @return the information on the named server
     */
    public static ServerDetails getServerDetails(String connectionUrl, String username, String password,
        String serverName) {

        DatabaseType db = null;
        Connection conn = null;
        ServerDetails result = null;

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            result = getServerDetails(db, conn, serverName);

        } catch (Exception e) {
            LOG.info("Unable to get server detail: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeConnection(conn);
            }
        }

        return result;
    }

    private static ServerDetails getServerDetails(DatabaseType db, Connection conn, String serverName) {
        PreparedStatement stm = null;
        ResultSet rs = null;
        ServerDetails result = null;

        if (null == serverName) {
            return result;
        }

        try {
            stm = conn.prepareStatement("" //
                + "SELECT s.address, s.port, s.secure_port " //
                + "  FROM rhq_server s " //
                + " WHERE s.name = ?");
            stm.setString(1, serverName.trim());

            rs = stm.executeQuery();

            if (rs.next()) {
                result = new ServerDetails(serverName, rs.getString(1), rs.getInt(2), rs.getInt(3));
            }
        } catch (SQLException e) {
            LOG.info("Unable to get server details for server [" + serverName + "]: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeResultSet(rs);
                db.closeStatement(stm);
            }
        }

        return result;
    }

    /**
     * Tests to make sure the server can be connected to with the given settings.
     * If the test is successful, <code>null</code>. If the test fails, the returned string
     * will be the error message to indicate the problem.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return error message if test failed; <code>null</code> if test succeeded
     */
    public static String testConnection(String connectionUrl, String username, String password) {

        // its possible the JDBC URL was changed, clear the factory cache in case the DB version is different now
        DatabaseTypeFactory.clearDatabaseTypeCache();

        try {
            ensureDatabaseIsSupported(connectionUrl, username, password);
            return null;
        } catch (Exception e) {
            LOG.warn("Installer failed to test connection", e);
            return ThrowableUtil.getAllMessages(e);
        }
    }

    /**
     * Call this when you need to confirm that the database is supported.
     *
     * @param connectionUrl
     * @param username
     * @param password
     *
     * @throws Exception if the database is not supported
     */
    public static void ensureDatabaseIsSupported(String connectionUrl, String username, String password)
        throws Exception {
        Connection conn = null;
        DatabaseType db = null;

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            String version = db.getVersion();

            if (DatabaseTypeFactory.isPostgres(db)) {
                if (version.startsWith("7") || version.equals("8") || version.startsWith("8.0")
                    || version.startsWith("8.1")) {
                    throw new Exception("Unsupported PostgreSQL [" + db + "]");
                }
            } else if (DatabaseTypeFactory.isOracle(db)) {
                if (version.startsWith("8") || version.startsWith("9")) {
                    throw new Exception("Unsupported Oracle [" + db + "]");
                }
            } else {
                throw new Exception("Unsupported DB [" + db + "]");
            }

            LOG.info("Database is supported: " + db);
        } finally {
            if (db != null) {
                db.closeConnection(conn);
            }
        }

        return;
    }

    /**
     * Returns a database connection with the given set of properties providing the settings that allow for a successful
     * database connection. If <code>props</code> is <code>null</code>, it will use the server properties from
     * {@link #getServerProperties()}.
     *
     * @param connectionUrl 
     * @param userName 
     * @param password 
     * @return the database connection
     *
     * @throws SQLException if cannot successfully connect to the database
     */
    public static Connection getDatabaseConnection(String connectionUrl, String userName, String password)
        throws SQLException {
        return DbUtil.getConnection(connectionUrl, userName, password);
    }

    /**
     * Use the internal JBossAS mechanism to obfuscate a password. This is not true encryption.
     *
     * @param password the clear text of the password to obfuscate
     * @return the obfuscated password
     */
    public static String obfuscatePassword(String password) {

        // We need to do some mumbo jumbo, as the interesting method is private
        // in SecureIdentityLoginModule

        try {
            String className = "org.picketbox.datasource.security.SecureIdentityLoginModule";
            Class<?> clazz = Class.forName(className);
            Object object = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("encode", String.class);
            method.setAccessible(true);
            String result = method.invoke(object, password).toString();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("obfuscating db password failed: ", e);
        }
    }

    /**
     * Use the internal JBossAS mechanism to de-obfuscate a password back to its
     * clear text form. This is not true encryption.
     *
     * @param obfuscatedPasswordd the obfuscated password
     * @return the clear-text password
     */
    public static String deobfuscatePassword(String obfuscatedPassword) {

        // We need to do some mumbo jumbo, as the interesting method is private
        // in SecureIdentityLoginModule

        try {
            String className = "org.picketbox.datasource.security.SecureIdentityLoginModule";
            Class<?> clazz = Class.forName(className);
            Object object = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("decode", String.class);
            method.setAccessible(true);
            char[] result = (char[]) method.invoke(object, obfuscatedPassword);
            return new String(result);
        } catch (Exception e) {
            throw new RuntimeException("de-obfuscating db password failed: ", e);
        }
    }

    /**
     * Stores the server details (such as the public endpoint) in the database. If the server definition already
     * exists, it will be updated; otherwise, a new server will be added to the HA cloud.
     *
     * @param serverProperties the server properties
     * @param password clear text password to connect to the database
     * @param serverDetails the details of the server to put into the database
     * @throws Exception
     */
    public static void storeServerDetails(HashMap<String, String> serverProperties, String password,
        ServerDetails serverDetails) throws Exception {

        DatabaseType db = null;
        Connection conn = null;

        try {
            String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            String userName = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
            conn = getDatabaseConnection(dbUrl, userName, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            updateOrInsertServer(db, conn, serverDetails);

        } catch (SQLException e) {
            // TODO: should we throw an exception here? This would abort the rest of the installation
            LOG.info("Unable to store server entry in the database: " + ThrowableUtil.getAllMessages(e));
        } finally {
            if (null != db) {
                db.closeConnection(conn);
            }
        }
    }

    private static void updateOrInsertServer(DatabaseType db, Connection conn, ServerDetails serverDetails) {
        PreparedStatement stm = null;
        ResultSet rs = null;

        if (null == serverDetails || isEmpty(serverDetails.getName())) {
            return;
        }

        try {
            stm = conn.prepareStatement("UPDATE rhq_server SET address=?, port=?, secure_port=? WHERE name=?");
            stm.setString(1, serverDetails.getEndpointAddress());
            stm.setInt(2, serverDetails.getEndpointPort());
            stm.setInt(3, serverDetails.getEndpointSecurePort());
            stm.setString(4, serverDetails.getName());
            if (0 == stm.executeUpdate()) {
                stm.close();

                // set all new servers to operation_mode=INSTALLED
                int i = 1;
                if (db instanceof PostgresqlDatabaseType || db instanceof OracleDatabaseType) {
                    stm = conn.prepareStatement("INSERT INTO rhq_server " //
                        + " ( id, name, address, port, secure_port, ctime, mtime, operation_mode, compute_power ) " //
                        + "VALUES ( ?, ?, ?, ?, ?, ?, ?, 'INSTALLED', 1 )");
                    stm.setInt(i++, db.getNextSequenceValue(conn, "rhq_server", "id"));
                } else {
                    throw new IllegalArgumentException("Unknown database type, can't continue: " + db);
                }

                stm.setString(i++, serverDetails.getName());
                stm.setString(i++, serverDetails.getEndpointAddress());
                stm.setInt(i++, serverDetails.getEndpointPort());
                stm.setInt(i++, serverDetails.getEndpointSecurePort());
                long now = System.currentTimeMillis();
                stm.setLong(i++, now);
                stm.setLong(i++, now);
                stm.executeUpdate();
            }

        } catch (SQLException e) {
            LOG.info("Unable to put the server details in the database: " + ThrowableUtil.getAllMessages(e));
        } finally {
            if (null != db) {
                db.closeResultSet(rs);
                db.closeStatement(stm);
            }
        }
    }

    /**
     * This will create the database schema in the database. <code>props</code> define the connection to the database -
     *
     * <p>Note that if the {@link #isDatabaseSchemaExist(Properties) schema already exists}, it will be purged of all
     * data/tables and recreated.</p>
     *
     * @param props the full set of server properties
     * @param serverDetails additional information about the server being installed
     * @param password the database password in clear text
     * @param logDir a directory where the db schema upgrade logs can be written
     *
     * @throws Exception if failed to create the new schema for some reason
     */
    public static void createNewDatabaseSchema(HashMap<String, String> props, ServerDetails serverDetails,
        String password, String logDir) throws Exception {
        String dbUrl = props.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        String userName = props.get(ServerProperties.PROP_DATABASE_USERNAME);

        try {
            // extract the dbsetup files which are located in the dbutils jar
            String dbsetupSchemaXmlFile = extractDatabaseXmlFile("db-schema-combined.xml", props, serverDetails, logDir);
            String dbsetupDataXmlFile = extractDatabaseXmlFile("db-data-combined.xml", props, serverDetails, logDir);

            // first uninstall any old existing schema, then create the tables then insert the data
            DBSetup dbsetup = new DBSetup(dbUrl, userName, password);
            dbsetup.uninstall(dbsetupSchemaXmlFile);
            dbsetup.setup(dbsetupSchemaXmlFile);
            dbsetup.setup(dbsetupDataXmlFile, null, true, false);
        } catch (Exception e) {
            LOG.fatal("Cannot install the database schema - the server will not run properly.", e);
            throw e;
        }

        return;
    }

    /**
     * This will update an existing database schema so it can be upgraded to the latest schema version.
     *
     * <p>Note that if the {@link #isDatabaseSchemaExist(Properties) schema does not already exist}, errors will
     * occur.</p>
     *
     * @param props the full set of server properties
     * @param serverDetails additional information about the server being installed
     * @param password the database password in clear text
     * @param logDir a directory where the db schema upgrade logs can be written
     *
     * @throws Exception if the upgrade failed for some reason
     */
    public static void upgradeExistingDatabaseSchema(HashMap<String, String> props, ServerDetails serverDetails,
        String password, String logDir) throws Exception {
        String dbUrl = props.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        String userName = props.get(ServerProperties.PROP_DATABASE_USERNAME);

        File logfile = new File(logDir, "rhq-installer-dbupgrade.log");

        logfile.delete(); // do not keep logs from previous dbupgrade runs

        try {
            // extract the dbupgrade ANT script which is located in the dbutils jar
            String dbupgradeXmlFile = extractDatabaseXmlFile("db-upgrade.xml", props, serverDetails, logDir);

            Properties antProps = new Properties();
            antProps.setProperty("jdbc.url", dbUrl);
            antProps.setProperty("jdbc.user", userName);
            antProps.setProperty("jdbc.password", password);
            antProps.setProperty("target.schema.version", "LATEST");

            startAnt(new File(dbupgradeXmlFile), "db-ant-tasks.properties", antProps, logfile);
        } catch (Exception e) {
            LOG.fatal("Cannot upgrade the database schema - the server will not run properly.", e);
            throw e;
        }

        return;
    }

    public static void installOrUpdateCassandraSchema(HashMap<String, String> props) {
        String[] hosts = props.get("rhq.cassandra.cluster.seeds").split(",");
        String username = props.get("rhq.cassandra.username");
        String password = props.get("rhq.cassandra.password");

        SchemaManager schemaManager = new SchemaManager(username, password, hosts);
        if (!schemaManager.schemaExists()) {
            schemaManager.createSchema();
        }
        schemaManager.updateSchema();
    }

    /**
     * Given a server property value string, returns true if it is not specified.
     *
     * @param s the property string value
     *
     * @return true if it is null or empty
     */
    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    /**
     * Takes the named XML file from the classloader and writes the file to the log directory. This is meant to extract
     * the schema/data xml files from the dbutils jar file. It can also be used to extract the db upgrade XML file.
     *
     * @param xmlFileName the name of the XML file, as found in the classloader
     * @param props properties whose values are used to replace the replacement strings found in the XML file
     * @param serverDetails additional information about the server being installed
     * @param logDir a directory where the db schema upgrade logs can be written
     *
     * @return the absolute path to the extracted file
     *
     * @throws IOException if failed to extract the file to the log directory
     */
    private static String extractDatabaseXmlFile(String xmlFileName, HashMap<String, String> props,
        ServerDetails serverDetails, String logDir) throws IOException {

        // first slurp the file contents in memory
        InputStream resourceInStream = ServerInstallUtil.class.getClassLoader().getResourceAsStream(xmlFileName);
        ByteArrayOutputStream contentOutStream = new ByteArrayOutputStream();
        StreamUtil.copy(resourceInStream, contentOutStream);

        // now replace their replacement strings with values from the properties
        String emailFromAddress = props.get(ServerProperties.PROP_EMAIL_FROM_ADDRESS);
        if (isEmpty(emailFromAddress)) {
            emailFromAddress = "rhqadmin@localhost";
        }

        String httpPort = props.get(ServerProperties.PROP_WEB_HTTP_PORT);
        if (isEmpty(httpPort)) {
            httpPort = String.valueOf(ServerDetails.DEFAULT_ENDPOINT_PORT);
        }

        String publicEndpoint = serverDetails.getEndpointAddress();
        if (isEmpty(publicEndpoint)) {
            try {
                publicEndpoint = props.get(ServerProperties.PROP_JBOSS_BIND_ADDRESS);
                if (isEmpty(publicEndpoint) || ("0.0.0.0".equals(publicEndpoint))) {
                    publicEndpoint = InetAddress.getLocalHost().getHostAddress();
                }
            } catch (Exception e) {
                publicEndpoint = "127.0.0.1";
            }
        }

        String content = contentOutStream.toString();
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_DATA@@@", "DEFAULT");
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_INDEX@@@", "DEFAULT");
        content = content.replaceAll("@@@ADMINUSERNAME@@@", "rhqadmin");
        content = content.replaceAll("@@@ADMINPASSWORD@@@", "x1XwrxKuPvYUILiOnOZTLg=="); // rhqadmin
        content = content.replaceAll("@@@ADMINEMAIL@@@", emailFromAddress);
        content = content.replaceAll("@@@BASEURL@@@", "http://" + publicEndpoint + ":" + httpPort + "/");
        content = content.replaceAll("@@@JAASPROVIDER@@@", "JDBC");
        content = content.replaceAll("@@@LDAPURL@@@", "ldap://localhost/");
        content = content.replaceAll("@@@LDAPPROTOCOL@@@", "");
        content = content.replaceAll("@@@LDAPLOGINPROP@@@", "cn");
        content = content.replaceAll("@@@LDAPBASEDN@@@", "o=JBoss,c=US");
        content = content.replaceAll("@@@LDAPSEARCHFILTER@@@", "");
        content = content.replaceAll("@@@LDAPBINDDN@@@", "");
        content = content.replaceAll("@@@LDAPBINDPW@@@", "");
        content = content.replaceAll("@@@MULTICAST_ADDR@@@", "");
        content = content.replaceAll("@@@MULTICAST_PORT@@@", "");

        // we now have the finished XML content - write out the file to the log directory
        File xmlFile = new File(logDir, xmlFileName);
        FileOutputStream xmlFileOutStream = new FileOutputStream(xmlFile);
        ByteArrayInputStream contentInStream = new ByteArrayInputStream(content.getBytes());
        StreamUtil.copy(contentInStream, xmlFileOutStream);

        return xmlFile.getAbsolutePath();
    }

    /**
     * Launches ANT and runs the default target in the given build file.
     *
     * @param  buildFile      the build file that ANT will run
     * @param  customTaskDefs the properties file found in classloader that contains all the taskdef definitions
     * @param  properties     set of properties to set for the ANT task to access
     * @param  logFile        where ANT messages will be logged (in addition to the app server's log file)
     *
     * @throws RuntimeException
     */
    private static void startAnt(File buildFile, String customTaskDefs, Properties properties, File logFile) {
        PrintWriter logFileOutput = null;

        try {
            logFileOutput = new PrintWriter(new FileOutputStream(logFile));

            ClassLoader classLoader = ServerInstallUtil.class.getClassLoader();

            Properties taskDefs = new Properties();
            InputStream taskDefsStream = classLoader.getResourceAsStream(customTaskDefs);
            try {
                taskDefs.load(taskDefsStream);
            } finally {
                taskDefsStream.close();
            }

            Project project = new Project();
            project.setCoreLoader(classLoader);
            project.init();

            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                project.setProperty(property.getKey().toString(), property.getValue().toString());
            }

            // notice we add our listener after we set the properties - we do not want the password to be in the log file
            // our dbupgrade script will echo the property settings, so we can still get the other values
            project.addBuildListener(new LoggerAntBuildListener(logFileOutput));

            for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
                project.addTaskDefinition(taskDef.getKey().toString(),
                    Class.forName(taskDef.getValue().toString(), true, classLoader));
            }

            new ProjectHelper2().parse(project, buildFile);
            project.executeTarget(project.getDefaultTarget());

        } catch (Exception e) {
            throw new RuntimeException("Cannot run ANT on script [" + buildFile + "]. Cause: " + e, e);
        } finally {
            if (logFileOutput != null) {
                logFileOutput.close();
            }
        }
    }

    /**
     * Creates a keystore whose cert has a CN of this server's public endpoint address.
     * 
     * @param serverDetails details of the server being installed
     * @param configDirStr location of a configuration directory where the keystore is to be stored
     */
    public static void createKeystore(ServerDetails serverDetails, String configDirStr) {
        File confDir = new File(configDirStr);
        File keystore = new File(confDir, "rhq.keystore");
        File keystoreBackup = new File(confDir, "rhq.keystore.backup");

        // if there is one out-of-box, we want to remove it and create one with our proper CN
        if (keystore.exists()) {
            keystoreBackup.delete();
            if (!keystore.renameTo(keystoreBackup)) {
                LOG.warn("Cannot backup existing keystore - cannot generate a new cert with a proper domain name. ["
                    + keystore + "] will be the keystore used by this server");
                return;
            }
        }

        try {
            String keystorePath = keystore.getAbsolutePath();
            String keyAlias = "RHQ";
            String domainName = "CN=" + serverDetails.getEndpointAddress() + ", OU=RHQ, O=rhq-project.org, C=US";
            String keystorePassword = "RHQManagement";
            String keyPassword = keystorePassword;
            String keyAlgorithm = "rsa";
            int validity = 7300;
            SecurityUtil.createKeyStore(keystorePath, keyAlias, domainName, keystorePassword, keyPassword,
                keyAlgorithm, validity);
            LOG.info("New keystore created [" + keystorePath + "] with cert domain name of [" + domainName + "]");
        } catch (Exception e) {
            LOG.warn("Could not generate a new cert with a proper domain name, will use the original keystore");
            keystore.delete();
            if (!keystoreBackup.renameTo(keystore)) {
                LOG.warn("Failed to restore the original keystore from backup - please rename [" + keystoreBackup
                    + "] to [" + keystore + "]");
            }
        }
    }

    /**
     * Create an rhqadmin/rhqadmin management user so when discovered, the AS7 plugin can immediately
     * connect to the RHQ Server.
     * 
     * @param serverDetails details of the server being installed
     * @param configDirStr location of a configuration directory where the mgmt-users.properties file lives
     */
    public static void createDefaultManagementUser(ServerDetails serverDetails, String configDirStr) {
        File confDir = new File(configDirStr);
        File mgmtUsers = new File(confDir, "mgmt-users.properties");

        // Add the default admin user, or if for some reason this file does not exist, just log the issue
        if (mgmtUsers.exists()) {
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(mgmtUsers, true);
                fos.write("\nrhqadmin=35c160c1f841a889d4cda53f0bfc94b6\n".getBytes());

            } catch (Exception e) {
                LOG.warn("Could not create default management user in file: [" + mgmtUsers.getPath() + "] : ", e);

            } finally {
                StreamUtil.safeClose(fos);
            }
        } else {
            LOG.warn("Could not create default management user. Could not find file: [" + mgmtUsers.getPath() + "]");
        }
    }

    public static void setSocketBindings(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {
        final SocketBindingJBossASClient client = new SocketBindingJBossASClient(mcc);
        for (SocketBindingInfo binding : defaultSocketBindings) {
            // use the port defined by the server's properties if set, otherwise, just use our hardcoded default
            int newPort = binding.port;
            String overrideValue = serverProperties.get(binding.sysprop);
            if (overrideValue != null) {
                try {
                    newPort = Integer.parseInt(overrideValue);
                } catch (Exception e) {
                    LOG.warn("Invalid port in system property [" + binding.sysprop + "]: " + overrideValue);
                }
            }
            LOG.info(String.format("Setting socket binding [%s] to [${%s:%d}]", binding.name, binding.sysprop, newPort));
            client.setStandardSocketBindingPortExpression(binding.name, binding.sysprop, newPort);
        }
    }

}
