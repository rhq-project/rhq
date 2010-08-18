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
     * ID Format: "scClassname-title"
     * </pre>
     * @param id not null or empty.
     */
    public LocatableIButton(String title) {
        super(title);
        String locatorId = this.getScClassName() + "-" + title;
        setID(SeleniumUtility.getSafeId(locatorId, locatorId));
    }

}
