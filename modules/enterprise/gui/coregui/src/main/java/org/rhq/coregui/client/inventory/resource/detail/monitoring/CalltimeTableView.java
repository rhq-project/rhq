/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.inventory.resource.detail.monitoring;

import java.util.ArrayList;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;

/**
 * @author Thomas Segismont
 */
public class CalltimeTableView extends Table<CalltimeDataSource> implements Refreshable, AutoRefresh {
    private final TextItem destinationFilter;
    private Timer refreshTimer;

    public CalltimeTableView(EntityContext context) {
        setDataSource(new CalltimeDataSource(context));
        destinationFilter = new TextItem(CalltimeDataSource.FILTER_DESTINATION,
            MSG.view_resource_monitor_calltime_destinationFilter());
        startRefreshCycle();
    }

    @Override
    protected void configureTableFilters() {
        setFilterFormItems(this.destinationFilter);
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
    }

    @Override
    public void refreshData() {
        if (isVisible() && !isRefreshing()) {
            refresh();
        }
    }

    @Override
    public void startRefreshCycle() {
        if (refreshTimer != null) {
            destroyRefreshTimer();
        }
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    public boolean isRefreshing() {
        return false;
    }

    @Override
    protected void onDestroy() {
        destroyRefreshTimer();
        super.onDestroy();
    }

    private void destroyRefreshTimer() {
        AutoRefreshUtil.onDestroy(refreshTimer);
    }
}
