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
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

/**
 * @author Greg Hinkle
 */
public class TwoLevelTabSet extends TabSet implements TabSelectedHandler, TwoLevelTabSelectedHandler {

    public void setTabs(TwoLevelTab... tabs) {
        super.setTabs(tabs);
        for (TwoLevelTab tab : tabs) {

            tab.getLayout().addTwoLevelTabSelectedHandler(this);

            updateTab(tab,tab.getPane());
        }

        addTabSelectedHandler(this);
    }


    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager m = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return m.addHandler(TwoLevelTabSelectedEvent.TYPE,handler);
    }

    public void onTabSelected(TabSelectedEvent tabSelectedEvent) {

        TwoLevelTab tab = (TwoLevelTab) getSelectedTab();

        TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent(
                getSelectedTab().getTitle(),
                tab.getLayout().currentlySelected,
                tabSelectedEvent.getTabNum(),
                tab.getLayout().getCurrentIndex(),
                tab.getLayout().currentlyDisplayed
        );
        m.fireEvent(event);
    }

    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        tabSelectedEvent.setTabNum(getSelectedTabNumber());
        tabSelectedEvent.setId(getSelectedTab().getTitle());

        m.fireEvent(tabSelectedEvent);
    }

    public Tab getTabByTitle(String title) {
        Tab[] tabs = getTabs();
        for (Tab tab : tabs) {
            if (tab.getTitle().equals(title)) {
                return tab;
            }
        }
        return null;
    }
}

