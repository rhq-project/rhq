package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;

import org.rhq.core.domain.auth.Subject;

@Path("/suspectMetrics")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The suspect metrics report")
public interface SuspectMetricLocal {

    @GZIP
    @GET
    @Produces("text/csv")
    @ApiOperation(value = "Export the Suspect Metrics data as CSV")
    StreamingOutput suspectMetrics(@Context HttpServletRequest request);

    StreamingOutput suspectMetricsInternal(HttpServletRequest request, Subject user);
}
