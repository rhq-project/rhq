package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/alertDefinitions")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The Alert definitions report")
public interface AlertDefinitionLocal {

    @GET
    @Produces({"text/csv"})
    @ApiOperation(value = "Export the AlertDefinitions in the system")
    StreamingOutput alertDefinitions(@Context HttpServletRequest request);

}
