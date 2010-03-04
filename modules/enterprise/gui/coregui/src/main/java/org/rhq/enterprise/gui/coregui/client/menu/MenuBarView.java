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

import com.google.gwt.user.client.ui.Hyperlink;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuBar;
import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;

import java.util.LinkedHashMap;

/**
 * @author Greg Hinkle
 */
public class MenuBarView extends HLayout {

    MenuBar menuBar;

    LinkedHashMap<String, Menu> menus = new LinkedHashMap<String, Menu>();

    private AboutModalWindow aboutModalWindow;

    public MenuBarView() {
        setHeight(30);
        setAlign(VerticalAlignment.BOTTOM);
        setAlign(Alignment.LEFT);
/*
        menuBar = new MenuBar();
        menuBar.setBackgroundColor("gray");
        

        Menu icon = new Menu();
        icon.setTitle("iManage");
        icon.addItem(new MenuItem("Dashboard"));
        menuBar.addMenus(new Menu[]{icon},0);

        Menu overview = new Menu();
        overview.setTitle("Applications");
        overview.addItem(new MenuItem("Content Configuration"));
        overview.addItem(new MenuItem("Monitoring"));
        overview.addItem(new MenuItem("Administration"));
        menuBar.addMenus(new Menu[]{overview},1);

        Menu views = new Menu();
        views.setTitle("Views");
        views.addItem(new MenuItem("Problems"));
        views.addItem(new MenuItem("Configuration Changes"));
        views.addItem(new MenuItem("Content Changes"));
        views.addItem(new MenuItem("Recent Administration"));
        menuBar.addMenus(new Menu[]{views},2);

        menuBar.setWidth100();

        addChild(menuBar);
        setWidth100();*/
    }


    @Override
    protected void onInit() {
        super.onInit();

        setMembersMargin(20);

        this.aboutModalWindow = new AboutModalWindow();
        Img logo = new Img("RHQ.png", 100, 35);
        logo.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                MenuBarView.this.aboutModalWindow.show();
            }
        });
        addMember(logo);

        VLayout right = new VLayout();



        HLayout links = new HLayout(25);

        Hyperlink dashboardLink = new Hyperlink("Dashboard", "Dashboard");
        dashboardLink.setStylePrimaryName("TopSectionLink");
        dashboardLink.setStyleName("TopSectionLink");
        links.addMember(dashboardLink);

        Hyperlink demoLink = new Hyperlink("Demo", "Demo");
        demoLink.setStylePrimaryName("TopSectionLink");
        links.addMember(demoLink);

        Hyperlink resourcesLink = new Hyperlink("Resources", "Resources");
        resourcesLink.setStylePrimaryName("TopSectionLink");
        links.addMember(resourcesLink);

        Hyperlink bundlesLink = new Hyperlink("Bundles", "Bundles");
        bundlesLink.setStylePrimaryName("TopSectionLink");
        links.addMember(bundlesLink);

        Hyperlink adminLink = new Hyperlink("Administration", "Administration");
        adminLink.setStylePrimaryName("TopSectionLink");
        links.addMember(adminLink);


        right.addMember(links);
        right.addMember(new SearchBarPane());


        addMember(right);
    }
}
