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

    private static String RED = "/images/icons/availability_red_16.png";
    private static String GREEN = "/images/icons/availability_green_16.png";
    private static String GREY = "/images/icons/availability_grey_16.png";

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        String result;
        if (value == null) {
            result = "<img src=\"" + GREY + "\"/>";
        } else if (value instanceof Boolean) {
            result = ((Boolean) value) ? "<img src=\"" + GREEN + "\"/>" : "<img src=\"" + RED + "\"/>";
        } else {
            throw new IllegalArgumentException("value parameter is not a Boolean.");
        }
        return result;
    }
}
