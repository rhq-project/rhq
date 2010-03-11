/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.summary;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.core.domain.plugin.SummaryCounts;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceBossGWTServiceAsync;

public class SummaryCountsView extends VLayout {

    private ResourceBossGWTServiceAsync resourceBossService = GWTServiceLookup.getResourceBossService();

    public SummaryCountsView() {
        resourceBossService.getInventorySummaryForLoggedInUser(new AsyncCallback<SummaryCounts>() {
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError("Failed to retrieve inventory summary", throwable);                    
            }

            public void onSuccess(SummaryCounts summary) {
                Grid grid = new Grid(4, 2);

                grid.setText(0, 0, "Platform Total");
                grid.setText(0, 1, summary.getPlatformCount().toString());

                grid.setText(1, 0, "Server Total");
                grid.setText(1, 1, summary.getServerCount().toString());

                addMember(grid);
            }
        });
    }

}
