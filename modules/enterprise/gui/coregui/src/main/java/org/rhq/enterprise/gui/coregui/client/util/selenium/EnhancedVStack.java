package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.VStack;

/**
 * Wrapper for a SmartGWT {@link VStack} that adds some destroy logic.
 * 
 * TODO: Ensure this added destroy logic is necessary now that Selenium support has been removed.
 * 
 * @author Jay Shaughnessy
 */
public class EnhancedVStack extends VStack implements Enhanced {

    public EnhancedVStack() {
        super();
    }

    public EnhancedVStack(int membersMargin) {
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
