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

@Path("/recentOperations")
@Local
public interface RecentOperationsLocal {

    @GET
    @Produces({"text/csv"})
    StreamingOutput recentOperations(
            @QueryParam("status") @DefaultValue("inprogress,success,failure,canceled") String operationRequestStatus,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context HttpServletRequest request);

}
