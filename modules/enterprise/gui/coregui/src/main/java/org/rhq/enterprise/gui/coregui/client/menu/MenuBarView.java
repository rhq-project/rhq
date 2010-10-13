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
package org.rhq.enterprise.gui.coregui.client.menu;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Hyperlink;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class MenuBarView extends LocatableVLayout {

    public static final String[] SECTIONS = { "Dashboard", "Inventory", "Reports", "Bundles", "Administration" };

    private String currentlySelectedSection = "Dashboard";

    public MenuBarView(String locatorId) {
        super(locatorId);
    }

    protected void onDraw() {
        super.onDraw();

        ToolStrip topStrip = new ToolStrip();
        topStrip.setHeight(34);
        topStrip.setWidth100();
        topStrip.setBackgroundImage("header/header_bg.png");
        topStrip.setMembersMargin(20);

        topStrip.addMember(getLogoSection());
        topStrip.addMember(getLinksSection());
        topStrip.addMember(getActionsSection());

        addMember(topStrip);
        addMember(new SearchBarPane(this.extendLocatorId("Search")));

        markForRedraw();
    }

    private Canvas getLogoSection() {
        final AboutModalWindow aboutModalWindow = new AboutModalWindow();
        Img logo = new Img("header/rhq_logo_28px.png", 80, 28);
        logo.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                aboutModalWindow.show();
            }
        });
        return logo;
    }

    private Canvas getLinksSection() {
        final HTMLFlow linksPane = new HTMLFlow();
        linksPane.setContents(setupLinks());

        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                String first = stringValueChangeEvent.getValue().split("/")[0];

                if ("Resource".equals(first)) {
                    first = "Inventory";
                }

                currentlySelectedSection = first;
                linksPane.setContents(setupLinks());
                linksPane.markForRedraw();
            }
        });
        return linksPane;
    }

    private String setupLinks() {
        StringBuilder headerString = new StringBuilder(
            "<table style=\"height: 34px;\" cellpadding=\"0\" cellspacing=\"0\"><tr>");

        headerString.append("<td style=\"width: 1px;\"><img src=\"images/header/header_bg_line.png\"/></td>");
        for (String section : SECTIONS) {

            String styleClass = "TopSectionLink";
            if (section.equals(currentlySelectedSection)) {
                styleClass += "Selected";
            }

            // Set explicit identifiers because the generated scLocator is not getting picked up by Selenium.
            headerString.append("<td id=\"" + section + "\" class=\"" + styleClass
                + "\" onclick=\"document.location='#" + section + "'\" >");
            headerString.append(section);
            headerString.append("</td>\n");

            headerString.append("<td style=\"width: 1px;\"><img src=\"images/header/header_bg_line.png\"/></td>");
        }

        headerString.append("</tr></table>");

        return headerString.toString();
    }

    private Canvas getActionsSection() {
        HLayout layout = new HLayout();
        layout.setMargin(10);
        layout.setAlign(Alignment.RIGHT);

        LocatableImg helpImage = new LocatableImg("HelpImage", "[SKIN]/actions/help.png", 16, 16);
        Hyperlink helpLink = SeleniumUtility.setHtmlId(new Hyperlink("Help", "Help"));
        Hyperlink logoutLink = SeleniumUtility.setHtmlId(new Hyperlink("Log Out", "LogOut"));

        layout.addMember(helpImage);
        layout.addMember(helpLink);
        layout.addMember(logoutLink);

        return layout;
    }
}
