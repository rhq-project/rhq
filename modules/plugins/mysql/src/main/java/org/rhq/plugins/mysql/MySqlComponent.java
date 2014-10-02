/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.plugins.mysql;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.system.AggregateProcessInfo;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.database.ConnectionPoolingSupport;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.PooledConnectionProvider;

/**
 * @author Greg Hinkle
 * @author Steve Millidge
 */
public class MySqlComponent implements DatabaseComponent<ResourceComponent<?>>, ConnectionPoolingSupport,
    ResourceComponent<ResourceComponent<?>>, MeasurementFacet {

    private static final Log LOG = LogFactory.getLog(MySqlComponent.class);

    private ResourceContext resourceContext;
    private AggregateProcessInfo aggregateProcessInfo;
    private Map<String, String> globalStatusValues = new HashMap<String, String>();
    private Map<String, String> globalVariables = new HashMap<String, String>();
    private MySqlPooledConnectionProvider pooledConnectionProvider;
    @Deprecated
    private Connection sharedConnection;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        buildSharedConnectionIfNeeded();
        pooledConnectionProvider = new MySqlPooledConnectionProvider(resourceContext.getPluginConfiguration());
        ProcessInfo processInfo = resourceContext.getNativeProcess();
        if (processInfo != null) {
            aggregateProcessInfo = processInfo.getAggregateProcessTree();
        } else {
            findProcessInfo();
        }
    }

    private void buildSharedConnectionIfNeeded() {
        try {
            if ((sharedConnection == null) || sharedConnection.isClosed()) {
                sharedConnection = MySqlDiscoveryComponent.buildConnection(this.resourceContext
                    .getPluginConfiguration());
            }
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not build shared connection", e);
            }
        }
    }

    public void stop() {
        resourceContext = null;
        DatabasePluginUtil.safeClose(sharedConnection);
        sharedConnection = null;
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

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("SHOW GLOBAL STATUS");
            while (resultSet.next()) {
                globalStatusValues.put(resultSet.getString(1), resultSet.getString(2));
            }

            resultSet.close();
            resultSet = statement.executeQuery("select * from information_schema.global_variables");
            while (resultSet.next()) {
                globalVariables.put(resultSet.getString(1), resultSet.getString(2));
            }
        } catch (SQLException ignore) {
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }

        // get process information
        aggregateProcessInfo = findProcessInfo();
        for (MeasurementScheduleRequest request : metrics) {
            String requestName = request.getName();
            if (requestName.startsWith("Process") && aggregateProcessInfo != null) {
                aggregateProcessInfo.refresh();
                if ("Process.aggregateMemory.resident".equals(requestName)) {
                    long mem = aggregateProcessInfo.getAggregateMemory().getResident();
                    report.addData(new MeasurementDataNumeric(request, new Double((double) mem)));
                } else if ("Process.aggregateMemory.size".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateMemory().getSize();
                    report.addData(new MeasurementDataNumeric(request, new Double((double) value)));
                } else if ("Process.aggregateMemory.pageFaults".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateMemory().getPageFaults();
                    report.addData(new MeasurementDataNumeric(request, new Double((double) value)));
                } else if ("Process.aggregateCpu.user".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateCpu().getUser();
                    report.addData(new MeasurementDataNumeric(request, new Double((double) value)));
                } else if ("Process.aggregateCpu.sys".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateCpu().getSys();
                    report.addData(new MeasurementDataNumeric(request, new Double((double) value)));
                } else if ("Process.aggregateCpu.percent".equals(requestName)) {
                    double value = aggregateProcessInfo.getAggregateCpu().getPercent();
                    report.addData(new MeasurementDataNumeric(request, new Double(value)));
                } else if ("Process.aggregateCpu.total".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateCpu().getTotal();
                    report.addData(new MeasurementDataNumeric(request, new Double((double) value)));
                } else if ("Process.aggregateFileDescriptor.total".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateFileDescriptor().getTotal();
                    report.addData(new MeasurementDataNumeric(request, new Double((double) value)));
                }
            } else {
                if (request.getDataType() == DataType.MEASUREMENT) {
                    try {
                        String strVal = globalStatusValues.get(request.getName());
                        double val = Double.parseDouble(strVal);
                        report.addData(new MeasurementDataNumeric(request, val));
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }

    public Connection getConnection() {
        buildSharedConnectionIfNeeded();
        return sharedConnection;
    }

    @Override
    public void removeConnection() {
        DatabasePluginUtil.safeClose(this.sharedConnection);
        this.sharedConnection = null;
    }

    private AggregateProcessInfo findProcessInfo() {
        AggregateProcessInfo result = null;
        // is still running reuse
        if (aggregateProcessInfo != null && aggregateProcessInfo.freshSnapshot().isRunning()) {
            result = aggregateProcessInfo;
        } else {
            long pid = findPID();
            if (pid != -1) {
                List<ProcessInfo> processes = resourceContext.getSystemInformation().getAllProcesses();
                for (ProcessInfo pi : processes) {
                    if (pid == pi.getPid()) {
                        result = pi.getAggregateProcessTree();
                        break;
                    }
                }
            }
        }
        return result;
    }

    private long findPID() {
        long result = -1;
        String pidFile = globalVariables.get("PID_FILE");
        if (pidFile == null) {
            return result;
        }
        File file = new File(pidFile);
        if (file.canRead()) {
            try {
                FileReader pidFileReader = new FileReader(file);
                try {
                    char pidData[] = new char[(int) file.length()];
                    pidFileReader.read(pidData);
                    String pidString = new String(pidData);
                    pidString = pidString.trim();
                    result = Long.valueOf(pidString);
                } finally {
                    pidFileReader.close();
                }
            } catch (Exception ex) {
                LOG.warn("Unable to read MySQL pid file " + pidFile);
            }
        }
        return result;
    }
}
