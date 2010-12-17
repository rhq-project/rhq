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
package org.rhq.enterprise.gui.coregui.client.bundle.version;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundleVersionDataSource;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class BundleVersionListView extends Table<RPCDataSource<BundleVersion>> {

    public BundleVersionListView(String locatorId) {
        this(locatorId, null);
    }

    public BundleVersionListView(String locatorId, Criteria criteria) {
        super(locatorId, MSG.view_bundle_bundleVersions(), criteria);
        setHeaderIcon("subsystems/bundle/BundleVersion_24.png");
        setDataSource(new BundleVersionDataSource());
    }

    @Override
    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.SINGLE;
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(FIELD_ID, MSG.common_title_id());

        ListGridField versionField = new ListGridField(BundleVersionDataSource.FIELD_VERSION, MSG
            .common_title_version());
        versionField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                Integer _bundleId = listGridRecord.getAttributeAsInt("bundleId");
                Integer _bvId = listGridRecord.getAttributeAsInt("id");
                return "<a href=\"" + LinkManager.getBundleVersionLink(_bundleId, _bvId) + "\">" + o + "</a>";
            }
        });

        ListGridField nameField = new ListGridField(BundleVersionDataSource.FIELD_NAME, MSG.common_title_name());

        ListGridField descriptionField = new ListGridField(BundleVersionDataSource.FIELD_DESCRIPTION, MSG
            .common_title_description());

        ListGridField fileCountField = new ListGridField(BundleVersionDataSource.FIELD_FILECOUNT, MSG
            .view_bundle_bundleFiles());

        idField.setWidth(50);
        versionField.setWidth("20%");
        nameField.setWidth("25%");
        descriptionField.setWidth("*");
        fileCountField.setWidth("10%");

        setListGridFields(idField, versionField, nameField, descriptionField, fileCountField);

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelection();
                if (selectedRows != null && selectedRows.length == 1) {
                    String selectedId = selectedRows[0].getAttribute(BundleVersionDataSource.FIELD_BUNDLE_ID);
                    String selectedVersionId = selectedRows[0].getAttribute(BundleVersionDataSource.FIELD_ID);
                    CoreGUI.goToView(LinkManager.getBundleVersionLink(Integer.valueOf(selectedId), Integer
                        .valueOf(selectedVersionId)));
                }
            }
        });
    }
}
