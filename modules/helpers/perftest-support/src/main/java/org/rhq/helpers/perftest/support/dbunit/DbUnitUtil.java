/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.dbunit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.rhq.helpers.perftest.support.Settings;

/**
 *
 * @author Lukas Krejci
 */
public class DbUnitUtil {

    private static final Log LOG = LogFactory.getLog(DbUnitUtil.class);
    
    private DbUnitUtil() {
        
    }
    
    public static IDatabaseConnection getConnection(Properties settings) throws SQLException, DatabaseUnitException {
        String driverClass = settings.getProperty(Settings.DATABASE_DRIVER_CLASS_PROPERTY);
        if (driverClass != null) {
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                LOG.error("Failed to load the driver class.", e);
            }
        }
        return getConnection(settings.getProperty("url"), settings.getProperty("user"), settings.getProperty("password"));
    }
    
    public static IDatabaseConnection getConnection(String url, String user, String password) throws SQLException, DatabaseUnitException {
        Connection jdbcConnection = DriverManager.getConnection(url, user, password);
        
        return new DatabaseConnection(jdbcConnection);
    }
}
