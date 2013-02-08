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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.auth.Subject;

@Path("/recentOperations")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The recent operations report")
public interface RecentOperationsLocal {

    @GZIP
    @GET
    @Produces({"text/csv"})
    @ApiOperation(value = "Export the Recent Operations Data as CSV")
    StreamingOutput recentOperations(
            @QueryParam("status") @DefaultValue("inprogress,success,failure,canceled") String operationRequestStatus,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime,
            @Context HttpServletRequest request);

    StreamingOutput recentOperationsInternal(
            String operationRequestStatus,
            Long startTime,
            Long endTime,
            HttpServletRequest request,
            Subject user);

}
