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

    private int bundleId;
    private Bundle bundle;
    private BundleVersion bundleVersion;


    public BundleDeploymentListView(Bundle bundle) {
        super("Bundle Deployments");

        this.bundle = bundle;
        this.bundleId = bundle.getId();
    }

    public BundleDeploymentListView(Criteria criteria) {
        super("Bundle Deployments", criteria);
    }

    public BundleDeploymentListView(BundleVersion bundleVersion) {
        this.bundleVersion = bundleVersion;
        this.bundleId = bundleVersion.getBundle().getId();
    }

    @Override
    protected void onInit() {
        super.onInit();
        setHeaderIcon("subsystems/bundle/BundleDeployment_24.png");




        String title = "Bundle Deployments";
        Criteria criteria = new Criteria();
        if (bundle != null) {
            title = bundle.getName() + " deployments";
            criteria.setAttribute("bundleId", bundle.getId());
        }
        if (bundleVersion != null) {
            title = bundleVersion.getVersion() + " deployments";
            criteria.setAttribute("bundleVersionId", bundleVersion.getId());
        }
        setTableTitle(title);

        refresh(criteria);

        setDataSource(new BundleDeploymentDataSource());

        getListGrid().getField("id").setWidth("60");
        getListGrid().getField("name").setWidth("25%");
        getListGrid().getField("name").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord record, int i, int i1) {
                return "<a href=\"#Bundles/Bundle/" + record.getAttribute("bundleId") + "/deployments/" + record.getAttribute("id") + "\">" + String.valueOf(o) + "</a>";
            }
        });

    }
}
