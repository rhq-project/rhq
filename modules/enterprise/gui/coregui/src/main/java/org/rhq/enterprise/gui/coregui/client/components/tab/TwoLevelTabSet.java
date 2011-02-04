/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.components.tab;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

/**
 * A tab set where each {@link TwoLevelTab tab} has one or more {@link SubTab subtab}s.
 *
 * @author Greg Hinkle
 */
public class TwoLevelTabSet extends NamedTabSet implements TabSelectedHandler, TwoLevelTabSelectedHandler {

    /** maps Tab locator IDs to Tabs. */
    private Map<String, TwoLevelTab> hiddenTabs = new LinkedHashMap<String, TwoLevelTab>();

    public TwoLevelTabSet(String locatorId) {
        super(locatorId);
    }

    public void setTabs(TwoLevelTab... tabs) {
        super.setTabs(tabs);
        for (TwoLevelTab tab : tabs) {
            tab.getLayout().addTwoLevelTabSelectedHandler(this);
            updateTab(tab, tab.getPane());
        }

        addTabSelectedHandler(this);
    }

    public TwoLevelTab[] getTabs() {
        Tab[] tabs = super.getTabs();
        TwoLevelTab[] twoLevelTabs = new TwoLevelTab[tabs.length];
        for (int i = 0, tabsLength = tabs.length; i < tabsLength; i++) {
            Tab tab = tabs[i];
            if (!(tab instanceof TwoLevelTab)) {
                throw new IllegalStateException("TwoLevelTabSet contains a Tab that is not a TwoLevelTab.");
            }
            twoLevelTabs[i] = (TwoLevelTab) tab;
        }
        return twoLevelTabs;
    }

    // Smartgwt does not currently offer the ability to hide a Tab (why!) so we fake it here.  This allows us to keep
    // the Tab structure in place while removing it from the TabSet
    public void setTabHidden(TwoLevelTab tab, boolean hidden) {
        if (hidden) {
            if (hiddenTabs.containsKey(tab.getLocatorId())) {
                return;
            }

            // don't let the removeTab() destroy the pane
            Canvas contentPane = tab.getPane();
            updateTab(tab, null);
            removeTab(tab);
            // Reset the pane on the tab, since the call to updateTab() above nulled it out.
            tab.setPane(contentPane);

            hiddenTabs.put(tab.getLocatorId(), tab);
        } else {
            if (!hiddenTabs.containsKey(tab.getLocatorId())) {
                return;
            }

            hiddenTabs.remove(tab.getLocatorId());
            addTab(tab);
        }
    }

    public void destroyViews() {
        for (TwoLevelTab tab : getTabs()) {
            tab.getLayout().destroyViews();
        }
        for (TwoLevelTab tab : hiddenTabs.values()) {
            tab.getLayout().destroyViews();
        }
    }

    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager m = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return m.addHandler(TwoLevelTabSelectedEvent.TYPE, handler);
    }

    public void onTabSelected(TabSelectedEvent tabSelectedEvent) {
        TwoLevelTab tab = (TwoLevelTab) getSelectedTab();
        SubTab currentSubTab = tab.getLayout().getCurrentSubTab();
        if (null != currentSubTab) {
            TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent(tab.getName(), tab.getLayout()
                .getCurrentSubTab().getName(), tabSelectedEvent.getTabNum(), tab.getLayout().getCurrentCanvas());
            m.fireEvent(event);
        }
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        tabSelectedEvent.setTabNum(getSelectedTabNumber());
        Tab tab = getSelectedTab();
        tabSelectedEvent.setId(this.getTabByTitle(tab.getTitle()).getName());
        m.fireEvent(tabSelectedEvent);
    }

    public TwoLevelTab getDefaultTab() {
        TwoLevelTab[] tabs = getTabs();
        for (TwoLevelTab tab : tabs) {
            if (!tab.getDisabled()) {
                return tab;
            }
        }
        return null;
    }

    public TwoLevelTab getTabByName(String name) {
        return (TwoLevelTab) super.getTabByName(name);
    }

    public TwoLevelTab getTabByTitle(String title) {
        return (TwoLevelTab) super.getTabByTitle(title);
    }

    public TwoLevelTab getTabByLocatorId(String locatorId) {
        return (TwoLevelTab) super.getTabByLocatorId(locatorId);
    }

    public void setTabEnabled(TwoLevelTab tab, boolean enabled) {
        if (enabled) {
            enableTab(tab);
        } else {
            disableTab(tab);
        }
    }

    @Override
    public void destroy() {
        // add the hidden tabs back under the TabSet. This will get them destroyed by smartgwt when the tabset
        // goes away. There is no explicit Tab.destroy().
        for (TwoLevelTab tab : hiddenTabs.values()) {
            addTab(tab);
        }
        for (TwoLevelTab tab : getTabs()) {
            tab.getLayout().destroyViews();
        }
        super.destroy();
    }
}
