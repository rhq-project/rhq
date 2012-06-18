package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Local;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

import com.wordnik.swagger.annotations.Api;

@Path("/suspectMetrics")
@Local
@Api(basePath="http://localhost:7080/coregui/reports", value = "The suspect metrics report")
public interface SuspectMetricLocal {

    @GET
    @Produces("text/csv")
    StreamingOutput suspectMetrics(@Context HttpServletRequest request);

}
