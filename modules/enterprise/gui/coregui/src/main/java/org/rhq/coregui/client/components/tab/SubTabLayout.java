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
package org.rhq.coregui.client.components.tab;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Greg Hinkle
 */
public class SubTabLayout extends EnhancedVLayout {

    /** maps subTab IDs to SubTabs. Unlike names, IDs are qualified by the Tab and therefore unique. */
    private Map<String, SubTab> subTabs = new LinkedHashMap<String, SubTab>();
    /** IDs of subTabs that are disabled. Unlike names, IDs are qualified by the Tab and therefore unique. */
    private Set<String> disabledSubTabs = new HashSet<String>();

    private SubTab currentlyDisplayed;
    private String currentlySelected;
    private ToolStrip buttonBar;

    private SubTab head;

    private SubTab tail;

    public SubTabLayout() {
        super();
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();
//        setMargin(0);
//        setPadding(0);

        buttonBar = new EnhancedToolStrip();
        buttonBar.setWidth100();
        buttonBar.setMembersMargin(30);
        buttonBar.setStyleName("subtabbar");

        addMember(buttonBar);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (null == currentlySelected) {
            SubTab initial = getDefaultSubTab();
            if (null != initial) {
                currentlySelected = initial.getId();
            }
        }
        if (null != currentlySelected) {
            selectSubTabById(currentlySelected, true);
        }
    }

    /**
     * Make subTab visible.
     *
     * @param subTab not null
     */
    public void showSubTab(SubTab subTab) {
        Button button = subTab.getButton();
        if (null == button) {
            button = createSubTabButton(subTab);

            if (head == null && tail == null) {
                head = subTab;
                tail = subTab;
                buttonBar.addMember(button);
            } else {
                SubTab successor = findClosestVisibleSuccessor(subTab);
                // if successor is null then that means we are updating the tail
                if (successor == null) {
                    tail.setVisibleNext(subTab);
                    subTab.setVisibleNext(null);
                    tail = subTab;
                    buttonBar.addMember(button);
                } else {
                    SubTab previous = findClosestVisiblePredecessor(successor);
                    // if previous is null then that means we are updating the head
                    if (previous == null) {
                        subTab.setVisibleNext(head);
                        head = subTab;
                        buttonBar.addMember(button, 0);
                    } else {
                        subTab.setVisibleNext(previous.getVisibleNext());
                        previous.setVisibleNext(subTab);
                        buttonBar.addMember(button, buttonBar.getMemberNumber(previous.getButton().getID()) + 1);
                    }
                }
            }
            subTab.setButton(button);
        }
        button.show();
    }

    /**
     * Make subTab not visible. Keeps any associated Canvas.
     *
     * @param subTab not null
     */
    public void hideSubTab(SubTab subTab) {
        SubTab previous = findClosestVisiblePredecessor(subTab);
        if (previous == null) {
            head = subTab.getVisibleNext();
        } else {
            previous.setVisibleNext(subTab.getVisibleNext());
            // check to see if the tail needs to be updated. If the
            // following check is true, then that means visiblePrevious is
            // now the tail.
            if (previous.getVisibleNext() == null) {
                tail = previous;
            }
        }
        subTab.setVisibleNext(null);
        subTab.destroyButton();
    }

    public boolean isSubTabVisible(SubTab subTab) {
        return (null != subTab && null != subTab.getButton());
    }

    private Button createSubTabButton(final SubTab subTab) {
        Button button = new Button(subTab.getTitle());
        button.setShowRollOver(false);
        button.setActionType(SelectionType.RADIO);
        button.setRadioGroup("subTabs");
        button.setBorder(null);
        button.setAutoFit(true);

        button.setBaseStyle("SubTabButton");

        //            button.setStyleName("SubTabButton");
        //            button.setStylePrimaryName("SubTabButton");

        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SubTabLayout.this.currentlySelected = subTab.getId();
                fireSubTabSelection();
                markForRedraw();
            }
        });

        return (button);
    }

    /**
     * Ignored if not visible. Otherwise, enabled.
     */
    public void enableSubTab(SubTab subTab) {
        if (isSubTabVisible(subTab)) {
            disabledSubTabs.remove(subTab.getId());
            subTab.getButton().enable();
            subTab.getButton().show();
        }
    }

    /**
     * Ignored if not visible. Otherwise, disabled.
     */
    public void disableSubTab(SubTab subTab) {
        if (isSubTabVisible(subTab)) {
            disabledSubTabs.add(subTab.getId());
            subTab.getButton().disable();
            subTab.getButton().show();
        }
    }

    /**
     * @return true if not visible or disabled
     */
    public boolean isSubTabDisabled(SubTab subTab) {
        return (!isSubTabVisible(subTab) || subTab.getButton().getDisabled());
    }

    public void registerSubTabs(SubTab... tabs) {
        for (SubTab subTab : tabs) {
            String id = subTab.getId();
            subTabs.put(id, subTab);
        }
        buildSubTabList(tabs);
    }

    private void buildSubTabList(SubTab[] tabs) {
        //        head = tabs[0];
        //        tail = tabs[tabs.length - 1];
        SubTab current = tabs[0];
        for (int i = 1; i < tabs.length; ++i) {
            current.setActualNext(tabs[i]);
            //current.setVisibleNext(tabs[i]);
            current = tabs[i];
        }
    }

    /**
     * Walks the list of visible tabs to find the closest, visible predecessor. Because it
     * is assumed that the tab argument is visible, one of the following must hold:
     * <ul>
     *   <li>The tab is the head so it has no predecessor or</li>
     *   <li>head is null which means the list has not yet been initialized or</li>
     *   <li>A predecessor is reached which may be the tail</li>
     * </ul>
     *
     * @param tab A {@link SubTab tab} that is currently visible
     * @return The closest, visible predecessor or null if the tab is the head or if the
     * list is not fully initialized. i.e., head and tail are null
     */
    private SubTab findClosestVisiblePredecessor(SubTab tab) {
        // head could be null if the list is not yet initialized. The head and
        // tail are null until a node is added to the list.
        if (tab == head || head == null) {
            return null;
        }

        SubTab current = head;
        while (current != tab) {
            // if we have reached the visible tail or the immediate predecessor
            // of the tab, then return it.
            if (current.getVisibleNext() == null || current.getVisibleNext() == tab) {
                return current;
            }
            current = current.getVisibleNext();
        }
        // Not sure that we should ever reach the following statement.
        return null;
    }

    /**
     * Walks the list to find the closest, visible successor. It is assumed that the tab
     * argument is not visible.
     *
     * @param tab A {@link SubTab tab} that is currently hidden
     * @return The closest, visisble successor or null if the insertion point
     * is the tail.
     */
    private SubTab findClosestVisibleSuccessor(SubTab tab) {
        SubTab current = tab;
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

    public SubTab getDefaultSubTab() {
        // the default subTab is the first one in the set that is visible and not disabled
        for (SubTab subTab : this.subTabs.values()) {
            if (!isSubTabDisabled(subTab)) {
                return subTab;
            }
        }
        return null;
    }

    /**
     * @param subtab the subtab to select
     * @param showCanvas if true then ensure the subtab canvas is shown. Otherwise the state is unchanged.
     * @return true if selected successfully, otherwise false
     */
    public boolean selectSubTab(SubTab subTab, boolean showCanvas) {
        if (subTab == null) {
            throw new IllegalArgumentException("subTab is null.");
        }
        return selectSubTabById(subTab.getId(), showCanvas);
    }

    /**
     * @param Id the subtab Id
     * @param showCanvas if true then ensure the subtab canvas is shown. Otherwise the state is unchanged.
     * @return true if selected successfully, otherwise false
     */
    public boolean selectSubTabById(String id, boolean showCanvas) {
        boolean foundTab = false;
        for (String subTabId : this.subTabs.keySet()) {
            if (subTabId.equals(id)) {
                if (this.disabledSubTabs.contains(subTabId)) {
                    // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                    SubTab subTab = this.subTabs.get(subTabId);
                    CoreGUI.getErrorHandler().handleError(MSG.view_subTab_error_disabled(subTab.getTitle()));
                } else {
                    this.currentlySelected = subTabId;
                    foundTab = true;
                }
                break;
            }
        }

        if (foundTab) {
            setCurrentlySelected(showCanvas);
        }

        return foundTab;
    }

    public SubTab getSubTabByName(String name) {
        for (String subTabId : this.subTabs.keySet()) {
            SubTab subTab = this.subTabs.get(subTabId);
            if (subTab.getName().equals(name)) {
                return subTab;
            }
        }

        return null;
    }

    public SubTab getSubTabById(String id) {
        for (String subTabId : this.subTabs.keySet()) {
            if (subTabId.equals(id)) {
                return this.subTabs.get(subTabId);
            }
        }

        return null;
    }

    /**
     * @param name subtab name
     * @param showCanvas if true then ensure the subtab canvas is shown. Otherwise the state is unchanged.
     * @return true if selected successfully, otherwise false
     */
    public boolean selectSubTabByName(String name, boolean showCanvas) {
        SubTab subTab = getSubTabByName(name);
        if (subTab == null) {
            return false;
        } else {
            if (this.disabledSubTabs.contains(subTab.getId())) {
                // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                CoreGUI.getErrorHandler().handleError(MSG.view_subTab_error_disabled(subTab.getTitle()));
                return false;
            }
            this.currentlySelected = subTab.getId();
            setCurrentlySelected(showCanvas);
            return true;
        }
    }

    public SubTab getCurrentSubTab() {
        if (null == currentlySelected) {
            SubTab current = getDefaultSubTab();
            if (null != current) {
                currentlySelected = current.getId();
            }
            return current;
        }

        return this.subTabs.get(this.currentlySelected);
    }

    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager hm = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return hm.addHandler(TwoLevelTabSelectedEvent.TYPE, handler);
    }

    public void fireSubTabSelection() {
        TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent("?", getCurrentSubTab().getName(), -1,
            getCurrentCanvas(), History.getToken());
        hm.fireEvent(event);
    }

    public Canvas getCurrentCanvas() {
        return currentlyDisplayed != null ? currentlyDisplayed.getCanvas() : subTabs.get(currentlySelected).getCanvas();
    }

    /**
     * Destroy all the currently held views so that they can be replaced with new versions
     */
    void destroyViews() {
        for (SubTab subTab : subTabs.values()) {
            subTab.destroyCanvas();
        }
        this.currentlyDisplayed = null;
    }

    private void setCurrentlySelected(boolean showCanvas) {
        if (null != this.currentlySelected) {
            Button button = this.subTabs.get(this.currentlySelected).getButton();
            button.select();

            SubTab currentSubTab = this.subTabs.get(this.currentlySelected);

            if (this.currentlyDisplayed != null && this.currentlyDisplayed.getCanvas() != currentSubTab.getCanvas()) {
                try {
                    this.currentlyDisplayed.getCanvas().hide();
                } catch (Exception e) {
                    // ignore this
                }
            }

            Canvas canvas = currentSubTab.getCanvas();
            if (canvas != null) {
                if (hasMember(canvas)) {
                    if (!canvas.isVisible() && showCanvas) {
                        canvas.show();
                    }
                } else {
                    if (!canvas.isCreated()) {
                        canvas.setOverflow(Overflow.SCROLL);
                    }
                    addMember(canvas);
                    if (!canvas.isVisible() && showCanvas) {
                        canvas.show();
                    }
                }
                markForRedraw();
                this.currentlyDisplayed = currentSubTab;
            }
        }
    }
}
