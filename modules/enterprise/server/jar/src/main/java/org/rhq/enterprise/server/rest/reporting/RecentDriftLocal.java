package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

@Path("/recentDrift")
@Local
public interface RecentDriftLocal {

    @GET
    @Produces({"text/csv", "application/xml"})
    StreamingOutput recentDrift(
            @QueryParam("categories") String categories,
            @QueryParam("snapshot") Integer snapshot,
            @QueryParam("path") String path,
            @QueryParam("definition") String definitionName,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest request,
            @Context HttpHeaders headers);

}
