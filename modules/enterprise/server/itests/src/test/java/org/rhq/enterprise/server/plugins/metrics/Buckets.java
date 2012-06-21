/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.plugins.metrics;

import org.joda.time.DateTime;

import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;

/**
 * @author John Sanda
 */
public class Buckets {

    private int numDataPoints = MeasurementDataManagerUtility.DEFAULT_NUM_DATA_POINTS;

    private long[] buckets = new long[numDataPoints];

    private long interval;

    public Buckets(DateTime beginTime, DateTime endTime) {
        interval = (endTime.getMillis() - beginTime.getMillis()) / numDataPoints;
        for (int i = 0; i < numDataPoints; ++i) {
            buckets[i] = beginTime.getMillis() + (interval * i);
        }
    }

    public int getNumDataPoints() {
        return numDataPoints;
    }

    public long getInterval() {
        return interval;
    }

    public long get(int index) {
        return buckets[index];
    }

}
