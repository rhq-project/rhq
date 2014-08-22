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
package org.rhq.enterprise.server.rest.reporting;

import org.rhq.core.domain.resource.Resource;

import java.text.DateFormat;
import java.util.Date;

/**
 * Formatting tools for rest reporting.
 */
public class ReportFormatHelper {

    private ReportFormatHelper(){
       // This is just a static utility class
    }

    /**
     * Quote column if there are delimiter characters in the data
     * @param input Original column data to be written to the CSV
     * @return String Quoted if necessary
     */
    public static String quoteIfInvalidCharacters(String input) {
        if (input == null) {
            return " ";
        } else if(input.contains(CsvWriter.DELIMITER)) {
            StringBuilder quoted = new StringBuilder();
            quoted.append("\"").append(input).append("\"");
            return quoted.toString();
        }
        return input;
    }

    /**
     * Strip out any invalid characters from CSV data.
     * @param input
     * @return Cleaned String suitable for inclusion in CSV file
     */
    public static String cleanForCSV(String input){
        if (input == null) {
            return " ";
        }
        return input.replace(',',' ').replace('\n', ' ');
    }

    /**
     * Standard Date/time format for CSV files
     * @param epochMillis
     * @return String formatted string (i.e. '11/4/03 8:14 PM')
     */
    public static String formatDateTime(long epochMillis){
        if(epochMillis != 0){
            Date date = new Date(epochMillis);
            return  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(date);
        }else {
            return " ";
        }
    }


    /**
     * Standard Date/time format for CSV files
     * @param epochMillis
     * @return String formatted string (i.e. '11/4/03')
     */
    public static String formatDate(long epochMillis){
        if(epochMillis != 0){
        Date date = new Date(epochMillis);
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
        }else {
            return " ";
        }
    }

    public static String parseAncestry(String ancestry) {
        if (null == ancestry) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
        for (int i = 0; i < ancestryEntries.length; ++i) {
            String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
            String ancestorName = entryTokens[2];
            builder.append((i > 0) ? " < " : "");
            builder.append(ancestorName);

        }

        return builder.toString();
    }

}
