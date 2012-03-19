package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/reports/alertDefinitions")
@Local
public interface AlertDefinitionReportLocal {

    @GET
    @Path("/")
    @Produces({"text/csv", "application/xml"})
    StreamingOutput inventorySummary(
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers );

}
