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

import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 */
public class FileListView extends VLayout {

    private List<BundleFile> files;

    public FileListView(List<BundleFile> files) {
        this.files = files;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        Table table = new Table("Bundle Files");

        ListGrid listGrid = table.getListGrid();

        ListGridField id = new ListGridField("id", "Id");
        id.setWidth("20%");

        ListGridField name = new ListGridField("name", "Name");
        name.setWidth("80%");

        listGrid.setFields(id,name);

        listGrid.setData(buildRecords(files));

        addMember(listGrid);

    }



    private ListGridRecord[] buildRecords(List<BundleFile> files) {
        ListGridRecord[] records = new ListGridRecord[files.size()];

        int i = 0;
        for (BundleFile file : files) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("id",file.getId());
            record.setAttribute("name", file.getPackageVersion().getFileName());
            records[i++] = record;
        }
        return records;
    }

}
