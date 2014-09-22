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

import com.google.gwt.user.client.Timer;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Thomas Segismont
 */
public class CalltimeView extends EnhancedVLayout implements Refreshable, AutoRefresh {
    private final CalltimeTableView calltimeTableView;
    private final ButtonBarDateTimeRangeEditor timeRangeEditor;
    private Timer refreshTimer;
    private boolean isRefreshing;

    public CalltimeView(EntityContext entityContext) {
        calltimeTableView = new CalltimeTableView(entityContext);
        timeRangeEditor = new ButtonBarDateTimeRangeEditor(this);
        startRefreshCycle();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        addMember(timeRangeEditor);
        addMember(calltimeTableView);
    }

    @Override
    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    public boolean isRefreshing() {
        return isRefreshing;
    }

    @Override
    public void refresh() {
        if (isVisible() && !isRefreshing()) {
            isRefreshing = true;
            try {
                timeRangeEditor.updateTimeRangeToNow();
                refreshData();
            } finally {
                isRefreshing = false;
            }
        }
    }

    @Override
    public void refreshData() {
        calltimeTableView.refresh(false);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);
        super.onDestroy();
    }
}
