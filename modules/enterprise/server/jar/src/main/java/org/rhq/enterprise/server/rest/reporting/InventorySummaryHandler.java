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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.core.domain.resource.InventoryStatus.COMMITTED;
import static org.rhq.core.domain.util.PageOrdering.ASC;
import static org.rhq.enterprise.server.rest.reporting.ReportFormatHelper.parseAncestry;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class InventorySummaryHandler extends AbstractRestBean implements InventorySummaryLocal {

    @EJB
    protected ResourceManagerLocal resourceMgr;

    @Override
    public StreamingOutput generateReport(UriInfo uriInfo, Request request, HttpHeaders headers,
        boolean showAllDetails, final String resourceTypeIds) {
        final List<ResourceInstallCount> results = getSummaryCounts();
        final MediaType mediaType = headers.getAcceptableMediaTypes().get(0);
        Set<Integer> ids = parseIds(resourceTypeIds);

        if (showAllDetails) {
            ids = Collections.emptySet();
            return new OutputDetailedInventorySummary(results, ids);
        } else if (!ids.isEmpty()) {
            return new OutputDetailedInventorySummary(results, ids);
        } else {
            return new StreamingOutput() {
                @Override
                public void write(OutputStream stream) throws IOException, WebApplicationException {
                    if (mediaType.toString().equals(MediaType.APPLICATION_XML)) {
                        try {
                            JAXBContext context = JAXBContext.newInstance(ResourceInstallCount.class);
                            Marshaller marshaller = context.createMarshaller();
                            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
                            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                            stream.write("<collection>".getBytes());
                            for (ResourceInstallCount installCount : results) {
                                marshaller.marshal(installCount, stream);
                            }
                            stream.write("</collection>".getBytes());
                        } catch (JAXBException e) {
                            throw new WebApplicationException(e);
                        }
                    } else if (mediaType.toString().equals("text/csv")) {
                        CsvWriter<ResourceInstallCount> csvWriter = new CsvWriter<ResourceInstallCount>();
                        csvWriter.setColumns("typeName", "typePlugin", "category.displayName", "version", "count");

                        stream.write((getHeader() + "\n").getBytes());
                        for (ResourceInstallCount installCount : results) {
                            csvWriter.write(installCount, stream);
                        }
                    }
                }
            };
        }
    }

    private Set<Integer> parseIds(String resourceTypeIdParam) {
        Set<Integer> ids = new TreeSet<Integer>();
        if (resourceTypeIdParam != null && !resourceTypeIdParam.isEmpty()) {
            for (String id : resourceTypeIdParam.split(",")) {
                ids.add(Integer.parseInt(id));
            }
        }
        return ids;
    }

    private class OutputDetailedInventorySummary implements StreamingOutput {

        // map of counts keyed by resource type id
        private Map<ResourceInstallCountKey, ResourceInstallCount> installCounts =
            new LinkedHashMap<ResourceInstallCountKey, ResourceInstallCount>();

        private Set<Integer> resourceTypeIds;

        public OutputDetailedInventorySummary(List<ResourceInstallCount> installCounts,
            Set<Integer> resourceTypeIds) {
            this.resourceTypeIds = resourceTypeIds;
            for (ResourceInstallCount installCount : installCounts) {
                this.installCounts.put(new ResourceInstallCountKey(installCount.getTypeId(), installCount.getVersion()),
                    installCount);
            }
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            ResourceCriteria criteria;
            CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor;
            CriteriaQuery<Resource, ResourceCriteria> query;

            CsvWriter<DetailedSummary> csvWriter = new CsvWriter<DetailedSummary>();
            csvWriter.setColumns("typeName", "plugin", "category", "version", "count", "resourceName", "ancestry",
                "description", "resourceTypeName", "resourceVersion", "currentAvailability");

            output.write((getHeader() + "," + getDetailsHeader() + "\n").getBytes());

            // if there are no resource type ids, that means we are fetching everything,
            // that is all resource details for all types.
            if (resourceTypeIds.isEmpty()) {
                criteria = getDetailsQueryCriteria(null);
                queryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                    @Override
                    public PageList<Resource> execute(ResourceCriteria criteria) {
                        return resourceMgr.findResourcesByCriteria(caller, criteria);
                    }
                };
                query = new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);
                for (Resource resource : query) {
                    ResourceInstallCountKey key = new ResourceInstallCountKey(resource.getResourceType().getId(),
                        resource.getVersion());
                    ResourceInstallCount installCount = installCounts.get(key);
                    if (installCount != null) {
                          csvWriter.write(new DetailedSummary(installCount, resource), output);
                    }
                }
            } else {
                for (ResourceInstallCount installCount : installCounts.values()) {
                    if (resourceTypeIds.contains(installCount.getTypeId())) {
                        criteria = getDetailsQueryCriteria(installCount.getTypeId());
                        queryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                            @Override
                            public PageList<Resource> execute(ResourceCriteria criteria) {
                                return resourceMgr.findResourcesByCriteria(caller, criteria);
                            }
                        };
                        query = new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);
                        for (Resource resource : query) {
                            csvWriter.write(new DetailedSummary(installCount, resource), output);
                        }
                    } else {
                        csvWriter.write(new DetailedSummary(installCount, null), output);
                    }
                }
            }
        }
    }

    protected ResourceCriteria getDetailsQueryCriteria(Integer resourceTypeId) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterInventoryStatus(COMMITTED);
        criteria.addSortResourceCategory(ASC);
        criteria.addSortPluginName(ASC);
        criteria.addSortResourceTypeName(ASC);

        if (resourceTypeId != null) {
            criteria.addFilterResourceTypeId(resourceTypeId);
        }

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
        return "Name,Ancestry,Description,Type,Version,Availability";
    }

    protected String toCSV(ResourceInstallCount installCount) {
        return installCount.getTypeName() + "," + installCount.getTypePlugin() + "," +
            installCount.getCategory().getDisplayName() + "," + installCount.getVersion() + "," +
            installCount.getCount();
    }

    protected String toCSV(Resource resource) {
        return resource.getName() + "," + parseAncestry(resource.getAncestry()) + "," +
            resource.getDescription() + "," + resource.getResourceType().getName() + "," + resource.getVersion() +
            "," + resource.getCurrentAvailability().getAvailabilityType();
    }

    private static class ResourceInstallCountKey {
        private int resourceTypeId;
        private String version;

        public ResourceInstallCountKey(int resourceTypeId, String version) {
            this.resourceTypeId = resourceTypeId;
            if (version == null) {
                // Storing empty string here to make equals/hashCode easier. In
                // this context, null and empty string are essentially the same.
                this.version = "";
            } else {
                this.version = version;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceInstallCountKey that = (ResourceInstallCountKey) o;

            if (resourceTypeId != that.resourceTypeId) return false;
            if (!version.equals(that.version)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = resourceTypeId;
            result = 31 * result + version.hashCode();
            return result;
        }
    }

}
