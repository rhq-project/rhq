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
package org.rhq.enterprise.server.resource;

import java.io.ByteArrayInputStream;
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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceStatus;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.content.ContentManagerHelper;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.jaxb.adapter.ConfigurationAdapter;

/**
 * Bean to handle interaction with the resource factory subsystem of the plugin container. !! Warning, the factory
 * interface is there to remove managed things from disk. Use caution when using this and don't confuse it with removing
 * something from the system inventory. !!
 *
 * @author Jason Dobies
 */
@Stateless
public class ResourceFactoryManagerBean implements ResourceFactoryManagerLocal, ResourceFactoryManagerRemote {
    // Constants  --------------------------------------------

    /**
     * Amount of time a request may be outstanding against an agent before it is marked as timed out.
     */
    private static final int REQUEST_TIMEOUT = 1000 * 60 * 60;

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(ResourceFactoryManagerBean.class);

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
        log.debug("Received call to complete create resource: " + response);

        // Load the persisted history entry
        CreateResourceHistory history = entityManager.find(CreateResourceHistory.class, response.getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (history == null) {
            log
                .error("Attempting to complete a request that was not found in the database: "
                    + response.getRequestId());
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

        // RHQ-666 - The resource name will likely come from the plugin. If both the user indicated a name at
        // creation time (which would be in the history item), use that to override what the plugin indicates
        String newResourceName = response.getResourceName();

        if (history.getCreatedResourceName() != null) {
            newResourceName = history.getCreatedResourceName();
        }

        // If the plugin reports it as successful, create the resource and mark it as committed
        // Currently commented out because of https://jira.jboss.org/jira/browse/JBNADM-3451
        // basically: this prevented getting a version of the resource with correct pluginConfig
        //    from autodiscovery back into the inventory
        //
        //        if (response.getStatus() == CreateResourceStatus.SUCCESS) {
        //            resourceFactoryManager.createInventoryResource(history.getParentResource().getId(), history
        //                .getResourceType().getId(), newResourceName, response.getResourceKey());
        //        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void completeDeleteResourceRequest(DeleteResourceResponse response) {
        log.debug("Received call to complete delete resource: " + response);

        // Load the persisted history entry
        DeleteResourceHistory history = entityManager.find(DeleteResourceHistory.class, response.getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (history == null) {
            log.error("Attemping to complete a request that was not found in the database: " + response.getRequestId());
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
            resource.setParentResource(null);
            resource.setItime(System.currentTimeMillis());
            entityManager.merge(resource);

            // uninventory the children of the deleted resource (see rhq-2378)
            uninventoryChildren(children);
        }
    }

    private void uninventoryChildren(Set<Resource> children) {
        for (Resource child : children) {
            resourceManager.uninventoryResource(subjectManager.getOverlord(), child.getId());
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
                    log.debug("Timing out request after duration: " + duration + " Request: " + request);

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
                    log.debug("Timing out request after duration: " + duration + " Request: " + request);

                    request.setErrorMessage("Request with duration " + duration + " exceeded the timeout threshold of "
                        + REQUEST_TIMEOUT);
                    request.setStatus(DeleteResourceStatus.TIMED_OUT);
                }
            }
        } catch (Throwable e) {
            log.error("Error while processing timed out requests", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CreateResourceHistory persistCreateHistory(Subject user, int parentResourceId, int resourceTypeId,
        String createResourceName, Configuration configuration) {
        // Load relationships
        Resource parentResource = entityManager.getReference(Resource.class, parentResourceId);
        ResourceType resourceType = entityManager.getReference(ResourceType.class, resourceTypeId);

        // CreateResourceHistory.configuration is one-to-one, so make sure to clone the config, zeroing out all id's.
        Configuration configurationClone = configuration.deepCopy(false);

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
            packageName, packageVersionNumber, architectureId, deploymentTimeConfiguration, packageBitStream, null);
    }

    public CreateResourceHistory createResource(Subject user, int parentResourceId, int newResourceTypeId,
        String newResourceName, Configuration pluginConfiguration, String packageName, String packageVersionNumber,
        Integer architectureId, Configuration deploymentTimeConfiguration, InputStream packageBitStream,
        Map<String, String> packageUploadDetails) {

        log.info("Received call to create package backed resource under parent [" + parentResourceId + "]");

        Resource parentResource = entityManager.find(Resource.class, parentResourceId);

        // Check permissions first
        if (!authorizationManager
            .hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, parentResource.getId())) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to create a child resource for resource [" + parentResource + "]");
        }

        ResourceType newResourceType = entityManager.find(ResourceType.class, newResourceTypeId);
        PackageType newPackageType = contentManager.getResourceCreationPackageType(newResourceTypeId);

        // unless version is set start versioning the package by timestamp
        packageVersionNumber = (null == packageVersionNumber) ? Long.toString(System.currentTimeMillis())
            : packageVersionNumber;

        // default to no required architecture
        architectureId = (null != architectureId) ? architectureId : contentManager.getNoArchitecture().getId();

        // Create/locate package and package version
        PackageVersion packageVersion = null;
        if (packageUploadDetails == null) {
            packageVersion = contentManager.createPackageVersion(user, packageName, newPackageType.getId(),
                packageVersionNumber, architectureId, packageBitStream);
        } else {
            packageVersion = contentManager.getUploadedPackageVersion(user, packageName, newPackageType.getId(),
                packageVersionNumber, architectureId, packageBitStream, packageUploadDetails, null);
        }

        return doCreatePackageBackedResource(user, parentResource, newResourceType, newResourceName,
            pluginConfiguration, deploymentTimeConfiguration, packageVersion);
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // Remote Interface Impl
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public CreateResourceHistory createResource(Subject user, int parentResourceId, int resourceTypeId,
        String resourceName, Configuration pluginConfiguration, Configuration resourceConfiguration) {
        log.debug("Received call to create configuration backed resource under parent: " + parentResourceId
            + " of type: " + resourceTypeId);

        ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
        Resource resource = entityManager.find(Resource.class, parentResourceId);
        Agent agent = resource.getAgent();

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, resource.getId())) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to create a child resource for resource [" + resource + "]");
        }

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        CreateResourceHistory persistedHistory = resourceFactoryManager.persistCreateHistory(user, parentResourceId,
            resourceTypeId, resourceName, resourceConfiguration);

        // Package into transfer object
        CreateResourceRequest request = new CreateResourceRequest(persistedHistory.getId(), parentResourceId,
            resourceName, resourceType.getName(), resourceType.getPlugin(), pluginConfiguration, resourceConfiguration);

        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.createResource(request);

            return persistedHistory;
        } catch (Exception e) {
            log.error("Error while sending create resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            CreateResourceResponse response = new CreateResourceResponse(persistedHistory.getId(), null, null,
                CreateResourceStatus.FAILURE, errorMessage, resourceConfiguration);
            resourceFactoryManager.completeCreateResource(response);

            throw new RuntimeException("Error while sending create resource request to agent service", e);
        }
    }

    public CreateResourceHistory createPackageBackedResource(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration pluginConfiguration, String packageName, String packageVersionNumber, Integer architectureId,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration deploymentTimeConfiguration, byte[] packageBits) {

        return createResource(subject, parentResourceId, newResourceTypeId, newResourceName, pluginConfiguration,
            packageName, packageVersionNumber, architectureId, deploymentTimeConfiguration, new ByteArrayInputStream(
                packageBits));
    }

    public CreateResourceHistory createPackageBackedResourceViaPackageVersion(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration pluginConfiguration,//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration deploymentTimeConfiguration,//
        int packageVersionId) {

        Resource parentResource = entityManager.find(Resource.class, parentResourceId);

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(subject, Permission.CREATE_CHILD_RESOURCES, parentResource
            .getId())) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to create a child resource for resource [" + parentResource + "]");
        }

        ResourceType newResourceType = entityManager.find(ResourceType.class, newResourceTypeId);
        PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);

        return doCreatePackageBackedResource(subject, parentResource, newResourceType, newResourceName,
            pluginConfiguration, deploymentTimeConfiguration, packageVersion);
    }

    private CreateResourceHistory doCreatePackageBackedResource(Subject subject, Resource parentResource,
        ResourceType newResourceType, String newResourceName, Configuration pluginConfiguration,
        Configuration deploymentTimeConfiguration, PackageVersion packageVersion) {

        Agent agent = parentResource.getAgent();

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        CreateResourceHistory persistedHistory = resourceFactoryManager.persistCreateHistory(subject, parentResource
            .getId(), newResourceType.getId(), newResourceName, packageVersion, deploymentTimeConfiguration);

        // Package into transfer object
        ResourcePackageDetails packageDetails = ContentManagerHelper.packageVersionToDetails(packageVersion);
        packageDetails.setDeploymentTimeConfiguration(deploymentTimeConfiguration);
        CreateResourceRequest request = new CreateResourceRequest(persistedHistory.getId(), parentResource.getId(),
            newResourceName, newResourceType.getName(), newResourceType.getPlugin(), pluginConfiguration,
            packageDetails);

        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.createResource(request);

            return persistedHistory;
        } catch (NoResultException nre) {
            return null;
            //eat the exception.  Some of the queries return no results if no package yet exists which is fine.
        } catch (Exception e) {
            log.error("Error while sending create resource request to agent service", e);

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
        log.debug("Received call to delete resource: " + resourceId);

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
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ResourceFactoryAgentService resourceFactoryAgentService = agentClient.getResourceFactoryAgentService();
            resourceFactoryAgentService.deleteResource(request);

            return persistedHistory;
        } catch (Exception e) {
            log.error("Error while sending delete resource request to agent service", e);

            // Submit the error as a failure response
            String errorMessage = ThrowableUtil.getAllMessages(e);
            DeleteResourceResponse response = new DeleteResourceResponse(persistedHistory.getId(),
                DeleteResourceStatus.FAILURE, errorMessage);
            resourceFactoryManager.completeDeleteResourceRequest(response);

            throw new RuntimeException("Error while sending delete resource request to agent service", e);
        }
    }

}
