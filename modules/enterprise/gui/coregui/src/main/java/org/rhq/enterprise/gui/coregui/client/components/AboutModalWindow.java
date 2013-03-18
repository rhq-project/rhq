/*
 * RHQ Management Platform
 * Copyright (C) 2010-2011 Red Hat, Inc.
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

import com.smartgwt.client.Version;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The "About RHQ" modal window.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
public class AboutModalWindow extends Window {

    private static final Messages MSG = CoreGUI.getMessages();

    public AboutModalWindow() {
        super();

        setWidth(300);
        setHeight(300);
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
        ProductInfo productInfo = CoreGUI.get().getProductInfo();

        setTitle(MSG.view_aboutBox_title(productInfo.getFullName()));

        EnhancedVLayout contentPane = new EnhancedVLayout();
        contentPane.setPadding(10);

        // TODO (ips, 09/06/11): Convert this raw HTML to SmartGWT widgets.
        HTMLPane htmlPane = new HTMLPane();
        String html = "<span class=\"DisplaySubhead\">\n" + "  <a href=\"" + productInfo.getUrl() + "\" title=\""
            + productInfo.getFullName() + " " + productInfo.getUrl() + "\" target=\"_blank\">"
            + productInfo.getFullName() + "</a>\n" + "</span><br/>\n" + "<span class=\"DisplayLabel\">"
            + MSG.view_aboutBox_version() + " " + productInfo.getVersion() + "</span><br/>\n"
            + "<span class=\"DisplayLabel\">" + MSG.view_aboutBox_buildNumber() + " " + productInfo.getBuildNumber()
            + "</span><p/>\n" + "<span class=\"DisplayLabel\">GWT " + MSG.common_title_version() + ": "
            + MSG.common_buildInfo_gwtVersion() + "</span><br/>\n" + "<span class=\"DisplayLabel\">SmartGWT "
            + MSG.common_title_version() + ": " + Version.getVersion() + "</span><br/>\n"
            + "<p><a href=\"http://jboss.org/\" title=\"JBoss " + MSG.view_aboutBox_homepage() + "\">\n"
            + "  <img height=\"55\" alt=\"" + MSG.view_aboutBox_jbossByRedHat()
            + "\" src=\"/images/jboss_logo.png\">\n" + "</a></p>\n" + "<div style=\"top-margin: 10px\">"
            + MSG.view_aboutBox_allRightsReserved() + "</div>\n";
        htmlPane.setContents(html);
        htmlPane.setHeight(220);
        contentPane.addMember(htmlPane);

        EnhancedHLayout buttonBar = new EnhancedHLayout();
        buttonBar.setHeight(30);
        buttonBar.setAlign(Alignment.RIGHT);

        EnhancedIButton closeButton = new EnhancedIButton(MSG.common_button_close());
        closeButton.setShowRollOver(true);
        closeButton.setShowDown(true);
        closeButton.setWidth(60);
        closeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                hide();
            }
        });
        buttonBar.addMember(closeButton);

        VLayout bottom = new VLayout();
        bottom.setAlign(VerticalAlignment.BOTTOM);
        bottom.addMember(buttonBar);
        contentPane.addMember(bottom);

        // NOTE: Since this is a subclass of Window, we MUST use addItem(), rather than addMember() from the
        //       Layout class.
        addItem(contentPane);
    }

}
