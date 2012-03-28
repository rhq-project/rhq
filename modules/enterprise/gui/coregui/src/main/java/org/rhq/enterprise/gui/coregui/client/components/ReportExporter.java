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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

import java.util.Set;

/**
 * Exporter for building urls to reports (csv).
 * The reports are RESTful urls opened up in a new window.
 *
 * @author Mike Thompson
 */
public class ReportExporter {

    private static final Messages MSG = CoreGUI.getMessages();

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
                                                              String snapshot, String[] driftCategories, String path) {
        ReportExporter newExportDialog = new ReportExporter(reportUrl);
        newExportDialog.setDriftCategories(driftCategories);
        newExportDialog.setDriftDefinition(definition);
        newExportDialog.setDriftPath(path);
        newExportDialog.setDriftSnapshot(snapshot);
        return newExportDialog;
    }

    public static ReportExporter createExporterForRecentAlerts(String reportUrl, String[] alertPriorityList) {
        ReportExporter newExportDialog = new ReportExporter(reportUrl);
        newExportDialog.setAlertPriorityFilters(alertPriorityList);
        return newExportDialog;
    }

    public static ReportExporter createExporterForRecentOperations(String reportUrl, String[] operationRequestStatuses) {
        ReportExporter newExportDialog = new ReportExporter(reportUrl);
        newExportDialog.setOperationRequestStatusList(operationRequestStatuses);
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

//        if (showAllDetail) {
//            queryString.append("details=").append(form.getValueAsString(DETAILS_FIELD));
//        }
        if(!isEmpty(operationRequestStatuses)){
            StringBuilder operationRequestStatusBuffer = new StringBuilder();
            for (String operationRequestStatus : operationRequestStatuses) {
                operationRequestStatusBuffer.append(operationRequestStatus);
                operationRequestStatusBuffer.append(",");
            }

            queryString.append("operationRequestStatus=").append(operationRequestStatusBuffer.toString());
        }
        if(!isEmpty(alertPriorityFilters)){
            StringBuilder alertsPriorityBuffer = new StringBuilder();
            for (String alertPriority : alertPriorityFilters) {
                alertsPriorityBuffer.append(alertPriority);
                alertsPriorityBuffer.append(",");
            }
            queryString.append("alertPriority=").append(alertsPriorityBuffer.toString());
        }

        // Drift Related
        if(!isEmpty(driftCategories)){
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

    public void export(){
        Window.open(determineUrl(), "download", null);

    }

}
