package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 * Wrapper for com.smartgwt.client.widgets.toolbar.ToolStrip that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableToolStrip extends ToolStrip implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableToolStrip(String locatorId) {
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

    public void destroyMembers() {
        SeleniumUtility.destroyMembers(this);
    }

    // It was my understanding that Layouts needed explicit removal of members but it seems
    // that ToolStrip may treat its members differently.  The logic below actually caused issues because
    // the member was being destroyed automatically.
    //
    //@Override
    //protected void onDestroy() {
    //    destroyMembers();
    //    super.onDestroy();
    //}

}
