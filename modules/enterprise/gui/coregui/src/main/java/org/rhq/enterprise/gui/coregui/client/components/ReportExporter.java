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

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;

import java.util.Date;
import java.util.Set;

/**
 * Exporter for building urls to reports (csv).
 * The reports are RESTful urls opened up in a new window.
 *
 * @author Mike Thompson
 */
public class ReportExporter {

    private static final String BASE_URL = GWT.getHostPageBaseURL().replace("coregui/","")+"rest/1/reports/";
    private static final String FORMAT = "csv"; //CSV is all we need right now

    private String reportUrl;

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

    // Date filtering
    Date startDate;
    Date endDate;


    /**
     * Private constructors to force use of static factory creation pattern.
     * @param reportUrl
     */
    private ReportExporter(String reportUrl) {

        this.reportUrl = reportUrl;
    }


    private ReportExporter(String reportUrl, boolean showDetail, Set<Integer> resourceTypeIds) {
        this.reportUrl = reportUrl;
        this.showAllDetail = showDetail;
        this.resourceTypeIds = resourceTypeIds;
    }

    public static ReportExporter createStandardExporter(String reportUrl) {
        return new ReportExporter(reportUrl);
    }


    public static ReportExporter createExporterForRecentDrift(String reportUrl, String definition,
                                                              String snapshot, String[] driftCategories, String path,
                                                              Date fromDate, Date toDate) {
        ReportExporter newExporter = new ReportExporter(reportUrl);
        newExporter.setDriftCategories(driftCategories);
        newExporter.setDriftDefinition(definition);
        newExporter.setDriftPath(path);
        newExporter.setDriftSnapshot(snapshot);
        newExporter.setStartDate(fromDate);
        newExporter.setEndDate(toDate);
        return newExporter;
    }

    public static ReportExporter createExporterForRecentAlerts(String reportUrl, String[] alertPriorityList, Date fromDate, Date toDate) {
        ReportExporter newExportDialog = new ReportExporter(reportUrl);
        newExportDialog.setAlertPriorityFilters(alertPriorityList);
        newExportDialog.setStartDate(fromDate);
        newExportDialog.setEndDate(toDate);
        return newExportDialog;
    }

    public static ReportExporter createExporterForRecentOperations(String reportUrl, String[] operationRequestStatuses, Date fromDate, Date toDate) {
        ReportExporter newExportDialog = new ReportExporter(reportUrl);
        newExportDialog.setOperationRequestStatusList(operationRequestStatuses);
        newExportDialog.setStartDate(fromDate);
        newExportDialog.setEndDate(toDate);
        return newExportDialog;
    }

    public static ReportExporter createExporterForInventorySummary(String reportUrl, boolean showAllDetails,
                                                                   Set<Integer> resourceTypeIdsForExport) {
        return new ReportExporter(reportUrl, showAllDetails, resourceTypeIdsForExport);
    }


    public void setAlertPriorityFilters(String[] alertPriorityFilters) {
        this.alertPriorityFilters = alertPriorityFilters;
    }

    public void setOperationRequestStatusList(String[] operationRequestStatuses) {
        this.operationRequestStatuses = operationRequestStatuses;
    }

    public String determineUrl() {
        StringBuilder queryString = new StringBuilder();

        if (showAllDetail) {
            queryString.append("showAllDetails=").append("true").append("&");
        } else if (null != resourceTypeIds && !resourceTypeIds.isEmpty()) {
            queryString.append("resourceTypeId=").append(StringUtility.toString(resourceTypeIds)).append("&");
        }

        if(!isEmpty(operationRequestStatuses)){
            StringBuilder operationRequestStatusBuffer = new StringBuilder();
            for (String operationRequestStatus : operationRequestStatuses) {
                operationRequestStatusBuffer.append(operationRequestStatus);
                operationRequestStatusBuffer.append(",");
            }

            queryString.append("operationRequestStatus=").append(operationRequestStatusBuffer.toString()).append("&");
        }
        if(!isEmpty(alertPriorityFilters)){
            StringBuilder alertsPriorityBuffer = new StringBuilder();
            for (String alertPriority : alertPriorityFilters) {
                alertsPriorityBuffer.append(alertPriority);
                alertsPriorityBuffer.append(",");
            }
            queryString.append("alertPriority=").append(alertsPriorityBuffer.toString()).append("&");
        }

        // Drift Related
        if(!isEmpty(driftCategories)){
            StringBuilder driftCategoriesBuffer = new StringBuilder();
            for (String category : driftCategories) {
                driftCategoriesBuffer.append(category).append(",");
            }
            queryString.append("categories=").append(driftCategoriesBuffer.toString()).append("&");
        }
        if (driftDefinition != null) {
            queryString.append("definition=").append(driftDefinition).append("&");
        }
        if (driftPath != null) {
            queryString.append("path=").append(driftDefinition).append("&");
        }
        if (driftSnapshot != null) {
            queryString.append("snapshot=").append(driftSnapshot).append("&");
        }

        // to/from Dates
        if(startDate != null){
            queryString.append("startTime=").append(startDate.getTime()).append("&");
        }
        if(endDate != null){
            queryString.append("endTime=").append(endDate.getTime()).append("&");
        }

        return URL.encode(BASE_URL + reportUrl + "." + FORMAT  + "?"+queryString);
    }

    private boolean isEmpty(String[] array) {
        return array == null || array.length == 0;
    }

    public boolean isShowAllDetail(){
        return showAllDetail;
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

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void export(){
        String reportUrl = determineUrl();
        Log.info("Opening Export CSV report on url: "+reportUrl);
        Window.open(reportUrl, "download", null);

    }

}
