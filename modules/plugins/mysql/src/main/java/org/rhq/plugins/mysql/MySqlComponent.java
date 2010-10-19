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
package org.rhq.plugins.mysql;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.DatabaseComponent;

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
import org.rhq.core.system.AggregateProcessInfo;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 * @author Greg Hinkle
 * @author Steve Millidge
 */
public class MySqlComponent implements DatabaseComponent, ResourceComponent, MeasurementFacet {

    private ResourceContext resourceContext;
    private AggregateProcessInfo aggregateProcessInfo;
    private MySqlConnectionInfo info;
    private Log log = LogFactory.getLog(this.getClass());
    private Map<String, String> globalStatusValues = new HashMap<String, String>();
    private Map<String, String> globalVariables = new HashMap<String, String>();

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        info = MySqlDiscoveryComponent.buildConnectionInfo(resourceContext.getPluginConfiguration());
        ProcessInfo processInfo = resourceContext.getNativeProcess();
        if (processInfo != null) {
            aggregateProcessInfo = processInfo.getAggregateProcessTree();
        } else {
            //findProcessInfo();
            //log.debug("Unable to locate native process information. Process level statistics will be unavailable.");
        }
    }

    public void stop() {
        MySqlConnectionManager.getConnectionManager().closeConnection(info);
    }

    public AvailabilityType getAvailability() {
        if (log.isDebugEnabled()) {
            log.debug("Doing an availability check on " + info.buildURL());
        }

        Connection conn = getConnection();
        AvailabilityType result = AvailabilityType.DOWN;
        if (conn != null) {
            // the connection must be OK as the validity check will have worked
            result = AvailabilityType.UP;
        }
        if (log.isDebugEnabled()) {
            log.debug("Availability check on " + info.buildURL() + " gives " + result);
        }
        return result;

    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Connection conn = getConnection();
        if (conn != null) {
            ResultSet rs = null;
            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SHOW GLOBAL STATUS");
                while (rs.next()) {
                    globalStatusValues.put(rs.getString(1), rs.getString(2));
                }

                rs.close();
                rs = stmt.executeQuery("select * from information_schema.global_variables");
                while (rs.next()) {
                    globalVariables.put(rs.getString(1), rs.getString(2));
                }
            } catch (SQLException sqle) {
            } finally {
                DatabaseQueryUtility.close(stmt, rs);
            }
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
                }else if ("Process.aggregateMemory.pageFaults".equals(requestName)) {
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
                }  else if ("Process.aggregateCpu.total".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateCpu().getTotal();
                    report.addData(new MeasurementDataNumeric(request, new Double((double)value)));
                }else if ("Process.aggregateFileDescriptor.total".equals(requestName)) {
                    long value = aggregateProcessInfo.getAggregateFileDescriptor().getTotal();
                    report.addData(new MeasurementDataNumeric(request, new Double((double)value)));
                }
            } else {
                if (request.getDataType() == DataType.MEASUREMENT) {
                    try {
                        String strVal = globalStatusValues.get(request.getName());
                        double val = Double.parseDouble(strVal);
                        report.addData(new MeasurementDataNumeric(request, val));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public Connection getConnection() {
        try {
            return MySqlConnectionManager.getConnectionManager().getConnection(info);
        } catch (SQLException ex) {
            log.warn("Unable to obtain database connection ", ex);
            return null;
        }
    }

    @Override
    public void removeConnection() {
        MySqlConnectionManager.getConnectionManager().closeConnection(info);
    }

    private AggregateProcessInfo findProcessInfo() {
        AggregateProcessInfo result = null;
        // is still running reuse
        if (aggregateProcessInfo != null && aggregateProcessInfo.isRunning()) {
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
        File file = new File(pidFile);
        if (file.canRead()) {
            try {
                FileReader pidFileReader = new FileReader(file);
                char pidData[] = new char[(int)file.length()];
                pidFileReader.read(pidData);
                String pidString = new String(pidData);
                pidString = pidString.trim();
                result = Long.valueOf(pidString);
            } catch (Exception ex) {
                log.warn("Unable to read MySQL pid file " + pidFile);
            }
        }
        return result;
    }
}
