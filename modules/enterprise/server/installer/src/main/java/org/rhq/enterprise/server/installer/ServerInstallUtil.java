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

package org.rhq.enterprise.server.installer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.sasl.util.UsernamePasswordHashUtil;

import org.rhq.common.jbossas.client.controller.Address;
import org.rhq.common.jbossas.client.controller.CoreJBossASClient;
import org.rhq.common.jbossas.client.controller.DatasourceJBossASClient;
import org.rhq.common.jbossas.client.controller.FailureException;
import org.rhq.common.jbossas.client.controller.InfinispanJBossASClient;
import org.rhq.common.jbossas.client.controller.JBossASClient;
import org.rhq.common.jbossas.client.controller.LoggingJBossASClient;
import org.rhq.common.jbossas.client.controller.MessagingJBossASClient;
import org.rhq.common.jbossas.client.controller.SecurityDomainJBossASClient;
import org.rhq.common.jbossas.client.controller.SocketBindingJBossASClient;
import org.rhq.common.jbossas.client.controller.TransactionsJBossASClient;
import org.rhq.common.jbossas.client.controller.VaultJBossASClient;
import org.rhq.common.jbossas.client.controller.WebJBossASClient;
import org.rhq.common.jbossas.client.controller.WebJBossASClient.ConnectorConfiguration;
import org.rhq.common.jbossas.client.controller.WebJBossASClient.SSLConfiguration;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.setup.DBSetup;
import org.rhq.core.db.upgrade.ServerVersionColumnUpgrader;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.obfuscation.ObfuscatedPreferences.RestrictedFormat;
import org.rhq.core.util.obfuscation.PropertyObfuscationVault;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * Provides utility methods necessary to complete the server installation.
 *
 * @author John Mazzitelli
 */
public class ServerInstallUtil {
    private static final Log LOG = LogFactory.getLog(ServerInstallUtil.class);
    private static final String version;

    static {
        version = ServerInstallUtil.class.getPackage().getImplementationVersion();
    }

    public enum ExistingSchemaOption {
        OVERWRITE, KEEP, SKIP
    }

    public enum SupportedDatabaseType {
        POSTGRES, ORACLE
    }

    private static class SocketBindingInfo {
        public String name;
        public String sysprop;
        public int port;
        public boolean required = true;
        public String interfaceName = null; // not null if we know we want it to be changed from its default setting

        public SocketBindingInfo(String n, String s, int p) {
            this.name = n;
            this.sysprop = s;
            this.port = p;
        }

        public SocketBindingInfo(String n, String s, int p, String i) {
            this.name = n;
            this.sysprop = s;
            this.port = p;
            this.interfaceName = i;
        }

        //        public SocketBindingInfo(String name, String sysprop, int port, String interfaceName, boolean required) {
        //            this.name = name;
        //            this.sysprop = sysprop;
        //            this.port = port;
        //            this.interfaceName = interfaceName;
        //            this.required = required;
        //        }
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
            "rhq.server.socket.binding.port.messaging", 4449, "management"));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MESSAGING_THRUPUT,
            "rhq.server.socket.binding.port.messaging-throughput", 4455, "management"));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MGMT_HTTP,
            "jboss.management.http.port", 6990));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MGMT_HTTPS,
            "jboss.management.https.port", 6443));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_MGMT_NATIVE,
            "jboss.management.native.port", 6999));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_REMOTING,
            "rhq.server.socket.binding.port.remoting", 3447, "management"));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_TXN_RECOVERY_ENV,
            "rhq.server.socket.binding.port.txn-recovery-environment", 3712));
        defaultSocketBindings.add(new SocketBindingInfo(SocketBindingJBossASClient.DEFAULT_BINDING_TXN_STATUS_MGR,
            "rhq.server.socket.binding.port.txn-status-manager", 3713));
    }

    private static final String RHQ_DATASOURCE_NAME_NOTX = "NoTxRHQDS";
    private static final String RHQ_DATASOURCE_NAME_XA = "RHQDS";
    private static final String RHQ_DS_SECURITY_DOMAIN_NOTX = "RHQDSSecurityDomainNoTx";
    private static final String RHQ_DS_SECURITY_DOMAIN_XA = "RHQDSSecurityDomainXa";
    private static final String RHQ_USER_SECURITY_DOMAIN = "RHQUserSecurityDomain";
    private static final String RHQ_REST_SECURITY_DOMAIN = "RHQRESTSecurityDomain";
    private static final String JDBC_LOGIN_MODULE_NAME = "org.rhq.enterprise.server.core.jaas.JDBCLoginModule";
    private static final String DELEGATING_LOGIN_MODULE_NAME = "org.rhq.enterprise.server.core.jaas.DelegatingLoginModule";
    private static final String JDBC_DRIVER_POSTGRES = "postgres";
    private static final String JDBC_DRIVER_ORACLE = "oracle";
    private static final String JMS_ALERT_CONDITION_QUEUE = "AlertConditionQueue";
    private static final String JMS_DRIFT_CHANGESET_QUEUE = "DriftChangesetQueue";
    private static final String JMS_DRIFT_FILE_QUEUE = "DriftFileQueue";
    private static final String RHQ_CACHE_CONTAINER = "rhq";
    private static final String RHQ_CACHE = "rhqCache";
    private static final String RHQ_REST_CACHE = "rhqRestCache";
    private static final String RHQ_MGMT_USER = "rhqadmin";
    private static final String XA_DATASOURCE_CLASS_POSTGRES = "org.postgresql.xa.PGXADataSource";
    private static final String XA_DATASOURCE_CLASS_ORACLE = "oracle.jdbc.xa.client.OracleXADataSource";

    /**
     * Configure the logging subsystem.
     * @param mcc JBossAS management client
     * @param serverProperties the server properties, which includes the default log level to use
     * @throws Exception
     */
    public static void configureLogging(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {
        LoggingJBossASClient client = new LoggingJBossASClient(mcc);

        // we want to create our own category
        String val = buildExpression(ServerProperties.PROP_LOG_LEVEL, serverProperties, true);
        client.setLoggerLevel("org.rhq", val);
        LOG.info("Logging category org.rhq set to [" + val + "]");

        client.setLoggerLevel("org.jboss.as.config", "INFO"); // BZ 1004730
        client.setLoggerLevel("org.rhq.enterprise.gui.common.framework", "FATAL"); // BZ 994267

        //noinspection StringBufferReplaceableByString
        StringBuilder sb = new StringBuilder("not(any(");
        sb.append("match(\"JBAS015960\")"); // BZ 1026786
        sb.append(",");
        sb.append("match(\"JBAS018567\")"); // BZ 1026786
        sb.append(",");
        sb.append("match(\"JBAS018568\")"); // BZ 1026786
        sb.append(",");
        sb.append("match(\"JSF1051\")"); // BZ 1078500
        sb.append(",");
        sb.append("match(\"PBOX00023\")"); // BZ 1026799, BZ 1144998
        sb.append(",");
        sb.append("match(\"IJ000906\")"); // BZ 1127272, waiting for fix of https://bugzilla.redhat.com/show_bug.cgi?id=1032641
        sb.append(",");
        sb.append("match(\"ARJUNA016009\")"); //BZ 1144998, waiting for fix of https://issues.jboss.org/browse/WFLY-2828
        sb.append(",");
        sb.append("match(\"JBAS014807\")"); //BZ 1144998, missing web subsystem is expected at times
        sb.append(",");
        sb.append("match(\"admin/user/UserAdminPortal\")"); //BZ 994267
        sb.append("))");
        client.setFilterSpec(sb.toString());
    }

    /**
     * Configure the transaction manager.
     * @param mcc JBossAS management client
     * @throws Exception
     */
    public static void configureTransactionManager(ModelControllerClient mcc) throws Exception {
        TransactionsJBossASClient client = new TransactionsJBossASClient(mcc);

        // we want to bump up the transaction timeout
        client.setDefaultTransactionTimeout(600);
        LOG.info("Default transaction timeout set to 600 seconds.");
    }

    /**
     * Configure the deployment scanner.
     * @param mcc JBossAS management client
     * @throws Exception
     */
    public static void configureDeploymentScanner(ModelControllerClient mcc) throws Exception {
        CoreJBossASClient client = new CoreJBossASClient(mcc);

        // we do not want our RHQ Server to support hot deployments via the scanner
        client.setAppServerDefaultDeploymentScanEnabled(false);
        LOG.info("Deployment scanner turned off.");
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

        String fromAddressExpr = buildExpression(ServerProperties.PROP_EMAIL_FROM_ADDRESS, serverProperties, true);
        String smtpHostExpr = buildExpression(ServerProperties.PROP_EMAIL_SMTP_HOST, serverProperties, true);
        String smtpPortExpr = buildExpression(ServerProperties.PROP_EMAIL_SMTP_PORT, serverProperties, true);

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
        JBossASClient.setPossibleExpression(writeHost, JBossASClient.VALUE, smtpHostExpr);

        // now the SMTP port
        addr = Address.root().add("socket-binding-group", "standard-sockets",
            "remote-destination-outbound-socket-binding", "mail-smtp");
        ModelNode writePort = JBossASClient.createRequest(JBossASClient.WRITE_ATTRIBUTE, addr);
        writePort.get(JBossASClient.NAME).set("port");
        JBossASClient.setPossibleExpression(writePort, JBossASClient.VALUE, smtpPortExpr);

        ModelNode batch = JBossASClient.createBatchRequest(writeFromAddr, writeHost, writePort);
        JBossASClient client = new JBossASClient(mcc);
        ModelNode response = client.execute(batch);
        if (!JBossASClient.isSuccess(response)) {
            throw new FailureException(response, "Failed to setup mail service");
        }
        LOG.info("Mail service has been configured.");
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
        if (dbType.toLowerCase().contains("postgres")) {
            return SupportedDatabaseType.POSTGRES;
        } else if (dbType.toLowerCase().contains("oracle")) {
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

        final String dbUsername = buildExpression(ServerProperties.PROP_DATABASE_USERNAME, serverProperties, true);
        final String obfuscatedPassword = buildExpression(ServerProperties.PROP_DATABASE_PASSWORD, serverProperties,
            true);
        final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);
        String securityDomain = RHQ_DS_SECURITY_DOMAIN_XA;
        if (!client.isSecurityDomain(securityDomain)) {
            client.createNewSecureIdentitySecurityDomain72(securityDomain, dbUsername, obfuscatedPassword);
            LOG.info("Security domain [" + securityDomain + "] created");
        } else {
            LOG.info("Security domain [" + securityDomain + "] already exists, skipping the creation request");
            client.updateSecureIdentitySecurityDomainCredentials(securityDomain, dbUsername, obfuscatedPassword);
            LOG.info("Credentials have been updated for security domain [" + securityDomain + "]");
        }

        // we need separate security domains per datasource due to BZ 1102332
        securityDomain = RHQ_DS_SECURITY_DOMAIN_NOTX;
        if (!client.isSecurityDomain(securityDomain)) {
            client.createNewSecureIdentitySecurityDomain72(securityDomain, dbUsername, obfuscatedPassword);
            LOG.info("Security domain [" + securityDomain + "] created");
        } else {
            LOG.info("Security domain [" + securityDomain + "] already exists, skipping the creation request");
            client.updateSecureIdentitySecurityDomainCredentials(securityDomain, dbUsername, obfuscatedPassword);
            LOG.info("Credentials have been updated for security domain [" + securityDomain + "]");
        }
    }

    /**
     * Create the standard user security domain with the JDBCLogin module installed
     *
     * @param mcc ModelControllerClient to talk to the underlying AS
     * @throws Exception If anything goes wrong
     */
    public static void createUserSecurityDomain(ModelControllerClient mcc) throws Exception {

        Map<String, String> options = new HashMap<String, String>(2);
        options.put("hashAlgorithm", "MD5");
        options.put("hashEncoding", "base64");

        SecurityDomainJBossASClient.LoginModuleRequest loginModuleRequest = new SecurityDomainJBossASClient.LoginModuleRequest(
            JDBC_LOGIN_MODULE_NAME, AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, options);

        SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);
        client.createNewSecurityDomain(RHQ_USER_SECURITY_DOMAIN, loginModuleRequest);

    }

    /**
     * Create a security domain for container managed security used with the rhq-rest.war
     * @param mcc ModelControllerClient to talk to the underlying AS.
     * @throws Exception If anything goes wrong
     */
    public static void createRestSecurityDomain(ModelControllerClient mcc) throws Exception {

        Map<String, String> options = new HashMap<String, String>(2);
        options.put("delegateTo", RHQ_USER_SECURITY_DOMAIN);
        options.put("roles", "rest-user");

        SecurityDomainJBossASClient.LoginModuleRequest loginModuleRequest = new SecurityDomainJBossASClient.LoginModuleRequest(
            DELEGATING_LOGIN_MODULE_NAME, AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, options);

        SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);
        client.createNewSecurityDomain(RHQ_REST_SECURITY_DOMAIN, loginModuleRequest);
    }

    /**
     * Creates the Vault required for RHQ property obfuscation.
     *
     * @param mcc the JBossAS management client
     * @param serverProperties server properties
     * @throws Exception
     */
    public static void createObfuscationVault(ModelControllerClient mcc, HashMap<String, String> serverProperties)
        throws Exception {

        final VaultJBossASClient client = new VaultJBossASClient(mcc);

        if (!client.isVault()) {
            ModelNode request = client.createNewVaultRequest(PropertyObfuscationVault.class.getName());
            ModelNode results = client.execute(request);
            if (!VaultJBossASClient.isSuccess(results)) {
                String vaultClass = client.getVaultClass();
                if (PropertyObfuscationVault.class.getName().equals(vaultClass)) {
                    LOG.info("RHQ Vault already configured, vault detected on the second read attempt");
                } else {
                    throw new FailureException(results, "Failed to create the RHQ vault");
                }
            } else {
                LOG.info("RHQ Vault created");
            }
        } else {
            String vaultClass = client.getVaultClass();
            if (PropertyObfuscationVault.class.getName().equals(vaultClass)) {
                LOG.info("RHQ vault already configured, skipping the creation process");
            } else {
                throw new FailureException("Failed to create the RHQ vault; a different vault is already configured");
            }
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
        String localCacheName = RHQ_CACHE;
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
                null, null, null);
            ModelNode results = client.execute(request);
            if (!MessagingJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create Local Cache [" + localCacheName + "]");
            } else {
                LOG.info("Local Cache [" + localCacheName + "] created");
            }

        } else {
            LOG.info("Local Cache [" + localCacheName + "] already exists, skipping the creation request");
        }

        localCacheName = RHQ_REST_CACHE;
        final long lifeSpan = 15 * 60 * 1000L; // 15min
        final long maxIdle = 5 * 60 * 1000L; // 5min
        if (!client.isLocalCache(cacheContainerName, localCacheName)) {
            ModelNode request = client.createNewLocalCacheRequest(cacheContainerName, localCacheName, null, null, null,
                lifeSpan, maxIdle, null);
            ModelNode results = client.execute(request);
            if (!MessagingJBossASClient.isSuccess(results)) {
                throw new FailureException(results, "Failed to create Local Cache [" + localCacheName + "]");
            } else {
                LOG.info("Local Cache [" + localCacheName + "] created");
            }
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
                "${rhq.server.database.connection-url:jdbc:postgresql://127.0.0.1:5432/rhq}", JDBC_DRIVER_POSTGRES,
                "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter", 15, false, 2, 5, 75,
                RHQ_DS_SECURITY_DOMAIN_NOTX, "-unused-stale-conn-checker-", "TRANSACTION_READ_COMMITTED",
                "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker", true, props);
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
                XA_DATASOURCE_CLASS_POSTGRES,
                "org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter", 15, 5, 50, null,
                    null, 75, null, RHQ_DS_SECURITY_DOMAIN_XA, null,
                "TRANSACTION_READ_COMMITTED",
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
                RHQ_DS_SECURITY_DOMAIN_NOTX,
                "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker",
                "TRANSACTION_READ_COMMITTED",
                "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker", true, props);
        } else {
            LOG.info("Oracle datasource [" + RHQ_DATASOURCE_NAME_NOTX + "] already exists");
        }

        if (!client.isDatasource(RHQ_DATASOURCE_NAME_XA)) {
            props.clear();
            props.put("URL", "${rhq.server.database.connection-url:jdbc:oracle:thin:@127.0.0.1:1521:rhq}");

            xaDsRequest = client.createNewXADatasourceRequest(RHQ_DATASOURCE_NAME_XA, 30000, JDBC_DRIVER_ORACLE,
                XA_DATASOURCE_CLASS_ORACLE, "org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter", 15,
                5, 50, null, Boolean.TRUE, 75, null, RHQ_DS_SECURITY_DOMAIN_XA,
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
        return Boolean.parseBoolean(enableProp);
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
    }

    /**
     * Returns a database connection with the given set of properties providing the settings that allow for a successful
     * database connection. If <code>props</code> is <code>null</code>, it will use the server properties from
     * {@link #getServerProperties}.
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
     * @param serverProperties the server properties
     * @param dbpassword clear text password to connect to the database
     * @throws Exception
     */
    public static void persistAdminPasswordIfNecessary(HashMap<String, String> serverProperties, String dbpassword)
        throws Exception {
        DatabaseType db = null;
        Connection connection = null;
        Statement queryStatement = null;
        Statement insertStatement = null;
        ResultSet resultSet = null;

        try {
            String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            String userName = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
            connection = getDatabaseConnection(dbUrl, userName, dbpassword);
            db = DatabaseTypeFactory.getDatabaseType(connection);

            if (!(db instanceof PostgresqlDatabaseType || db instanceof OracleDatabaseType)) {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + db);
            }

            queryStatement = connection.createStatement();
            resultSet = queryStatement.executeQuery("SELECT count(*) FROM rhq_principal WHERE id=2");
            resultSet.next();

            if (resultSet.getInt(1) == 0) {
                connection.setAutoCommit(false);

                try {
                    LOG.info("Persisting admin password to database for property [rhq.autoinstall.server.admin.password]");

                    insertStatement = connection.createStatement();
                    insertStatement.executeUpdate("INSERT INTO rhq_principal VALUES (2, 'rhqadmin', '"
                        + serverProperties.get(ServerProperties.PROP_AUTOINSTALL_ADMIN_PASSWORD) + "')");

                    connection.commit();
                } catch (SQLException e) {
                    LOG.error(
                        "Failed to persist admin password to database for property [rhq.autoinstall.server.admin.password]. Transaction will be rolled back.",
                        e);
                    connection.rollback();
                    throw e;
                }
            } else {
                LOG.info("Admin user password is already set, property [rhq.autoinstall.server.admin.password] will be ignored.");
            }

        } finally {
            if (db != null) {
                db.closeResultSet(resultSet);
                db.closeStatement(queryStatement);
                db.closeStatement(insertStatement);
                db.closeConnection(connection);
            }
        }
    }

    /**
     * Delete all of the current storage nodes defined in the database, unless they are already managed (meaning,
     * at least one is linked to a resource). This can be useful for re-installs where the original install
     * may have misconfigured the storage node addresses.
     *
     * @param serverProperties the server properties
     * @param password clear text password to connect to the database
     * @param force, if true, all storageNodes are deleted regardless of whether they are managed.
     * @throws Exception
     */
    public static void deleteStorageNodes(HashMap<String, String> serverProperties, String password, boolean force)
        throws Exception {
        DatabaseType db = null;
        Connection connection = null;
        Statement queryStatement = null;
        ResultSet resultSet = null;
        PreparedStatement deleteStorageNodes = null;

        try {
            String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            String userName = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
            connection = getDatabaseConnection(dbUrl, userName, password);
            db = DatabaseTypeFactory.getDatabaseType(connection);

            if (!(db instanceof PostgresqlDatabaseType || db instanceof OracleDatabaseType)) {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + db);
            }

            // unless forced, delete storage nodes *only* if they are *all* unmanaged.
            queryStatement = connection.createStatement();
            resultSet = queryStatement
                .executeQuery("SELECT count(*) FROM rhq_storage_node sn WHERE NOT sn.resource_id IS NULL");
            resultSet.next();

            if (resultSet.getInt(1) == 0 || force) {
                connection.setAutoCommit(false);

                try {
                    deleteStorageNodes = connection.prepareStatement("DELETE FROM rhq_storage_node");
                    int numRemoved = deleteStorageNodes.executeUpdate();
                    if (numRemoved > 0) {
                        LOG.info("Removed [" + numRemoved + "] storage nodes. They will be redefined...");
                    }

                    connection.commit();
                } catch (SQLException e) {
                    LOG.error("Failed to delete storage nodes. Transaction will be rolled back.", e);
                    connection.rollback();
                    throw e;
                }
            } else {
                LOG.info("Managed Storage nodes already in database. Server configuration property [rhq.storage.nodes] will be ignored.");
            }

        } finally {
            if (db != null) {
                db.closeResultSet(resultSet);
                db.closeStatement(queryStatement);
                db.closeStatement(deleteStorageNodes);
                db.closeConnection(connection);
            }
        }
    }

    /**
     * Persists the storage nodes to the database only if no storage node entities already exist. This method is used
     * to persist storage nodes created from the rhq.storage.nodes server configuration property. The only time those
     * seed nodes should be created is during an initial server installation. After the initial installation storage
     * nodes should be created using <code>rhqctl install</code>. This ensures that any necessary cluster maintenance
     * tasks will be performed.
     *
     * @param serverProperties the server properties
     * @param password clear text password to connect to the database
     * @param storageNodes the {@link StorageNode storage nodes} to persist
     * @throws Exception
     */
    public static void persistStorageNodesIfNecessary(HashMap<String, String> serverProperties, String password,
        Set<StorageNode> storageNodes) throws Exception {
        DatabaseType db = null;
        Connection connection = null;
        Statement queryStatement = null;
        ResultSet resultSet = null;
        PreparedStatement insertStorageNode = null;
        PreparedStatement deleteStorageNodes = null;

        try {
            String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            String userName = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
            connection = getDatabaseConnection(dbUrl, userName, password);
            db = DatabaseTypeFactory.getDatabaseType(connection);

            if (!(db instanceof PostgresqlDatabaseType || db instanceof OracleDatabaseType)) {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + db);
            }

            // IF there are no current storage nodes then we can persist the specified storage nodes.
            queryStatement = connection.createStatement();
            resultSet = queryStatement.executeQuery("SELECT count(*) FROM rhq_storage_node sn");
            resultSet.next();

            if (resultSet.getInt(1) == 0) {
                connection.setAutoCommit(false);

                try {
                    LOG.info("Persisting to database new storage nodes for values specified in server configuration property [rhq.storage.nodes]");

                    insertStorageNode = connection
                        .prepareStatement("INSERT INTO rhq_storage_node (id, address, cql_port, operation_mode, ctime, mtime, maintenance_pending, version) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

                    int id = 1001;
                    for (StorageNode storageNode : storageNodes) {
                        insertStorageNode.setInt(1, id);
                        insertStorageNode.setString(2, storageNode.getAddress());
                        insertStorageNode.setInt(3, storageNode.getCqlPort());
                        insertStorageNode.setString(4, StorageNode.OperationMode.INSTALLED.toString());
                        insertStorageNode.setLong(5, System.currentTimeMillis());
                        insertStorageNode.setLong(6, System.currentTimeMillis());
                        insertStorageNode.setBoolean(7, false);
                        insertStorageNode.setString(8, version);

                        insertStorageNode.executeUpdate();
                        id += 1;
                    }

                    connection.commit();
                } catch (SQLException e) {
                    LOG.error("Failed to persist to database the storage nodes specified by server configuration "
                        + "property [rhq.storage.nodes]. Transaction will be rolled back.", e);
                    connection.rollback();
                    throw e;
                }
            } else {
                LOG.info("Storage nodes already exist in database. Server configuration property [rhq.storage.nodes] will be ignored.");
            }

        } finally {
            if (db != null) {
                db.closeResultSet(resultSet);
                db.closeStatement(queryStatement);
                db.closeStatement(deleteStorageNodes);
                db.closeStatement(insertStorageNode);
                db.closeConnection(connection);
            }
        }
    }

    public static Map<String, String> fetchStorageClusterSettings(HashMap<String, String> serverProperties,
        String password) throws Exception {

        Map<String, String> result = new HashMap<String, String>(5);
        DatabaseType db = null;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            String userName = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
            connection = getDatabaseConnection(dbUrl, userName, password);
            db = DatabaseTypeFactory.getDatabaseType(connection);

            if (!(db instanceof PostgresqlDatabaseType || db instanceof OracleDatabaseType)) {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + db);
            }

            try {
                statement = connection.prepareStatement("" //
                    + "SELECT property_key, property_value FROM rhq_system_config " //
                    + " WHERE property_key LIKE 'STORAGE%' " //
                    + "   AND NOT property_value IS NULL ");
                resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String key = resultSet.getString(1);
                    String value = resultSet.getString(2);

                    if (key.equals("STORAGE_USERNAME")) {
                        result.put(ServerProperties.PROP_STORAGE_USERNAME, value);
                    } else if (key.equals("STORAGE_PASSWORD")) {
                        result.put(ServerProperties.PROP_STORAGE_PASSWORD, value);
                    } else if (key.equals("STORAGE_GOSSIP_PORT")) {
                        result.put(ServerProperties.PROP_STORAGE_GOSSIP_PORT, value);
                    } else if (key.equals("STORAGE_CQL_PORT")) {
                        result.put(ServerProperties.PROP_STORAGE_CQL_PORT, value);
                    }
                }
            } finally {
                db.closeResultSet(resultSet);
                db.closeStatement(statement);
            }

            try {
                statement = connection.prepareStatement("" //
                    + "SELECT address FROM rhq_storage_node " //
                    + " WHERE operation_mode in " + "('" + OperationMode.NORMAL.name()
                    + "', '"
                    + OperationMode.INSTALLED.name() + "') ");
                resultSet = statement.executeQuery();

                StringBuilder addressList = new StringBuilder();
                while (resultSet.next()) {
                    String address = resultSet.getString(1);

                    if (address != null && !address.trim().isEmpty()) {
                        if (addressList.length() != 0) {
                            addressList.append(',');
                        }
                        addressList.append(address);
                    }
                }

                if (addressList.length() != 0) {
                    result.put(ServerProperties.PROP_STORAGE_NODES, addressList.toString());
                }
            } finally {
                db.closeResultSet(resultSet);
                db.closeStatement(statement);
            }
        } catch (SQLException e) {
            LOG.error("Failed to fetch storage cluster settings.", e);
            throw e;
        } finally {
            if (db != null) {
                db.closeConnection(connection);
            }
        }

        return result;
    }

    public static void persistStorageClusterSettingsIfNecessary(HashMap<String, String> serverProperties,
        String password) throws Exception {
        DatabaseType db = null;
        Connection connection = null;
        PreparedStatement updateClusterSetting = null;

        try {
            String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            String userName = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
            connection = getDatabaseConnection(dbUrl, userName, password);
            db = DatabaseTypeFactory.getDatabaseType(connection);

            if (!(db instanceof PostgresqlDatabaseType || db instanceof OracleDatabaseType)) {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + db);
            }

            connection = getDatabaseConnection(dbUrl, userName, password);
            connection.setAutoCommit(false);

            updateClusterSetting = connection.prepareStatement("" //
                + "UPDATE rhq_system_config " //
                + "   SET property_value = ?, default_property_value = ? " //
                + " WHERE property_key = ? " //
                + "   AND ( property_value IS NULL OR property_value = '' OR property_value = 'UNSET' ) ");

            updateClusterSetting.setString(1, serverProperties.get(ServerProperties.PROP_STORAGE_USERNAME));
            updateClusterSetting.setString(2, serverProperties.get(ServerProperties.PROP_STORAGE_USERNAME));
            updateClusterSetting.setString(3, "STORAGE_USERNAME");
            updateClusterSetting.executeUpdate();

            updateClusterSetting.setString(1, serverProperties.get(ServerProperties.PROP_STORAGE_PASSWORD));
            updateClusterSetting.setString(2, serverProperties.get(ServerProperties.PROP_STORAGE_PASSWORD));
            updateClusterSetting.setString(3, "STORAGE_PASSWORD");
            updateClusterSetting.executeUpdate();

            updateClusterSetting.setString(1, serverProperties.get(ServerProperties.PROP_STORAGE_CQL_PORT));
            updateClusterSetting.setString(2, serverProperties.get(ServerProperties.PROP_STORAGE_CQL_PORT));
            updateClusterSetting.setString(3, "STORAGE_CQL_PORT");
            updateClusterSetting.executeUpdate();

            updateClusterSetting.setString(1, serverProperties.get(ServerProperties.PROP_STORAGE_GOSSIP_PORT));
            updateClusterSetting.setString(2, serverProperties.get(ServerProperties.PROP_STORAGE_GOSSIP_PORT));
            updateClusterSetting.setString(3, "STORAGE_GOSSIP_PORT");
            updateClusterSetting.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOG.error("Failed to initialize storage cluster settings. Transaction will be rolled back.", e);
            connection.rollback();
            throw e;
        } finally {
            if (db != null) {
                db.closeStatement(updateClusterSetting);
                db.closeConnection(connection);
            }
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
                    stm = conn
                        .prepareStatement("INSERT INTO rhq_server " //
                            + " ( id, name, address, port, secure_port, ctime, mtime, operation_mode, compute_power, version ) " //
                            + "VALUES ( ?, ?, ?, ?, ?, ?, ?, 'INSTALLED', 1, ? )");
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
                stm.setString(i++, version);
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
     * <p>Note that if the {@link #isDatabaseSchemaExist schema already exists}, it will be purged of all
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
    }

    /**
     * This will update an existing database schema so it can be upgraded to the latest schema version.
     *
     * <p>Note that if the {@link #isDatabaseSchemaExist schema does not already exist}, errors will
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
     * Ensures our web connectors are configured properly.
     *
     * @param mcc the AS client
     * @param configDirStr location of a configuration directory where the keystore is to be stored
     * @param serverProperties the full set of server properties
     * @throws Exception
     */
    public static void setupWebConnectors(ModelControllerClient mcc, String configDirStr,
        HashMap<String, String> serverProperties) throws Exception {

        // 6.3.0.Alpha1 (aka 7.4.0.Final-redhat-4) has a bug (BZ 1133061) that can be worked around by changing the connector protocol
        // This bug was fixed in 6.3.0.GA (aka 7.4.0.Final-redhat-19) so we don't need the workaround for anything after that.
        // AFAIK, it isn't bad if we install the workaround, but we want to keep the original config if we can help it so we try to
        // determine if we don't need it - if we don't, we won't install the workaround.
        String appServerVersion = getAppServerReleaseVersion(mcc);
        if (appServerVersion == null) {
            appServerVersion = "7.4.0.Final-redhat-4";
            LOG.warn("Will assume app server release version is [" + appServerVersion + "]");
        }
        boolean needProtocolWorkaround = false;
        if (appServerVersion.startsWith("7.4.0")) {
            if (appServerVersion.endsWith("redhat-4")) {
                needProtocolWorkaround = true;
            }
        }
        if (needProtocolWorkaround) {
            LOG.info("App server version is [" + appServerVersion
                + "] - will apply workaround to http and https connectors as per BZ 1133061");
        } else {
            LOG.debug("No BZ 1133061 workaround will be applied to http and https connectors since app server version is: "
                + appServerVersion);
        }

        // out of box, we always get a non-secure connector (called "http")...
        final String connectorName = "http";

        // ...but we want a secure SSL connector, too.
        // This is the name of the secure connector we want to create.
        final String sslConnectorName = "https";

        WebJBossASClient client = new WebJBossASClient(mcc);

        // because some of the connector attributes do not (yet) support expressions, let's remove any existing
        // connector we may have created before and create it again with our current attribute values.
        client.removeConnector(sslConnectorName);

        LOG.info("Creating https connector...");
        ConnectorConfiguration connector = buildSecureConnectorConfiguration(configDirStr, serverProperties);

        // verify that we have a truststore file - if user is relying on our self-signed certs, we'll have to create one for them
        String truststoreFileString = connector.getSslConfiguration().getCaCertificateFile();
        truststoreFileString = resolveExpression(mcc, truststoreFileString);
        if (truststoreFileString == null) {
            LOG.warn("Missing a valid truststore location - you must specify a valid truststore location!");
        } else {
            File truststoreFile = new File(truststoreFileString);
            if (!truststoreFile.exists()) {
                // user didn't provide a truststore file, copy the keystore and use it as the truststore; tell the user about this
                String keystoreFileString = connector.getSslConfiguration().getCertificateKeyFile();
                keystoreFileString = resolveExpression(mcc, keystoreFileString);
                File keystoreFile = new File(keystoreFileString);
                if (!keystoreFile.isFile()) {
                    LOG.warn("Missing both keystore [" + keystoreFile + "] and truststore [" + truststoreFile + "]");
                } else {
                    LOG.warn("Missing the truststore [" + truststoreFile + "] - will copy the keystore ["
                        + keystoreFile + "] and make the copy the truststore.");
                    try {
                        FileUtil.copyFile(keystoreFile, truststoreFile);
                    } catch (Exception e) {
                        LOG.error("Failed to copy keystore to make truststore - a truststore still does not exist", e);
                    }
                }
            }
        }

        if (needProtocolWorkaround) {
            connector.setProtocol("org.apache.coyote.http11.Http11Protocol");
        }

        client.addConnector("https", connector);
        LOG.info("https connector created.");

        if (client.isConnector(connectorName)) {
            client.changeConnector(connectorName, "max-connections",
                buildExpression("rhq.server.startup.web.max-connections", serverProperties, true));
            client.changeConnector(connectorName, "redirect-port",
                buildExpression("rhq.server.socket.binding.port.https", serverProperties, true));

            if (needProtocolWorkaround) {
                client.changeConnector(connectorName, "protocol", "org.apache.coyote.http11.Http11Protocol");
            }
        } else {
            LOG.warn("There doesn't appear to be a http connector configured already - this is strange.");
        }
    }

    private static String resolveExpression(ModelControllerClient mcc, String expression) {
        if (expression == null) {
            return null;
        }

        CoreJBossASClient client = new CoreJBossASClient(mcc);
        String resolvedExpression;
        try {
            resolvedExpression = client.resolveExpression(expression);

            // https://issues.jboss.org/browse/WFLY-1177 - app server doesn't do recursive resolving, we have to do it here
            while (resolvedExpression != null && resolvedExpression.contains("${")
                && !resolvedExpression.equals(expression)) {
                expression = resolvedExpression;
                resolvedExpression = client.resolveExpression(expression);
            }
        } catch (Exception e) {
            LOG.warn("Cannot resolve expression [" + expression + "]; will use it as-is but errors may occur later.");
            resolvedExpression = expression;
        }
        return resolvedExpression;
    }

    private static ConnectorConfiguration buildSecureConnectorConfiguration(String configDirStr,
        HashMap<String, String> serverProperties) {

        SSLConfiguration ssl = new SSLConfiguration();

        // Because of https://issues.jboss.org/browse/WFLY-1177 we cannot build expressions for key/truststore files.
        // Otherwise, we end up with recursive expressions (${${x}:a}) which is what's broken. For now, just use ${x} which is allowed.

        // truststore
        ssl.setCaCertificateFile(buildExpression("rhq.server.tomcat.security.truststore.file", serverProperties, false));
        ssl.setCaCertificationPassword(buildExpression("rhq.server.tomcat.security.truststore.password",
            serverProperties, true, true, true));
        ssl.setTruststoreType(buildExpression("rhq.server.tomcat.security.truststore.type", serverProperties, true,
            true, false));

        // keystore
        ssl.setCertificateKeyFile(buildExpression("rhq.server.tomcat.security.keystore.file", serverProperties, false));
        ssl.setPassword(buildExpression("rhq.server.tomcat.security.keystore.password", serverProperties, true, true,
            true));
        ssl.setKeyAlias(buildExpression("rhq.server.tomcat.security.keystore.alias", serverProperties, true));
        ssl.setKeystoreType(buildExpression("rhq.server.tomcat.security.keystore.type", serverProperties, true));

        // SSL protocol config
        ssl.setProtocol(buildExpression("rhq.server.tomcat.security.secure-socket-protocol", serverProperties, true));
        ssl.setVerifyClient(buildExpression("rhq.server.tomcat.security.client-auth-mode", serverProperties, true));

        // note: there doesn't appear to be a way for AS7 to support algorithm, like SunX509 or IbmX509
        // so I think it just uses the JVM's default. This means "rhq.server.tomcat.security.algorithm" is unused

        ConnectorConfiguration connector = new ConnectorConfiguration();
        connector.setMaxConnections(buildExpression("rhq.server.startup.web.max-connections", serverProperties, true));
        connector.setScheme("https");
        connector.setSocketBinding("https");
        connector.setSslConfiguration(ssl);
        return connector;
    }

    //    /**
    //     * For a property whose value might be a file, return that file's absolute path. If the property
    //     * has a value whose pathname is already absolute, return it. If the property has a value whose path
    //     * is relative, it is considered relative to defaultRootDir and its absolute path based on that root dir
    //     * is returned.
    //     *
    //     * @param propertyName the property whose value in properties is considered a pathname (which may
    //     *                     relative or it may be absolute).
    //     * @param properties where to find the named property
    //     * @param defaultRootDir if the property value is a relative file path, this is what it is relative to
    //     * @return the absolute path of the file
    //     */
    //    private static String getAbsoluteFileLocation(String propertyName, HashMap<String, String> properties,
    //        String defaultRootDir) {
    //
    //        if (properties == null || !properties.containsKey(propertyName)) {
    //            return null;
    //        }
    //
    //        String propertyValue = properties.get(propertyName);
    //        File path = new File(propertyValue);
    //        if (path.isAbsolute()) {
    //            return path.getAbsolutePath();
    //        } else {
    //            return new File(defaultRootDir, propertyValue).getAbsolutePath();
    //        }
    //    }

    private static String buildExpression(String propName, HashMap<String, String> defaultProperties,
        boolean supportsExpression) {
        return buildExpression(propName, defaultProperties, supportsExpression, false, false);
    }

    /**
     * You would think this would be simple - just set a value to ${propName:defaultVal} but some
     * JBossAS attributes don't support expressions when you think they could or should. In this
     * case, we'll pass in supportsExpression=false until we support AS versions that support
     * expressions (at which time we'll change to code to pass in true for those attributes).
     * If supportsExpression is true, the returned string will be ${propName} if defaultProperties is
     * null or doesn't have that property defined; ${propName:defaultVal} if the default property
     * is found.
     * If supportsExpression is false, this will take the actual property value for
     * propName found in the given default properties and return that string.
     * If there is no sysprop of that name, this method returns an empty string.
     *
     * @param propName
     * @param defaultProperties
     * @param supportsExpression
     * @return the attribute expression value (or real value if supportsExpression is false).
     */
    private static String buildExpression(String propName, HashMap<String, String> defaultProperties,
        boolean supportsExpression, boolean vault, boolean restricted) {

        if (supportsExpression) {
            String expressionFormat;

            if (!vault) {
                if ((defaultProperties != null) && (defaultProperties.containsKey(propName))) {
                    expressionFormat = "${%s:%s}";
                } else {
                    expressionFormat = "${%s}";
                }
            } else {
                String attributeType = "restricted";
                if (!restricted) {
                    attributeType = "open";
                }
                if ((defaultProperties != null) && (defaultProperties.containsKey(propName))) {
                    expressionFormat = "${VAULT::" + attributeType + "::%s::%s}";
                } else {
                    expressionFormat = "${VAULT::" + attributeType + "::%s:: }";
                }
            }

            if ((defaultProperties != null) && (defaultProperties.containsKey(propName))) {
                String value = defaultProperties.get(propName);

                if (RestrictedFormat.isRestrictedFormat(value)) {
                    value = RestrictedFormat.retrieveValue(value);
                }

                return String.format(expressionFormat, propName, value);
            } else {
                return String.format(expressionFormat, propName);
            }
        } else {
            if ((defaultProperties != null) && (defaultProperties.containsKey(propName))) {
                return defaultProperties.get(propName);
            } else {
                LOG.warn("There is no known value for property [" + propName + "]");
                return "";
            }
        }
    }

    /**
     * Creates a keystore whose cert has a CN of this server's public endpoint address.
     *
     * @param serverDetails details of the server being installed - must not be null and endpoint must be included in it
     * @param configDirStr location of a configuration directory where the keystore is to be stored
     * @return where the keystore file should be created (if an error occurs, this file won't exist)
     */
    public static File createKeystore(ServerDetails serverDetails, String configDirStr) {
        File confDir = new File(configDirStr);
        File keystore = new File(confDir, "rhq.keystore");
        File keystoreBackup = new File(confDir, "rhq.keystore.backup");

        // if there is one out-of-box, we want to remove it and create one with our proper CN
        if (keystore.exists()) {
            keystoreBackup.delete();
            if (!keystore.renameTo(keystoreBackup)) {
                LOG.warn("Cannot backup existing keystore - cannot generate a new cert with a proper domain name. ["
                    + keystore + "] will be the keystore used by this server");
                return keystore;
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

        return keystore;
    }

    /**
     * Create an rhqadmin management user so when discovered, the AS7 plugin can use it to connect
     * to the RHQ Server.  The password is set in rhq-server.properties.  Because the plugin can't guess
     * the password, if not set to the default then the AS7 plugin will fail to connect, and the
     * RHQ Server resource connection properties will need to be updated after discovery and import.
     *
     * @param password the management password
     * @param serverDetails details of the server being installed
     * @param configDirStr location of a configuration directory where the mgmt-users.properties file lives
     */
    public static void createDefaultManagementUser(String password, ServerDetails serverDetails, String configDirStr) {
        File confDir = new File(configDirStr);
        File mgmtUsers = new File(confDir, "mgmt-users.properties");

        if (ServerInstallUtil.isEmpty(password)) {
            LOG.warn("Could not create default management user in file: [" + mgmtUsers + "] : invalid password ["
                + password + "].");
            return;
        }

        // Add the default admin user, or if for some reason this file does not exist, just log the issue
        if (mgmtUsers.exists()) {
            try {
                PropertiesFileUpdate mgmtUsersPropFile = new PropertiesFileUpdate(mgmtUsers.getAbsolutePath());
                Properties existingUsers = mgmtUsersPropFile.loadExistingProperties();
                if (existingUsers.containsKey(RHQ_MGMT_USER)) {
                    LOG.info("There is already a mgmt user named [" + RHQ_MGMT_USER + "], will not create another");
                    return;
                }
            } catch (Exception e) {
                LOG.warn("Cannot determine if mgmt user exists in [" + mgmtUsers + "]; will try to create it anyway", e);
            }

            FileOutputStream fos = null;

            try {
                String encodedPassword = new UsernamePasswordHashUtil().generateHashedHexURP(RHQ_MGMT_USER,
                    "ManagementRealm", password.toCharArray());

                fos = new FileOutputStream(mgmtUsers, true);
                fos.write(("\n" + RHQ_MGMT_USER + "=" + encodedPassword + "\n").getBytes());

            } catch (Exception e) {
                LOG.warn("Could not create default management user in file: [" + mgmtUsers + "] : ", e);

            } finally {
                StreamUtil.safeClose(fos);
            }
        } else {
            LOG.warn("Could not create default management user. Could not find file: [" + mgmtUsers + "]");
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
            try {
                client.setStandardSocketBindingPortExpression(binding.name, binding.sysprop, newPort);
            } catch (Exception e) {
                // If the binding is required, we re-throw a possible exception. Otherwise just log
                if (binding.required) {
                    throw e;
                } else {
                    LOG.info(String.format("Setting socket binding port for [%s] resulted in [%s] - this is harmless ",
                        binding.name, e.getMessage())); // TODO log at debug level only?
                }
            }

            // if we need to switch the binding's interface, do it now
            if (binding.interfaceName != null) {
                LOG.info(String.format("Setting socket binding [%s] to use interface [%s]", binding.name,
                    binding.interfaceName));
                try {
                    client.setStandardSocketBindingInterface(binding.name, binding.interfaceName);
                } catch (Exception e) {
                    // If the binding is required, we re-throw a possible exception. Otherwise just log
                    if (binding.required) {
                        throw e;
                    } else {
                        LOG.info(String.format(
                            "Setting socket binding interface for [%s] resulted in [%s] - this is harmless ",
                            binding.name, e.getMessage())); // TODO log at debug level only?
                    }
                }
            }
        }
    }

    /**
     * In case some things should only be installed if the underlying app server is a specific version,
     * call this to get the release-version of the app server. Release versions are something like:
     * 7.4.0.Final-redhat-4 (which corresponds to a EAP product-version of "6.3.0.Alpha1")
     * 7.4.0.Final-redhat-19 (which corresponds to a EAP product-version of "6.3.0.GA")
     *
     * @return release version or null if not known/can't be determined
     */
    private static String getAppServerReleaseVersion(ModelControllerClient mcc) {
        try {
            return new CoreJBossASClient(mcc).getAppServerVersion();
        } catch (Exception e) {
            LOG.warn("Cannot determine what the underlying app server release version is - installation may not work");
            return null;
        }
    }

    /**
     * Update server version stamp with the install version.
     *
     * For two reasons we add the column here, as opposed to db-upgrade.xml. First, a SN upgrade may
     * happen before a Server upgrade, so db-upgrade may not have yet run.  Second, we can limit the
     * setting of the version to the row in question, db-upgrade would not know which row to set.
     *

     * @param connectionUrl
     * @param username
     * @param password
     * @param serverName
     *
     * @throws Exception if failed to communicate with the database or could not stamp version
     */
    public static void updateServerVersion(String connectionUrl, String username, String password, String serverName)
        throws Exception {

        Connection connection = null;
        try {
            connection = getDatabaseConnection(connectionUrl, username, password);
            ServerVersionColumnUpgrader versionColumnUpgrader = new ServerVersionColumnUpgrader();
            versionColumnUpgrader.upgrade(connection, version);
            int rowsUpdated = versionColumnUpgrader.setVersionForServerWithName(connection, version, serverName);
            if (1 != rowsUpdated) {
                throw new IllegalStateException("Expected [1] Server update but updated [" + rowsUpdated + "].");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to update Server [" + serverName + "] to version [" + version + "]", e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

    /**
     * Get the list of existing servers from an existing schema.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return LinkedHashMap<serverName, version>. Null if version field does not exist, empty if no
     *         servers exist.  Ordered by server name asc.
     *
     * @throws Exception if failed to communicate with the database
     */
    public static LinkedHashMap<String, String> getServerVersions(String connectionUrl, String username, String password)
        throws Exception {
        DatabaseType db = null;
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        LinkedHashMap<String, String> result = null;

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            if (db.checkColumnExists(conn, "rhq_server", "version")) {

                stm = conn.createStatement();
                rs = stm.executeQuery("SELECT name, version FROM rhq_server ORDER BY name asc");

                result = new LinkedHashMap<String, String>();

                while (rs.next()) {
                    result.put(rs.getString(1), rs.getString(2));
                }
            }
        } catch (IllegalStateException e) {
            // column does not exist
        } catch (SQLException e) {
            LOG.info("Unable to fetch server version info: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeJDBCObjects(conn, stm, rs);
            }
        }

        return result;
    }

    /**
     * Get the list of existing servers from an existing schema.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return LinkedHashMap<storageNodeAddress, version>. Null if version field does not exist, empty if no
     *         storage nodes exist.  Ordered by storage node address asc.
     *
     * @throws Exception if failed to communicate with the database
     */
    public static LinkedHashMap<String, String> getStorageNodeVersions(String connectionUrl, String username,
        String password) throws Exception {
        DatabaseType db = null;
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        LinkedHashMap<String, String> result = null;

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            if (db.checkColumnExists(conn, "rhq_storage_node", "version")) {

                stm = conn.createStatement();
                rs = stm.executeQuery("SELECT address, version FROM rhq_storage_node ORDER BY address asc");

                result = new LinkedHashMap<String, String>();

                while (rs.next()) {
                    result.put(rs.getString(1), rs.getString(2));
                }
            }
        } catch (IllegalStateException e) {
            // column does not exist
        } catch (SQLException e) {
            LOG.info("Unable to fetch storage node version info: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeJDBCObjects(conn, stm, rs);
            }
        }

        return result;
    }

}
