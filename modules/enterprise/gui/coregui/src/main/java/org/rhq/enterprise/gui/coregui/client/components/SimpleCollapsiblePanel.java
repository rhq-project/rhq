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

/**
 * TODO: Not Quite Working.  For some reason the icon is not clickable.
 * 
 * @author Greg Hinkle
 */
public class SimpleCollapsiblePanel extends VLayout {

    private Canvas content;
    private String title;

    private boolean expanded = true;

    public SimpleCollapsiblePanel(String title, Canvas content) {
        this.content = content;
        this.title = title;
        setWidth100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        final Button button = new Button(title);
        button.setShowRollOver(false);
        button.setActionType(SelectionType.RADIO);
        //button.setBorder(null);
        button.setAutoFit(true);

        button.setIcon("[skin]/images/SectionHeader/opener_opened.png");
        //button.setBaseStyle("SimpleButton");

        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                expanded = !expanded;
                if (expanded) {
                    button.setIcon("[skin]/images/SectionHeader/opener_opened.png");
                    content.show();
                } else {
                    button.setIcon("[skin]/images/SectionHeader/opener_closed.png");
                    content.hide();
                    setHeight(30);
                }
                getParentElement().markForRedraw();
                markForRedraw();
            }
        });

        addMember(button);
        addMember(content);
    }
}
