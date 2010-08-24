package org.rhq.enterprise.gui.coregui.client.components.table;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import java.util.Date;

/**
 * Formats a timestamp (i.e. milliseconds since Epoch).
 *
 * @author Ian Springer
 */
public class TimestampCellFormatter implements CellFormatter {
    private static final DateTimeFormat DATE_TIME_FORMAT = DateTimeFormat.getMediumDateTimeFormat();

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        if (value == null) {
            return "";
        }
        long longValue;
        if (value instanceof Long) {
            longValue = (Long)value;
        } else if (value instanceof Integer) {
            longValue = (Integer)value;
        } else if (value instanceof String) {
            longValue = Long.parseLong((String)value);
        } else {
            throw new IllegalArgumentException("value parameter is not a Long, an Integer, or a String.");
        }
        Date date = new Date(longValue);
        return DATE_TIME_FORMAT.format(date);
    }
}
