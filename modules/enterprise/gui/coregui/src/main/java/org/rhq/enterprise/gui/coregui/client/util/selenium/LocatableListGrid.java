package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.grid.ListGrid;

/**
 * Wrapper for com.smartgwt.client.widgets.grid.ListGrid that sets the ID for use with selenium scLocators.
 *
 * @author Jay Shaughnessy
 */
public class LocatableListGrid extends ListGrid {

    /**
     * <pre>
     * ID Format: "scClassname-id"
     * </pre>
     *
     * @param id not null or empty.
     */
    public LocatableListGrid(String id) {
        super();
        String locatorId = this.getScClassName() + "-" + id;
//        setID(SeleniumUtility.getSafeId(locatorId, locatorId));
    }
}
