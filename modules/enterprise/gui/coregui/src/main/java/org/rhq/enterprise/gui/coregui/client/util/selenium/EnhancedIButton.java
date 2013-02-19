package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.IButton;

/**
 * @author Jay Shaughnessy
 */
public class EnhancedIButton extends IButton {

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public EnhancedIButton() {
        super();
        init();
    }

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param title
     */
    public EnhancedIButton(String title) {
        super(title);
        init();
    }

    private void init() {
        if (getTitle().length() > 15) {
            setAutoFit(true);
        }
    }
}
