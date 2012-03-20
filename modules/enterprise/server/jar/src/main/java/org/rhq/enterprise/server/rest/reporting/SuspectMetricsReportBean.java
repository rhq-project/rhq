package org.rhq.enterprise.server.rest.reporting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.*;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class SuspectMetricsReportBean extends AbstractRestBean implements SuspectMetricsReportLocal {

    private final Log log = LogFactory.getLog(SuspectMetricsReportBean.class);

    @EJB
    private MeasurementOOBManagerLocal measurementOOBMManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public Response suspectMetrics(UriInfo uriInfo, javax.ws.rs.core.Request request, HttpHeaders headers ) {
        StringBuilder sb;
        log.info(" ** Suspect Metric History REST invocation");

        PageControl pageControl = new PageControl(0, 200); // not sure what the paging size should be?
        PageList<MeasurementOOBComposite> comps =  measurementOOBMManager.getSchedulesWithOOBs(caller, null, null, null, pageControl);
        log.info(" Found MeasurementOOBComposite records: " + comps.size());
        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        log.debug(" Suspect Metric media type: "+mediaType.toString());
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            builder = Response.ok(comps.getValues(), mediaType);

        } else if (mediaType.toString().equals("text/csv")) {
            // CSV version
            log.info("text/csv Suspect handler for REST");
            sb = new StringBuilder("Id,Name,ResourceTypeId,\n"); // set title row
            if(!comps.isEmpty()){
                for (MeasurementOOBComposite oobComposite : comps) {
                    sb.append( oobComposite.getResourceName());
                    sb.append(",");
                    //@todo: ancestry
                    sb.append( oobComposite.getFormattedBaseband());
                    sb.append(",");
                    sb.append( oobComposite.getOutlier());
                    sb.append(",");
                    sb.append( oobComposite.getFactor());
                    sb.append("\n");
                }
            } else {
                //empty
                sb.append("No Data Available");
            }
            builder = Response.ok(sb.toString(), mediaType);

        } else {
            log.debug("Unknown Media Type: "+ mediaType.toString());
            builder = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE);

        }
        return  builder.build();
    }


}
