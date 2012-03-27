package org.rhq.enterprise.server.rest.reporting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RecentAlertHandler extends AbstractRestBean implements RecentAlertLocal {

    private final Log log = LogFactory.getLog(RecentAlertHandler.class);

    @EJB
    private AlertManagerLocal alertManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public StreamingOutput recentAlerts(
        final String alertPriority,
        UriInfo uriInfo,
        Request request, HttpHeaders headers) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                final AlertCriteria criteria = new AlertCriteria();

                List<AlertPriority> alertPriorityList = new ArrayList<AlertPriority>(10);
                String alertPriorities[] = alertPriority.split(",");
                for ( String alertPriorityValue : alertPriorities) {
                    log.info("Alert Priority Filter set for: " + alertPriorityValue);
                    alertPriorityList.add(AlertPriority.valueOf(alertPriorityValue.toUpperCase()));
                }
                criteria.addFilterPriorities(alertPriorityList.toArray(new AlertPriority[alertPriorityList.size()]));

                CriteriaQueryExecutor<Alert, AlertCriteria> queryExecutor =
                        new CriteriaQueryExecutor<Alert, AlertCriteria>() {
                            @Override
                            public PageList<Alert> execute(AlertCriteria criteria) {

                                return alertManager.findAlertsByCriteria(caller, criteria);
                            }
                        };

                CriteriaQuery<Alert, AlertCriteria> query =
                        new CriteriaQuery<Alert, AlertCriteria>(criteria, queryExecutor);

                stream.write((getHeader() + "\n").getBytes());
                for (Alert alert : query) {
                    String record = toCSV(alert)  + "\n";
                    stream.write(record.getBytes());
                }

            }
            private String toCSV(Alert alert) {
                return formatDateTime(alert.getCtime()) + "," +
                        cleanForCSV(alert.getAlertDefinition().getName()) + "," +
                        "Condition Text" + "," +
                        alert.getAlertDefinition().getPriority() + "," +
                        "Status"+ "," +
                        cleanForCSV(alert.getAlertDefinition().getResource().getName())+","+
                        cleanForCSV(ReportHelper.parseAncestry(alert.getAlertDefinition().getResource().getAncestry()));
            }

            private String getHeader(){
                return "Creation Time,Name,Condition Text,Priority,Status,Resource,Ancestry";
            }

        };

    }

}
