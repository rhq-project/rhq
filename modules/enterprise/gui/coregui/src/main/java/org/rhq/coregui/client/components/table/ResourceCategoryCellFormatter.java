package org.rhq.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;

/**
 * Formats a ResourceCategory value.
 * 
 * @author Ian Springer
 */
public class ResourceCategoryCellFormatter implements CellFormatter {

    private static final Messages MSG = CoreGUI.getMessages();

    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        if (value == null) {
            return "null";
        }
        ResourceCategory resourceCategory;
        if (value instanceof ResourceCategory) {
            resourceCategory = (ResourceCategory) value;
        } else if (value instanceof String) {
            String categoryName = (String) value;
            resourceCategory = ResourceCategory.valueOf(categoryName);
        } else {
            throw new IllegalArgumentException("This cell formatter does not support values of type " +
                    value.getClass().getName());
        }

        return getDisplayName(resourceCategory);
    }

    private String getDisplayName(ResourceCategory resourceCategory) {
        String displayName = "";

        switch (resourceCategory) {
            case PLATFORM:
                displayName = MSG.common_title_platform();
                break;
            case SERVER:
                displayName = MSG.common_title_server();
                break;
            case SERVICE:
                displayName = MSG.common_title_service();
                break;
        }

        return displayName;
    }

}
