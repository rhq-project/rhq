package org.rhq.enterprise.server.rest.reporting;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class DriftComplianceHandler extends InventorySummaryHandler implements DriftComplianceLocal {

    @Override
    protected List<ResourceInstallCount> getSummaryCounts() {
        return resourceMgr.findResourceComplianceCounts(caller);
    }

    @Override
    protected ResourceCriteria getDetailsQueryCriteria(Map <Integer, ResourceInstallCount> installCounts) {
        ResourceCriteria criteria = super.getDetailsQueryCriteria(installCounts);
        criteria.fetchDriftDefinitions(true);
        criteria.addFilterResourceTypeIds(installCounts.keySet().toArray(new Integer[installCounts.size()]));
        return criteria;
    }

    @Override
    protected String getHeader() {
        return super.getHeader() + ",In Compliance?";
    }

    @Override
    protected String toCSV(ResourceInstallCount installCount) {
        return super.toCSV(installCount) + "," + installCount.isInCompliance();
    }

    @Override
    protected String toCSV(Resource resource) {
        return super.toCSV(resource) + "," + isInCompliance(resource);
    }

    private boolean isInCompliance(Resource resource) {
        for (DriftDefinition def : resource.getDriftDefinitions()) {
            if (def.getComplianceStatus() != DriftComplianceStatus.IN_COMPLIANCE) {
                return false;
            }
        }
        return true;
    }

}
