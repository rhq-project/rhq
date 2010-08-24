package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Wrapper for com.smartgwt.client.widgets.layout.VLayout that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableVLayout extends VLayout {

    /** 
     * <pre>
     * ID Format: "scClassname-id"
     * </pre>
     * @param id not null or empty.
     */
    public LocatableVLayout(String id) {
        super();
        String locatorId = this.getScClassName() + "-" + id;
        //        setID(SeleniumUtility.getSafeId(locatorId, locatorId));
    }

}
