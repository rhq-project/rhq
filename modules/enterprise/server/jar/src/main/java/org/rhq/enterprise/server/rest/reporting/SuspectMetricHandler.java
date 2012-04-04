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

import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.parseAncestry;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class SuspectMetricHandler extends AbstractRestBean implements SuspectMetricLocal {

    private final Log log = LogFactory.getLog(SuspectMetricHandler.class);

    @EJB
    private MeasurementOOBManagerLocal measurementOOBMManager;

    @Override
    public StreamingOutput suspectMetrics(UriInfo uriInfo, Request request, HttpHeaders headers ) {
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

                CsvWriter<MeasurementOOBComposite> csvWriter = new CsvWriter<MeasurementOOBComposite>();
                csvWriter.setColumns("resourceName", "ancestry", "scheduleName", "formattedBaseband",
                    "formattedOutlier");

                csvWriter.setPropertyConverter("ancestry", new PropertyConverter<MeasurementOOBComposite>() {
                    @Override
                    public Object convert(MeasurementOOBComposite composite, String propertyName) {
                        return parseAncestry(composite.getResourceAncestry());
                    }
                });

                output.write((getHeader() + "\n").getBytes());
                for (MeasurementOOBComposite composite : query) {
                    applyFormatting(composite);
                    formatBaseband(composite);
                    csvWriter.write(composite, output);
                }
            }
        };
    }

    private String getHeader() {
        return "Resource,Ancestry,Metric,Band,Outlier,Out of Range Factor (%)";
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
