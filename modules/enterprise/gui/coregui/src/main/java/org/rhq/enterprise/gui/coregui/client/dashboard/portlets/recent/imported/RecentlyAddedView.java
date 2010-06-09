/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.imported;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.TreeGrid;

import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletView;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.store.StoredPortlet;

public class RecentlyAddedView extends VLayout implements PortletView {


    public static final String KEY = "Recently Added Portlet";


    @Override
    protected void onInit() {
        super.onInit();
        TreeGrid treeGrid = new TreeGrid();
        treeGrid.setDataSource(new RecentlyAddedResourceDS());
        treeGrid.setAutoFetchData(true);
        treeGrid.setTitle("Recently Added Resources");
        treeGrid.setResizeFieldsInRealTime(true);
        treeGrid.setTreeFieldTitle("Resource Name");

        ListGridField resourceNameField = new ListGridField("name", "Resource Name");

        resourceNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Resource/" + listGridRecord.getAttribute("id") + "\">" + String.valueOf(o) + "</a>";
            }
        });

        ListGridField timestampField = new ListGridField("timestamp", "Date//Time");

        treeGrid.setFields(resourceNameField, timestampField);


        addMember(new HeaderLabel("Recently Added Resources"));

        addMember(treeGrid);

    }

    public void configure(StoredPortlet storedPortlet) {

    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet displays resources that have recently been imported into the inventory.");
    }

    public Canvas getSettingsCanvas() {
        return null;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final PortletView getInstance() {
            return GWT.create(RecentlyAddedView.class);
        }
    }
}
