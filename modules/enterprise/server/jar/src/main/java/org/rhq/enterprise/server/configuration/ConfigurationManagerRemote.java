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

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.group.AggregatePluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.AggregateResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UpdateException;

/**
 * The configuration manager which allows you to request resource configuration changes, view current resource
 * configuration and previous update history and view/edit plugin configuration.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface ConfigurationManagerRemote {

    @WebMethod
    AggregatePluginConfigurationUpdate getAggregatePluginConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "configurationUpdateId") int configurationUpdateId) //
        throws FetchException;

    @WebMethod
    AggregateResourceConfigurationUpdate getAggregateResourceConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "configurationUpdateId") int configurationUpdateId) //
        throws FetchException;

    @WebMethod
    Configuration getConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "configurationId") int configurationId) //
        throws FetchException;

    /**
     * Get the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     * resource's plugin configuration is not yet initialized.
     *
     * @param  user             The logged in user's subject.
     * @param  resourceId       Resource Id
     *
     * @return the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     *         resource's configuration is not yet initialized
     * @throws FetchException
     */
    @Nullable
    @WebMethod
    Configuration getPluginConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId) //
        throws FetchException;

    /**
     * Get the current Resource configuration.
     * @param  subject             The logged in user's subject.
     * @param resourceId        A resource id.
     * @return The specified configuration.
     * 
     * @throws FetchException In case where there was a problem fetching the resource configuration
     */
    @WebMethod
    Configuration getResourceConfiguration(Subject subject, //
        @WebParam(name = "resourceId") int resourceId) //
        throws FetchException;

    @WebMethod
    PluginConfigurationUpdate getLatestPluginConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId) //
        throws FetchException;

    @WebMethod
    ResourceConfigurationUpdate getLatestResourceConfigurationUpdate( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId) //
        throws FetchException;

    /**
     * Get whether the the specified resource is in the process of updating its configuration.
     * @param user             The logged in user's subject.
     * @param resourceId       A resource id.
     * @return True if in progress, else False.
     * @throws FetchException
     */
    @WebMethod
    boolean isResourceConfigurationUpdateInProgress( //
        @WebParam(name = "user") Subject user, //
        @WebParam(name = "resourceId") int resourceId) throws FetchException;

    @WebMethod
    boolean isAggregateResourceConfigurationUpdateInProgress( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceGroupId") int resourceGroupId) //
        throws FetchException;

    /* this currently doesn't build because jaxws requires a default, no-arg constructor from all objects in the graph
     * in order to perform serialization correctly, and java.util.Map does not have one (because it's an interface)
    @WebMethod
    int scheduleAggregateResourceConfigurationUpdate(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "compatibleGroupId") int compatibleGroupId, //
        @WebParam(name = "newResourceConfigurationMap") Map<Integer, Configuration> newResourceConfigurationMap) //
        throws UpdateException;
        */

    /**
     * Updates the plugin configuration used to connect and communicate with the resource. The given <code>
     * newConfiguration</code> is usually a modified version of a configuration returned by
     * {@link #getPluginConfiguration(Subject, int)}.
     *
     * @param  user             The logged in user's subject.
     * @param  resourceId       a {@link Resource} id
     * @param  newConfiguration the new plugin configuration
     *
     * @return the plugin configuration update item corresponding to this request
     * @throws UpdateException In case where there was a problem fetching the plugin configuration
     */
    @WebMethod
    PluginConfigurationUpdate updatePluginConfiguration( //
        @WebParam(name = "user") Subject user, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "newConfiguration") Configuration newConfiguration) //
        throws UpdateException;

    /**
     * This method is called when a user has requested to change the resource configuration for an existing resource. If
     * the user does not have the proper permissions to change the resource's configuration, an exception is thrown.
     *
     * <p>This will not wait for the agent to finish the configuration update. This will return after the request is
     * sent. Once the agent finishes with the request, it will send the completed request information to
     * {@link #completedResourceConfigurationUpdate(AbstractResourceConfigurationUpdate)}.</p>
     *
     * @param  user             The logged in user's subject.
     * @param  resourceId       identifies the resource to be updated
     * @param  newConfiguration the resource's desired new configuration
     *
     * @return the resource configuration update item corresponding to this request
     * @throws UpdateException 
     */
    @WebMethod
    ResourceConfigurationUpdate updateResourceConfiguration( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "newConfiguration") Configuration newConfiguration) throws UpdateException;

    //--Methods that are not available in spec--

    /**
     * Get the currently live resource configuration for the {@link Resource} with the given id. This actually asks for
     * the up-to-date configuration directly from the agent. An exception will be thrown if communications with the
     * agent cannot be made.
     *
     * @param  user             The logged in user's subject.
     * @param                   resourceId
     * @param pingAgentFirst
     * 
     * @return the live configuration
     *
     * @throws Exception if failed to get the configuration from the agent
     */
    Configuration getLiveResourceConfiguration( //
        @WebParam(name = "user") Subject user, //
        @WebParam(name = "resourceId") int resourceId, boolean pingAgentFirst) throws Exception;

}