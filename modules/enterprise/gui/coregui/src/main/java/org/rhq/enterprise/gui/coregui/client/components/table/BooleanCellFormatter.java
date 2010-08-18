package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author Ian Springer
 */
public class BooleanCellFormatter implements CellFormatter {
    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "yes" : "no";
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value) ? "yes" : "no";
        } else {
            throw new IllegalArgumentException("value parameter is not a Boolean or a String.");
        }
    }
}
