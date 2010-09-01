package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.Img;

/**
 * Wrapper for com.smartgwt.client.widgets.Img that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableImg extends Img {

    /** 
     * <pre>
     * ID Format: "simpleClassname-locatorId"
     * </pre>
     * @param locatorId not null.
     * @param img not null or empty. 
     */
    public LocatableImg(String locatorId, String src, int width, int height) {
        super(src, width, height);
        SeleniumUtility.setID(this, locatorId);
    }

}
