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
package org.rhq.enterprise.server.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;

/**
 * Returns information about the database.
 * 
 * @see ServerDetails
 *
 * @author Joseph Marques
 * @author John Mazzitelli
 */
public final class SystemDatabaseInformation {

    private final Log log = LogFactory.getLog(SystemDatabaseInformation.class);

    private Map<Property, String> properties;

    public enum Property {
        DATABASE_CONNECTION_URL, //
        DATABASE_PRODUCT_NAME, //
        DATABASE_PRODUCT_VERSION, //
        DATABASE_DRIVER_NAME, //
        DATABASE_DRIVER_VERSION, //
        CURRENT_MEASUREMENT_TABLE, //
        NEXT_MEASUREMENT_TABLE_ROTATION;
    }

    private SystemDatabaseInformation() {
        DataSource ds = null;
        Connection conn = null;
        try {
            ds = LookupUtil.getDataSource();
            conn = ds.getConnection();
            DatabaseMetaData metadata = conn.getMetaData();

            String url = metadata.getURL();
            String productName = metadata.getDatabaseProductName();
            String productVersion = metadata.getDatabaseProductVersion();
            String driverName = metadata.getDriverName();
            String driverVersion = metadata.getDriverVersion();

            Map<Property, String> values = new HashMap<Property, String>();
            values.put(Property.DATABASE_CONNECTION_URL, url);
            values.put(Property.DATABASE_PRODUCT_NAME, productName);
            values.put(Property.DATABASE_PRODUCT_VERSION, productVersion);
            values.put(Property.DATABASE_DRIVER_NAME, driverName);
            values.put(Property.DATABASE_DRIVER_VERSION, driverVersion);

            values.put(Property.CURRENT_MEASUREMENT_TABLE, MeasurementDataManagerUtility.getCurrentRawTable());
            values.put(Property.NEXT_MEASUREMENT_TABLE_ROTATION, MeasurementDataManagerUtility.getNextRotationTime());

            properties = Collections.unmodifiableMap(values);

        } catch (Exception e) {
            log.error("Could not load properties for " + SystemDatabaseInformation.class.getSimpleName());
        } finally {
            if (properties == null) {
                Map<Property, String> values = new HashMap<Property, String>();
                for (Property prop : Property.values()) {
                    values.put(prop, "unknown");
                }
                properties = Collections.unmodifiableMap(values);
            }
            JDBCUtil.safeClose(conn);
        }
    }

    public static SystemDatabaseInformation getInstance() {
        return new SystemDatabaseInformation();
    }

    public Map<Property, String> getProperties(Property property) {
        return properties;
    }

    public String getDatabaseConnectionURL() {
        return properties.get(Property.DATABASE_CONNECTION_URL);
    }

    public String getDatabaseProductName() {
        return properties.get(Property.DATABASE_PRODUCT_NAME);
    }

    public String getDatabaseProductVersion() {
        return properties.get(Property.DATABASE_PRODUCT_VERSION);
    }

    public String getDatabaseDriverName() {
        return properties.get(Property.DATABASE_DRIVER_NAME);
    }

    public String getDatabaseDriverVersion() {
        return properties.get(Property.DATABASE_DRIVER_VERSION);
    }

    public String getCurrentMeasurementTable() {
        return properties.get(Property.CURRENT_MEASUREMENT_TABLE);
    }

    public String getNextMeasurementTableRotation() {
        return properties.get(Property.NEXT_MEASUREMENT_TABLE_ROTATION);
    }
}
