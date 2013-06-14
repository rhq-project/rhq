/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.admin.storage;

import java.util.List;

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasource.StorageNodeLoadCompositeDatasource;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The component for displaying the StorageNodeLoadComposite data.
 *
 * @author Jirka Kremser
 */
public class StorageNodeLoadComponent extends EnhancedVLayout {
    private final ListGrid loadGrid;

    public StorageNodeLoadComponent(int storageNodeId) {
        this(storageNodeId, null, null);
    }

    public StorageNodeLoadComponent(int storageNodeId, final ListGrid parentGrid, final ListGridRecord record) {
        super(5);
        setPadding(5);
        setBackgroundColor("#ffffff");
        loadGrid = new ListGrid() {
            @Override
            protected String getCellCSSText(ListGridRecord record, int rowNum, int colNum) {
                if ("avg".equals(getFieldName(colNum))
                    && (StorageNodeLoadCompositeDatasource.HEAP_PERCENTAGE_KEY.equals(record.getAttribute("id")) || StorageNodeLoadCompositeDatasource.DISK_SPACE_PERCENTAGE_KEY
                        .equals(record.getAttribute("id")))) {
                    if (record.getAttributeAsFloat("avgFloat") > .85) {
                        return "font-weight:bold; color:#d64949;";
                    } else if (record.getAttributeAsFloat("avgFloat") > .7) {
                        return "color:#ed9b26;";
                    } else {
                        return "color:#26aa26;";
                    }
                } else {
                    return super.getCellCSSText(record, rowNum, colNum);
                }
            }
        };
        loadGrid.setWidth100();
        loadGrid.setHeight(200);
        StorageNodeLoadCompositeDatasource datasource = StorageNodeLoadCompositeDatasource.getInstance(storageNodeId);
        List<ListGridField> fields = datasource.getListGridFields();
        loadGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        loadGrid.setAutoFetchData(true);

        IButton refreshButton = new IButton(MSG.common_button_refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                loadGrid.fetchData();
            }
        });
        ToolStrip toolStrip = new ToolStrip();
        toolStrip.addMember(refreshButton);

        if (parentGrid != null && record != null) {
            IButton closeButton = new IButton(MSG.common_button_close());
            closeButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    parentGrid.collapseRecord(record);
                }
            });
            toolStrip.addMember(closeButton);
        }
        loadGrid.setDataSource(datasource);
        addMember(loadGrid);
        addMember(toolStrip);

    }
}
