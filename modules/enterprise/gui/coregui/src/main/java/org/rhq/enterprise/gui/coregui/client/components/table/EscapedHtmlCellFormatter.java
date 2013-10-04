package org.rhq.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.util.StringUtility;

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
