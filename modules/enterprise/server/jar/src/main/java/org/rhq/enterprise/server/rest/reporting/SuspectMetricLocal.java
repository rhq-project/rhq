package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/suspectMetrics")
@Local
public interface SuspectMetricLocal {

    @GET
    @Produces("text/csv")
    StreamingOutput suspectMetrics(
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers);

}
