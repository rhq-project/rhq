/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.configuration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.Nullable;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyOptionsSource;
import org.rhq.core.domain.configuration.group.AbstractGroupConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.criteria.AbstractConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.GroupPluginConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.ResourceUtility;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.auth.prefs.SubjectPreferences;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.configuration.job.AbstractGroupConfigurationUpdateJob;
import org.rhq.enterprise.server.configuration.job.GroupPluginConfigurationUpdateJob;
import org.rhq.enterprise.server.configuration.job.GroupResourceConfigurationUpdateJob;
import org.rhq.enterprise.server.configuration.util.ConfigurationMaskingUtility;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.QuartzUtil;

/**
 * The manager responsible for working with Resource and plugin configurations.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Stateless
public class ConfigurationManagerBean implements ConfigurationManagerLocal, ConfigurationManagerRemote {
    private static final Log LOG = LogFactory.getLog(ConfigurationManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
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

    @Override
    @Nullable
    public Configuration getPluginConfiguration(Subject subject, int resourceId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting current plugin configuration for resource [" + resourceId + "]");
        }
        
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new IllegalStateException("Cannot retrieve plugin config for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view plugin configuration for [" + resource + "]");
        }

        Configuration pluginConfiguration = configurationManager.getPluginConfiguration(resourceId);

        return pluginConfiguration;
    }

    // Use new transaction because this only works if the resource in question has not
    // yet been loaded by Hibernate.  We want the query to return a non-proxied configuration,
    // this is critical for remote API use.
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Configuration getPluginConfiguration(int resourceId) {
        // Ensure that we return a non-proxied Configuration object that can survive after the
        // Hibernate session goes away.
        Query query = entityManager.createNamedQuery(Configuration.QUERY_GET_PLUGIN_CONFIG_BY_RESOURCE_ID);
        query.setParameter("resourceId", resourceId);
        Configuration pluginConfiguration = (Configuration) query.getSingleResult();

        // Mask the configuration before returning it.
        Resource resource = resourceManager.getResourceById(subjectManager.getOverlord(), resourceId);
        ConfigurationDefinition pluginConfigurationDefinition = getPluginConfigurationDefinitionForResourceType(
            subjectManager.getOverlord(), resource.getResourceType().getId());
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        pluginConfiguration.getMap().size();
        entityManager.clear();
        ConfigurationMaskingUtility.maskConfiguration(pluginConfiguration, pluginConfigurationDefinition);

        return pluginConfiguration;
    }

    @Override
    public void completePluginConfigurationUpdate(Integer updateId) {
        PluginConfigurationUpdate update = entityManager.find(PluginConfigurationUpdate.class, updateId);
        configurationManager.completePluginConfigurationUpdate(update);
    }

    /*
     * this method will not fire off the update asynchronously (like the completeResourceConfigurationUpdate method
     * does); instead, it will block until an update response is retrieved from the agent-side resource
     */
    @Override
    public void completePluginConfigurationUpdate(PluginConfigurationUpdate update) {
        // use EJB3 reference to ourself so that transaction semantics are correct
        ConfigurationUpdateResponse response = configurationManager.executePluginConfigurationUpdate(update);
        Resource resource = update.getResource();

        // link to the newer, persisted configuration object -- regardless of errors
        resource.setAgentSynchronizationNeeded();
        resource.setPluginConfiguration(update.getConfiguration());

        if (response.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
            update.setStatus(ConfigurationUpdateStatus.SUCCESS);

            resource.setConnected(true);

            removeAnyExistingInvalidPluginConfigurationErrors(resource);
            // Flush before merging to ensure the update has been persisted and avoid StaleStateExceptions.
            entityManager.flush();
            entityManager.merge(update);

        } else {
            handlePluginConfiguratonUpdateRemoteException(resource, response.getStatus().toString(),
                response.getErrorMessage());

            update.setStatus(response.getStatus());
            update.setErrorMessage(response.getErrorMessage());
        }
    }

    // use requires new so that exceptions bubbling up from the agent.updatePluginConfiguration don't force callers to rollback as well
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ConfigurationUpdateResponse executePluginConfigurationUpdate(PluginConfigurationUpdate update) {
        Resource resource = update.getResource();
        Configuration configuration = update.getConfiguration();
        configuration = configuration.deepCopy(false);

        ConfigurationUpdateResponse response = null;

        try {
            // now let's tell the agent to actually update the resource component's plugin configuration
            AgentClient agentClient = this.agentManager.getAgentClient(resource.getAgent());

            agentClient.getDiscoveryAgentService().updatePluginConfiguration(resource.getId(), configuration);
            try {
                agentClient.getDiscoveryAgentService().executeServiceScanDeferred();
            } catch (Exception e) {
                LOG.warn("Failed to execute service scan - cannot detect children of the newly connected resource ["
                        + resource + "]", e);
            }

            response = new ConfigurationUpdateResponse(update.getId(), null, ConfigurationUpdateStatus.SUCCESS, null);
        } catch (Exception e) {
            response = new ConfigurationUpdateResponse(update.getId(), null, e);
        }

        return response;
    }

    @Override
    public PluginConfigurationUpdate updatePluginConfiguration(Subject subject, int resourceId,
        Configuration newPluginConfiguration) throws ResourceNotFoundException {

        Subject overlord = subjectManager.getOverlord();
        Resource resource = resourceManager.getResourceById(overlord, resourceId);

        // make sure the user has the proper permissions to do this
        ensureModifyPermission(subject, resource);

        // Make sure to unmask the configuration before persisting the update.
        Configuration existingPluginConfiguration = resource.getPluginConfiguration();
        ConfigurationMaskingUtility.unmaskConfiguration(newPluginConfiguration, existingPluginConfiguration);

        // create our new update request and assign it to our resource - its status will initially be "in progress"
        PluginConfigurationUpdate update = new PluginConfigurationUpdate(resource, newPluginConfiguration,
            subject.getName());

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

    @Override
    public PluginConfigurationUpdate upgradePluginConfiguration(Subject subject, int resourceId,
        Configuration newPluginConfiguration) throws ResourceNotFoundException {

        Subject overlord = subjectManager.getOverlord();
        Resource resource = resourceManager.getResourceById(overlord, resourceId);

        // make sure the user has the proper permissions to do this
        ensureModifyPermission(subject, resource);

        // Make sure to unmask the configuration before persisting the update.
        Configuration existingPluginConfiguration = resource.getPluginConfiguration();
        ConfigurationMaskingUtility.unmaskConfiguration(newPluginConfiguration, existingPluginConfiguration);

        // create our new update request and assign it to our resource - its status will initially be "in progress"
        PluginConfigurationUpdate update = new PluginConfigurationUpdate(resource, newPluginConfiguration,
            subject.getName());

        update.setStatus(ConfigurationUpdateStatus.SUCCESS);
        entityManager.persist(update);

        resource.addPluginConfigurationUpdates(update);
        resource.setPluginConfiguration(update.getConfiguration());

        entityManager.merge(update);

        return update;
    }

    @Override
    public Configuration getResourceConfiguration(Subject subject, int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_READ, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        Configuration result = configurationManager.getResourceConfiguration(resourceId);

        return result;
    }

    // local only
    @Override
    public void setResourceConfiguration(int resourceId, Configuration configuration) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException("Resource [" + resourceId + "] does not exist.");
        }
        resource.setResourceConfiguration(configuration);
        entityManager.merge(resource);
    }

    // Use new transaction because this only works if the resource in question has not
    // yet been loaded by Hibernate.  We want the query to return a non-proxied configuration,
    // this is critical for remote API use.
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Configuration getResourceConfiguration(int resourceId) {
        // Ensure that we return a non-proxied Configuration object that can survive after the
        // Hibernate session goes away.
        Query query = entityManager.createNamedQuery(Configuration.QUERY_GET_RESOURCE_CONFIG_BY_RESOURCE_ID);
        query.setParameter("resourceId", resourceId);

        Configuration resourceConfiguration = (Configuration) query.getSingleResult();

        // Mask the configuration before returning it.
        Resource resource = resourceManager.getResourceById(subjectManager.getOverlord(), resourceId);
        ConfigurationDefinition resourceConfigurationDefinition = getResourceConfigurationDefinitionForResourceType(
            subjectManager.getOverlord(), resource.getResourceType().getId());
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        resourceConfiguration.getMap().size();
        entityManager.clear();
        ConfigurationMaskingUtility.maskConfiguration(resourceConfiguration, resourceConfigurationDefinition);

        return resourceConfiguration;
    }

    @Override
    public ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId,
        boolean fromStructured) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting current Resource configuration for Resource [" + resourceId + "]...");
        }

        Resource resource = entityManager.getReference(Resource.class, resourceId);
        if (resource == null) {
            throw new NoResultException("Cannot get latest resource configuration for unknown Resource [" + resourceId
                + "].");
        }

        if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_READ, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view Resource configuration for [" + resource + "].");
        }

        ResourceConfigurationUpdate current;
        // Get the latest configuration as known to the server (i.e. persisted in the DB).
        try {
            Query query = entityManager
                .createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG);
            query.setParameter("resourceId", resourceId);
            current = (ResourceConfigurationUpdate) query.getSingleResult();
            resource = current.getResource();
        } catch (NoResultException nre) {
            // The Resource hasn't been successfully configured yet - return null.
            current = null;
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
                if (current != null) {
                    // Fetch and mask the configuration before returning the update.
                    Configuration configuration = current.getConfiguration();
                    configuration.getMap().size();
                    ConfigurationDefinition configurationDefinition = getResourceConfigurationDefinitionForResourceType(
                        subjectManager.getOverlord(), resource.getResourceType().getId());
                    // We do not want the masked configuration persisted, so detach all entities before masking the configuration.
                    entityManager.clear();
                    ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
                }
                return current;
            }
        } catch (NoResultException nre) {
            // The resource hasn't been successfully configured yet - not a problem, we'll ask the agent for the live
            // config...
        }

        // ask the agent to get us the live, most up-to-date configuration for the resource,
        // then compare it to make sure what we think is the latest configuration is really the latest
        Configuration liveConfig = getLiveResourceConfiguration(resource, true, fromStructured);

        if (liveConfig != null) {
            Configuration currentConfig = (current != null) ? current.getConfiguration() : null;
            // Compare the live values and, if there is a difference with the current, store the live config as a new
            // update. Note that, if there is no current configuration stored, the live config is stored as the first
            // update.
            boolean theSame = (currentConfig != null && currentConfig.equals(liveConfig));

            // Someone dorked with the configuration on the agent side - save the live config as a new update.
            if (!theSame) {
                try {
                    current = persistNewAgentReportedResourceConfiguration(resource, liveConfig);
                } catch (ConfigurationUpdateStillInProgressException e) {
                    // This means a config update is INPROGRESS.
                    // Return the current in this case since it is our latest committed active config.
                    // Note that even though this application exception specifies "rollback=true", it will
                    // not effect our current transaction since the persist call was made with REQUIRES_NEW
                    // and thus only that new tx was rolled back
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Resource is currently in progress of changing its resource configuration - "
                            + "since it hasn't finished yet, will use the last successful resource configuration: " + e);
                    }
                }
            }
        } else {
            LOG.warn("Could not get live resource configuration for resource [" + resource
                    + "]; will assume latest resource configuration update is the current resource configuration.");
        }

        if (current != null) {
            // Mask the configuration before returning the update.
            Configuration configuration = current.getConfiguration();
            configuration.getMap().size();
            ConfigurationDefinition configurationDefinition = getResourceConfigurationDefinitionForResourceType(
                subjectManager.getOverlord(), resource.getResourceType().getId());
            // We do not want the masked configuration persisted, so detach all entities before masking the configuration.
            // But before we detach the entities, let's flush the entity manager to persist any pending changes.
            // This will ensure that:
            // 1) All changes in the entity manager are persisted
            // 2) Any changes to the entities made after the clear() call are NOT persisted.
            entityManager.flush();
            entityManager.clear();
            ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
        }
        return current;
    }

    @Override
    @Nullable
    public ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(Subject subject, int resourceId) {
        return getLatestResourceConfigurationUpdate(subject, resourceId, true);
    }

    /**
     * Will return the persisted Resource Configuration update, or null if the specified Configuration
     * is identical to the currently persisted Configuration.
     */
    private ResourceConfigurationUpdate persistNewAgentReportedResourceConfiguration(Resource resource,
        Configuration liveConfig) throws ConfigurationUpdateStillInProgressException {

        if (liveConfig.getRawConfigurations() != null) {
            for (RawConfiguration raw : liveConfig.getRawConfigurations()) {
                MessageDigestGenerator sha256Generator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
                sha256Generator.add(raw.getContents().getBytes());
                raw.setSha256(sha256Generator.getDigestString());
            }
        }

        /*
        * NOTE: We pass the overlord, since this is a system side-effect.  here, the system
        * and *not* the user, is choosing to persist the most recent configuration because it was different
        * from the last known value.  again, the user isn't attempting to change the value; instead, *JON*
        * is triggering save based on the semantics that we want to provide for configuration updates.
        * For the same reason, we pass null as the subject.
        */
        ResourceConfigurationUpdate update = this.configurationManager
            .persistResourceConfigurationUpdateInNewTransaction(this.subjectManager.getOverlord(), resource.getId(),
                liveConfig, ConfigurationUpdateStatus.SUCCESS, null, false);

        // resource.setResourceConfiguration(liveConfig.deepCopy(false));
        resource.setResourceConfiguration(liveConfig.deepCopyWithoutProxies());
        return update;
    }

    @Override
    public PluginConfigurationUpdate getLatestPluginConfigurationUpdate(Subject subject, int resourceId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting current plugin configuration for resource [" + resourceId + "]...");
        }

        Resource resource = entityManager.getReference(Resource.class, resourceId);
        if (resource == null) {
            throw new NoResultException("Cannot get latest plugin configuration for unknown Resource [" + resourceId
                + "].");
        }

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view plugin configuration for [" + resource + "].");
        }

        PluginConfigurationUpdate current;
        // Get the latest configuration as known to the server (i.e. persisted in the DB).
        try {
            Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG);
            query.setParameter("resourceId", resourceId);
            current = (PluginConfigurationUpdate) query.getSingleResult();

            resource = current.getResource();

            // Mask the configuration before returning the update.
            Configuration configuration = current.getConfiguration();
            configuration.getMap().size();
            ConfigurationDefinition configurationDefinition = getPluginConfigurationDefinitionForResourceType(
                subjectManager.getOverlord(), resource.getResourceType().getId());
            // We do not want the masked configuration persisted, so detach all entities before masking the configuration.
            entityManager.clear();
            ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
        } catch (NoResultException nre) {
            // The resource hasn't been successfully configured yet - return null.
            current = null;
        }

        return current;
    }

    @Override
    public boolean isResourceConfigurationUpdateInProgress(Subject subject, int resourceId) {
        boolean updateInProgress;
        try {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            ResourceConfigurationUpdate latestConfigUpdate = (ResourceConfigurationUpdate) query.getSingleResult();
            if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_READ, latestConfigUpdate
                .getResource().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view Resource configuration for ["
                    + latestConfigUpdate.getResource() + "]");
            }
            updateInProgress = (latestConfigUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }
        return updateInProgress;
    }

    @Override
    public boolean isPluginConfigurationUpdateInProgress(Subject subject, int resourceId) {
        boolean updateInProgress;
        try {
            Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            PluginConfigurationUpdate latestConfigUpdate = (PluginConfigurationUpdate) query.getSingleResult();
            if (!authorizationManager.canViewResource(subject, latestConfigUpdate.getResource().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view plugin configuration for ["
                    + latestConfigUpdate.getResource() + "]");
            }
            updateInProgress = (latestConfigUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }
        return updateInProgress;
    }

    @Override
    public boolean isGroupResourceConfigurationUpdateInProgress(Subject subject, int groupId) {
        boolean updateInProgress;
        try {
            Query query = entityManager
                .createNamedQuery(GroupResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID);
            query.setParameter("groupId", groupId);
            GroupResourceConfigurationUpdate latestConfigGroupUpdate = (GroupResourceConfigurationUpdate) query
                .getSingleResult();
            if (!authorizationManager.canViewGroup(subject, latestConfigGroupUpdate.getGroup().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view group Resource configuration for ["
                    + latestConfigGroupUpdate.getGroup() + "]");
            }

            updateInProgress = (latestConfigGroupUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The group resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }

        return updateInProgress;
    }

    @Override
    public boolean isGroupPluginConfigurationUpdateInProgress(Subject subject, int groupId) {
        boolean updateInProgress;
        try {
            Query query = entityManager.createNamedQuery(GroupPluginConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID);
            query.setParameter("groupId", groupId);
            GroupPluginConfigurationUpdate latestConfigGroupUpdate = (GroupPluginConfigurationUpdate) query
                .getSingleResult();
            if (!authorizationManager.canViewGroup(subject, latestConfigGroupUpdate.getGroup().getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view group plugin configuration for ["
                    + latestConfigGroupUpdate.getGroup() + "]");
            }

            updateInProgress = (latestConfigGroupUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS);
        } catch (NoResultException nre) {
            // The group resource config history is empty, so there's obviously no update in progress.
            updateInProgress = false;
        }

        return updateInProgress;
    }

    @Override
    public Map<Integer, Configuration> getResourceConfigurationsForCompatibleGroup(Subject subject, int groupId)
        throws ConfigurationUpdateStillInProgressException, Exception {

        if (authorizationManager.hasGroupPermission(subject, Permission.CONFIGURE_READ, groupId) == false) {
            throw new PermissionException("User[name=" + subject.getName()
                + "] does not have permission to view configuration for group[id=" + groupId + "]");
        }

        // The below call will also handle the check to see if the subject has perms to view the group.
        ResourceGroupComposite groupComposite = this.resourceGroupManager.getResourceGroupComposite(subject, groupId);

        // if the group is empty (has no members) the availability will be null
        if (0 == groupComposite.getExplicitCount()) {
            return new HashMap<Integer, Configuration>();
        }

        if (groupComposite.getExplicitDown() > 0)
            throw new Exception("Current group Resource configuration for " + groupId
                + " cannot be calculated, because one or more of this group's member Resources are DOWN.");

        // If we got this far, all member Resources are UP. Now check to make sure no config updates, group-level or
        // resource-level, are in progress.
        ResourceGroup group = groupComposite.getResourceGroup();
        ensureNoResourceConfigurationUpdatesInProgress(group);

        // If we got this far, no updates are in progress. Now try to obtain the live configs from the Agents.
        // If any of the requests for live configs fail (e.g. because an Agent is down) or if all of the live
        // configs can't be obtained within the specified timeout, this call will throw an exception.
        int userPreferencesTimeout = new SubjectPreferences(subject).getGroupConfigurationTimeoutPeriod();
        Set<Resource> groupMembers = group.getExplicitResources();
        Map<Integer, Configuration> liveConfigs = LiveConfigurationLoader.getInstance().loadLiveResourceConfigurations(
            groupMembers, userPreferencesTimeout);

        // If we got this far, we were able to retrieve all of the live configs from the Agents. Now load the current
        // persisted configs from the DB and compare them to the corresponding live configs. For any that are not equal,
        // persist the live config to the DB as the new current config.
        Map<Integer, Configuration> currentPersistedConfigs = getPersistedResourceConfigurationsForCompatibleGroup(group);
        for (Resource memberResource : groupMembers) {
            Configuration liveConfig = liveConfigs.get(memberResource.getId());
            // NOTE: The persisted config may be null if no config has been persisted yet.
            Configuration currentPersistedConfig = currentPersistedConfigs.get(memberResource.getId());
            if (!liveConfig.equals(currentPersistedConfig)) {
                // If the live config is different than the persisted config, persist it as the new current config.
                ResourceConfigurationUpdate update = persistNewAgentReportedResourceConfiguration(memberResource,
                    liveConfig);
                if (update != null) {
                    currentPersistedConfigs.put(memberResource.getId(), update.getConfiguration());
                    LOG.info("Live configuration for [" + memberResource
                            + "] did not match latest associated ResourceConfigurationUpdate with SUCCESS status.");
                } else {
                    // this means the live config is identical to the persisted config
                    currentPersistedConfigs.put(memberResource.getId(), liveConfig);
                }
            }
        }

        // Mask the configurations before returning them.
        for (Configuration resourceConfiguration : currentPersistedConfigs.values()) {
            resourceConfiguration.getMap().size();
        }
        ConfigurationDefinition resourceConfigurationDefinition = getResourceConfigurationDefinitionForResourceType(
            subjectManager.getOverlord(), group.getResourceType().getId());
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        entityManager.clear();
        for (Configuration resourceConfiguration : currentPersistedConfigs.values()) {
            ConfigurationMaskingUtility.maskConfiguration(resourceConfiguration, resourceConfigurationDefinition);
        }

        return currentPersistedConfigs;
    }

    @Override
    public Map<Integer, Configuration> getPluginConfigurationsForCompatibleGroup(Subject subject, int groupId)
        throws ConfigurationUpdateStillInProgressException, Exception {
        // The below call will also handle the check to see if the subject has perms to view the group.
        ResourceGroup group = this.resourceGroupManager
            .getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);

        // Check to make sure no config updates, group-level or resource-level, are in progress.
        ensureNoPluginConfigurationUpdatesInProgress(group);

        // If we got this far, no updates are in progress, so go ahead and load the plugin configs from the DB.
        Map<Integer, Configuration> currentPersistedConfigs = getPersistedPluginConfigurationsForCompatibleGroup(group);

        // Mask the configurations before returning them.
        for (Configuration pluginConfiguration : currentPersistedConfigs.values()) {
            pluginConfiguration.getMap().size();
        }
        ConfigurationDefinition pluginConfigurationDefinition = getPluginConfigurationDefinitionForResourceType(
            subjectManager.getOverlord(), group.getResourceType().getId());
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        entityManager.clear();
        for (Configuration pluginConfiguration : currentPersistedConfigs.values()) {
            ConfigurationMaskingUtility.maskConfiguration(pluginConfiguration, pluginConfigurationDefinition);
        }

        return currentPersistedConfigs;
    }

    @SuppressWarnings("unchecked")
    private void ensureNoResourceConfigurationUpdatesInProgress(ResourceGroup compatibleGroup)
        throws ConfigurationUpdateStillInProgressException {
        if (isGroupResourceConfigurationUpdateInProgress(this.subjectManager.getOverlord(), compatibleGroup.getId())) {
            throw new ConfigurationUpdateStillInProgressException("Current group Resource configuration for "
                + compatibleGroup
                + " cannot be calculated, because a group Resource configuration update is currently in progress "
                + " (please wait for this update to complete or delete it from the history).");
        }

        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID_AND_STATUS);
        countQuery.setParameter("groupId", compatibleGroup.getId());
        countQuery.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
        long count = (Long) countQuery.getSingleResult();
        if (count != 0) {
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID_AND_STATUS);
            query.setParameter("groupId", compatibleGroup.getId());
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<Resource> resources = query.getResultList();
            List<String> names = new ArrayList<String>();
            for (Resource resource : resources) {
                names.add(resource.getName());
            }
            throw new ConfigurationUpdateStillInProgressException("Current group Resource configuration for "
                + compatibleGroup
                + " cannot be calculated, because Resource configuration updates are currently in progress for the"
                + " following Resources (please wait for these updates to complete or delete them from the history): "
                + names);
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureNoPluginConfigurationUpdatesInProgress(ResourceGroup compatibleGroup)
        throws ConfigurationUpdateStillInProgressException {
        if (isGroupPluginConfigurationUpdateInProgress(this.subjectManager.getOverlord(), compatibleGroup.getId())) {
            throw new ConfigurationUpdateStillInProgressException("Current group plugin configuration for "
                + compatibleGroup
                + " cannot be calculated, because a group plugin configuration update is currently in progress.");
        }
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID_AND_STATUS);
        countQuery.setParameter("groupId", compatibleGroup.getId());
        countQuery.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
        long count = (Long) countQuery.getSingleResult();
        if (count > 0) {
            Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID_AND_STATUS);
            query.setParameter("groupId", compatibleGroup.getId());
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<PluginConfigurationUpdate> pluginConfigUpdates = query.getResultList();
            if (!pluginConfigUpdates.isEmpty()) {
                List<Integer> resourceIds = new ArrayList<Integer>(pluginConfigUpdates.size());
                for (PluginConfigurationUpdate pluginConfigUpdate : pluginConfigUpdates) {
                    resourceIds.add(pluginConfigUpdate.getResource().getId());
                }
                throw new ConfigurationUpdateStillInProgressException("Current group plugin configuration for "
                    + compatibleGroup
                    + " cannot be calculated, because plugin configuration updates are currently in progress for the"
                    + " member Resources with the following ID's (please wait for these updates to complete): "
                    + resourceIds);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Configuration> getPersistedResourceConfigurationsForCompatibleGroup(
        ResourceGroup compatibleGroup) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID);
        countQuery.setParameter("resourceGroupId", compatibleGroup.getId());
        long count = (Long) countQuery.getSingleResult();
        int groupSize = resourceGroupManager.getExplicitGroupMemberCount(compatibleGroup.getId());
        if (count != groupSize) {
            throw new IllegalStateException("Size of group changed from " + groupSize + " to " + count
                + " - please retry the operation.");
        }

        // Configurations are very expensive to load, so load 'em in chunks to ease the strain on the DB.
        PageControl pageControl = new PageControl(0, 20);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_ID, new OrderingField("r.id", PageOrdering.ASC));
        query.setParameter("resourceGroupId", compatibleGroup.getId());

        Map<Integer, Configuration> results = new HashMap<Integer, Configuration>((int) count);
        int rowsProcessed = 0;
        while (true) {
            PersistenceUtility.setDataPage(query, pageControl); // retrieve one page at a time
            List<Object[]> pagedResults = query.getResultList();

            if (pagedResults.size() <= 0) {
                break;
            }

            for (Object[] result : pagedResults) {
                results.put((Integer) result[0], (Configuration) result[1]);
            }

            rowsProcessed += pagedResults.size();
            if (rowsProcessed >= count) {
                break;
            }

            pageControl.setPageNumber(pageControl.getPageNumber() + 1); // advance the page
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Configuration> getPersistedPluginConfigurationsForCompatibleGroup(ResourceGroup compatibleGroup) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID);
        countQuery.setParameter("resourceGroupId", compatibleGroup.getId());
        long count = (Long) countQuery.getSingleResult();
        int groupSize = resourceGroupManager.getExplicitGroupMemberCount(compatibleGroup.getId());
        if (count != groupSize) {
            throw new IllegalStateException("Size of group changed from " + groupSize + " to " + count
                + " - please retry the operation.");
        }

        // Configurations are very expensive to load, so load 'em in chunks to ease the strain on the DB.
        PageControl pageControl = new PageControl(0, 20);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_ID, new OrderingField("r.id", PageOrdering.ASC));
        query.setParameter("resourceGroupId", compatibleGroup.getId());

        Map<Integer, Configuration> results = new HashMap<Integer, Configuration>((int) count);
        int rowsProcessed = 0;
        while (true) {
            PersistenceUtility.setDataPage(query, pageControl); // retrieve one page at a time
            List<Object[]> pagedResults = query.getResultList();

            if (pagedResults.size() <= 0) {
                break;
            }

            for (Object[] result : pagedResults) {
                results.put((Integer) result[0], (Configuration) result[1]);
            }

            rowsProcessed += pagedResults.size();
            if (rowsProcessed >= count) {
                break;
            }
            pageControl.setPageNumber(pageControl.getPageNumber() + 1); // advance the page
        }
        return results;
    }

    @Override
    public Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst)
        throws Exception {
        return getLiveResourceConfiguration(subject, resourceId, pingAgentFirst, true);
    }

    @Override
    public Configuration getLiveResourceConfiguration(Subject subject, int resourceId, boolean pingAgentFirst,
        boolean fromStructured) throws Exception {
        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        // ask the agent to get us the live, most up-to-date configuration for the resource
        Configuration liveConfig = getLiveResourceConfiguration(resource, pingAgentFirst, fromStructured);

        return liveConfig;
    }

    @Override
    public void checkForTimedOutConfigurationUpdateRequests() {
        LOG.debug("Begin scanning for timed out configuration update requests");
        checkForTimedOutResourceConfigurationUpdateRequests();
        checkForTimedOutGroupResourceConfigurationUpdateRequests();
        LOG.debug("Finished scanning for timed out configuration update requests");
    }

    @SuppressWarnings("unchecked")
    private void checkForTimedOutResourceConfigurationUpdateRequests() {
        try {
            // TODO (ips): Optimize this so the query actually does the timeout check too,
            //             i.e. "WHERE cu.createdtime > :maxCreatedTime".
            Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS);
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<ResourceConfigurationUpdate> requests = query.getResultList();
            for (ResourceConfigurationUpdate request : requests) {
                // TODO [mazz]: should we make this configurable?
                long timeout = 1000L * 60 * 10; // 10 minutes - should be more than enough time

                long duration = request.getDuration();
                if (duration > timeout) {
                    LOG.info("Resource configuration update request seems to have been orphaned - timing it out: "
                            + request);
                    request.setErrorMessage("Timed out - did not complete after " + duration + " ms"
                        + " (the timeout period was " + timeout + " ms)");
                    request.setStatus(ConfigurationUpdateStatus.FAILURE);
                    // If it's part of a group update, check if all member updates of the group update have completed,
                    // and, if so, update the group update's status.
                    checkForCompletedGroupResourceConfigurationUpdate(request.getId());
                }
            }
        } catch (Throwable t) {
            LOG.error("Failed to check for timed out Resource configuration update requests. Cause: " + t);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkForTimedOutGroupResourceConfigurationUpdateRequests() {
        try {
            // TODO (ips): Optimize this so the query actually does the timeout check too,
            //             i.e. "WHERE cu.createdtime > :maxCreatedTime".
            Query query = entityManager.createNamedQuery(GroupResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS);
            query.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
            List<GroupResourceConfigurationUpdate> requests = query.getResultList();
            for (GroupResourceConfigurationUpdate request : requests) {
                // Note: Make this a little longer than the timeout used for individual Resource config updates
                //       (see checkForTimedOutResourceConfigurationUpdateRequests()), to ensure a group update never
                //       gets timed out before one or more of its member updates.
                long timeout = 1000L * 60 * 11; // 11 minutes

                long duration = request.getDuration();
                if (duration > timeout) {

                    LOG.info("Group Resource config update request seems to be orphaned - timing it out: " + request);

                    request.setErrorMessage("Timed out - did not complete after " + duration + " ms"
                        + " (the timeout period was " + timeout + " ms)");
                    request.setStatus(ConfigurationUpdateStatus.FAILURE);
                }
            }
        } catch (Throwable t) {
            LOG.error("Failed to check for timed out group Resource configuration update requests. Cause: " + t);
        }
    }

    /**
     * @deprecated use {@link #findPluginConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria)}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<PluginConfigurationUpdate> findPluginConfigurationUpdates(Subject subject, int resourceId,
        Long beginDate, Long endDate, PageControl pc) {

        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource.getResourceType().getPluginConfigurationDefinition() == null
            || resource.getResourceType().getPluginConfigurationDefinition().getPropertyDefinitions().isEmpty()) {
            return new PageList<PluginConfigurationUpdate>(pc);
        }

        pc.initDefaultOrderingField("cu.id", PageOrdering.DESC);

        String queryName = PluginConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        queryCount.setParameter("startTime", beginDate);
        query.setParameter("startTime", beginDate);

        queryCount.setParameter("endTime", endDate);
        query.setParameter("endTime", endDate);

        long totalCount = (Long) queryCount.getSingleResult();
        List<PluginConfigurationUpdate> updates = query.getResultList();

        if ((updates == null) || (updates.size() == 0)) {
            // there is no configuration yet - get the latest from the agent, if possible
            updates = new ArrayList<PluginConfigurationUpdate>();
            PluginConfigurationUpdate latest = getLatestPluginConfigurationUpdate(subject, resourceId);
            if (latest != null) {
                updates.add(latest);
            }
        } else if (updates.size() > 0) {
            resource = updates.get(0).getResource();
            if (!authorizationManager.canViewResource(subject, resource.getId())) {
                throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to view resource [" + resource + "]");
            }
        }

        /*// Mask the configurations before returning the updates.
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        entityManager.clear();
        for (PluginConfigurationUpdate update : updates) {
            Configuration configuration = update.getConfiguration();
            ConfigurationDefinition configurationDefinition = getPluginConfigurationDefinitionForResourceType(
                subjectManager.getOverlord(), update.getResource().getResourceType().getId());
            ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
        }*/

        return new PageList<PluginConfigurationUpdate>(updates, (int) totalCount, pc);
    }

    /**
     * @deprecated use {@link #findResourceConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria)}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(Subject subject, Integer resourceId,
        Long beginDate, Long endDate, boolean suppressOldest, PageControl pc) {

        if (resourceId == null && !authorizationManager.isInventoryManager(subject)) {
            throw new PermissionException("User[" + subject.getName() + "] Must be an inventory manager to query "
                + "without a resource id.");
        } else if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_READ, resourceId)) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view configuration history for resource[id=" + resourceId + "]");
        }

        // TODO (ips, 04/01/10): Our id's are not guaranteed to be sequential, because our sequences are configured to
        //                       pre-create and cache blocks of 10 sequence id's, so it may be better to order by
        //                       "cu.createdTime", rather than "cu.id".
        pc.initDefaultOrderingField("cu.id", PageOrdering.DESC);

        String queryName = ResourceConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        queryCount.setParameter("resourceId", resourceId);
        query.setParameter("resourceId", resourceId);

        queryCount.setParameter("startTime", beginDate);
        query.setParameter("startTime", beginDate);

        queryCount.setParameter("endTime", endDate);
        query.setParameter("endTime", endDate);

        int includeAll = suppressOldest ? 0 : 1;
        queryCount.setParameter("includeAll", includeAll);
        query.setParameter("includeAll", includeAll);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ResourceConfigurationUpdate> updates = query.getResultList();

        if (suppressOldest == false && updates.size() == 0) {
            // there is no configuration yet - get the latest from the agent, if possible
            updates = new ArrayList<ResourceConfigurationUpdate>();
            ResourceConfigurationUpdate latest = getLatestResourceConfigurationUpdate(subject, resourceId);
            if (latest != null) {
                updates.add(latest);
            }
        }

        /*// Mask the configurations before returning the updates.
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        entityManager.clear();
        for (ResourceConfigurationUpdate update : updates) {
            Configuration configuration = update.getConfiguration();
            ConfigurationDefinition configurationDefinition = getResourceConfigurationDefinitionForResourceType(
                subjectManager.getOverlord(), update.getResource().getResourceType().getId());
            ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
        }*/

        return new PageList<ResourceConfigurationUpdate>(updates, (int) totalCount, pc);
    }

    /**
     * @deprecated use criteria-based API
     */
    @Override
    public PluginConfigurationUpdate getPluginConfigurationUpdate(Subject subject, int configurationUpdateId) {
        PluginConfigurationUpdate update = entityManager.find(PluginConfigurationUpdate.class, configurationUpdateId);

        if (!authorizationManager.canViewResource(subject, update.getResource().getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view plugin configuration update for [" + update.getResource() + "]");
        }

        update.getConfiguration(); // this is EAGER loaded, so this really doesn't do anything

        return update;
    }

    /**
     * @deprecated use criteria-based API
     */
    @Override
    public ResourceConfigurationUpdate getResourceConfigurationUpdate(Subject subject, int configurationUpdateId) {
        ResourceConfigurationUpdate update = entityManager.find(ResourceConfigurationUpdate.class,
            configurationUpdateId);

        if (!authorizationManager.canViewResource(subject, update.getResource().getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration update for [" + update.getResource() + "]");
        }

        update.getConfiguration(); // this is EAGER loaded, so this really doesn't do anything

        return update;
    }

    @Override
    public void purgePluginConfigurationUpdate(Subject subject, int configurationUpdateId, boolean purgeInProgress) {
        PluginConfigurationUpdate doomedRequest = entityManager.find(PluginConfigurationUpdate.class,
            configurationUpdateId);

        if (doomedRequest == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Asked to purge a non-existing config update request [" + configurationUpdateId + "]");
            }
            return;
        }

        if ((doomedRequest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) && !purgeInProgress) {
            throw new IllegalStateException(
                "The update request is still in the in-progress state. Please wait for it to complete: "
                    + doomedRequest);
        }

        // make sure the user has the proper permissions to do this
        Resource resource = doomedRequest.getResource();
        if (!authorizationManager.hasResourcePermission(subject, Permission.MODIFY_RESOURCE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to purge a plugin configuration update audit trail for resource ["
                + resource + "]");
        }

        resource.getPluginConfigurationUpdates().remove(doomedRequest);
        entityManager.remove(doomedRequest);
        entityManager.flush();

        return;
    }

    @Override
    public void purgePluginConfigurationUpdates(Subject subject, int[] configurationUpdateIds, boolean purgeInProgress) {
        if ((configurationUpdateIds == null) || (configurationUpdateIds.length == 0)) {
            return;
        }

        // TODO [mazz]: ugly - let's make this more efficient, just getting this to work first
        for (int configurationUpdateId : configurationUpdateIds) {
            purgePluginConfigurationUpdate(subject, configurationUpdateId, purgeInProgress);
        }

        return;
    }

    @Override
    public void purgeResourceConfigurationUpdate(Subject subject, int configurationUpdateId, boolean purgeInProgress) {
        ResourceConfigurationUpdate doomedRequest = entityManager.find(ResourceConfigurationUpdate.class,
            configurationUpdateId);

        if (doomedRequest == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Asked to purge a non-existing config update request [" + configurationUpdateId + "]");
            }
            return;
        }

        if ((doomedRequest.getStatus() == ConfigurationUpdateStatus.INPROGRESS) && !purgeInProgress) {
            throw new IllegalStateException(
                "The update request is still in the in-progress state. Please wait for it to complete: "
                    + doomedRequest);
        }

        // make sure the user has the proper permissions to do this
        Resource resource = doomedRequest.getResource();
        if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_WRITE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to purge a configuration update audit trail for resource [" + resource
                + "]");
        }

        resource.getResourceConfigurationUpdates().remove(doomedRequest);
        entityManager.remove(doomedRequest);
        entityManager.flush();

        return;
    }

    @Override
    public void purgeResourceConfigurationUpdates(Subject subject, int[] configurationUpdateIds, boolean purgeInProgress) {
        if ((configurationUpdateIds == null) || (configurationUpdateIds.length == 0)) {
            return;
        }

        // TODO [mazz]: ugly - let's make this more efficient, just getting this to work first
        for (int configurationUpdateId : configurationUpdateIds) {
            purgeResourceConfigurationUpdate(subject, configurationUpdateId, purgeInProgress);
        }

        return;
    }

    @Override
    public ResourceConfigurationUpdate updateStructuredOrRawConfiguration(Subject subject, int resourceId,
        Configuration newConfiguration, boolean fromStructured) throws ResourceNotFoundException,
        ConfigurationUpdateStillInProgressException {

        if (authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_WRITE, resourceId) == false) {
            throw new PermissionException("User[name=" + subject.getName()
                + "] does not have the permission to update configuration for resource[id=" + resourceId + "]");
        }

        Configuration configToUpdate = newConfiguration;

        if (isStructuredAndRawSupported(resourceId)) {
            configToUpdate = translateResourceConfiguration(subject, resourceId, newConfiguration, fromStructured);
        }
        try {
            Configuration invalidConfig = validateResourceConfiguration(subject, resourceId, newConfiguration,
                fromStructured);
            if (null != invalidConfig) {
                Resource resource = resourceManager.getResourceById(subject, resourceId);
                ResourceConfigurationUpdate resourceConfigurationUpdate = new ResourceConfigurationUpdate(resource,
                    invalidConfig, subject.getName());
                resourceConfigurationUpdate.setErrorMessage("resource.validation.failed");
                resourceConfigurationUpdate.setStatus(ConfigurationUpdateStatus.FAILURE);

                return resourceConfigurationUpdate;
            }
        } catch (PluginContainerException e) {
            Resource resource = resourceManager.getResourceById(subject, resourceId);
            ResourceConfigurationUpdate resourceConfigurationUpdate = new ResourceConfigurationUpdate(resource,
                newConfiguration, subject.getName());
            resourceConfigurationUpdate.setErrorMessage(e.getMessage());
            resourceConfigurationUpdate.setStatus(ConfigurationUpdateStatus.FAILURE);
            return resourceConfigurationUpdate;
        }

        ResourceConfigurationUpdate newUpdate = configurationManager
            .persistResourceConfigurationUpdateInNewTransaction(subject, resourceId, configToUpdate,
                ConfigurationUpdateStatus.INPROGRESS, subject.getName(), false);
        executeResourceConfigurationUpdate(newUpdate);
        return newUpdate;
    }

    private Configuration validateResourceConfiguration(Subject subject, int resourceId, Configuration configuration,
        boolean isStructured) throws PluginContainerException {
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }
        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }
        Agent agent = resource.getAgent();
        AgentClient agentClient = this.agentManager.getAgentClient(agent);
        ConfigurationAgentService configService = agentClient.getConfigurationAgentService();
        return configService.validate(configuration, resourceId, isStructured);
    }

    private boolean isStructuredAndRawSupported(int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        ConfigurationDefinition configDef = resource.getResourceType().getResourceConfigurationDefinition();
        if (configDef == null) {
            return false;
        }
        return ConfigurationFormat.STRUCTURED_AND_RAW == configDef.getConfigurationFormat();
    }

    @Override
    @Nullable
    public ResourceConfigurationUpdate updateResourceConfiguration(Subject subject, int resourceId,
        Configuration newResourceConfiguration) throws ResourceNotFoundException {

        if (isStructuredAndRawSupported(resourceId)) {
            throw new ConfigurationUpdateNotSupportedException("Cannot update a resource configuration that "
                + "supports both structured and raw configuration using this method because there is insufficient "
                + "information. You should instead call updateStructuredOrRawConfiguration() which requires you "
                + "whether the structured or raw was updated.");
        }

        if (authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_WRITE, resourceId) == false) {
            throw new PermissionException("User[name=" + subject.getName()
                + "] does not have the permission to update configuration for resource[id=" + resourceId + "]");
        }

        // Make sure to unmask the configuration before persisting the update.
        Resource resource = resourceManager.getResource(subjectManager.getOverlord(), resourceId);
        Configuration existingResourceConfiguration = resource.getResourceConfiguration();
        ConfigurationMaskingUtility.unmaskConfiguration(newResourceConfiguration, existingResourceConfiguration);

        // Calling the persist method via the EJB interface to pick up the method's REQUIRES_NEW semantics and persist
        // the update in a separate transaction; this way, the update is committed prior to sending the agent request
        // Note, the persist method will return null if newConfiguration is no different than the current Resource
        // configuration.
        // TODO: Consider synchronizing to avoid the condition where someone calls this method twice quickly in two
        // different tx's, which would put two updates in INPROGRESS and cause havoc.
        ResourceConfigurationUpdate newUpdate = configurationManager
            .persistResourceConfigurationUpdateInNewTransaction(subject, resourceId, newResourceConfiguration,
                ConfigurationUpdateStatus.INPROGRESS, subject.getName(), false);

        executeResourceConfigurationUpdate(newUpdate);

        return newUpdate;
    }

    @Override
    public void executeResourceConfigurationUpdate(int updateId) {
        ResourceConfigurationUpdate update = getResourceConfigurationUpdate(subjectManager.getOverlord(), updateId);
        Configuration originalConfig = update.getConfiguration();
        update.setConfiguration(originalConfig.deepCopy(false));
        executeResourceConfigurationUpdate(update);
    }

    /**
     * Tells the Agent to asynchronously update a managed resource's configuration as per the specified
     * <code>ResourceConfigurationUpdate</code>.
     */
    private void executeResourceConfigurationUpdate(ResourceConfigurationUpdate update) {
        try {
            AgentClient agentClient = agentManager.getAgentClient(update.getResource().getAgent());
            ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(update.getId(),
                update.getConfiguration(), update.getResource().getId());
            agentClient.getConfigurationAgentService().updateResourceConfiguration(request);
        } catch (RuntimeException e) {
            // Any exception means the remote call itself failed - make sure to change the status on the update to FAILURE
            // and set its error message field.
            if (null != update) {
                update.setStatus(ConfigurationUpdateStatus.FAILURE);
                update.setErrorMessage(ThrowableUtil.getStackAsString(e));

                // Here we call ourselves, but we do so via the EJB interface so we pick up the REQUIRES_NEW semantics.
                this.configurationManager.mergeConfigurationUpdate(update);
                checkForCompletedGroupResourceConfigurationUpdate(update.getId());
            }
        }
    }

    @Override
    public void rollbackResourceConfiguration(Subject subject, int resourceId, int configHistoryId)
        throws ConfigurationUpdateException {
        ResourceConfigurationUpdate configurationUpdateHistory = entityManager.find(ResourceConfigurationUpdate.class,
            configHistoryId);
        Configuration configuration = configurationUpdateHistory.getConfiguration();
        if (configuration == null) {
            throw new ConfigurationUpdateException("No configuration history element exists with id = '"
                + configHistoryId + "'");
        }

        if (isStructuredAndRawSupported(resourceId)) {
            updateStructuredOrRawConfiguration(subject, resourceId, configuration, false);
        } else {
            updateResourceConfiguration(subject, resourceId, configuration);
        }
    }

    @Override
    public void rollbackPluginConfiguration(Subject subject, int resourceId, int configHistoryId)
        throws ConfigurationUpdateException {
        PluginConfigurationUpdate configurationUpdateHistory = entityManager.find(PluginConfigurationUpdate.class,
            configHistoryId);
        Configuration configuration = configurationUpdateHistory.getConfiguration();
        if (configuration == null) {
            throw new ConfigurationUpdateException("No plugin configuration history element exists with id = '"
                + configHistoryId + "'");
        }

        updatePluginConfiguration(subject, resourceId, configuration);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResourceConfigurationUpdate persistResourceConfigurationUpdateInNewTransaction(Subject subject,
        int resourceId, Configuration newConfiguration, ConfigurationUpdateStatus newStatus, String newSubject,
        boolean isPartofGroupUpdate) throws ResourceNotFoundException, ConfigurationUpdateStillInProgressException {

        ResourceConfigurationUpdate current = null;
        String errorMessage = null;

        // for efficiency, in one query fetch IN_PROGRESS and/or the current update
        Query query = entityManager
            .createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_CURRENT_AND_IN_PROGRESS_CONFIGS);
        query.setParameter("resourceId", resourceId);
        List<?> updates = query.getResultList();
        for (Object result : updates) {
            current = (ResourceConfigurationUpdate) result;

            if (ConfigurationUpdateStatus.INPROGRESS == current.getStatus()) {
                // A previous update is still in progress for this Resource. If this update is part of a group
                // update, persist it with FAILURE status, so it is still listed as part of the group history.
                // Otherwise, throw an exception that can bubble up to the GUI.
                if (isPartofGroupUpdate) {
                    newStatus = ConfigurationUpdateStatus.FAILURE;
                    errorMessage = "Resource configuration Update was aborted because an update request for the Resource was already in progress.";

                } else {
                    // NOTE: If you change this to another exception, make sure you change getLatestResourceConfigurationUpdate().
                    throw new ConfigurationUpdateStillInProgressException(
                        "Resource ["
                            + resourceId
                            + "] has a resource configuration update request already in progress - please wait for it to finish: "
                            + current);
                }
            }
        }

        Resource resource = null;

        if (null != current) {
            // Always persist a group update because each member must have an update. Otherwise, only persist a
            // single resource update if it has changed.
            if (!isPartofGroupUpdate) {
                Configuration currentConfiguration = current.getConfiguration();
                Hibernate.initialize(currentConfiguration.getMap());
                if (currentConfiguration.equals(newConfiguration)) {
                    return null;
                }
            }

            resource = current.getResource();

        } else {
            // make sure the resource exists
            resource = resourceManager.getResourceById(subject, resourceId);
        }

        Configuration zeroedConfiguration = newConfiguration.deepCopyWithoutProxies();

        // create our new update request and assign it to our resource - its status will initially be INPROGRESS
        ResourceConfigurationUpdate newUpdateRequest = new ResourceConfigurationUpdate(resource, zeroedConfiguration,
            newSubject);
        newUpdateRequest.setStatus(newStatus);

        if (ConfigurationUpdateStatus.FAILURE == newStatus) {
            newUpdateRequest.setErrorMessage(errorMessage);
        }

        entityManager.persist(newUpdateRequest);

        // No need to alert on the first configuration update, only on subsequent change
        if (null != current) {
            if (ConfigurationUpdateStatus.SUCCESS == newStatus) {
                notifyAlertConditionCacheManager("persistNewResourceConfigurationUpdateHistory", newUpdateRequest);
            }
        }

        resource.addResourceConfigurationUpdates(newUpdateRequest);

        // provide the agent while we have the entity, it's typically needed by the caller
        Hibernate.initialize(resource.getAgent());

        return newUpdateRequest;
    }

    private void notifyAlertConditionCacheManager(String callingMethod, ResourceConfigurationUpdate update) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(update);

        if (LOG.isDebugEnabled()) {
            LOG.debug(callingMethod + ": " + stats);
        }
    }

    @Override
    public void completeResourceConfigurationUpdate(ConfigurationUpdateResponse response) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received a configuration-update-completed message: " + response);
        }

        // find the current update request that is persisted - this is the one that is being reported as being complete
        ResourceConfigurationUpdate update = entityManager.find(ResourceConfigurationUpdate.class,
            response.getConfigurationUpdateId());
        if (update == null) {
            throw new IllegalStateException(
                "The completed request passed in does not match any request for any resource in inventory: " + response);
        }

        if (response.getStatus() == ConfigurationUpdateStatus.FAILURE) {
            // TODO [mazz]: what happens if the plugin dorked with the configuration ID? need to assert it hasn't changed
            if (response.getConfiguration() != null) {
                Configuration failedConfiguration = response.getConfiguration();
                failedConfiguration = entityManager.merge(failedConfiguration); // merge in any property error messages
                update.setConfiguration(failedConfiguration);
            }
        } else if (response.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
            // link to the newer, persisted configuration object
            Resource resource = update.getResource();
            resource.setResourceConfiguration(update.getConfiguration().deepCopyWithoutProxies());
            notifyAlertConditionCacheManager("completeResourceConfigurationUpdate", update);
        }

        // Make sure we update the persisted request with the new status and any error message.
        update.setStatus(response.getStatus());
        update.setErrorMessage(response.getErrorMessage());

        /*
         * instead of checking for completed group resource configuration updates here, let our caller (the
         * ConfigurationServerService) do it so that this transaction completes before the check begins
         */
        return;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void checkForCompletedGroupResourceConfigurationUpdate(int resourceConfigUpdateId) {
        ResourceConfigurationUpdate resourceConfigUpdate = entityManager.find(ResourceConfigurationUpdate.class,
            resourceConfigUpdateId);
        if (resourceConfigUpdate.getStatus() == ConfigurationUpdateStatus.INPROGRESS) {
            // If this update isn't done, then, by definition, the group update isn't done either.
            return;
        }

        GroupResourceConfigurationUpdate groupConfigUpdate = resourceConfigUpdate.getGroupConfigurationUpdate();
        if (groupConfigUpdate == null) {
            // The update isn't part of a group update - nothing we need to do.
            return;
        }

        Query inProgressResourcesCountQuery = PersistenceUtility.createCountQuery(this.entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID_AND_STATUS);
        inProgressResourcesCountQuery.setParameter("parentUpdateId", groupConfigUpdate.getId());
        inProgressResourcesCountQuery.setParameter("status", ConfigurationUpdateStatus.INPROGRESS);
        long inProgressResourcesCount = (Long) inProgressResourcesCountQuery.getSingleResult();
        if (inProgressResourcesCount == 0) {
            // No more member updates in progress - the group update is complete.
            Query failedResourcesQuery = this.entityManager
                .createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID_AND_STATUS);
            failedResourcesQuery.setParameter("parentUpdateId", groupConfigUpdate.getId());
            failedResourcesQuery.setParameter("status", ConfigurationUpdateStatus.FAILURE);
            List<Resource> failedResources = failedResourcesQuery.getResultList();
            ConfigurationUpdateStatus groupStatus;
            if (failedResources.isEmpty()) {
                groupStatus = ConfigurationUpdateStatus.SUCCESS;
            } else {
                groupStatus = ConfigurationUpdateStatus.FAILURE;
                groupConfigUpdate.setErrorMessage("The following Resources failed to update their Configurations: "
                    + failedResources);
            }
            groupConfigUpdate.setStatus(groupStatus);
            LOG.info("Group Resource configuration update [" + groupConfigUpdate.getId() + "] for "
                    + groupConfigUpdate.getGroup() + " has completed with status [" + groupStatus + "].");
            // TODO: Add support for alerting on completion of group resource config updates.
            //notifyAlertConditionCacheManager("checkForCompletedGroupResourceConfigurationUpdate", groupUpdate);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Group Resource configuration update [" + groupConfigUpdate.getId() + "] for "
                    + groupConfigUpdate.getGroup() + " has " + inProgressResourcesCount
                    + " member updates still in progress.");
            }
        }
        return;
    }

    @Override
    public ConfigurationDefinition getPackageTypeConfigurationDefinition(Subject subject, int packageTypeId) {
        Query query = entityManager.createNamedQuery(ConfigurationDefinition.QUERY_FIND_DEPLOYMENT_BY_PACKAGE_TYPE_ID);
        query.setParameter("packageTypeId", packageTypeId);
        ConfigurationDefinition configurationDefinition = null;
        try {
            configurationDefinition = (ConfigurationDefinition) query.getSingleResult();
        } catch (NoResultException e) {
            PackageType packageType = entityManager.find(PackageType.class, packageTypeId);
            if (packageType == null) {
                throw new EntityNotFoundException("A package type with id " + packageTypeId + " does not exist.");
            }
        }

        return configurationDefinition;
    }

    @Override
    public ConfigurationDefinition getResourceConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
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

    @Override
    @Nullable
    public ConfigurationDefinition getResourceConfigurationDefinitionWithTemplatesForResourceType(Subject subject,
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

    @Override
    public ConfigurationDefinition getPluginConfigurationDefinitionForResourceType(Subject subject, int resourceTypeId) {
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
     * @param  pingAgentFirst true if the underlying Agent should be pinged successfully before attempting to retrieve
     *                        the configuration, or false otherwise
     *
     * @return the resource's live configuration or <code>null</code> if it could not be retrieved from the agent
     */
    private Configuration getLiveResourceConfiguration(Resource resource, boolean pingAgentFirst, boolean fromStructured) {
        Configuration liveConfig = null;

        try {
            Agent agent = resource.getAgent();
            AgentClient agentClient = this.agentManager.getAgentClient(agent);

            boolean agentPingedSuccessfully = false;
            // Getting live configuration is mostly for the UI's benefit - as such, do not hang
            // for a long time in the event the agent is down or can't be reached.  Let's make the UI
            // responsive even in the case of an agent down by pinging it quickly to verify the agent is up.
            if (pingAgentFirst)
                agentPingedSuccessfully = agentClient.ping(5000L);

            if (!pingAgentFirst || agentPingedSuccessfully) {
                liveConfig = agentClient.getConfigurationAgentService().loadResourceConfiguration(resource.getId());
                if (liveConfig == null) {
                    // This should really never occur - the PC should never return a null, always at least an empty config.
                    LOG.debug("ConfigurationAgentService.loadResourceConfiguration() returned a null Configuration.");
                    liveConfig = new Configuration();
                }
            } else {
                LOG.warn("Agent is unreachable [" + agent + "] - cannot get live configuration for resource ["
                        + resource + "]");
            }
        } catch (Exception e) {
            LOG.warn("Could not get live configuration for resource [" + resource + "]"
                    + ThrowableUtil.getAllMessages(e, true));
        }

        return liveConfig;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AbstractResourceConfigurationUpdate mergeConfigurationUpdate(
        AbstractResourceConfigurationUpdate configurationUpdate) {
        return this.entityManager.merge(configurationUpdate);
    }

    @Override
    public Configuration getConfigurationById(int id) {
        return entityManager.find(Configuration.class, id);
    }

    @Override
    public Configuration getConfiguration(Subject subject, int configurationId) {
        Configuration configuration = getConfigurationById(configurationId);
        return configuration;
    }

    @Override
    public Configuration getConfigurationFromDefaultTemplate(ConfigurationDefinition definition) {
        ConfigurationDefinition managedDefinition = entityManager.find(ConfigurationDefinition.class,
            definition.getId());
        Configuration configuration = managedDefinition.getDefaultTemplate().getConfiguration();
        ConfigurationMaskingUtility.maskConfiguration(configuration, managedDefinition);
        return configuration;
    }

    private void handlePluginConfiguratonUpdateRemoteException(Resource resource, String summary, String detail) {
        resource.setConnected(false);
        ResourceError invalidPluginConfigError = new ResourceError(resource,
            ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, summary, detail, Calendar.getInstance().getTimeInMillis());
        this.resourceManager.addResourceError(invalidPluginConfigError);
    }

    private void removeAnyExistingInvalidPluginConfigurationErrors(Resource resource) {
        this.resourceManager.clearResourceConfigError(resource.getId());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int createGroupConfigurationUpdate(AbstractGroupConfigurationUpdate update) throws SchedulerException {
        entityManager.persist(update);
        return update.getId();
    }

    @Override
    public int scheduleGroupPluginConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> memberPluginConfigurations) {

        if (memberPluginConfigurations == null) {
            throw new IllegalArgumentException(
                "GroupPluginConfigurationUpdate must have non-null member configurations.");
        }

        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, compatibleGroupId);

        ensureModifyResourcePermission(subject, group);

        /*
         * we need to create and persist the group in a new/separate transaction before the rest of the
         * processing of this method; if we try to create and attach the PluginConfigurationUpdate children
         * to the parent group before the group update is actually persisted, we'll get StaleStateExceptions
         * from Hibernate because of our use of flush/clear (we're trying to update it before it actually
         * actually exists)
         */
        GroupPluginConfigurationUpdate groupUpdate = new GroupPluginConfigurationUpdate(group, subject.getName());
        int updateId = -1;
        try {
            updateId = configurationManager.createGroupConfigurationUpdate(groupUpdate);
        } catch (SchedulerException sche) {
            String message = "Error scheduling plugin configuration update for group[id=" + group.getId() + "]";
            LOG.error(message, sche);
            throw new ResourceGroupUpdateException(message + ": " + sche);
        }

        // Create and persist updates for each of the members.
        for (Integer resourceId : memberPluginConfigurations.keySet()) {
            Configuration memberPluginConfiguration = memberPluginConfigurations.get(resourceId);
            // Make sure to unmask the configuration before persisting the update.
            Resource resource = resourceManager.getResource(subjectManager.getOverlord(), resourceId);
            Configuration existingPluginConfiguration = resource.getPluginConfiguration();
            ConfigurationMaskingUtility.unmaskConfiguration(memberPluginConfiguration, existingPluginConfiguration);
            Resource flyWeight = new Resource(resourceId);
            PluginConfigurationUpdate memberUpdate = new PluginConfigurationUpdate(flyWeight,
                memberPluginConfiguration, subject.getName());
            memberUpdate.setGroupConfigurationUpdate(groupUpdate);
            entityManager.persist(memberUpdate);
        }

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_CONFIG_GROUP_UPDATE_ID, updateId);
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_SUBJECT_ID, subject.getId());

        /*
         * acquire quartz objects and schedule the group update, but deferred the execution for 10 seconds
         * because we need this transaction to complete so that the data is available when the quartz job triggers
         */
        JobDetail jobDetail = GroupPluginConfigurationUpdateJob.getJobDetail(group, subject, jobDataMap);
        Trigger trigger = QuartzUtil.getFireOnceOffsetTrigger(jobDetail, 10000);
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            String message = "Error scheduling job named '" + jobDetail.getName() + "':";
            LOG.error(message, e);
            throw new ResourceGroupUpdateException(message + e.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled plugin configuration update against compatibleGroup[id=" + compatibleGroupId + "]");
        }

        return updateId;
    }

    @Override
    public int scheduleGroupResourceConfigurationUpdate(Subject subject, int compatibleGroupId,
        Map<Integer, Configuration> newResourceConfigurationMap) {

        if (newResourceConfigurationMap == null) {
            throw new IllegalArgumentException(
                "GroupResourceConfigurationUpdate must have non-null member configurations.");
        }

        ResourceGroup group = getCompatibleGroupIfAuthorized(subject, compatibleGroupId);

        if (!authorizationManager.hasGroupPermission(subject, Permission.CONFIGURE_WRITE, group.getId())) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission "
                + "to modify Resource configurations for members of group [" + group + "].");
        }

        ensureNoResourceConfigurationUpdatesInProgress(group);

        /*
         * we need to create and persist the group update in a new/separate transaction before the rest of the
         * processing of this method; if we try to create and attach the PluginConfigurationUpdate children
         * to the parent group before the group update is actually persisted, we'll get StaleStateExceptions
         * from Hibernate because of our use of flush/clear (we're trying to update it before it actually exists)
         */
        GroupResourceConfigurationUpdate groupUpdate = new GroupResourceConfigurationUpdate(group, subject.getName());
        int updateId = -1;
        try {
            updateId = configurationManager.createGroupConfigurationUpdate(groupUpdate);
        } catch (SchedulerException sche) {
            String message = "Error scheduling resource configuration update for group[id=" + group.getId() + "]";
            LOG.error(message, sche);
            throw new ResourceGroupUpdateException(message + ": " + sche);
        }

        // Create and persist updates for each of the members.
        for (Integer resourceId : newResourceConfigurationMap.keySet()) {
            Configuration memberResourceConfiguration = newResourceConfigurationMap.get(resourceId);
            // Make sure to unmask the configuration before persisting the update.
            Resource resource = resourceManager.getResource(subjectManager.getOverlord(), resourceId);
            Configuration existingResourceConfiguration = resource.getResourceConfiguration();
            ConfigurationMaskingUtility.unmaskConfiguration(memberResourceConfiguration, existingResourceConfiguration);
            Resource flyWeight = new Resource(resourceId);
            ResourceConfigurationUpdate memberUpdate = new ResourceConfigurationUpdate(flyWeight,
                memberResourceConfiguration, subject.getName());
            memberUpdate.setGroupConfigurationUpdate(groupUpdate);
            entityManager.persist(memberUpdate);
        }

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_CONFIG_GROUP_UPDATE_ID, updateId);
        jobDataMap.putAsString(AbstractGroupConfigurationUpdateJob.DATAMAP_INT_SUBJECT_ID, subject.getId());

        /*
         * Acquire Quartz objects and schedule the group update, but defer the execution for 10 seconds,
         * because we need this transaction to complete so that the data is available when the Quartz job triggers.
         */
        JobDetail jobDetail = GroupResourceConfigurationUpdateJob.getJobDetail(group, subject, jobDataMap);
        Trigger trigger = QuartzUtil.getFireOnceOffsetTrigger(jobDetail, 10000);
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            String message = "Error scheduling job named '" + jobDetail.getName() + "':";
            LOG.error(message, e);
            throw new ResourceGroupUpdateException(message + e.getMessage());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scheduled Resource configuration update against compatible ResourceGroup[id="
                + compatibleGroupId + "].");
        }

        return updateId;
    }

    private ResourceGroup getCompatibleGroupIfAuthorized(Subject subject, int compatibleGroupId) {
        ResourceGroup group;

        try {
            // resourceGroupManager will test for necessary permissions too
            group = resourceGroupManager.getResourceGroupById(subject, compatibleGroupId, GroupCategory.COMPATIBLE);
        } catch (ResourceGroupNotFoundException e) {
            throw new RuntimeException("Cannot get support operations for unknown group [" + compatibleGroupId + "]: "
                + e, e);
        }

        return group;
    }

    private void ensureModifyPermission(Subject subject, Resource resource) throws PermissionException {
        if (!authorizationManager.hasResourcePermission(subject, Permission.MODIFY_RESOURCE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission "
                + "to modify plugin configuration for resource [" + resource + "]");
        }
    }

    private void ensureModifyResourcePermission(Subject subject, ResourceGroup group) throws PermissionException {
        if (!authorizationManager.hasGroupPermission(subject, Permission.MODIFY_RESOURCE, group.getId())) {
            throw new PermissionException("User [" + subject.getName() + "] does not have permission "
                + "to modify plugin configuration for members of group [" + group + "]");
        }
    }

    /**
     * @deprecated use {@link #findGroupPluginConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.GroupPluginConfigurationUpdateCriteria)}
     */
    @Override
    public GroupPluginConfigurationUpdate getGroupPluginConfigurationById(int configurationUpdateId) {
        GroupPluginConfigurationUpdate update = entityManager.find(GroupPluginConfigurationUpdate.class,
            configurationUpdateId);
        return update;
    }

    /**
     * @deprecated use {@link #findGroupResourceConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria)}
     */
    @Override
    public GroupResourceConfigurationUpdate getGroupResourceConfigurationById(int configurationUpdateId) {
        GroupResourceConfigurationUpdate update = entityManager.find(GroupResourceConfigurationUpdate.class,
            configurationUpdateId);
        return update;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageList<ConfigurationUpdateComposite> findPluginConfigurationUpdateCompositesByParentId(
        int configurationUpdateId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", configurationUpdateId);

        long count = getPluginConfigurationUpdateCountByParentId(configurationUpdateId);

        List<ConfigurationUpdateComposite> results = query.getResultList();

        return new PageList<ConfigurationUpdateComposite>(results, (int) count, pageControl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageList<ConfigurationUpdateComposite> findResourceConfigurationUpdateCompositesByParentId(Subject subject,
        int configurationUpdateId, PageControl pageControl) {

        // will perform CONFIGURE_READ security check for us, no need to save the

        getGroupResourceConfigurationUpdate(subject, configurationUpdateId);

        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", configurationUpdateId);

        long count = getResourceConfigurationUpdateCountByParentId(configurationUpdateId);

        List<ConfigurationUpdateComposite> results = query.getResultList();

        return new PageList<ConfigurationUpdateComposite>(results, (int) count, pageControl);
    }

    /**
     * @deprecated use {@link #findPluginConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria)}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<Integer> findPluginConfigurationUpdatesByParentId(int configurationUpdateId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", configurationUpdateId);

        long count = getPluginConfigurationUpdateCountByParentId(configurationUpdateId);

        List<Integer> results = query.getResultList();

        return new PageList<Integer>(results, (int) count, pageControl);
    }

    @Override
    public long getPluginConfigurationUpdateCountByParentId(int configurationUpdateId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            PluginConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID);
        countQuery.setParameter("groupConfigurationUpdateId", configurationUpdateId);
        return (Long) countQuery.getSingleResult();
    }

    /**
     * @deprecated use {@link #findResourceConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria)}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<Integer> findResourceConfigurationUpdatesByParentId(int groupConfigurationUpdateId,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("cu.modifiedTime");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, pageControl);
        query.setParameter("groupConfigurationUpdateId", groupConfigurationUpdateId);

        long count = getResourceConfigurationUpdateCountByParentId(groupConfigurationUpdateId);

        List<Integer> results = query.getResultList();

        return new PageList<Integer>(results, (int) count, pageControl);
    }

    @Override
    public long getResourceConfigurationUpdateCountByParentId(int groupConfigurationUpdateId) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID);
        countQuery.setParameter("groupConfigurationUpdateId", groupConfigurationUpdateId);
        return (Long) countQuery.getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Configuration> getResourceConfigurationMapForGroupUpdate(Subject subject,
        Integer groupResourceConfigurationUpdateId) {
        // this method will perform the CONFIGURE_READ security check for us, no need to keep reference to result
        GroupResourceConfigurationUpdate groupResourceConfigurationUpdate = getGroupResourceConfigurationUpdate(
            subject, groupResourceConfigurationUpdateId);

        Tuple<String, Object> groupIdParameter = new Tuple<String, Object>("groupConfigurationUpdateId",
            groupResourceConfigurationUpdateId);
        Map<Integer, Configuration> results = executeGetConfigurationMapQuery(
            Configuration.QUERY_GET_RESOURCE_CONFIG_MAP_BY_GROUP_UPDATE_ID, 100, groupIdParameter);

        // Mask the configurations before returning them.
        for (Configuration configuration : results.values()) {
            configuration.getMap().size();
        }
        ConfigurationDefinition configurationDefinition = getResourceConfigurationDefinitionForResourceType(
            subjectManager.getOverlord(), groupResourceConfigurationUpdate.getGroup().getResourceType().getId());
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        entityManager.clear();
        for (Configuration configuration : results.values()) {
            ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
        }

        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Configuration> getPluginConfigurationMapForGroupUpdate(Subject subject,
        Integer groupPluginConfigurationUpdateId) {
        // this method will perform the CONFIGURE_READ security check for us, no need to keep reference to result
        GroupPluginConfigurationUpdate groupPluginConfigurationUpdate = getGroupPluginConfigurationUpdate(subject,
            groupPluginConfigurationUpdateId);

        Tuple<String, Object> groupIdParameter = new Tuple<String, Object>("groupConfigurationUpdateId",
            groupPluginConfigurationUpdateId);
        Map<Integer, Configuration> results = executeGetConfigurationMapQuery(
            Configuration.QUERY_GET_PLUGIN_CONFIG_MAP_BY_GROUP_UPDATE_ID, 100, groupIdParameter);

        // Mask the configurations before returning them.
        for (Configuration configuration : results.values()) {
            configuration.getMap().size();
        }
        ConfigurationDefinition configurationDefinition = getPluginConfigurationDefinitionForResourceType(
            subjectManager.getOverlord(), groupPluginConfigurationUpdate.getGroup().getResourceType().getId());
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        entityManager.clear();
        for (Configuration configuration : results.values()) {
            ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Configuration> executeGetConfigurationMapQuery(String memberQueryName, int maxSize,
        Tuple<String, Object>... parameters) {
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, memberQueryName);
        Query query = entityManager.createNamedQuery(memberQueryName);

        for (Tuple<String, Object> param : parameters) {
            countQuery.setParameter(param.lefty, param.righty);
            query.setParameter(param.lefty, param.righty);
        }

        PersistenceUtility.setDataPage(query, new PageControl(0, maxSize)); // limit the results

        long count = (Long) countQuery.getSingleResult();
        int resultsSize;
        if (count > maxSize) {
            LOG.error("Configuration set contains more than " + maxSize + " members - " + "returning only " + maxSize
                    + " Configurations (the maximum allowed).");
            resultsSize = maxSize;
        } else {
            resultsSize = (int) count;
        }

        // initialize the map to be 150% more than the results, so that the fill factor only reaches 66%
        Map<Integer, Configuration> results = new HashMap<Integer, Configuration>((int) (resultsSize * 1.5));
        List<Object[]> pagedResults = query.getResultList();
        for (Object[] result : pagedResults) {
            Integer resourceId = (Integer) result[0];
            Configuration configuration = (Configuration) result[1];
            results.put(resourceId, configuration);
        }
        return results;
    }

    /**
     * @deprecated use {@link #findGroupPluginConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.GroupPluginConfigurationUpdateCriteria)}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<GroupPluginConfigurationUpdate> findGroupPluginConfigurationUpdates(int groupId, PageControl pc) {
        pc.initDefaultOrderingField("modifiedTime", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            GroupPluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            GroupPluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID);
        query.setParameter("groupId", groupId);
        countQuery.setParameter("groupId", groupId);

        long count = (Long) countQuery.getSingleResult();

        List<GroupPluginConfigurationUpdate> results = query.getResultList();

        return new PageList<GroupPluginConfigurationUpdate>(results, (int) count, pc);
    }

    /**
     * @deprecated use {@link #findGroupResourceConfigurationUpdatesByCriteria(org.rhq.core.domain.auth.Subject, org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria)}
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<GroupResourceConfigurationUpdate> findGroupResourceConfigurationUpdates(Subject subject,
        int groupId, PageControl pc) {
        if (authorizationManager.hasGroupPermission(subject, Permission.CONFIGURE_READ, groupId) == false) {
            throw new PermissionException("User[name=" + subject.getName()
                + "] does not have permission to view configuration for group[id=" + groupId + "]");
        }

        pc.initDefaultOrderingField("modifiedTime", PageOrdering.DESC);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            GroupResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager,
            GroupResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID);
        query.setParameter("groupId", groupId);
        countQuery.setParameter("groupId", groupId);

        long count = (Long) countQuery.getSingleResult();

        List<GroupResourceConfigurationUpdate> results = query.getResultList();

        return new PageList<GroupResourceConfigurationUpdate>(results, (int) count, pc);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationUpdateStatus updateGroupResourceConfigurationUpdateStatus(
        int groupResourceConfigurationUpdateId, String errorMessages) {
        GroupResourceConfigurationUpdate groupResourceConfigUpdate = configurationManager
            .getGroupResourceConfigurationById(groupResourceConfigurationUpdateId);

        // NOTE: None of the individual updates should still be INPROGRESS at the time this method is called!
        Query query = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_FIND_STATUS_BY_PARENT_UPDATE_ID);
        query.setParameter("groupConfigurationUpdateId", groupResourceConfigUpdate.getId());
        List<ConfigurationUpdateStatus> updateStatusTuples = query.getResultList();

        return completeGroupConfigurationUpdate(groupResourceConfigUpdate, updateStatusTuples, errorMessages);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationUpdateStatus updateGroupPluginConfigurationUpdateStatus(int groupPluginConfigurationUpdateId,
        String errorMessages) {
        GroupPluginConfigurationUpdate groupPluginConfigUpdate = configurationManager
            .getGroupPluginConfigurationById(groupPluginConfigurationUpdateId);

        // NOTE: None of the individual updates should still be INPROGRESS at the time this method is called!
        Query query = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_FIND_STATUS_BY_PARENT_UPDATE_ID);
        query.setParameter("groupConfigurationUpdateId", groupPluginConfigUpdate.getId());
        List<ConfigurationUpdateStatus> updateStatusTuples = query.getResultList();

        return completeGroupConfigurationUpdate(groupPluginConfigUpdate, updateStatusTuples, errorMessages);
    }

    private ConfigurationUpdateStatus completeGroupConfigurationUpdate(
        AbstractGroupConfigurationUpdate groupConfigUpdate,
        List<ConfigurationUpdateStatus> memberConfigUpdateStatusTuples, String errorMessages) {
        ConfigurationUpdateStatus groupConfigUpdateStatus;
        if (memberConfigUpdateStatusTuples.contains(ConfigurationUpdateStatus.FAILURE) || errorMessages != null) {
            groupConfigUpdateStatus = ConfigurationUpdateStatus.FAILURE;
        } else {
            groupConfigUpdateStatus = ConfigurationUpdateStatus.SUCCESS;
        }
        groupConfigUpdate.setStatus(groupConfigUpdateStatus);
        groupConfigUpdate.setErrorMessage(errorMessages);
        configurationManager.updateGroupConfigurationUpdate(groupConfigUpdate);

        return groupConfigUpdateStatus; // if the caller wants to know what the new status was
    }

    @Override
    public int deleteGroupPluginConfigurationUpdates(Subject subject, Integer resourceGroupId,
        Integer[] groupPluginConfigurationUpdateIds) {

        // TODO: use subject and resourceGroupId to perform security check

        if (authorizationManager.hasGroupPermission(subject, Permission.MODIFY_RESOURCE, resourceGroupId) == false) {
            LOG.error(subject + " attempted to delete " + groupPluginConfigurationUpdateIds.length
                    + " group resource configuration updates for ResourceGroup[id" + resourceGroupId
                    + "], but did not have the " + Permission.MODIFY_RESOURCE.name() + " permission for this group");
            return 0;
        }

        int removed = 0;
        for (Integer apcuId : groupPluginConfigurationUpdateIds) {
            /*
             * use this strategy instead of GroupPluginConfigurationUpdate.QUERY_DELETE_BY_ID because removing via
             * the entityManager will respect cascading rules, using a JPQL DELETE statement will not
             */
            try {
                // break the plugin configuration update links in order to preserve individual change history
                Query q = entityManager.createNamedQuery(PluginConfigurationUpdate.QUERY_DELETE_GROUP_UPDATE);
                q.setParameter("apcuId", apcuId);
                q.executeUpdate();

                GroupPluginConfigurationUpdate update = getGroupPluginConfigurationById(apcuId);
                entityManager.remove(update);
                removed++;
            } catch (Exception e) {
                LOG.error("Problem removing group plugin configuration update", e);
            }
        }

        return removed;
    }

    @Override
    public int deleteGroupResourceConfigurationUpdates(Subject subject, Integer resourceGroupId,
        Integer[] groupResourceConfigurationUpdateIds) {

        if (authorizationManager.hasGroupPermission(subject, Permission.MODIFY_RESOURCE, resourceGroupId) == false) {
            LOG.error(subject + " attempted to delete " + groupResourceConfigurationUpdateIds.length
                    + " group resource configuration updates for ResourceGroup[id" + resourceGroupId
                    + "], but did not have the " + Permission.MODIFY_RESOURCE.name() + " permission for this group");
            return 0;
        }

        int removed = 0;
        for (Integer arcuId : groupResourceConfigurationUpdateIds) {
            /*
             * use this strategy instead of GroupResourceConfigurationUpdate.QUERY_DELETE_BY_ID because removing via
             * the entityManager will respect cascading rules, using a JPQL DELETE statement will not
             */
            try {
                // break the resource configuration update links in order to preserve individual change history
                Query q = entityManager.createNamedQuery(ResourceConfigurationUpdate.QUERY_DELETE_GROUP_UPDATE);
                q.setParameter("arcuId", arcuId);
                q.executeUpdate();

                GroupResourceConfigurationUpdate update = getGroupResourceConfigurationById(arcuId);
                entityManager.remove(update);
                removed++;
            } catch (Exception e) {
                LOG.error("Problem removing group resource configuration update", e);
            }
        }

        return removed;
    }

    @Override
    public void updateGroupConfigurationUpdate(AbstractGroupConfigurationUpdate groupUpdate) {
        // TODO jmarques: if (errorMessages != null) set any remaining INPROGRESS children to FAILURE
        entityManager.merge(groupUpdate);
    }

    @Override
    public void deleteConfigurations(List<Integer> configurationIds) {
        if (configurationIds == null || configurationIds.size() == 0) {
            return;
        }

        boolean supportsCascade = DatabaseTypeFactory.getDefaultDatabaseType().supportsSelfReferringCascade();
        if (supportsCascade == false) {
            Query breakPropertyRecursionQuery = entityManager
                .createNamedQuery(Configuration.QUERY_BREAK_PROPERTY_RECURSION_BY_CONFIGURATION_IDS);
            breakPropertyRecursionQuery.setParameter("configurationIds", configurationIds);
            breakPropertyRecursionQuery.executeUpdate();
        }

        Query rawConfigurationsQuery = entityManager
            .createNamedQuery(Configuration.QUERY_DELETE_RAW_CONFIGURATIONS_CONFIGURATION_IDS);
        Query configurationsQuery = entityManager
            .createNamedQuery(Configuration.QUERY_DELETE_CONFIGURATIONS_BY_CONFIGURATION_IDs);

        rawConfigurationsQuery.setParameter("configurationIds", configurationIds);
        configurationsQuery.setParameter("configurationIds", configurationIds);

        rawConfigurationsQuery.executeUpdate();
        configurationsQuery.executeUpdate(); // uses DB-level cascades to delete properties
    }

    @Override
    public void deleteProperties(int[] propertyIds) {
        if (propertyIds == null || propertyIds.length == 0) {
            return;
        }

        Query propertiesQuery = entityManager.createNamedQuery(Property.QUERY_DELETE_BY_PROPERTY_IDS);
        propertiesQuery.setParameter("propertyIds", ArrayUtils.wrapInList(propertyIds));
        propertiesQuery.executeUpdate();
    }

    @Override
    public GroupPluginConfigurationUpdate getGroupPluginConfigurationUpdate(Subject subject, int configurationUpdateId) {
        GroupPluginConfigurationUpdate update = getGroupPluginConfigurationById(configurationUpdateId);

        int groupId = update.getGroup().getId();

        if (!authorizationManager.canViewGroup(subject, groupId)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view group Resource configuration for [" + update.getGroup() + "]");

        }

        return update;
    }

    @Override
    public GroupResourceConfigurationUpdate getGroupResourceConfigurationUpdate(Subject subject,
        int configurationUpdateId) {
        GroupResourceConfigurationUpdate update = getGroupResourceConfigurationById(configurationUpdateId);

        int groupId = update.getGroup().getId();
        if (authorizationManager.hasGroupPermission(subject, Permission.CONFIGURE_READ, groupId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to view group resourceConfiguration[id=" + configurationUpdateId + "]");
        }

        return update;
    }

    @Override
    public Configuration translateResourceConfiguration(Subject subject, int resourceId, Configuration configuration,
        boolean fromStructured) {

        if (!isStructuredAndRawSupported(resourceId)) {
            throw new TranslationNotSupportedException("The translation operation is only supported for "
                + "configurations that support both structured and raw.");
        }

        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            throw new NoResultException("Cannot get live configuration for unknown resource [" + resourceId + "]");
        }

        if (!authorizationManager.hasResourcePermission(subject, Permission.CONFIGURE_READ, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to view resource configuration for [" + resource + "]");
        }

        try {
            Agent agent = resource.getAgent();
            AgentClient agentClient = this.agentManager.getAgentClient(agent);
            ConfigurationAgentService configService = agentClient.getConfigurationAgentService();

            return configService.merge(configuration, resourceId, fromStructured);
        } catch (PluginContainerException e) {
            LOG.error("An error occurred while trying to translate the configuration.", e);
            return null;
        }
    }

    @Override
    public Configuration mergeConfiguration(Configuration config) {
        Configuration out = entityManager.merge(config);
        return out;
    }

    @Override
    public PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdatesByCriteria(Subject subject,
        ResourceConfigurationUpdateCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "resource", subject.getId());
        }

        CriteriaQueryRunner<ResourceConfigurationUpdate> queryRunner = new CriteriaQueryRunner<ResourceConfigurationUpdate>(
            criteria, generator, entityManager);

        PageList<ResourceConfigurationUpdate> updates = queryRunner.execute();

        // If configurations were fetched, mask them before returning the updates.
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        Set<String> fetchFields = new HashSet<String>(generator.getFetchFields(criteria));
        if (fetchFields.contains(AbstractConfigurationUpdateCriteria.FETCH_FIELD_CONFIGURATION)) {
            for (ResourceConfigurationUpdate update : updates) {
                Configuration configuration = update.getConfiguration();
                configuration.getMap().size();
            }
            entityManager.clear();
            for (ResourceConfigurationUpdate update : updates) {
                Configuration configuration = update.getConfiguration();
                ConfigurationDefinition configurationDefinition = getResourceConfigurationDefinitionForResourceType(
                    subjectManager.getOverlord(), update.getResource().getResourceType().getId());
                ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
            }
        }

        return updates;
    }

    @Override
    public PageList<PluginConfigurationUpdate> findPluginConfigurationUpdatesByCriteria(Subject subject,
        PluginConfigurationUpdateCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "resource", subject.getId());
        }

        CriteriaQueryRunner<PluginConfigurationUpdate> queryRunner = new CriteriaQueryRunner<PluginConfigurationUpdate>(
            criteria, generator, entityManager);

        PageList<PluginConfigurationUpdate> updates = queryRunner.execute();

        // If configurations were fetched, mask them before returning the updates.
        // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
        Set<String> fetchFields = new HashSet<String>(generator.getFetchFields(criteria));
        if (fetchFields.contains(AbstractConfigurationUpdateCriteria.FETCH_FIELD_CONFIGURATION)) {
            for (PluginConfigurationUpdate update : updates) {
                Configuration configuration = update.getConfiguration();
                configuration.getMap().size();
            }
            entityManager.clear();
            for (PluginConfigurationUpdate update : updates) {
                Configuration configuration = update.getConfiguration();
                ConfigurationDefinition configurationDefinition = getPluginConfigurationDefinitionForResourceType(
                    subjectManager.getOverlord(), update.getResource().getResourceType().getId());
                ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
            }
        }

        return updates;
    }

    @Override
    public PageList<GroupResourceConfigurationUpdate> findGroupResourceConfigurationUpdatesByCriteria(Subject subject,
        GroupResourceConfigurationUpdateCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.GROUP, "group",
                subject.getId());
        }

        CriteriaQueryRunner<GroupResourceConfigurationUpdate> queryRunner = new CriteriaQueryRunner<GroupResourceConfigurationUpdate>(
            criteria, generator, entityManager);

        PageList<GroupResourceConfigurationUpdate> updates = queryRunner.execute();

        /*Set<String> fetchFields = new HashSet<String>(generator.getFetchFields(criteria));
        if (fetchFields.contains(AbstractGroupConfigurationUpdateCriteria.FETCH_FIELD_CONFIGURATION_UPDATES)) {
            // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
            entityManager.clear();
            for (GroupResourceConfigurationUpdate update : updates) {
                List<ResourceConfigurationUpdate> memberUpdates = update.getConfigurationUpdates();
                // Mask the configurations before returning the updates.
                for (ResourceConfigurationUpdate memberUpdate : memberUpdates) {
                    Configuration configuration = memberUpdate.getConfiguration();
                    ConfigurationDefinition configurationDefinition = getResourceConfigurationDefinitionForResourceType(
                        subjectManager.getOverlord(), memberUpdate.getResource().getResourceType().getId());
                    ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
                }
            }
        }*/

        return updates;
    }

    @Override
    public PageList<GroupPluginConfigurationUpdate> findGroupPluginConfigurationUpdatesByCriteria(Subject subject,
        GroupPluginConfigurationUpdateCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.GROUP, "group",
                subject.getId());
        }

        CriteriaQueryRunner<GroupPluginConfigurationUpdate> queryRunner = new CriteriaQueryRunner<GroupPluginConfigurationUpdate>(
            criteria, generator, entityManager);

        PageList<GroupPluginConfigurationUpdate> updates = queryRunner.execute();

        /*Set<String> fetchFields = new HashSet<String>(generator.getFetchFields(criteria));
        if (fetchFields.contains(AbstractGroupConfigurationUpdateCriteria.FETCH_FIELD_CONFIGURATION_UPDATES)) {
            // We do not want the masked configurations persisted, so detach all entities before masking the configurations.
            entityManager.clear();
            for (GroupPluginConfigurationUpdate update : updates) {
                List<PluginConfigurationUpdate> memberUpdates = update.getConfigurationUpdates();
                // Mask the configurations before returning the updates.
                for (PluginConfigurationUpdate memberUpdate : memberUpdates) {
                    Configuration configuration = memberUpdate.getConfiguration();
                    ConfigurationDefinition configurationDefinition = getPluginConfigurationDefinitionForResourceType(
                        subjectManager.getOverlord(), memberUpdate.getResource().getResourceType().getId());
                    ConfigurationMaskingUtility.maskConfiguration(configuration, configurationDefinition);
                }
            }
        }*/

        return updates;
    }

    @Override
    public ConfigurationDefinition getOptionsForConfigurationDefinition(Subject subject, int resourceId,
        int parentResourceId, ConfigurationDefinition def) {
        
        Resource resource = null, baseResource = null;
        if (resourceId >= 0) {
            resource = resourceManager.getResource(subject, resourceId);
        }
        if (parentResourceId >= 0) {
            Resource parentResource = resourceManager.getResource(subject, parentResourceId);
            baseResource = ResourceUtility.getBaseServerOrService(parentResource);
        } else if (resource != null) {
            baseResource = ResourceUtility.getBaseServerOrService(resource);
        }

        for (Map.Entry<String, PropertyDefinition> entry : def.getPropertyDefinitions().entrySet()) {
            PropertyDefinition pd = entry.getValue();

            if (pd instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;
                handlePDS(subject, resource, baseResource, pds);

            } else if (pd instanceof PropertyDefinitionList) {
                PropertyDefinitionList pdl = (PropertyDefinitionList) pd;
                PropertyDefinition memberDef = pdl.getMemberDefinition();
                if (memberDef instanceof PropertyDefinitionSimple) {
                    PropertyDefinitionSimple pds = (PropertyDefinitionSimple) memberDef;
                    handlePDS(subject, resource, baseResource, pds);
                } else if (memberDef instanceof PropertyDefinitionMap) {
                    PropertyDefinitionMap pdm = (PropertyDefinitionMap) memberDef;
                    for (PropertyDefinition inner : pdm.getOrderedPropertyDefinitions()) {
                        if (inner instanceof PropertyDefinitionSimple) {
                            handlePDS(subject, resource, baseResource, (PropertyDefinitionSimple) inner);
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("3 ____[ " + inner.toString() + " in " + pdl.toString()
                                + " ]____ not yet supported");
                        }
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("2 ____[ " + memberDef.toString() + " in " + pdl.toString()
                            + " ]____ not yet supported");
                    }
                }

            } else if (pd instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap pdm = (PropertyDefinitionMap) pd;
                for (PropertyDefinition inner : pdm.getOrderedPropertyDefinitions()) {
                    if (inner instanceof PropertyDefinitionSimple) {
                        handlePDS(subject, resource, baseResource, (PropertyDefinitionSimple) inner);
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("4 ____[ " + inner.toString() + " in " + pdm.toString()
                                + " ]____ not yet supported");
                        }
                    }
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("1 ____[ " + pd.toString() + " ]____ not yet supported");
                }
            }
        }

        return def; // TODO clone the incoming definition?
    }

    /**
     * Determine the dynamic enumeration values for one PropertyDefinitionSimple
     * @param subject Subject of the caller - may limit search results
     * @param pds the PropertyDefinitionSimple to work on
     */
    private void handlePDS(final Subject subject, Resource resource, Resource baseResource, PropertyDefinitionSimple pds) {

        if (pds.getOptionsSource() != null) {
            // evaluate the source parameters
            PropertyOptionsSource pos = pds.getOptionsSource();
            PropertyOptionsSource.TargetType tt = pos.getTargetType();
            String expression = pos.getExpression();
            PropertyOptionsSource.ExpressionScope expressionScope = pos.getExpressionScope();
            String filter = pos.getFilter();
            Pattern filterPattern = null;
            if (filter != null)
                filterPattern = Pattern.compile(filter);

            if (tt == PropertyOptionsSource.TargetType.RESOURCE || tt == PropertyOptionsSource.TargetType.CONFIGURATION) {
                ResourceCriteria criteria = new ResourceCriteria();

                //Use CriteriaQuery to automatically chunk/page through criteria query results
                CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                    @Override
                    public PageList<Resource> execute(ResourceCriteria criteria) {
                        return resourceManager.findResourcesByCriteria(subject, criteria);
                    }
                };

                Iterable<Resource> foundResources = null;
                if (tt == PropertyOptionsSource.TargetType.CONFIGURATION) {
                    // split out expression part for target=configuration
                    // return if no property specifier is given
                    String expr = expression;
                    if (expr.contains(":")) {
                        expr = expr.substring(expr.indexOf(':') + 1);

                        if (!"self".equals(expr)) {
                            criteria.setSearchExpression(expr);
                            foundResources = new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);
                            if (expressionScope == PropertyOptionsSource.ExpressionScope.BASE_RESOURCE && baseResource != null) {
                                foundResources = Iterables.filter(foundResources, new IsInBaseResourcePredicate(
                                        baseResource));
                            }
                        } else if (resource != null) {
                            ArrayList<Resource> resourceList = new ArrayList<Resource>();
                            resourceList.add(resource);
                            foundResources = resourceList;
                        } else {
                            LOG.warn("Self reference requested but resource id is not valid."
                                    + "Option source expression:" + expression);
                            return;
                        }
                    } else {
                        LOG.warn("Option source expression for property " + pds.getName()
                                + " and target configuration contains no ':'");
                        return;
                    }
                } else {
                    criteria.setSearchExpression(expression);
                    foundResources = new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);
                    if (expressionScope == PropertyOptionsSource.ExpressionScope.BASE_RESOURCE && baseResource != null) {
                        foundResources = Iterables.filter(foundResources, new IsInBaseResourcePredicate(
                            baseResource));
                    }
                }
                
                for (Resource foundResource : foundResources) {
                    processPropertyOptionsSource(resource, baseResource, pds, tt, expression, filterPattern, foundResource);
                }
            } else if (tt == PropertyOptionsSource.TargetType.GROUP) {
                // spinder 2-15-13: commenting out this code below as we don't appear to be using any of it. Half done.                
                //                // for groups we need to talk to the group manager
                //                ResourceGroupCriteria criteria = new ResourceGroupCriteria();
                //                criteria.setSearchExpression(expression);
                //
                //                resourceGroupManager.findResourceGroupCompositesByCriteria(subject, criteria);
            }
            // TODO plugin and resourceType
        }

    }

    private void processPropertyOptionsSource(Resource resource, Resource baseResource, PropertyDefinitionSimple pds,
        PropertyOptionsSource.TargetType tt, String expression, Pattern filterPattern, Resource foundResource) {
        if (tt == PropertyOptionsSource.TargetType.RESOURCE) {
            String name = foundResource.getName();

            // filter if the user provided a filter
            if (filterPattern != null) {
                Matcher m = filterPattern.matcher(name);
                if (m.matches()) {
                    PropertyDefinitionEnumeration pde = new PropertyDefinitionEnumeration(name, "" + name);
                    pds.getEnumeratedValues().add(pde);
                }
            } else { // Filter is null -> none provided -> do not filter
                PropertyDefinitionEnumeration pde = new PropertyDefinitionEnumeration(name, "" + name);
                pds.getEnumeratedValues().add(pde);
            }
        } else if (tt == PropertyOptionsSource.TargetType.CONFIGURATION) {
            //  for configuration we need to drill down into the resource configuration
            if (!handleConfigurationTarget(resource, baseResource, pds, expression, foundResource))
                return;

        }
    }

    /**
     * Drill down in the case the user set up a target of "configuration". We need to check
     * that the target property actually exists and that it has a format we understand
     *
     * @param resource the resource we are looking options for
     * @param baseResource the base resource of <code>resource</code>
     * @param pds Property definition to examine
     * @param expression The whole expression starting with identifier: for the configuration
     * identifier. This looks like <i>listname</i> for list of
     * property simple or <i>mapname=mapkey</i> for a map with simple properties
     * @param foundResource the resource to look at
     * @return false if the property can not be resolved, true otherwise
     */
    private boolean handleConfigurationTarget(Resource resource, Resource baseResource, PropertyDefinitionSimple pds,
        String expression, Resource foundResource) {
        Configuration configuration = foundResource.getResourceConfiguration();

        if (expression.indexOf(":") != -1) {
            expression = expression.substring(0, expression.indexOf(":"));
        }

        boolean isMapOrList = expression.contains("=");
        Property p;
        if (isMapOrList) {
            String mapPropLocation = expression.substring(0, expression.indexOf("="));
            if (mapPropLocation.contains("/")) {
                mapPropLocation = mapPropLocation.substring(0, mapPropLocation.indexOf('/'));
            }
            p = configuration.get(mapPropLocation);
        } else {
            p = configuration.get(expression);
        }

        if (p == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(resource + " in " + baseResource + ": option source expression for property " + pds.getName()
                    + " and target configuration of " + foundResource + " not found");
            }
            return false;
        }
        if (!(p instanceof PropertyList)) {
            LOG.warn(resource + " in " + baseResource + ": option source expression for property " + pds.getName()
                + " and target configuration does not point to a list");
            return false;
        }
        PropertyList pl = (PropertyList) p;
        List<Property> propertyList = pl.getList();
        if (propertyList.size() == 0)
            return false;

        // Now List of simple or list of maps (of simple) ?

        if (propertyList.get(0) instanceof PropertySimple) {
            if (isMapOrList) {
                LOG.warn(resource + " in " + baseResource + ": expected a List of Maps, but got a list of simple");
                return false;
            }

            for (Property tmp : propertyList) {
                PropertySimple ps = (PropertySimple) tmp;
                String name = ps.getStringValue();
                if (name != null) {
                    PropertyDefinitionEnumeration pde = new PropertyDefinitionEnumeration(name, name);
                    pds.getEnumeratedValues().add(pde);
                }
            }
        } else if (propertyList.get(0) instanceof PropertyMap) {
            if (!isMapOrList) {
                LOG.warn(resource + " in " + baseResource + ": expected a List of simple, but got a list of Maps");
                return false;
            }
            String subPropName;
            subPropName = expression.substring(expression.indexOf("=") + 1);

            for (Property tmp : propertyList) {
                PropertyMap pm = (PropertyMap) tmp;
                Property ps = pm.get(subPropName);
                if (ps == null) {
                    LOG.warn(resource + " in " + baseResource + ": option source expression for property "
                        + pds.getName() + " and target configuration does not have a map element " + subPropName);
                    return false;
                }
                if (!(ps instanceof PropertySimple)) {
                    LOG.warn(resource + " in " + baseResource + ": ListOfMapOf!Simple are not supported");
                    return false;
                }
                PropertySimple propertySimple = (PropertySimple) ps;
                String name = propertySimple.getStringValue();
                if (name != null) {
                    PropertyDefinitionEnumeration pde = new PropertyDefinitionEnumeration(name, name);
                    pds.getEnumeratedValues().add(pde);
                }
            }
        }

        return true;
    }

    private static final class IsInBaseResourcePredicate implements Predicate<Resource> {

        private Resource baseResource;

        private IsInBaseResourcePredicate(Resource baseResource) {
            this.baseResource = baseResource;
        }

        @Override
        public boolean apply(Resource resource) {
            Resource baseServerOrService = ResourceUtility.getBaseServerOrService(resource);
            return baseResource.equals(baseServerOrService);
        }
    }
}
