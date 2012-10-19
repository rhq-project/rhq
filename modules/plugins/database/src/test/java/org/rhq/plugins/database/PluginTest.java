/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.database;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;

@Test
public class PluginTest extends ComponentTest {

    public void test() throws Exception {
        H2Database db = (H2Database)manuallyAdd("H2 Database");
        assertUp(db);
        Connection connection = db.getConnection();
        connection.prepareStatement("create table sometable(a int, b int)").execute();
        connection.prepareStatement("insert into sometable values(42, 54)").execute();
        ResourceType rt = resourceTypes.get("Generic Query");
        Configuration configuration = getConfiguration(rt);
        CustomTableComponent ctc = (CustomTableComponent) manuallyAdd(rt, configuration, db);
        MeasurementReport report = getMeasurementReport(ctc);
        assertAll(report, getResourceDescriptor("Generic Query"));
        List<Map<String, Object>> grid = DatabaseQueryUtility.getGridValues(db, "select a, b from sometable");
        assert grid.size() == 1;
        Map<String, Object> map = grid.get(0);
        AssertJUnit.assertEquals(42, map.get("A"));
    }

}
