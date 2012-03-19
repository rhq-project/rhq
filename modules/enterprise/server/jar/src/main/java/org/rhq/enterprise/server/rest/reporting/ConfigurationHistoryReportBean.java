package org.rhq.enterprise.server.rest.reporting;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.*;

import static org.rhq.core.domain.util.PageOrdering.ASC;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class ConfigurationHistoryReportBean extends AbstractRestBean implements ConfigurationHistoryReportLocal {

    private final Log log = LogFactory.getLog(ConfigurationHistoryReportBean.class);

    @EJB
    private ConfigurationManagerLocal configurationManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public Response configurationHistory(UriInfo uriInfo, Request request, HttpHeaders headers ) {
        StringBuilder sb;
        log.info(" ** Configuration History REST invocation");
        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.fetchConfiguration(true);
        criteria.addSortCreatedTime(ASC);

        CriteriaQueryExecutor<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria> queryExecutor =
                new CriteriaQueryExecutor<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria>() {
                    @Override
                    public PageList<ResourceConfigurationUpdate> execute(ResourceConfigurationUpdateCriteria criteria) {
                        return configurationManager.findResourceConfigurationUpdatesByCriteria(caller, criteria);
                    }
                };

        CriteriaQuery<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria> query =
                new CriteriaQuery<ResourceConfigurationUpdate, ResourceConfigurationUpdateCriteria>(criteria, queryExecutor);

        Response.ResponseBuilder builder = Response.status(Response.Status.NOT_ACCEPTABLE); // default error response
        MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        log.debug(" Suspect Metric media type: " + mediaType.toString());
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            builder = Response.ok(query, mediaType);

        } else if (mediaType.toString().equals("text/csv")) {
            // CSV version
            log.info("text/csv handler for REST");

            sb = new StringBuilder("ID,Status\n"); // set title row
            for (ResourceConfigurationUpdate configUpdate : query) {
                sb.append(configUpdate.getId());
                sb.append(",");
                sb.append(configUpdate.getStatus());
                sb.append("\n");
            }

            builder = Response.ok(sb.toString(), mediaType);

        } else {
            log.debug("Unknown Media Type: " + mediaType.toString());
            builder = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE);

        }
        return builder.build();
    }


}
