package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.form.DynamicForm;

/**
 * Wrapper for com.smartgwt.client.widgets.form.DynamicForm that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableDynamicForm extends DynamicForm implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "scClassname-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableDynamicForm(String locatorId) {
        super();
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
