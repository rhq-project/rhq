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
package org.rhq.enterprise.server.content;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.content.transfer.ContentDiscoveryReport;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Implementation of the server-side interface used by the agents to perform content-related stuff.
 *
 * @author John Mazzitelli
 * @author Jason Dobies
 */
public class ContentServerServiceImpl implements ContentServerService {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    // ContentServerService Implementation  --------------------------------------------

    public void mergeDiscoveredPackages(ContentDiscoveryReport report) {
        long start = System.currentTimeMillis();
        ContentManagerLocal contentManager = LookupUtil.getContentManager();
        contentManager.mergeDiscoveredPackages(report);
        long elapsed = (System.currentTimeMillis() - start);
        if (elapsed > 30000L) {
            log.info("Performance: merged package report [" + report + "] in (" + elapsed + ")ms");
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: merged package report [" + report + "] in (" + elapsed + ")ms");
        }
    }

    public void completeDeployPackageRequest(DeployPackagesResponse response) {
        ContentManagerLocal contentManager = LookupUtil.getContentManager();
        contentManager.completeDeployPackageRequest(response);
    }

    public void completeDeletePackageRequest(RemovePackagesResponse response) {
    }

    public void completeRetrievePackageBitsRequest(ContentServiceResponse response, InputStream contentStream) {
    }

    public Set<ResourcePackageDetails> loadDependencies(int requestId, Set<PackageDetailsKey> dependencyPackages) {
        return null;
    }

    public long downloadPackageBitsGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream) {
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        return manager.outputPackageVersionBitsGivenResource(resourceId, packageDetailsKey, outputStream);
    }

    public long downloadPackageBitsForChildResource(int parentResourceId, String resourceTypeName, PackageDetailsKey packageDetailsKey, OutputStream outputStream) {
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        return manager.outputPackageBitsForChildResource(parentResourceId, resourceTypeName,
            packageDetailsKey, outputStream);
    }

    public long downloadPackageBitsRangeGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte) {
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        return manager.outputPackageVersionBitsRangeGivenResource(resourceId, packageDetailsKey, outputStream,
            startByte, endByte);
    }

    public PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(int resourceId, PageControl pc) {
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        long start = System.currentTimeMillis();
        PageList<PackageVersionMetadataComposite> metadataMap = manager.getPackageVersionMetadata(resourceId, pc);
        long elapsed = (System.currentTimeMillis() - start);
        if (elapsed > 30000L) {
            log.info("Performance: metadata for resource [" + resourceId + "] has [" + metadataMap.size()
                + "] packages in (" + elapsed + ")ms");
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: metadata for resource [" + resourceId + "] has [" + metadataMap.size()
                + "] packages in (" + elapsed + ")ms");
        }

        return metadataMap;
    }

    public String getResourceSubscriptionMD5(int resourceId) {
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        long start = System.currentTimeMillis();
        String metadataMD5 = manager.getResourceSubscriptionMD5(resourceId);
        long elapsed = (System.currentTimeMillis() - start);
        if (elapsed > 5000L) {
            log.info("Performance: metadata for resource [" + resourceId + "] has MD5 [" + metadataMD5 + "] in ("
                + elapsed + ")ms");
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: metadata for resource [" + resourceId + "] has MD5 [" + metadataMD5 + "] in ("
                + elapsed + ")ms");
        }

        return metadataMD5;
    }

    public long getPackageBitsLength(int resourceId, PackageDetailsKey packageDetailsKey) {
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        long size = manager.getPackageBitsLength(resourceId, packageDetailsKey);
        return size;
    }
}