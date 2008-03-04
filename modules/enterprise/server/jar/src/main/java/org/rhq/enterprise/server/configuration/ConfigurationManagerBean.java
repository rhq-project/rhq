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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.rhq.core.clientapi.agent.PluginPermissionException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.group.AggregatePluginConfigurationUpdate;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.configuration.job.AbstractAggregateConfigurationUpdateJob;
import org.rhq.enterprise.server.configuration.job.AggregatePluginConfigurationUpdateJob;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.QuartzUtil;

/**
 * The manager responsible for working with resource and plugin configurations.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Stateless
public class ConfigurationManagerBean implements ConfigurationManagerLocal {
    private final Log log = LogFactory.getLog(ConfigurationManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private ResourceManagerLocal resourceManager;
    @EJB
    private ConfigurationManagerLocal configurationManager; // yes, this is ourself
    @EJB
    private SchedulerLocal scheduler;
    @EJB
    private SubjectManagerLocal subjectManager;

    @Nullable
    public Configuration getCurrentPluginConfiguration(Subject whoami, int resourceId) {
        log.debug("Getting current plugin configuration for resource [" + resourceId + "]");

        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new IllegalStateException("Cannot retrieve plugin config for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(whoami, resource.getId())) {
            throw new PermissionException("User [" + whoami.getName()
                + "] does not have permission to view plugin configuration for [" + resource + "]");
        }

        Configuration pluginConfiguration = resource.getPluginConfiguration();
        pluginConfiguration.getId(); // Initialize the lazy proxy
        return pluginConfiguration;
    }

    public void completeAggregatePluginConfigurationUpdate(int aggregatePluginConfigurationUpdateId) {
        // then loop over the results and update the aggregate as appropriate
        AggregatePluginConfigurationUpdate groupUpdate = configurationManager
            .getAggregatePluginConfigurationById(aggregatePluginConfigurationUpdateId);

        for (PluginConfigurationUpdate singleUpdate : groupUpdate.getConfigurationUpdates()) {
            configurationManager.completePluginConfigurationUpdate(singleUpdate);
        }

        groupUpdate.setStatus(ConfigurationUpdateStatus.SUCCESS); // to be overridden if there were any failures
        for (PluginConfigurationUpdate childUpdate : groupUpdate.getConfigurationUpdates()) {
            if (childUpdate.getStatus() != ConfigurationUpdateStatus.SUCCESS) {
                groupUpdate.setStatus(ConfigurationUpdateStatus.FAILURE);
            }
        }
    }

    public void completePluginConfigurationUpdate(PluginConfigurationUpdate update) {
        // use EJB3 reference to ourself so that transaction semantics are correct
        ConfigurationUpdateResponse response = configurationManager.executePluginConfigurationUpdate(update);
        Resource resource = update.getResource();

        // link to the newer, persisted configuration object -- regardless of errors
        resource.setPluginConfiguration(update.getConfiguration());

        if (response.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
            update.setStatus(ConfigurationUpdateStatus.SUCCESS);

            resource.setConnected(true);

            removeAnyExistingInvalidPluginConfigurationErrors(subjectManager.getOverlord(), resource);
        } else {
            handlePluginConfiguratonUpdateRemoteException(resource, response.getStatus().toString(), response
                .getErrorMessage());

            update.setStatus(response.getStatus());
            update.setErrorMessage(response.getErrorMessage());
        }
    }

    // use requires new so that exceptions bubbling up from the agent.updatePluginConfiguration don't force callers to rollback as well
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ConfigurationUpdateResponse executePluginConfigurationUpdate(PluginConfigurationUpdate update) {
        Resource resource = update.getResource();
        Configuration configuration = update.getConfiguration();

        ConfigurationUpdateResponse response = null;

        try {
            // now let's tell the agent to actually update the resource component's plugin configuration
            AgentClient agentClient = this.agentManager.getAgentClient(resource.getAgent());

            agentClient.getDiscoveryAgentService().updatePluginConfiguration(resource.getId(), configuration);
            try {
                agentClient.getDiscoveryAgentService().executeServiceScanImmediately();
            } catch (Exception e) {
                log.warn("Failed to execute service scan - cannot detect children of the newly connected resource ["
                    + resource + "]", e);
            }

            response = new ConfigurationUpdateResponse(update.getId(), null, ConfigurationUpdateStatus.SUCCESS, null);
        } catch (Exception e) {
            response = new ConfigurationUpdateResponse(update.getId(), null, e);
        }

        return response;
    }

    public PluginConfigurationUpdate updatePluginConfiguration(Subject whoami, int resourceId,
        Configuration configuration) {
        Resource resource = this.entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new IllegalStateException("Cannot update plugin config for unknown resource [" + resourceId + "]");
        }

        // make sure the user has the proper permissions to do this
        ensureModifyPermission(whoami, resource);

        // create our new update request and assign it to our resource - its status will initially be "in progress"
        PluginConfigurationUpdate update = new PluginConfigurationUpdate(resource, configuration, whoami.getName());

        update.setStatus(ConfigurationUpdateStatus.SUCCESS);
        entityManager.persist(update);

        resource.addPluginConfigurationUpdates(update);

        // agent field is LAZY - force it to load because the caller will need it.
        Agent agent = resource.getAgent();
        if (agent != null) {
            agent.getName();
        }

        configurationManager.completePluginConfigurationUpdate(update);

        return update;
    }

    public Configuration getActiveResourceConfiguration(int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        Configuration resourceConfiguration = resource.getResourceConfiguration();
        if (resourceConfiguration != null) {
            resourceConfiguration.getProperties().size(); // cause it's lazy
        }
        return resourceConfiguration;
    }

    @Nullable
    public ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject whoami, int resourceId) {
        log.debug("Getting current resource configuration for resource [" + resourceId + "]");

        Resource resource;
        ResourceConfigurationUpdate current;

        // Get the latest configuration as known to the server (i.e. persisted in the DB).
        try {
            Query query = entityManager
                .createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG);
            query.setParameter("resourceId", resourceId);
            current = (ResourceConfigurationUpdate) query.getSingleResult();
            resource = current.getResource();
        } catch (NoResultException nre) {
            current = null; // The resource hasn't been successfully configured yet.

            // We still need the resource, so we can get its agent.
            resource = entityManager.find(Resource.class, resourceId);
            if (resource == null) {
                throw new NoResultException("Cannot get latest resource configuration for unknown resource ["
                    + resourceId + "]");
            }
        }

        if (!authorizationManager.canViewResource(whoami, resource.getId())) {
            throw new PermissionException("User [" + whoami.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        // Check whether or not a resource configuration update is currently in progress.
        ResourceConfigurationUpdate latest;
        try {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            latest = (ResourceConfigurationUpdate) query.getSingleResult();
            if (latest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) {
                // The agent is in the process of a config update, so we do not want to ask it for the live config.
                // Instead, simply return the most recent persisted config w/ a SUCCESS status (possibly null).
                return current;
            }
        } catch (NoResultException nre) {
            // The resource hasn't been successfully configured yet - not a problem, we'll ask the agent for the live
            // config...
        }

        // ask the agent to get us the live, most up-to-date configuration for the resource
        // then compare it to make sure what we think is the latest configuration is really the latest
        Configuration liveConfig = getLiveResourceConfiguration(resource);

        if (liveConfig != null) {
            // Compare the live values and, if there is a difference with the current, store the live config as a new
            // update. Note that, if there is no current configuration stored, the live config is stored as the first
            // update.
            boolean theSame;

            if (current == null) {
                theSame = false;
            } else {
                theSame = current.getConfiguration().equals(liveConfig);
            }

            // Someone dorked with the configuration on the agent side - save the live config as a new update.
            if (!theSame) {
                try {
                    /*
                     * Note that we pass null as the subject - we don't know who changed it!
                     *
                     * also note that we're passing the overlord, since this is a system side-effect.  here, the system
                     * and *not* the user, is choosing to persist the most recent configuration because it was different
                     * from the last known value.  again, the user isn't attempting to change the value; instead, *JON*
                     * is triggering save based on the semantics that we want to provide for configuration updates.
                     */
                    Subject overlord = subjectManager.getOverlord();
                    current = configurationManager.persistNewResourceConfigurationUpdateHistory(overlord, resourceId,
                        liveConfig, ConfigurationUpdateStatus.SUCCESS, null);
                } catch (UpdateStillInProgressException e) {
                    // This means a config update is INPROGRESS.
                    // Return the current in this case since it is our latest committed active config.
                    // Note that even though this application exception specifies "rollback=true", it will
                    // not effect our current transaction since the persist call was made with REQUIRES_NEW
                    // and thus only that new tx was rolled back
                    log.debug("Resource is currently in progress of changing its resource configuration - "
                        + "since it hasn't finished yet, will use the last successful resource configuration: " + e);
                }
            }
        } else {
            log.warn("Could not get live resource configuration for resource [" + resource
                + "]; will assume latest resource configuration update is the current resource configuration.");
        }

        return current;
    }

    public PluginConfigurationUpdate getLatestPluginConfigurationUpdate(Subject whoami, int resourceId) {
        log.debug("Getting current plugin configuration for resource [" + resourceId + "]");

        Resource resource;
        PluginConfigurationUpdate current;

        // Get the latest configuration as known to the server (i.e. persisted in the DB).
        try {
            Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG);
            query.setParameter("resourceId", resourceId);
            current = (PluginConfigurationUpdate) query.getSingleResult();
            resource = current.getResource();
        } catch (NoResultException nre) {
            current = null; // The resource hasn't been successfully configured yet.

            // We still need the resource, so we can get its agent.
            resource = entityManager.find(Resource.class, resourceId);
            if (resource == null) {
                throw new NoResultException("Cannot get latest plugin configuration for unknown resource ["
                    + resourceId + "]");
            }
        }

        if (!authorizationManager.canViewResource(whoami, resource.getId())) {
            throw new PermissionException("User [" + whoami.getName()
                + "] does not have permission to view plugin configuration for [" + resource + "]");
        }

        return current;
    }

    public boolean isResourceConfigurationUpdateInProgress(Subject whoami, int resourceId) {
        boolean updateInProgress;
        ResourceConfigurationUpdate latestConfigUpdate;
        try {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            latestConfigUpdate = (ResourceConfigurationUpdate) query.getSingleResult();
            if (!authorizationManager.canViewResource(whoami, latestConfigUpdate.getResource().getId())) {
                throw new PermissionException("User [" + whoami.getName()
                    + "] does not have permission to view resource configuration for ["
                    + latestConfigUpdate.getResource() + "]");
            }

            updateInProgress = (latestConfigUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }

        return updateInProgress;
    }

    public Configuration getLiveResourceConfiguration(Subject whoami, int resourceId) throws Exception {
        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(whoami, resource.getId())) {
            throw new PermissionException("User [" + whoami.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        // ask the agent to get us the live, most up-to-date configuration for the resource
        Configuration liveConfig = getLiveResourceConfiguration(resource);

        return liveConfig;
    }

    @SuppressWarnings("unchecked")
    public void checkForTimedOutUpdateRequests() {
        log.debug("Scanning configuration update requests to see if any in-progress executions have timed out");

        // the purpose of this method is really to clean up requests when we detect
        // they probably will never move out of the in progress status.  This will occur if the
        // agent dies before it has a chance to report success/failure.  In that case, we'll never
        // get an agent completion message and the update request will remain in progress status forever.
        // This method just tries to detect this scenario - if it finds an update request that has been
        // in progress for a very long time, we assume we'll never hear from the agent and time out
        // that request (that is, set its status to FAILURE and set an error string).
        try {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS);
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<ResourceConfigurationUpdate> requests = query.getResultList();
            for (ResourceConfigurationUpdate request : requests) {
                long timeout = 1000 * 60 * 60; // TODO [mazz]: should we make this configurable?

                if (request.getDuration() > timeout) {
                    request.setErrorMessage("Timed out : did not complete after " + request.getDuration() + "ms"
                        + " (the timeout period was [" + timeout + "] ms)");
                    request.setStatus(ConfigurationUpdateStatus.FAILURE);

                    log.debug("Configuration update request seems to have been orphaned - timing it out: " + request);
                }
            }
        } catch (Throwable t) {
            log.warn("Failed to check for timed out configuration update requests. Cause: " + t);
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceConfigurationUpdate> getResourceConfigurationUpdates(Subject whoami, int resourceId,
        PageControl pc) {
        pc.initDefaultOrderingField("cu.id", PageOrdering.DESC);

        String queryName = ResourceConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ResourceConfigurationUpdate> updates = query.getResultList();

        if ((updates == null) || (updates.size() == 0)) {
            // there is no configuration yet - get the latest from the agent, if possible
            updates = new ArrayList<ResourceConfigurationUpdate>();
            ResourceConfigurationUpdate latest = getLatestResourceConfigurationUpdate(whoami, resourceId);
            if (latest != null) {
                updates.add(latest);
            }
        } else if (updates.size() > 0) {
            Resource resource = updates.get(0).getResource();
            if (!authorizationManager.canViewResource(whoami, resource.getId())) {
                throw new PermissionException("User [" + whoami.getName()
                    + "] does not have permission to view resource [" + resource + "]");
            }
        }

        return new PageList<ResourceConfigurationUpdate>(updates, (int) totalCount, pc);
    }

    public ResourceConfigurationUpdate getResourceConfigurationUpdate(Subject whoami, int configurationUpdateId) {
        ResourceConfigurationUpdate update = entityManager.find(ResourceConfigurationUpdate.class,
            configurationUpdateId);

        if (!authorizationManager.canViewResource(whoami, update.getResource().getId())) {
            throw new PermissionException("User [" + whoami.getName()
                + "] does not have permission to view resource configuration update for [" + update.getResource() + "]");
        }

        update.getConfiguration(); // this is EAGER loaded, so this really doesn't do anything

        return update;
    }

    public void purgeResourceConfigurationUpdate(Subject whoami, int configurationUpdateId, boolean purgeInProgress) {
        ResourceConfigurationUpdate doomedRequest = entityManager.find(ResourceConfigurationUpdate.class,
            configurationUpdateId);

        if (doomedRequest == null) {
            log.debug("Asked to purge a non-existing config update request [" + configurationUpdateId + "]");
            return;
        }

        if ((doomedRequest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) && !purgeInProgress) {
            throw new IllegalStateException(
                "The update request is still in the in-progress state. Please wait for it to complete: "
                    + doomedRequest);
        }

        // make sure the user has the proper permissions to do this
        Resource resource = doomedRequest.getResource();
        if (!authorizationManager.hasResourcePermission(whoami, Permission.CONFIGURE, resource.getId())) {
            throw new PermissionException("User [" + whoami.getName()
                + "] does not have permission to purge a configuration update audit trail for resource [" + resource
                + "]");
        }

        resource.getResourceConfigurationUpdates().remove(doomedRequest);
        entityManager.remove(doomedRequest);
        entityManager.flush();

        return;
    }

    public void purgeResourceConfigurationUpdates(Subject whoami, int[] configurationUpdateIds, boolean purgeInProgress) {
        if ((configurationUpdateIds == null) || (configurationUpdateIds.length == 0)) {
            return;
        }

        // TODO [mazz]: ugly - let's make this more efficient, just getting this to work first
        for (int configurationUpdateId : configurationUpdateIds) {
            purgeResourceConfigurationUpdate(whoami, configurationUpdateId, purgeInProgress);
        }

        return;
    }

    public ResourceConfigurationUpdate updateResourceConfiguration(Subject whoami, int resourceId,
        Configuration newConfiguration) {
        // must do this in a separate transaction so it is committed prior to sending the agent request
        // (consider synchronizing to avoid the condition where someone calls this method twice quickly
        // in two different txs which would put two updates in INPROGRESS and cause havoc)
        ResourceConfigurationUpdate newUpdate;

        // here we call ourself, but we do so via the EJB interface so we pick up the REQUIRES_NEW semantics
        newUpdate = configurationManager.persistNewResourceConfigurationUpdateHistory(whoami, resourceId,
            newConfiguration, ConfigurationUpdateStatus.INPROGRESS, whoami.getName());

        // now let's tell the agent to actually update the managed resource's configuration
        try {
            AgentClient agentClient = agentManager.getAgentClient(newUpdate.getResource().getAgent());
            ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(newUpdate.getId(), newUpdate
                .getConfiguration(), newUpdate.getResource().getId());
            agentClient.getConfigurationAgentService().updateResourceConfiguration(request);
        } catch (RuntimeException e) {
            // Any exception means the remote call itself failed - make sure to change the status on the update to FAILURE
            // and set its error message field.
            newUpdate.setStatus(ConfigurationUpdateStatus.FAILURE);
            newUpdate.setErrorMessageFromThrowable(e);

            // here we call ourself, but we do so via the EJB interface so we pick up the REQUIRES_NEW semantics
            this.configurationManager.mergeConfigurationUpdate(newUpdate);
        }

        return newUpdate;
    }

    public void rollbackResourceConfiguration(Subject whoami, int resourceId, int configHistoryId)
        throws ConfigurationUpdateException {
        ResourceConfigurationUpdate configurationUpdateHistory = entityManager.find(ResourceConfigurationUpdate.class,
            configHistoryId);
        Configuration configuration = configurationUpdateHistory.getConfiguration();
        if (configuration == null) {
            throw new ConfigurationUpdateException("No configuration history element exists with id = '"
                + configHistoryId + "'");
        }

        updateResourceConfiguration(whoami, resourceId, configuration);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResourceConfigurationUpdate persistNewResourceConfigurationUpdateHistory(Subject whoami, int resourceId,
        Configuration newConfiguration, ConfigurationUpdateStatus newStatus, String newSubject)
        throws UpdateStillInProgressException {
        // get the resource that we will be updating
        Resource resource = entityManager.find(Resource.class, resourceId);

        // make sure the user has the proper permissions to do this
        if (!authorizationManager.hasResourcePermission(whoami, Permission.CONFIGURE, resource.getId())) {
            throw new PermissionException("User [" + whoami.getName()
                + "] does not have permission to modify configuration for resource [" + resource + "]");
        }

        // see if there was a previous update request and make sure it isn't still in progress
        List<ResourceConfigurationUpdate> previousRequests = resource.getResourceConfigurationUpdates();

        if (previousRequests != null) {
            for (ResourceConfigurationUpdate previousRequest : previousRequests) {
                if (previousRequest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) {
                    // if you change this to another exception, make sure you change getLatestResourceConfigurationUpdate
                    throw new UpdateStillInProgressException(
                        "Resource ["
                            + resource
                            + "] has a resource configuration update request already in progress - please wait for it to finish: "
                            + previousRequest);
                }
            }
        }

        // let's make sure all IDs are zero - we want to persist a brand new copy
        Configuration zeroedConfiguration = newConfiguration.deepCopy(false);

        // create our new update request and assign it to our resource - its status will initially be "in progress"
        ResourceConfigurationUpdate newUpdateRequest = new ResourceConfigurationUpdate(resource, zeroedConfiguration,
            newSubject);

        newUpdateRequest.setStatus(newStatus);

        entityManager.persist(newUpdateRequest);
        resource.addResourceConfigurationUpdates(newUpdateRequest);

        resource.getChildResources().size();

        // agent field is LAZY - force it to load because the caller will need it.
        Agent agent = resource.getAgent();
        if (agent != null) {
            agent.getName();
        }

        return newUpdateRequest;
    }

    public void completeResourceConfigurationUpdate(ConfigurationUpdateResponse response) {
        log.debug("Received a configuration-update-completed message: " + response);

        ResourceConfigurationUpdate persistedRequest;

        // find the current update request that is persisted - this is the one that is being reported as being complete
        persistedRequest = entityManager.find(ResourceConfigurationUpdate.class, response.getConfigurationUpdateId());
        if (persistedRequest == null) {
            throw new IllegalStateException(
                "The completed request passed in does not match any request for any resource in inventory: " + response);
        }

        if (response.getStatus() == ConfigurationUpdateStatus.FAILURE) {
            // TODO [mazz]: what happens if the plugin dorked with the configuration ID? need to assert is hasn't changed
            if (response.getConfiguration() != null) {
                Configuration failedConfiguration = response.getConfiguration();
                failedConfiguration = entityManager.merge(failedConfiguration); // merge in any property error messages
                persistedRequest.setConfiguration(failedConfiguration);
            }
        } else if (response.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
            // link to the newer, persisted configuration object
            Resource resource = persistedRequest.getResource();
            resource.setResourceConfiguration(persistedRequest.getConfiguration().deepCopy(false));
        }

        // make sure we update the persisted request with the new status and any error message
        persistedRequest.setStatus(response.getStatus());
        persistedRequest.setErrorMessage(response.getErrorMessage());

        return;
    }

    @Nullable
    public ConfigurationDefinition getResourceConfigurationDefinitionForResourceType(Subject whoami, int resourceTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_RESOURCE_BY_RESOURCE_TYPE_ID);
        query.setParameter("resourceTypeId", resourceTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            if (resourceType == null) {
                throw new EntityNotFoundException("A resource type with id " + resourceTypeId + " does not exist.");
            }
        }

        return configurationDefinition;
    }

    @Nullable
    public ConfigurationDefinition getResourceConfigurationDefinitionWithTemplatesForResourceType(Subject whoami,
        int resourceTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_RESOURCE_BY_RESOURCE_TYPE_ID);
        query.setParameter("resourceTypeId", resourceTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            if (resourceType == null) {
                throw new EntityNotFoundException("A resource type with id " + resourceTypeId + " does not exist.");
            }
        }

        // Eager Load the templates
        if ((configurationDefinition != null) && (configurationDefinition.getTemplates() != null)) {
            configurationDefinition.getTemplates().size();
        }

        return configurationDefinition;
    }

    @Nullable
    public ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(Subject whoami, int resourceTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_PLUGIN_BY_RESOURCE_TYPE_ID);
        query.setParameter("resourceTypeId", resourceTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            if (resourceType == null) {
                throw new EntityNotFoundException("A resource type with id " + resourceTypeId + " does not exist.");
            }
        }

        // Eager Load the templates
        if ((configurationDefinition != null) && (configurationDefinition.getTemplates() != null)) {
            configurationDefinition.getTemplates().size();
        }

        return configurationDefinition;
    }

    /**
     * Given an actual resource, this asks the agent to return that resource's live configuration. Note that this does
     * not perform any authorization checks - it is assumed the caller has permissions to view the configuration. This
     * also assumes <code>resource</code> is a non-<code>null</code> and existing resource.
     *
     * <p>If failed to contact the agent or any other communications problem occurred, <code>null</code> will be
     * returned.</p>
     *
     * @param  resource an existing resource whose live configuration is to be retrieved
     *
     * @return the resource's live configuration or <code>null</code> if it could not be retrieved from the agent
     */
    private Configuration getLiveResourceConfiguration(Resource resource) {
        Configuration liveConfig = null;

        try {
            Agent agent = resource.getAgent();
            AgentClient agentClient = this.agentManager.getAgentClient(agent);

            // Getting live configuration is mostly for the UI's benefit - as such, do not hang
            // for a long time in the event the agent is down or can't be reached.  Let's make the UI
            // responsive even in the case of an agent down by pinging it quickly to verify the agent is up.
            boolean agentPinged = agentClient.ping(5000L);

            if (agentPinged) {
                liveConfig = agentClient.getConfigurationAgentService().loadResourceConfiguration(resource.getId());
                if (liveConfig == null) {
                    // This should really never occur - the PC should never return a null, always at least an empty config.
                    log.debug("ConfigurationAgentService.loadResourceConfiguration() returned a null Configuration.");
                    liveConfig = new Configuration();
                }
            } else {
                log.warn("Agent is unreachable [" + agent + "] - cannot get live configuration for resource ["
                    + resource + "]");
            }
        } catch (Exception e) {
            if (e instanceof PluginPermissionException) {
                throw new PermissionException(e.getMessage());
            }

            log.warn("Could not get live configuration for resource [" + resource + "]"
                + ThrowableUtil.getAllMessages(e, true));
        }

        return liveConfig;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AbstractResourceConfigurationUpdate mergeConfigurationUpdate(
        AbstractResourceConfigurationUpdate configurationUpdate) {
        return this.entityManager.merge(configurationUpdate);
    }

    public Configuration getConfigurationById(int id) {
        return entityManager.find(Configuration.class, id);
    }

    public Configuration getConfigurationFromDefaultTemplate(ConfigurationDefinition definition) {
        ConfigurationDefinition managedDefinition = entityManager.find(ConfigurationDefinition.class, definition
            .getId());
        return managedDefinition.getDefaultTemplate().getConfiguration();
    }

    private void handlePluginConfiguratonUpdateRemoteException(Resource resource, String summary, String detail) {
        resource.setConnected(false);
        ResourceError invalidPluginConfigError = new ResourceError(resource,
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, summary, detail, Calendar.getInstance().getTimeInMillis());
        this.resourceManager.addResourceError(invalidPluginConfigError);
    }

    private void removeAnyExistingInvalidPluginConfigurationErrors(Subject whoami, Resource resource) {
        List<ResourceError> invalidPluginConfigErrors = this.resourceManager.getResourceErrors(whoami,
            resource.getId(), ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
        if (invalidPluginConfigErrors.size() > 1) {
            log.error(resource + " has more than one INVALID_PLUGIN_CONFIGURATION ResourceError associated with it.");
        }

        for (ResourceError invalidPluginConfigError : invalidPluginConfigErrors) {
            this.resourceManager.deleteResourceError(whoami, invalidPluginConfigError.getId());
        }
    }

    public int scheduleAggregatePluginConfigurationUpdate(Subject whoami, int compatibleGroupId,
        Configuration pluginConfigurationUpdate) throws SchedulerException {
        ResourceGroup group = getCompatibleGroupIfAuthorized(whoami, compatibleGroupId);

        ensureModifyPermission(whoami, group);
        if (pluginConfigurationUpdate == null) {
            throw new IllegalArgumentException(
                "AggregatePluginConfigurationUpdate must have non-null pluginConfigurationUpdate");
        }

        AggregatePluginConfigurationUpdate update = new AggregatePluginConfigurationUpdate(group,
            pluginConfigurationUpdate, whoami.getName());
        for (Resource groupMember : group.getImplicitResources()) {
            /*
             * addConfigurationUpdate does all the magic of creating a new plugin configuration from the to-update
             * elements of the aggregate
             */
            update.addConfigurationUpdate(groupMember);
        }

        entityManager.persist(update);
        for (PluginConfigurationUpdate childUpdate : update.getConfigurationUpdates()) {
            entityManager.persist(childUpdate);
        }

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAsString(AbstractAggregateConfigurationUpdateJob.DATAMAP_INT_CONFIG_GROUP_UPDATE_ID, update
            .getId());
        jobDataMap.putAsString(AbstractAggregateConfigurationUpdateJob.DATAMAP_INT_SUBJECT_ID, whoami.getId());

        // acquire quartz objects and schedule the aggregate update
        JobDetail jobDetail = AggregatePluginConfigurationUpdateJob.getJobDetail(group, whoami, jobDataMap);
        Trigger trigger = QuartzUtil.getFireOnceImmediateTrigger(jobDetail);
        scheduler.scheduleJob(jobDetail, trigger);

        log.debug("Scheduled " + update + " for immediate execution");

        return update.getId();
    }

    private ResourceGroup getCompatibleGroupIfAuthorized(Subject whoami, int compatibleGroupId) {
        ResourceGroup group;

        try {
            // resourceGroupManager will test for necessary permissions too
            group = resourceGroupManager.getResourceGroupById(whoami, compatibleGroupId, GroupCategory.COMPATIBLE);
        } catch (ResourceGroupNotFoundException e) {
            throw new RuntimeException("Cannot get support operations for unknown group [" + compatibleGroupId + "]: "
                + e, e);
        }

        return group;
    }

    private void ensureModifyPermission(Subject whoami, Resource resource) throws PermissionException {
        if (!authorizationManager.hasResourcePermission(whoami, Permission.MODIFY_RESOURCE, resource.getId())) {
            throw new PermissionException("User [" + whoami.getName() + "] does not have permission "
                + "to modify plugin configuration for resource [" + resource + "]");
        }
    }

    private void ensureModifyPermission(Subject whoami, ResourceGroup group) throws PermissionException {
        if (!authorizationManager.hasGroupPermission(whoami, Permission.MODIFY_RESOURCE, group.getId())) {
            throw new PermissionException("User [" + whoami.getName() + "] does not have permission "
                + "to modify plugin configuration for members of group [" + group + "]");
        }
    }

    public AggregatePluginConfigurationUpdate getAggregatePluginConfigurationById(int configurationUpdateId) {
        AggregatePluginConfigurationUpdate update = entityManager.find(AggregatePluginConfigurationUpdate.class,
            configurationUpdateId);
        if (update == null) {
            return null;
        }

        // initialize all data that will be needed by AggregatePluginConfigurationUpdateJob caller
        for (PluginConfigurationUpdate childUpdate : update.getConfigurationUpdates()) {
            childUpdate.getResource().getAgent().getName();
        }

        return update;
    }

    @SuppressWarnings("unchecked")
    public PageList<PluginConfigurationUpdate> getPluginConfigurationUpdatesByParentId(int configurationUpdateId,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, pageControl);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID);
        query.setParameter("aggregateConfigurationUpdateId", configurationUpdateId);
        countQuery.setParameter("aggregateConfigurationUpdateId", configurationUpdateId);

        long count = (Long) countQuery.getSingleResult();

        List<PluginConfigurationUpdate> results = query.getResultList();

        return new PageList<PluginConfigurationUpdate>(results, (int) count, pageControl);
    }

    public List<Configuration> getPluginConfigurationsForCompatibleGroup(ResourceGroup group) {
        ResourceGroup compatibleGroup = entityManager.find(ResourceGroup.class, group.getId());
        Set<Resource> groupMembers = compatibleGroup.getImplicitResources();

        List<Configuration> pluginConfigurations = new ArrayList<Configuration>(groupMembers.size());

        for (Resource member : groupMembers) {
            Configuration update = member.getPluginConfiguration();
            update.getId(); // initialize
            pluginConfigurations.add(update);
        }

        return pluginConfigurations;
    }

    @SuppressWarnings("unchecked")
    public PageList<AggregatePluginConfigurationUpdate> getAggregatePluginConfigurationUpdatesByGroupId(int groupId,
        PageControl pc) {
        pc.initDefaultOrderingField("modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            AggregatePluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            AggregatePluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID);
        query.setParameter("groupId", groupId);
        countQuery.setParameter("groupId", groupId);

        long count = (Long) countQuery.getSingleResult();

        List<AggregatePluginConfigurationUpdate> results = null;
        results = query.getResultList();

        return new PageList<AggregatePluginConfigurationUpdate>(results, (int) count, pc);
    }

    public int deleteAggregatePluginConfigurationUpdates(Subject subject, Integer resourceGroupId,
        Integer[] aggregatePluginConfigurationUpdateIds) {
        //TODO: use subject and resourceGroupId to perform security check
        int removed = 0;
        for (Integer id : aggregatePluginConfigurationUpdateIds) {
            /*
             * use this strategy instead of AggregatePluginConfigurationUpdate.QUERY_DELETE_BY_ID because removing via
             * the entityManager will respect cascading rules, using a JPQL DELETE statement will not
             */
            try {
                AggregatePluginConfigurationUpdate update = getAggregatePluginConfigurationById(id);
                entityManager.remove(update);
                removed++;
            } catch (Exception e) {
            }
        }

        return removed;
    }

    public AggregatePluginConfigurationUpdate updateAggregatePluginConfigurationUpdate(
        AggregatePluginConfigurationUpdate groupUpdate) {
        return entityManager.merge(groupUpdate);
    }
}