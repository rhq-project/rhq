package org.rhq.enterprise.server.rest.reporting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RecentDriftHandler extends AbstractRestBean implements RecentDriftLocal {

    private final Log log = LogFactory.getLog(RecentDriftHandler.class);

    @EJB
    private DriftManagerLocal driftManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public StreamingOutput recentDrift(final String categories, final Integer snapshot, final String path, final String definition, final Long startTime, final Long endTime, final UriInfo uriInfo, Request request, final HttpHeaders headers) {

        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                GenericDriftCriteria criteria  = new GenericDriftCriteria();
                criteria.addFilterChangeSetStartVersion(1);// always start at 1 for this report

                if(snapshot != null){
                    log.info("Drift Snapshot version Filter set for: " + snapshot);
                    criteria.addFilterChangeSetEndVersion(snapshot);
                }
                if(definition != null){
                    //@todo: drift definition search
                }

                List<DriftCategory> driftCategoryList = new ArrayList<DriftCategory>(10);
                String categoryArray[] = categories.split(",");
                for (String category : categoryArray) {
                    log.info("DriftCategories Filter set for: " + category);
                    driftCategoryList.add(DriftCategory.valueOf(category.toUpperCase()));
                }
                criteria.addFilterCategories(driftCategoryList.toArray(new DriftCategory[driftCategoryList.size()]));

                if(startTime != null){
                   criteria.addFilterStartTime(startTime);
                }
                if(endTime != null){
                    criteria.addFilterEndTime(endTime);
                }

                CriteriaQueryExecutor<DriftComposite, DriftCriteria> queryExecutor =
                        new CriteriaQueryExecutor<DriftComposite, DriftCriteria>() {
                            @Override
                            public PageList<DriftComposite> execute(DriftCriteria criteria) {

                                return driftManager.findDriftCompositesByCriteria(caller, criteria);
                            }
                        };

                CriteriaQuery<DriftComposite, DriftCriteria> query =
                        new CriteriaQuery<DriftComposite, DriftCriteria>(criteria, queryExecutor);

                stream.write((getHeader() + "\n").getBytes());
                for (DriftComposite alert : query) {
                    String record = toCSV(alert)  + "\n";
                    stream.write(record.getBytes());
                }

            }
            private String toCSV(DriftComposite drift) {
                return formatDateTime(drift.getDrift().getCtime()) + "," +
                        cleanForCSV(drift.getDriftDefinitionName()) + "," +
                        drift.getDrift().getChangeSet().getVersion()+","+
                        drift.getDrift().getCategory() + "," +
                        drift.getDrift().getPath() + "," +
                        cleanForCSV(drift.getResource().getName()) +","+
                        cleanForCSV(ReportHelper.parseAncestry(drift.getResource().getAncestry()));
            }

            private String getHeader(){
                return "Creation Time,Definition,Snapshot,Category,Path,Resource,Ancestry";
            }

        };

    }

}
