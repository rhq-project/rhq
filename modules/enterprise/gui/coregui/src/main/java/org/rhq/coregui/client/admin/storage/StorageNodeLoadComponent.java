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
package org.rhq.coregui.client.admin.storage;

import static org.rhq.coregui.client.admin.storage.StorageNodeDatasource.DONT_MISS_ME_CLASS;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasource.OK_CLASS;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasource.WARN_CLASS;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.coregui.client.admin.storage.StorageNodeDatasource.StorageNodeLoadCompositeDatasource;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

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
        this.sparkLineData = sparkLineData;
        final boolean showSparkLine = sparkLineData != null && !sparkLineData.isEmpty();
        loadGrid = new ListGrid() {
            @Override
            protected String getCellCSSText(ListGridRecord record, int rowNum, int colNum) {
                if ("avg".equals(getFieldName(colNum))
                    && (StorageNodeLoadCompositeDatasource.KEY_HEAP_PERCENTAGE.equals(record.getAttribute("id"))
                        || StorageNodeLoadCompositeDatasource.KEY_DATA_DISK_SPACE_PERCENTAGE.equals(record
                            .getAttribute("id")) || StorageNodeLoadCompositeDatasource.KEY_TOTAL_DISK_SPACE_PERCENTAGE
                            .equals(record.getAttribute("id")))) {
                    if (record.getAttributeAsFloat("avgFloat") > .85) {
                        return DONT_MISS_ME_CLASS;
                    } else if (record.getAttributeAsFloat("avgFloat") > .7) {
                        return WARN_CLASS;
                    } else {
                        return OK_CLASS;
                    }
                } else if ("max".equals(getFieldName(colNum))
                    && StorageNodeLoadCompositeDatasource.KEY_FREE_DISK_TO_DATA_SIZE_RATIO.equals(record
                        .getAttribute("id"))) {
                    if (record.getAttributeAsFloat("avgFloat") < .7) {
                        return DONT_MISS_ME_CLASS;
                    } else if (record.getAttributeAsFloat("avgFloat") < 1.5) {
                        return WARN_CLASS;
                    } else {
                        return OK_CLASS;
                    }
                } else {
                    return super.getCellCSSText(record, rowNum, colNum);
                }
            }
        };
        loadGrid
            .setID(EnhancedUtility.getSafeId(this.getClass().getName() + storageNodeId + "_" + Random.nextDouble()));
        loadGrid.setWidth100();
        loadGrid.setHeight(200);
        loadGrid.setAutoFitData(Autofit.VERTICAL);
        StorageNodeLoadCompositeDatasource datasource = StorageNodeLoadCompositeDatasource.getInstance(storageNodeId);
        List<ListGridField> fields = datasource.getListGridFields();
        if (showSparkLine) {
            fields.add(0, new ListGridField("sparkline", MSG.view_adminTopology_storageNodes_detail_chart(), 75));
        }
        loadGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        loadGrid.setHoverWidth(300);
        loadGrid.setDataSource(datasource);
        loadGrid.setAutoFetchData(true);
        //        loadGrid.fetchData();
        if (showSparkLine) {
            loadGrid.addDataArrivedHandler(new DataArrivedHandler() {
                @Override
                public void onDataArrived(DataArrivedEvent event) {
                    showSparkLineGraphs();
                    loadGrid.redraw();
                }
            });
        }
        addMember(loadGrid);

    }

    private void showSparkLineGraphs() {
        ListGridRecord[] records = loadGrid.getRecords();
        for (int i = 0; i < records.length; i++) {
            String metricName = records[i].getAttributeAsString("id");
            boolean someChartedData = false;
            List<MeasurementDataNumericHighLowComposite> data = sparkLineData.get(metricName);
            // locate last and minimum values.
            double lastValue = -1;
            double minValue = Double.MAX_VALUE;
            if (data != null) {
                for (MeasurementDataNumericHighLowComposite d : data) {
                    if ((!Double.isNaN(d.getValue())) && (!String.valueOf(d.getValue()).contains("NaN"))) {
                        if (d.getValue() < minValue) {
                            minValue = d.getValue();
                        }
                        lastValue = d.getValue();
                    }
                }
            }
            // if graph content returned
            someChartedData = lastValue != -1;

            // collapse the data into comma delimited list for consumption by third party javascript library (jquery.sparkline)
            StringBuilder commaDelimitedList = new StringBuilder();
            if (data != null) {
                for (MeasurementDataNumericHighLowComposite d : data) {
                    if ((!Double.isNaN(d.getValue())) && (!String.valueOf(d.getValue()).contains("NaN"))) {
                        commaDelimitedList.append(d.getValue()).append(",");
                    }
                }
            }
            if (commaDelimitedList.length() > 0) {
                commaDelimitedList = commaDelimitedList.deleteCharAt(commaDelimitedList.length() - 1);
            }
            if (!commaDelimitedList.toString().contains(",")) {
                // prepend another value just so we have 2 values and it will graph
                commaDelimitedList.insert(0, "0,");
            }

            if (someChartedData) {
                String contents = "<span id='sparkline_" + metricName + "' class='dynamicsparkline' width='70' "
                    + "values='" + commaDelimitedList.toString() + "'>...</span>";
                records[i].setAttribute("sparkline", contents);
            }
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
