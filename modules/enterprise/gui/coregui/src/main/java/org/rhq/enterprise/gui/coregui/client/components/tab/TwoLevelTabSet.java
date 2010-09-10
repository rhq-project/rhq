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

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;

/**
 * A tab set where each {@link TwoLevelTab tab} has one or more {@link SubTab subtab}s.
 *
 * @author Greg Hinkle
 */
public class TwoLevelTabSet extends LocatableTabSet implements TabSelectedHandler, TwoLevelTabSelectedHandler {

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

    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager m = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return m.addHandler(TwoLevelTabSelectedEvent.TYPE, handler);
    }

    public void onTabSelected(TabSelectedEvent tabSelectedEvent) {
        System.out.println("TwoLevelTabSet.onTabSelected(): " + tabSelectedEvent.getTab().getTitle());
        TwoLevelTab tab = (TwoLevelTab) getSelectedTab();
        TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent(tab.getTitle(),
            tab.getLayout().getCurrentSubTab().getTitle(), tabSelectedEvent.getTabNum(),
                tab.getLayout().getCurrentIndex(), tab.getLayout().getCurrentCanvas());
        m.fireEvent(event);
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        System.out.println("TwoLevelTabSet.onTwoLevelTabSelected(): " + tabSelectedEvent.getId() + "/" +
                tabSelectedEvent.getSubTabId());
        tabSelectedEvent.setTabNum(getSelectedTabNumber());
        Tab tab = getSelectedTab();
        tabSelectedEvent.setId(tab.getTitle());
        m.fireEvent(tabSelectedEvent);
    }

    public TwoLevelTab getTabByTitle(String title) {
        return (TwoLevelTab) super.getTabByTitle(title);
    }

    public TwoLevelTab getTabByLocatorId(String locatorId) {
        return (TwoLevelTab) super.getTabByLocatorId(locatorId);
    }
}
