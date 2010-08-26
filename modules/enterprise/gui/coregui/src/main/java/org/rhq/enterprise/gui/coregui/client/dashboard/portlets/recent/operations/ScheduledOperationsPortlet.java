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
import com.smartgwt.client.types.GroupStartOpen;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.operation.RecentOperationsDataSource;

/**
 * A view that displays a live table of completed Operations and scheduled operations. 
 *
 * @author Simeon Pinder
 */
public class ScheduledOperationsPortlet extends VLayout implements Portlet {

    public static final String KEY = "Operations";
    private static final String TITLE = KEY;

    public ScheduledOperationsPortlet() {
    }

    @Override
    protected void onInit() {
        super.onInit();

        // Add the list table as the top half of the view.
        ListGrid listGrid = new ListGrid();
        listGrid.setDataSource(new RecentOperationsDataSource());
        listGrid.setGroupStartOpen(GroupStartOpen.ALL);  
        listGrid.setGroupByField("continent");
        listGrid.setAutoFetchData(true);
        listGrid.setTitle(TITLE);
        listGrid.setResizeFieldsInRealTime(true);
        addMember(listGrid);
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
            return GWT.create(ScheduledOperationsPortlet.class);
        }
    }

}
