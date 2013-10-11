package org.rhq.coregui.client.util.enhanced;

import com.smartgwt.client.widgets.IButton;

/**
 * @author Jay Shaughnessy
 */
public class EnhancedIButton extends IButton {

    public EnhancedIButton() {
        this(null);
    }

    public EnhancedIButton(String title) {
        super(title);
        init();
    }

    private void init() {
        String title = getTitle();
        if (title != null && title.length() > 15) {
            setAutoFit(true);
        }
    }
}
