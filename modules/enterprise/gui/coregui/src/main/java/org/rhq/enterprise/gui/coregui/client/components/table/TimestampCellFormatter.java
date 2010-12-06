package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * Formats a timestamp (i.e. milliseconds since Epoch).
 *
 * @author Ian Springer
 */
public class TimestampCellFormatter implements CellFormatter {
    public static final DateTimeFormat DATE_TIME_FORMAT = DateTimeFormat.getMediumDateTimeFormat();

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        return format(value);
    }

    /**
     * This is a public static so others can use the same date/time format as the cell formatter,
     * even if the caller doesn't have data from a cell or list grid. This can keep date formatting
     * consistent across the app, whether the data is in a cell or not.
     * 
     * @param date the date to format as a Date, Long, Integer or a String
     * @return the formatted date string
     */
    public static String format(Object value) {
        if (value == null) {
            return "";
        }
        Date date;
        if (value instanceof Date) {
            date = (Date) value;
        } else {
            long longValue;
            if (value instanceof Long) {
                longValue = (Long) value;
            } else if (value instanceof Integer) {
                longValue = (Integer) value;
            } else if (value instanceof String) {
                longValue = Long.parseLong((String) value);
            } else {
                throw new IllegalArgumentException("value parameter is not a Date, Long, Integer, or a String.");
            }
            date = new Date(longValue);
        }
        return DATE_TIME_FORMAT.format(date);
    }
}
