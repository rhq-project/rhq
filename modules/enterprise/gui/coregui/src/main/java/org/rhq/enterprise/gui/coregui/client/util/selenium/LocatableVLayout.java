package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Wrapper for a SmartGWT {@link VLayout} that sets the ID for use with Selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableVLayout extends VLayout implements Locatable {

    public LocatableVLayout() {
        super();
    }

    public LocatableVLayout(int membersMargin) {
        super(membersMargin);
    }

    /** 
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableVLayout(String locatorId) {
        super();
        init(locatorId);
    }

    /** 
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param membersMargin 
     */
    public LocatableVLayout(String locatorId, int membersMargin) {
        super(membersMargin);
        init(locatorId);
    }

    private void init(String locatorId) {
        SeleniumUtility.setID(this, locatorId);
    }

    public String getLocatorId() {
        return "";
    }

    public String extendLocatorId(String extension) {
        return extension;
    }

    public void destroyMembers() {
        SeleniumUtility.destroyMembers(this);
    }

    @Override
    public void destroy() {
        destroyMembers();
        super.destroy();
    }
}
