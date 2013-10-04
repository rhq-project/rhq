package org.rhq.coregui.client.components.tab;

import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * A Wrapper for {@link TabSet} allowing for a Set of NamedTabs.
 * 
 * @author Jay Shaughnessy
 */
public class NamedTabSet extends TabSet {

    public NamedTabSet() {
        super();
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

    public NamedTab getTabByTitle(String title) {
        NamedTab[] tabs = getTabs();
        for (NamedTab tab : tabs) {
            if (tab.getTitle().equals(title)) {
                return tab;
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        for (NamedTab tab : getTabs()) {
            tab.destroy();
        }
        super.destroy();
    }
}
