/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.reporting.AlertDefinitionLocal;
import org.rhq.enterprise.server.rest.reporting.ConfigurationHistoryLocal;
import org.rhq.enterprise.server.rest.reporting.DriftComplianceLocal;
import org.rhq.enterprise.server.rest.reporting.InventorySummaryLocal;
import org.rhq.enterprise.server.rest.reporting.PlatformUtilizationLocal;
import org.rhq.enterprise.server.rest.reporting.RecentAlertLocal;
import org.rhq.enterprise.server.rest.reporting.RecentDriftLocal;
import org.rhq.enterprise.server.rest.reporting.RecentOperationsLocal;
import org.rhq.enterprise.server.rest.reporting.SuspectMetricLocal;

/**
 * The reports from the reports section of Coregui.
 * This class basically wrap the reports in the org.rhq.enterprise.server.rest.reports package
 * so that they are available outside Coregui as well.
 *
 * @author Heiko W. Rupp
 */
@Stateless
@Path("/reports")
@Produces({"text/csv"})
@Api("Provide the reports that are also run from the Reports section of the UI. All reports are only available in CSV format")
@Interceptors(SetCallerInterceptor.class)
public class ReportsHandlerBean extends AbstractRestBean {

    @EJB
    private AlertDefinitionLocal alertDefinitionLocal;
    @EJB
    private ConfigurationHistoryLocal configurationHistoryLocal;
    @EJB
    private DriftComplianceLocal driftComplianceLocal;
    @EJB
    private InventorySummaryLocal inventorySummaryLocal;
    @EJB
    private PlatformUtilizationLocal platformUtilizationLocal;
    @EJB
    private RecentAlertLocal recentAlertLocal;
    @EJB
    private RecentDriftLocal recentDriftLocal;
    @EJB
    private RecentOperationsLocal recentOperationsLocal;
    @EJB
    private SuspectMetricLocal suspectMetricLocal;

    private String[] reports = {
        "alertDefinitions",
        "configurationHistory",
        "driftCompliance",
        "inventorySummary",
        "platformUtilization",
        "recentAlerts",
        "recentDrift",
        "recentOperations",
        "suspectMetrics"
    };

    @GET
    @Path("/")
    @ApiOperation(value = "List the available reports", responseClass = "String", multiValueResponse = true)
    @Produces({MediaType.TEXT_HTML,MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,"text/csv"})
    public Response listReports(@Context HttpHeaders headers, @Context UriInfo uriInfo) {

        List<Link> links = new ArrayList<Link>(reports.length);
        for (String report: reports) {
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/reports/{report}");
            URI uri = uriBuilder.build(report);
            Link link = new Link(report,uri.toString());
            links.add(link);
        }

        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder = Response.ok();
        if (mediaType.getType().equals("text") && mediaType.getSubtype().equals("csv")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Report,URL\n");
            for (Link link: links) {
                stringBuilder.append(link.getRel()).append(",").append(link.getHref());
                stringBuilder.append('\n');
            }
            builder.entity(stringBuilder.toString());
        }
        else if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder.entity(renderTemplate("reportIndex", links));
        } else {
            GenericEntity<List<Link>> list = new GenericEntity<List<Link>>(links) {
            };
            builder.entity(list);
        }
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(86400); // TODO 1 day or longer? What unit is this anyway?
        builder.cacheControl(cacheControl);
        return builder.build();
    }

    @GZIP
    @GET
    @Path("alertDefinitions")
    @ApiOperation(value = "Export the AlertDefinitions as CSV", responseClass = "String", multiValueResponse = true)
    public StreamingOutput alertDefinitions(@Context HttpServletRequest request) {

        return alertDefinitionLocal.alertDefinitionsInternal(request, caller);
    }

    @GZIP
    @GET
    @Path("configurationHistory")
    @ApiOperation(value = "Export the Configuration History data as CSV", responseClass = "String", multiValueResponse = true)
    public StreamingOutput configurationHistory(@Context HttpServletRequest request) {

        return configurationHistoryLocal.configurationHistoryInternal(request,caller);
    }

    @GZIP
    @GET
    @Path("driftCompliance")
    @ApiOperation(value = "Export the drift compliance data", responseClass = "String", multiValueResponse = true)
    public StreamingOutput generateDriftComplianceReport(
        @Context HttpServletRequest request,
        @QueryParam("resourceTypeId") String resourceTypeId,
        @QueryParam("version") String version) {

        return driftComplianceLocal.generateReportInternal(request,resourceTypeId,version,caller);
    }

    @GZIP
    @GET
    @Path("inventorySummary")
    @ApiOperation(value = "Export the Inventory Summary data as CSV", responseClass = "String", multiValueResponse = true)
    public StreamingOutput generateInventorySummaryReport(
        @Context HttpServletRequest request,
        @QueryParam("resourceTypeId") String resourceTypeId,
        @QueryParam("version") String version) {

        return inventorySummaryLocal.generateReportInternal(request,resourceTypeId,version,caller);
    }


    @GZIP
    @GET
    @Path("platformUtilization")
    @ApiOperation(value = "Export the Platform utilization data as CSV", responseClass = "String", multiValueResponse = true)
    public StreamingOutput generateReport(@Context HttpServletRequest request) {

        return platformUtilizationLocal.generateReportInternal(request, caller);
    }

    @GZIP
    @GET
    @Path("recentAlerts")
    @ApiOperation(value = "Export the Recent Alert data as CSV", responseClass = "String", multiValueResponse = true)
    public StreamingOutput recentAlerts(
            @QueryParam("alertPriority") @DefaultValue("high,medium,low") String alertPriority,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context HttpServletRequest request) {

        return recentAlertLocal.recentAlertsInternal(alertPriority, startTime, endTime, request, caller);
    }

    @GZIP
    @GET
    @Path("recentDrift")
    @ApiOperation(value = "Export the Recent drift data as CSV", responseClass = "String", multiValueResponse = true)
    @ApiError(code = 404, reason = "If no category is provided or the category is wrong")
    public StreamingOutput recentDrift(
            @ApiParam(value = "Drift categories to report on", allowableValues = "FILE_ADDED, FILE_CHANGED, FILE_REMOVED") @QueryParam("categories") String categories,
            @QueryParam("snapshot") Integer snapshot,
            @QueryParam("path") String path,
            @QueryParam("definition") String definitionName,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context HttpServletRequest request) {

        if (categories==null) {
            throw new BadArgumentException("categories","You need to provide at least one category");
        }
        String [] cats = categories.split(",");
        for (String cat : cats) {
            try {
                DriftCategory.valueOf(cat.toUpperCase());
            }
            catch (IllegalArgumentException iae) {
                throw new BadArgumentException("category",cat);
            }
        }

        return recentDriftLocal.recentDriftInternal(categories,snapshot,path,definitionName,startTime,endTime,request,caller);
    }

    @GZIP
    @GET
    @Path("recentOperations")
    @ApiOperation(value = "Export the Recent Operations Data as CSV", responseClass = "String", multiValueResponse = true)
    public StreamingOutput recentOperations(
        @ApiParam("Status to look for. If parameter is not given, all values are used")
            @QueryParam("status") @DefaultValue("inprogress,success,failure,canceled") String operationRequestStatus,
        @ApiParam("The start time in ms since epoch of the time range to export.")
            @QueryParam("startTime") Long startTime,
        @ApiParam("The end time in ms since epoch of the time range to export. Defaults to 'now' if only the start time is given")
            @QueryParam("endTime") Long endTime,
            @Context HttpServletRequest request) {

        return recentOperationsLocal.recentOperationsInternal(operationRequestStatus,startTime,endTime,request,caller);
    }

    @GZIP
    @GET
    @Path("suspectMetrics")
    @ApiOperation(value = "Export the Suspect Metrics data as CSV", responseClass = "String", multiValueResponse = true)
    public StreamingOutput suspectMetrics(@Context HttpServletRequest request) {

        return suspectMetricLocal.suspectMetricsInternal(request,caller);
    }


}

