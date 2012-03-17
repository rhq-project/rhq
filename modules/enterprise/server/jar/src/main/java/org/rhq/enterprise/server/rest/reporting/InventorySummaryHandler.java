package org.rhq.enterprise.server.rest.reporting;

import static org.rhq.core.domain.resource.InventoryStatus.COMMITTED;
import static org.rhq.core.domain.util.PageOrdering.ASC;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class InventorySummaryHandler extends AbstractRestBean implements InventorySummaryLocal {

    @EJB
    private ResourceManagerLocal resourceMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    @GET
    @Path("/inventorySummary")
    @Produces({"text/csv", "application/xml"})
    public StreamingOutput inventorySummary(UriInfo uriInfo, javax.ws.rs.core.Request request, HttpHeaders headers,
                                            boolean includeDetails) {
        final List<ResourceInstallCount> results = resourceMgr.findResourceInstallCounts(caller, true);

        if (includeDetails) {
            return new OutputDetailedInventorySummary(results);
        } else {
            return new StreamingOutput() {
                @Override
                public void write(OutputStream stream) throws IOException, WebApplicationException {
                    stream.write("Resource Type,Plugin,Category,Version,Count\n".getBytes());
                    for (ResourceInstallCount installCount : results) {
                        String record = toCSV(installCount) + "\n";
                        stream.write(record.getBytes());
                    }
                }
            };
        }
    }

    private class OutputDetailedInventorySummary implements StreamingOutput {

        // map of counts keyed by resource type id
        private Map<Integer, ResourceInstallCount> installCounts = new HashMap<Integer, ResourceInstallCount>();

        public OutputDetailedInventorySummary(List<ResourceInstallCount> installCountList) {
            for (ResourceInstallCount installCount : installCountList) {
                installCounts.put(installCount.getTypeId(), installCount);
            }
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            final ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterInventoryStatus(COMMITTED);
            criteria.addSortResourceCategory(ASC);
            criteria.addSortPluginName(ASC);
            criteria.addSortResourceTypeName(ASC);

            CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor =
                new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                    @Override
                    public PageList<Resource> execute(ResourceCriteria criteria) {
                        // TODO why doesn't this work with caller?
                        // When passing caller as the first arg instead of overlord, I get an
                        // error saying that the session id for the user rhqadmin is
                        // invalid. What is really interesting is that I get the same error
                        // when I try a different user. That is, the error message still
                        // cites rhqadmin.
                        //
                        // jsanda
                        return resourceMgr.findResourcesByCriteria(subjectMgr.getOverlord(), criteria);
                    }
                };

            CriteriaQuery<Resource, ResourceCriteria> query =
                new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);
            for (Resource resource : query) {
                ResourceInstallCount installCount = installCounts.get(resource.getResourceType().getId());
                String record = toCSV(installCount) + "," + toCSV(resource) + "\n";
                output.write(record.getBytes());
            }
        }
    }

    private String toCSV(ResourceInstallCount installCount) {
        return installCount.getTypeName() + "," + installCount.getTypePlugin() + "," +
            installCount.getCategory().getDisplayName() + "," + installCount.getVersion() + "," +
            installCount.getCount();
    }

    private String toCSV(Resource resource) {
        return resource.getName() + "," + resource.getAncestry() + "," + resource.getDescription() + "," +
            resource.getResourceType().getName() + "," + resource.getVersion() + "," +
            resource.getCurrentAvailability().getAvailabilityType();
    }
}
