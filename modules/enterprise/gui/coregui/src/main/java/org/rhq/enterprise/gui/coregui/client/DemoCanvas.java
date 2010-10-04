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
package org.rhq.enterprise.gui.coregui.client;

import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.TabBarControls;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.menu.events.ItemClickHandler;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeTreeView;
import org.rhq.enterprise.gui.coregui.client.report.tag.TagCloudView;

/**
 * @author Greg Hinkle
 */
public class DemoCanvas extends Canvas {
    public static final String VIEW_ID = "Demo";

    @Override
    protected void onInit() {

        setWidth100();
        setHeight100();

        final TabSet topTabSet = new TabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setWidth100();//(1200);
        topTabSet.setHeight100(); //(900);

        //        topTabSet.setTop(50);

        Tab tagTab = new Tab("Tag Cloud");
        Tab selectorTab = new Tab("Selector");
        Tab typeTree = new Tab("Type Tree");
        Tab treeTab = new Tab("Resource Tree");
        final Tab configTab = new Tab("Configuration Editor");
        //Tab alertHistoryTab = new Tab("Alert History");

        tagTab.setPane(new TagCloudView("TagTabTagCloud"));

        selectorTab.setPane(new ResourceSelector("Demo Resource Selector"));

        // Agent:  resource (10005) type (10060)
        // Raw: 10003 / 10023
        // both:  10002 / 10022

        //configTab.setPane(new ConfigurationEditor(10005, 10060, ConfigurationEditor.ConfigType.plugin));
        //treeTab.setPane(new ResourceTreeView(new Resource(10001)));
        typeTree.setPane(new ResourceTypeTreeView("DemoTypeTree"));
        //alertHistoryTab.setPane(new AbstractAlertsView());

        topTabSet.addTab(tagTab);
        topTabSet.addTab(selectorTab);
        topTabSet.addTab(typeTree);
        topTabSet.addTab(configTab);
        topTabSet.addTab(treeTab);
        //topTabSet.addTab(alertHistoryTab);
        //topTabSet.selectSubTabByTitle(alertHistoryTab);

        final Menu configSelectMenu = new Menu();
        configSelectMenu.addItem(new MenuItem("Agent"));
        configSelectMenu.addItem(new MenuItem("Raw Only"));
        configSelectMenu.addItem(new MenuItem("Structured and Raw"));
        configSelectMenu.addItem(new MenuItem("List Of Maps"));
        configSelectMenu.addItemClickHandler(new ItemClickHandler() {
            public void onItemClick(ItemClickEvent itemClickEvent) {
                int x = configSelectMenu.getItemNum(itemClickEvent.getItem());
                com.allen_sauer.gwt.log.client.Log.info("Loading: " + x);
                topTabSet.removeTab(configTab);
                switch (x) {
                case 0:
                    configTab.setPane(new ConfigurationEditor("Case0", 10005, 10060));
                    break;
                case 1:
                    configTab.setPane(new ConfigurationEditor("Case1", 10003, 10023));
                    break;
                case 2:
                    configTab.setPane(new ConfigurationEditor("Case2", 10002, 10022));
                    break;
                case 3:
                    configTab.setPane(new ConfigurationEditor("Case3", 10149, 10134));
                    break;
                }
                topTabSet.addTab(configTab, 0);
                topTabSet.selectTab(0);
                topTabSet.redraw();

            }
        });

        topTabSet.setTabBarControls(TabBarControls.TAB_SCROLLER, TabBarControls.TAB_PICKER, new MenuButton(
            "Config Resource", configSelectMenu));

        addChild(topTabSet);
    }
}
