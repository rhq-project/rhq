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
package org.rhq.core.clientapi.agent.content;

import java.util.List;
import java.util.Set;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.content.transfer.ContentDiscoveryReport;
import org.rhq.core.domain.content.transfer.DeletePackagesRequest;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesRequest;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.RetrievePackageBitsRequest;

/**
 * The interface to the agent's content subsystem that the server can call into.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public interface ContentAgentService {
    /**
     * Returns all content known for the resource as of the last discovery that was executed. In other words, this
     * returns the installed packages known to the Plugin Container for the resource without triggering another
     * discovery.
     *
     * @param  resourceId identifies the resource
     *
     * @return installed packages for the resource; <code>null</code> if no content exist for the resource or if a
     *         previous discovery has not taken place
     */
    Set<ResourcePackageDetails> getLastDiscoveredResourcePackages(int resourceId);

    /**
     * Immediately triggers a content discovery. The discovery will be executed against the specified resource for the
     * specified package type.
     *
     * @param  resourceId      resource against which the discovery will be made; must correspond to a valid resource in
     *                         the plugin container's inventory
     * @param  packageTypeName name of the type of package to discover in the scan
     *
     * @return report of discovered content
     *
     * @throws PluginContainerException if an error occurs at any point in the discovery (including in the plugin
     *                                  itself)
     */
    ContentDiscoveryReport executeResourcePackageDiscoveryImmediately(int resourceId, String packageTypeName)
        throws PluginContainerException;

    /**
     * Requests that the plugin translate the package's metadata into domain specific installation instructions. These
     * instructions can then be displayed to the user. Additionally, once the call to
     * {@link #deployPackages(org.rhq.core.domain.content.transfer.DeployPackagesRequest)} is made, the results of each
     * individual step will be reported. Installation steps are optional. This method may return <code>null</code> if
     * the plugin chooses to not express the installation in terms of steps.
     *
     * @param  resourceId     identifies the resource against which the package in question will be deployed
     * @param  packageDetails contains metadata that describes the package to be installed, including the configuration
     *                        values specified by the user for this deployment (these values may factor into the
     *                        translation of the steps)
     *
     * @return list of steps if the package deployment can be defined in such terms; <code>null</code> if the plugin
     *         chooses not to explicitly describe its steps
     *
     * @throws PluginContainerException if an error occurs at any point, including in the plugin itself
     */
    List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails)
        throws PluginContainerException;

    /**
     * Begins the process of deploying a new set of versioned packages of content to a resource. The plugin will call
     * back into the server to retrieve the bits for the package at a later time. Additionally, the plugin may determine
     * more packages need be installed, in which case the plugin container will be responsible for notifying the server
     * of the new dependencies. Note that this method should not throw any exceptions; instead, all error conditions
     * should be indicated in the response to this request.
     *
     * @param request information necessary to know what content to deploy (must not be <code>null</code>)
     */
    void deployPackages(DeployPackagesRequest request);

    /**
     * Deletes existing content from a resource. Note that this method should not throw any exceptions; instead all
     * error conditions should be indicated in the response to this request.
     *
     * @param request information necessary to know what content to delete (must not be <code>null</code>)
     */
    void deletePackages(DeletePackagesRequest request);

    /**
     * Requests the plugin retrieve the content for a specified package and send the data to the server. Note that this
     * method should not throw any exceptions; instead all error conditions should be indicated in the response to this
     * request.
     *
     * @param request information necessary to know what content to retrieve (must not be <code>null</code>)
     */
    void retrievePackageBits(RetrievePackageBitsRequest request);
}