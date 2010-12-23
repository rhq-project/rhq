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

import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.bundle.BundleTopView;
import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardsView;
import org.rhq.enterprise.gui.coregui.client.help.HelpView;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.report.ReportTopView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class MenuBarView extends LocatableVLayout {

    public static final ViewName[] SECTIONS = { DashboardsView.VIEW_ID, InventoryView.VIEW_ID, ReportTopView.VIEW_ID,
        BundleTopView.VIEW_ID, AdministrationView.VIEW_ID, HelpView.VIEW_ID };

    private String currentlySelectedSection = DashboardsView.VIEW_ID.getName();
    private LocatableLabel userLabel;

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
        //addMember(new SearchBarPane(this.extendLocatorId("Search")));

        markForRedraw();
    }

    // When redrawing, ensire the correct session infor is displayed
    @Override
    public void markForRedraw() {
        String currentDisplayName = userLabel.getContents();
        String currentUsername = UserSessionManager.getSessionSubject().getName();
        if (!currentUsername.equals(currentDisplayName)) {
            userLabel.setContents(currentUsername);
        }

        super.markForRedraw();
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
        for (ViewName section : SECTIONS) {

            String styleClass = "TopSectionLink";
            if (section.equals(currentlySelectedSection)) {
                styleClass += "Selected";
            }

            // Set explicit identifiers because the generated scLocator is not getting picked up by Selenium.
            headerString.append("<td style=\"vertical-align:middle\" id=\"").append(section).append("\" class=\"")
                .append(styleClass).append("\" onclick=\"document.location='#").append(section).append("'\" >");
            headerString.append(section.getTitle());
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

        userLabel = new LocatableLabel(this.extendLocatorId("User"), UserSessionManager.getSessionSubject().getName());
        userLabel.setAutoWidth();

        LocatableLabel lineLabel = new LocatableLabel(this.extendLocatorId("Line"), " | ");
        lineLabel.setWidth("10px");
        lineLabel.setAlign(Alignment.CENTER);

        Hyperlink logoutLink = SeleniumUtility.setHtmlId(new Hyperlink(MSG.view_menuBar_logout(), "LogOut"));
        logoutLink.setWidth("50px");

        layout.addMember(userLabel);
        layout.addMember(lineLabel);
        layout.addMember(logoutLink);

        return layout;
    }
}
