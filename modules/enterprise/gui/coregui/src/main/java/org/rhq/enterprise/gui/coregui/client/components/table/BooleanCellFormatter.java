package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.ImageManager;

/**
 * Formats a Boolean value as an icon.
 * 
 * @author Ian Springer
 */
public class BooleanCellFormatter implements CellFormatter {

    private static String ICON_URL_NULL = ImageManager.getFullImagePath("subsystems/availability/availability_grey_16.png");
    private static String ICON_URL_TRUE = ImageManager.getFullImagePath("global/permission_enabled_11.png");
    private static String ICON_URL_FALSE = ImageManager.getFullImagePath("global/permission_disabled_11.png");

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        String imageUrl;
        if (value == null) {
            imageUrl = ICON_URL_NULL;
        } else if (value instanceof Boolean) {
            imageUrl = ((Boolean) value) ? ICON_URL_TRUE : ICON_URL_FALSE;
        } else {
            throw new IllegalArgumentException("value parameter is not a Boolean.");
        }
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        String result = "<img src=\"" + imageUrl + "\" width=\"11\" height=\"11\"/>";
        return result;
    }

}
