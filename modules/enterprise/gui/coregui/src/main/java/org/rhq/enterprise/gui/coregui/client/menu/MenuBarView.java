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

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DemoCanvas;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuBar;
import com.smartgwt.client.widgets.menu.MenuItem;

import java.util.LinkedHashMap;

/**
 * @author Greg Hinkle
 */
public class MenuBarView extends HLayout {

    MenuBar menuBar;


    LinkedHashMap<String, Menu> menus = new LinkedHashMap<String, Menu>();

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

        Img logo = new Img("RHQ.png", 100, 35);
        addMember(logo);

        Hyperlink dashboardLink = new Hyperlink("Dashboard", "Dashboard");
        dashboardLink.setStylePrimaryName("TopSectionLink");
        dashboardLink.setStyleName("TopSectionLink");
        addMember(dashboardLink);

        Hyperlink demoLink = new Hyperlink("Demo", "Demo");
        demoLink.setStylePrimaryName("TopSectionLink");
        addMember(demoLink);

        Hyperlink resourcesLink = new Hyperlink("Resources", "Resources");
        resourcesLink.setStylePrimaryName("TopSectionLink");
        addMember(resourcesLink);

        Hyperlink bundlesLink = new Hyperlink("Bundles", "Bundles");
        bundlesLink.setStylePrimaryName("TopSectionLink");
        addMember(bundlesLink);
        

        Hyperlink adminLink = new Hyperlink("Administration", "Administration");
        adminLink.setStylePrimaryName("TopSectionLink");
        addMember(adminLink);



//
//        LinkItem demoLink = new LinkItem("Demo");
//        demoLink.setShowTitle(false);
//        demoLink.setLinkTitle("Demo");
//        demoLink.addClickHandler(new ClickHandler() {
//            public void onClick(ClickEvent clickEvent) {
//                CoreGUI.setContent(new DemoCanvas());
//            }
//        });
//
//
//        LinkItem adminLink = new LinkItem("Administration");
//        adminLink.setShowTitle(false);
//        adminLink.setLinkTitle("Administration");
//
//        demoLink.addClickHandler(new ClickHandler() {
//            public void onClick(ClickEvent clickEvent) {
//                CoreGUI.setContent(new AdministrationView());
//            }
//        });
//
//        DynamicForm form = new DynamicForm();
//
//        form.setItems(demoLink, adminLink);
//
//        addMember(form);

    }
}
