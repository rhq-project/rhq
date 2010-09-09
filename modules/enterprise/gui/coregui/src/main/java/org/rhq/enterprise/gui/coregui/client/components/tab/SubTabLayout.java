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
    private ToolStrip buttonBar;

    private Map<String, SubTab> subtabs = new LinkedHashMap<String, SubTab>();
    private Map<String, Button> subTabButtons = new HashMap<String, Button>();
    private Set<String> disabledSubTabs = new HashSet<String>();

    private SubTab currentlyDisplayed;
    private String currentlySelected;
    private int currentIndex;

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

        int i = 0;

        for (final String locatorId : subtabs.keySet()) {

            SubTab subTab = subtabs.get(locatorId);

            if (currentlySelected == null) {
                // currentlyDisplayed = subTab;
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

            final Integer index = i++;

            button.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    currentlySelected = locatorId;
                    currentIndex = index;
                    fireSubTabSelection();
                    draw(subtabs.get(locatorId));
                }
            });

            subTabButtons.put(locatorId, button);

            buttonBar.addMember(button);
        }

        // Initial settings
        selectTabByLocatorId(currentlySelected);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        selectTabByLocatorId(currentlySelected);
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
        subtabs.put(locatorId, subTab);
        if (isDrawn() && locatorId.equals(currentlySelected)) {
            draw(subTab);
        }
    }

    private void draw(SubTab subTab) {
        if (currentlyDisplayed != null && currentlyDisplayed.getCanvas() != subTab.getCanvas()) {
            try {
                currentlyDisplayed.getCanvas().hide();
            } catch (Exception e) {
                // ignore this
            }
        }

        Canvas canvas = subTab.getCanvas();
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
            currentlyDisplayed = subTab;
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

    public int getCurrentIndex() {
        return currentIndex;
    }

    public String getCurrentTitle() {
        return subtabs.get(currentlySelected).getTitle();
    }

    public SubTab getDefaultSubTab() {
        return subtabs.values().iterator().next();
    }

    public boolean selectTabByLocatorId(String locatorId) {
        boolean foundTab = false;
        this.currentlySelected = locatorId;
        int i = 0;
        for (String subtabLocatorId : this.subtabs.keySet()) {
            if (subtabLocatorId.equals(locatorId)) {
                SubTab subtab = this.subtabs.get(subtabLocatorId);
                if (this.disabledSubTabs.contains(subtabLocatorId)) {
                    // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                    CoreGUI.getErrorHandler().handleError("Cannot select disabled subtab '" + subtab.getTitle() + "'.");
                    break;
                }
                this.currentIndex = i;
                foundTab = true;
                break;
            }
            i++;
        }

        if (foundTab && isDrawn()) {
            Button button = (Button) this.buttonBar.getMember(this.currentIndex);
            button.select();
            draw(this.subtabs.get(locatorId));
        }

        return foundTab;
    }

    public boolean selectTab(String title) {
        boolean foundTab = false;
        int i = 0;
        for (String subtabLocatorId : this.subtabs.keySet()) {
            SubTab subtab = this.subtabs.get(subtabLocatorId);
            if (subtab.getTitle().equals(title)) {
                if (this.disabledSubTabs.contains(subtab.getLocatorId())) {
                    // Nice try - user tried to select a disabled tab, probably by going directly to a bookmark URL.
                    CoreGUI.getErrorHandler().handleError("Cannot select disabled subtab '" + title + "'.");
                    break;
                }
                this.currentlySelected = subtab.getLocatorId();
                this.currentIndex = i;
                foundTab = true;
                break;
            }
            i++;
        }

        if (foundTab && isDrawn()) {
            Button button = (Button) buttonBar.getMember(currentIndex);
            button.select();
            draw(subtabs.get(currentlySelected));
        }

        return foundTab;
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
        TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent("?", getCurrentTitle(), -1, currentIndex,
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
}