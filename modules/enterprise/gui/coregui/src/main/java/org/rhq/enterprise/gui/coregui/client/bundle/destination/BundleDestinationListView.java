/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.bundle.destination;

import java.util.HashMap;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 */
public class BundleDestinationListView extends Table {

    public BundleDestinationListView(String locatorId) {
        super(locatorId, "Bundle Destinations");

    }

    public BundleDestinationListView(String locatorId, Criteria criteria) {
        super(locatorId, "Bundle Destinations", criteria);
        setHeaderIcon("subsystems/bundle/BundleDestination_24.png");
    }

    @Override
    protected void onInit() {
        super.onInit();

        setDataSource(new BundleDestinationDataSource());

        getListGrid().getField("id").setWidth(45);
        getListGrid().getField("name").setWidth("20%");
        getListGrid().getField("name").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Bundles/Bundles/" + listGridRecord.getAttribute("bundleId") + "/destinations/"
                    + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        getListGrid().getField("description").setWidth("15%");
        //getListGrid().getField("bundleName").setWidth("20%");
        getListGrid().hideField("bundleName");
        getListGrid().getField("groupName").setWidth("15%");
        getListGrid().getField("deployDir").setWidth("15%");

        ListGridField status = getListGrid().getField("latestDeploymentStatus");
        HashMap<String, String> statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.WARN.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setWidth(80);

    }
}
