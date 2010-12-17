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
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundleVersionDataSource;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentListView extends Table<RPCDataSource<BundleDeployment>> {

    private final boolean canManageBundles;

    public BundleDeploymentListView(String locatorId, Criteria criteria, boolean canManageBundles) {
        super(locatorId, MSG.view_bundle_bundleDeployments(), criteria);
        this.canManageBundles = canManageBundles;
        setDataSource(new BundleDeploymentDataSource());
        setHeaderIcon("subsystems/bundle/BundleDeployment_24.png");
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(BundleDeploymentDataSource.FIELD_ID, MSG.common_title_id());
        ListGridField nameField = new ListGridField(BundleDeploymentDataSource.FIELD_NAME, MSG
            .view_bundle_deploy_name());
        ListGridField descriptionField = new ListGridField(BundleDeploymentDataSource.FIELD_DESCRIPTION, MSG
            .common_title_description());
        ListGridField bundleVersionField = new ListGridField(BundleDeploymentDataSource.FIELD_BUNDLE_VERSION_VERSION,
            MSG.view_bundle_bundleVersion());
        ListGridField statusField = new ListGridField(BundleDeploymentDataSource.FIELD_STATUS, MSG
            .common_title_status());
        ListGridField deployTimeField = new ListGridField(BundleDeploymentDataSource.FIELD_DEPLOY_TIME, MSG
            .view_bundle_deploy_time());

        deployTimeField.setType(ListGridFieldType.DATE);
        deployTimeField.setDateFormatter(DateDisplayFormat.TOLOCALESTRING);

        // only users that are authorized can see deployments
        if (canManageBundles) {
            nameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord record, int i, int i1) {
                    return "<a href=\""
                        + LinkManager.getBundleDeploymentLink(record
                            .getAttributeAsInt(BundleDeploymentDataSource.FIELD_BUNDLE_ID), record
                            .getAttributeAsInt(BundleDeploymentDataSource.FIELD_ID)) + "\">" + String.valueOf(o)
                        + "</a>";
                }
            });
        }

        bundleVersionField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord record, int i, int i1) {
                return "<a href=\""
                    + LinkManager.getBundleVersionLink(record
                        .getAttributeAsInt(BundleDeploymentDataSource.FIELD_BUNDLE_ID), record
                        .getAttributeAsInt(BundleDeploymentDataSource.FIELD_BUNDLE_VERSION_ID)) + "\">"
                    + String.valueOf(o) + "</a>";
            }
        });

        HashMap<String, String> statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Error_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.WARN.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
        statusField.setValueIcons(statusIcons);
        statusField.setValueIconWidth(11);
        statusField.setValueIconHeight(11);

        idField.setWidth(50);
        nameField.setWidth("30%");
        descriptionField.setWidth("40%");
        bundleVersionField.setWidth("15%");
        deployTimeField.setWidth("15%");
        statusField.setWidth(80);

        setListGridFields(idField, nameField, descriptionField, bundleVersionField, deployTimeField, statusField);

        if (canManageBundles) {
            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid listGrid = (ListGrid) event.getSource();
                    ListGridRecord[] selectedRows = listGrid.getSelection();
                    if (selectedRows != null && selectedRows.length == 1) {
                        String selectedId = selectedRows[0].getAttribute(BundleVersionDataSource.FIELD_BUNDLE_ID);
                        String selectedVersionId = selectedRows[0].getAttribute(BundleVersionDataSource.FIELD_ID);
                        CoreGUI.goToView(LinkManager.getBundleDeploymentLink(Integer.valueOf(selectedId), Integer
                            .valueOf(selectedVersionId)));
                    }
                }
            });
        }
    }
}
