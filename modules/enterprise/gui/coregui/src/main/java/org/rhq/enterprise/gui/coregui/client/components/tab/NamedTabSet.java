package org.rhq.enterprise.gui.coregui.client.components.tab;

import com.smartgwt.client.widgets.tab.Tab;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;

/**
 * A Wrapper for org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet allowing for a Set of NamedTabs.
 * 
 * @author Jay Shaughnessy
 */
public class NamedTabSet extends LocatableTabSet {

    public NamedTabSet(String locatorId) {
        super(locatorId);
    }

    public void setTabs(NamedTab... tabs) {
        super.setTabs(tabs);
    }

    public NamedTab[] getTabs() {
        Tab[] tabs = super.getTabs();
        NamedTab[] namedTabs = new NamedTab[tabs.length];
        for (int i = 0, tabsLength = tabs.length; i < tabsLength; i++) {
            Tab tab = tabs[i];
            if (!(tab instanceof NamedTab)) {
                throw new IllegalStateException("NamedTabSet contains a Tab that is not a NamedTab.");
            }
            namedTabs[i] = (NamedTab) tab;
        }
        return namedTabs;
    }

    public NamedTab getTabByName(String name) {
        NamedTab[] tabs = getTabs();
        for (NamedTab tab : tabs) {
            if (tab.getName().equals(name)) {
                return tab;
            }
        }
        return null;
    }

}
