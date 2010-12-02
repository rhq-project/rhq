package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.ImageManager;

/**
 * Formats a Boolean value as "yes" or "no" image.
 * 
 * @author Ian Springer
 */
public class BooleanCellFormatter implements CellFormatter {
    private static String RED = ImageManager.getAvailabilityIcon(Boolean.FALSE);
    private static String GREEN = ImageManager.getAvailabilityIcon(Boolean.TRUE);
    private static String GREY = ImageManager.getAvailabilityIcon(null);

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
