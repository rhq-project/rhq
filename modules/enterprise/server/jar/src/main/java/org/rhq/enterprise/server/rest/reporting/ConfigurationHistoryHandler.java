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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.ReportsInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.core.domain.util.PageOrdering.ASC;
import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.formatDateTime;

@Interceptors(ReportsInterceptor.class)
@Stateless
public class ConfigurationHistoryHandler extends AbstractRestBean implements ConfigurationHistoryLocal {

    private final Log log = LogFactory.getLog(ConfigurationHistoryHandler.class);

    @EJB
    private ConfigurationManagerLocal configurationManager;

    @Override
    public StreamingOutput configurationHistory(final HttpServletRequest request) {
        return configurationHistoryInternal(request,caller);
    }

    @Override
    public StreamingOutput configurationHistoryInternal(final HttpServletRequest request, Subject user) {
        this.caller = user;
        if (log.isDebugEnabled()) {
            log.debug("Received request to generate report for " + caller);
        }

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
                return  configurationUpdate.getId() + ","
                        + formatDateTime(configurationUpdate.getCreatedTime())+","
                        + formatDateTime(configurationUpdate.getModifiedTime())+","
                        + configurationUpdate.getStatus()+","
                        + cleanForCSV(configurationUpdate.getResource().getName())+ ","
                        + cleanForCSV(ReportFormatHelper.parseAncestry(configurationUpdate.getResource().getAncestry())) + ","
                        + getDetailsURL(configurationUpdate);
                //@todo: check dates, user, update-type
            }

            private String getHeader(){
                return "Version,Date Submitted,Date Completed,Status,Name,Ancestry,Details URL";
            }

            private String getDetailsURL(ResourceConfigurationUpdate configUpdate) {
                String protocol;
                if (request.isSecure()) {
                    protocol = "https";
                } else {
                    protocol = "http";
                }

                return protocol + "://" + request.getServerName() + ":" + request.getServerPort() +
                    "/coregui/#Resource/" + configUpdate.getResource().getId() + "/Configuration/History/" +
                    configUpdate.getId();
            }
        };
    }

}
