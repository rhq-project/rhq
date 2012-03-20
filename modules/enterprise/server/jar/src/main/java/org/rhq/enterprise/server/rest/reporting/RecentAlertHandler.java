package org.rhq.enterprise.server.rest.reporting;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RecentAlertHandler extends AbstractRestBean implements RecentAlertLocal {

    @EJB
    private AlertManagerLocal alertManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public StreamingOutput recentAlerts(UriInfo uriInfo, javax.ws.rs.core.Request request, HttpHeaders headers ) {

        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                final AlertCriteria criteria = new AlertCriteria();

                CriteriaQueryExecutor<Alert, AlertCriteria> queryExecutor =
                        new CriteriaQueryExecutor<Alert, AlertCriteria>() {
                            @Override
                            public PageList<Alert> execute(AlertCriteria criteria) {

                                return alertManager.findAlertsByCriteria(subjectMgr.getOverlord(), criteria);
                            }
                        };

                CriteriaQuery<Alert, AlertCriteria> query =
                        new CriteriaQuery<Alert, AlertCriteria>(criteria, queryExecutor);
                for (Alert alert : query) {
                    String record = toCSV(alert)  + "\n";
                    stream.write(record.getBytes());
                }

            }
            private String toCSV(Alert alert) {
               //@todo:conditionText, status, ancestry
                return formatDateTime(alert.getCtime()) + "," + cleanForCSV(alert.getAlertDefinition().getName()) + "," +
                        alert.getAlertDefinition().getPriority() + "," + cleanForCSV(alert.getAlertDefinition().getResource().getName());
            }

        };

    }

}
