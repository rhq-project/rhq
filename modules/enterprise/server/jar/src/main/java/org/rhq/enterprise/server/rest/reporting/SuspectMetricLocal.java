package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/reports/suspectMetrics")
@Local
public interface SuspectMetricLocal {

    @GET
    @Path("/")
    @Produces("text/csv")
    StreamingOutput suspectMetrics(
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers);

}
