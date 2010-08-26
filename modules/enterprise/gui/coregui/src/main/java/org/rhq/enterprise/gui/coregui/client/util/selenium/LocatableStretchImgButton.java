package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.StretchImgButton;

/**
 * Wrapper for com.smartgwt.client.widgets.StretchImgButton that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableStretchImgButton extends StretchImgButton {

    /** 
     * <pre>
     * ID Format: "scClassname-locatorId"
     * </pre>
     * @param locatorId not null.
     * @param img not null or empty. 
     */
    public LocatableStretchImgButton(String locatorId) {
        super();
        SeleniumUtility.setID(this, locatorId);
    }

}
