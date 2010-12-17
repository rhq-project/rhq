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
package org.rhq.enterprise.gui.coregui.client.bundle.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.AutoFitTextAreaItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 */
public class BundleResourceDeploymentHistoryListView extends LocatableVLayout {

    private BundleResourceDeployment resourceDeployment;
    private HashMap<String, String> statusIcons;

    public BundleResourceDeploymentHistoryListView(String locatorId, BundleResourceDeployment resourceDeployment) {
        super(locatorId);
        setWidth100();
        setHeight100();
        this.resourceDeployment = resourceDeployment;

        statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Error_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.WARN.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
    }

    @Override
    protected void onInit() {
        super.onInit();

        final ListGrid grid = new LocatableListGrid(this.getLocatorId());
        grid.setWidth100();
        grid.setHeight100();
        grid.setSelectionType(SelectionStyle.SINGLE);
        grid.setCanExpandRecords(true);
        grid.setExpansionMode(ExpansionMode.DETAIL_FIELD);
        grid.setDetailField("message");

        ListGridField action = new ListGridField("action", MSG.view_bundle_deploy_action());
        action.setAutoFitWidth(true);
        action.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);

        ListGridField message = new ListGridField("info", MSG.common_title_info());
        message.setWidth("60%");

        ListGridField user = new ListGridField("user", MSG.common_title_user());
        user.setHidden(true);

        ListGridField timestamp = new ListGridField("timestamp", MSG.common_title_timestamp());
        timestamp.setWidth("40%");

        ListGridField status = new ListGridField("status", MSG.common_title_status());
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setValueIconWidth(11);
        status.setAutoFitWidth(true);
        status.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);

        ListGridField details = new ListGridField("attachment", MSG.common_title_details());
        details.setWidth(50);
        details.setAlign(Alignment.CENTER);
        details.setType(ListGridFieldType.ICON);
        details.setIconHeight(11);
        details.setIconWidth(11);
        details.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<img src=\"images/subsystems/bundle/Details_11.png\"/>";
            }
        });
        details.addRecordClickHandler(new RecordClickHandler() {
            public void onRecordClick(RecordClickEvent recordClickEvent) {
                showDetails((ListGridRecord) recordClickEvent.getRecord());
            }
        });

        grid.setFields(action, message, timestamp, status, user, details);
        grid.setData(buildRecords());

        grid.addDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                showDetails(grid.getSelectedRecord());
            }
        });

        addMember(grid);
    }

    private void showDetails(ListGridRecord record) {
        DynamicForm form = new LocatableDynamicForm(extendLocatorId("detailsForm"));
        form.setHeight100();
        form.setWidth100();
        form.setPadding(20);

        StaticTextItem status = new StaticTextItem("status", MSG.common_title_status());
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setValueIconWidth(11);
        status.setShowValueIconOnly(true);

        StaticTextItem user = new StaticTextItem("user", MSG.common_title_user());

        StaticTextItem timestamp = new StaticTextItem("timestamp", MSG.common_title_timestamp());
        timestamp.setDateFormatter(DateDisplayFormat.TOLOCALESTRING);

        StaticTextItem action = new StaticTextItem("action", MSG.view_bundle_deploy_action());
        StaticTextItem info = new StaticTextItem("info", MSG.common_title_info());
        StaticTextItem category = new StaticTextItem("category", MSG.common_title_category());

        StaticTextItem message = new StaticTextItem("message", MSG.common_title_message());
        message.setTitleVAlign(VerticalAlignment.TOP);

        AutoFitTextAreaItem detail = new AutoFitTextAreaItem("attachment", MSG.common_title_details());
        detail.setTitleVAlign(VerticalAlignment.TOP);

        form.setItems(timestamp, action, category, user, status, info, message, detail);
        form.editRecord(record);

        final Window window = new LocatableWindow(extendLocatorId("detailsWin"));
        window.setTitle(MSG.view_bundle_deploy_installDetails());
        window.setAutoSize(true);
        window.setWidth(500);
        window.setAutoCenter(true);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.addItem(form);
        window.setShowMinimizeButton(false);
        window.setShowMaximizeButton(true);
        window.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClientEvent event) {
                window.destroy();
            }
        });
        window.show();
    }

    public ListGridRecord[] buildRecords() {
        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (BundleResourceDeploymentHistory step : resourceDeployment.getBundleResourceDeploymentHistories()) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("id", step.getId());
            record.setAttribute("action", step.getAction());
            record.setAttribute("info", step.getInfo());

            if (step.getCategory() != null) {
                record.setAttribute("category", step.getCategory().toString());
            }

            record.setAttribute("message", step.getMessage());
            record.setAttribute("attachment", step.getAttachment());
            record.setAttribute("status", step.getStatus().name());
            record.setAttribute("timestamp", new Date(step.getAuditTime()));
            record.setAttribute("user", step.getSubjectName());
            records.add(record);
        }

        return records.toArray(new ListGridRecord[records.size()]);
    }
}
