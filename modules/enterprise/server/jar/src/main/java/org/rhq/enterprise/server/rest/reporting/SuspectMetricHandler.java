package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.ReportsInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.parseAncestry;

@Interceptors(ReportsInterceptor.class)
@Stateless
public class SuspectMetricHandler extends AbstractRestBean implements SuspectMetricLocal {

    private final Log log = LogFactory.getLog(SuspectMetricHandler.class);

    @EJB
    private MeasurementOOBManagerLocal measurementOOBMManager;

    public StreamingOutput suspectMetricsInternal(HttpServletRequest request, Subject user) {
        this.caller = user;
        return suspectMetrics(request);
    }

    @Override
    public StreamingOutput suspectMetrics(HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Received request to generate report for " + caller);
        }
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
                    "formattedOutlier", "factor");

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
