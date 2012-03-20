package org.rhq.enterprise.server.rest.reporting;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class DriftComplianceHandler extends InventorySummaryHandler implements DriftComplianceLocal {

    @Override
    protected String getHeader() {
        return super.getHeader() + "In Compliance?";
    }

    @Override
    protected String toCSV(ResourceInstallCount installCount) {
        return super.toCSV(installCount) + "," + installCount.isInCompliance();
    }

}
