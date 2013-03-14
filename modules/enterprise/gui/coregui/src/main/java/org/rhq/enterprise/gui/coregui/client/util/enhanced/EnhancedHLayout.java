package org.rhq.enterprise.gui.coregui.client.util.enhanced;

import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Wrapper for a SmartGWT {@link HLayout} that adds some destroy logic.
 * 
 * TODO: Ensure this added destroy logic is necessary now that Selenium support has been removed.
 * 
 * @author Jay Shaughnessy
 */
public class EnhancedHLayout extends HLayout implements Enhanced {

    public EnhancedHLayout() {
        super();
    }

    /** 
     * @param membersMargin
     */
    public EnhancedHLayout(int membersMargin) {
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
