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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Greg Hinkle
 */
public class BundleDestinationListView extends VLayout {


    private Integer bundleId;

    public BundleDestinationListView() {
        setWidth100();
        setHeight100();
    }

    public BundleDestinationListView(Integer bundleId) {
        this();
        this.bundleId = bundleId;
    }

    @Override
    protected void onDraw() {
        super.onDraw();


        Criteria criteria = new Criteria();
        if (bundleId != null) {
            criteria.setAttribute("bundleId",bundleId.intValue());
        }

        Table table = new Table("Bundle Destinations", criteria);

        table.setDataSource(new BundleDestinationDataSource());

        table.getListGrid().getField("id").setWidth(25);
        table.getListGrid().getField("name").setWidth("20%");
        table.getListGrid().getField("name").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Bundles/Bundles/" + listGridRecord.getAttribute("bundleId") + "/destinations/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });


        table.getListGrid().getField("description").setWidth("25%");
        //table.getListGrid().getField("bundleName").setWidth("20%");
        table.getListGrid().hideField("bundleName");
        table.getListGrid().getField("groupName").setWidth("20%");
        table.getListGrid().getField("deployDir").setWidth("20%");


        addMember(table);
    }
}
