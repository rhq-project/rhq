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

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuBar;
import com.smartgwt.client.widgets.menu.MenuItem;

import java.util.LinkedHashMap;

/**
 * @author Greg Hinkle
 */
public class MenuBarView extends Composite {

    WidgetCanvas canvas;
    MenuBar menuBar;


    LinkedHashMap<String, Menu> menus = new LinkedHashMap<String, Menu>();

    public MenuBarView() {

        menuBar = new MenuBar();


        Menu icon = new Menu();
        icon.setTitle("iManage");
        menus.put("icon",icon);


        Menu overview = new Menu();
        overview.setTitle("Applications");
        overview.addItem(new MenuItem("Content Configuration"));
        overview.addItem(new MenuItem("Monitoring"));
        overview.addItem(new MenuItem("Administration"));
        menus.put(overview.getTitle(), overview);


        Menu views = new Menu();
        views.setTitle("Views");
        views.addItem(new MenuItem("Problems"));
        views.addItem(new MenuItem("Configuration Changes"));
        views.addItem(new MenuItem("Content Changes"));
        views.addItem(new MenuItem("Recent Administration"));
        menus.put(views.getTitle(),views);

        menuBar.setMenus(menus.values().toArray(new Menu[menus.size()]));


        initWidget(menuBar);
        setWidth("100%");
    }

}
