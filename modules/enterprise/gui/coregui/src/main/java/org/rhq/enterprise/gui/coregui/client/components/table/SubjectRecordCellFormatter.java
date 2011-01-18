package org.rhq.enterprise.gui.coregui.client.components.table;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.OperationScheduleDataSource;

/**
 * Formats a {@link org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.OperationScheduleDataSource.SubjectRecord}
 * (the SmartGWT {@link Record} representation of a {@link Subject}).
 * 
 * @author Ian Springer
 */
public class SubjectRecordCellFormatter implements CellFormatter {

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        String result;
        if (value == null) {
            result = "";
        } else if (value instanceof JavaScriptObject) {
            JavaScriptObject javaScriptObject = (JavaScriptObject) value;
            Record subjectRecord = new ListGridRecord(javaScriptObject);
            result = subjectRecord.getAttribute("name"); // the Subject's username
        } else {
            throw new IllegalArgumentException("value parameter is not a SubjectRecord - it is a "
                    + value.getClass().getName() + ".");
        }
        return result;
    }

}
