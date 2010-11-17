package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * Formats a Boolean value as "yes" or "no".
 * 
 * @author Ian Springer
 */
public class BooleanCellFormatter implements CellFormatter {
    private static final Messages MSG = CoreGUI.getMessages();

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        String result;
        if (value == null) {
            result = "";
        } else if (value instanceof Boolean) {
            result = ((Boolean) value) ? MSG.common_val_yes_lower() : MSG.common_val_no_lower();
        } else {
            throw new IllegalArgumentException("value parameter is not a Boolean.");
        }
        return result;
    }
}
