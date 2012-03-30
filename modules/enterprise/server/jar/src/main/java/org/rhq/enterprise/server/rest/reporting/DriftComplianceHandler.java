/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.rest.reporting;

import java.util.List;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

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
    public StreamingOutput generateReport(UriInfo uriInfo, Request request, HttpHeaders headers, boolean showAllDetails,
        String resourceTypeIds) {
        return super.generateReport(uriInfo, request, headers, showAllDetails, "");
    }

    @Override
    protected List<ResourceInstallCount> getSummaryCounts() {
        return resourceMgr.findResourceComplianceCounts(caller);
    }

    @Override
    protected ResourceCriteria getDetailsQueryCriteria(Integer resourceTypeId) {
        ResourceCriteria criteria = super.getDetailsQueryCriteria(resourceTypeId);
        criteria.fetchDriftDefinitions(true);
        return criteria;
    }

    @Override
    protected String getHeader() {
        return super.getHeader() + ",In Compliance?";
    }

    @Override
    protected String getDetailsHeader() {
        return super.getDetailsHeader() + ",In Compliance?";
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
