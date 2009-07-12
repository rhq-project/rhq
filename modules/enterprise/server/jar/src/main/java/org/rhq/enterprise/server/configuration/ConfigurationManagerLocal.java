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

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.group.AbstractAggregateConfigurationUpdate;
import org.rhq.core.domain.configuration.group.AggregatePluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.AggregateResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.configuration.job.AggregatePluginConfigurationUpdateJob;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UpdateException;

/**
 * The configuration manager which allows you to request resource configuration changes, view current resource
 * configuration and previous update history and view/edit plugin configuration.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Local
public interface ConfigurationManagerLocal {

    /**
     * Updates the plugin configuration used to connect and communicate with the resource using the information in the
     * passed {@link PluginConfigurationUpdate} object. This update object will be changed to reflect the result of the
     * plugin configuration update attempt. This is an internal method called by
     * {@link #updatePluginConfiguration(Subject, int, Configuration)} and {@link AggregatePluginConfigurationUpdateJob}
     * . It is not intended to be used for general, public consumption.
     *
     * @param update a previously server-side persisted update, which has not yet made it to the agent
     */
    void completePluginConfigurationUpdate(PluginConfigurationUpdate update);

    void completePluginConfigurationUpdate(Integer updateId);

    /** This does not perform permission checks and should be used internally only. In general, use
     * {@link #getPluginConfiguration(Subject, int)}.
     */
    Configuration getPluginConfiguration(int resourceId);

    /** This does not perform permission checks and should be used internally only. In general, use
     * {@link #getCurrentResourceConfiguration}.
     * @throws FetchException TODO
     */
    Configuration getResourceConfiguration(int resourceId) throws FetchException;

    /**
     * Get the currently live resource configuration for the {@link Resource} with the given id. This actually asks for
     * the up-to-date configuration directly from the agent. An exception will be thrown if communications with the
     * agent cannot be made.
     *
     * @param  subject     the user who wants to see the information
     * @param  resourceId a {@link Resource} id
     * @param  pingAgentFirst true if the underlying Agent should be pinged successfully before attempting to retrieve
     *                        the configuration, or false otherwise
     *
     * @return the live configuration
     *
     * @throws Exception if failed to get the configuration from the agent
     */
    Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst)
        throws Exception;

    PageList<PluginConfigurationUpdate> findPluginConfigurationUpdates(Subject subject, int resourceId, Long beginDate,
        Long endDate, PageControl pc);

    /**
     * Returns the list of all resource configuration updates for the given resource. This will show you an audit trail
     * of the update history for the resource (who updated it, when and what did they do). You can pick one
     * configuration version to later rollback to that version via
     * {@link #updateResourceConfiguration(Subject, int, Configuration)}.
     *
     * @param  subject         the user who wants to see the information
     * @param  resourceId     the resource whose update requests are to be returned, if null will not filter by resourceId
     * @param  beginDate      filter used to show only results occurring after this epoch millis parameter, nullable
     * @param  endDate        filter used to show only results occurring before this epoch millis parameter, nullable
     * @param  suppressOldest if true, will not include the oldest element in the history (usually the initial update)
     * @param  pc             the pagination controls
     *
     * @return the resource's complete list of updates (will be empty (not <code>null</code>) if none)
     */
    PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(Subject subject, Integer resourceId,
        Long beginDate, Long endDate, boolean suppressOldest, PageControl pc);

    PluginConfigurationUpdate getPluginConfigurationUpdate(Subject subject, int configurationUpdateId);

    /**
     * Returns a single resource configuration update
     *
     * @param  subject                the user who wants to see the information
     * @param  configurationUpdateId the ID of the configuration update entity to return
     *
     * @return the resource configuration update
     */
    ResourceConfigurationUpdate getResourceConfigurationUpdate(Subject subject, int configurationUpdateId);

    /**
     * This method is called when a user has requested to change the resource configuration for an existing resource to
     * one of its previous values. If the user does not have the proper permissions to change the resource's
     * configuration, an exception is thrown.
     *
     * <p>This will not wait for the agent to finish the configuration update. This will return after the request is
     * sent. Once the agent finishes with the request, it will send the completed request information to
     * {@link #completeResourceConfigurationUpdate}.</p>
     *
     * @param  subject          the user who is requesting the update
     * @param  resourceId      identifies the resource to be updated
     * @param  configHistoryId the id of the resource's previous configuration to rollback to
     *
     * @throws ConfigurationUpdateException if the configHistoryId does not exist
     */
    void rollbackResourceConfiguration(Subject subject, int resourceId, int configHistoryId)
        throws ConfigurationUpdateException;

    /**
     * For internal use only - do not call this method. This is for
     * {@link #updateResourceConfiguration(Subject, int, Configuration)} to call with REQUIRES_NEW transaction scope so
     * it can force the new request to be committed to the DB. Also used by
     * {@link #getLatestResourceConfigurationUpdate(Subject, int)} and
     * {@link #scheduleAggregateResourceConfigurationUpdate}.
     *
     * @param  subject
     * @param  resourceId
     * @param  newConfiguration
     * @param  newStatus
     * @param  newSubject       user to associate with this update change (may be <code>null</code>)
     *
     * @param isPartofAggregateUpdate
     * @return the persisted Resource Configuration update, or null if the specified Configuration is identical to the
     *         currently persisted Configuration
     */
    @Nullable
    ResourceConfigurationUpdate persistNewResourceConfigurationUpdateHistory(Subject subject, int resourceId,
        Configuration newConfiguration, ConfigurationUpdateStatus newStatus, String newSubject,
        boolean isPartofAggregateUpdate);

    /**
     * A callback method that is called when an agent has completed updating a resource's configuration.
     *
     * @param response information that contains the status of the update (i.e. was it successfully updated or did it
     *                 fail?) as well as the configuration if it failed (with the properties containing error messages
     *                 to describe what failed). If the update was a success, the completed request's configuration will
     *                 be <code>null</code> to indicate that the configuration that was sent to the agent was used
     *                 as-is.
     */
    void completeResourceConfigurationUpdate(ConfigurationUpdateResponse response);

    void checkForCompletedGroupResourceConfigurationUpdate(int resourceConfigUpdateId);

    ConfigurationUpdateResponse executePluginConfigurationUpdate(PluginConfigurationUpdate update);

    public void purgePluginConfigurationUpdate(Subject subject, int configurationUpdateId, boolean purgeInProgress);

    /**
     * This deletes the update information belonging to the {@link AbstractResourceConfigurationUpdate} object with the
     * given ID. Once this returns, the complete audit trail for that update will be gone and you will not be able to
     * rollback to that configuration.
     *
     * <p>Under normal circumstances, you will not want to purge an update that is currently in progress. However, there
     * may be conditions in which an update request "gets stuck" in the in-progress state, even though you know the
     * agent will never report the completed request (typically caused by an unusual crash of the agent). In this case,
     * you should pass <code>true</code> in for the <code>purgeInProgress</code> parameter to tell this method to delete
     * the request even if it says it is in-progress.
     *
     * @param subject                the user who is requesting the purge
     * @param configurationUpdateId identifies the update record to be deleted
     * @param purgeInProgress       if <code>true</code>, delete it even if its
     *                              {@link ConfigurationUpdateStatus#INPROGRESS in progress}
     */
    void purgeResourceConfigurationUpdate(Subject subject, int configurationUpdateId, boolean purgeInProgress);

    /**
     * This deletes one or more configuration updates from the resource's configuration history.
     *
     * @param subject                 the user who is requesting the purge
     * @param configurationUpdateIds identifies the update records to be deleted
     * @param purgeInProgress        if <code>true</code>, delete those even if
     *                               {@link ConfigurationUpdateStatus#INPROGRESS in progress}
     */
    void purgeResourceConfigurationUpdates(Subject subject, int[] configurationUpdateIds, boolean purgeInProgress);

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
    @Nullable
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
    @Nullable
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
    @Nullable
    ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId);

    /**
     * Merge the specified configuration update into the DB.
     *
     * @param  configurationUpdate a configuration update
     *
     * @return an attached copy of the configuration update
     */
    AbstractResourceConfigurationUpdate mergeConfigurationUpdate(AbstractResourceConfigurationUpdate configurationUpdate);

    /**
     * This is a generic method that any caller can use to obtain any configuration given a configuration ID. This can
     * be used to obtain any configuration, which includes such things as an operation's parameters or an operations
     * results.
     *
     * @param  id identifies the configuration to return
     *
     * @return the configuration with the given ID, or <code>null</code> if there is no configuration with that ID
     */
    Configuration getConfigurationById(int id);

    /**
     * This is a convenience method that is equivalent to the following: <code>
     * definition.getDefaultTemplate().getConfiguration()</code> If the definition is already a managed bean, then there
     * is no real reason to call this since EJB3 will traverse the object relationships just fine. However, if the
     * definition is detached from the managed context, and because of the lazy-loading semantics of the entities
     * involved in the call chain above, then you can use this method since it will re-attach it and get the
     * configuration from the default template as shown above.
     *
     * @param  definition a configuration definition
     *
     * @return the {@link Configuration} from the default {@link ConfigurationTemplate} of the passed definition
     */
    Configuration getConfigurationFromDefaultTemplate(ConfigurationDefinition definition);

    AggregatePluginConfigurationUpdate getAggregatePluginConfigurationById(int configurationUpdateId);

    PageList<ConfigurationUpdateComposite> findPluginConfigurationUpdateCompositesByParentId(int configurationUpdateId,
        PageControl pageControl);

    PageList<ConfigurationUpdateComposite> findResourceConfigurationUpdateCompositesByParentId(
        int configurationUpdateId, PageControl pageControl);

    PageList<Integer> findPluginConfigurationUpdatesByParentId(int configurationUpdateId, PageControl pageControl);

    long getPluginConfigurationUpdateCountByParentId(int configurationUpdateId);

    int createAggregateConfigurationUpdate(AbstractAggregateConfigurationUpdate update);

    int scheduleAggregatePluginConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> pluginConfigurationUpdate) throws UpdateException;

    PageList<AggregatePluginConfigurationUpdate> findAggregatePluginConfigurationUpdatesByGroupId(int groupId,
        PageControl pc);

    PageList<AggregateResourceConfigurationUpdate> findAggregateResourceConfigurationUpdatesByGroupId(int groupId,
        PageControl pc);

    ConfigurationUpdateStatus updateAggregatePluginConfigurationUpdateStatus(int aggregatePluginConfigurationUpdateId,
        String errorMessages);

    int deleteAggregatePluginConfigurationUpdates(Subject subject, Integer resourceGroupId,
        Integer[] aggregatePluginConfigurationUpdateIds);

    int deleteAggregateResourceConfigurationUpdates(Subject subject, Integer resourceGroupId,
        Integer[] aggregateResourceConfigurationUpdateIds);

    void updateAggregateConfigurationUpdate(AbstractAggregateConfigurationUpdate groupUpdate);

    void deleteConfigurations(List<Integer> configurationIds);

    void deleteProperties(int[] propertyIds);

    PageList<Integer> findResourceConfigurationUpdatesByParentId(int aggregateConfigurationUpdateId,
        PageControl pageControl);

    long getResourceConfigurationUpdateCountByParentId(int aggregateConfigurationUpdateId);

    void executeResourceConfigurationUpdate(int updateId);

    AggregateResourceConfigurationUpdate getAggregateResourceConfigurationById(int configurationUpdateId);

    Map<Integer, Configuration> getResourceConfigurationMapForAggregateUpdate(
        Integer aggregateResourceConfigurationUpdateId);

    Map<Integer, Configuration> getResourceConfigurationMapForCompatibleGroup(ResourceGroup compatibleGroup);

    /**
     * Returns the current Resource configurations for the members in the specified compatible group.
     *
     * @param subject the current subject
     * @param groupId the id of the compatible group
     * @return
     * @throws ConfigurationUpdateInProgressException if config updates, for the group or any member, are in progress, 
     * @throws Exception if 1) one or more of the group's members are DOWN, or 2) we fail to retrieve one or more member
     *         live configs from the corresponding Agents
     */
    Map<Integer, Configuration> getResourceConfigurationsForCompatibleGroup(Subject subject, int groupId)
        throws ConfigurationUpdateStillInProgressException, Exception;

    Map<Integer, Configuration> getPluginConfigurationsForCompatibleGroup(Subject subject, int groupId)
        throws ConfigurationUpdateStillInProgressException, Exception;

    Map<Integer, Configuration> getPluginConfigurationMapForAggregateUpdate(Integer aggregatePluginConfigurationUpdateId);

    /**
     * The purpose of this method is really to clean up requests when we detect
     * they probably will never move out of the in-progress status.  This will occur if the
     * Agent dies before it has a chance to report success/failure.  In that case, we'll never
     * get an Agent completion message and the update request will remain in progress status forever.
     * This method just tries to detect this scenario - if it finds an update request that has been
     * in progress for a very long time, we assume we'll never hear from the Agent and time out
     * that request (that is, set its status to FAILURE and set an error string that says the request
     * timed out).
     */
    void checkForTimedOutConfigurationUpdateRequests();

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public AggregatePluginConfigurationUpdate getAggregatePluginConfigurationUpdate(Subject subject,
        int configurationUpdateId) throws FetchException;

    public AggregateResourceConfigurationUpdate getAggregateResourceConfigurationUpdate(Subject subject,
        int configurationUpdateId) throws FetchException;

    public Configuration getConfiguration(Subject subject, int configurationId) throws FetchException;

    /**
     * Get the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     * resource's plugin configuration is not yet initialized.
     *
     * @param  subject     the user who wants to see the information
     * @param  resourceId a {@link Resource} id
     *
     * @return the current plugin configuration for the {@link Resource} with the given id, or <code>null</code> if the
     *         resource's configuration is not yet initialized
     * @throws FetchException TODO
     */
    @Nullable
    Configuration getPluginConfiguration(Subject subject, int resourceId) throws FetchException;

    /**
     * Get the latest plugin configuration for the {@link Resource} with the given id. Returns the configuration as it
     * is known on the server-side in the database.
     *
     * @param  subject     the user who wants to see the information
     * @param  resourceId a {@link Resource} id
     *
     * @return the current plugin configuration (along with additional information about the configuration) for the
     *         {@link Resource} with the given id
     * @throws FetchException TODO
     */
    PluginConfigurationUpdate getLatestPluginConfigurationUpdate(Subject subject, int resourceId) throws FetchException;

    /**
     * Get the latest resource configuration for the {@link Resource} with the given id, or <code>null</code> if the
     * resource's configuration is not yet initialized and for some reason we can't get its current live configuration
     * (e.g. in the case the agent or resource is down). Returns the configuration as it is known on the server-side in
     * the database. The database will be sync'ed with the live values, if the currently live configuration is actually
     * different than the latest configuration update found in history.
     *
     * @param  subject     the user who wants to see the information
     * @param  resourceId a {@link Resource} id
     *
     * @return the current configuration (along with additional information about the configuration) for the
     *         {@link Resource} with the given id, or <code>null</code> if the resource's configuration is not yet
     *         initialized and its live configuration could not be determined
     * @throws FetchException TODO
     */
    @Nullable
    ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId)
        throws FetchException;

    boolean isResourceConfigurationUpdateInProgress(Subject subject, int resourceId) throws FetchException;

    boolean isAggregateResourceConfigurationUpdateInProgress(Subject subject, int groupId) throws FetchException;

    int scheduleAggregateResourceConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> newResourceConfigurationMap) throws UpdateException;

    /**
     * Updates the plugin configuration used to connect and communicate with the resource. The given <code>
     * newConfiguration</code> is usually a modified version of a configuration returned by
     * {@link #getPluginConfiguration(Subject, int)}.
     *
     * @param  subject           the user who wants to see the information
     * @param  resourceId       a {@link Resource} id
     * @param  newConfiguration the new plugin configuration
     *
     * @return the plugin configuration update item corresponding to this request
     * @throws UpdateException TODO
     */
    PluginConfigurationUpdate updatePluginConfiguration(Subject subject, int resourceId, Configuration newConfiguration)
        throws UpdateException;

    /**
     * This method is called when a user has requested to change the resource configuration for an existing resource. If
     * the user does not have the proper permissions to change the resource's configuration, an exception is thrown.
     *
     * <p>This will not wait for the agent to finish the configuration update. This will return after the request is
     * sent. Once the agent finishes with the request, it will send the completed request information to
     * {@link #completeResourceConfigurationUpdate}.</p>
     *
     * @param  subject           the user who is requesting the update
     * @param  resourceId       identifies the resource to be updated
     * @param  newConfiguration the resource's desired new configuration
     *
     * @return the resource configuration update item corresponding to this request. null 
     * if newConfiguration is equal to the existing configuration.
     * @throws UpdateException TODO
     */
    @Nullable
    ResourceConfigurationUpdate updateResourceConfiguration(Subject subject, int resourceId,
        Configuration newConfiguration) throws UpdateException;

    Configuration getResourceConfiguration(Subject subject, int resourceId) throws FetchException;
}