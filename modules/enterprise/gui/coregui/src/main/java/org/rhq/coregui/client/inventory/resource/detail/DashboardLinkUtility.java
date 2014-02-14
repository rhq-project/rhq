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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.dashboard.portlets.inventory.resource.graph.ResourceD3GraphPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.message.Message;

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
                        }
                    }

                }
            });
        measurements.setSubmenu(measurementsSubMenu);
        return measurements;
    }
}
