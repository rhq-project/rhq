package org.rhq.coregui.client.util.enhanced;

import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Wrapper for a SmartGWT {@link VLayout} that adds some destroy logic.
 * 
 * TODO: Ensure this added destroy logic is necessary now that Selenium support has been removed.
 * 
 * @author Jay Shaughnessy
 */
public class EnhancedVLayout extends VLayout implements Enhanced {

    public EnhancedVLayout() {
        super();
    }

    public EnhancedVLayout(int membersMargin) {
        super(membersMargin);
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
