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
import java.util.HashMap;

import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.AutoFitTextAreaItem;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class BundleResourceDeploymentHistoryListView extends LocatableVLayout {

    private BundleResourceDeployment resourceDeployment;

    public BundleResourceDeploymentHistoryListView(String locatorId, BundleResourceDeployment resourceDeployment) {
        super(locatorId);

        setWidth100();
        setHeight100();
        this.resourceDeployment = resourceDeployment;

    }

    @Override
    protected void onInit() {
        super.onInit();

        ListGrid grid = new LocatableListGrid(this.getLocatorId());
        grid.setWidth100();
        grid.setHeight100();

        ListGridField action = new ListGridField("action", "Action");
        ListGridField message = new ListGridField("info", "Info");
        ListGridField status = new ListGridField("status", "status");

        HashMap<String, String> icons = new HashMap<String, String>();
        icons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        icons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Warning_11.png");
        icons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        icons.put(BundleDeploymentStatus.WARN.name(), "subsystems/bundle/Warning_11.png");
        icons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
        status.setValueIcons(icons);
        status.setValueIconHeight(11);
        status.setWidth(80);

        grid.setCanExpandRecords(true);
        grid.setExpansionMode(ExpansionMode.DETAIL_FIELD);
        grid.setDetailField("message");

        ListGridField details = new ListGridField("attachment", "Details");
        details.setWidth(50);
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

        grid.setFields(action, message, status, details);
        grid.setData(buildRecords());
        addMember(grid);

    }

    private void showDetails(ListGridRecord record) {

        DynamicForm form = new DynamicForm();

        StaticTextItem action = new StaticTextItem("action", "Action");
        StaticTextItem info = new StaticTextItem("info", "Info");
        StaticTextItem category = new StaticTextItem("category", "Category");
        StaticTextItem message = new StaticTextItem("message", "Message");

        AutoFitTextAreaItem detail = new AutoFitTextAreaItem("attachement", "Detail");
        detail.setTitleOrientation(TitleOrientation.TOP);
        detail.setColSpan(2);

        ButtonItem close = new ButtonItem("close", "Close");

        form.setItems(action, info, category, message, detail, close);

        form.editRecord(record);

        final Window window = new Window();
        window.setTitle("Install Details");
        window.setWidth(800);
        window.setHeight(600);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(form);
        window.show();

        close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                window.destroy();
            }
        });
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

            records.add(record);
        }

        return records.toArray(new ListGridRecord[records.size()]);

    }
}
