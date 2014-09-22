/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.bundle.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ErrorMessageWindow;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Greg Hinkle
 */
public class BundleResourceDeploymentHistoryListView extends EnhancedVLayout {

    private BundleResourceDeployment resourceDeployment;
    private HashMap<String, String> statusIcons;
    private ListGrid grid;

    public BundleResourceDeploymentHistoryListView(BundleResourceDeployment resourceDeployment) {
        super();
        setWidth100();
        setHeight100();
        this.resourceDeployment = resourceDeployment;

        statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.PENDING.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Error_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
        // bundle deployment history statuses are success/failure/warn/info - two of which have the same names/icons
        // as bundle-deployment-status. however, there is no "warn" or "info" in bundle-deployment-status, so add them here
        statusIcons.put(BundleResourceDeploymentHistory.Status.WARN.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleResourceDeploymentHistory.Status.INFO.name(), "subsystems/bundle/Info_11.png");
    }

    @Override
    protected void onInit() {
        super.onInit();

        grid = new ListGrid();
        grid.setWidth100();
        grid.setHeight100();
        grid.setSelectionType(SelectionStyle.SINGLE);
        grid.setCanExpandRecords(true);
        grid.setExpansionMode(ExpansionMode.DETAIL_FIELD);
        grid.setDetailField("message");
        grid.setSortField("timestamp");
        grid.setSortDirection(SortDirection.ASCENDING);
        grid.setEmptyMessage(MSG.common_msg_loading());

        ListGridField action = new ListGridField("action", MSG.view_bundle_deploy_action());
        action.setAutoFitWidth(true);
        action.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);

        ListGridField message = new ListGridField("info", MSG.common_severity_info());
        message.setWidth("60%");

        ListGridField user = new ListGridField("user", MSG.common_title_user());
        user.setHidden(true);

        ListGridField timestamp = new ListGridField("timestamp", MSG.common_title_timestamp());
        TimestampCellFormatter.prepareDateField(timestamp);
        timestamp.setWidth("40%");

        ListGridField status = new ListGridField("status", MSG.common_title_status());
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setValueIconWidth(11);
        status.setShowValueIconOnly(true);
        status.setAutoFitWidth(true);
        status.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);

        ListGridField details = new ListGridField("attachment", MSG.common_title_details());
        details.setWidth(50);
        details.setAlign(Alignment.CENTER);
        details.setType(ListGridFieldType.ICON);
        details.setIconHeight(11);
        details.setIconWidth(11);
        details.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<img src=\"images/subsystems/bundle/Details_11.png\"/>";
            }
        });
        details.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent recordClickEvent) {
                showDetails((ListGridRecord) recordClickEvent.getRecord());
            }
        });

        grid.setFields(action, message, timestamp, status, user, details);

        grid.addDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                showDetails(grid.getSelectedRecord());
            }
        });

        addMember(grid);

        loadData();
    }

    private void loadData() {
        BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService(30000);
        bundleService.getBundleResourceDeploymentHistories(resourceDeployment.getId(),
            new AsyncCallback<List<BundleResourceDeploymentHistory>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_tree_loadFailure(), caught);
                    grid.setShowEmptyMessage(false);
                }

                @Override
                public void onSuccess(List<BundleResourceDeploymentHistory> result) {
                    grid.setData(buildRecords(result));
                    grid.setShowEmptyMessage(false);
                }
            });
    }

    private void showDetails(ListGridRecord record) {
        DynamicForm form = new DynamicForm();
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
        StaticTextItem info = new StaticTextItem("info", MSG.common_severity_info());
        StaticTextItem category = new StaticTextItem("category", MSG.common_title_category());

        StaticTextItem message = new StaticTextItem("message", MSG.common_title_message());
        message.setTitleVAlign(VerticalAlignment.TOP);

        TextAreaItem detail = new TextAreaItem("attachment", MSG.common_title_details());
        detail.setTitleVAlign(VerticalAlignment.TOP);
        detail.setMinHeight(100);
        detail.setHeight("100%");
        detail.setWidth("100%");
        detail.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                event.cancel(); // we want the user to interact with the text to copy it, but not to edit it
            }
        });

        form.setItems(timestamp, action, category, user, status, info, message, detail);
        form.editRecord(record);

        Window win = new ErrorMessageWindow(MSG.view_bundle_deploy_installDetails(), form);
        win.setWidth(500);
        win.show();
    }

    private static ListGridRecord[] buildRecords(List<BundleResourceDeploymentHistory> histories) {
        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();

        for (BundleResourceDeploymentHistory history : histories) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("id", history.getId());
            record.setAttribute("action", history.getAction());
            record.setAttribute("info", history.getInfo());

            if (history.getCategory() != null) {
                record.setAttribute("category", history.getCategory().toString());
            }

            record.setAttribute("message", history.getMessage());
            record.setAttribute("attachment", history.getAttachment());
            record.setAttribute("status", history.getStatus().name());
            record.setAttribute("timestamp", new Date(history.getAuditTime()));
            record.setAttribute("user", history.getSubjectName());
            records.add(record);
        }

        return records.toArray(new ListGridRecord[records.size()]);
    }
}
