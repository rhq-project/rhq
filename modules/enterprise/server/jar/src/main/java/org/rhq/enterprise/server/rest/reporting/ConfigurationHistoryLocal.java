package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

@Path("/configurationHistory")
@Local
public interface ConfigurationHistoryLocal {

    @GET
    @Produces({"text/csv"})
    StreamingOutput configurationHistory(@Context HttpServletRequest request);
}
