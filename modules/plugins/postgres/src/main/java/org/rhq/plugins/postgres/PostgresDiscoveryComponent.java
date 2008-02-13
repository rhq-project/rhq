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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.postgres.util.PostgresqlConfFile;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class PostgresDiscoveryComponent implements ResourceDiscoveryComponent {
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
            if (!pgData.exists()) {
                log.error("PostgreSQL data directory (" + pgData + ") does not exist.");
                continue;
            }

            log.debug("PostgreSQL data directory: " + pgData);

            String configFilePath = getConfigFilePath(procInfo);
            File postgresConfFile = (configFilePath != null) ? new File(configFilePath) : new File(pgData,
                PostgresServerComponent.DEFAULT_CONFIG_FILE_NAME);
            if (!postgresConfFile.exists()) {
                log.error("PostgreSQL configuration file (" + postgresConfFile + ") does not exist.");
                continue;
            }

            log.debug("PostgreSQL configuration file: " + postgresConfFile);

            PostgresqlConfFile confFile;
            try {
                confFile = new PostgresqlConfFile(postgresConfFile);
            } catch (IOException e) {
                log.error("Could not load PostgreSQL configuration file.", e);
                continue;
            }

            Configuration pluginConfig = context.getDefaultPluginConfiguration();

            pluginConfig.put(new PropertySimple(PGDATA_DIR_CONFIGURATION_PROPERTY, pgData));
            pluginConfig.put(new PropertySimple(CONFIG_FILE_CONFIGURATION_PROPERTY, postgresConfFile));

            String port = confFile.getPort();
            if (port != null) {
                // Override the default (5432) from the descriptor.
                pluginConfig.put(new PropertySimple(PORT_CONFIGURATION_PROPERTY, port));
            }

            DiscoveredResourceDetails resourceDetails = createResourceDetails(context, pluginConfig, procInfo);
            servers.add(resourceDetails);
        }

        // Process any manually-added resources.
        List<Configuration> contextPluginConfigurations = context.getPluginConfigurations();
        for (Configuration pluginConfiguration : contextPluginConfigurations) {
            ProcessInfo processInfo = null;
            DiscoveredResourceDetails resourceDetails = createResourceDetails(context, pluginConfiguration, processInfo);
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

    protected static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfiguration, @Nullable
        ProcessInfo processInfo) {
        String key = buildUrl(pluginConfiguration);
        String db = pluginConfiguration.getSimple(DB_CONFIGURATION_PROPERTY).getStringValue();
        String name = "Postgres [" + db + "]";
        String version = getVersion(pluginConfiguration);
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

    protected static String getVersion(Configuration config) {
        String version = null;
        try {
            Connection conn = buildConnection(config);
            version = conn.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            // TODO GH: How to put this back to the server while inventorying this resource in an unconfigured state
            log.info("Exception detecting postgres instance", e);
        }

        return version;
    }

    public static Connection buildConnection(Configuration configuration) throws SQLException {
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

        return DriverManager.getConnection(url, principal, credentials);
    }

    @Nullable
    private static String getDataDirPath(@NotNull
    ProcessInfo procInfo) {
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
    private static String getConfigFilePath(@NotNull
    ProcessInfo procInfo) {
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
}