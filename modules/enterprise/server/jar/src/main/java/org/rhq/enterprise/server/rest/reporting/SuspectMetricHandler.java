package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.parseAncestry;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class SuspectMetricHandler extends AbstractRestBean implements SuspectMetricLocal {

    private final Log log = LogFactory.getLog(SuspectMetricHandler.class);

    @EJB
    private MeasurementOOBManagerLocal measurementOOBMManager;

    @Override
    public StreamingOutput suspectMetrics(UriInfo uriInfo, Request request, HttpHeaders headers ) {
//        StringBuilder sb;
//        log.info(" ** Suspect Metric History REST invocation");
//
//        PageControl pageControl = new PageControl(0, 200); // not sure what the paging size should be?
//        PageList<MeasurementOOBComposite> comps =  measurementOOBMManager.getSchedulesWithOOBs(caller, null, null, null, pageControl);
//        log.info(" Found MeasurementOOBComposite records: " + comps.size());
//        Response.ResponseBuilder  builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
//        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
//        log.debug(" Suspect Metric media type: "+mediaType.toString());
//        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
//            builder = Response.ok(comps.getValues(), mediaType);
//
//        } else if (mediaType.toString().equals("text/csv")) {
//            // CSV version
//            log.info("text/csv Suspect handler for REST");
//            sb = new StringBuilder("Id,Name,ResourceTypeId,\n"); // set title row
//            if(!comps.isEmpty()){
//                for (MeasurementOOBComposite oobComposite : comps) {
//                    sb.append( oobComposite.getResourceName());
//                    sb.append(",");
//                    sb.append(ReportFormatHelper.parseAncestry(oobComposite.getResourceAncestry()));
//                    sb.append(",");
//                    sb.append( oobComposite.getUnits()); // Metric
//                    sb.append(",");
//                    sb.append( oobComposite.getFormattedBaseband());
//                    sb.append(",");
//                    sb.append( oobComposite.getOutlier());
//                    sb.append(",");
//                    sb.append( oobComposite.getFactor());
//                    sb.append("\n");
//                }
//            } else {
//                //empty
//                sb.append("No Data Available");
//            }
//            builder = Response.ok(sb.toString(), mediaType);
//
//        } else {
//            log.debug("Unknown Media Type: "+ mediaType.toString());
//            builder = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE);
//
//        }
//        return  builder.build();

        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Criteria criteria = new Criteria() {
                    @Override
                    public Class<?> getPersistentClass() {
                        return MeasurementOOBComposite.class;
                    }

                };
                criteria.setPaging(0, 5);
                CriteriaQueryExecutor<MeasurementOOBComposite, Criteria> queryExecutor =
                    new CriteriaQueryExecutor<MeasurementOOBComposite, Criteria>() {
                        @Override
                        public PageList<MeasurementOOBComposite> execute(Criteria criteria) {
                            return measurementOOBMManager.getSchedulesWithOOBs(caller, null, null, null,
                                new PageControl(criteria.getPageNumber(), criteria.getPageSize()));
                        }
                    };
                CriteriaQuery<MeasurementOOBComposite, Criteria> query =
                    new CriteriaQuery<MeasurementOOBComposite, Criteria>(criteria, queryExecutor);

                output.write((getHeader() + "\n").getBytes());
                for (MeasurementOOBComposite composite : query) {
                    applyFormatting(composite);
                    formatBaseband(composite);
                    String record = toCSV(composite) + "\n";
                    output.write(record.getBytes());
                }
            }
        };
    }

    private String getHeader() {
        return "Resource,Ancestry,Metric,Band,Outlier,Out of Range Factor (%)";
    }

    private String toCSV(MeasurementOOBComposite composite) {
        return cleanForCSV(composite.getResourceName()) + "," +
            cleanForCSV(parseAncestry(composite.getResourceAncestry())) + "," +
            cleanForCSV(composite.getScheduleName()) + "," +
            cleanForCSV(composite.getFormattedBaseband()) + "," +
            cleanForCSV(composite.getFormattedOutlier()) + "," +
            composite.getFactor();
    }

    private void applyFormatting(MeasurementOOBComposite oob) {
        oob.setFormattedOutlier(MeasurementConverter.format(oob.getOutlier(), oob.getUnits(), true));
        formatBaseband(oob);
    }

    private void formatBaseband(MeasurementOOBComposite oob) {
        String min = MeasurementConverter.format(oob.getBlMin(), oob.getUnits(), true);
        String max = MeasurementConverter.format(oob.getBlMax(), oob.getUnits(), true);
        oob.setFormattedBaseband(min + ", " + max);
    }

}
