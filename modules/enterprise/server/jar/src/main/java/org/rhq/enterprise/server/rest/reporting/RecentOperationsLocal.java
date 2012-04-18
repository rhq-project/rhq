package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

@Path("/recentOperations")
@Local
public interface RecentOperationsLocal {

    @GET
    @Produces({"text/csv", "application/xml"})
    StreamingOutput recentOperations(
            @QueryParam("status") @DefaultValue("inprogress,success,failure,canceled") String operationRequestStatus,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers);

}
