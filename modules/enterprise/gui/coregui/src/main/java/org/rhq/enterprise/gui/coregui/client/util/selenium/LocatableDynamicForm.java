package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.form.DynamicForm;

/**
 * Wrapper for com.smartgwt.client.widgets.form.DynamicForm that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableDynamicForm extends DynamicForm {

    /** 
     * <pre>
     * ID Format: "scClassname-id"
     * </pre>
     * @param id not null or empty.
     */
    public LocatableDynamicForm(String id) {
        super();
        String locatorId = this.getScClassName() + "-" + id;
        setID(SeleniumUtility.getSafeId(locatorId, locatorId));
    }

}
