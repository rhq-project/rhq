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

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.ReportsInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

@Interceptors(ReportsInterceptor.class)
@Stateless
public class AlertDefinitionHandler extends AbstractRestBean implements AlertDefinitionLocal {

    private final Log log = LogFactory.getLog(AlertDefinitionHandler.class);

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;

    @EJB
    ResourceManagerLocal resourceManager;

    @Override
    public StreamingOutput alertDefinitions(final HttpServletRequest request) {
        return alertDefinitionsInternal(request, caller);
    }

    public StreamingOutput alertDefinitionsInternal(final HttpServletRequest request, Subject user) {
        this.caller = user;
            if (log.isDebugEnabled()) {
                log.debug("Received request to generate report for " + caller);
            }
            return new StreamingOutput() {

                @Override
                public void write(OutputStream stream) throws IOException, WebApplicationException {
                    final AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
                    criteria.addFilterResourceOnly(true);
                    criteria.fetchGroupAlertDefinition(true);
                    criteria.fetchResource(true);
                    // TODO figure out why resourceType is not getting fetched
                    // The resource type id is needed for the parent url when we have a
                    // template alert definition. I previously tried accessing the resource
                    // type id via AlertDefinition.resourceType.id, but resourceType is null
                    // even though fetchResourceType is set to true in the critera.
                    //
                    // jsanda
                    criteria.fetchResourceType(true);

                    CriteriaQueryExecutor<AlertDefinition, AlertDefinitionCriteria> queryExecutor =
                            new CriteriaQueryExecutor<AlertDefinition, AlertDefinitionCriteria>() {
                                @Override
                                public PageList<AlertDefinition> execute(AlertDefinitionCriteria criteria) {
                                    return alertDefinitionManager.findAlertDefinitionsByCriteria(caller, criteria);
                                }
                            };

                    CriteriaQuery<AlertDefinition, AlertDefinitionCriteria> query =
                            new CriteriaQuery<AlertDefinition, AlertDefinitionCriteria>(criteria, queryExecutor);

                    CsvWriter<AlertDefinition> csvWriter = new CsvWriter<AlertDefinition>();
                    csvWriter.setColumns("name", "description", "enabled", "priority", "parent", "ancestry",
                            "detailsURL", "resource");

                    csvWriter.setPropertyConverter("parent", new PropertyConverter<AlertDefinition>() {
                        @Override
                        public Object convert(AlertDefinition alertDef, String propertyName) {
                            return  getParentURL(alertDef);
                        }
                    });

                    csvWriter.setPropertyConverter("resource", new PropertyConverter<AlertDefinition>() {
                        @Override
                        public Object convert(AlertDefinition alertDef, String propertyName) {
                            return alertDef.getResource().getName();
                        }
                    });

                    csvWriter.setPropertyConverter("ancestry", new PropertyConverter<AlertDefinition>() {
                        @Override
                        public Object convert(AlertDefinition alertDef, String propertyName) {
                            return ReportFormatHelper.parseAncestry(alertDef.getResource().getAncestry());
                        }
                    });

                    csvWriter.setPropertyConverter("detailsURL", new PropertyConverter<AlertDefinition>() {
                        @Override
                        public Object convert(AlertDefinition alertDef, String propertyName) {
                            return getDetailsURL(alertDef);
                        }
                    });

                    stream.write((getHeader() + "\n").getBytes());
                    for (AlertDefinition alertDef : query) {
                        csvWriter.write(alertDef, stream);
                    }

                }

                private String getParentURL(AlertDefinition alertDef) {
                    Integer templateId = alertDef.getParentId();
                    if (templateId != null && templateId > 0) {
                        return getBaseURL() + "/#Administration/Configuration/AlertDefTemplates/" +
                            alertDef.getResource().getResourceType().getId() + "/" + templateId;
                    } else if (alertDef.getGroupAlertDefinition() != null) {
                        return getBaseURL() + "/#ResourceGroup/" + alertDef.getGroupAlertDefinition().getGroup().getId()
                        + "/Alerts/Definitions/" +
                            alertDef.getGroupAlertDefinition().getId();
                    } else {
                        return "";
                    }
                }


                private String getHeader(){
                   return "Name,Description,Enabled,Priority,Parent,Ancestry,Details URL,Resource";
                }

                private String getDetailsURL(AlertDefinition alertDef) {
                    return getBaseURL() + "/#Resource/" + alertDef.getResource().getId() + "/Alerts/Definitions/" +
                        alertDef.getId();
                }

                private String getBaseURL() {
                    String protocol;
                    if (request.isSecure()) {
                        protocol = "https";
                    } else {
                        protocol = "http";
                    }

                    return protocol + "://" + request.getServerName() + ":" + request.getServerPort() + "/coregui";
                }
            };
    }

}
