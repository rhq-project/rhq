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
import java.util.HashMap;
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

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class InventorySummaryHandler extends AbstractRestBean implements InventorySummaryLocal {

    @EJB
    protected ResourceManagerLocal resourceMgr;

    @Override
    public StreamingOutput generateReport(UriInfo uriInfo, Request request, HttpHeaders headers, boolean includeDetails,
        final List<Integer> resourceTypeIds) {
        final List<ResourceInstallCount> results = getSummaryCounts();
        final MediaType mediaType = headers.getAcceptableMediaTypes().get(0);

        if (includeDetails) {
            return new OutputDetailedInventorySummary(results, new TreeSet<Integer>(resourceTypeIds));
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
                        stream.write((getHeader() + "\n").getBytes());

                        if (resourceTypeIds.isEmpty()) {
                            for (ResourceInstallCount installCount : results) {
                                String record = toCSV(installCount) + "\n";
                                stream.write(record.getBytes());
                            }
                        } else {
                            Set<Integer> ids = new TreeSet<Integer>(resourceTypeIds);
                            for (ResourceInstallCount installCount : results) {
                                if (ids.contains(installCount.getTypeId())) {
                                    String record = toCSV(installCount) + "\n";
                                    stream.write(record.getBytes());
                                }
                            }
                        }
                    }
                }
            };
        }
    }

    private class OutputDetailedInventorySummary implements StreamingOutput {

        // map of counts keyed by resource type id
        private Map<Integer, ResourceInstallCount> installCounts = new HashMap<Integer, ResourceInstallCount>();

        private Set<Integer> resourceTypeIds;

        public OutputDetailedInventorySummary(List<ResourceInstallCount> installCountList,
            Set<Integer> resourceTypeIds) {
            this.resourceTypeIds = resourceTypeIds;
            for (ResourceInstallCount installCount : installCountList) {
                installCounts.put(installCount.getTypeId(), installCount);
            }
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            final ResourceCriteria criteria = getDetailsQueryCriteria(installCounts);
            if (!resourceTypeIds.isEmpty()) {
                criteria.addFilterResourceTypeIds(resourceTypeIds.toArray(new Integer[resourceTypeIds.size()]));
            }

            CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor =
                new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                    @Override
                    public PageList<Resource> execute(ResourceCriteria criteria) {
                        return resourceMgr.findResourcesByCriteria(caller, criteria);
                    }
                };

            CriteriaQuery<Resource, ResourceCriteria> query =
                new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);
            output.write((getHeader() + "\n").getBytes());
            for (Resource resource : query) {
                 ResourceInstallCount installCount = installCounts.get(resource.getResourceType().getId());
                if (installCount != null) {
                    String record = toCSV(installCount) + "," + toCSV(resource) + "\n";
                    output.write(record.getBytes());
                }
            }
        }
    }

    protected ResourceCriteria getDetailsQueryCriteria(Map<Integer, ResourceInstallCount> installCounts) {
        ResourceCriteria criteria = new ResourceCriteria();
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

    protected String toCSV(ResourceInstallCount installCount) {
        return installCount.getTypeName() + "," + installCount.getTypePlugin() + "," +
            installCount.getCategory().getDisplayName() + "," + installCount.getVersion() + "," +
            installCount.getCount();
    }

    protected String toCSV(Resource resource) {
        return resource.getName() + "," + ReportHelper.parseAncestry(resource.getAncestry()) + "," +
            resource.getDescription() + "," + resource.getResourceType().getName() + "," + resource.getVersion() +
            "," + resource.getCurrentAvailability().getAvailabilityType();
    }
}
