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
package org.rhq.enterprise.gui.coregui.client.bundle.deployment;

import java.util.HashMap;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentListView extends Table {

    public BundleDeploymentListView(Criteria criteria) {
        super("Bundle Deployments", criteria);
    }


    @Override
    protected void onInit() {
        super.onInit();
        setHeaderIcon("subsystems/bundle/BundleDeployment_24.png");

        setDataSource(new BundleDeploymentDataSource());

        getListGrid().getField("id").setWidth("60");
        getListGrid().getField("name").setWidth("25%");
        getListGrid().getField("name").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord record, int i, int i1) {
                return "<a href=\"#Bundles/Bundle/" + record.getAttribute("bundleId") + "/deployments/" + record.getAttribute("id") + "\">" + String.valueOf(o) + "</a>";
            }
        });


        getListGrid().getField("bundleVersionVersion").setWidth("80");
        getListGrid().getField("bundleVersionVersion").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Bundles/Bundle/" + listGridRecord.getAttribute("bundleId") + "/versions/"
                        + listGridRecord.getAttribute("bundleVersionId") + "\">" + o + "</a>";
            }
        });

        getListGrid().getField("description").setWidth("25%");
        getListGrid().getField("deploymentTime").setWidth("20%");
        getListGrid().getField("status").setWidth("25%");
        ListGridField status = getListGrid().getField("status");
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
