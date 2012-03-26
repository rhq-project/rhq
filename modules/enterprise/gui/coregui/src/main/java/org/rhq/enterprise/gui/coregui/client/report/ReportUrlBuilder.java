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
package org.rhq.enterprise.gui.coregui.client.report;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.operation.OperationRequestStatus;

import java.util.Date;
import java.util.List;

/**
 * Typesafe url builder for the reports module.
 *
 * <code>  ReportUrlBuilder urlBuilder  = new ReportUrlBuilder.Builder(reportUrl, "csv").startDate(new Date()).endDate(new Date()).showDetail(true).build();
 * return URL.encode(urlBuilder.generateUrl());
 * </code>
 * @author Mike Thompson
 */
public class ReportUrlBuilder {

    private static String BASE_URL = "http://localhost:7080/rest/1/reports/";

    // Required fields
    private String reportUrl;
    private String mediaType;
    // optional fields
    private boolean showDetail;
    private List<AlertPriority> alertPriorityList;
    private List<OperationRequestStatus> operationRequestStatusList;
    private Date startDate;
    private Date endDate;

    public static class Builder {
       private String reportUrl;
       private String mediaType;

        // optional
        private boolean showDetail;
        private List<AlertPriority> alertPriorityList;
        private List<OperationRequestStatus> operationRequestStatusList;
        private Date startDate;
        private Date endDate;

        public Builder(String reportUrl, String mediaType){
           this.reportUrl = reportUrl;
           this.mediaType = mediaType;
        }
        
        public Builder showDetail(boolean detail){
            showDetail = detail;
            return this;

        }

        public Builder addAlertPriorityList(List<AlertPriority> alerts){
            alertPriorityList = alerts;
            return this;

        }
        public Builder addOperationRequestStatusList(List<OperationRequestStatus> statusList){
            operationRequestStatusList = statusList;
            return this;

        }

        public Builder startDate(Date aStartDate){
            startDate = aStartDate;
            return this;
        }


        public Builder endDate(Date aEndDate){
            startDate = aEndDate;
            return this;

        }
        
        public ReportUrlBuilder build() {
            return new ReportUrlBuilder(this);
        }
    }
    
    private ReportUrlBuilder(Builder builder){
        reportUrl = builder.reportUrl;
        mediaType = builder.mediaType;
        showDetail = builder.showDetail;
        startDate = builder.startDate;
        endDate = builder.endDate;
        alertPriorityList = builder.alertPriorityList;
        operationRequestStatusList = builder.operationRequestStatusList;
    }

    public String generateUrl(){
        StringBuilder sb = new StringBuilder(BASE_URL);
        sb.append(reportUrl);
        sb.append(mediaType);
        sb.append("?");
        if(showDetail) sb.append("details="+showDetail+";");
        //@todo: format the dates
        if(startDate != null) sb.append("start_date="+startDate+";");
        if(endDate != null ) sb.append("end_date="+endDate+";");
        //@todo: alertPriorityList and operationRequestStatusList
        return sb.toString();
    }

}
