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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.core.domain.resource.InventorySummary;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceBossGWTServiceAsync;

import java.util.ArrayList;
import java.util.List;

public class SummaryCountsView extends VLayout {

    private ResourceBossGWTServiceAsync resourceBossService = GWTServiceLookup.getResourceBossService();

    private DynamicForm form;

    public SummaryCountsView() {
        resourceBossService.getInventorySummaryForLoggedInUser(new AsyncCallback<InventorySummary>() {
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError("Failed to retrieve inventory summary", throwable);                    
            }

            public void onSuccess(InventorySummary summary) {
                form = new DynamicForm();
                List<FormItem> formItems = new ArrayList<FormItem>();

                StaticTextItem platformTotal = createSummaryRow("platformTotal", "Platform Total",
                    summary.getPlatformCount());
                formItems.add(platformTotal);

                StaticTextItem serverTotal = createSummaryRow("serverTotal", "Server Total", summary.getServerCount());
                formItems.add(serverTotal);

                StaticTextItem serviceTotal = createSummaryRow("serviceTotal", "Service Total",
                    summary.getServiceCount());
                formItems.add(serviceTotal);

                StaticTextItem compatibleGroupTotal = createSummaryRow("compatibleGroupTotal", "Compatible Group Total",
                    summary.getCompatibleGroupCount());
                formItems.add(compatibleGroupTotal);

                StaticTextItem mixedGroupTotal = createSummaryRow("mixedGroupTotal", "Mixed Group Total",
                    summary.getMixedGroupCount());
                formItems.add(mixedGroupTotal);

                StaticTextItem groupDefinitionTotal = createSummaryRow("groupDefinitionTotal", "Group Definition Total",
                    summary.getGroupDefinitionCount());
                formItems.add(groupDefinitionTotal);

                StaticTextItem avergeMetricsTotal = createSummaryRow("averageMetricsTotal",
                    "Average Metrics per Minute", summary.getScheduledMeasurementsPerMinute());
                formItems.add(avergeMetricsTotal);

                form.setItems(formItems.toArray(new FormItem[formItems.size()]));
                form.setWrapItemTitles(false);
                form.setCellSpacing(15);
                addMember(form);
            }
        });
    }

    private StaticTextItem createSummaryRow(String name, String label, int value) {
        final LinkItem item = new LinkItem(name);
        item.setTitle(label);
        item.setValue(value);

        return item;
    }
}
