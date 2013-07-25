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

import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasource.DONT_MISS_ME_COLOR;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasource.OK_COLOR;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasource.WARN_COLOR;

import java.util.List;

import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
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

    public StorageNodeLoadComponent(final int storageNodeId, final ListGrid parentGrid, final ListGridRecord record) {
        super(5);
        setPadding(5);
        setBackgroundColor("#ffffff");
        loadGrid = new ListGrid() {
            @Override
            protected String getCellCSSText(ListGridRecord record, int rowNum, int colNum) {
                if ("avg".equals(getFieldName(colNum)) 
                    && (StorageNodeLoadCompositeDatasource.HEAP_PERCENTAGE_KEY.equals(record.getAttribute("id")) ||
                        StorageNodeLoadCompositeDatasource.DATA_DISK_SPACE_PERCENTAGE_KEY.equals(record.getAttribute("id")) ||
                        StorageNodeLoadCompositeDatasource.TOTAL_DISK_SPACE_PERCENTAGE_KEY.equals(record.getAttribute("id")))) {
                    if (record.getAttributeAsFloat("avgFloat") > .85) {
                        return DONT_MISS_ME_COLOR;
                    } else if (record.getAttributeAsFloat("avgFloat") > .7) {
                        return WARN_COLOR;
                    } else {
                        return OK_COLOR;
                    }
                } else if ("max".equals(getFieldName(colNum))
                    && StorageNodeLoadCompositeDatasource.FREE_DISK_TO_DATA_SIZE_RATIO_KEY.equals(record
                        .getAttribute("id"))) {
                    if (record.getAttributeAsFloat("avgFloat") < .7) {
                        return DONT_MISS_ME_COLOR;
                    } else if (record.getAttributeAsFloat("avgFloat") < 1.5) {
                        return WARN_COLOR;
                    } else {
                        return OK_COLOR;
                    }
                }
                else {
                    return super.getCellCSSText(record, rowNum, colNum);
                }
            }
        };
        loadGrid.setWidth100();
        loadGrid.setHeight(200);
        loadGrid.setAutoFitData(Autofit.VERTICAL);
        StorageNodeLoadCompositeDatasource datasource = StorageNodeLoadCompositeDatasource.getInstance(storageNodeId);
        List<ListGridField> fields = datasource.getListGridFields();
        loadGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        loadGrid.setAutoFetchData(true);
        loadGrid.setHoverWidth(300);


        ToolStrip toolStrip = new ToolStrip();
        IButton settingsButton = new IButton("Settings");
        settingsButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                CoreGUI.goToView(StorageNodeAdminView.VIEW_PATH + "/" + storageNodeId + "/Config");
            }
        });
        settingsButton.setExtraSpace(5);
        toolStrip.addMember(settingsButton);
        
        IButton refreshButton = new IButton(MSG.common_button_refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                loadGrid.fetchData();
            }
        });
        refreshButton.setExtraSpace(5);
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
//        addMember(toolStrip);

    }
}
