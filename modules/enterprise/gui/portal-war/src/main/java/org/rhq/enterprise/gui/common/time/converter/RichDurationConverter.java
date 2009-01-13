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
 * @author Greg Hinkle
 */
public class RichDurationConverter implements Converter {

    private static final long MILLIS_IN_SECOND = 1000L;
    private static final long MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE;
    private static final long MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;

    private static final DecimalFormat twoDigitFormatter = new DecimalFormat("00");

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        long millis = 0;
        if (value != null) {
            String[] parts = value.split(":");
            millis += (Integer.parseInt(parts[0]) * MILLIS_IN_HOUR);
            millis += (Integer.parseInt(parts[1]) * MILLIS_IN_MINUTE);
            millis += (Integer.parseInt(parts[2]) * MILLIS_IN_SECOND);
        }
        return Long.valueOf(millis);
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return "0"; // visual indicator of issue
        }
        long millis = (Long) value;
        StringBuilder buf = new StringBuilder();

        int days = (int) (millis / MILLIS_IN_DAY);
        millis %= MILLIS_IN_DAY;
        if (days > 0) {
            buf.append(days + " day");
            if (days != 1)
                buf.append("s");
        }

        int hours = (int) (millis / MILLIS_IN_HOUR);
        millis %= MILLIS_IN_HOUR;
        if (hours > 0) {
            if (buf.length() > 0) buf.append(", ");
            buf.append(hours + " hour");
            if (hours != 1)
                buf.append("s");

        }

        int mins = (int) (millis / MILLIS_IN_MINUTE);
        millis %= MILLIS_IN_MINUTE;
        if (mins > 0) {
            if (buf.length() > 0) buf.append(", ");
            buf.append(mins + " minute");
            if (mins != 1)
                buf.append("s");

        }


        return buf.toString();
    }

}