package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

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

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RecentOperationsHandler extends AbstractRestBean implements RecentOperationsLocal {

    @EJB
    private OperationManagerLocal operationManager;

    @EJB
    private SubjectManagerLocal subjectMgr;


    @Override
    public StreamingOutput recentOperations(
        @QueryParam("operationRequestStatus") @DefaultValue("failure")
        final String operationRequestStatus,
        @Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                //@todo: add ability to pass multiple operations
                final ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
                if(operationRequestStatus != null){
                    criteria.addFilterStatus(OperationRequestStatus.valueOf(operationRequestStatus.toUpperCase()));
                }

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
                        cleanForCSV(operation.getJobName()) + "," +
                        operation.getSubjectName() + "," +
                        operation.getStatus() + "," +
                        cleanForCSV(operation.getResource().getName()) +","+
                        cleanForCSV(operation.getResource().getAncestry());
            }

            private String getHeader(){
                return "Date Submitted,Operation,Requestor,Status,Resource,Ancestry";
            }

        };

    }

}
