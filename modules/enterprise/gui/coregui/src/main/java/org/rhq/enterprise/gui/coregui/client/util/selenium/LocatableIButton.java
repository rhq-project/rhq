package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.IButton;

/**
 * Wrapper for com.smartgwt.client.widgets.IButton that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableIButton extends IButton {

    /** 
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableIButton(String locatorId, String title) {
        super(title);
        SeleniumUtility.setID(this, locatorId);
    }

}
