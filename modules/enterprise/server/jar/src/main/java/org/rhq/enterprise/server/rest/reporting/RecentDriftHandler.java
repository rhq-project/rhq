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
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.ReportsInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.parseAncestry;

@Interceptors(ReportsInterceptor.class)
@Stateless
public class RecentDriftHandler extends AbstractRestBean implements RecentDriftLocal {

    private final Log log = LogFactory.getLog(RecentDriftHandler.class);

    @EJB
    private DriftManagerLocal driftManager;

    public StreamingOutput recentDriftInternal(
            String categories,
            Integer snapshot,
            String path,
            String definitionName,
            Long startTime,
            Long endTime,
            HttpServletRequest request,
            Subject user) {
        this.caller = user;

        return recentDrift(categories,snapshot,path,definitionName,startTime,endTime,request);
    }

    @Override
    public StreamingOutput recentDrift(final String categories, final Integer snapshot, final String path,
        final String definitionName, final Long startTime, final Long endTime, final HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Received request to generate report for " + caller);
        }

        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                GenericDriftCriteria criteria  = new GenericDriftCriteria();
                // Need to fetch the change set so that we can the definition id which is
                // needed to build the details url.
                criteria.fetchChangeSet(true);
                criteria.addFilterChangeSetStartVersion(1);// always start at 1 for this report

                if(startTime != null){
                    criteria.addFilterStartTime(startTime);
                }
                if(endTime != null){
                    criteria.addFilterEndTime(endTime);
                }
                // lets default the end time for them to now if they didnt enter it
                if(startTime != null && endTime == null){
                    Date today = new Date();
                    criteria.addFilterEndTime(today.getTime());
                }


                if(snapshot != null) {
                    log.info("Drift Snapshot version Filter set for: " + snapshot);
                    criteria.addFilterChangeSetEndVersion(snapshot);
                }
                if(path != null) {
                    log.info("Drift Path Filter set for: " + path);
                    criteria.addFilterPath(path);
                }
                if(definitionName != null) {
                    log.info("Drift Definition Filter set for: " + definitionName);
                    //@todo: drift sorting is done in the resultset after no criteria for definition
                }

                criteria.addFilterCategories(getCategories());

                CriteriaQueryExecutor<DriftComposite, DriftCriteria> queryExecutor =
                        new CriteriaQueryExecutor<DriftComposite, DriftCriteria>() {
                            @Override
                            public PageList<DriftComposite> execute(DriftCriteria criteria) {

                                return driftManager.findDriftCompositesByCriteria(caller, criteria);
                            }
                        };

                CriteriaQuery<DriftComposite, DriftCriteria> query =
                        new CriteriaQuery<DriftComposite, DriftCriteria>(criteria, queryExecutor);

                CsvWriter<DriftComposite> csvWriter = new CsvWriter<DriftComposite>();
                csvWriter.setColumns("drift.ctime", "driftDefinitionName", "drift.changeSet.version", "drift.category",
                    "drift.path", "resource.name", "ancestry", "detailsURL");

                csvWriter.setPropertyConverter("drift.ctime", csvWriter.DATE_CONVERTER);

                csvWriter.setPropertyConverter("ancestry", new PropertyConverter<DriftComposite>() {
                    @Override
                    public Object convert(DriftComposite driftComposite, String propertyName) {
                        return parseAncestry(driftComposite.getResource().getAncestry());
                    }
                });

                csvWriter.setPropertyConverter("detailsURL", new PropertyConverter<DriftComposite>() {
                    @Override
                    public Object convert(DriftComposite driftComposite, String propertyName) {
                        return getDetailsURL(driftComposite);
                    }
                });

                stream.write((getHeader() + "\n").getBytes());
                if (definitionName != null) {
                    for (DriftComposite driftComposite : query) {
                        if(driftComposite.getDriftDefinitionName() != null &&
                            driftComposite.getDriftDefinitionName().contains(definitionName)) {
                            csvWriter.write(driftComposite, stream);
                        }
                    }
                } else {
                    for (DriftComposite driftComposite : query) {
                        csvWriter.write(driftComposite, stream);
                    }
                }
            }

            private DriftCategory[] getCategories() {
                List<DriftCategory> driftCategoryList = new ArrayList<DriftCategory>(10);
                String categoryArray[] = categories.split(",");
                for (String category : categoryArray) {
                    log.info("DriftCategories Filter set for: " + category);
                    driftCategoryList.add(DriftCategory.valueOf(category.toUpperCase()));
                }
                return (driftCategoryList.toArray(new DriftCategory[driftCategoryList.size()]));
            }

            private String getHeader(){
                return "Creation Time,Definition,Snapshot,Category,Path,Resource,Ancestry,Details URL";
            }

            private String getDetailsURL(DriftComposite driftDetails) {
                String protocol;
                if (request.isSecure()) {
                    protocol = "https";
                } else {
                    protocol = "http";
                }

                return protocol + "://" + request.getServerName() + ":" + request.getServerPort() +
                    "/coregui/#Resource/" + driftDetails.getResource().getId() + "/Drift/Definitions/" +
                    driftDetails.getDrift().getChangeSet().getDriftDefinitionId() + "/Drift/0id_" +
                    driftDetails.getDrift().getId();
            }

        };

    }

}
