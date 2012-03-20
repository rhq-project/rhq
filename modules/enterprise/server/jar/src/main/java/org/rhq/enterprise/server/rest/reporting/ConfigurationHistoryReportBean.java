package org.rhq.enterprise.server.rest.reporting;


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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStream;

import static org.rhq.core.domain.util.PageOrdering.ASC;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class ConfigurationHistoryReportBean extends AbstractRestBean implements ConfigurationHistoryReportLocal {

    @EJB
    private ConfigurationManagerLocal configurationManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public StreamingOutput configurationHistory(UriInfo uriInfo, Request request, HttpHeaders headers ) {

        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                final ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
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

                stream.write((getHeader() + "\n").getBytes());
                for (ResourceConfigurationUpdate alert : query) {
                    String record = toCSV(alert)  + "\n";
                    stream.write(record.getBytes());
                }

            }
            private String toCSV(ResourceConfigurationUpdate configurationUpdate) {
                return cleanForCSV(configurationUpdate.getResource().getName()) + "," +
                        configurationUpdate.getConfiguration().getVersion() + ","
                        + formatDateTime(configurationUpdate.getCreatedTime())+","
                        + formatDateTime(configurationUpdate.getModifiedTime())+","
                        + "," + configurationUpdate.getStatus()
                        + "," + cleanForCSV(configurationUpdate.getResource().getAncestry());
                //@todo: check dates, user, update-type
            }
            private String getHeader(){
                return "Name,Version,Date Submitted,Date Completed,Status,Ancestry";
            }

        };

    }

}
