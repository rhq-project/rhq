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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.database.DatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 *
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlTableComponent implements DatabaseComponent, MeasurementFacet {

    private String tableName;
    private MySqlDatabaseComponent parent;
    private String databaseName;
    private Log log = LogFactory.getLog(this.getClass());

    @Override
    public Connection getConnection() {
        return parent.getConnection();
    }

    @Override
    public void removeConnection() {
        parent.removeConnection();
    }

    @Override
    public void start(ResourceContext rc) throws InvalidPluginConfigurationException, Exception {
        tableName = rc.getResourceKey();
        parent = (MySqlDatabaseComponent)rc.getParentResourceComponent();
        databaseName = parent.getName();
    }

    @Override
    public void stop() {
    }

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType result = AvailabilityType.DOWN;
        Connection conn = parent.getConnection();
        if (conn != null) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("show tables from " + databaseName + " like '" + tableName + "'");
                if (rs.first()) {
                    result = AvailabilityType.UP;
                }
            }catch (SQLException se) {
                // ignore as unablailable if we can't execute the query
            }finally {
                DatabaseQueryUtility.close(stmt, rs);
            }
        }
        return result;
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> set) throws Exception {
        Connection conn = parent.getConnection();
        if (conn != null ) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("show table status from " + databaseName+ " like '" + tableName + "'");
                if (rs.next()) {
                    for (MeasurementScheduleRequest request : set) {
                        String value = rs.getString(request.getName());
                        if (value == null) {value = "0";}
                        switch (request.getDataType()) {
                            case MEASUREMENT: {
                                mr.addData(new MeasurementDataNumeric(request, Double.valueOf(value)));
                                break;
                            } case TRAIT: {
                                mr.addData(new MeasurementDataTrait(request, value));
                                break;
                            } default: {
                                break;
                            }
                        }
                    }
                }
            } catch(Exception se) {
                if (log.isInfoEnabled()) {
                    log.info("Unable to measure table statistics", se);
                }
            }finally {
                DatabaseQueryUtility.close(stmt, rs);
            }
        }
    }

}
