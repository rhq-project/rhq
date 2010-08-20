package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.tree.TreeGrid;

/**
 * Wrapper for com.smartgwt.client.widgets.tree.TreeGrid that sets the ID for use with selenium scLocators.
 * 
 * @author jay shaughnessy
 */
public class LocatableTreeGrid extends TreeGrid {

    /**
     * ID set explicitly
     * @param id not null or empty
     */
    public LocatableTreeGrid(String id) {
        super();
        String locatorId = this.getScClassName() + "-" + id;
        setID(SeleniumUtility.getSafeId(locatorId, locatorId));
    }

}
