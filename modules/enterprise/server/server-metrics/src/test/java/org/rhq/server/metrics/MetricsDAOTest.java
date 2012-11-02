/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import static org.joda.time.DateTime.now;
import static org.testng.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

/**
 * @author John Sanda
 */
public class MetricsDAOTest extends CassandraIntegrationTest {

    @Test
    public void findOneHourIndexEntries() {
        Connection connection = null;
        PreparedStatement statement = null;

        String sql = "INSERT INTO " + MetricsDAO.METRICS_INDEX_TABLE +
            " (bucket, time, schedule_id, null_col) VALUES (?, ?, ?, ?)";

        DateTime hour0 = now().hourOfDay().roundFloorCopy().minusHours(now().hourOfDay().get());
        int numSchedules = 2;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sql);

            for (int i = 0; i < 2; ++i) {
                for (int j = 0; j < numSchedules; ++j) {
                    statement.setString(1, MetricsDAO.ONE_HOUR_METRICS_TABLE);
                    statement.setDate(2, new java.sql.Date(hour0.plusHours(i + j).getMillis()));
                    statement.setInt(3, 100 + j);
                    statement.setBoolean(4, false);

                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CQLException(e);
        }

        MetricsDAO dao = new MetricsDAO(dataSource);
        List<MetricsIndexEntry> actualEntries = dao.findMetricsIndexEntries(MetricsDAO.ONE_HOUR_METRICS_TABLE);

        assertEquals(actualEntries.size(), 4, "Expected to get 4 entries but got " + actualEntries);
    }

}
