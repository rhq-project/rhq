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
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleDestinationListView extends Table<RPCDataSource<BundleDestination>> {

    public BundleDestinationListView(String locatorId) {
        this(locatorId, null);
    }

    public BundleDestinationListView(String locatorId, Criteria criteria) {
        super(locatorId, MSG.view_bundle_bundleDestinations(), criteria);
        setHeaderIcon("subsystems/bundle/BundleDestination_24.png");
        setDataSource(new BundleDestinationDataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(BundleDestinationDataSource.FIELD_ID, MSG.common_title_id());
        ListGridField nameField = new ListGridField(BundleDestinationDataSource.FIELD_NAME, MSG.common_title_name());
        ListGridField descriptionField = new ListGridField(BundleDestinationDataSource.FIELD_DESCRIPTION, MSG
            .common_title_description());
        ListGridField bundleNameField = new ListGridField(BundleDestinationDataSource.FIELD_BUNDLE_NAME, MSG
            .view_bundle_bundle());
        ListGridField groupNameField = new ListGridField(BundleDestinationDataSource.FIELD_GROUP_NAME, MSG
            .view_bundle_dest_group());
        ListGridField deployDirField = new ListGridField(BundleDestinationDataSource.FIELD_DEPLOY_DIR, MSG
            .view_bundle_dest_deployDir());
        ListGridField latestDeploymentVersionField = new ListGridField(
            BundleDestinationDataSource.FIELD_LATEST_DEPLOY_VERSION, MSG.view_bundle_dest_lastDeployedVersion());
        ListGridField latestDeploymentDateField = new ListGridField(
            BundleDestinationDataSource.FIELD_LATEST_DEPLOY_DATE, MSG.view_bundle_dest_lastDeploymentDate());
        ListGridField latestDeploymentStatusField = new ListGridField(
            BundleDestinationDataSource.FIELD_LATEST_DEPLOY_STATUS, MSG.view_bundle_dest_lastDeploymentStatus());

        latestDeploymentDateField.setType(ListGridFieldType.DATE);
        latestDeploymentDateField.setDateFormatter(DateDisplayFormat.TOLOCALESTRING);

        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                Integer bundleId = listGridRecord.getAttributeAsInt(BundleDestinationDataSource.FIELD_BUNDLE_ID);
                Integer bundleDestId = listGridRecord.getAttributeAsInt(BundleDestinationDataSource.FIELD_ID);
                return "<a href=\"" + LinkManager.getBundleDestinationLink(bundleId, bundleDestId) + "\">" + o + "</a>";
            }
        });

        groupNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                Integer groupId = listGridRecord.getAttributeAsInt(BundleDestinationDataSource.FIELD_GROUP_ID);
                return "<a href=\"" + LinkManager.getResourceGroupLink(groupId) + "\">" + o + "</a>";
            }
        });

        bundleNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                Integer bid = listGridRecord.getAttributeAsInt(BundleDestinationDataSource.FIELD_BUNDLE_ID);
                return "<a href=\"" + LinkManager.getBundleLink(bid) + "\">" + o + "</a>";
            }
        });

        HashMap<String, String> statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.PENDING.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Error_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
        latestDeploymentStatusField.setValueIcons(statusIcons);
        latestDeploymentStatusField.setValueIconHeight(11);
        latestDeploymentStatusField.setValueIconWidth(11);

        idField.setWidth(50);
        nameField.setWidth("20%");
        descriptionField.setWidth("25%");
        bundleNameField.setHidden(true);
        groupNameField.setWidth("15%");
        deployDirField.setWidth("20%");
        latestDeploymentVersionField.setWidth("10%");
        latestDeploymentDateField.setWidth("10%");
        latestDeploymentStatusField.setWidth(80);

        // XXX there seems to be a bug here - i want to hide the bundle column, but setHidden(true) causes the entire rendering to fail
        setListGridFields(idField, nameField, descriptionField, /*bundleNameField, */groupNameField, deployDirField,
            latestDeploymentVersionField, latestDeploymentDateField, latestDeploymentStatusField);

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelection();
                if (selectedRows != null && selectedRows.length == 1) {
                    String selectedId = selectedRows[0].getAttribute(BundleDestinationDataSource.FIELD_BUNDLE_ID);
                    String selectedDestId = selectedRows[0].getAttribute(BundleDestinationDataSource.FIELD_ID);
                    CoreGUI.goToView(LinkManager.getBundleDestinationLink(Integer.valueOf(selectedId), Integer
                        .valueOf(selectedDestId)));
                }
            }
        });
    }
}
