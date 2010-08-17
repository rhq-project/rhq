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
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 */
public class MenuBarView extends VLayout {

    private AboutModalWindow aboutModalWindow;

    public static final String[] SECTIONS = { "Dashboard", "Inventory", "Reports", "Bundles", "Administration" };

    private String selected = "Dashboard";

    private HTMLFlow linksPane;

    protected void onDraw() {
        super.onDraw();

        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
                String first = stringValueChangeEvent.getValue().split("/")[0];

                if ("Resource".equals(first)) {
                    first = "Inventory";
                }

                selected = first;
                linksPane.setContents(setupLinks());
                linksPane.markForRedraw();
            }
        });

        ToolStrip topStrip = new ToolStrip();
        topStrip.setHeight(34);
        topStrip.setWidth100();
        topStrip.setBackgroundImage("header/header_bg.png");
        topStrip.setMembersMargin(20);

        this.aboutModalWindow = new AboutModalWindow();
        Img logo = new Img("header/rhq_logo_28px.png", 80, 28);
        logo.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                MenuBarView.this.aboutModalWindow.show();
            }
        });
        topStrip.addMember(logo);

        linksPane = new HTMLFlow();
        linksPane.setContents(setupLinks());

        topStrip.addMember(linksPane);

        topStrip.addMember(new LayoutSpacer());

        //HLayout helpLayout = new HLayout();
        //        Label loggedInAs = new Label("Logged in as " + CoreGUI.getSessionSubject().getName());
        //        loggedInAs.setWrap(false);
        //        loggedInAs.setValign(VerticalAlignment.CENTER);
        //        helpLayout.addMember(loggedInAs);

        topStrip.addMember(SeleniumUtility.setHtmlId(new Hyperlink("Help", "Help")));
        topStrip.addMember(SeleniumUtility.setHtmlId(new Hyperlink("Preferences", "Preferences")));
        topStrip.addMember(SeleniumUtility.setHtmlId(new Hyperlink("Log Out", "LogOut")));
        //        helpLayout.setLayoutAlign(VerticalAlignment.CENTER);
        //        topStrip.addMember(helpLayout);

        /* DynamicForm links = new DynamicForm();
                links.setNumCols(SECTIONS.length * 2);
                links.setHeight100();

                int i = 0;
                FormItem[] linkItems = new FormItem[SECTIONS.length];
                for (String section : SECTIONS) {
                    LinkItem sectionLink = new LinkItem();
                    sectionLink.setTitle(section);
                    sectionLink.setValue("#" + section);
                    sectionLink.setShowTitle(false);

                    if (section.equals("Demo")) {
                        sectionLink.setCellStyle("TopSectionLinkSelected");
        //                sectionLink.("header/header_bg_selected.png");
                    } else {
                        sectionLink.setCellStyle("TopSectionLink");
        //                widgetCanvas.setStyleName("TopSectionLink");
                    }
                    linkItems[i++] = sectionLink;
                }
                links.setItems(linkItems);

                topStrip.addMember(links);
        */
        addMember(topStrip);
        addMember(new SearchBarPane());

        markForRedraw();
    }

    private String setupLinks() {
        StringBuilder headerString = new StringBuilder(
            "<table style=\"height: 34px;\" cellpadding=\"0\" cellspacing=\"0\"><tr>");

        boolean first = true;
        for (String section : SECTIONS) {
            if (first) {
                headerString.append("<td style=\"width: 1px;\"><img src=\"images/header/header_bg_line.png\"/></td>");
            }
            first = false;

            String styleClass = "TopSectionLink";
            if (section.equals(selected)) {
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

}
