package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Wrapper for a SmartGWT {@link HLayout} that sets the ID for use with Selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class EnhancedHLayout extends HLayout implements Locatable {

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
        SeleniumUtility.destroyMembers(this);
    }

    @Override
    public void destroy() {
        destroyMembers();
        super.destroy();
    }

}
