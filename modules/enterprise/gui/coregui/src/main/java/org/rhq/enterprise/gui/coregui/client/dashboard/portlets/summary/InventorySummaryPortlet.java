/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.InventorySummary;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceBossGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class InventorySummaryPortlet extends LocatableVLayout implements AutoRefreshPortlet {
    private ResourceBossGWTServiceAsync resourceBossService = GWTServiceLookup.getResourceBossService();

    private LocatableDynamicForm form;
    public static final String KEY = MSG.common_title_summary_counts();
    private Timer defaultReloader;

    public InventorySummaryPortlet(String locatorId) {
        super(locatorId);

        loadInventoryViewDiata();
    }

    private void loadInventoryViewDiata() {
        for (Canvas child : getChildren()) {
            child.destroy();
        }
        resourceBossService.getInventorySummaryForLoggedInUser(new AsyncCallback<InventorySummary>() {
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError(MSG.view_portlet_inventory_error1(), throwable);
            }

            public void onSuccess(InventorySummary summary) {
                form = new LocatableDynamicForm("Portlet_Inventory_Summary");
                List<FormItem> formItems = new ArrayList<FormItem>();

                //                HeaderItem headerItem = new HeaderItem("header");
                //                headerItem.setValue("Inventory Summary");
                //                formItems.add(headerItem);

                StaticTextItem platformTotal = createSummaryRow("platformTotal", MSG.common_title_platform_total(),
                    summary.getPlatformCount(), "Inventory/Resources/Platforms");
                formItems.add(platformTotal);

                StaticTextItem serverTotal = createSummaryRow("serverTotal", MSG.common_title_server_total(), summary
                    .getServerCount(), "Inventory/Resources/Servers");
                formItems.add(serverTotal);

                StaticTextItem serviceTotal = createSummaryRow("serviceTotal", MSG.common_title_service_total(),
                    summary.getServiceCount(), "Inventory/Resources/Services");
                formItems.add(serviceTotal);

                StaticTextItem compatibleGroupTotal = createSummaryRow("compatibleGroupTotal", MSG
                    .common_title_compatibleGroups_total(), summary.getCompatibleGroupCount(),
                    "Inventory/Groups/CompatibleGroups");
                formItems.add(compatibleGroupTotal);

                StaticTextItem mixedGroupTotal = createSummaryRow("mixedGroupTotal", MSG
                    .common_title_mixedGroups_total(), summary.getMixedGroupCount(), "Inventory/Groups/MixedGroups");
                formItems.add(mixedGroupTotal);

                StaticTextItem groupDefinitionTotal = createSummaryRow("groupDefinitionTotal", MSG
                    .common_title_group_def_total(), summary.getGroupDefinitionCount(),
                    "Inventory/Groups/DynagroupManager");
                formItems.add(groupDefinitionTotal);

                StaticTextItem avergeMetricsTotal = createSummaryRow("averageMetricsTotal", MSG
                    .common_title_average_metrics(), summary.getScheduledMeasurementsPerMinute(), null);
                formItems.add(avergeMetricsTotal);

                form.setItems(formItems.toArray(new FormItem[formItems.size()]));
                form.setWrapItemTitles(false);
                form.setCellSpacing(5);
                addMember(form);
            }
        });
    }

    private StaticTextItem createSummaryRow(String name, String label, int value, final String viewPath) {
        final StaticTextItem item;
        if (viewPath != null) {
            item = new LinkItem(name);
            item.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    CoreGUI.goToView(viewPath);
                }
            });
        } else {
            item = new StaticTextItem(name);
        }

        item.setTitle(label);
        item.setDefaultValue(value);

        return item;
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        // TODO: Implement this method.
    }

    public Canvas getHelpCanvas() {
        return null; // TODO: Implement this method.
    }

    public DynamicForm getCustomSettingsForm() {
        return null; // TODO: Implement this method.
    }

    /** Custom refresh operation as we are not directly extending Table
     */
    @Override
    public void redraw() {
        super.redraw();
        if (form != null) {
            //destroy form
            form.destroy();
        }
        //now reload the data
        loadInventoryViewDiata();
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();
        private Portlet reference;

        public final Portlet getInstance(String locatorId) {
            // return GWT.create(InventorySummaryView.class);
            if (reference == null) {
                reference = new InventorySummaryPortlet(locatorId);
            }
            return reference;
        }
    }

    @Override
    public void startRefreshCycle() {
        //current setting
        final int retrievedRefreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();
        //cancel previous operation
        if (defaultReloader != null) {
            defaultReloader.cancel();
        }
        if (retrievedRefreshInterval >= MeasurementUtility.MINUTES) {
            defaultReloader = new Timer() {
                public void run() {
                    redraw();
                    //launch again until portlet reference and child references GC.
                    defaultReloader.schedule(retrievedRefreshInterval);
                }
            };
            defaultReloader.schedule(retrievedRefreshInterval);
        }
    }
}
