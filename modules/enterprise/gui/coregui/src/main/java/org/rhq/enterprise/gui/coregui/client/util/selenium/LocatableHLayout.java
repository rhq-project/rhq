package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Wrapper for com.smartgwt.client.widgets.layout.HLayout that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableHLayout extends HLayout implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "simpleClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableHLayout(String locatorId) {
        super();
        init(locatorId);
    }

    /** 
     * <pre>
     * ID Format: "simpleClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param membersMargin
     */
    public LocatableHLayout(String locatorId, int membersMargin) {
        super(membersMargin);
        init(locatorId);
    }

    private void init(String locatorId) {
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
