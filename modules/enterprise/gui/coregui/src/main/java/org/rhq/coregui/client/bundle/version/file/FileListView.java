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
package org.rhq.coregui.client.bundle.version.file;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Greg Hinkle
 */
public class FileListView extends EnhancedVLayout {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String FILESIZE = "fileSize";
    private static final String MD5 = "md5";
    private static final String SHA256 = "sha256";
    private static final String VERSION = "version";

    private int bundleVersionId;

    public FileListView(int bundleVersionId) {
        super();
        this.bundleVersionId = bundleVersionId;
    }

    private void viewFiles(PageList<BundleFile> files) {

        Table table = new Table(MSG.view_bundle_bundleFiles());
        table.setShowFooterRefresh(false);

        ListGridField id = new ListGridField(ID, MSG.common_title_id());
        ListGridField name = new ListGridField(NAME, MSG.common_title_name());
        ListGridField size = new ListGridField(FILESIZE, MSG.view_bundle_fileListView_fileSize());
        //ListGridField md5 = new ListGridField(MD5, MSG.view_bundle_fileListView_md5());
        ListGridField sha256 = new ListGridField(SHA256, MSG.view_bundle_fileListView_sha256());
        ListGridField version = new ListGridField(VERSION, MSG.common_title_version());

        id.setWidth("50");
        setAutoFitOnField(name);
        setAutoFitOnField(size);
        //setAutoFitOnField(md5);
        setAutoFitOnField(sha256);
        setAutoFitOnField(version);

        // To get the ListGrid the Table must be initialized (via onInit()) by adding to the Canvas
        addMember(table);

        ListGrid listGrid = table.getListGrid();
        listGrid.setFields(id, name, size, /*md5,*/sha256, version); // today, we don't set md5, no sense showing it
        listGrid.setData(buildRecords(files));
        table.setTableActionDisableOverride(true);
    }

    private void setAutoFitOnField(ListGridField field) {
        field.setAutoFitWidth(true);
        field.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        BundleFileCriteria criteria = new BundleFileCriteria();
        criteria.addFilterBundleVersionId(bundleVersionId);
        criteria.fetchPackageVersion(true);
        criteria.setPageControl(PageControl.getUnlimitedInstance());

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
            PackageVersion packageVersion = file.getPackageVersion();

            record.setAttribute(ID, file.getId());
            record.setAttribute(NAME, returnValueOrUnknown(packageVersion.getFileName()));
            record.setAttribute(MD5, returnValueOrUnknown(packageVersion.getMD5()));
            record.setAttribute(SHA256, returnValueOrUnknown(packageVersion.getSHA256()));
            record.setAttribute(VERSION, returnValueOrUnknown(packageVersion.getVersion()));

            Long size = packageVersion.getFileSize();
            if (size != null) {
                record.setAttribute(FILESIZE,
                    MeasurementConverterClient.format(size.doubleValue(), MeasurementUnits.BYTES, true));
            } else {
                record.setAttribute(FILESIZE, MSG.common_val_na());
            }
            records[i++] = record;
        }
        return records;
    }

    private String returnValueOrUnknown(String val) {
        return (val != null && val.length() > 0) ? val : MSG.common_val_na();
    }
}
