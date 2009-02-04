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
package org.rhq.enterprise.gui.common.time.converter;

import java.text.DecimalFormat;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * @author Joseph Marques
 */
public class ShortDurationConverter implements Converter {

    private static final long MILLIS_IN_SECOND = 1000L;
    private static final long MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE;
    private static final long MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;

    private static final long SECONDS_IN_MINUTE = 60L;
    private static final long SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE;
    private static final long MINUTES_IN_HOUR = 60L;

    private static final long HOURS_IN_DAY = 24L;
    private static final long MINS_IN_DAY = 60 * HOURS_IN_DAY;
    private static final long SECS_IN_DAY = 60 * MINS_IN_DAY;

    private static final DecimalFormat twoDigitFormatter = new DecimalFormat("0.00");

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        throw new UnsupportedOperationException("The " + ShortDurationConverter.class.getSimpleName()
            + " does not support getAsObject(FacesContext, UIComponent, String");
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "0"; // visual indicator of issue
        }
        long millis = 0;
        if (value instanceof Long) {
            millis = (Long) value;
        } else if (value instanceof Double) {
            millis = ((Double) value).longValue();
        } else {
            throw new IllegalArgumentException("The " + ShortDurationConverter.class.getSimpleName()
                + " converter does not support rendering objects of type " + value.getClass().getSimpleName());
        }

        int days = (int) (millis / MILLIS_IN_DAY);
        millis %= MILLIS_IN_DAY;

        int hours = (int) (millis / MILLIS_IN_HOUR);
        millis %= MILLIS_IN_HOUR;

        int mins = (int) (millis / MILLIS_IN_MINUTE);
        millis %= MILLIS_IN_MINUTE;

        int secs = (int) (millis / MILLIS_IN_SECOND);
        millis %= MILLIS_IN_SECOND;

        double result;
        String suffix;
        if (days > 0) {
            double fractionalDays = ((double) hours / HOURS_IN_DAY) + ((double) mins / MINS_IN_DAY)
                + ((double) secs / SECS_IN_DAY) + ((double) millis / MILLIS_IN_DAY);
            result = hours + fractionalDays; // totalDays
            suffix = "d";
        } else if (hours > 0) {
            double fractionalHrs = ((double) mins / MINUTES_IN_HOUR) + ((double) secs / SECONDS_IN_HOUR)
                + ((double) millis / MILLIS_IN_HOUR);
            result = hours + fractionalHrs; // totalHrs
            suffix = "h";
        } else if (mins > 0) {
            double fractionalMins = ((double) secs / SECONDS_IN_MINUTE) + ((double) millis / MILLIS_IN_MINUTE);
            result = mins + fractionalMins; // totalMins
            suffix = "m";
        } else if (secs > 0) {
            double fractionalSecs = ((double) millis / MILLIS_IN_SECOND);
            result = secs + fractionalSecs; // totalSecs
            suffix = "s";
        } else {
            result = millis;
            suffix = "ms";
        }

        return twoDigitFormatter.format(result) + suffix;
    }

}