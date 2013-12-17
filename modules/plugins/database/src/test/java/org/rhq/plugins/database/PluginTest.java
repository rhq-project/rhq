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

package org.rhq.plugins.database;

import static org.rhq.plugins.database.DatabasePluginUtil.getGridValues;
import static org.testng.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

@Test
public class PluginTest extends ComponentTest {

    public void test() throws Exception {

        H2Database db = (H2Database) manuallyAdd("H2 Database");
        assertUp(db);

        setupData(db);

        // Pooling < Non Pooling < Pooling hierarchy

        ResourceComponent level1 = add("Generic Query", db);
        assertUp(level1);
        checkAllMetrics("Generic Query", level1);

        ResourceComponent level2 = add("Generic Query Non Pooling", level1);
        assertUp(level2);
        checkAllMetrics("Generic Query Non Pooling", level2);

        ResourceComponent level3 = add("Nested Generic Query", level2);
        assertUp(level3);
        checkAllMetrics("Generic Query Non Pooling", level3);

        checkData(db);
    }

    private ResourceComponent add(String resourceType, ResourceComponent component) throws Exception {
        ResourceType rt = resourceTypes.get(resourceType);
        Configuration configuration = getConfiguration(rt);
        return manuallyAdd(rt, configuration, component);
    }

    private void checkAllMetrics(String resourceType, ResourceComponent component) throws Exception {
        MeasurementReport report = getMeasurementReport(component);
        assertAll(report, getResourceDescriptor(resourceType));
    }

    private void setupData(H2Database db) throws SQLException {
        Connection connection = null;
        try {
            connection = db.getPooledConnectionProvider().getPooledConnection();
            connection.prepareStatement("create table table_a(a int, b int)").execute();
            connection.prepareStatement("insert into table_a values(1, 2)").execute();
            connection.prepareStatement("create table table_b(a int, b int)").execute();
            connection.prepareStatement("insert into table_b values(3, 4)").execute();
            connection.prepareStatement("create table table_c(a int, b int)").execute();
            connection.prepareStatement("insert into table_c values(5, 6)").execute();
        } finally {
            DatabasePluginUtil.safeClose(connection);
        }
    }

    private void checkData(H2Database db) throws SQLException {
        List<Map<String, Object>> grid = getGridValues(db, "select a, b from table_a");
        assertEquals(grid.size(), 1);
        assertEquals(grid.get(0).get("A"), 1);
        assertEquals(grid.get(0).get("B"), 2);
        grid = getGridValues(db, "select a, b from table_b");
        assertEquals(grid.size(), 1);
        assertEquals(grid.get(0).get("A"), 3);
        assertEquals(grid.get(0).get("B"), 4);
        grid = getGridValues(db, "select a, b from table_c");
        assertEquals(grid.size(), 1);
        assertEquals(grid.get(0).get("A"), 5);
        assertEquals(grid.get(0).get("B"), 6);
        System.out.println("grid = " + grid);
    }

}
