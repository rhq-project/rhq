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
package org.rhq.enterprise.server.resource;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.remoting.CannotConnectException;

import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.CannotConnectToAgentException;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceStatus;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.content.ContentManagerHelper;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * Bean to handle interaction with the resource factory subsystem of the plugin container. !! Warning, the factory
 * interface is there to remove managed things from disk. Use caution when using this and don't confuse it with removing
 * something from the system inventory. !!
 *
 * @author Jason Dobies
 */
@Stateless
public class ResourceFactoryManagerBean implements ResourceFactoryManagerLocal, ResourceFactoryManagerRemote {
    private static final Log LOG = LogFactory.getLog(ResourceFactoryManagerBean.class);

    // Constants  --------------------------------------------

    /**
     * Amount of time a request may be outstanding against an agent before it is marked as timed out.
     */
    private static final int REQUEST_TIMEOUT = 1000 * 60 * 60;

    // Attributes  --------------------------------------------

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private ResourceFactoryManagerLocal resourceFactoryManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private ContentManagerLocal contentManager;

    // ResourceFactoryManagerLocal Implementation  --------------------------------------------

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void completeCreateResource(CreateResourceResponse response) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received call to complete create resource: " + response);
        }

        // Load the persisted history entry
        CreateResourceHistory history = entityManager.find(CreateResourceHistory.class, response.getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (history == null) {
            LOG.error("Attempting to complete a request that was not found in the database: " + response.getRequestId());
            return;
        }

        // Update the history entry
        history.setNewResourceKey(response.getResourceKey());
        history.setErrorMessage(response.getErrorMessage());
        history.setStatus(response.getStatus());

        // The configuration may now have error messages in it, so merge with the persisted one
        if (response.getResourceConfiguration() != null) {
            entityManager.merge(response.getResourceConfiguration());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void completeDeleteResourceRequest(DeleteResourceResponse response) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received call to complete delete resource: " + response);
        }

        // Load the persisted history entry
        DeleteResourceHistory history = entityManager.find(DeleteResourceHistory.class, response.getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (history == null) {
            LOG.error("Attempting to complete a request that was not found in the database: " + response.getRequestId());
            return;
        }

        // Update the history entry
        history.setErrorMessage(response.getErrorMessage());
        history.setStatus(response.getStatus());

        // If successful mark resource as deleted and uninventory children
        if (response.getStatus() == DeleteResourceStatus.SUCCESS) {
            Resource resource = history.getResource();

            // get doomed children
            Set<Resource> children = resource.getChildResources();

            // set the resource deleted and update the db in case it matters to the child operations
            resource.setInventoryStatus(InventoryStatus.DELETED);
            //resource.setParentResource(null); can't null this out since the query DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID needs it
            resource.setItime(System.currentTimeMillis());
            entityManager.merge(resource);

            // uninventory the children of the deleted resource (see rhq-2378)
            uninventoryChildren(children);
        }
    }

    private void uninventoryChildren(Set<Resource> children) {
        for (Resource child : children) {
            resourceManager.uninventoryResourceInNewTransaction(child.getId());
        }
    }

    @SuppressWarnings("unchecked")
    public void checkForTimedOutRequests() {
        try {
            Query query;

            // Create Requests
            query = entityManager.createNamedQuery(CreateResourceHistory.QUERY_FIND_WITH_STATUS);
            query.setParameter("status", CreateResourceStatus.IN_PROGRESS);
            List<CreateResourceHistory> createHistories = query.getResultList();

            if (createHistories == null) {
                return;
            }

            for (CreateResourceHistory request : createHistories) {
                long duration = request.getDuration();

                // If the duration exceeds the timeout threshold, mark it as timed out
                if (duration > REQUEST_TIMEOUT) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Timing out request after duration: " + duration + " Request: " + request);
                    }
                    request.setErrorMessage("Request with duration " + duration + " exceeded the timeout threshold of "
                        + REQUEST_TIMEOUT);
                    request.setStatus(CreateResourceStatus.TIMED_OUT);
                }
            }

            // Delete Requests
            query = entityManager.createNamedQuery(CreateResourceHistory.QUERY_FIND_WITH_STATUS);
            query.setParameter("status", CreateResourceStatus.IN_PROGRESS);
            List<DeleteResourceHistory> deleteHistories = query.getResultList();

            if (deleteHistories == null) {
                return;
            }

            for (DeleteResourceHistory request : deleteHistories) {
                long duration = request.getDuration();

                // If the duration exceeds the timeout threshold, mark it as timed out
                if (duration > REQUEST_TIMEOUT) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Timing out request after duration: " + duration + " Request: " + request);
                    }
                    request.setErrorMessage("Request with duration " + duration + " exceeded the timeout threshold of "
                        + REQUEST_TIMEOUT);
                    request.setStatus(DeleteResourceStatus.TIMED_OUT);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error while processing timed out requests", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CreateResourceHistory persistCreateHistory(Subject user, int parentResourceId, int resourceTypeId,
        String createResourceName, Configuration configuration) {
        // Load relationships
        Resource parentResource = entityManager.getReference(Resource.class, parentResourceId);
        ResourceType resourceType = entityManager.getReference(ResourceType.class, resourceTypeId);

        // CreateResourceHistory.configuration is one-to-one, so make sure to clone the config, zeroing out all id's.
        Configuration configurationClone = (configuration != null) ? configuration.deepCopy(false) : null;

        // Persist and establish relationships
        CreateResourceHistory history = new CreateResourceHistory(parentResource, resourceType, user.getName(),
            configurationClone);
        history.setCreatedResourceName(createResourceName);
        history.setStatus(CreateResourceStatus.IN_PROGRESS);

        entityManager.persist(history);
        parentResource.addCreateChildResourceHistory(history);

        // Caller will need this
        parentResource.getAgent();

        return history;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CreateResourceHistory persistCreateHistory(Subject user, int parentResourceId, int resourceTypeId,
        String createResourceName, PackageVersion packageVersion, Configuration deploymentTimeConfiguration) {
        // Load relationships
        Resource parentResource = entityManager.getReference(Resource.class, parentResourceId);
        ResourceType resourceType = entityManager.getReference(ResourceType.class, resourceTypeId);

        // Persist and establish relationships
        // TODO: Note, InstalledPackage is set to null because it doesn't really make sense. An InstalledPackage
        // represents a backing package relationship between a Resource and a PackageVersion, not its parent.
        // I think it should probably be removed from the history entity. -jshaughn 9/1/09.
        CreateResourceHistory history = new CreateResourceHistory(parentResource, resourceType, user.getName(),
            (InstalledPackage) null);
        history.setCreatedResourceName(createResourceName);
        history.setConfiguration(deploymentTimeConfiguration);
        history.setStatus(CreateResourceStatus.IN_PROGRESS);

        entityManager.persist(history);
        parentResource.addCreateChildResourceHistory(history);

        // Caller will need this
        parentResource.getAgent();

        return history;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DeleteResourceHistory persistDeleteHistory(Subject user, int resourceId) {
        // Load relationships
        Resource resource = entityManager.find(Resource.class, resourceId);

        // Persist and establish relationships
        DeleteResourceHistory history = new DeleteResourceHistory(resource, user.getName());
        history.setStatus(DeleteResourceStatus.IN_PROGRESS);

        entityManager.persist(history);
        resource.addDeleteResourceHistory(history);

        // Caller will need this
        resource.getAgent();

        return history;
    }

    public CreateResourceHistory getCreateHistoryItem(int historyItemId) {
        Query query = entityManager.createNamedQuery(CreateResourceHistory.QUERY_FIND_BY_ID);
        query.setParameter("id", historyItemId);

        CreateResourceHistory history = (CreateResourceHistory) query.getSingleResult();

        return history;
    }

    public int getCreateChildResourceHistoryCount(int parentResourceId, Long beginDate, Long endDate) {
        Query query = PersistenceUtility.createCountQuery(entityManager,
            CreateResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID);

        query.setParameter("id", parentResourceId);
        query.setParameter("startTime", beginDate);
        query.setParameter("endTime", endDate);

        long totalCount = (Long) query.getSingleResult();

        return (int) totalCount;
    }

    @SuppressWarnings("unchecked")
    public PageList<CreateResourceHistory> findCreateChildResourceHistory(Subject subject, int parentResourceId,
        Long beginDate, Long endDate, PageControl pageControl) {
        pageControl.initDefaultOrderingField("crh.id", PageOrdering.DESC);

        int totalCount = getCreateChildResourceHistoryCount(parentResourceId, beginDate, endDate);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            CreateResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, pageControl);

        query.setParameter("id", parentResourceId);
        query.setParameter("startTime", beginDate);
        query.setParameter("endTime", endDate);

        List<CreateResourceHistory> history = query.getResultList();

        PageList<CreateResourceHistory> pageList = new PageList<CreateResourceHistory>(history, totalCount, pageControl);
        return pageList;
    }

    public int getDeleteChildResourceHistoryCount(int parentResourceId, Long beginDate, Long endDate) {
        Query query = PersistenceUtility.createCountQuery(entityManager,
            DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID);

        query.setParameter("id", parentResourceId);
        query.setParameter("startTime", beginDate);
        query.setParameter("endTime", endDate);

        long totalCount = (Long) query.getSingleResult();

        return (int) totalCount;
    }

    @SuppressWarnings("unchecked")
    public PageList<DeleteResourceHistory> findDeleteChildResourceHistory(Subject subject, int parentResourceId,
        Long beginDate, Long endDate, PageControl pageControl) {
        pageControl.initDefaultOrderingField("drh.id", PageOrdering.DESC);

        int totalCount = getDeleteChildResourceHistoryCount(parentResourceId, beginDate, endDate);

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            DeleteResourceHistory.QUERY_FIND_BY_PARENT_RESOURCE_ID, pageControl);

        query.setParameter("id", parentResourceId);
        query.setParameter("startTime", beginDate);
        query.setParameter("endTime", endDate);

        List<DeleteResourceHistory> history = query.getResultList();

        PageList<DeleteResourceHistory> pageList = new PageList<DeleteResourceHistory>(history, totalCount, pageControl);
        return pageList;
    }

    public CreateResourceHistory createResource(Subject user, int parentResourceId, int newResourceTypeId,
        String newResourceName, Configuration pluginConfiguration, String packageName, String packageVersionNumber,
        Integer architectureId, Configuration deploymentTimeConfiguration, InputStream packageBitStream) {

        return createResource(user, parentResourceId, newResourceTypeId, newResourceName, pluginConfiguration,
            packageName, packageVersionNumber, architectureId, deploymentTimeConfiguration, packageBitStream,
            (Map<String, String>) null, (Integer) null);
    }

    public CreateResourceHistory createResource(Subject user, int parentResourceId, int newResourceTypeId,
        String newResourceName, Configuration pluginConfiguration, String packageName, String packageVersionNumber,
        Integer architectureId, Configuration deploymentTimeConfiguration, InputStream packageBitStream,
        Map<String, String> packageUploadDetails) {

        return createResource(user, parentResourceId, newResourceTypeId, newResourceName, pluginConfiguration,
            packageName, packageVersionNumber, architectureId, deploymentTimeConfiguration, packageBitStream,
            packageUploadDetails, null);
    }

    public CreateResourceHistory createResource(Subject user, int parentResourceId, int newResourceTypeId,
        String newResourceName, Configuration pluginConfiguration, String packageName, String packageVersionNumber,
        Integer architectureId, Configuration deploymentTimeConfiguration, InputStream packageBitStream,
        Map<String, String> packageUploadDetails, Integer timeout) {

        LOG.info("Received call to create package backed resource under parent [" + parentResourceId + "]");

        Resource parentResource = entityManager.find(Resource.class, parentResourceId);

        // Check permissions first
        if (!authorizationManager
            .hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, parentResource.getId())) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to create a child resource for resource [" + parentResource + "]");
        }

        ResourceType newResourceType = entityManager.find(ResourceType.class, newResourceTypeId);
        PackageType newPackageType = contentManager.getResourceCreationPackageType(newResourceTypeId);

        if (!newResourceType.isCreatable()
            || (newResourceType.getCreationDataType() != ResourceCreationDataType.CONTENT)) {
            throw new RuntimeException("Cannot create " + newResourceType + " child Resource under parent "
                + parentResource + ", since the " + newResourceType
                + " type does not support content-based Resource creation.");
        }

        abortResourceCreationIfExistingSingleton(parentResource, newResourceType);

        // unless version is set start versioning the package by timestamp
        packageVersionNumber = (null == packageVersionNumber) ? Long.toString(System.currentTimeMillis())
            : packageVersionNumber;

        // default to no required architecture
        architectureId = (null != architectureId) ? architectureId : contentManager.getNoArchitecture().getId();

        // Create/locate package and package version
        PackageVersion packageVersion = null;
        if (packageUploadDetails == null) {
            packageVersion = contentManager.createPackageVersionWithDisplayVersion(user, packageName,
                newPackageType.getId(), packageVersionNumber, null, architectureId, packageBitStream);
        } else {
            packageVersion = contentManager.getUploadedPackageVersion(user, packageName, newPackageType.getId(),
                packageVersionNumber, architectureId, packageBitStream, packageUploadDetails, null);
        }

        return doCreatePackageBackedResource(user, parentResource, newResourceType, newResourceName,
            pluginConfiguration, deploymentTimeConfiguration, packageVersion, timeout);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // Remote Interface Impl
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public CreateResourceHistory createResource(Subject user, int parentResourceId, int resourceTypeId,
        String resourceName, Configuration pluginConfiguration, Configuration resourceConfiguration) {

        return createResource(user, parentResourceId, resourceTypeId, resourceName, pluginConfiguration,
            resourceConfiguration, (Integer) null);
    }

    public CreateResourceHistory createResource(Subject user, int parentResourceId, int resourceTypeId,
        String resourceName, Configuration pluginConfiguration, Configuration resourceConfiguration, Integer timeout) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Received call to create configuration backed resource under parent: " + parentResourceId
                + " of type: " + resourceTypeId);
        }

        ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
        Resource parentResource = entityManager.find(Resource.class, parentResourceId);
        Agent agent = parentResource.getAgent();

        // Check permissions first
        if (!authorizationManager
            .hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, parentResource.getId())) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to create a child resource for resource [" + parentResource + "]");
        }

        if (!resourceType.isCreatable()
            || (resourceType.getCreationDataType() != ResourceCreationDataType.CONFIGURATION)) {
            throw new RuntimeException("Cannot create " + resourceType + " child Resource under parent "
                + parentResource + ", since the " + resourceType
                + " type does not support configuration-based Resource creation.");
        }

        abortResourceCreationIfExistingSingleton(parentResource, resourceType);

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        CreateResourceHistory persistedHistory = resourceFactoryManager.persistCreateHistory(user, parentResourceId,
            resourceTypeId, resourceName, resourceConfiguration);

        // Package into transfer object
        CreateResourceRequest request = new CreateResourceRequest(persistedHistory.getId(), parentResourceId,
            resourceName, resourceType.getName(), resourceType.getPlugin(), pluginConfiguration, resourceConfiguration,
            timeout);

        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.createResource(request);

            return persistedHistory;
        } catch (Exception e) {
            LOG.error("Error while sending create resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            CreateResourceResponse response = new CreateResourceResponse(persistedHistory.getId(), null, null,
                CreateResourceStatus.FAILURE, errorMessage, resourceConfiguration);
            resourceFactoryManager.completeCreateResource(response);

            throw new RuntimeException("Error while sending create resource request to agent service", e);
        }
    }

    @Override
    public CreateResourceHistory createPackageBackedResource(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName, Configuration pluginConfiguration, String packageName,
        String packageVersionNumber, Integer architectureId, Configuration deploymentTimeConfiguration,
        byte[] packageBits, Integer timeout) {

        return createResource(subject, parentResourceId, newResourceTypeId, newResourceName, pluginConfiguration,
            packageName, packageVersionNumber, architectureId, deploymentTimeConfiguration, new ByteArrayInputStream(
                packageBits), (Map<String, String>) null, timeout);
    }

    @Override
    public CreateResourceHistory createPackageBackedResourceViaContentHandle(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName, Configuration pluginConfiguration, String packageName,
        String packageVersion, Integer architectureId, Configuration deploymentTimeConfiguration,
        String temporaryContentHandle, Integer timeout) {
        FileInputStream packageBitStream = null;
        try {
            packageBitStream = new FileInputStream(contentManager.getTemporaryContentFile(temporaryContentHandle));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return createResource(subject, parentResourceId, newResourceTypeId, newResourceName, pluginConfiguration,
            packageName, packageVersion, architectureId, deploymentTimeConfiguration, packageBitStream,
            (Map<String, String>) null, timeout);
    }

    @Override
    public CreateResourceHistory createPackageBackedResource(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName, Configuration pluginConfiguration, String packageName,
        String packageVersionNumber, Integer architectureId, Configuration deploymentTimeConfiguration,
        byte[] packageBits) {

        return createResource(subject, parentResourceId, newResourceTypeId, newResourceName, pluginConfiguration,
            packageName, packageVersionNumber, architectureId, deploymentTimeConfiguration, new ByteArrayInputStream(
                packageBits));
    }

    @Override
    public CreateResourceHistory createPackageBackedResourceViaPackageVersion(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName, Configuration pluginConfiguration,
        Configuration deploymentTimeConfiguration, int packageVersionId) {

        return createPackageBackedResourceViaPackageVersion(subject, parentResourceId, newResourceTypeId,
            newResourceName, pluginConfiguration, deploymentTimeConfiguration, packageVersionId, (Integer) null);
    }

    @Override
    public CreateResourceHistory createPackageBackedResourceViaPackageVersion(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName, Configuration pluginConfiguration,
        Configuration deploymentTimeConfiguration, int packageVersionId, Integer timeout) {

        Resource parentResource = entityManager.find(Resource.class, parentResourceId);

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(subject, Permission.CREATE_CHILD_RESOURCES,
            parentResource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to create a child resource for resource [" + parentResource + "]");
        }

        ResourceType newResourceType = entityManager.find(ResourceType.class, newResourceTypeId);
        PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);

        if (!newResourceType.isCreatable()
            || (newResourceType.getCreationDataType() != ResourceCreationDataType.CONTENT)) {
            throw new RuntimeException("Cannot create " + newResourceType + " child Resource under parent "
                + parentResource + ", since the " + newResourceType
                + " type does not support content-based Resource creation.");
        }

        abortResourceCreationIfExistingSingleton(parentResource, newResourceType);

        return doCreatePackageBackedResource(subject, parentResource, newResourceType, newResourceName,
            pluginConfiguration, deploymentTimeConfiguration, packageVersion, timeout);
    }

    private CreateResourceHistory doCreatePackageBackedResource(Subject subject, Resource parentResource,
        ResourceType newResourceType, String newResourceName, Configuration pluginConfiguration,
        Configuration deploymentTimeConfiguration, PackageVersion packageVersion, Integer timeout) {

        Agent agent = parentResource.getAgent();

        // add the timeout to the deploymentTimeConfiguration
        if (deploymentTimeConfiguration != null) {
            if (timeout != null) {
                deploymentTimeConfiguration.put(new PropertySimple("userProvidedTimeoutMillis", timeout));
            }
        }

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        CreateResourceHistory persistedHistory = resourceFactoryManager.persistCreateHistory(subject,
            parentResource.getId(), newResourceType.getId(), newResourceName, packageVersion,
            deploymentTimeConfiguration);

        // Package into transfer object
        ResourcePackageDetails packageDetails = ContentManagerHelper.packageVersionToDetails(packageVersion);
        packageDetails.setDeploymentTimeConfiguration(deploymentTimeConfiguration);
        CreateResourceRequest request = new CreateResourceRequest(persistedHistory.getId(), parentResource.getId(),
            newResourceName, newResourceType.getName(), newResourceType.getPlugin(), pluginConfiguration,
            packageDetails, timeout);

        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.createResource(request);

            return persistedHistory;
        } catch (NoResultException nre) {
            return null;
            //eat the exception.  Some of the queries return no results if no package yet exists which is fine.
        } catch (CannotConnectException e) {
            LOG.error("Error while sending create resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            CreateResourceResponse response = new CreateResourceResponse(persistedHistory.getId(), null, null,
                CreateResourceStatus.FAILURE, errorMessage, null);
            resourceFactoryManager.completeCreateResource(response);

            throw new CannotConnectToAgentException("Error while sending create resource request to agent service", e);
        } catch (Exception e) {
            LOG.error("Error while sending create resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            CreateResourceResponse response = new CreateResourceResponse(persistedHistory.getId(), null, null,
                CreateResourceStatus.FAILURE, errorMessage, null);
            resourceFactoryManager.completeCreateResource(response);

            throw new RuntimeException("Error while sending create resource request to agent service", e);
        }
    }

    public List<DeleteResourceHistory> deleteResources(Subject user, int[] resourceIds) {
        List<Integer> deleteResourceIds = new ArrayList<Integer>();
        List<DeleteResourceHistory> deleteResourceHistories = new ArrayList<DeleteResourceHistory>();

        for (Integer resourceId : resourceIds) {
            if (!deleteResourceIds.contains(resourceId)) {
                deleteResourceHistories.add(deleteResource(user, resourceId));
            }
        }

        return deleteResourceHistories;
    }

    public DeleteResourceHistory deleteResource(Subject subject, int resourceId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received call to delete resource: " + resourceId);
        }

        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(subject, Permission.DELETE_RESOURCE, resource.getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to delete resource [" + resource + "]");
        }

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        DeleteResourceHistory persistedHistory = resourceFactoryManager.persistDeleteHistory(subject, resourceId);

        // Package into transfer object
        DeleteResourceRequest request = new DeleteResourceRequest(persistedHistory.getId(), resourceId);

        try {
            if (resource.isSynthetic()) {
                resourceFactoryManager.completeDeleteResourceRequest(new DeleteResourceResponse(persistedHistory.getId(), DeleteResourceStatus.SUCCESS, null));
            } else {
                AgentClient agentClient = agentManager.getAgentClient(agent);
                ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
                resourceFactoryAgentService.deleteResource(request);
            }

            return persistedHistory;
        } catch (CannotConnectException e) {
            LOG.error("Error while sending delete resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            DeleteResourceResponse response = new DeleteResourceResponse(persistedHistory.getId(),
                DeleteResourceStatus.FAILURE, errorMessage);
            resourceFactoryManager.completeDeleteResourceRequest(response);

            throw new CannotConnectToAgentException("Error while sending delete resource request to agent service", e);
        } catch (Exception e) {
            LOG.error("Error while sending delete resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            DeleteResourceResponse response = new DeleteResourceResponse(persistedHistory.getId(),
                DeleteResourceStatus.FAILURE, errorMessage);
            resourceFactoryManager.completeDeleteResourceRequest(response);

            throw new RuntimeException("Error while sending delete resource request to agent service", e);
        }
    }

    private void abortResourceCreationIfExistingSingleton(Resource parentResource, ResourceType resourceType) {
        if (resourceType.isSingleton()) {
            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.addFilterParentResourceId(parentResource.getId());
            resourceCriteria.addFilterResourceTypeId(resourceType.getId());
            resourceCriteria.clearPaging();//disable paging as the code assumes all the results will be returned.

            PageList<Resource> childResourcesOfType = resourceManager.findResourcesByCriteria(
                subjectManager.getOverlord(), resourceCriteria);
            if (childResourcesOfType.size() >= 1) {
                throw new RuntimeException("Cannot create " + resourceType + " child Resource under parent "
                    + parentResource + ", since " + resourceType
                    + " is a singleton type, and there is already a child Resource of that type. "
                    + "If the existing child Resource corresponds to a managed Resource which no longer exists, "
                    + "uninventory it and then try again.");
            }
        }
    }

}
