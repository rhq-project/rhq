package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/reports/recentAlerts")
@Local
public interface RecentAlertLocal {

    @GET
    @Path("/")
    @Produces({"text/csv", "application/xml"})
    StreamingOutput recentAlerts(
            @QueryParam("alertPriority") @DefaultValue("High") String alertPriority,
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers);

}
