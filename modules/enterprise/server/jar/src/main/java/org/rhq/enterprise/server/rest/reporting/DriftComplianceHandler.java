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

import java.util.LinkedHashMap;
import java.util.List;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.StreamingOutput;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.enterprise.server.rest.ReportsInterceptor;

@Interceptors(ReportsInterceptor.class)
@Stateless
public class DriftComplianceHandler extends InventorySummaryHandler implements DriftComplianceLocal {

    @Override
    public StreamingOutput generateReport(HttpServletRequest request, String resourceTypeId, String version) {
        return super.generateReport(request, resourceTypeId, version);
    }

    @Override
    public StreamingOutput generateReportInternal(
        HttpServletRequest request,
        String resourceTypeId,
        String version, Subject user) {

        this.caller = user;

        return super.generateReport(request,resourceTypeId,version);
    }


    @Override
    protected List<ResourceInstallCount> getSummaryCounts() {
        return resourceMgr.findResourceComplianceCounts(caller);
    }

    @Override
    protected ResourceCriteria getDetailsQueryCriteria(Integer resourceTypeId, String version) {
        ResourceCriteria criteria = super.getDetailsQueryCriteria(resourceTypeId, version);
        criteria.fetchDriftDefinitions(true);
        return criteria;
    }

    @Override
    protected String getHeader() {
        return super.getHeader() + ",In Compliance?";
    }

    @Override
    protected String getDetailsHeader() {
        return "Resource Type,Plugin,Category,Version,Name,Ancestry,Description,Availability,In Compliance?," +
            "Details URL";
    }

    @Override
    protected List<String> getColumns() {
        List<String> columns = super.getColumns();
        columns.add("inCompliance");
        return columns;
    }

    @Override
    protected List<String> getDetailsColumns() {
        List<String> columns = super.getDetailsColumns();
        columns.add(columns.size() - 1, "inCompliance");
        return columns;
    }

    @Override
    protected LinkedHashMap<String, PropertyConverter<Resource>> getPropertyConverters(HttpServletRequest request) {
        LinkedHashMap<String, PropertyConverter<Resource>> propertyConverters = super.getPropertyConverters(request);
        propertyConverters.put("inCompliance", new PropertyConverter<Resource>() {
            @Override
            public Object convert(Resource resource, String propertyName) {
                for (DriftDefinition def : resource.getDriftDefinitions()) {
                    if (def.getComplianceStatus() != DriftComplianceStatus.IN_COMPLIANCE) {
                        return false;
                    }
                }
                return true;
            }
        });

        return propertyConverters;
    }

    @Override
    protected String getDebugReportName() {
        return "drift compliance";
    }
}
