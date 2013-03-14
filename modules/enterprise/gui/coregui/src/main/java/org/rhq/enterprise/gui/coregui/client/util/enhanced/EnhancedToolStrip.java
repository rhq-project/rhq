package org.rhq.enterprise.gui.coregui.client.util.enhanced;

import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 * Wrapper for a SmartGWT {@link ToolStrip} that adds some destroy logic.
 * 
 * TODO: Ensure this added destroy logic is necessary now that Selenium support has been removed.
 * 
 * @author Jay Shaughnessy
 */
public class EnhancedToolStrip extends ToolStrip implements Enhanced {

    public EnhancedToolStrip() {
        super();
    }

    public void destroyMembers() {
        EnhancedUtility.destroyMembers(this);
    }

    @Override
    public void destroy() {
        destroyMembers();
        super.destroy();
    }
}
