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
package org.rhq.enterprise.gui.image.chart;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScaleFormatter {
    private static final int MINUTE = 60000;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    private static final String DATE_FORMAT = "M/d";
    private static final String TIME_FORMAT = "h:mma";
    private static final String DATETIME_FORMAT = "M/d/yyyy h:mma";
    private static final String MULTILINE_DATETIME_FORMAT = TIME_FORMAT + '\n' + DATE_FORMAT;

    private static final String[] AM_PM = { "a", "p" };

    private static final SimpleDateFormat m_fmt = new SimpleDateFormat();

    public static String formatTime(long time) {
        m_fmt.applyPattern(DATETIME_FORMAT);
        return m_fmt.format(new Date(time));
    }

    public static String formatTime(long time, long scale, long units) {
        DateFormatSymbols symMod = m_fmt.getDateFormatSymbols();
        symMod.setAmPmStrings(AM_PM);
        m_fmt.setDateFormatSymbols(symMod);

        //        long tmp = (scale / units );
        //        if( tmp > DAY)
        //            m_fmt.applyLocalizedPattern(DATE_FORMAT);
        //        else
        //            m_fmt.applyLocalizedPattern(TIME_FORMAT);

        m_fmt.applyPattern(MULTILINE_DATETIME_FORMAT);

        return m_fmt.format(new Date(time));
    }
}