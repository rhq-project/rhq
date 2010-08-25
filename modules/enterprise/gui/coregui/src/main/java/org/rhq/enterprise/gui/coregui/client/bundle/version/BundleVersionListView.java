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
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.bundle.list.BundleVersionDataSource;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 */
public class BundleVersionListView extends Table {

    public BundleVersionListView(String locatorId) {
        this(locatorId, null);
    }

    public BundleVersionListView(String locatorId, Criteria criteria) {
        super(locatorId, "Bundle Versions", criteria);
        setHeaderIcon("subsystems/bundle/BundleVersion_24.png");
        BundleVersionDataSource bundleVersionsDataSource = new BundleVersionDataSource();
        setDataSource(bundleVersionsDataSource);
    }

    @Override
    protected void configureTable() {

        getListGrid().getField("id").setWidth("60");
        getListGrid().getField("name").setWidth("25%");
        getListGrid().getField("name").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Bundles/Bundle/" + listGridRecord.getAttribute("bundleId") + "/versions/"
                    + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        getListGrid().getField("version").setWidth("10%");
        getListGrid().getField("fileCount").setWidth("10%");
        getListGrid().getField("description").setWidth("*");

        getListGrid().setSelectionType(SelectionStyle.NONE);
        getListGrid().setSelectionAppearance(SelectionAppearance.ROW_STYLE);

    }
}
