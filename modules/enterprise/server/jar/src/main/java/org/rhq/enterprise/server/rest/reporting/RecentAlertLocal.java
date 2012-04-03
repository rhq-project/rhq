package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

@Path("/reports/recentAlerts")
@Local
public interface RecentAlertLocal {

    @GET
    @Path("/")
    @Produces({"text/csv", "application/xml"})
    StreamingOutput recentAlerts(
            @QueryParam("alertPriority") @DefaultValue("high,medium,low") String alertPriority,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers);

}
