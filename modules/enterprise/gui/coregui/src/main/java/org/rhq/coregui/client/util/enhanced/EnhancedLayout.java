package org.rhq.coregui.client.util.enhanced;

import com.smartgwt.client.widgets.layout.Layout;

/**
 * Wrapper for a SmartGWT {@link Layout} that adds some destroy logic.
 * 
 * TODO: Ensure this added destroy logic is necessary now that Selenium support has been removed.
 * 
 * @author Jay Shaughnessy
 */
public class EnhancedLayout extends Layout implements Enhanced {

    public EnhancedLayout() {
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
