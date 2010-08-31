package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations;

/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.HeaderSpan;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.operation.RecentOperationsDataSource;
import org.rhq.enterprise.gui.coregui.client.operation.ScheduledOperationsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view that displays a live table of completed Operations and scheduled operations. 
 *
 * @author Simeon Pinder
 */
public class OperationsPortlet extends LocatableVLayout implements Portlet {

    public static final String KEY = "Operations";
    private static final String TITLE = KEY;
    private static String recentOperations = "Recent Operations";
    private static String scheduledOperations = "Scheduled Operations";

    public OperationsPortlet(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onInit() {
        super.onInit();

        //set title for larger container
        setTitle(TITLE);

        // Add the list table as the top half of the view.
        LocatableListGrid recentOperationsGrid = new LocatableListGrid(recentOperations);
        recentOperationsGrid.setDataSource(new RecentOperationsDataSource());
        recentOperationsGrid.setAutoFetchData(true);
        String[] allRows = new String[] { RecentOperationsDataSource.location, RecentOperationsDataSource.operation,
            RecentOperationsDataSource.resource, RecentOperationsDataSource.status, RecentOperationsDataSource.time };
        recentOperationsGrid.setHeaderSpans(new HeaderSpan(recentOperations, allRows));
        recentOperationsGrid.setHeaderSpanHeight(new Integer(20));
        recentOperationsGrid.setHeaderHeight(40);
        recentOperationsGrid.setResizeFieldsInRealTime(true);
        recentOperationsGrid.setCellHeight(50);
        recentOperationsGrid.setWrapCells(true);
        addMember(recentOperationsGrid);

        // Add the list table as the top half of the view.
        LocatableListGrid scheduledOperationsGrid = new LocatableListGrid(scheduledOperations);
        scheduledOperationsGrid.setDataSource(new ScheduledOperationsDataSource());
        scheduledOperationsGrid.setAutoFetchData(true);
        String[] allRows2 = new String[] { ScheduledOperationsDataSource.location,
            ScheduledOperationsDataSource.operation, ScheduledOperationsDataSource.resource,
            ScheduledOperationsDataSource.time };
        scheduledOperationsGrid.setHeaderSpans(new HeaderSpan(scheduledOperations, allRows2));
        scheduledOperationsGrid.setHeaderSpanHeight(new Integer(20));
        scheduledOperationsGrid.setHeaderHeight(40);

        scheduledOperationsGrid.setTitle(scheduledOperations);
        scheduledOperationsGrid.setResizeFieldsInRealTime(true);
        scheduledOperationsGrid.setCellHeight(50);
        scheduledOperationsGrid.setWrapCells(true);

        addMember(scheduledOperationsGrid);

    }

    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        // TODO implement this.

    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet displays both operations that have occurred and are scheduled to occur.");
    }

    public DynamicForm getCustomSettingsForm() {
        return null;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance() {
            return GWT.create(OperationsPortlet.class);
        }

        public final Portlet getInstance(String locatorId) {
            return new OperationsPortlet(locatorId);
        }

    }

}
