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
package org.rhq.enterprise.gui.coregui.client.components;

import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import java.util.LinkedHashMap;

/**
 * @author Greg Hinkle
 */
public class SubTabLayout extends VLayout {

    ToolStrip buttonBar;

    LinkedHashMap<String, Canvas> subtabs = new LinkedHashMap<String, Canvas>();

    Canvas currentlyDisplayed;

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


        for (final String title : subtabs.keySet()) {
            if (currentlyDisplayed == null) {
                currentlyDisplayed = subtabs.get(title);
            }


            Button button = new Button(title);
            button.setShowRollOver(false);
            button.setActionType(SelectionType.RADIO);
            button.setRadioGroup("subtabs");
            button.setBorder(null);

            button.setBaseStyle("SubTabButton");
//            button.setStyleName("SubTabButton");
//            button.setStylePrimaryName("SubTabButton");

            button.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    if (currentlyDisplayed != null) {
                        removeMember(currentlyDisplayed);
                    }
                    currentlyDisplayed = subtabs.get(title);
                    addMember(currentlyDisplayed);
                    markForRedraw();
                }
            });

            buttonBar.addMember(button);

        }
        addMember(buttonBar);

        // Initial settings
        Button b = (Button) buttonBar.getMember(0);
        b.select();
        addMember(currentlyDisplayed);
    }


    public void registerSubTab(String title, Canvas canvas) {
        subtabs.put(title, canvas);
    }
}
