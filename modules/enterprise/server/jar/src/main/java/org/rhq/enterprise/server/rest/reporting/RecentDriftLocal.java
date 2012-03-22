package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;

@Path("/reports/recentDrift")
@Local
public interface RecentDriftLocal {

    @GET
    @Path("/")
    @Produces({"text/csv", "application/xml"})
    StreamingOutput recentDrift(
            @QueryParam("categoryId") Integer categoryId,
            @QueryParam("snapshot") Integer snapshot,
            @QueryParam("path") String path,
            @QueryParam("definition") String definition,
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers);

}
