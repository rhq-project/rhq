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

package org.rhq.plugins.postgres;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.system.AggregateProcessInfo;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.PooledConnectionProvider;
import org.rhq.plugins.postgres.util.PostgresqlConfFile;

/**
 * Management for a PostgreSQL server
 *
 * @author Greg Hinkle
 */
public class PostgresServerComponent<T extends ResourceComponent<?>> implements DatabaseComponent<T>,
    ConnectionPoolingSupport, ConfigurationFacet, MeasurementFacet, OperationFacet, CreateChildResourceFacet {

    private static final Log LOG = LogFactory.getLog(PostgresServerComponent.class);

    private static final String METRIC_RUNTIME_PREFIX = "Runtime.";

    static final String DEFAULT_CONFIG_FILE_NAME = "postgresql.conf";

    private AggregateProcessInfo aggregateProcessInfo;
    @Deprecated
    private Connection connection;
    private ResourceContext resourceContext;
    private PostgresPooledConnectionProvider pooledConnectionProvider;

    public void start(ResourceContext context) throws Exception {
        this.resourceContext = context;
        buildSharedConnectionIfNeeded();
        try {
            pooledConnectionProvider = new PostgresPooledConnectionProvider(resourceContext.getPluginConfiguration());
        } catch (SQLException e) {
            if (e.getCause() instanceof SQLException) {
                SQLException cause = (SQLException) e.getCause();
                if ("28P01".equals(cause.getSQLState())) {
                    throw new InvalidPluginConfigurationException("Invalid password");
                }
            }
            throw new InvalidPluginConfigurationException("Cannot open database connection", e);
        }
        ProcessInfo processInfo = resourceContext.getNativeProcess();
        if (processInfo != null) {
            aggregateProcessInfo = processInfo.getAggregateProcessTree();
        } else {
            findProcessInfo();
        }
    }

    public void stop() {
        resourceContext = null;
        DatabasePluginUtil.safeClose(connection);
        connection = null;
        pooledConnectionProvider.close();
        pooledConnectionProvider = null;
        aggregateProcessInfo = null;
    }

    @Override
    public boolean supportsConnectionPooling() {
        return true;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return pooledConnectionProvider;
    }

    protected String getJDBCUrl() {
        return PostgresDiscoveryComponent.buildUrl(resourceContext.getPluginConfiguration());
    }

    public AvailabilityType getAvailability() {
        Connection jdbcConnection = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            return jdbcConnection.isValid(1) ? UP : DOWN;
        } catch (SQLException e) {
            return DOWN;
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection);
        }
    }

    ResourceContext getResourceContext() {
        return resourceContext;
    }

    public Connection getConnection() {
        buildSharedConnectionIfNeeded();
        return connection;
    }

    private void buildSharedConnectionIfNeeded() {
        try {
            if ((connection == null) || connection.isClosed()) {
                connection = PostgresDiscoveryComponent.buildConnection(this.resourceContext.getPluginConfiguration(),
                    true);
            }
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not build shared connection", e);
            }
        }
    }

    public void removeConnection() {
        DatabasePluginUtil.safeClose(this.connection);
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
        ConfigurationDefinition configDef = resourceContext.getResourceType().getResourceConfigurationDefinition();

        // Persisted settings - obtained by reading postgresql.conf.
        PostgresqlConfFile confFile = getConfigurationFile();
        for (String propName : CONFIG_FILE_PROPERTIES) {
            String value = confFile.getProperty(propName);
            PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple(propName);
            PropertySimple prop = createProperty(value, propDef);
            config.put(prop);
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
                }
            }

            PostgresqlConfFile confFile = getConfigurationFile();
            confFile.setProperties(parameters);
        } catch (IOException e) {
            LOG.error("Unable to update postgres configuration file", e);
        }

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    /**
     * Get data about the database server. Currently we have two categories:
     * <ul>
     * <li>Database.* are metrics that are obtained from the database server itself</li>
     * <li>Process.* are metrics obtained from the native system.</li>
     * </ul>
     *
     * @param  report  the report where all collected measurement data will be added
     * @param  metrics the schedule of what needs to be collected when
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {

        Map<String, MeasurementScheduleRequest> runtimePropertiesRequests = new HashMap<String, MeasurementScheduleRequest>(
            metrics.size());

        for (MeasurementScheduleRequest request : metrics) {
            String metricName = request.getName();
            if (metricName.startsWith("Process.")) {
                if (aggregateProcessInfo != null) {
                    aggregateProcessInfo.refresh();

                    //report.addData(new MeasurementDataNumeric(request, getProcessProperty(request.getName())));

                    Object val = lookupAttributeProperty(aggregateProcessInfo,
                        metricName.substring("Process.".length()));
                    if (val != null && val instanceof Number) {
                        //                        aggregateProcessInfo.getAggregateMemory().Cpu().getTotal()
                        report.addData(new MeasurementDataNumeric(request, ((Number) val).doubleValue()));
                    }
                }
            } else if (metricName.startsWith("Database")) {
                if (metricName.endsWith("startTime")) {
                    // db start time
                    Connection jdbcConnection = null;
                    Statement statement = null;
                    ResultSet resultSet = null;
                    try {
                        jdbcConnection = getPooledConnectionProvider().getPooledConnection();
                        statement = jdbcConnection.createStatement();
                        resultSet = statement.executeQuery("SELECT pg_postmaster_start_time()");
                        if (resultSet.next()) {
                            report.addData(new MeasurementDataTrait(request, resultSet.getTimestamp(1).toString()));
                        }
                    } catch (SQLException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Can not collect metric: " + metricName + ": " + e.getLocalizedMessage());
                        }
                    } finally {
                        DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
                    }
                } else if (metricName.endsWith("backends")) {
                    // number of connected backends
                    Connection jdbcConnection = null;
                    Statement statement = null;
                    ResultSet resultSet = null;
                    try {
                        jdbcConnection = getPooledConnectionProvider().getPooledConnection();
                        statement = jdbcConnection.createStatement();
                        resultSet = statement.executeQuery("select count(*) from pg_stat_activity");
                        if (resultSet.next()) {
                            report.addData(new MeasurementDataNumeric(request, (double) resultSet.getLong(1)));
                        }
                    } catch (SQLException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Can not collect metricName: " + metricName + ": " + e.getLocalizedMessage());
                        }
                    } finally {
                        DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
                    }
                }
            } else if (metricName.startsWith(METRIC_RUNTIME_PREFIX)) {
                runtimePropertiesRequests.put(metricName.substring(METRIC_RUNTIME_PREFIX.length()), request);
            }
        }

        if (!runtimePropertiesRequests.isEmpty()) {
            Connection jdbcConnection = null;
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                jdbcConnection = getPooledConnectionProvider().getPooledConnection();
                statement = jdbcConnection.createStatement();
                resultSet = statement.executeQuery("show all");

                while (resultSet.next()) {
                    String runtimeProperty = resultSet.getString("name");
                    if (!runtimePropertiesRequests.containsKey(runtimeProperty)) {
                        continue;
                    }
                    String setting = resultSet.getString("setting");
                    MeasurementScheduleRequest request = runtimePropertiesRequests.get(runtimeProperty);
                    switch (request.getDataType()) {
                    case TRAIT:
                        report.addData(new MeasurementDataTrait(request, setting));
                        break;
                    default:
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Unsupported metric data type: " + request.getName() + ", "
                                + request.getDataType());
                        }
                    }
                }
            } catch (SQLException e) {
                LOG.debug("Can not collect metrics: " + runtimePropertiesRequests.keySet() + ": "
                    + e.getLocalizedMessage());
            } finally {
                DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
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

    protected Object lookupAttributeProperty(Object value, String property) {
        String[] ps = property.split("\\.", 2);

        String searchProperty = ps[0];

        // Try to use reflection
        try {
            PropertyDescriptor[] pds = Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                if (pd.getName().equals(searchProperty)) {
                    value = pd.getReadMethod().invoke(value);
                }
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to read property from measurement attribute [" + searchProperty + "] not found on ["
                    + this.resourceContext.getResourceKey() + "]");
            }
        }

        if (ps.length > 1) {
            value = lookupAttributeProperty(value, ps[1]);
        }

        return value;
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
            LOG.error("Error occurred while retrieving property '" + name + "' from object [" + object + "]", e);
        }

        return Double.NaN;
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        if (name.equals("listProcessStatistics")) {
            Connection jdbcConnection = null;
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                jdbcConnection = getPooledConnectionProvider().getPooledConnection();

                DatabaseMetaData metaData = jdbcConnection.getMetaData();
                int databaseMajorVersion = metaData.getDatabaseMajorVersion();
                int databaseMinorVersion = metaData.getDatabaseMinorVersion();
                StringBuilder sqlQuery = new StringBuilder("select ");
                // See http://wiki.postgresql.org/wiki/What%27s_new_in_PostgreSQL_9.2#pg_stat_activity_and_pg_stat_replication.27s_definitions_have_changed
                if (databaseMajorVersion >= 9 && databaseMinorVersion >= 2) {
                    sqlQuery.append("pid").append(",");
                    sqlQuery.append("usename").append(",");
                    sqlQuery.append("query").append(",");
                    sqlQuery.append("state").append(",");
                    sqlQuery.append("client_addr").append(",");
                    sqlQuery.append("client_port").append(" ");
                } else {
                    sqlQuery.append("procpid as pid").append(",");
                    sqlQuery.append("usename").append(",");
                    sqlQuery.append("current_query as query").append(",");
                    sqlQuery.append("'' as state").append(",");
                    sqlQuery.append("client_addr").append(",");
                    sqlQuery.append("client_port").append(" ");
                }
                sqlQuery.append("from pg_stat_activity order by pid asc");

                statement = jdbcConnection.createStatement();
                resultSet = statement.executeQuery(sqlQuery.toString());

                PropertyList procList = new PropertyList("processList");
                while (resultSet.next()) {
                    PropertyMap pm = new PropertyMap("process");
                    pm.put(new PropertySimple("pid", resultSet.getInt("pid")));
                    pm.put(new PropertySimple("userName", resultSet.getString("usename")));
                    pm.put(new PropertySimple("query", resultSet.getString("query")));
                    pm.put(new PropertySimple("state", resultSet.getString("state")));
                    pm.put(new PropertySimple("address", resultSet.getString("client_addr")));
                    pm.put(new PropertySimple("port", resultSet.getInt("client_port")));
                    procList.add(pm);
                }

                OperationResult result = new OperationResult();
                result.getComplexResults().put(procList);
                return result;
            } finally {
                DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
            }
        }

        return null;
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        Configuration userConfig = report.getResourceConfiguration();
        String user = userConfig.getSimpleValue("user", null);

        Connection jdbcConnection = null;
        Statement statement = null;
        String sql = PostgresUserComponent.getUserSQL(userConfig, PostgresUserComponent.UpdateType.CREATE);
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            // NOTE: Postgres doesn't seem to indicate the expect count of 1 row updated but this work
            // Postgres returns 0 for DDL that does not return rows
            statement.executeUpdate(sql);
            report.setResourceKey(user);
            report.setStatus(CreateResourceStatus.SUCCESS);
        } catch (SQLException e) {
            report.setException(e);
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement);
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
                LOG.warn("Boolean PostgreSQL configuration parameter '" + propDef.getName()
                    + "' has an invalid value: '" + value + "' - defaulting value to '" + jonValue + "'");
            }
        } else {
            jonValue = value;
        }

        return new PropertySimple(propDef.getName(), jonValue);
    }

    public void findProcessInfo() {

        List<ProcessInfo> processes = this.resourceContext
            .getSystemInformation()
            .getProcesses(
                "process|basename|match=^(?i)(postgres|postmaster)\\.exe$,process|basename|nomatch|parent=^(?i)(postgres|postmaster)\\.exe$");

        processes.addAll(this.resourceContext.getSystemInformation().getProcesses(
            "process|basename|match=^(postgres|postmaster)$,process|basename|nomatch|parent=^(postgres|postmaster)$"));

        for (ProcessInfo processInfo : processes) {
            String pgDataPath = PostgresDiscoveryComponent.getDataDirPath(processInfo);
            if (pgDataPath != null) {
                this.aggregateProcessInfo = processInfo.getAggregateProcessTree();
                break;
            }
        }
    }

}
