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
package org.rhq.enterprise.gui.coregui.client.bundle.version.file;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class FileListView extends LocatableVLayout {

    private int bundleVersionId;

    public FileListView(String locatorId, int bundleVersionId) {
        super(locatorId);
        this.bundleVersionId = bundleVersionId;
    }

    private void viewFiles(PageList<BundleFile> files) {

        Table table = new Table(extendLocatorId("BundleFiles"), MSG.view_bundle_bundleFiles());

        ListGridField id = new ListGridField("id", MSG.common_title_id());
        id.setWidth("20%");

        ListGridField name = new ListGridField("name", MSG.common_title_name());
        name.setWidth("60%");

        ListGridField size = new ListGridField("size", MSG.view_bundle_fileListView_fileSize());
        name.setWidth("20%");

        // To get the ListGrid the Table must be initialized (via onInit()) by adding to the Canvas
        addMember(table);
        table.getListGrid().setFields(id, name, size);
        table.getListGrid().setData(buildRecords(files));

    }

    @Override
    protected void onDraw() {
        super.onDraw();

        BundleFileCriteria criteria = new BundleFileCriteria();
        criteria.addFilterBundleVersionId(bundleVersionId);
        criteria.fetchPackageVersion(true);

        GWTServiceLookup.getBundleService().findBundleFilesByCriteria(criteria,
            new AsyncCallback<PageList<BundleFile>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_fileListView_loadFailure(), caught);
                }

                public void onSuccess(PageList<BundleFile> result) {
                    viewFiles(result);
                }
            });
    }

    private ListGridRecord[] buildRecords(List<BundleFile> files) {
        ListGridRecord[] records = new ListGridRecord[files.size()];

        int i = 0;
        for (BundleFile file : files) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("id", file.getId());
            record.setAttribute("name", file.getPackageVersion().getFileName());

            Long size = file.getPackageVersion().getFileSize();
            if (size != null) {
                record.setAttribute("size", MeasurementConverterClient.format(size.doubleValue(),
                    MeasurementUnits.BYTES, true));
            }
            records[i++] = record;
        }
        return records;
    }

}
