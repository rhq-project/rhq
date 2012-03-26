/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components;

import com.google.gwt.user.client.Window;
import com.smartgwt.client.data.DateRange;
import com.smartgwt.client.data.RelativeDate;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.DateRangeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.enterprise.gui.coregui.client.PopupWindow;

import java.util.List;

/**
 * Build a custom Export window based for particular export screens.
 *
 * @author Mike Thompson
 */
public class ExportModalWindow {

    //@todo:pull from message bundle
    private static String BASE_URL = "http://localhost:7080/rest/1/reports/";

    private String reportUrl;

    PopupWindow exportWindow;

    private boolean showDetail;

    // optional Fields

    /**
     * For recent Alerts.
     */
    List<AlertPriority> alertPriorityList;
    /**
     * For Recent Operations.
     */
    List<OperationRequestStatus> operationRequestStatusList;




    private ExportModalWindow(String reportUrl) {
        this.reportUrl = reportUrl;
        createDialogWindow();
    }
    private ExportModalWindow(String reportUrl, boolean showDetail) {
        this.reportUrl = reportUrl;
        this.showDetail = showDetail;
        createDialogWindow();
    }

    public static ExportModalWindow createStandardExportWindow(String reportUrl) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForRecentDrift(String reportUrl) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForRecentAlerts(String reportUrl, List<AlertPriority> alertPriorityList) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        newExportDialog.setAlertPriorityList(alertPriorityList);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForRecentOperations(String reportUrl, List<OperationRequestStatus> operationRequestStatus) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        newExportDialog.setOperationRequestStatusList(operationRequestStatus);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForInventorySummary(String reportUrl) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl, true);
        return newExportDialog;
    }

    private void createDialogWindow() {
        exportWindow = new PopupWindow("exportSettings", null);
        exportWindow.setTitle("Export Settings");

        VLayout dialogLayout = new VLayout();

        HLayout headerLayout = new HLayout();
        headerLayout.setAlign(Alignment.CENTER);
        Label header = new Label();
        header.setContents("Export Settings");
        header.setWidth100();
        header.setHeight(40);
        header.setPadding(10);
        //header.setStyleName("HeaderLabel");
        headerLayout.addMember(header);
        dialogLayout.addMember(headerLayout);

        HLayout formLayout = new HLayout();
        formLayout.setAlign(VerticalAlignment.TOP);

        DynamicForm form = new DynamicForm();

        final SelectItem formatsList = new SelectItem("Format", "Format");
        formatsList.setValueMap("CSV", "XML");
        formatsList.setDefaultValue("CSV");

        CheckboxItem detailCheckboxItem = new CheckboxItem();
        detailCheckboxItem.setTitle("Show Detail");
        detailCheckboxItem.setDisabled(!showDetail);
        detailCheckboxItem.setValue(false);

        DateRangeItem dateRangeItem = new DateRangeItem("dri", "Date Range");
        dateRangeItem.setAllowRelativeDates(true);
        DateRange dateRange = new DateRange();
        dateRange.setRelativeStartDate(new RelativeDate("-1m"));
        dateRange.setRelativeEndDate(RelativeDate.TODAY);
        dateRangeItem.setValue(dateRange);


        form.setItems(formatsList, detailCheckboxItem, dateRangeItem);
        formLayout.addMember(form);
        dialogLayout.addMember(formLayout);

        ToolStrip buttonBar = new ToolStrip();
        buttonBar.setAlign(Alignment.RIGHT);

        IButton finishButton = new IButton("Export", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                exportWindow.hide();
                Window.open(calculateUrl(formatsList.getValueAsString()), "download", null);
            }
        });
        buttonBar.addMember(finishButton);
        dialogLayout.addMember(buttonBar);

        exportWindow.addItem(dialogLayout);
    }

    public void setAlertPriorityList(List<AlertPriority> alertPriorityList) {
        this.alertPriorityList = alertPriorityList;
    }

    public void setOperationRequestStatusList(List<OperationRequestStatus> operationRequestStatusList) {
        this.operationRequestStatusList = operationRequestStatusList;
    }

    public String calculateUrl(String format) {
        return BASE_URL + reportUrl + "." + format.toLowerCase();
    }

    public boolean  isShowDetail(){
        return showDetail;
    }

    public void setShowDetail(boolean showDetail) {
        this.showDetail = showDetail;
    }

    public void show() {
        exportWindow.show();
    }

}
