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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class SubTabLayout extends LocatableVLayout {

    /** maps subTab locator IDs to SubTabs. Unlike names, locatorIDs are qualified by the Tab and therefore unique. */
    private Map<String, SubTab> subTabs = new LinkedHashMap<String, SubTab>();
    /** locator IDs of subTabs that are disabled. Unlike names, locator IDs are qualified by the Tab and therefore unique. */
    private Set<String> disabledSubTabs = new HashSet<String>();

    private SubTab currentlyDisplayed;
    private String currentlySelected;
    private ToolStrip buttonBar;

    public SubTabLayout(String locatorId) {
        super(locatorId);
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();
        setMargin(0);
        setPadding(0);

        buttonBar = new ToolStrip();
        buttonBar.setBackgroundColor("grey");
        buttonBar.setWidth100();
        buttonBar.setBorder(null);
        buttonBar.setMembersMargin(30);

        addMember(buttonBar);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (null == currentlySelected) {
            SubTab initial = getDefaultSubTab();
            if (null != initial) {
                currentlySelected = initial.getLocatorId();
            }
        }
        if (null != currentlySelected) {
            selectSubTabByLocatorId(currentlySelected, true);
        }
    }

    /**
     * Make subTab visible. 
     * 
     * @param subTab not null
     */
    public void showSubTab(SubTab subTab) {
        LocatableButton button = subTab.getButton();
        if (null == button) {
            button = createSubTabButton(subTab);
            buttonBar.addMember(button);
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
        subTab.destroyButton();
    }

    public boolean isSubTabVisible(SubTab subTab) {
        return (null != subTab && null != subTab.getButton());
    }

    private LocatableButton createSubTabButton(final SubTab subTab) {
        LocatableButton button = new LocatableButton(subTab.getLocatorId(), subTab.getTitle());
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
                SubTabLayout.this.currentlySelected = subTab.getLocatorId();
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
            disabledSubTabs.remove(subTab.getLocatorId());
            subTab.getButton().enable();
            subTab.getButton().show();
        }
    }

    /**
     * Ignored if not visible. Otherwise, disabled.
     */
    public void disableSubTab(SubTab subTab) {
        if (isSubTabVisible(subTab)) {
            disabledSubTabs.add(subTab.getLocatorId());
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

    public void registerSubTab(SubTab subTab) {
        String locatorId = subTab.getLocatorId();

        subTabs.put(locatorId, subTab);
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
        return selectSubTabByLocatorId(subTab.getLocatorId(), showCanvas);
    }

    /**
     * @param name locatorId the subtab locatorId
     * @param showCanvas if true then ensure the subtab canvas is shown. Otherwise the state is unchanged.
     * @return true if selected successfully, otherwise false
     */
    public boolean selectSubTabByLocatorId(String locatorId, boolean showCanvas) {
        boolean foundTab = false;
        for (String subTabLocatorId : this.subTabs.keySet()) {
            if (subTabLocatorId.equals(locatorId)) {
                if (this.disabledSubTabs.contains(subTabLocatorId)) {
                    // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                    SubTab subTab = this.subTabs.get(subTabLocatorId);
                    CoreGUI.getErrorHandler().handleError(MSG.view_subTab_error_disabled(subTab.getTitle()));
                } else {
                    this.currentlySelected = subTabLocatorId;
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
        for (String subTabLocatorId : this.subTabs.keySet()) {
            SubTab subTab = this.subTabs.get(subTabLocatorId);
            if (subTab.getName().equals(name)) {
                return subTab;
            }
        }

        return null;
    }

    public SubTab getSubTabByLocatorId(String locatorId) {
        for (String subTabLocatorId : this.subTabs.keySet()) {
            if (subTabLocatorId.equals(locatorId)) {
                return this.subTabs.get(subTabLocatorId);
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
            if (this.disabledSubTabs.contains(subTab.getLocatorId())) {
                // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                CoreGUI.getErrorHandler().handleError(MSG.view_subTab_error_disabled(subTab.getTitle()));
                return false;
            }
            this.currentlySelected = subTab.getLocatorId();
            setCurrentlySelected(showCanvas);
            return true;
        }
    }

    public SubTab getCurrentSubTab() {
        if (null == currentlySelected) {
            SubTab current = getDefaultSubTab();
            if (null != current) {
                currentlySelected = current.getLocatorId();
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
            getCurrentCanvas());
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