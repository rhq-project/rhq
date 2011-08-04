package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;

/**
 * A cell formatter that escapes any HTML in the cell's value.
 * 
 * @author Ian Springer
 */
public class EscapedHtmlCellFormatter implements CellFormatter {

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        return (value != null) ? StringUtility.escapeHtml(value.toString()) : "";
    }

}
