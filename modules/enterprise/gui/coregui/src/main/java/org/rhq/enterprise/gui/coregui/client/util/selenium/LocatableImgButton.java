package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.ImgButton;

/**
 * Wrapper for com.smartgwt.client.widgets.Img that sets the ID for use with selenium scLocators.
 *
 * @author Simeon Pinder
 */
public class LocatableImgButton extends ImgButton {

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null.
     * @param img not null or empty.
     */
    public LocatableImgButton(String locatorId) {
        SeleniumUtility.setID(this, locatorId);
    }

}
