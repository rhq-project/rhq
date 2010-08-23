package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.HTMLPane;

/**
 * Wrapper for com.smartgwt.client.widgets.HTMLPane that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableHTMLPane extends HTMLPane implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "scClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableHTMLPane(String locatorId) {
        super();
        this.locatorId = locatorId;
        String unsafeId = this.getScClassName() + "-" + locatorId;
        setID(SeleniumUtility.getSafeId(unsafeId));
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String extendLocatorId(String extension) {
        return this.locatorId + "-" + extension;
    }

}
