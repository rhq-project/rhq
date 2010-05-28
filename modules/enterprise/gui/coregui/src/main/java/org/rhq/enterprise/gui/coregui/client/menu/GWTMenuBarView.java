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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;

/**
 *
 * This is a backup menu bar that is implemented with the standard gwt component.
 * It doesn't have many features or look very good.
 *
 * @author Greg Hinkle
 */
public class GWTMenuBarView extends VLayout {

    private AboutModalWindow aboutModalWindow;

    public static final String[] SECTIONS = {"Dashboard", "Demo", "Inventory", "Bundles", "Administration"};

    private String selected = "Dashboard";

    private HTMLFlow linksPane;

    public GWTMenuBarView() {
        super(5);
        setHeight(50);
        setWidth100();


    }

    public static class LinkCommand implements Command {
        private String href;

        public LinkCommand(String href) {
            this.href = href;
        }

        public void execute() {
            History.newItem(href);
        }
    }


    protected void onDraw() {
        super.onDraw();


        // Create a command that will execute on menu item selection
        Command menuCommand = new Command() {
            public void execute() {
                SC.say("Clicked");
            }
        };

        // Create a menu bar
        MenuBar menu = new MenuBar();
        menu.setAutoOpen(true);
        menu.setWidth("100%");
        menu.setAnimationEnabled(false);
        menu.setStylePrimaryName("gwtMenuBar");


        MenuBar subsystemsMenu = new MenuBar(true);
        subsystemsMenu.setStylePrimaryName("gwtMenu");
        subsystemsMenu.addItem("Configuration Changes", new LinkCommand("#config"));
        subsystemsMenu.addItem("Suspect Metrics", new LinkCommand("#config"));
        subsystemsMenu.addItem("Operations", new LinkCommand("#config"));
        subsystemsMenu.addItem("Alerts", new LinkCommand("#config"));
        subsystemsMenu.addItem("Alert Definitions", new LinkCommand("#config"));

        MenuBar overviewMenu = new MenuBar(true);
        overviewMenu.setStylePrimaryName("gwtMenu");
        overviewMenu.addItem("Subsystem Views", subsystemsMenu);
        overviewMenu.addItem("AutoDiscovery Queue",new LinkCommand("#cofng"));
        overviewMenu.addItem("Dashboard",new LinkCommand("#cofng"));


        MenuBar resourcesMenu = new MenuBar(true);
        resourcesMenu.setStylePrimaryName("gwtMenu");
        resourcesMenu.addItem("All Resources", new LinkCommand("#fsdf"));
        resourcesMenu.addItem("Platforms", new LinkCommand("#fsdf"));
        resourcesMenu.addItem("Servers", new LinkCommand("#fsdf"));
        resourcesMenu.addItem("Services", new LinkCommand("#fsdf"));
        resourcesMenu.addSeparator();
//        resourcesMenu.addItem("Favorite Resources",)

        MenuBar groupsMenu = new MenuBar(true);
        groupsMenu.setStylePrimaryName("gwtMenu");
        groupsMenu.addItem("All Groups", new LinkCommand("#sf"));
        groupsMenu.addItem("Compatible Groups", new LinkCommand("#sf"));
        groupsMenu.addItem("Mixed Groups", new LinkCommand("#sf"));
        groupsMenu.addItem("Group Definitions", new LinkCommand("#sf"));
        groupsMenu.addSeparator();
        groupsMenu.addItem("New Group", new LinkCommand("#sf"));
        groupsMenu.addItem("New Group Definition", new LinkCommand("#sf"));
        groupsMenu.addSeparator();
        groupsMenu.addItem("Favorites", new LinkCommand("#sf"));




        MenuBar helpMenu = new MenuBar(true);
        helpMenu.setStylePrimaryName("gwtMenu");
        helpMenu.addItem("Online Documentation", new LinkCommand("#sdfs"));
            helpMenu.addItem("Open a support case", new LinkCommand("#sdfs"));
        helpMenu.addItem("About", new LinkCommand("#sdfs"));



        menu.addItem(new MenuItem("<img src=\"images/header/rhq_logo_28px.png\"/>", true, new LinkCommand("#about")));
        menu.addItem(new MenuItem("Overview", overviewMenu));
        menu.addItem(new MenuItem("Resources", resourcesMenu));
        menu.addItem(new MenuItem("Groups", groupsMenu));
        menu.addItem(new MenuItem("Help", helpMenu));

        // Return the menu
        menu.ensureDebugId("topMenuBar");


        addMember(menu);
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

            headerString.append("<td class=\"" + styleClass + "\" onclick=\"document.location='#" + section + "'\" >");
            headerString.append(section);
            headerString.append("</td>\n");

            headerString.append("<td style=\"width: 1px;\"><img src=\"images/header/header_bg_line.png\"/></td>");
        }

        headerString.append("</tr></table>");

        return headerString.toString();
    }
}