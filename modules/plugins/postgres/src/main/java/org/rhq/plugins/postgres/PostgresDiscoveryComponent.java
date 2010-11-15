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
package org.rhq.plugins.postgres;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.postgres.util.PostgresqlConfFile;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class PostgresDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {
    private static final Log log = LogFactory.getLog(PostgresDiscoveryComponent.class);

    public static final String PGDATA_DIR_CONFIGURATION_PROPERTY = "pgdataDir";
    public static final String CONFIG_FILE_CONFIGURATION_PROPERTY = "configFile";
    public static final String DRIVER_CONFIGURATION_PROPERTY = "driverClass";
    public static final String HOST_CONFIGURATION_PROPERTY = "host";
    public static final String PORT_CONFIGURATION_PROPERTY = "port";
    public static final String DB_CONFIGURATION_PROPERTY = "db";
    public static final String PRINCIPAL_CONFIGURATION_PROPERTY = "principal";
    public static final String CREDENTIALS_CONFIGURATION_PROPERTY = "credentials";

    private static final String PGDATA_ENV_VAR = "PGDATA";
    private static final String DEFAULT_RESOURCE_DESCRIPTION = "Postgres relational database server";
    private static final String POSTGRES_DEFAULT_DATABASE_NAME = "postgres";
    private static final Pattern VERSION_FROM_COMMANDLINE = Pattern.compile("\\d+(?:\\.\\d+)*");

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> servers = new LinkedHashSet<DiscoveredResourceDetails>();

        // Process any auto-discovered resources.
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult result : autoDiscoveryResults) {
            log.info("Discovered a postgres process: " + result);

            ProcessInfo procInfo = result.getProcessInfo();

            String pgDataPath = getDataDirPath(procInfo);
            if (pgDataPath == null) {
                log.error("Unable to obtain data directory for postgres process with pid " + procInfo.getPid()
                    + " (tried checking both -D command line argument, as well as " + PGDATA_ENV_VAR
                    + " environment variable).");
                continue;
            }

            File pgData = new File(pgDataPath);
            String configFilePath = getConfigFilePath(procInfo);
            PostgresqlConfFile confFile = null;

            if (!pgData.exists()) {
                log.warn("PostgreSQL data directory (" + pgData + ") does not exist or cannot be read.");
            } else {

                log.debug("PostgreSQL data directory: " + pgData);

                File postgresConfFile = (configFilePath != null) ? new File(configFilePath) : new File(pgData,
                    PostgresServerComponent.DEFAULT_CONFIG_FILE_NAME);
                if (!postgresConfFile.exists()) {
                    log.warn("PostgreSQL configuration file (" + postgresConfFile + ") does not exist.");
                }

                log.debug("PostgreSQL configuration file: " + postgresConfFile);

                try {
                    confFile = new PostgresqlConfFile(postgresConfFile);
                } catch (IOException e) {
                    log.warn("Could not load PostgreSQL configuration file.", e);
                }
            }

            Configuration pluginConfig = context.getDefaultPluginConfiguration();

            pluginConfig.put(new PropertySimple(PGDATA_DIR_CONFIGURATION_PROPERTY, pgData));
            pluginConfig.put(new PropertySimple(CONFIG_FILE_CONFIGURATION_PROPERTY, configFilePath));

            if (confFile != null) {
                String port = confFile.getPort();
                if (port != null) {
                    // Override the default (5432) from the descriptor.
                    pluginConfig.put(new PropertySimple(PORT_CONFIGURATION_PROPERTY, port));
                }
                List<String> listenAddresses = confFile.getPropertyList("listen_addresses");
                if (listenAddresses.size() > 0) {
                    String listenAddress = listenAddresses.get(0).trim();
                    if ("*".equals(listenAddress)) {
                        listenAddress = "127.0.0.1";
                    }
                    pluginConfig.put(new PropertySimple(HOST_CONFIGURATION_PROPERTY, listenAddress));
                }
            }

            DiscoveredResourceDetails resourceDetails = createResourceDetails(context, pluginConfig, procInfo, false);
            servers.add(resourceDetails);
        }

        return servers;

        /* TODO GH: Deal with the different error types and inventory except in case of connection refused
         * Bad password org.postgresql.util.PSQLException: FATAL: password authentication failed for user "jon" Wrong
         * port org.postgresql.util.PSQLException: Connection refused. Check that the hostname and port are correct and
         * that the postmaster is accepting TCP/IP connections. Wrong db org.postgresql.util.PSQLException: FATAL:
         * database "jon2" does not exist
         */
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException {
        ProcessInfo processInfo = null;
        DiscoveredResourceDetails resourceDetails = createResourceDetails(discoveryContext, pluginConfig, processInfo,
            true);
        return resourceDetails;
    }

    protected static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfiguration, @Nullable ProcessInfo processInfo, boolean logConnectionFailure) {
        String key = buildUrl(pluginConfiguration);
        Connection conn = getConnection(pluginConfiguration, logConnectionFailure);
        String name = getServerResourceName(pluginConfiguration, conn);
        String version = getVersion(pluginConfiguration, processInfo, discoveryContext.getSystemInformation(), conn);
        JDBCUtil.safeClose(conn);
        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version,
            DEFAULT_RESOURCE_DESCRIPTION, pluginConfiguration, processInfo);
    }

    protected static String buildUrl(Configuration config) {
        String host = config.getSimple(HOST_CONFIGURATION_PROPERTY).getStringValue();
        String port = config.getSimple(PORT_CONFIGURATION_PROPERTY).getStringValue();
        String db = config.getSimple(DB_CONFIGURATION_PROPERTY).getStringValue();
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        return url;
    }

    protected static String getVersion(Configuration config, ProcessInfo processInfo, SystemInfo systemInfo,
        Connection conn) {
        String version = null;
        try {
            if (conn != null) {
                version = conn.getMetaData().getDatabaseProductVersion();
            }
        } catch (SQLException e) {
            // TODO GH: How to put this back to the server while inventorying this resource in an unconfigured state
            log.info("Exception detecting postgres instance version.", e);
        }

        //now try to extract the version information by asking the server executable itself
        if (version == null && processInfo != null) {
            try {
                String postgresExe = processInfo.getExecutable().getName();
                ProcessExecution execution = new ProcessExecution(postgresExe);
                execution.setArguments(new String[] { "--version" });
                execution.setCaptureOutput(true);
                ProcessExecutionResults results = systemInfo.executeProcess(execution);
                String versionInfo = results.getCapturedOutput();
                Matcher m = VERSION_FROM_COMMANDLINE.matcher(versionInfo);
                if (m.find()) {
                    version = versionInfo.substring(m.start(), m.end());
                }
            } catch (Exception e) {
                log.info("Failed to obtain Postgres version information from the executable file.", e);
            }
        }

        return version;
    }

    public static Connection buildConnection(Configuration configuration, boolean logFailure) throws SQLException {
        String driverClass = configuration.getSimple(DRIVER_CONFIGURATION_PROPERTY).getStringValue();
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driverClass
                + ") not found.");
        }

        String url = buildUrl(configuration);

        String principal = configuration.getSimple(PRINCIPAL_CONFIGURATION_PROPERTY).getStringValue();
        String credentials = configuration.getSimple(CREDENTIALS_CONFIGURATION_PROPERTY).getStringValue();

        try {
            return DriverManager.getConnection(url, principal, credentials);
        } catch (SQLException e) {
            if (logFailure) {
                log.info("Failed to connect to the database", e);
            } else {
                log.debug("Failed to connect to the database", e);
            }
            return null;
        }
    }

    @Nullable
    protected static String getDataDirPath(@NotNull ProcessInfo procInfo) {
        String dataDirPath = null;
        String[] cmdLine = procInfo.getCommandLine();
        for (int i = 0; i < cmdLine.length; i++) {
            if (cmdLine[i].equals("-D")) {
                if (i != (cmdLine.length - 1)) {
                    dataDirPath = cmdLine[i + 1];
                    break;
                } else {
                    log.error("-D option was last option on postgres command line: " + Arrays.asList(cmdLine));
                }
            }
        }

        if (dataDirPath == null) {
            dataDirPath = procInfo.getEnvironmentVariable(PGDATA_ENV_VAR);
        }

        if (dataDirPath == null) {
        }

        return dataDirPath;
    }

    @Nullable
    private static String getConfigFilePath(@NotNull ProcessInfo procInfo) {
        String configFilePath = null;
        String[] cmdLine = procInfo.getCommandLine();
        for (int i = 0; i < cmdLine.length; i++) {
            if (cmdLine[i].equals("-c")) {
                if (i != (cmdLine.length - 1)) {
                    String paramString = cmdLine[i + 1];
                    int equalsIndex = paramString.indexOf('=');
                    if (equalsIndex == -1) {
                        log.error("Invalid value '" + paramString + "' for -c option on postgres command line: "
                            + Arrays.asList(cmdLine));
                        continue;
                    }

                    String paramName = paramString.substring(0, equalsIndex);
                    if (paramName.equalsIgnoreCase("config_file")) {
                        configFilePath = paramString.substring(equalsIndex + 1);
                        break;
                    }
                } else {
                    log.error("-c option was last option on postgres command line: " + Arrays.asList(cmdLine));
                }
            }
        }

        return configFilePath;
    }

    private static Connection getConnection(Configuration config, boolean logFailure) {
        try {
            return buildConnection(config, logFailure);
        } catch (SQLException e) {
            log.info("Failed to connect to postgres database.", e);

            return null;
        }
    }

    private static List<String> getDatabaseNames(Configuration config, Connection conn) {
        Statement statement = null;
        ResultSet resultSet = null;

        if (conn == null) {
            return Collections.emptyList();
        }
        
        try {
            List<String> ret = new ArrayList<String>();

            statement = conn.createStatement();
            resultSet = statement
                .executeQuery("SELECT *, pg_database_size(datname) FROM pg_database where datistemplate = false");
            while (resultSet.next()) {
                String databaseName = resultSet.getString("datname");
                ret.add(databaseName);
            }

            return ret;
        } catch (SQLException e) {
            log.info("Failed to obtain the list of databases in a postgres instance", e);
            return Collections.emptyList();
        } finally {
            JDBCUtil.safeClose(statement, resultSet);
        }
    }

    private static String getServerResourceName(Configuration config, Connection conn) {
        List<String> schemas = getDatabaseNames(config, conn);
        if (schemas.size() > 0 && schemas.size() < 3) {
            String firstDatabase = schemas.get(0);
            String secondDatabase = schemas.get(1);
            return POSTGRES_DEFAULT_DATABASE_NAME.equals(firstDatabase) ? secondDatabase : firstDatabase;
        } else {
            return config.getSimpleValue(DB_CONFIGURATION_PROPERTY, POSTGRES_DEFAULT_DATABASE_NAME);
        }
    }
}
