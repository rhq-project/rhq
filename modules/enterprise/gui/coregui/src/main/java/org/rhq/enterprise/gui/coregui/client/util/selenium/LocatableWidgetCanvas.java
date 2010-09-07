package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.widgets.WidgetCanvas;

/**
 * Wrapper for com.smartgwt.client.widgets.WidgetCanvas that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableWidgetCanvas extends WidgetCanvas implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "simpleClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableWidgetCanvas(String locatorId, Widget widget) {
        super(widget);
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
