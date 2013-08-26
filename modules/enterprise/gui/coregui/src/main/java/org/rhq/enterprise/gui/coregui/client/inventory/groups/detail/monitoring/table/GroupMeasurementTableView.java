/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.measurement.AbstractMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.AutoRefresh;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Views a resource's measurements in a tabular view.
 *
 * @author John Mazzitelli
 * @author Simeon Pinder
 * @author Mike Thompson
 */
public class GroupMeasurementTableView extends Table<GroupMetricsTableDataSource> implements AutoRefresh, Refreshable {

    private final int groupId;
    private final boolean isAutogroup;
    protected final MeasurementUserPreferences measurementUserPrefs;
    protected final ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor;
    protected Timer refreshTimer;

    public GroupMeasurementTableView(ResourceGroupComposite groupComposite, int groupId) {
        super();
        this.groupId = groupId;
        this.isAutogroup = groupComposite.getResourceGroup().getAutoGroupParentResource() != null;
        setDataSource(new GroupMetricsTableDataSource(groupComposite, groupId));
        //disable fields used when is full screen
        setShowFooterRefresh(true);
        setTitle(MSG.common_title_numeric_metrics());

        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        buttonBarDateTimeRangeEditor = new ButtonBarDateTimeRangeEditor(measurementUserPrefs,this);
    }

    @Override
    public void refreshData() {

    }

    @Override
    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy( refreshTimer);

        super.onDestroy();
    }

    @Override
    public boolean isRefreshing() {
        return false;
    }

    //Custom refresh operation as we are not directly extending Table
    @Override
    public void refresh() {
        if (isVisible() && !isRefreshing()) {
            Date now = new Date();
            AbstractMeasurementRangeEditor.MetricRangePreferences metricRangePreferences =  measurementUserPrefs.getMetricRangePreferences();
            long timeRange = metricRangePreferences.end - metricRangePreferences.begin;
            Date newStartDate = new Date(now.getTime() - timeRange);
            buttonBarDateTimeRangeEditor.updateDateTimeRangeDisplay(newStartDate, now);
            buttonBarDateTimeRangeEditor.saveDateRange(newStartDate.getTime(), now.getTime());

            refreshData();
        }
    }

    @Override
    protected void configureTable() {
        addTopWidget(buttonBarDateTimeRangeEditor);

        ArrayList<ListGridField> fields = getDataSource().getListGridFields();

        //add cell click handler to execute on Table data entries.
        getListGrid().addCellClickHandler(new CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                Record record = event.getRecord();
                String title = record.getAttribute(GroupMetricsTableDataSource.FIELD_METRIC_LABEL);
                ChartViewWindow window = new ChartViewWindow("MeasurementTableFrame", title);
                int defId = record.getAttributeAsInt(GroupMetricsTableDataSource.FIELD_METRIC_DEF_ID);

                CompositeGroupD3GraphListView graph = new CompositeGroupD3MultiLineGraph(groupId, defId, isAutogroup);
                window.addItem(graph);
                graph.populateData();
                window.show();
            }
        });
        setListGridFields(fields.toArray(new ListGridField[getDataSource().getListGridFields().size()]));
    }

}
