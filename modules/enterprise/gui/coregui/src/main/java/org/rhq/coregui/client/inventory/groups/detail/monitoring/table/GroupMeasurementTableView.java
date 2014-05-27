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

package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;

/**
 * Views a resource's measurements in a tabular view.
 *
 * @author John Mazzitelli
 * @author Simeon Pinder
 * @author Mike Thompson
 */
public class GroupMeasurementTableView extends Table<GroupMetricsTableDataSource> implements AutoRefresh, Refreshable {

    EntityContext context;
    protected final ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor;
    protected Timer refreshTimer;

    public GroupMeasurementTableView(ResourceGroupComposite groupComposite) {
        super();
        this.context = EntityContext.forGroup(groupComposite.getResourceGroup());
        setDataSource(new GroupMetricsTableDataSource(groupComposite));
        //disable fields used when is full screen
        setShowFooterRefresh(false);
        setTitle(MSG.common_title_numeric_metrics());
        buttonBarDateTimeRangeEditor = new ButtonBarDateTimeRangeEditor(this);
        startRefreshCycle();
    }

    @Override
    public void refreshData() {
        if (isVisible() && !isRefreshing()) {
            Date now = new Date();
            long timeRange = CustomDateRangeState.getInstance().getTimeRange();
            Date newStartDate = new Date(now.getTime() - timeRange);
            buttonBarDateTimeRangeEditor.showUserFriendlyTimeRange(newStartDate.getTime(), now.getTime());
            buttonBarDateTimeRangeEditor.saveDateRange(newStartDate.getTime(), now.getTime());
            refresh();
        }
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
                ChartViewWindow window = new ChartViewWindow("", title);
                int defId = record.getAttributeAsInt(GroupMetricsTableDataSource.FIELD_METRIC_DEF_ID);

                CompositeGroupD3GraphListView graph = new CompositeGroupD3MultiLineGraph(context, defId);
                window.addItem(graph);
                graph.populateData();
                window.show();
            }
        });
        setListGridFields(fields.toArray(new ListGridField[getDataSource().getListGridFields().size()]));
    }

}
