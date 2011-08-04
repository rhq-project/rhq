package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.HTMLFlow;

/**
 * Wrapper for com.smartgwt.client.widgets.HTMLFlow that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableHTMLFlow extends HTMLFlow implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableHTMLFlow(String locatorId) {
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
