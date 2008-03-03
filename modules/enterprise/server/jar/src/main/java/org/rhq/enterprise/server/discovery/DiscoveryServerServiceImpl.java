/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.discovery;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.discovery.InventoryReportResponse;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.concurrent.AvailabilityReportSerializer;

/**
 * This is the service that receives inventory data from agents. As agents discover resources, they report back to the
 * server what they have found via this service. // TODO GH: Should this be renamed inventory server service?
 *
 * @author John Mazzitelli
 */
public class DiscoveryServerServiceImpl implements DiscoveryServerService {
    private Log log = LogFactory.getLog(DiscoveryServerServiceImpl.class);

    /**
     * @see DiscoveryServerService#mergeInventoryReport(InventoryReport)
     */
    public InventoryReportResponse mergeInventoryReport(InventoryReport report) throws InvalidInventoryReportException {
        long start = System.currentTimeMillis();
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        InventoryReportResponse response = discoveryBoss.mergeInventoryReport(report);
        log.info("Performance: inventory merge of [" + response.getUuidToIntegerMapping().size() + "] resource in ("
            + (System.currentTimeMillis() - start) + ")ms");
        return response;
    }

    // TODO GH: Should this be on the measurement server service?
    public boolean mergeAvailabilityReport(AvailabilityReport availabilityReport) {
        AvailabilityReportSerializer.getSingleton().lock(availabilityReport.getAgentName());
        try {
            String reportToString = availabilityReport.toString(false);
            log.debug("Processing " + reportToString);

            long start = System.currentTimeMillis();
            AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();
            boolean ok = availabilityManager.mergeAvailabilityReport(availabilityReport);
            long elapsed = (System.currentTimeMillis() - start);

            // log at INFO level if we are going to ask for a full report, its a full report or it took a long time, DEBUG otherwise
            if (!ok || !availabilityReport.isChangesOnlyReport() || (elapsed > 20000L)) {
                log.info("Processed " + reportToString + " - need full=[" + !ok + "] in (" + elapsed + ")ms");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Processed " + reportToString + " - need full=[" + !ok + "] in (" + elapsed + ")ms");
                }
            }

            return ok;
        } catch (Exception e) {
            log.info("Error processing availability report from [" + availabilityReport.getAgentName() + "]: "
                + ThrowableUtil.getAllMessages(e));
            return true; // not sure what happened, but avoid infinite recursion during error conditions; do not ask for a full report
        } finally {
            AvailabilityReportSerializer.getSingleton().unlock(availabilityReport.getAgentName());
        }
    }

    public Resource getResourceTree(int rootResourceId) {
        long start = System.currentTimeMillis();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Resource resource = resourceManager.getResourceTree(rootResourceId);
        log.debug("Performance: get resource tree [" + rootResourceId + "] timing ("
            + (System.currentTimeMillis() - start) + ")ms");
        return resource;
    }

    public Map<Integer, InventoryStatus> getInventoryStatus(int rootResourceId, boolean descendents) {
        long start = System.currentTimeMillis();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Map<Integer, InventoryStatus> statuses = resourceManager.getResourceStatuses(rootResourceId, descendents);
        log.debug("Performance: get inventory statuses for [" + statuses.size() + "] timing ("
            + (System.currentTimeMillis() - start) + ")ms");
        return statuses;
    }

    public void setResourceError(ResourceError resourceError) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        resourceManager.addResourceError(resourceError);
    }

    public MergeResourceResponse addResource(Resource resource, int creatorSubjectId) {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        return discoveryBoss.addResource(resource, creatorSubjectId);
    }

    public boolean updateResourceVersion(int resourceId, String version) {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        return discoveryBoss.updateResourceVersion(resourceId, version);
    }
}