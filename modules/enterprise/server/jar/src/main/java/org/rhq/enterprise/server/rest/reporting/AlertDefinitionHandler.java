package org.rhq.enterprise.server.rest.reporting;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.Resource;
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
public class AlertDefinitionHandler extends AbstractRestBean implements AlertDefinitionLocal {

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

                                    return alertDefinitionManager.findAlertDefinitionsByCriteria(caller, criteria);
                                }
                            };

                    CriteriaQuery<AlertDefinition, AlertDefinitionCriteria> query =
                            new CriteriaQuery<AlertDefinition, AlertDefinitionCriteria>(criteria, queryExecutor);

                    stream.write((getHeader() + "\n").getBytes());
                    for (AlertDefinition alert : query) {
                        String record = toCSV(alert)  + "\n";
                        stream.write(record.getBytes());
                    }

                }
                private String toCSV(AlertDefinition alertDefinition) {
                    return cleanForCSV(alertDefinition.getName()) + ","
                            + cleanForCSV(alertDefinition.getDescription()) + ","
                            + alertDefinition.getEnabled() + ","
                            + alertDefinition.getPriority() + ","
                            + cleanForCSV(getParentName(alertDefinition.getResource())) + ","
                            + cleanForCSV(alertDefinition.getResource().getAncestry());
                }

                private String getParentName(Resource resource){
                    return null != resource.getParentResource()  ? resource.getParentResource().getName() : "";
                }


                private String getHeader(){
                   return "Name,Description,Enabled,Priority,Parent,Ancestry";
                }

            };

    }

}
