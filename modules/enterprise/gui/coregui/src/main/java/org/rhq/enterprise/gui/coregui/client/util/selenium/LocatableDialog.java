package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.Dialog;

/**
 * Wrapper for com.smartgwt.client.widgets.Dialog that sets the ID for use with selenium scLocators.
 * 
 * @author jay shaughnessy
 */
public class LocatableDialog extends Dialog implements Locatable {

    private String locatorId;

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null.
     */
    public LocatableDialog(String locatorId) {
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
}
