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
package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.ArrayList;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.menu.events.ItemClickHandler;

import org.rhq.enterprise.gui.coregui.client.dashboard.store.StoredDashboard;
import org.rhq.enterprise.gui.coregui.client.dashboard.store.StoredPortlet;

/**
 * @author Greg Hinkle
 */
public class DashboardView extends VLayout {

    StoredDashboard storedDashboard;

    boolean editMode = false;

    PortalLayout portalLayout;
    DynamicForm form;
    IMenuButton addPortlet;


    public DashboardView(StoredDashboard storedDashboard) {
        this.storedDashboard = storedDashboard;
        setOverflow(Overflow.AUTO);
        setPadding(5);
    }

    public void redraw() {
        for (Canvas c : getChildren()) {
            c.destroy();
        }

        buildPortlets();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        buildPortlets();
    }

    public void buildPortlets() {
        setWidth100();
        setHeight100();


        portalLayout = new PortalLayout(storedDashboard.getColumns());
        portalLayout.setWidth100();
        portalLayout.setHeight100();


        loadPortlets();


        form = new DynamicForm();
        form.setAutoWidth();
        form.setNumCols(7);

        final StaticTextItem numColItem = new StaticTextItem();
        numColItem.setTitle("Columns");
        numColItem.setValue(portalLayout.getMembers().length);

        ButtonItem addColumn = new ButtonItem("addColumn", "Add Column");
//        addColumn.setIcon("silk/application_side_expand.png");
        addColumn.setAutoFit(true);
        addColumn.setStartRow(false);
        addColumn.setEndRow(false);


        addColumn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                portalLayout.addMember(new PortalColumn());
                numColItem.setValue(portalLayout.getMembers().length);

            }
        });

        ButtonItem removeColumn = new ButtonItem("removeColumn", "Remove Column");
//        removeColumn.setIcon("silk/application_side_contract.png");
        removeColumn.setAutoFit(true);
        removeColumn.setStartRow(false);
        removeColumn.setEndRow(false);


        removeColumn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {

                Canvas[] canvases = portalLayout.getMembers();
                int numMembers = canvases.length;
                if (numMembers > 0) {
                    Canvas lastMember = canvases[numMembers - 1];
                    portalLayout.removeMember(lastMember);
                    numColItem.setValue(numMembers - 1);
                }

            }
        });


        final ButtonItem editButton = new ButtonItem("editable", editMode ? "View Mode" : "Edit Mode");
        editButton.setAutoFit(true);
        editButton.setStartRow(false);
        editButton.setEndRow(false);
        editButton.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                editMode = !editMode;
                redraw();
            }
        });


        Menu addPorletMenu = new Menu();
        for (String portletName : PortletFactory.getRegisteredPortlets()) {
            addPorletMenu.addItem(new MenuItem(portletName));
        }


        addPortlet = new IMenuButton("Add Portlet", addPorletMenu);


//        addPortlet = new ButtonItem("addPortlet", "Add Portlet");
        addPortlet.setIcon("[skin]/images/actions/add.png");
        addPortlet.setAutoFit(true);

        addPorletMenu.addItemClickHandler(new ItemClickHandler() {
            public void onItemClick(ItemClickEvent itemClickEvent) {
                String portalTitle = itemClickEvent.getItem().getTitle();
                addPortlet(portalTitle);
            }
        });

        CanvasItem addCanvas = new CanvasItem();
        addCanvas.setShowTitle(false);
        addCanvas.setCanvas(addPortlet);
        addCanvas.setStartRow(false);
        addCanvas.setEndRow(false);


        if (editMode) {
            form.setItems(numColItem, addCanvas, addColumn, removeColumn, editButton);
        } else {
            form.setItems(editButton);
        }
        addMember(form);
        addMember(portalLayout);

    }

    private void loadPortlets() {

        int col = 0;
        for (ArrayList<StoredPortlet> column : storedDashboard.getPortlets()) {

            for (StoredPortlet storedPortlet : column) {
                Canvas portalCanvas = PortletFactory.buildPortlet(storedPortlet);

                final Portlet portlet = new Portlet(editMode);
                portlet.addItem(portalCanvas);
                portlet.setTitle(storedPortlet.getName());

                portlet.setHeight(storedPortlet.getHeight());
                portlet.setVisible(true);

//                newPortlet.setHelpClickHandler(handler);
//                newPortlet.setSettingsClickHandler(handler);

                portalLayout.addPortlet(portlet, col);
            }

            col++;
        }


    }


    private void addPortlet(String portletName) {
        final Portlet newPortlet = new Portlet(true);

        StoredPortlet storedPortlet = new StoredPortlet(portletName, portletName, 250);
        Canvas canvas = PortletFactory.buildPortlet(storedPortlet);

        newPortlet.setTitle(portletName);


        ClickHandler handler = new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                PortletSettingsWindow settingsWindow = new PortletSettingsWindow("Recently Added Resources");
                settingsWindow.show();
            }
        };

        newPortlet.addItem(canvas);

        newPortlet.setHeight(350);
        newPortlet.setVisible(false);
        newPortlet.setHelpClickHandler(handler);
        newPortlet.setSettingsClickHandler(handler);


        PortalColumn column = portalLayout.addPortlet(newPortlet);

        // also insert a blank spacer element, which will trigger the built-in
        //  animateMembers layout animation
        final LayoutSpacer placeHolder = new LayoutSpacer();
        placeHolder.setRect(newPortlet.getRect());
        column.addMember(placeHolder, 0); // add to top

        // create an outline around the clicked button
        final Canvas outline = new Canvas();
        outline.setLeft(form.getAbsoluteLeft() + addPortlet.getLeft());
        outline.setTop(form.getAbsoluteTop());
        outline.setWidth(addPortlet.getWidth());
        outline.setHeight(addPortlet.getHeight());
        outline.setBorder("2px solid 8289A6");
        outline.draw();
        outline.bringToFront();

        outline.animateRect(newPortlet.getPageLeft(), newPortlet.getPageTop(),
                newPortlet.getVisibleWidth(), newPortlet.getViewportHeight(),
                new AnimationCallback() {
                    public void execute(boolean earlyFinish) {
                        // callback at end of animation - destroy placeholder and outline; show the new portlet
                        placeHolder.destroy();
                        outline.destroy();
                        newPortlet.show();
                    }
                }, 750);
    }
}
