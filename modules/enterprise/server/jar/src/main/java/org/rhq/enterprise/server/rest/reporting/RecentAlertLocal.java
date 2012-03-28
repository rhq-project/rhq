package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
            @Context UriInfo uriInfo,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers);

}
