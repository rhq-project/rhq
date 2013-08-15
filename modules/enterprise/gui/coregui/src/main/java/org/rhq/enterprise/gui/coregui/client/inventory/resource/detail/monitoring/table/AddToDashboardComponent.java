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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table;

import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph.ResourceD3GraphPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.Enhanced;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Mike Thompson
 */
public class AddToDashboardComponent extends EnhancedToolStrip implements Enhanced {
    final private Resource resource;
    private SelectItem dashboardSelectItem;
    private Dashboard selectedDashboard;
    private IButton addToDashboardButton;
    private LinkedHashMap<String, String> dashboardMenuMap;
    private LinkedHashMap<Integer, Dashboard> dashboardMap;
    private MetricsTableView.MetricsTableListGrid metricsListGrid;

    public AddToDashboardComponent(Resource resource) {
        this.resource = resource;
        setPadding(5);
        setMembersMargin(15);
        setWidth(300);
        dashboardMenuMap = new LinkedHashMap<String, String>();
        dashboardMap = new LinkedHashMap<Integer, Dashboard>();
        createToolstrip();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        createToolstrip();
    }

    private void createToolstrip() {
        addSpacer(15);
        addToDashboardButton = new IButton(MSG.view_metric_addToDashboard());
        addToDashboardButton.disable();

        dashboardSelectItem = new SelectItem();
        dashboardSelectItem.setTitle(MSG.chart_metrics_add_to_dashboard_label());
        dashboardSelectItem.setWidth(300);
        dashboardSelectItem.setPickListWidth(210);
        populateDashboardMenu();
        addFormItem(dashboardSelectItem);
        addMember(addToDashboardButton);

        dashboardSelectItem.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                Integer selectedDashboardId = Integer.valueOf((String) changeEvent.getValue());
                selectedDashboard = dashboardMap.get(selectedDashboardId);
            }
        });
        addToDashboardButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                ListGridRecord[] selectedRecords = metricsListGrid.getSelectedRecords();
                for (ListGridRecord selectedRecord : selectedRecords) {
                    for (MeasurementDefinition measurementDefinition : resource.getResourceType()
                        .getMetricDefinitions()) {
                        if (measurementDefinition.getId() == selectedRecord
                            .getAttributeAsInt(MetricsViewDataSource.FIELD_METRIC_DEF_ID)) {
                            Log.info("Add to Dashboard -- Storing: " + measurementDefinition.getDisplayName() + " in "
                                + selectedDashboard.getName());
                            storeDashboardMetric(selectedDashboard, resource, measurementDefinition);
                            break;
                        }
                    }
                }
            }
        });
    }

    public void disableAddToDashboardButton() {
        addToDashboardButton.disable();
    }

    public void enableAddToDashboardButton() {
        addToDashboardButton.enable();
    }

    public void populateDashboardMenu() {
        dashboardMenuMap.clear();
        dashboardMap.clear();

        DashboardCriteria criteria = new DashboardCriteria();
        GWTServiceLookup.getDashboardService().findDashboardsByCriteria(criteria,
            new AsyncCallback<PageList<Dashboard>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_dashboard(),
                        caught);
                }

                public void onSuccess(PageList<Dashboard> dashboards) {
                    if(dashboards.size() > 0){
                        for (final Dashboard dashboard : dashboards) {
                            dashboardMenuMap.put(String.valueOf(dashboard.getId()),
                                MSG.view_tree_common_contextMenu_addChartToDashboard(dashboard.getName()));
                            dashboardMap.put(dashboard.getId(), dashboard);
                        }
                        selectedDashboard = dashboards.get(0);
                        dashboardSelectItem.setValueMap(dashboardMenuMap);
                        dashboardSelectItem.setValue(selectedDashboard.getId());
                    }
                }
            });
    }

    /**
     * The metrics list grid is not available on object creation so we must attach later after it has been initialized.
     * @param metricsListGrid
     */
    public void setMetricsListGrid(MetricsTableView.MetricsTableListGrid metricsListGrid) {
        this.metricsListGrid = metricsListGrid;
    }

    private void storeDashboardMetric(Dashboard dashboard, Resource resource, MeasurementDefinition definition) {
        DashboardPortlet dashboardPortlet = new DashboardPortlet(MSG.view_tree_common_contextMenu_resourceGraph(),
            ResourceD3GraphPortlet.KEY, 250);
        dashboardPortlet.getConfiguration().put(
            new PropertySimple(ResourceD3GraphPortlet.CFG_RESOURCE_ID, resource.getId()));
        dashboardPortlet.getConfiguration().put(
            new PropertySimple(ResourceD3GraphPortlet.CFG_DEFINITION_ID, definition.getId()));

        dashboard.addPortlet(dashboardPortlet);

        GWTServiceLookup.getDashboardService().storeDashboard(dashboard, new AsyncCallback<Dashboard>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(),
                    caught);
            }

            public void onSuccess(Dashboard result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_tree_common_contextMenu_saveChartToDashboardSuccessful(result.getName()),
                        Message.Severity.Info));
            }
        });
    }

}
