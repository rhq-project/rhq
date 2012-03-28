package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/reports/recentOperations")
@Local
public interface RecentOperationsLocal {

    @GET
    @Path("/")
    @Produces({"text/csv", "application/xml"})
    StreamingOutput recentOperations(
            @QueryParam("operationRequestStatus") @DefaultValue("inprogress,success,failure,cancelled") String operationRequestStatus,
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers);

}
