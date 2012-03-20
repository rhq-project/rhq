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
package org.rhq.enterprise.server.rest.reporting;

import java.text.DateFormat;
import java.util.Date;

/**
 * Formatting tools for rest reporting.
 */
public class ReportHelper {

    private ReportHelper(){
       // This is just a static utility class
    }

    /**
     * Strip out any invalid characters from CSV data.
     * @param input
     * @return Cleaned String suitable for inclusion in CSV file
     */
    public static String cleanForCSV(String input){
        return input.replace(',',' ').replace('\n', ' ');
    }

    /**
     * Standard Date/time format for CSV files
     * @param epochMillis
     * @return String formatted string (i.e. '11/4/03 8:14 PM')
     */
    public static String formatDateTime(long epochMillis){
        Date date = new Date(epochMillis);
        return DateFormat.getInstance().format(date);
    }


    /**
     * Standard Date/time format for CSV files
     * @param epochMillis
     * @return String formatted string (i.e. '11/4/03')
     */
    public static String formatDate(long epochMillis){
        Date date = new Date(epochMillis);
        return DateFormat.getDateInstance().format(date);
    }

}
