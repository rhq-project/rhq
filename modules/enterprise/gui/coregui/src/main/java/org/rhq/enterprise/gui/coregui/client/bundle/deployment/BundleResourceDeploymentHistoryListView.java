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

import java.util.ArrayList;

import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;

/**
 * @author Greg Hinkle
 */
public class BundleResourceDeploymentHistoryListView extends VLayout {

    private BundleResourceDeployment resourceDeployment;

    public BundleResourceDeploymentHistoryListView(BundleResourceDeployment resourceDeployment) {
        setWidth100();
        setHeight100();
        this.resourceDeployment = resourceDeployment;

    }

    @Override
    protected void onInit() {
        super.onInit();

        ListGrid grid = new ListGrid();
        grid.setWidth100();
        grid.setHeight100();

        ListGridField action = new ListGridField("action", "Action");
        ListGridField message = new ListGridField("message", "Message");
        ListGridField status = new ListGridField("status", "status");

        grid.setFields(action, message, status);

        grid.setData(buildRecords());

        addMember(grid);

    }

    public ListGridRecord[] buildRecords() {
        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (BundleResourceDeploymentHistory step : resourceDeployment.getBundleResourceDeploymentHistories()) {

            ListGridRecord record = new ListGridRecord();
            record.setAttribute("id", step.getId());

            record.setAttribute("action", step.getAction());

            record.setAttribute("info", step.getInfo());

            record.setAttribute("category", step.getCategory().toString());

            record.setAttribute("message", step.getMessage());

            record.setAttribute("attachment", step.getAttachment());

            record.setAttribute("status", step.getStatus().name());

            records.add(record);
        }

        return records.toArray(new ListGridRecord[records.size()]);

    }
}
