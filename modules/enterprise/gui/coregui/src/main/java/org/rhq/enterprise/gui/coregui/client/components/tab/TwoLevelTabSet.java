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
import com.google.gwt.user.client.History;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

/**
 * A tab set where each {@link TwoLevelTab tab} has one or more {@link SubTab subtab}s.
 *
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class TwoLevelTabSet extends NamedTabSet implements TabSelectedHandler, TwoLevelTabSelectedHandler {

    private Map<String, TwoLevelTab> hiddenTabs = new LinkedHashMap<String, TwoLevelTab>();

    private boolean ignoreSelectEvents = false;

    private TwoLevelTab head;

    /**
     * This is the visible tail. Because the actual order of tabs is fixed we know that the
     * actual tail will always be the content tab.
     */
    private TwoLevelTab tail;

    public TwoLevelTabSet() {
        super();
        // Need to set destroyPanes property to false so that we do not lose tab
        // content when hiding a tab.
        setDestroyPanes(false);
    }

    public void setTabs(TwoLevelTab... tabs) {
        super.setTabs(tabs);
        for (TwoLevelTab tab : tabs) {
            tab.getLayout().addTwoLevelTabSelectedHandler(this);
            updateTab(tab, tab.getPane());
        }
        buildTabList();
        addTabSelectedHandler(this);
    }

    /**
     * This method initializes the head and tail pointers. Then it initializes the
     * {@link TwoLevelTab#getActualNext actualNext} and {@link TwoLevelTab#getVisibleNext visibleNext}
     * properties of each tab. This list is built so that when hiding and showing tabs, the
     * tab order remains consistent. The order of the list is the same as the order of the
     * tabs passed to {@link #setTabs(TwoLevelTab...)}
     */
    private void buildTabList() {
        TwoLevelTab[] tabs = getTabs();
        head = tabs[0];
        tail = tabs[tabs.length - 1];
        TwoLevelTab current = head;
        for (int i = 1; i < tabs.length; ++i) {
            current.setActualNext(tabs[i]);
            current.setVisibleNext(tabs[i]);
            current = tabs[i];
        }
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
            if (hiddenTabs.containsKey(tab.getName())) {
                return;
            }

            TwoLevelTab visiblePrevious = findClosestVisiblePredecessor(tab);
            if (visiblePrevious == null) {
                // if visiblePrevious is null then that means we are updating
                // then head. Note that as of now (02/21/2012), the visible head,
                // the summary tab, is fixed, so we don't really need to worry
                // about updating the head; however, doing so will make it easier
                // to support things like hiding arbitrary tabs or reordering tabs.
                head = tab.getVisibleNext();

            } else {
                visiblePrevious.setVisibleNext(tab.getVisibleNext());
                // check to see if the tail needs to be updated. If the
                // following check is true, then that means visiblePrevious is
                // now the tail.
                if (visiblePrevious.getVisibleNext() == null) {
                    tail = visiblePrevious;
                }
            }
            tab.setVisibleNext(null);
            // Note that removing the tab does *not* destroy its content pane
            // since we set the destroyPanes property to false in the
            removeTab(tab);
            hiddenTabs.put(tab.getName(), tab);
        } else {
            if (!hiddenTabs.containsKey(tab.getName())) {
                return;
            }

            hiddenTabs.remove(tab.getName());
            TwoLevelTab successor = findClosestVisibleSuccessor(tab);
            if (successor == null) {
                // if successor is null then that means we are updating the tail
                tail.setVisibleNext(tab);
                tail = tab;
                addTab(tab);
            } else {
                TwoLevelTab visiblePrevious = findClosestVisiblePredecessor(successor);
                tab.setVisibleNext(visiblePrevious.getVisibleNext());
                visiblePrevious.setVisibleNext(tab);
                addTab(tab, (getTabNumber(visiblePrevious.getID()) + 1));
            }
        }
    }

    /**
     * Walks the list of tabs to find the closest, visible predecessor.
     *
     * @param tab A {@link TwoLevelTab tab} that is currently visible
     * @return The closest, visible predecessor or null if have the head
     */
    private TwoLevelTab findClosestVisiblePredecessor(TwoLevelTab tab) {
        if (tab == head) {
            return null;
        }

        TwoLevelTab current = head;
        while (current != tab) {
            // if we have reached the visible tail or the immediate predecessor
            // of the tab, then return it.
            if (current.getVisibleNext() == null || current.getVisibleNext() == tab) {
                return current;
            }
            current = current.getVisibleNext();
        }
        // Not sure what we should do if we get here. return null for now
        return null;
    }

    /**
     * Walks the list to find the closest, visible successor.
     *
     * @param tab A {@link TwoLevelTab tab} that is currently hidden
     * @return The closest, visisble successor or null if the insertion point
     * is the tail.
     */
    private TwoLevelTab findClosestVisibleSuccessor(TwoLevelTab tab) {
        TwoLevelTab current = tab;
        while (current != null) {
            // Walk the list of tabs until we reach a visible successor or the tail
            if (current.getVisibleNext() == null && current != tail) {
                current = current.getActualNext();
            } else {
                return current;
            }
        }
        // if we reach this point then that means we will be inserting at the tail
        return null;
    }

    public void destroyViews() {
        for (TwoLevelTab tab : getTabs()) {
            tab.getLayout().destroyViews();
        }
    }

    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager m = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return m.addHandler(TwoLevelTabSelectedEvent.TYPE, handler);
    }

    // This is invoked by smartgwt when the user clicks on a Tab in the TabSet, or TabSet.selectTab() is called. It
    // sets the current SubTab and fires an event to notify AbstractTwoLevelTabSet that a tab/subtab has been selected.
    public void onTabSelected(TabSelectedEvent tabSelectedEvent) {
        // if requested, ignore select tab notifications. smartgwt can generate unwanted notifications
        // while we manipulate the tabset (e.g. when hiding the current tab). We want to manage this at a higher level
        if (isIgnoreSelectEvents()) {
            return;
        }

        TwoLevelTab tab = (TwoLevelTab) getSelectedTab();
        SubTab currentSubTab = tab.getLayout().getCurrentSubTab();
        if (null != currentSubTab) {
            TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent(tab.getName(), tab.getLayout()
                .getCurrentSubTab().getName(), tabSelectedEvent.getTabNum(), tab.getLayout().getCurrentCanvas(),
                History.getToken());
            m.fireEvent(event);
        }
    }

    // This is invoked by an event fired in SubTabLayout when the user clicks a SubTab button. It sets the Tab
    // and fires an event to notify AbstractTwoLevelTabSet that a tab/subtab has been selected.
    public void onTabSelected(TwoLevelTabSelectedEvent tabSelectedEvent) {
        // if requested, ignore select tab notifications. smartgwt can generate unwanted notifications
        // while we manipulate the tabset (e.g. when hiding the current tab). We want to manage this at a higher level
        if (isIgnoreSelectEvents()) {
            return;
        }

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

    public void setTabEnabled(TwoLevelTab tab, boolean enabled) {
        if (enabled) {
            enableTab(tab);
        } else {
            disableTab(tab);
        }
    }

    public boolean isIgnoreSelectEvents() {
        return ignoreSelectEvents;
    }

    public void setIgnoreSelectEvents(boolean ignoreSelectEvents) {
        this.ignoreSelectEvents = ignoreSelectEvents;
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
