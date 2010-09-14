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

import java.util.*;

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

    /** maps subtab locator IDs to SubTabs */
    private Map<String, SubTab> subtabs = new LinkedHashMap<String, SubTab>();
    /** maps subtab locator IDs to subtab Buttons */
    private Map<String, Button> subTabButtons = new LinkedHashMap<String, Button>();
    /** locator IDs of subtabs that are disabled */
    private Set<String> disabledSubTabs = new HashSet<String>();

    private SubTab currentlyDisplayed;
    private String currentlySelected;

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

        ToolStrip buttonBar = new ToolStrip();
        buttonBar.setBackgroundColor("grey");
        buttonBar.setWidth100();
        buttonBar.setBorder(null);
        buttonBar.setMembersMargin(30);

        addMember(buttonBar);

        int i = 0;

        for (final String locatorId : subtabs.keySet()) {

            SubTab subTab = subtabs.get(locatorId);

            if (currentlySelected == null) {
                currentlySelected = locatorId;
            }

            Button button = new LocatableButton(locatorId, subTab.getTitle());
            button.setShowRollOver(false);
            button.setActionType(SelectionType.RADIO);
            button.setRadioGroup("subtabs");
            button.setBorder(null);
            button.setAutoFit(true);
            if (disabledSubTabs.contains(locatorId)) {
                button.disable();
            } else {
                button.enable();
            }

            button.setBaseStyle("SubTabButton");

            //            button.setStyleName("SubTabButton");
            //            button.setStylePrimaryName("SubTabButton");

            button.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    currentlySelected = locatorId;
                    fireSubTabSelection();
                    draw();
                }
            });

            subTabButtons.put(locatorId, button);

            buttonBar.addMember(button);
        }

        // Initial settings
        selectSubTabByLocatorId(currentlySelected);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        selectSubTabByLocatorId(currentlySelected);
    }

    public void enableSubTab(String locatorId) {
        disabledSubTabs.remove(locatorId);
        if (subTabButtons.containsKey(locatorId)) {
            subTabButtons.get(locatorId).enable();
            markForRedraw();
        }
    }

    public void disableSubTab(String locatorId) {
        disabledSubTabs.add(locatorId);
        if (subTabButtons.containsKey(locatorId)) {
            subTabButtons.get(locatorId).disable();
            markForRedraw();
        }
    }

    public void updateSubTab(SubTab subTab) {

        // Destroy old views so they don't leak
        // TODO: You've already leaked because the subtab has already had its canvas replaced.
//        Canvas oldCanvas = subTab.getCanvas();
//        if (oldCanvas != null) {
//            oldCanvas.destroy();
//        }

        String locatorId = subTab.getLocatorId();
        this.subtabs.put(locatorId, subTab);
        if (locatorId.equals(this.currentlySelected)) {
            refresh();
        }
    }

    public void unregisterAllSubTabs() {
        subtabs.clear();
    }

    public void registerSubTab(SubTab subTab) {
        String locatorId = subTab.getLocatorId();

        if (currentlySelected == null) {
            currentlySelected = locatorId;
        }
        subtabs.put(locatorId, subTab);
    }

    public SubTab getDefaultSubTab() {
        // the default subtab is the first one in the set that is not disabled
        for (SubTab subtab : this.subtabs.values()) {
            if (!this.disabledSubTabs.contains(subtab.getLocatorId())) {
                return subtab;
            }
        }
        return null;
    }

    public boolean selectSubTab(SubTab subtab) {
        if (subtab == null) {
            throw new IllegalArgumentException("subtab is null.");
        }
        return selectSubTabByLocatorId(subtab.getLocatorId());
    }

    public boolean selectSubTabByLocatorId(String locatorId) {
        boolean foundTab = false;
        for (String subtabLocatorId : this.subtabs.keySet()) {
            if (subtabLocatorId.equals(locatorId)) {
                if (this.disabledSubTabs.contains(subtabLocatorId)) {
                    // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                    SubTab subtab = this.subtabs.get(subtabLocatorId);
                    CoreGUI.getErrorHandler().handleError("Cannot select disabled subtab '" + subtab.getTitle() + "'.");
                } else {
                    this.currentlySelected = subtabLocatorId;
                    foundTab = true;
                }
                break;
            }
        }

        if (foundTab) {
            refresh();
        }

        return foundTab;
    }

    public SubTab getSubTabByTitle(String title) {
        for (String subtabLocatorId : this.subtabs.keySet()) {
            SubTab subtab = this.subtabs.get(subtabLocatorId);
            if (subtab.getTitle().equals(title)) {
                return subtab;
            }
        }

        return null;
    }

    public boolean selectSubTabByTitle(String title) {
        SubTab subtab = getSubTabByTitle(title);
        if (subtab == null) {
            return false;
        } else {
            if (this.disabledSubTabs.contains(subtab.getLocatorId())) {
                // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                CoreGUI.getErrorHandler().handleError("Cannot select disabled subtab '" + title + "'.");
                return false;
            }
            this.currentlySelected = subtab.getLocatorId();
            refresh();
            return true;
        }
    }

    public SubTab getCurrentSubTab() {
        return this.subtabs.get(this.currentlySelected);
    }

    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager hm = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return hm.addHandler(TwoLevelTabSelectedEvent.TYPE, handler);
    }

    public void fireSubTabSelection() {
        TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent("?", getCurrentSubTab().getTitle(), -1,
                getCurrentCanvas());
        hm.fireEvent(event);
    }

    public Canvas getCurrentCanvas() {
        return currentlyDisplayed != null ? currentlyDisplayed.getCanvas() : subtabs.get(currentlySelected).getCanvas();
    }

    /**
     * Destroy all the currently held views so that they can be replaced with new versions
     */
    public void destroyViews() {
        for (SubTab subtab : subtabs.values()) {
            if (subtab.getCanvas() != null) {
                subtab.getCanvas().destroy();
            }
        }
    }

    private void refresh() {
        if (isDrawn()) {
            Button button = this.subTabButtons.get(this.currentlySelected);
            button.select();

            SubTab currentSubtab = this.subtabs.get(this.currentlySelected);

            if (this.currentlyDisplayed != null && this.currentlyDisplayed.getCanvas() != currentSubtab.getCanvas()) {
                try {
                    this.currentlyDisplayed.getCanvas().hide();
                } catch (Exception e) {
                    // ignore this
                }
            }

            Canvas canvas = currentSubtab.getCanvas();
            if (canvas != null) {
                if (hasMember(canvas)) {
                    if (!canvas.isVisible()) {
                        canvas.show();
                    }
                } else {
                    if (!canvas.isCreated()) {
                        canvas.setOverflow(Overflow.SCROLL);
                    }
                    addMember(canvas);
                }
                markForRedraw();
                this.currentlyDisplayed = currentSubtab;
            }
        }
    }
}