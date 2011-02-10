package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * Wrapper for com.smartgwt.client.widgets.grid.ListGrid that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableListGrid extends ListGrid implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "scClassname-id"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableListGrid(String locatorId) {
        super();
        this.locatorId = locatorId;
        SeleniumUtility.setID(this, locatorId);
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String extendLocatorId(String extension) {
        return this.locatorId + "_" + extension;
    }

    public String getValueIcon(ListGridField field, Object value, ListGridRecord record) {
        return null;
    }

}
