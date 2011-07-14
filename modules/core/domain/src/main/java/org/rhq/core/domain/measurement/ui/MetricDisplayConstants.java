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
package org.rhq.core.domain.measurement.ui;

/**
 * TODO we should convert this to an enum
 */
public class MetricDisplayConstants {
    /**
     * the peak value per interval of the set of metrics of this type measured in the timeframe under question. The peak
     * is the highest of the high values.
     */
    public static final String MAX_KEY = "max";

    /**
     * the low value of the set of metrics of this type per interval measured in the timeframe under question. The low
     * value is the lowest of the low values.
     */
    public static final String MIN_KEY = "min";

    /**
     * the average value per interval of the set of metrics of this type measured in the timeframe under question. The
     * average is the average of all the values.
     */
    public static final String AVERAGE_KEY = "average";

    /**
     * the last value per interval of the set of metrics of this type measured in the timeframe under question
     */
    public static final String LAST_KEY = "last";

    /**
     * the user defined main baseline to compare against for this metric
     */
    public static final String BASELINE_KEY = "baseline";

    /**
     * the user defined high range of values to compare against for this metric
     */
    public static final String HIGH_RANGE_KEY = "high";

    /**
     * the user defined low range of values to compare against for this metric
     */
    public static final String LOW_RANGE_KEY = "low";

    /**
     * a summary value for groups
     */
    public static final String SUMMARY_KEY = "summary";

    public static final String[] attrKey = { MAX_KEY, MIN_KEY, AVERAGE_KEY, LAST_KEY, BASELINE_KEY, HIGH_RANGE_KEY,
        LOW_RANGE_KEY, SUMMARY_KEY };
}