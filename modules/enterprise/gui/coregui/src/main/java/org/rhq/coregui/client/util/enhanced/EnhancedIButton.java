package org.rhq.coregui.client.util.enhanced;

import com.smartgwt.client.widgets.IButton;

/**
 * @author Jay Shaughnessy
 */
public class EnhancedIButton extends IButton {
    
    public enum ButtonColor {
        RED, BLUE, GRAY
    }

    public EnhancedIButton() {
        this(null, ButtonColor.GRAY);
    }
    
    public EnhancedIButton(String title) {
        this(title, ButtonColor.GRAY);
    }

    public EnhancedIButton(String title, ButtonColor buttonColor) {
        super(title);
        if (!ButtonColor.GRAY.equals(buttonColor)) {
            setID((ButtonColor.BLUE.equals(buttonColor) ? "primary" : "destructive") + id + title);
        }
        init();
    }

    private void init() {
        String title = getTitle();
        if (title != null && title.length() > 15) {
            setAutoFit(true);
        }
    }
}
