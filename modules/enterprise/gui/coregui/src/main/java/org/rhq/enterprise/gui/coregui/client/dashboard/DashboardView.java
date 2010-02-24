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

import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.SmallGraphView;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.google.gwt.user.client.Random;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class DashboardView extends VLayout implements Presenter {

    
     private static String[] colors = new String[]{
             "FF6600", "808000", "008000", "008080", "0000FF", "666699",
             "FF0000", "FF9900", "99CC00", "339966", "33CCCC", "3366FF",
             "800080", "969696", "FF00FF", "FFCC00", "FFFF00", "00FF00",
             "00FFFF", "00CCFF", "993366", "C0C0C0", "FF99CC", "FFCC99",
             "FFFF99", "CCFFCC", "CCFFFF", "99CCFF", "CC99FF", "FFFFFF"
     };


    public void onInit() {
        setWidth100();
        setHeight100();


        final PortalLayout portalLayout = new PortalLayout(3);
        portalLayout.setWidth100();
        portalLayout.setHeight100();

        // create portlets...
        for (int i = 1; i <= 2; i++) {
            Portlet portlet = new Portlet();
            portlet.setTitle("Portlet");

            // Label label = new Label();
            // label.setAlign(Alignment.CENTER);
            // label.setLayoutAlign(VerticalAlignment.CENTER);
            // label.setContents("Portlet contents");
            // label.setBackgroundColor(colors[Random.nextInt(colors.length - 1)]);
            
            portlet.addItem(new SmallGraphView());
            portlet.setHeight(400);
            portalLayout.addPortlet(portlet);
        }

        VLayout vLayout = new VLayout(15);

        final DynamicForm form = new DynamicForm();
        form.setAutoWidth();
        form.setNumCols(5);

        final StaticTextItem numColItem = new StaticTextItem();
        numColItem.setTitle("Columns");
        numColItem.setValue(portalLayout.getMembers().length);

        ButtonItem addColumn = new ButtonItem("addColun", "Add Column");
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

        final ButtonItem addPortlet = new ButtonItem("addPortlet", "Add Portlet");
        addPortlet.setIcon("[skin]/images/actions/add.png");
        addPortlet.setAutoFit(true);

        addPortlet.setStartRow(false);
        addPortlet.setEndRow(false);
        addPortlet.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {

                final Portlet newPortlet = new Portlet();
                newPortlet.setTitle("Portlet ");

//                Label label = new Label();
//                label.setAlign(Alignment.CENTER);
//                label.setLayoutAlign(VerticalAlignment.CENTER);
//                label.setContents("Portlet contents");
//                label.setBackgroundColor(colors[Random.nextInt(colors.length - 1)]);
//                newPortlet.addItem(label);

                if (Random.nextBoolean()) {
                    ResourceSearchView item = new ResourceSearchView();
                    newPortlet.addItem(item);

                } else {
                    newPortlet.addItem(new RolesView());    
                }
                newPortlet.setHeight(350);

                newPortlet.setVisible(false);
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
        });


        form.setItems(numColItem, addPortlet, addColumn, removeColumn);

        addMember(form);
        addMember(portalLayout);

    }

    public boolean fireDisplay(Place place, List<Place> children) {
        return place.equals(getPlace());
    }

    public Place getPlace() {
        return new Place("Dashboard", "Dashboard");
    }
}
