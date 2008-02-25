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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginPermissionException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.AggregateProcessInfo;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.postgres.util.PostgresqlConfFile;

/**
 * Management for a PostgreSQL server
 *
 * @author Greg Hinkle
 */
public class PostgresServerComponent implements DatabaseComponent, ConfigurationFacet, MeasurementFacet,
    OperationFacet, CreateChildResourceFacet {
    private static Log log = LogFactory.getLog(PostgresServerComponent.class);

    private Connection connection;

    private AggregateProcessInfo aggregateProcessInfo;

    private ResourceContext resourceContext;

    static final String DEFAULT_CONFIG_FILE_NAME = "postgresql.conf";

    /*
     * TODO: Other things to support active sessions: select * from pg_stat_activity
     */

    public void start(ResourceContext context) throws SQLException {
        this.resourceContext = context;
        Configuration config = context.getPluginConfiguration();

        JDBCUtil.safeClose(this.connection); // just to be sure we don't leak a connection
        this.connection = PostgresDiscoveryComponent.buildConnection(config);

        ProcessInfo processInfo = resourceContext.getNativeProcess();
        if (processInfo != null) {
            aggregateProcessInfo = processInfo.getAggregateProcessTree();
        } else {
            log.debug("Unable to locate native process information. Process level statistics will be unavailable.");
        }
    }

    public void stop() {
        this.resourceContext = null;
        JDBCUtil.safeClose(this.connection);
        this.connection = null;
    }

    protected String getJDBCUrl() {
        return PostgresDiscoveryComponent.buildUrl(resourceContext.getPluginConfiguration());
    }

    public AvailabilityType getAvailability() {
        AvailabilityType type;
        getConnection(); // This retries the connection if its null
        if (connection == null) {
            type = AvailabilityType.DOWN;
        } else {
            type = AvailabilityType.UP;
        }

        return type;
    }

    ResourceContext getResourceContext() {
        return resourceContext;
    }

    public Connection getConnection() {
        // TODO: This method should probably be synchronized to prevent connection leaks. (ips, 10/4/07)
        try {
            if ((connection == null) || connection.isClosed()) {
                connection = PostgresDiscoveryComponent.buildConnection(this.resourceContext.getPluginConfiguration());
            }
        } catch (SQLException e) {
            // TODO Should we throw this?
        }

        return connection;
    }

    public void removeConnection() {
        JDBCUtil.safeClose(this.connection);
        this.connection = null;
    }

    // TODO: Why are we only supporting this small subset of config file params? (ips, 10/4/07)
    private static final String[] CONFIG_FILE_PROPERTIES = { "port", "max_connections", "shared_buffers",
        "max_fsm_pages", "log_destination", "redirect_stderr", "stats_start_collector", "stats_block_level",
        "stats_row_level", "autovacuum" };

    protected PostgresqlConfFile getConfigurationFile() throws IOException {
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        String dataDirPath = pluginConfig.getSimpleValue(PostgresDiscoveryComponent.PGDATA_DIR_CONFIGURATION_PROPERTY,
            null);
        String configFilePath = pluginConfig.getSimpleValue(
            PostgresDiscoveryComponent.CONFIG_FILE_CONFIGURATION_PROPERTY, null);
        File configFile = (configFilePath != null) ? new File(configFilePath) : new File(dataDirPath,
            DEFAULT_CONFIG_FILE_NAME);
        return new PostgresqlConfFile(configFile);
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = new Configuration();
        PostgresqlConfFile confFile;
        ConfigurationDefinition def = resourceContext.getResourceType().getResourceConfigurationDefinition();
        try {
            confFile = getConfigurationFile();
            for (String propName : CONFIG_FILE_PROPERTIES) {
                String value = confFile.getProperty(propName);
                PropertyDefinitionSimple propDef = def.getPropertyDefinitionSimple(propName);
                PropertySimple prop = createProperty(value, propDef);
                config.put(prop);
            }
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                String msg = "Can not read the configuration files: " + e.getMessage();
                log.debug(msg);
                throw new PluginPermissionException(msg);
            }

            //log.info("Couldn't load postgres configuration file", e);
            e.printStackTrace();
        }

        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("show all");

            PropertyMap runtimeSettings = new PropertyMap("runtimeSettings");
            PropertyDefinitionMap mapDef = def.getPropertyDefinitionMap("runtimeSettings");
            config.put(runtimeSettings);
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String setting = resultSet.getString("setting");

                PropertyDefinitionSimple pd = mapDef.getPropertyDefinitionSimple(name);

                if ((pd != null) && (pd.getType() == PropertySimpleType.BOOLEAN)) {
                    runtimeSettings.put(new PropertySimple(name, "on".equalsIgnoreCase(setting)));
                } else if (setting != null) {
                    runtimeSettings.put(new PropertySimple(name, setting));
                }
            }
        } finally {
            JDBCUtil.safeClose(statement, resultSet);
        }

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            ConfigurationDefinition def = resourceContext.getResourceType().getResourceConfigurationDefinition();
            Map<String, String> parameters = new HashMap<String, String>();

            for (PropertySimple prop : report.getConfiguration().getSimpleProperties().values()) {
                PropertyDefinitionSimple pd = def.getPropertyDefinitionSimple(prop.getName());
                if ("configFile".equals(pd.getPropertyGroupDefinition().getName())) {
                    // configuration file
                    String value = getPostgresParameterValue(prop, pd);
                    parameters.put(prop.getName(), value);
                } else {
                    // session param
                    if (!pd.isReadOnly()) {
                        // TODO: Update param using SQL SET command. Probably should do a SHOW ALL at the top of this method,
                        //       and then only call SET on params that have changed. (ips, 10/4/07)
                    }
                }
            }

            PostgresqlConfFile confFile = getConfigurationFile();
            confFile.setProperties(parameters);
        } catch (IOException e) {
            log.error("Unable to update postgres configuration file", e);
        }

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        if (aggregateProcessInfo != null) {
            aggregateProcessInfo.refresh();
            for (MeasurementScheduleRequest request : metrics) {
                if (request.getName().startsWith("Process.")) {
                    report.addData(new MeasurementDataNumeric(request, getProcessProperty(request.getName())));
                } else if (request.getName().equals("startTime")) {
                    /* db start time
                     * try { ResultSet rs = getConnection().createStatement().executeQuery("SELECT
                     * pg_postmaster_start_time()"); report.addData(new MeasurementDataTrait(request,
                     * rs.getTimestamp(1).getTime())); return } catch (SQLException e) {
                     *
                     *}*/
                }
            }
        }
    }

    private Double getProcessProperty(String property) {
        property = property.substring("Process.".length());

        if (property.startsWith("Memory.")) {
            property = property.substring("Memory.".length());
            return getObjectProperty(aggregateProcessInfo.getMemory(), property);
        } else {
            return getObjectProperty(aggregateProcessInfo, property);
        }
    }

    public double getObjectProperty(Object object, String name) {
        try {
            BeanInfo info = Introspector.getBeanInfo(object.getClass());
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                if (pd.getName().equals(name)) {
                    return ((Number) pd.getReadMethod().invoke(object)).doubleValue();
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while retrieving property '" + name + "' from object [" + object + "]", e);
        }

        return Double.NaN;
    }

    /*private ProcessInfo getProcess(String pgdata)
     * { List<ProcessScanResult> matches = this.resourceContext.getNativeProcessesForType(); for (ProcessScanResult
     * process : matches) {   if (pgdata.equals(process.getProcessInfo().getEnvironmentProperty("PGDATA")))      return
     * process.getProcessInfo(); } return null;}*/

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        if (name.equals("listProcessStatistics")) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = getConnection().createStatement();
                rs = stmt.executeQuery("SELECT * FROM pg_stat_activity ORDER BY current_query desc");

                PropertyList procList = new PropertyList("processList");
                while (rs.next()) {
                    PropertyMap pm = new PropertyMap("process");
                    pm.put(new PropertySimple("pid", rs.getInt("procpid")));
                    pm.put(new PropertySimple("userName", rs.getString("usename")));
                    pm.put(new PropertySimple("query", rs.getString("current_query")));
                    pm.put(new PropertySimple("address", rs.getString("client_addr")));
                    pm.put(new PropertySimple("port", rs.getInt("client_port")));

                    procList.add(pm);
                }

                OperationResult result = new OperationResult();
                result.getComplexResults().put(procList);
                return result;
            } finally {
                if (rs != null) {
                    rs.close();
                }

                if (stmt != null) {
                    stmt.close();
                }
            }
        }

        return null;
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        Configuration userConfig = report.getResourceConfiguration();
        String user = userConfig.getSimpleValue("user", null);

        Statement statement = null;
        String sql = PostgresUserComponent.getUserSQL(userConfig, PostgresUserComponent.UpdateType.CREATE);
        try {
            statement = getConnection().createStatement();

            // NOTE: Postgres doesn't seem to indicate the expect count of 1 row updated but this work
            // Postgres returns 0 for DDL that does not return rows
            statement.executeUpdate(sql);
            report.setResourceKey(user);
            report.setStatus(CreateResourceStatus.SUCCESS);
        } catch (SQLException e) {
            report.setException(e);
        } finally {
            JDBCUtil.safeClose(statement);
        }

        return report;
    }

    private String getPostgresParameterValue(PropertySimple prop, PropertyDefinitionSimple propDef) {
        String value;
        if ((propDef.getType() == PropertySimpleType.BOOLEAN) && (prop.getBooleanValue() != null)) {
            //noinspection ConstantConditions
            value = (prop.getBooleanValue()) ? "on" : "off";
        } else {
            value = prop.getStringValue();
        }

        return value;
    }

    private PropertySimple createProperty(String value, PropertyDefinitionSimple propDef) {
        String jonValue;
        if ((propDef.getType() == PropertySimpleType.BOOLEAN) && (value != null)) {
            String lowerCaseValue = value.toLowerCase();
            if ("on".equals(lowerCaseValue) || "true".startsWith(lowerCaseValue) || "yes".startsWith(lowerCaseValue)
                || "1".equals(lowerCaseValue)) {
                jonValue = Boolean.TRUE.toString();
            } else if (("off".startsWith(lowerCaseValue) && (lowerCaseValue.length() != 1))
                || "false".startsWith(lowerCaseValue) || "no".startsWith(lowerCaseValue) || "0".equals(lowerCaseValue)) {
                jonValue = Boolean.FALSE.toString();
            } else {
                jonValue = (propDef.isRequired()) ? Boolean.FALSE.toString() : null;
                log.warn("Boolean PostgreSQL configuration parameter '" + propDef.getName()
                    + "' has an invalid value: '" + value + "' - defaulting value to '" + jonValue + "'");
            }
        } else {
            jonValue = value;
        }

        return new PropertySimple(propDef.getName(), jonValue);
    }
}