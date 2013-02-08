package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.ReportsInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

@Interceptors(ReportsInterceptor.class)
@Stateless
public class RecentOperationsHandler extends AbstractRestBean implements RecentOperationsLocal {

    private final Log log = LogFactory.getLog(RecentOperationsHandler.class);

    @EJB
    private OperationManagerLocal operationManager;

    public StreamingOutput recentOperationsInternal(
            String operationRequestStatus,
            Long startTime,
            Long endTime,
            HttpServletRequest request,
            Subject user) {
        this.caller = user;

        return recentOperations(operationRequestStatus,startTime,endTime,request);
    }

    @Override
    public StreamingOutput recentOperations(final String operationRequestStatus, final Long startTime,
        final Long endTime, final HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Received request to generate report for " + caller);
        }
        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                final ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
                criteria.addSortEndTime(PageOrdering.DESC);

                if(startTime != null){
                    criteria.addFilterStartTime(startTime);
                }
                if(endTime != null){
                    criteria.addFilterEndTime(endTime);
                }
                // lets default the end time for them to now if they didn't enter it
                if(startTime != null && endTime == null){
                    Date today = new Date();
                    criteria.addFilterEndTime(today.getTime());
                }


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

                CsvWriter<ResourceOperationHistory> csvWriter = new CsvWriter<ResourceOperationHistory>();
                csvWriter.setColumns("startedTime", "operationDefinition.displayName", "subjectName", "status",
                    "resource.name", "ancestry", "detailsURL");

                csvWriter.setPropertyConverter("startedTime", csvWriter.DATE_CONVERTER);

                csvWriter.setPropertyConverter("ancestry", new PropertyConverter<ResourceOperationHistory>() {
                    @Override
                    public Object convert(ResourceOperationHistory history, String propertyName) {
                        return ReportFormatHelper.parseAncestry(history.getResource().getAncestry());
                    }
                });

                csvWriter.setPropertyConverter("detailsURL", new PropertyConverter<ResourceOperationHistory>() {
                    @Override
                    public Object convert(ResourceOperationHistory history, String propertyName) {
                        return getDetailsURL(history);
                    }
                });

                stream.write((getHeader() + "\n").getBytes());
                for (ResourceOperationHistory history : query) {
                    csvWriter.write(history, stream);
                }

            }

            private String getHeader(){
                return "Date Submitted,Operation,Requester,Status,Resource,Ancestry,Details URL";
            }

            private String getDetailsURL(ResourceOperationHistory history) {
                String protocol;
                if (request.isSecure()) {
                    protocol = "https";
                } else {
                    protocol = "http";
                }

                return protocol + "://" + request.getServerName() + ":" + request.getServerPort() +
                    "/coregui/#Resource/" + history.getResource().getId() + "/Operations/History/" +
                    history.getId();
            }

        };

    }

}
