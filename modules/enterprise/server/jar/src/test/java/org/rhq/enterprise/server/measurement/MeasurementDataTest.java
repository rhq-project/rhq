/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.measurement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;

@Test
public class MeasurementDataTest {

    public void testComparator() {
        MeasurementData m1 = new MeasurementDataNumeric(20L, 1, 100.0);
        MeasurementData m2 = new MeasurementDataNumeric(20L, 2, 100.0);
        MeasurementData m3 = new MeasurementDataNumeric(20L, 3, 100.0);
        MeasurementData m4 = new MeasurementDataNumeric(10L, 1, 100.0); // same schedId, earlier time
        MeasurementData m5 = new MeasurementDataNumeric(10L, 2, 100.0); // same schedId, earlier time
        MeasurementData m6 = new MeasurementDataNumeric(10L, 3, 100.0); // same schedId, earlier time;
        MeasurementData m7 = new MeasurementDataNumeric(20L, 3, 100.0); // duplicate

        ArrayList<MeasurementData> data = new ArrayList<MeasurementData>();
        data.add(m1);
        data.add(m2);
        data.add(m3);
        data.add(m4);
        data.add(m5);
        data.add(m6);
        data.add(m7);
        Set<MeasurementData> insertedData = new TreeSet<MeasurementData>(new Comparator<MeasurementData>() {
            @Override
            public int compare(MeasurementData d1, MeasurementData d2) {
                int c = Integer.compare(d1.getScheduleId(), d2.getScheduleId());
                if (c != 0) {
                    return c;
                }
                return Long.compare(d1.getTimestamp(), d2.getTimestamp());
            }
        });
        insertedData.addAll(data);
        Assert.assertEquals(insertedData.size(), 6, data.toString());
        Iterator<MeasurementData> i = insertedData.iterator();
        MeasurementData d1 = i.next();
        MeasurementData d2 = i.next();
        MeasurementData d3 = i.next();
        MeasurementData d4 = i.next();
        MeasurementData d5 = i.next();
        MeasurementData d6 = i.next();
        Assert.assertEquals(d1, m4, d1.toString());
        Assert.assertEquals(d2, m1, d2.toString());
        Assert.assertEquals(d3, m5, d3.toString());
        Assert.assertEquals(d4, m2, d4.toString());
        Assert.assertEquals(d5, m6, d5.toString());
        Assert.assertEquals(d6, m3, d6.toString());
    }
}
