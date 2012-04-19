package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/alertDefinitions")
@Local
public interface AlertDefinitionLocal {

    @GET
    @Produces({"text/csv"})
    StreamingOutput alertDefinitions(@Context HttpServletRequest request);

}
