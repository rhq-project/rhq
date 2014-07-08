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
package org.rhq.coregui.client.components;

import static com.smartgwt.client.data.RelativeDate.END_OF_TODAY;
import static com.smartgwt.client.data.RelativeDate.START_OF_TODAY;
import static com.smartgwt.client.types.RelativeDateRangePosition.END;
import static com.smartgwt.client.types.RelativeDateRangePosition.START;

import java.util.Date;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.smartgwt.client.util.DateUtil;

import org.rhq.coregui.client.util.Log;
/**
 * Exporter for building urls to reports (csv).
 * The reports are RESTful urls opened up in a new window.
 *
 * @author Mike Thompson
 */
public class ReportExporter {

    //private static final String BASE_URL = GWT.getHostPageBaseURL().replace("coregui/","")+"rest/1/reports/";
    private static final String BASE_URL = "/coregui/reports/";
    private static final String FORMAT = "csv"; //CSV is all we need right now

    private String reportUrl;
    private StringBuilder queryString;


    // optional Fields

    /**
     * For recent Alerts.
     */
    String[] alertPriorityFilters;
    /**
     * For Recent Operations.
     */
    String[] operationRequestStatuses;

    String resourceTypeId;
    String version;
    
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


    public static ReportExporter createStandardExporter(String reportUrl) {
        return new ReportExporter(reportUrl);
    }


    public static ReportExporter createExporterForRecentDrift(String reportUrl, String definition,
                                                              String snapshot, String[] driftCategories, String path,
                                                              Integer startDateOffset , Integer endDateOffset) {
        ReportExporter newExporter = new ReportExporter(reportUrl);
        newExporter.setDriftCategories(driftCategories);
        newExporter.setDriftDefinition(definition);
        newExporter.setDriftPath(path);
        newExporter.setDriftSnapshot(snapshot);
        newExporter.setStartDate(addDateOffsetToNow(startDateOffset));
        newExporter.setEndDate(addDateOffsetToNow(endDateOffset));
        return newExporter;
    }

    public static ReportExporter createExporterForRecentAlerts(String reportUrl, String[] alertPriorityList, Integer startDateOffset, Integer endDateOffset) {
        ReportExporter newExportDialog = new ReportExporter(reportUrl);
        newExportDialog.setAlertPriorityFilters(alertPriorityList);
        newExportDialog.setStartDate(addDateOffsetToNow(startDateOffset));
        newExportDialog.setEndDate(addDateOffsetToNow(endDateOffset));
        return newExportDialog;
    }

    public static ReportExporter createExporterForRecentOperations(String reportUrl, String[] operationRequestStatuses, Integer startDateOffset, Integer endDateOffset) {
        ReportExporter newExportDialog = new ReportExporter(reportUrl);
        newExportDialog.setOperationRequestStatusList(operationRequestStatuses);
        newExportDialog.setStartDate(addDateOffsetToNow(startDateOffset));
        newExportDialog.setEndDate(addDateOffsetToNow(endDateOffset));
        return newExportDialog;
    }

    public static ReportExporter createExporterForInventorySummary(String reportUrl) {
        return new ReportExporter(reportUrl);
    }

    public static ReportExporter createExporterForInventorySummary(String reportUrl, String resourceTypeId,
        String version) {
        ReportExporter exporter = new ReportExporter(reportUrl);
        exporter.resourceTypeId = resourceTypeId;
        exporter.version = version;
        return exporter;
    }

    private static Date addDateOffsetToNow(final Integer dateOffset){
        if(dateOffset == null || dateOffset.equals(new Integer(0))){
            return null;
        } else {
            Date now = new Date();
            CalendarUtil.addDaysToDate(now, dateOffset);
            return now;
        }
    }


    public void setAlertPriorityFilters(String[] alertPriorityFilters) {
        this.alertPriorityFilters = alertPriorityFilters;
    }

    public void setOperationRequestStatusList(String[] operationRequestStatuses) {
        this.operationRequestStatuses = operationRequestStatuses;
    }

    private String buildUrl() {
        buildQueryParameters();

        // trim the last "&" off the url if exists
        final String cleanQueryString = queryString.toString().endsWith("&") ? queryString.substring(0,queryString.toString().length() -1)  : queryString.toString();
        final String queryStringNotEndingWithQuestionMark = cleanQueryString.endsWith("?") ? cleanQueryString.substring(0,cleanQueryString.length() -1)  : cleanQueryString;
        return URL.encode(BASE_URL + reportUrl + "." + FORMAT  + "?"+  queryStringNotEndingWithQuestionMark);
    }

    private void buildQueryParameters() {
        queryString = new StringBuilder();

        if (null != resourceTypeId && !resourceTypeId.isEmpty()) {
            addQueryParameter("resourceTypeId", resourceTypeId);
        }

        if (null != version && !version.isEmpty()) {
            addQueryParameter("version", version);
        }

        if(!isEmpty(operationRequestStatuses)){
            StringBuilder operationRequestStatusBuffer = new StringBuilder();
            for (String operationRequestStatus : operationRequestStatuses) {
                operationRequestStatusBuffer.append(operationRequestStatus);
                operationRequestStatusBuffer.append(",");
            }
            addQueryParameter("status", operationRequestStatusBuffer.toString().substring(0,operationRequestStatusBuffer.toString().length() -1));
        }

        if(!isEmpty(alertPriorityFilters)){
            StringBuilder alertsPriorityBuffer = new StringBuilder();
            for (String alertPriority : alertPriorityFilters) {
                alertsPriorityBuffer.append(alertPriority);
                alertsPriorityBuffer.append(",");
            }
            addQueryParameter("alertPriority", alertsPriorityBuffer.toString().substring(0,alertsPriorityBuffer.toString().length() -1));
        }

        // Drift Related
        if(!isEmpty(driftCategories)){
            StringBuilder driftCategoriesBuffer = new StringBuilder();
            for (String category : driftCategories) {
                driftCategoriesBuffer.append(category).append(",");
            }
            addQueryParameter("categories", driftCategoriesBuffer.toString());
        }

        addQueryParameter("definition", driftDefinition);
        addQueryParameter("path", driftPath);
        addQueryParameter("snapshot", driftSnapshot);

        // to/from Dates
        addQueryParameter("startTime", startDate);
        addQueryParameter("endTime", endDate);
    }

    private void addQueryParameter(String parameterName, String parameterValue){
        if(parameterValue != null){
           queryString.append(parameterName).append("=").append(parameterValue).append("&");
        }
    }
    private void addQueryParameter(String parameterName, Date parameterValue){
        if(parameterValue != null){
            addQueryParameter(parameterName, String.valueOf(parameterValue.getTime()));
        }
    }

    private boolean isEmpty(String[] array) {
        return array == null || array.length == 0;
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
        this.startDate = (startDate != null) ?  DateUtil.getAbsoluteDate(START_OF_TODAY, startDate, START) : null;
    }

    public void setEndDate(Date endDate) {
        this.endDate = (endDate != null ) ? DateUtil.getAbsoluteDate(END_OF_TODAY, endDate, END) : null;
    }


    /**
     * Using the url built in buildUrl() open the RESTful CSV report in a new window.
     */
    public void export(){
        String reportUrl = buildUrl();
        Log.info("Opening Export CSV report on url: " + reportUrl);
        Window.open(reportUrl, "download", null);

    }

}
