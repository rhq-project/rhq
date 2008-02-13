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
package org.rhq.core.pluginapi.content;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;

/**
 * Components that implement this facet expose the ability to detect and manage packaged content.
 *
 * @author Jeff Ortel
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public interface ContentFacet {
    /**
     * Initializes the content facet, providing the context that should be used for invocations back into the plugin
     * container (see {@link ContentServices}).
     *
     * @param context resource specific context
     */
    void startContentFacet(ContentContext context);

    /**
     * Returns a list of installation steps that will take place when installing the specified package. When the request
     * to install the package is actually placed, the response from that call should contain a reference to the steps
     * specified here, along with the result (success/failure) of each step. If they cannot be determined, this method
     * will return <code>null</code>.
     *
     * @param  packageDetails describes the package to be installed
     *
     * @return steps that will be taken and reported on when deploying this package; <code>null</code> if they cannot be
     *         determined
     */
    List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails);

    /**
     * Requests that the content for the given packages be deployed to the resource. After the facet completes its work,
     * the facet should update each installed package object with the new status and any error message that is
     * appropriate (in the case where the installation failed). This method should not throw exceptions - any errors
     * that occur should be stored in the {@link ResourcePackageDetails} object.
     *
     * @param  packages        the packages to install
     * @param  contentServices a proxy object that allows the facet implementation to be able to request things from the
     *                         plugin container (such as being able to pull down a package's content from an external
     *                         source).
     *
     * @return Contains a reference to each package to be installed. Each reference should describe the results of
     *         attempting to install the package (success/failure).
     */
    DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices);

    /**
     * Requests that the given installed packages be deleted from the resource. After the facet completes its work, the
     * facet should update each installed package object with the new status and any error message that is appropriate
     * (in the case where the installation failed). This method should not throw exceptions - any errors that occur
     * should be stored in the {@link ResourcePackageDetails} object.
     *
     * @param  packages the packages to remove
     *
     * @return Contains a reference to each package that was requested to be removed. Each reference should describe the
     *         results of attempting to remove the package (success/failure).
     */
    RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages);

    /**
     * Asks that the component run a discovery and return information on all currently installed packages of the
     * specified type.
     *
     * @param  type the type of packaged content that should be discovered
     *
     * @return information on all discovered content of the given package type
     */
    Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type);

    /**
     * Asks that a stream of data containing the installed package contents be returned.
     *
     * @param  packageDetails the package whose contents should be streamed back to the caller
     *
     * @return stream containing the full content of the package
     */
    InputStream retrievePackageBits(ResourcePackageDetails packageDetails);
}