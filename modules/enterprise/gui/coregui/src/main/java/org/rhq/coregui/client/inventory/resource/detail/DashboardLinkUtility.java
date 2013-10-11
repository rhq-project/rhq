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
package org.rhq.coregui.client.inventory.resource.detail;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.dashboard.portlets.inventory.resource.graph.ResourceD3GraphPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.preferences.MeasurementUserPreferences;
import org.rhq.coregui.client.util.preferences.UserPreferences;

/**
 * Utility Class to build menus for linking to the Dashboard.
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 * @author Mike Thompson
 */
public class DashboardLinkUtility {
    final static Messages MSG = CoreGUI.getMessages();

    private DashboardLinkUtility() {
    }

    public static MenuItem buildMetricsMenu(final ResourceType resourceType, final Resource resource, String label) {

        MenuItem measurements = new MenuItem(label);
        final Menu measurementsSubMenu = new Menu();

        DashboardCriteria criteria = new DashboardCriteria();
        GWTServiceLookup.getDashboardService().findDashboardsByCriteria(criteria,
            new AsyncCallback<PageList<Dashboard>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_contextMenu_loadFailed_dashboard(),
                        caught);
                }

                public void onSuccess(PageList<Dashboard> result) {
                    //sort the display items alphabetically
                    TreeSet<String> ordered = new TreeSet<String>();
                    Map<String, MeasurementDefinition> definitionMap = new HashMap<String, MeasurementDefinition>();
                    for (MeasurementDefinition m : resourceType.getMetricDefinitions()) {
                        ordered.add(m.getDisplayName());
                        definitionMap.put(m.getDisplayName(), m);
                    }

                    for (String displayName : ordered) {
                        final MeasurementDefinition def = definitionMap.get(displayName);
                        //only add menu items for Measurement
                        if (def.getDataType().equals(DataType.MEASUREMENT)) {
                            MenuItem defItem = new MenuItem(def.getDisplayName());
                            measurementsSubMenu.addItem(defItem);
                            Menu defSubItem = new Menu();
                            defItem.setSubmenu(defSubItem);

                            for (final Dashboard d : result) {
                                MenuItem addToDBItem = new MenuItem(MSG
                                    .view_tree_common_contextMenu_addChartToDashboard(d.getName()));
                                defSubItem.addItem(addToDBItem);

                                addToDBItem.addClickHandler(new ClickHandler() {

                                    public void onClick(MenuItemClickEvent menuItemClickEvent) {
                                        DashboardPortlet p = new DashboardPortlet(MSG
                                            .view_tree_common_contextMenu_resourceGraph(), ResourceD3GraphPortlet.KEY,
                                            250);
                                        p.getConfiguration()
                                            .put(
                                                new PropertySimple(ResourceD3GraphPortlet.CFG_RESOURCE_ID, resource
                                                    .getId()));
                                        p.getConfiguration().put(
                                            new PropertySimple(ResourceD3GraphPortlet.CFG_DEFINITION_ID, def.getId()));

                                        d.addPortlet(p);

                                        GWTServiceLookup.getDashboardService().storeDashboard(d,
                                            new AsyncCallback<Dashboard>() {

                                                public void onFailure(Throwable caught) {
                                                    CoreGUI.getErrorHandler().handleError(
                                                        MSG.view_tree_common_contextMenu_saveChartToDashboardFailure(),
                                                        caught);
                                                }

                                                public void onSuccess(Dashboard result) {
                                                    CoreGUI
                                                        .getMessageCenter()
                                                        .notify(
                                                            new Message(
                                                                MSG.view_tree_common_contextMenu_saveChartToDashboardSuccessful(result
                                                                    .getName()), Message.Severity.Info));
                                                }
                                            });

                                    }

                                });

                            }

                            //add new menu item for adding current graphable element to view if on Monitor/Graphs tab
                            String currentViewPath = History.getToken();
                            if (currentViewPath.contains("Monitoring/Metrics")) {
                                MenuItem addGraphItem = new MenuItem(MSG.common_title_add_graph_to_view());
                                defSubItem.addItem(addGraphItem);

                                addGraphItem.addClickHandler(new ClickHandler() {
                                    public void onClick(MenuItemClickEvent menuItemClickEvent) {
                                        //generate javascript to call out to.
                                        //Ex. menuLayers.hide();addMetric('${metric.resourceId},${metric.scheduleId}')
                                        if (getScheduleDefinitionId(resource, def.getName()) > -1) {
                                            final String resourceGraphElements = resource.getId() + ","
                                                + getScheduleDefinitionId(resource, def.getName());

                                            //Once, the portal-war will be rewritten to GWT and operations performed
                                            //within the iframe + JSF will update the user preferences, the following
                                            //2 lines could be uncommented and the lines below them refactorized
                                            //MeasurementUserPreferences measurementPreferences = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
                                            //String selectedView = measurementPreferences.getSelectedView(String.valueOf(resource.getId()));

                                            final int sid = UserSessionManager.getSessionSubject().getId();
                                            SubjectCriteria c = new SubjectCriteria();
                                            c.addFilterId(sid);

                                            GWTServiceLookup.getSubjectService().findSubjectsByCriteria(c,
                                                new AsyncCallback<PageList<Subject>>() {
                                                    public void onSuccess(PageList<Subject> result) {
                                                        if (result.size() > 0) {
                                                            UserPreferences uPreferences = new UserPreferences(result
                                                                .get(0));
                                                            MeasurementUserPreferences mPreferences = new MeasurementUserPreferences(
                                                                uPreferences);
                                                            String selectedView = mPreferences.getSelectedView(String
                                                                .valueOf(resource.getId()));

                                                            addNewMetric(String.valueOf(resource.getId()),
                                                                selectedView, resourceGraphElements);
                                                        } else {
                                                            Log.warn("DashboardLinkUtility: Error obtaining subject with id:" + sid);
                                                        }
                                                    }

                                                    public void onFailure(Throwable caught) {
                                                        Log.warn("DashboardLinkUtility: Error obtaining subject with id:" + sid, caught);
                                                    }
                                                });
                                        }
                                    }
                                });
                            }
                        }
                    }

                }
            });
        measurements.setSubmenu(measurementsSubMenu);
        return measurements;
    }

    /** Locate the specific schedule definition using the definition identifier.
     */
    private static int getScheduleDefinitionId(Resource resource, String definitionName) {
        int id = -1;
        if (resource.getSchedules() != null) {
            boolean located = false;
            MeasurementSchedule[] schedules = new MeasurementSchedule[resource.getSchedules().size()];
            resource.getSchedules().toArray(schedules);
            for (int i = 0; (!located && i < resource.getSchedules().size()); i++) {
                MeasurementSchedule schedule = schedules[i];
                MeasurementDefinition definition = schedule.getDefinition();
                if ((definition != null) && definition.getName().equals(definitionName)) {
                    located = true;
                    id = schedule.getId();
                }
            }
        }
        return id;
    }

    private static void addNewMetric(String id, String selectedView, String resourceGraphElements) {
        //construct portal.war url to access
        String baseUrl = "/portal/resource/common/monitor/visibility/IndicatorCharts.do";
        baseUrl += "?id=" + id;
        baseUrl += "&view=" + selectedView;
        baseUrl += "&action=addChart&metric=" + resourceGraphElements;
        final String url = baseUrl;
        //initiate HTTP request
        final RequestBuilder b = new RequestBuilder(RequestBuilder.GET, baseUrl);

        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {
                    Log.trace("Successfully submitted request to add graph to view:" + url);

                    //kick off a page reload.
                    String currentViewPath = History.getToken();
                    CoreGUI.goToView(currentViewPath, true);
                }

                @Override
                public void onError(Request request, Throwable t) {
                    Log.trace("Error adding Metric:" + url, t);
                }
            });
            b.send();
        } catch (RequestException e) {
            Log.warn("Error adding Metric:" + url, e);
        }

    }
}
