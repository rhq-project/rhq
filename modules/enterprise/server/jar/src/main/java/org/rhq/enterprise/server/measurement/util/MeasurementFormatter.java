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
package org.rhq.enterprise.server.measurement.util;

import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.util.MeasurementConversionException;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;

public class MeasurementFormatter {
    private static final String MEASUREMENT_BASELINE_MIN_TEXT = "Min Value";
    private static final String MEASUREMENT_BASELINE_MEAN_TEXT = "Baseline Value";
    private static final String MEASUREMENT_BASELINE_MAX_TEXT = "Max Value";

    public static String getBaselineText(String baselineOption, MeasurementSchedule schedule) {
        if ((null != schedule) && (null != schedule.getBaseline())) {
            MeasurementBaseline baseline = schedule.getBaseline();

            String lookupText = null;
            Double value = null;

            if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MIN)) {
                lookupText = MEASUREMENT_BASELINE_MIN_TEXT;
                value = baseline.getMin();
            } else if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MEAN)) {
                lookupText = MEASUREMENT_BASELINE_MEAN_TEXT;
                value = baseline.getMean();
            } else if (baselineOption.equals(MeasurementConstants.BASELINE_OPT_MAX)) {
                lookupText = MEASUREMENT_BASELINE_MAX_TEXT;
                value = baseline.getMax();
            }

            if (value != null) {
                try {
                    String formatted = MeasurementConverter.scaleAndFormat(value, schedule, true);
                    return formatted + " (" + lookupText + ")";
                } catch (MeasurementConversionException mce) {
                    return lookupText;
                }
            }
            /*
             * will need a fall-through here because the value was null; this can happen when the user requests to view
             * the formatted baseline before the first time it has been calculated
             */
        }

        // here is the fall-through
        if (MeasurementConstants.BASELINE_OPT_MIN.equals(baselineOption)) {
            return MEASUREMENT_BASELINE_MIN_TEXT;
        } else if (MeasurementConstants.BASELINE_OPT_MAX.equals(baselineOption)) {
            return MEASUREMENT_BASELINE_MAX_TEXT;
        } else {
            return MEASUREMENT_BASELINE_MEAN_TEXT;
        }
    }
}