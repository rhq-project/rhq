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
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.database.DatabaseComponent;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class MySqlComponent implements DatabaseComponent, ResourceComponent, MeasurementFacet {

    private ResourceContext resourceContext;
    private Connection connection;
    private static final Log log = LogFactory.getLog(MySqlComponent.class);

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        getConnection();
    }

    public void stop() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            log.warn(e);
        }
    }

    public AvailabilityType getAvailability() {

        try {
            getConnection().createStatement().executeQuery("select 1");
            return AvailabilityType.UP;
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("getAvail failed: " + e.getMessage());
            }
            return AvailabilityType.DOWN;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        ResultSet rs = getConnection().createStatement().executeQuery("SHOW /*!50002 GLOBAL */ STATUS");

        Map<String, String> values = new HashMap<String, String>();
        while (rs.next()) {
            values.put(rs.getString(1), rs.getString(2));
        }


        for (MeasurementScheduleRequest request : metrics) {
            if (!request.getName().startsWith("Process")) {

                if (request.getDataType() == DataType.MEASUREMENT) {
                    try {
                        String strVal = values.get(request.getName());
                        double val = Double.parseDouble(strVal);
                        report.addData(new MeasurementDataNumeric(request, val));
                    } catch (Exception e) {  }
                }
            }
        }
    }





    public Connection getConnection() {
        try {
            this.connection = MySqlDiscoveryComponent.buildConnection(resourceContext.getPluginConfiguration());

        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("getAvail failed: " + e.getMessage());
            }
        }
        return connection;
    }

    public void removeConnection() {
        this.connection = null;
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            conn =
               DriverManager.getConnection("jdbc:mysql://192.168.1.5?user=rhqadmin&password=rhqadmin");

            DatabaseMetaData dmd = conn.getMetaData();
                        System.out.println("Version: " + dmd.getDatabaseProductVersion());
                        System.out.println("Product: " + dmd.getDatabaseProductName());



            // The default changed in 5.0.2... the following gets globabl data for all versions
//            ResultSet rs = conn.createStatement().executeQuery("SHOW /*!50002 GLOBAL *//* STATUS");
            ResultSet rs = conn.createStatement().executeQuery("SHOW TABLE STATUS FROM mysql");
//            ResultSet rs = dmd.getTables(null, null, null, null);//Catalogs();//Schemas();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount();i++) {
                System.out.print(md.getColumnName(i) + "    ");
            }
            System.out.println("");
            while (rs.next()) {

                for (int i = 1; i <= md.getColumnCount();i++) {
                    System.out.print(rs.getObject(i) + "    ");
                }
                System.out.println("");
            }
        } catch (SQLException ex) {
            // handle any errors
            log.info("SQLException: " + ex.getMessage());
            log.info("SQLState: " + ex.getSQLState());
            log.info("VendorError: " + ex.getErrorCode());
        }
    }
}
