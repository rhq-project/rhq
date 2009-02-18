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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * @author Joseph Marques
 */
public class RelativeDurationConverter implements Converter {

    private static final long MILLIS_IN_SECOND = 1000L;
    private static final long MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE;
    private static final long MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;

    private static final SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aaa z");

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return null; // we only support rendering, deserialization back into object not possible
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
            throw new IllegalArgumentException("The " + RelativeDurationConverter.class.getSimpleName()
                + " converter does not support rendering objects of type " + value.getClass().getSimpleName());
        }

        long lastMidnight = getMidnightMillis();
        long previousMidnight = lastMidnight - MILLIS_IN_DAY;
        String result = null;
        if (millis < previousMidnight) {
            // <time> "ago"
            result = getRelativeTimeAgo(millis) + " ago";
        } else if (millis < lastMidnight) {
            // "yesterday" <time>
            result = "Yesterday, " + formatter.format(new Date(millis));
        } else {
            // <time>
            result = formatter.format(new Date(millis));
        }

        return result;
    }

    private long getMidnightMillis() {
        long epochNow = System.currentTimeMillis();
        long epochMillisSinceMidnight = epochNow % MILLIS_IN_DAY;

        int localOffset = TimeZone.getDefault().getOffset(epochNow);
        long epochMidnight = epochNow - epochMillisSinceMidnight;
        long localMidnightMillis = epochMidnight - localOffset;

        return localMidnightMillis;
    }

    private String getRelativeTimeAgo(long millis) {
        StringBuilder buf = new StringBuilder();
        long timeAgo = System.currentTimeMillis() - millis;

        int days = (int) (timeAgo / MILLIS_IN_DAY);
        timeAgo %= MILLIS_IN_DAY;
        if (days > 0) {
            buf.append(days + " day");
            if (days != 1)
                buf.append("s");
        }

        int hours = (int) (timeAgo / MILLIS_IN_HOUR);
        timeAgo %= MILLIS_IN_HOUR;
        if (hours > 0) {
            if (buf.length() > 0)
                buf.append(", ");
            buf.append(hours + " hour");
            if (hours != 1)
                buf.append("s");
        }

        int mins = (int) (timeAgo / MILLIS_IN_MINUTE);
        timeAgo %= MILLIS_IN_MINUTE;
        if (mins > 0) {
            if (buf.length() > 0)
                buf.append(", ");
            buf.append(mins + " minute");
            if (mins != 1)
                buf.append("s");
        }

        return buf.toString();
    }

}