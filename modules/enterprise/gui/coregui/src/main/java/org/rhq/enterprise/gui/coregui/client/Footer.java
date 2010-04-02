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
package org.rhq.enterprise.gui.coregui.client;

import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripSeparator;

import org.rhq.enterprise.gui.coregui.client.footer.FavoritesButton;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenterView;

/**
 * @author Greg Hinkle
 */
public class Footer extends ToolStrip {

    MessageCenterView recentMessage;

    public Footer() {
        super();
        setHeight(30);
        setAlign(VerticalAlignment.CENTER);
//        setPadding(5);
        setWidth100();
        setMembersMargin(15);
    }


    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(new Label("Welcome to RHQ"));
        addMember(new ToolStripSeparator());

        recentMessage = new MessageCenterView();
        recentMessage.setWidth("*");

        addMember(recentMessage);

        addMember(new ToolStripSeparator());

        addMember(new FavoritesButton());

        addMember(new Img("/images/icons/Alert_red_16.png",16,16));
        addMember(new Label("15 recent alerts"));
    }







}
