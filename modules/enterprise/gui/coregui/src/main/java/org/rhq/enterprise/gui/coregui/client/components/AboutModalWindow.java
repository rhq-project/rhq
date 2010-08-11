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
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * The "About RHQ" modal window.
 *
 * @author Ian Springer
 */
public class AboutModalWindow extends Window {
    private static final Messages MESSAGES = CoreGUI.getMessages();
    private static final ProductInfo PRODUCT_INFO = CoreGUI.getProductInfo();

    public AboutModalWindow() {
        setTitle(MESSAGES.about_title(PRODUCT_INFO.getFullName()));
        setWidth(300);
        setHeight(255);
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
                 "  <a href=\"" + PRODUCT_INFO.getUrl() + "\" title=\"" + PRODUCT_INFO.getFullName() + " "
                         + MESSAGES.about_homepage() + "\" target=\"_blank\">" + PRODUCT_INFO.getFullName() + "</a>\n" +
                 "</span><br/>\n" +
                 "<span class=\"DisplayLabel\">" + MESSAGES.about_version() + " " + PRODUCT_INFO.getVersion()
                         + "</span><br/>\n" +
                 "<span class=\"DisplayLabel\">" + MESSAGES.about_buildNumber() + " " + PRODUCT_INFO.getBuildNumber()
                         + "</span>\n" +
                 "<p><a href=\"http://jboss.org/\" title=\"JBoss " + MESSAGES.about_homepage() + "\">\n" +
                 "  <img height=\"55\" alt=\"" + MESSAGES.about_jbossByRedHat() + "\" src=\"/images/jboss_logo.png\">\n" +
                 "</a></p>\n" +
                 "<div style=\"top-margin: 10px\">" + MESSAGES.about_allRightsReserved() + "</div>\n";
        htmlFlow.setContents(html);
        contentPane.addMember(htmlFlow);

        HLayout bottomPanel = new HLayout();
        bottomPanel.setAlign(VerticalAlignment.BOTTOM);
        contentPane.addMember(bottomPanel);
        Canvas spacer = new Canvas();
        spacer.setWidth("*");
        bottomPanel.addMember(spacer);
        Button closeButton = new Button(MESSAGES.button_close());
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
