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
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasource.StorageNodeLoadCompositeDatasource;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The component for displaying the StorageNodeLoadComposite data.
 *
 * @author Jirka Kremser
 */
public class StorageNodeLoadComponent extends EnhancedVLayout {
    private final ListGrid loadGrid;
    private Map<String, List<MeasurementDataNumericHighLowComposite>> sparkLineData;

    public StorageNodeLoadComponent(final int storageNodeId,
        Map<String, List<MeasurementDataNumericHighLowComposite>> sparkLineData) {
        super(5);
        setPadding(5);
        setBackgroundColor("#ffffff");
        this.sparkLineData = sparkLineData;
        final boolean showSparkLine = sparkLineData != null && !sparkLineData.isEmpty();
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
        if (showSparkLine) {
            fields.add(0, new ListGridField("sparkline", "Chart", 75));
        }
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
        loadGrid.setDataSource(datasource);
        if (showSparkLine) {
            loadGrid.addDataArrivedHandler(new DataArrivedHandler() {
                @Override
                public void onDataArrived(DataArrivedEvent event) {
                    showSparkLineGraphs();
                }
            });
        }
        addMember(loadGrid);
        
    }

    private void showSparkLineGraphs() {
        ListGridRecord[] records = loadGrid.getRecords();
        int i = 0;
        for (Entry<String, List<MeasurementDataNumericHighLowComposite>> entry : sparkLineData.entrySet()) {
            boolean someChartedData = false;
            List<MeasurementDataNumericHighLowComposite> data = entry.getValue();
            //locate last and minimum values.
            double lastValue = -1;
            double minValue = Double.MAX_VALUE;//
            for (MeasurementDataNumericHighLowComposite d : data) {
                if ((!Double.isNaN(d.getValue()))
                    && (!String.valueOf(d.getValue()).contains("NaN"))) {
                    if (d.getValue() < minValue) {
                        minValue = d.getValue();
                    }
                    lastValue = d.getValue();
                }
            }
            
            //collapse the data into comma delimited list for consumption by third party javascript library(jquery.sparkline)
            String commaDelimitedList = "";
            for (MeasurementDataNumericHighLowComposite d : data) {
                if ((!Double.isNaN(d.getValue()))
                    && (!String.valueOf(d.getValue()).contains("NaN"))) {
                    commaDelimitedList += d.getValue() + ",";
                }
            }
            
            //if graph content returned
            someChartedData = lastValue != -1;
            
            if (someChartedData && records.length > i) {
                String contents = "<span id='sparkline_" + entry.getKey() + "' class='dynamicsparkline' width='70' "
                    + "values='" + commaDelimitedList + "'>...</span>";
                records[i].setAttribute("sparkline", contents);
            }
            i++;
        }
        loadGrid.setData(records);
        new Timer() {
            @Override
            public void run() {
                BrowserUtility.graphSparkLines();
                scheduleRepeating(5000);
            }
        }.schedule(150);
    }
}
