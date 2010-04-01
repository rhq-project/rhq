/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.clientapi.server.bundle;

import java.io.OutputStream;
import java.util.List;

import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.communications.command.annotation.Timeout;
import org.rhq.core.domain.bundle.BundleDeploymentHistory;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.content.PackageVersion;

/**
 * Interface for agents to use when needing to inform the server about bundle tasks. Implementations of this interface, once registered with the
 * plugin container, will received the requests that were issued updated with the operation results.
 *
 * @author John Mazzitelli
 */
public interface BundleServerService {

    @Asynchronous(guaranteedDelivery = true)
    void addDeploymentHistory(int bundleDeploymentId, BundleDeploymentHistory history);

    /**
     * Requests that the server download and stream the bits for the specified package version.
     * If the package cannot be found, an exception will be thrown.
     *
     * @param  packageVersion identifies the package to download
     * @param  outputStream   an output stream where the server should write the package contents. It is up to the
     *                        caller to prepare this output stream in order to write the package content to an
     *                        appropriate location.
     *
     * @return the number of bytes written to the output stream - this is the size of the package version that was
     *         downloaded
     */
    @Timeout(45 * 60 * 1000L)
    @LimitedConcurrency(ContentServerService.CONCURRENCY_LIMIT_CONTENT_DOWNLOAD)
    long downloadPackageBits(PackageVersion packageVersion, OutputStream outputStream);

    List<PackageVersion> getAllBundleVersionPackageVersions(int bundleVersionId);

    /**
     * Set the (completion) status of a deployment.  If required, detailed messages should be provided via
     * addDeploymentHistory(). Deployments are automatically initialized to IN_PROGRESS.
     * @param bundleDeploymentId
     * @param status
     */
    @Asynchronous(guaranteedDelivery = true)
    void setBundleDeploymentStatus(int bundleDeploymentId, BundleDeploymentStatus status);
}