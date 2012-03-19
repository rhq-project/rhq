package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/reports/suspectMetrics")
@Local
public interface SuspectMetricsReportLocal {

    @GET
    @Path("/")
    @Produces({"text/csv", "application/xml"})
    Response suspectMetrics(
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers);

}
