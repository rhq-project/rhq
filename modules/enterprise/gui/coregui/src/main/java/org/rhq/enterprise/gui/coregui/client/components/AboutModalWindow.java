/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * The "About RHQ" modal window.
 *
 * @author Ian Springer
 */
public class AboutModalWindow extends Window {
    private static final String TITLE = "About RHQ";

    public AboutModalWindow() {
        setTitle(TITLE);
        setWidth(300);
        setHeight(240);
        setOverflow(Overflow.VISIBLE);
        setShowMinimizeButton(false);
        setIsModal(true);
        setShowModalMask(true);
        setCanDragResize(false);
        setCanDragReposition(false);
        setAlign(VerticalAlignment.TOP);
        centerInPage();
    }

    @Override
    protected void onInit() {
        super.onInit();
        
        VLayout contentPane = new VLayout();
        contentPane.setPadding(15);
        contentPane.setMembersMargin(25);
        
        HTMLFlow htmlFlow = new HTMLFlow();
        String html =
                 "<span class=\"DisplaySubhead\">\n" +
                 "  <a href=\"http://rhq-project.org/\" title=\"RHQ Homepage\" target=\"_blank\">RHQ</a>\n" +
                 "</span><br/>\n" +
                 "<span class=\"DisplayLabel\">Version: 3.0.0-SNAPSHOT</span><br/>\n" +
                 "<span class=\"DisplayLabel\">Build Number: 0</span>\n" +
                 "<p><a href=\"http://jboss.org/\" title=\"JBoss Homepage\">\n" +
                 "  <img height=\"55\" alt=\"JBoss by Red Hat\" src=\"/images/jboss_logo.png\">\n" +
                 "</a></p>\n" +
                 "<div style=\"top-margin: 10px\">All rights reserved.</div>\n";
        htmlFlow.setContents(html);
        contentPane.addMember(htmlFlow);

        HLayout bottomPanel = new HLayout();
        contentPane.addMember(bottomPanel);
        Canvas spacer = new Canvas();
        spacer.setWidth("*");
        bottomPanel.addMember(spacer);
        Button closeButton = new Button("Close");
        closeButton.setShowRollOver(true);
        closeButton.setShowDown(true);
        closeButton.setWidth("60");
        closeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                hide();
            }
        });
        bottomPanel.addMember(closeButton);

        // NOTE: Since this is a subclass of Window, we MUST use addItem(), rather than addMember() from the
        //       Layout class.
        addItem(contentPane);        
    }
}
