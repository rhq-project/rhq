package org.rhq.enterprise.server.rest.reporting;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
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

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.*;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class AlertDefinitionReportBean extends AbstractRestBean implements AlertDefinitionReportLocal {

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public StreamingOutput alertDefinitions(UriInfo uriInfo, javax.ws.rs.core.Request request, HttpHeaders headers ) {

            return new StreamingOutput() {
                @Override
                public void write(OutputStream stream) throws IOException, WebApplicationException {
                    final AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
                    criteria.addFilterResourceOnly(true);

                    CriteriaQueryExecutor<AlertDefinition, AlertDefinitionCriteria> queryExecutor =
                            new CriteriaQueryExecutor<AlertDefinition, AlertDefinitionCriteria>() {
                                @Override
                                public PageList<AlertDefinition> execute(AlertDefinitionCriteria criteria) {

                                    return alertDefinitionManager.findAlertDefinitionsByCriteria(subjectMgr.getOverlord(), criteria);
                                }
                            };

                    CriteriaQuery<AlertDefinition, AlertDefinitionCriteria> query =
                            new CriteriaQuery<AlertDefinition, AlertDefinitionCriteria>(criteria, queryExecutor);
                    for (AlertDefinition alert : query) {
                        String record = toCSV(alert)  + "\n";
                        stream.write(record.getBytes());
                    }

                }
                private String toCSV(AlertDefinition alertDefinition) {
                    return cleanForCSV(alertDefinition.getName()) + "," + cleanForCSV(alertDefinition.getDescription()) + "," +
                            alertDefinition.getEnabled() + "," + alertDefinition.getPriority()
                            + "," + alertDefinition.getParentId()
                            + "," + alertDefinition.getPriority();
                    //@todo:ancestry
                }

            };

    }

}
