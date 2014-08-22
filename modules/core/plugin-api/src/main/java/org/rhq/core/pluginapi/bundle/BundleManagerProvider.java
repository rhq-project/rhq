/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pluginapi.bundle;

import java.io.OutputStream;
import java.util.List;

import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;

/**
 * Provides bundle functionality that plugin components will need in order to process bundles.
 *
 * @since 3.0
 * @author John Mazzitelli
 */
public interface BundleManagerProvider {
    /**
     * Bundle plugins call back into this manager to add progressive auditing of a deployment.
     *
     * @param deployment The resource deployment tracking this bundle deployment
     * @param action The audit action, a short summary easily displayed (e.g "File Download")
     * @param info Info about the action target, easily displayed (e.g. "myfile.zip")
     * @param category A useful categorization of the audit, defaults to null
     * @param status Optional, defaults to SUCCESS
     * @param message Optional, verbose message being audited, failure message, etc
     * @param attachment Optional, verbose data, such as full file text
     * @throws Exception
     */
    void auditDeployment(BundleResourceDeployment deployment, String action, String info,
        BundleResourceDeploymentHistory.Category category, BundleResourceDeploymentHistory.Status status,
        String message, String attachment) throws Exception;

    /**
     * Bundle plugins call back into this manager to obtain the bundle files that belong to a given bundle version.
     *
     * @param bundleVersion a bundle version
     *
     * @return the bundle files that are associated with the given bundle
     *
     * @throws Exception on failure
     */
    List<PackageVersion> getAllBundleVersionPackageVersions(BundleVersion bundleVersion) throws Exception;

    /**
     * Bundle plugins call back into this manager to obtain the bundle file content for the given package.
     *
     * @param packageVersion the package whose bits are to be downloaded
     * @param outputStream where the package bits will get written to
     *
     * @return the size of the package version content that was downloaded and output
     *
     * @throws Exception on failure
     */
    long getFileContent(PackageVersion packageVersion, OutputStream outputStream) throws Exception;

    /**
     * Requests participation of the bundle target resource component in the bundle deployment.
     *
     * @param bundleTarget bundle target resource
     * @param handoverRequest handover parameters and context
     * @return a report object indicating success or failure
     *
     * @since 4.12
     */
    BundleHandoverResponse handoverContent(Resource bundleTarget, BundleHandoverRequest handoverRequest);
}
