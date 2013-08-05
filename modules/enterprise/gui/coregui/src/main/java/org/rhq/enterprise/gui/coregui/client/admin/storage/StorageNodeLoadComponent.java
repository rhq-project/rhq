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
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ALERTS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasource.StorageNodeLoadCompositeDatasource;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
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
            fields.add(new ListGridField("sparkline", 90));
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
                String contents = "<span id='sparkline_" + entry.getKey() + "' class='dynamicsparkline' width='0' "
                    + "values='" + commaDelimitedList + "'>...</span>";
                records[i].setAttribute("sparkline", contents);
            }
            i++;
        }
        loadGrid.setData(records);
        
        
        
        
        
//        
//        
//        
//        
//
//        if (!results.isEmpty()) {
//            
//            //iterate over the retrieved charting data
//            for (int index = 0; index < displayOrder.length; index++) {
//                //retrieve the correct measurement definition
//                final MeasurementDefinition md = measurementDefMap
//                    .get(displayOrder[index]);
//
//                //load the data results for the given metric definition
//                List<MeasurementDataNumericHighLowComposite> data = results
//                    .get(index);
//
//                //locate last and minimum values.
//                double lastValue = -1;
//                double minValue = Double.MAX_VALUE;//
//                for (MeasurementDataNumericHighLowComposite d : data) {
//                    if ((!Double.isNaN(d.getValue()))
//                        && (!String.valueOf(d.getValue()).contains("NaN"))) {
//                        if (d.getValue() < minValue) {
//                            minValue = d.getValue();
//                        }
//                        lastValue = d.getValue();
//                    }
//                }
//
//                //collapse the data into comma delimited list for consumption by third party javascript library(jquery.sparkline)
//                String commaDelimitedList = "";
//
//                for (MeasurementDataNumericHighLowComposite d : data) {
//                    if ((!Double.isNaN(d.getValue()))
//                        && (!String.valueOf(d.getValue()).contains("NaN"))) {
//                        commaDelimitedList += d.getValue() + ",";
//                    }
//                }
//                DynamicForm row = new DynamicForm();
//                row.setNumCols(3);
//                row.setColWidths(65, "*", 100);
//                row.setWidth100();
//                row.setAutoHeight();
//                row.setOverflow(Overflow.VISIBLE);
//                HTMLFlow sparklineGraph = new HTMLFlow();
//                String contents = "<span id='sparkline_" + index
//                    + "' class='dynamicsparkline' width='0' " + "values='"
//                    + commaDelimitedList + "'>...</span>";
//                sparklineGraph.setContents(contents);
//                sparklineGraph.setContentsType(ContentsType.PAGE);
//                //disable scrollbars on span
//                sparklineGraph.setScrollbarSize(0);
//
//                CanvasItem sparklineContainer = new CanvasItem();
//                sparklineContainer.setShowTitle(false);
//                sparklineContainer.setHeight(16);
//                sparklineContainer.setWidth(60);
//                sparklineContainer.setCanvas(sparklineGraph);
//
//                //Link/title element
//                final String title = md.getDisplayName();
//                LinkItem link = AbstractActivityView.newLinkItem(title, null);
//                link.setTooltip(title);
//                link.setTitleVAlign(VerticalAlignment.TOP);
//                link.setAlign(Alignment.LEFT);
//                link.setClipValue(true);
//                link.setWrap(true);
//                link.setHeight(26);
//                link.setWidth("100%");
//                if (!BrowserUtility.isBrowserPreIE9()){
//                    link.addClickHandler(new ClickHandler() {
//                        @Override
//                        public void onClick(ClickEvent event) {
//                            window = new ChartViewWindow(title);
//
//                            graphView = D3GraphListView
//                                .createSingleGraph(resourceComposite.getResource(),
//                                    md.getId(), true);
//
//                            window.addItem(graphView);
//                            window.show();
//                        }
//                    });
//                } else{
//                    link.disable();
//                }
//
//
//                //Value
//                String convertedValue;
//                convertedValue = AbstractActivityView.convertLastValueForDisplay(
//                    lastValue, md);
//                StaticTextItem value = AbstractActivityView
//                    .newTextItem(convertedValue);
//                value.setVAlign(VerticalAlignment.TOP);
//                value.setAlign(Alignment.RIGHT);
//
//                row.setItems(sparklineContainer, link, value);
//                row.setWidth100();
//
//                //if graph content returned
//                if ((!md.getName().trim().contains("Trait.")) && (lastValue != -1)) {
//                    column.addMember(row);
//                    someChartedData = true;
//                }
//            }
//            if (!someChartedData) {// when there are results but no chartable entries.
//                DynamicForm row = AbstractActivityView.createEmptyDisplayRow(
//
//                AbstractActivityView.RECENT_MEASUREMENTS_NONE);
//                column.addMember(row);
//            } else {
//                //insert see more link
//                DynamicForm row = new DynamicForm();
//                String link = LinkManager
//                    .getResourceMonitoringGraphsLink(resourceId);
//                AbstractActivityView.addSeeMoreLink(row, link, column);
//            }
//            //call out to 3rd party javascript lib
//            new Timer(){
//                @Override
//                public void run() {
//                    BrowserUtility.graphSparkLines();
//                }
//            }.schedule(200);
//        } else {
//            DynamicForm row = AbstractActivityView
//                .createEmptyDisplayRow(AbstractActivityView.RECENT_MEASUREMENTS_NONE);
//            column.addMember(row);
//        }
//        setRefreshing(false);
    }
}
