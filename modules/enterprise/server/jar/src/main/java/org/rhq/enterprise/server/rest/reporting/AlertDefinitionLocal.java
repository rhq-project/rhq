package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.auth.Subject;

@Path("/alertDefinitions")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The Alert definitions report")
public interface AlertDefinitionLocal {

    @GZIP
    @GET
    @Produces({"text/csv"})
    @ApiOperation(value = "Export the AlertDefinitions as CSV")
    StreamingOutput alertDefinitions(@Context HttpServletRequest request);

    public StreamingOutput alertDefinitionsInternal(final HttpServletRequest request, Subject user);
}
