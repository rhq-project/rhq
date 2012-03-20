package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

@Path("/reports/configurationHistory")
@Local
public interface ConfigurationHistoryLocal {

    @GET
    @Path("/")
    @Produces({"text/csv", "application/xml"})
    StreamingOutput configurationHistory(
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders headers);

}
