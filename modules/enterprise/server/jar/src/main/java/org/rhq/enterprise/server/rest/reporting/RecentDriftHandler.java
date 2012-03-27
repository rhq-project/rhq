package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.DriftComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RecentDriftHandler extends AbstractRestBean implements RecentDriftLocal {


    @EJB
    private DriftManagerLocal driftManager;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    public StreamingOutput recentDrift(@QueryParam("categoryId") final Integer categoryId, @QueryParam("snapshot") final Integer snapshot, @QueryParam("path") final String path, @QueryParam("definition") final String definition, @Context UriInfo uriInfo, @Context Request request, @Context HttpHeaders headers) {

        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                GenericDriftCriteria criteria  = new GenericDriftCriteria();
                criteria.addFilterChangeSetStartVersion(1);// always start at 1 for this report

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
