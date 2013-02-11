package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.auth.Subject;

@Path("/recentAlerts")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The recent alerts report")
public interface RecentAlertLocal {

    @GZIP
    @GET
    @Produces({"text/csv"})
    @ApiOperation(value = "Export the Recent Alert data as CSV")
    StreamingOutput recentAlerts(
            @QueryParam("alertPriority") @DefaultValue("high,medium,low") String alertPriority,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context HttpServletRequest request);

    StreamingOutput recentAlertsInternal(
            String alertPriority,
            Long startTime,
            Long endTime,
            HttpServletRequest request,
            Subject user
    );

}
