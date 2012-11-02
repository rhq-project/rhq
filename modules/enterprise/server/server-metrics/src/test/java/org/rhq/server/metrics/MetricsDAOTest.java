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

import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;
import static org.rhq.server.metrics.MetricsDAO.ONE_HOUR_METRICS_TABLE;
import static org.rhq.test.AssertUtils.assertCollectionMatchesNoOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

/**
 * @author John Sanda
 */
public class MetricsDAOTest extends CassandraIntegrationTest {

    @Test
    public void updateAndFindOneHourIndexEntries() {
        DateTime hour0 = now().hourOfDay().roundFloorCopy().minusHours(now().hourOfDay().get());

        Map<Integer, DateTime> updates = new HashMap<Integer, DateTime>();
        updates.put(100, hour0);
        updates.put(101, hour0);

        MetricsDAO dao = new MetricsDAO(dataSource);
        dao.updateMetricsIndex(ONE_HOUR_METRICS_TABLE, updates);
        List<MetricsIndexEntry> actual = dao.findMetricsIndexEntries(ONE_HOUR_METRICS_TABLE);

        List<MetricsIndexEntry> expected = asList(new MetricsIndexEntry(ONE_HOUR_METRICS_TABLE, hour0, 100),
            new MetricsIndexEntry(ONE_HOUR_METRICS_TABLE, hour0, 101));
        assertCollectionMatchesNoOrder(expected, actual, "Failed to update or retrieve metrics index entries");
    }

}
