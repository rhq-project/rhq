package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class AlertDefinitionHandler extends AbstractRestBean implements AlertDefinitionLocal {

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;

    @EJB
    ResourceManagerLocal resourceManager;

    @Override
    public StreamingOutput alertDefinitions(UriInfo uriInfo, javax.ws.rs.core.Request request, HttpHeaders headers ) {

            return new StreamingOutput() {

                Map<Integer, Resource> resources = new TreeMap<Integer, Resource>();

                @Override
                public void write(OutputStream stream) throws IOException, WebApplicationException {
                    final AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
                    criteria.addFilterResourceOnly(true);
                    criteria.fetchResource(true);

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
                        int resourceId = alert.getResource().getId();
                        if (!resources.containsKey(resourceId)) {
                            resources.put(resourceId, loadResource(resourceId));
                        }
                        String record = toCSV(alert)  + "\n";
                        stream.write(record.getBytes());
                    }

                }
                private String toCSV(AlertDefinition alertDefinition) {
                    Resource resource = resources.get(alertDefinition.getResource().getId());
                    return cleanForCSV(alertDefinition.getName()) + ","
                            + cleanForCSV(alertDefinition.getDescription()) + ","
                            + alertDefinition.getEnabled() + ","
                            + alertDefinition.getPriority() + ","
                            + cleanForCSV(getParentName(resource)) + ","
                            + cleanForCSV(resource.getAncestry());
                }

                private String getParentName(Resource resource){
                    return null != resource.getParentResource()  ? resource.getParentResource().getName() : "";
                }


                private String getHeader(){
                   return "Name,Description,Enabled,Priority,Parent,Ancestry";
                }

                private Resource loadResource(int resourceId) {
                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.addFilterId(resourceId);
                    criteria.fetchParentResource(true);
                    PageList<Resource> resources = resourceManager.findResourcesByCriteria(caller, criteria);

                    return resources.get(0);
                }

            };
    }

}
