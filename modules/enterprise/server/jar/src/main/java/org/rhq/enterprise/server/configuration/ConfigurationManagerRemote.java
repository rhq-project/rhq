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
package org.rhq.enterprise.server.configuration;

import java.util.Map;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;

/**
 * The configuration manager which allows you to request resource configuration changes, view current resource
 * configuration and previous update history and view/edit plugin configuration.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Remote
public interface ConfigurationManagerRemote {

    GroupPluginConfigurationUpdate getGroupPluginConfigurationUpdate(Subject subject, int configurationUpdateId);

    GroupResourceConfigurationUpdate getGroupResourceConfigurationUpdate(Subject subject, int configurationUpdateId);

    /**
     * Get the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     * resource's plugin configuration is not yet initialized.
     *
     * @param  subject     the user who wants to see the information
     * @param  resourceId a {@link Resource} id
     *
     * @return the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     *         resource's configuration is not yet initialized
     * @throws FetchException
     */
    Configuration getPluginConfiguration(Subject subject, int resourceId);

    /**
     * Get the current Resource configuration.
     * @param  subject             The logged in user's subject.
     * @param resourceId        A resource id.
     * @return The specified configuration.
     * 
     * @throws FetchException In case where there was a problem fetching the resource configuration
     */
    Configuration getResourceConfiguration(//
        Subject subject, int resourceId);

    PluginConfigurationUpdate getLatestPluginConfigurationUpdate(Subject subject, int resourceId);

    ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId);

    /**
     * Get whether the the specified resource is in the process of updating its configuration.
     * @param subject          The logged in user's subject.
     * @param resourceId       A resource id.
     * @return True if in progress, else False.
     * @throws FetchException
     */
    boolean isResourceConfigurationUpdateInProgress(Subject subject, int resourceId);

    boolean isGroupResourceConfigurationUpdateInProgress(Subject subject, int resourceGroupId);

    int scheduleGroupResourceConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> newResourceConfigurationMap);

    /**
     * Updates the plugin configuration used to connect and communicate with the resource. The given <code>
     * newConfiguration</code> is usually a modified version of a configuration returned by
     * {@link #getPluginConfiguration(Subject, int)}.
     *
     * @param  subject          The logged in user's subject.
     * @param  resourceId       a {@link Resource} id
     * @param  newConfiguration the new plugin configuration
     *
     * @return the plugin configuration update item corresponding to this request
     */
    PluginConfigurationUpdate updatePluginConfiguration(Subject subject, int resourceId, Configuration newConfiguration)
        throws ResourceNotFoundException;

    /**
     * This method is called when a user has requested to change the resource configuration for an existing resource. If
     * the user does not have the proper permissions to change the resource's configuration, an exception is thrown.
     *
     * <p>This will not wait for the agent to finish the configuration update. This will return after the request is
     * sent. Once the agent finishes with the request, it will send the completed request information to
     * {@link #completedResourceConfigurationUpdate(AbstractResourceConfigurationUpdate)}.</p>
     *
     * @param  subject          The logged in user's subject.
     * @param  resourceId       identifies the resource to be updated
     * @param  newConfiguration the resource's desired new configuration
     *
     * @return the resource configuration update item corresponding to this request
     */

    ResourceConfigurationUpdate updateResourceConfiguration(Subject subject, int resourceId,

    Configuration newConfiguration) throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException;

    /**
     * Get the currently live resource configuration for the {@link Resource} with the given id. This actually asks for
     * the up-to-date configuration directly from the agent. An exception will be thrown if communications with the
     * agent cannot be made.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId resourceId
     * @param pingAgentFirst
     * 
     * @return the live configuration
     *
     * @throws Exception if failed to get the configuration from the agent
     */
    Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst)
        throws Exception;

    /**
     * Return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     * specified id.
     *
     * @param  subject         the user who is requesting the resource configuration definition
     * @param  resourceTypeId identifies the resource type whose resource configuration definition is being requested
     *
     * @return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     *         specified id, or <code>null</code> if the ResourceType does not define a resource configuration
     */
    ConfigurationDefinition getResourceConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId);

    /**
     * Return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     * specified id. The templates will be loaded in the definition returned from this call.
     *
     * @param  subject         the user who is requesting the resource configuration definition
     * @param  resourceTypeId identifies the resource type whose resource configuration definition is being requested
     *
     * @return the resource configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     *         specified id, or <code>null</code> if the ResourceType does not define a resource configuration
     */
    ConfigurationDefinition getResourceConfigurationDefinitionWithTemplatesForResourceType(Subject subject,
        int resourceTypeId);

    /**
     * Return the plugin configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     * specified id.
     *
     * @param  subject         the user who is requesting the plugin configuration definition
     * @param  resourceTypeId identifies the resource type whose plugin configuration definition is being requested
     *
     * @return the plugin configuration definition for the {@link org.rhq.core.domain.resource.ResourceType} with the
     *         specified id, or <code>null</code> if the ResourceType does not define a plugin configuration
     */
    ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId);

    /**
     * Return the deploy configuration definition for the {@link org.rhq.core.domain.content.PackageType} with the
     * specified id.
     *
     * @param  subject        the user who is requesting the plugin configuration definition
     * @param  packageTypeId  identifies the package type whose configuration definition is being requested
     *
     * @return the  the deploy configuration definition for the {@link org.rhq.core.domain.content.PackageType} with the
     * specified id.
     */
    ConfigurationDefinition getPackageTypeConfigurationDefinition(Subject subject, int packageTypeId);

    Configuration translateResourceConfiguration(Subject subject, int resourceId, Configuration configuration,
        boolean fromStructured) throws ResourceNotFoundException;
}