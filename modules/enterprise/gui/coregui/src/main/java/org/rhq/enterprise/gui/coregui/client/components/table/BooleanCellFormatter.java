package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * Formats a Boolean value as "yes" or "no".
 * 
 * @author Ian Springer
 */
public class BooleanCellFormatter implements CellFormatter {
    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        String result;
        if (value == null) {
            result = "";
        } else if (value instanceof Boolean) {
            result = ((Boolean) value) ? "yes" : "no";
        } else {
            throw new IllegalArgumentException("value parameter is not a Boolean.");
        }
        return result;
    }
}
