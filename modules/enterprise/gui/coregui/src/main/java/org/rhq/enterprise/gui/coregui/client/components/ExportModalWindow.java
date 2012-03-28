/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
package org.rhq.enterprise.gui.coregui.client.components;

import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.smartgwt.client.data.DateRange;
import com.smartgwt.client.data.RelativeDate;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.DateRangeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.PopupWindow;

/**
 * Build a custom Export window based for particular export screens.
 *
 * @author Mike Thompson
 */
public class ExportModalWindow {

    private static final Messages MSG = CoreGUI.getMessages();

    private static final String BASE_URL = GWT.getHostPageBaseURL().replace("coregui/","")+"rest/1/reports/";

    private static final String FORMAT_FIELD = "format";
    private static final String DETAILS_FIELD = "details";
    private static final String DATE_RANGE_FIELD = "dri";

    private String reportUrl;

    private PopupWindow exportWindow;

    private boolean showAllDetail;

    // optional Fields

    /**
     * For recent Alerts.
     */
    String[] alertPriorityFilters;
    /**
     * For Recent Operations.
     */
    String[] operationRequestStatuses;

    Set<Integer> resourceTypeIds;
    
    // for Recent Drift Report
    String driftDefinition;
    String driftSnapshot;
    String[] driftCategories;
    String driftPath;


    private ExportModalWindow(String reportUrl) {
        this.reportUrl = reportUrl;
        createDialogWindow();
    }
    private ExportModalWindow(String reportUrl, boolean showDetail, Set<Integer> resourceTypeIds) {
        this.reportUrl = reportUrl;
        this.showAllDetail = showDetail;
        this.resourceTypeIds = resourceTypeIds;
        createDialogWindow();
    }

    public static ExportModalWindow createStandardExportWindow(String reportUrl) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForRecentDrift(String reportUrl, String definition,
                                                                     String snapshot, String[] driftCategories, String path) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        newExportDialog.setDriftCategories(driftCategories);
        newExportDialog.setDriftDefinition(definition);
        newExportDialog.setDriftPath(path);
        newExportDialog.setDriftSnapshot(snapshot);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForRecentAlerts(String reportUrl, String[] alertPriorityList) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        newExportDialog.setAlertPriorityFilters(alertPriorityList);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForRecentOperations(String reportUrl, String[] operationRequestStatuses ) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl);
        newExportDialog.setOperationRequestStatusList(operationRequestStatuses);
        return newExportDialog;
    }

    public static ExportModalWindow createExportWindowForInventorySummary(String reportUrl, boolean showAllDetails,
        Set<Integer> resourceTypeIdsForExport) {
        ExportModalWindow newExportDialog = new ExportModalWindow(reportUrl, showAllDetails, resourceTypeIdsForExport);
        return newExportDialog;
    }

    private void createDialogWindow() {
        exportWindow = new PopupWindow("exportSettings", null);
        exportWindow.setTitle("Export Dialog");

        VLayout dialogLayout = new VLayout();

        HLayout headerLayout = new HLayout();
        headerLayout.setHeight(25);
        headerLayout.setAlign(Alignment.CENTER);
        TitleBar titleBar = new TitleBar(exportWindow, "Export Settings", IconEnum.REPORT.getIcon24x24Path());
        headerLayout.addMember(titleBar);
        dialogLayout.addMember(headerLayout);

        HLayout formLayout = new HLayout();
        formLayout.setAlign(VerticalAlignment.TOP);

        final DynamicForm form = new DynamicForm();

        final SelectItem formatsList = new SelectItem(FORMAT_FIELD, "Format");
        LinkedHashMap<String, String> formats = new LinkedHashMap<String, String>();
        formats.put("csv", "CSV");
        formats.put("xml", "XML");
        formatsList.setValueMap(formats);
        formatsList.setDefaultValue("csv");

        CheckboxItem detailCheckboxItem = new CheckboxItem(DETAILS_FIELD, "Show Detail");
        detailCheckboxItem.setVisible(showAllDetail);
        detailCheckboxItem.setValue(false);

        DateRangeItem dateRangeItem = new DateRangeItem(DATE_RANGE_FIELD, "Date Range");
        dateRangeItem.setAllowRelativeDates(true);
        DateRange dateRange = new DateRange();
        dateRange.setRelativeStartDate(new RelativeDate("-1m"));
        dateRange.setRelativeEndDate(RelativeDate.TODAY);
        dateRangeItem.setValue(dateRange);


        form.setItems(new SpacerItem(),formatsList, detailCheckboxItem, new SpacerItem(),new SpacerItem(), dateRangeItem);
        formLayout.addMember(form);
        dialogLayout.addMember(formLayout);

        ToolStrip buttonBar = new ToolStrip();
        buttonBar.setAlign(Alignment.RIGHT);
        buttonBar.setPadding(5);
        buttonBar.setMembersMargin(10);

        IButton cancelButton = new IButton("Cancel", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                exportWindow.hide();
            }
        });
        buttonBar.addMember(cancelButton);

        IButton finishButton = new IButton("Export", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                exportWindow.hide();
                Window.open(calculateUrl(form), "download", null);
            }
        });
        buttonBar.addMember(finishButton);

        dialogLayout.addMember(buttonBar);

        exportWindow.addItem(dialogLayout);
    }

    public void setAlertPriorityFilters(String[] alertPriorityFilters) {
        this.alertPriorityFilters = alertPriorityFilters;
    }

    public void setOperationRequestStatusList(String[] operationRequestStatuses) {
        this.operationRequestStatuses = operationRequestStatuses;
    }

    public String calculateUrl(DynamicForm form) {
        String format = form.getValueAsString(FORMAT_FIELD);
        StringBuilder queryString = new StringBuilder();

        if (showAllDetail) {
            queryString.append("details=").append(form.getValueAsString(DETAILS_FIELD));
        }
        if(operationRequestStatuses != null){
            StringBuilder operationRequestStatusBuffer = new StringBuilder();
            for (String operationRequestStatus : operationRequestStatuses) {
                operationRequestStatusBuffer.append(operationRequestStatus);
                operationRequestStatusBuffer.append(",");
            }

            queryString.append("operationRequestStatus=").append(operationRequestStatusBuffer.toString());
        }
        if(alertPriorityFilters != null){
            StringBuilder alertsPriorityBuffer = new StringBuilder();
            for (String alertPriority : alertPriorityFilters) {
                alertsPriorityBuffer.append(alertPriority);
                alertsPriorityBuffer.append(",");
            }
            queryString.append("alertPriority=").append(alertsPriorityBuffer.toString());
        }

        // Drift Related
        if(driftCategories != null){
            StringBuilder driftCategoriesBuffer = new StringBuilder();
            for (String category : driftCategories) {
                driftCategoriesBuffer.append(category).append(",");
            }
            queryString.append("categories=").append(driftCategoriesBuffer.toString());
        }
        if (driftDefinition != null) {
            queryString.append("definition=").append(driftDefinition);
        }
        if (driftPath != null) {
            queryString.append("path=").append(driftDefinition);
        }
        if (driftSnapshot != null) {
            queryString.append("snapshot=").append(driftSnapshot);
        }

        
        return URL.encode(BASE_URL + reportUrl + "." + format  + "?"+queryString);
    }

    public boolean isShowAllDetail(){
        return showAllDetail;
    }

    public void setShowAllDetail(boolean showAllDetail) {
        this.showAllDetail = showAllDetail;
    }

    public void setDriftDefinition(String driftDefinition) {
        this.driftDefinition = driftDefinition;
    }

    public void setDriftSnapshot(String driftSnapshot) {
        this.driftSnapshot = driftSnapshot;
    }

    public void setDriftCategories(String[] driftCategories) {
        this.driftCategories = driftCategories;
    }

    public void setDriftPath(String driftPath) {
        this.driftPath = driftPath;
    }

    public void show() {
        exportWindow.show();
    }

}
