package org.rhq.enterprise.server.rest.reporting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
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
public class RecentOperationsHandler extends AbstractRestBean implements RecentOperationsLocal {

    private final Log log = LogFactory.getLog(RecentOperationsHandler.class);
    @EJB
    private OperationManagerLocal operationManager;

    @EJB
    private SubjectManagerLocal subjectMgr;


    @Override
    public StreamingOutput recentOperations(
        final String operationRequestStatus,
        UriInfo uriInfo, Request request, HttpHeaders headers) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                final ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

                List<OperationRequestStatus> operationRequestStatusList = new ArrayList<OperationRequestStatus>(10);
                String statuses[] = operationRequestStatus.split(",");
                for (String requestStatus : statuses) {
                    log.info("OperationRequestStatus Filter set for: " + requestStatus);
                    operationRequestStatusList.add(OperationRequestStatus.valueOf(requestStatus.toUpperCase()));
                }
                criteria.addFilterStatuses(operationRequestStatusList.toArray(new OperationRequestStatus[operationRequestStatusList.size()]));

                CriteriaQueryExecutor<ResourceOperationHistory, ResourceOperationHistoryCriteria> queryExecutor =
                        new CriteriaQueryExecutor<ResourceOperationHistory, ResourceOperationHistoryCriteria>() {
                            @Override
                            public PageList<ResourceOperationHistory> execute(ResourceOperationHistoryCriteria criteria) {

                                return operationManager.findResourceOperationHistoriesByCriteria(caller, criteria);
                            }
                        };

                CriteriaQuery<ResourceOperationHistory, ResourceOperationHistoryCriteria> query =
                        new CriteriaQuery<ResourceOperationHistory, ResourceOperationHistoryCriteria>(criteria, queryExecutor);

                stream.write((getHeader() + "\n").getBytes());
                for (ResourceOperationHistory alert : query) {
                    String record = toCSV(alert)  + "\n";
                    stream.write(record.getBytes());
                }

            }
            private String toCSV(ResourceOperationHistory operation) {
                return formatDateTime(operation.getStartedTime()) + "," +
                        cleanForCSV(operation.getOperationDefinition().getDisplayName()) + "," +
                        operation.getSubjectName() + "," +
                        operation.getStatus() + "," +
                        cleanForCSV(operation.getResource().getName()) +","+
                        cleanForCSV(ReportHelper.parseAncestry(operation.getResource().getAncestry()));
            }

            private String getHeader(){
                return "Date Submitted,Operation,Requester,Status,Resource,Ancestry";
            }

        };

    }

}
