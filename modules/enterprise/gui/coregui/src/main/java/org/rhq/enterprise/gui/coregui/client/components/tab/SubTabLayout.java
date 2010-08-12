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
import java.util.Set;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 * @author Greg Hinkle
 */
public class SubTabLayout extends VLayout {

    ToolStrip buttonBar;

    LinkedHashMap<String, Canvas> subtabs = new LinkedHashMap<String, Canvas>();
    Set<String> disabledSubTabs = new HashSet<String>();

    Canvas currentlyDisplayed;
    String currentlySelected;
    int currentIndex = 0;

    public SubTabLayout() {
        super();
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

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

        for (final String title : subtabs.keySet()) {

            if (currentlySelected == null) {
                currentlyDisplayed = subtabs.get(title);
                currentlySelected = title;
            }

            Button button = new Button(title);
            button.setShowRollOver(false);
            button.setActionType(SelectionType.RADIO);
            button.setRadioGroup("subtabs");
            button.setBorder(null);
            button.setAutoFit(true);
            if (disabledSubTabs.contains(title)) {
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
                    currentlySelected = title;
                    currentIndex = index;
                    fireSubTabSelection();
                    draw(subtabs.get(title));
                }
            });

            buttonBar.addMember(button);

        }

        // Initial settings
        selectTab(currentlySelected);
    }

    public void enableSubTab(String title) {
        disabledSubTabs.remove(title);
    }

    public void disableSubTab(String title) {
        disabledSubTabs.add(title);
    }

    public void updateSubTab(String title, Canvas canvas) {
        subtabs.put(title, canvas);
        if (isDrawn() && title.equals(currentlySelected)) {
            draw(canvas);
        }

    }

    private void draw(Canvas canvas) {
        if (currentlyDisplayed != null) {
            currentlyDisplayed.hide();
            //            removeMember(currentlyDisplayed);
        }
        if (canvas != null) {
            if (hasMember(canvas)) {
                canvas.show();
            } else {
                if (!canvas.isCreated()) {
                    canvas.setOverflow(Overflow.SCROLL);
                }
                addMember(canvas);
                markForRedraw();
            }
            currentlyDisplayed = canvas;
        }
    }

    public void unregisterAllSubTabs() {
        subtabs.clear();
    }

    public void registerSubTab(String title, Canvas canvas) {
        if (currentlySelected == null) {
            currentlySelected = title;
        }
        subtabs.put(title, canvas);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean selectTab(String title) {
        boolean foundTab = false;
        currentlySelected = title;
        int i = 0;
        for (String sub : subtabs.keySet()) {
            if (sub.equals(title)) {
                currentIndex = i;
                foundTab = true;
                break;
            }
            i++;
        }

        if (isDrawn()) {
            ((Button) buttonBar.getMember(currentIndex)).select();
            draw(subtabs.get(title));
        }

        return foundTab;
    }

    // ------- Event support -------
    // Done with a separate handler manager from parent class on purpose (compatibility issue)

    private HandlerManager hm = new HandlerManager(this);

    public HandlerRegistration addTwoLevelTabSelectedHandler(TwoLevelTabSelectedHandler handler) {
        return hm.addHandler(TwoLevelTabSelectedEvent.TYPE, handler);
    }

    public void fireSubTabSelection() {
        TwoLevelTabSelectedEvent event = new TwoLevelTabSelectedEvent("?", currentlySelected, -1, currentIndex,
            currentlyDisplayed);
        hm.fireEvent(event);
    }
}