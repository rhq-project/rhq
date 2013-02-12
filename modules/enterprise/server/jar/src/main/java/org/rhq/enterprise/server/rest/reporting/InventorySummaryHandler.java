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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.StringUtil;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.ReportsInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.core.domain.resource.InventoryStatus.COMMITTED;
import static org.rhq.core.domain.util.PageOrdering.ASC;

@Interceptors(ReportsInterceptor.class)
@Stateless
public class InventorySummaryHandler extends AbstractRestBean implements InventorySummaryLocal {

    private final Log log = LogFactory.getLog(getClass());

    @EJB
    protected ResourceManagerLocal resourceMgr;

    public StreamingOutput generateReportInternal(
            HttpServletRequest request,
            String resourceTypeId,
            String version,
                Subject user) {
        this.caller = user;

        return generateReport(request,resourceTypeId,version);
    }


    @Override
    public StreamingOutput generateReport(final HttpServletRequest request, final String resourceTypeId,
        final String version) {

        if (log.isDebugEnabled()) {
            log.debug("Received request to generate " + getDebugReportName() + " report for " + caller);
        }

        final List<ResourceInstallCount> results = getSummaryCounts();

        if (StringUtil.isEmpty(resourceTypeId)) {
            // output only resource types
            return new StreamingOutput() {
                @Override
                public void write(OutputStream stream) throws IOException, WebApplicationException {
                    if (log.isDebugEnabled()) {
                        log.debug("Generating inventory summary CSV report for resource types.");
                    }
                    CsvWriter<ResourceInstallCount> csvWriter = new CsvWriter<ResourceInstallCount>();
                    List<String> columns = getColumns();
                    csvWriter.setColumns(columns.toArray(new String[columns.size()]));

                    stream.write((getHeader() + "\n").getBytes());
                    for (ResourceInstallCount installCount : results) {
                        csvWriter.write(installCount, stream);
                    }
                }
            };
        } else {
            // output resource details for specified type and version
            return new StreamingOutput() {
                @Override
                public void write(OutputStream stream) throws IOException, WebApplicationException {
                    if (log.isDebugEnabled()) {
                        log.debug("Generating detailed inventory summary CSV report for [resourceTypeId: " +
                            resourceTypeId + ", version: " + version + "]");
                    }
                    ResourceCriteria criteria = getDetailsQueryCriteria(Integer.parseInt(resourceTypeId), version);

                    CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor =
                        new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                            @Override
                            public PageList<Resource> execute(ResourceCriteria criteria) {
                                return resourceMgr.findResourcesByCriteria(caller, criteria);
                            }
                        };

                    CriteriaQuery<Resource, ResourceCriteria> query =
                        new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);

                    CsvWriter<Resource> csvWriter = new CsvWriter<Resource>();
                    List<String> columns = getDetailsColumns();
                    csvWriter.setColumns(columns.toArray(new String[columns.size()]));

                    Map<String, PropertyConverter<Resource>> propertyConverters = getPropertyConverters(request);
                    for (String property : propertyConverters.keySet()) {
                        csvWriter.setPropertyConverter(property, propertyConverters.get(property));
                    }

                    stream.write((getDetailsHeader() + "\n").getBytes());
                    for (Resource resource : query) {
                        csvWriter.write(resource, stream);
                    }
                }
            };
        }
    }

    protected String getDebugReportName() {
        return "inventory summary";
    }

    protected List<String> getColumns() {
        List<String> columns = new ArrayList<String>(20);
        Collections.addAll(columns, "typeName", "typePlugin", "category.displayName", "version", "count");
        return columns;
    }

    protected List<String> getDetailsColumns() {
        List<String> columns = new ArrayList<String>(10);
        Collections.addAll(columns, "resourceType.name", "resourceType.plugin", "resourceType.category.displayName",
            "version", "name", "ancestry", "description", "currentAvailability.availabilityType", "detailsURL");
        return columns;
    }

    protected ResourceCriteria getDetailsQueryCriteria(Integer resourceTypeId, String version) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceTypeId(resourceTypeId);
        criteria.addFilterVersion(version);
        criteria.addFilterInventoryStatus(COMMITTED);
        criteria.addSortResourceCategory(ASC);
        criteria.addSortPluginName(ASC);
        criteria.addSortResourceTypeName(ASC);

        return criteria;
    }

    protected List<ResourceInstallCount> getSummaryCounts() {
        // TODO add support for filtering by resource type id in query
        return resourceMgr.findResourceInstallCounts(caller, true);
    }

    protected String getHeader() {
        return "Resource Type,Plugin,Category,Version,Count";
    }

    protected String getDetailsHeader() {
        return "Resource Type,Plugin,Category,Version,Name,Ancestry,Description,Availability,Details URL";
    }

    protected LinkedHashMap<String, PropertyConverter<Resource>> getPropertyConverters(final HttpServletRequest request) {
        LinkedHashMap<String, PropertyConverter<Resource>> propertyConverters =
            new LinkedHashMap<String, PropertyConverter<Resource>>();
        propertyConverters.put("ancestry", new PropertyConverter<Resource>() {
            @Override
            public Object convert(Resource resource, String propertyName) {
                return ReportFormatHelper.parseAncestry(resource.getAncestry());
            }
        });

        propertyConverters.put("detailsURL", new PropertyConverter<Resource>() {
            @Override
            public Object convert(Resource resource, String propertyName) {
                return getDetailsURL(resource, request);
            }
        });

        return propertyConverters;
    }

    private String getDetailsURL(Resource resource, HttpServletRequest request) {
        String protocol;
        if (request.isSecure()) {
            protocol = "https";
        } else {
            protocol = "http";
        }

        return protocol + "://" + request.getServerName() + ":" + request.getServerPort() + "/coregui/#Resource/" +
            resource.getId();
    }
}
