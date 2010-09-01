package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.events.ClickHandler;

/**
 * Wrapper for com.smartgwt.client.widgets.layout.VLayout that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableHeaderControl extends HeaderControl implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "simpleClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param icon
     */
    public LocatableHeaderControl(String locatorId, HeaderIcon icon) {
        super(icon);
        init(locatorId);
    }

    /** 
     * <pre>
     * ID Format: "simpleClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param icon
     * @param handler
     */
    public LocatableHeaderControl(String locatorId, HeaderIcon icon, ClickHandler handler) {
        super(icon, handler);
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
