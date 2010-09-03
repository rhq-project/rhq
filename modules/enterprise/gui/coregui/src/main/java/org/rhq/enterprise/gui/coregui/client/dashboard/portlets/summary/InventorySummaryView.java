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

package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.summary;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.History;
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
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceBossGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class InventorySummaryView extends LocatableVLayout implements Portlet {

    private ResourceBossGWTServiceAsync resourceBossService = GWTServiceLookup.getResourceBossService();

    private DynamicForm form;
    public static final String KEY = "Summary Counts";

    public InventorySummaryView(String locatorId) {
        super(locatorId);

        resourceBossService.getInventorySummaryForLoggedInUser(new AsyncCallback<InventorySummary>() {
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError("Failed to retrieve inventory summary", throwable);
            }

            public void onSuccess(InventorySummary summary) {
                form = new DynamicForm();
                List<FormItem> formItems = new ArrayList<FormItem>();

                //                HeaderItem headerItem = new HeaderItem("header");
                //                headerItem.setValue("Inventory Summary");
                //                formItems.add(headerItem);

                StaticTextItem platformTotal = createSummaryRow("platformTotal", "Platform Total", summary
                    .getPlatformCount());
                formItems.add(platformTotal);

                StaticTextItem serverTotal = createSummaryRow("serverTotal", "Server Total", summary.getServerCount());
                formItems.add(serverTotal);

                StaticTextItem serviceTotal = createSummaryRow("serviceTotal", "Service Total", summary
                    .getServiceCount());
                formItems.add(serviceTotal);

                StaticTextItem compatibleGroupTotal = createSummaryRow("compatibleGroupTotal",
                    "Compatible Group Total", summary.getCompatibleGroupCount());
                formItems.add(compatibleGroupTotal);

                StaticTextItem mixedGroupTotal = createSummaryRow("mixedGroupTotal", "Mixed Group Total", summary
                    .getMixedGroupCount());
                formItems.add(mixedGroupTotal);

                StaticTextItem groupDefinitionTotal = createSummaryRow("groupDefinitionTotal",
                    "Group Definition Total", summary.getGroupDefinitionCount());
                formItems.add(groupDefinitionTotal);

                StaticTextItem avergeMetricsTotal = createSummaryRow("averageMetricsTotal",
                    "Average Metrics per Minute", summary.getScheduledMeasurementsPerMinute());
                formItems.add(avergeMetricsTotal);

                form.setItems(formItems.toArray(new FormItem[formItems.size()]));
                form.setWrapItemTitles(false);
                form.setCellSpacing(5);
                addMember(form);
            }
        });
    }

    private StaticTextItem createSummaryRow(String name, String label, int value) {
        final LinkItem item = new LinkItem(name);
        item.setTitle(label);
        item.setValue(value);
        item.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                // TODO Figure out to where the click events should be navigating
                History.newItem(InventoryView.VIEW_PATH);
            }
        });

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

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            // return GWT.create(InventorySummaryView.class);
            return new InventorySummaryView(locatorId);
        }
    }

}
