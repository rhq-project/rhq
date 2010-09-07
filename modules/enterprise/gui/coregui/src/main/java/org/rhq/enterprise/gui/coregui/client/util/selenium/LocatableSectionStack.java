package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.SectionStack;

/**
 * Wrapper for com.smartgwt.client.widgets.layout.SectionStack that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableSectionStack extends SectionStack implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "simpleClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableSectionStack(String locatorId) {
        super();
        this.locatorId = locatorId;
        SeleniumUtility.setID(this, locatorId);
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String extendLocatorId(String extension) {
        return this.locatorId + "-" + extension;
    }

}
