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
package org.rhq.enterprise.server.content;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.ContentRequestType;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.InstalledPackageHistoryStatus;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ContentDiscoveryReport;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeletePackagesRequest;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesRequest;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.RetrievePackageBitsRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * EJB that handles content subsystem interaction with resources, including content discovery reports and create/delete
 * functionality.
 *
 * @author Jason Dobies
 */
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.content.ContentManagerRemote")
public class ContentManagerBean implements ContentManagerLocal, ContentManagerRemote {
    // Constants  --------------------------------------------

    /**
     * Amount of time a request may be outstanding against an agent before it is marked as timed out.
     */
    private static final int REQUEST_TIMEOUT = 1000 * 60 * 60;

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    public DataSource dataSource;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private ContentManagerLocal contentManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    // ContentManagerLocal Implementation  --------------------------------------------

    @SuppressWarnings("unchecked")
    public void mergeDiscoveredPackages(ContentDiscoveryReport report) {
        int resourceId = report.getResourceId();

        // For performance tracking
        long start = System.currentTimeMillis();

        log.info("Merging packages for resource ID [" + resourceId + "]. Package count ["
            + report.getDeployedPackages().size() + "]");

        // Load the resource and its installed packages
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            log.error("Invalid resource ID specified for merge. Resource ID: " + resourceId);
            return;
        }

        // Timestamp to use for all audit trail entries from this report
        Date timestamp = new Date();

        // Before we process the report, get a list of all installed packages on the resource.
        // InstalledPackage objects in this list that are not referenced in the report are to be removed.
        Query currentInstalledPackageQuery = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID);
        currentInstalledPackageQuery.setParameter("resourceId", resource.getId());

        Set<InstalledPackage> doomedPackages = new HashSet<InstalledPackage>(currentInstalledPackageQuery
            .getResultList());

        // The report contains an entire snapshot of packages, so each of these has to be represented
        // as an InstalledPackage
        for (ResourcePackageDetails resourcePackage : report.getDeployedPackages()) {
            // Load the overall package (used in a few places later in this loop)
            Query packageQuery = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_RESOURCE_TYPE);
            packageQuery.setFlushMode(FlushModeType.COMMIT);
            packageQuery.setParameter("name", resourcePackage.getName());
            packageQuery.setParameter("packageTypeName", resourcePackage.getPackageTypeName());
            packageQuery.setParameter("resourceTypeId", resource.getResourceType().getId());

            List<Package> existingPackages = packageQuery.getResultList();

            Package generalPackage = null;
            if (existingPackages.size() > 0) {
                generalPackage = existingPackages.get(0);
            }

            // See if package version already exists for the resource package
            Query packageVersionQuery = entityManager
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            packageVersionQuery.setFlushMode(FlushModeType.COMMIT);
            packageVersionQuery.setParameter("packageName", resourcePackage.getName());
            packageVersionQuery.setParameter("packageTypeName", resourcePackage.getPackageTypeName());
            packageVersionQuery.setParameter("resourceTypeId", resource.getResourceType().getId());
            packageVersionQuery.setParameter("architectureName", resourcePackage.getArchitectureName());
            packageVersionQuery.setParameter("version", resourcePackage.getVersion());

            List<PackageVersion> existingPackageVersionList = packageVersionQuery.getResultList();

            PackageVersion packageVersion = null;
            if (existingPackageVersionList.size() > 0) {
                packageVersion = existingPackageVersionList.get(0);
            }

            // If we didn't find a package version for this deployed package, we will need to create it
            if (packageVersion == null) {
                if (generalPackage == null) {
                    Query packageTypeQuery = entityManager
                        .createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME);
                    packageTypeQuery.setFlushMode(FlushModeType.COMMIT);
                    packageTypeQuery.setParameter("typeId", resource.getResourceType().getId());
                    packageTypeQuery.setParameter("name", resourcePackage.getPackageTypeName());

                    PackageType packageType = (PackageType) packageTypeQuery.getSingleResult();

                    generalPackage = new Package(resourcePackage.getName(), packageType);
                    entityManager.persist(generalPackage);
                }

                // Create a new package version and attach to the general package
                Query architectureQuery = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
                architectureQuery.setFlushMode(FlushModeType.COMMIT);
                architectureQuery.setParameter("name", resourcePackage.getArchitectureName());

                Architecture packageArchitecture;

                // We don't have an architecture enum, so it's very possible the plugin will pass in a crap string here.
                // Catch and throw with a better error message
                try {
                    packageArchitecture = (Architecture) architectureQuery.getSingleResult();
                } catch (Exception e) {
                    throw new RuntimeException("Could not load architecture for architecture name ["
                        + resourcePackage.getArchitectureName() + "] for package [" + resourcePackage.getName() + "]");
                }

                packageVersion = new PackageVersion(generalPackage, resourcePackage.getVersion(), packageArchitecture);
                packageVersion.setDisplayName(resourcePackage.getDisplayName());
                packageVersion.setDisplayVersion(resourcePackage.getDisplayVersion());
                packageVersion.setFileCreatedDate(resourcePackage.getFileCreatedDate());
                packageVersion.setFileName(resourcePackage.getFileName());
                packageVersion.setFileSize(resourcePackage.getFileSize());
                packageVersion.setLicenseName(resourcePackage.getLicenseName());
                packageVersion.setLicenseVersion(resourcePackage.getLicenseVersion());
                packageVersion.setLongDescription(resourcePackage.getLongDescription());
                packageVersion.setMD5(resourcePackage.getMD5());
                packageVersion.setMetadata(resourcePackage.getMetadata());
                packageVersion.setSHA256(resourcePackage.getSHA265());
                packageVersion.setShortDescription(resourcePackage.getShortDescription());
                packageVersion.setExtraProperties(resourcePackage.getExtraProperties());

                entityManager.persist(packageVersion);
            } // end package version null check
            else {
                // If the package version was already in the system, see if there is an installed package for
                // this package version. If so, we're done processing this package
                Query installedPackageQuery = entityManager
                    .createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_AND_PACKAGE_VER);
                installedPackageQuery.setFlushMode(FlushModeType.COMMIT);
                installedPackageQuery.setParameter("resourceId", resource.getId());
                installedPackageQuery.setParameter("packageVersionId", packageVersion.getId());

                List<InstalledPackage> installedPackageList = installedPackageQuery.getResultList();

                if (installedPackageList.size() > 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Discovered package is already known to the inventory "
                            + installedPackageList.iterator().next());
                    }

                    // This represents a package that was previously installed and still is. We need to remove
                    // the reference to this from the doomed packages list so it's not marked as deleted at the end.
                    for (InstalledPackage ip : installedPackageList) {
                        doomedPackages.remove(ip);
                    }

                    continue;
                }
            }

            // At this point, we have the package and package version in the system (now added if they weren't already)
            // We've also punched out early if we already knew about the installed package, so we won't add another
            // reference from the resource to the package nor another audit trail entry saying it was discovered.

            // Create a new installed package entry in the audit
            InstalledPackage newlyInstalledPackage = new InstalledPackage();
            newlyInstalledPackage.setPackageVersion(packageVersion);
            newlyInstalledPackage.setResource(resource);

            entityManager.persist(newlyInstalledPackage);

            // Create an audit trail entry to show how this package was added to the system
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setDeploymentConfigurationValues(resourcePackage.getDeploymentTimeConfiguration());
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setStatus(InstalledPackageHistoryStatus.DISCOVERED);
            history.setTimestamp(timestamp);

            entityManager.persist(history);

            entityManager.flush();
        } // end resource package loop

        // For any previously active installed packages that were not found again (and thus removed from the doomed
        // list), delete them.
        for (InstalledPackage doomedPackage : doomedPackages) {
            doomedPackage = entityManager.find(InstalledPackage.class, doomedPackage.getId());

            // Add an audit trail entry to indicate the package was not rediscovered
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setPackageVersion(doomedPackage.getPackageVersion());
            history.setResource(resource);
            history.setStatus(InstalledPackageHistoryStatus.MISSING);
            history.setTimestamp(timestamp);

            entityManager.remove(doomedPackage);
        }

        log.info("Finished merging " + report.getDeployedPackages().size() + " packages in "
            + (System.currentTimeMillis() - start) + "ms");
    }

    public void deployPackages(Subject user, Set<Integer> resourceIds, Set<Integer> packageVersionIds) {
        for (Integer rid : resourceIds) {
            Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();
            for (Integer pid : packageVersionIds) {
                PackageVersion pv = entityManager.find(PackageVersion.class, pid);
                if (pid == null) {
                    throw new IllegalArgumentException("PackageVersion ID passed in was null");
                }

                if (pv == null) {
                    throw new IllegalArgumentException("PackageVersion: [" + pid + "] not found!");
                }

                PackageDetailsKey key = new PackageDetailsKey(pv.getGeneralPackage().getName(), pv.getVersion(), pv
                    .getGeneralPackage().getPackageType().getName(), pv.getArchitecture().getName());
                ResourcePackageDetails details = new ResourcePackageDetails(key);
                packages.add(details);
            }

            deployPackages(user, rid, packages);
        }
    }

    public void deployPackages(Subject user, int resourceId, Set<ResourcePackageDetails> packages) {
        if (packages == null) {
            throw new IllegalArgumentException("packages cannot be null");
        }

        log.info("Deploying " + packages.size() + " packages on resource ID [" + resourceId + "]");

        if (packages.size() == 0) {
            return;
        }

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to deploy packages for resource ID [" + resourceId + "]");
        }

        // Load entities for references later in the method
        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        // This call will also create the audit trail entry.
        ContentServiceRequest persistedRequest = contentManager.createDeployRequest(resourceId, user.getName(),
            packages);

        // Package into transfer object
        DeployPackagesRequest transferRequest = new DeployPackagesRequest(persistedRequest.getId(), resourceId,
            packages);

        // Make call to agent
        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ContentAgentService agentService = agentClient.getContentAgentService();
            agentService.deployPackages(transferRequest);
        } catch (RuntimeException e) {
            log.error("Error while sending deploy request to agent", e);

            // Update the request with the failure
            contentManager.failRequest(persistedRequest.getId(), e);

            // Throw so caller knows an error happened
            throw e;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentServiceRequest createDeployRequest(int resourceId, String username,
        Set<ResourcePackageDetails> packages) {
        Resource resource = entityManager.find(Resource.class, resourceId);

        ContentServiceRequest persistedRequest = new ContentServiceRequest(resource, username,
            ContentRequestType.DEPLOY);
        persistedRequest.setStatus(ContentRequestStatus.IN_PROGRESS);

        Date timestamp = new Date();

        for (ResourcePackageDetails packageDetails : packages) {
            // Load the package version for the relationship
            PackageDetailsKey key = packageDetails.getKey();
            Query packageVersionQuery = entityManager
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            packageVersionQuery.setParameter("packageName", key.getName());
            packageVersionQuery.setParameter("packageTypeName", key.getPackageTypeName());
            packageVersionQuery.setParameter("architectureName", key.getArchitectureName());
            packageVersionQuery.setParameter("version", key.getVersion());
            packageVersionQuery.setParameter("resourceTypeId", resource.getResourceType().getId());

            PackageVersion packageVersion = (PackageVersion) packageVersionQuery.getSingleResult();

            // Create the history entity
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setContentServiceRequest(persistedRequest);
            history.setDeploymentConfigurationValues(packageDetails.getDeploymentTimeConfiguration());
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setStatus(InstalledPackageHistoryStatus.BEING_INSTALLED);
            history.setTimestamp(timestamp);

            persistedRequest.addInstalledPackageHistory(history);
        }

        entityManager.persist(persistedRequest);

        return persistedRequest;
    }

    @SuppressWarnings("unchecked")
    public void completeDeployPackageRequest(DeployPackagesResponse response) {
        log.info("Completing deploy package response: " + response);

        // Load persisted request
        Query query = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID_WITH_INSTALLED_PKG_HIST);
        query.setParameter("id", response.getRequestId());
        ContentServiceRequest persistedRequest = (ContentServiceRequest) query.getSingleResult();
        Resource resource = persistedRequest.getResource();

        int resourceTypeId = persistedRequest.getResource().getResourceType().getId();
        Subject user = subjectManager.findSubjectByName(persistedRequest.getSubjectName());

        // Update the persisted request
        persistedRequest.setErrorMessage(response.getOverallRequestErrorMessage());
        persistedRequest.setStatus(translateRequestResultStatus(response.getOverallRequestResult()));

        // Handle each individual package
        Date timestamp = new Date();

        for (DeployIndividualPackageResponse singleResponse : response.getPackageResponses()) {
            // Load the package version for the relationship
            PackageDetailsKey key = singleResponse.getKey();
            Query packageVersionQuery = entityManager
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            packageVersionQuery.setParameter("packageName", key.getName());
            packageVersionQuery.setParameter("packageTypeName", key.getPackageTypeName());
            packageVersionQuery.setParameter("architectureName", key.getArchitectureName());
            packageVersionQuery.setParameter("version", key.getVersion());
            packageVersionQuery.setParameter("resourceTypeId", resourceTypeId);

            PackageVersion packageVersion = (PackageVersion) packageVersionQuery.getSingleResult();

            // Create the history entity
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setContentServiceRequest(persistedRequest);
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setTimestamp(timestamp);

            // Link the deployment configuration values that were saved for the initial history entity for this
            // package with this entity as well. This will let us show the user the configuration values on the
            // last entry in the audit trail (i.e. for a failed package, the configuration values will be accessible).
            Query deploymentConfigurationQuery = entityManager
                .createNamedQuery(InstalledPackageHistory.QUERY_FIND_CONFIG_BY_PACKAGE_VERSION_AND_REQ);
            deploymentConfigurationQuery.setParameter("packageVersion", packageVersion);
            deploymentConfigurationQuery.setParameter("contentServiceRequest", persistedRequest);
            deploymentConfigurationQuery.setMaxResults(1);

            Configuration deploymentConfiguration = null;
            List deploymentConfigurationResults = deploymentConfigurationQuery.getResultList();
            if (deploymentConfigurationResults.size() > 0) {
                deploymentConfiguration = (Configuration) deploymentConfigurationResults.get(0);
            }

            history.setDeploymentConfigurationValues(deploymentConfiguration);

            // If the package indicated installation steps, link them to the resulting history entry
            List<DeployPackageStep> transferObjectSteps = singleResponse.getDeploymentSteps();
            if (transferObjectSteps != null) {
                List<PackageInstallationStep> installationSteps = translateInstallationSteps(transferObjectSteps,
                    history);
                history.setInstallationSteps(installationSteps);
            }

            if (singleResponse.getResult() == ContentResponseResult.SUCCESS) {
                history.setStatus(InstalledPackageHistoryStatus.INSTALLED);
            } else {
                history.setStatus(InstalledPackageHistoryStatus.FAILED);
                history.setErrorMessage(singleResponse.getErrorMessage());
            }

            entityManager.persist(history);
            persistedRequest.addInstalledPackageHistory(history);
        }
    }

    public void deletePackages(Subject user, Set<Integer> resourceIds, Set<Integer> installedPackageIds) {
        for (Integer rid : resourceIds) {
            deletePackages(user, rid, installedPackageIds);
        }
    }

    @SuppressWarnings("unchecked")
    public void deletePackages(Subject user, int resourceId, Set<Integer> installedPackageIds) {
        if (installedPackageIds == null) {
            throw new IllegalArgumentException("installedPackages cannot be null");
        }

        log.info("Deleting " + installedPackageIds.size() + " from resource ID [" + resourceId + "]");

        if (installedPackageIds.size() == 0) {
            return;
        }

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to delete installedPackageIds from resource ID [" + resourceId + "]");
        }

        // Load entities for references later in the method
        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        // This will also create the audit trail entry
        ContentServiceRequest persistedRequest = contentManager.createRemoveRequest(resourceId, user.getName(),
            installedPackageIds);

        // Package into transfer object
        Query query = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_BY_SET_OF_IDS);
        query.setParameter("packageIds", installedPackageIds);

        List<InstalledPackage> installedPackageList = query.getResultList();
        Set<ResourcePackageDetails> transferPackages = new HashSet<ResourcePackageDetails>(installedPackageList.size());

        for (InstalledPackage installedPackage : installedPackageList) {
            ResourcePackageDetails transferPackage = ContentManagerHelper.installedPackageToDetails(installedPackage);
            transferPackages.add(transferPackage);
        }

        DeletePackagesRequest transferRequest = new DeletePackagesRequest(persistedRequest.getId(), resourceId,
            transferPackages);

        // Make call to agent
        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ContentAgentService agentService = agentClient.getContentAgentService();
            agentService.deletePackages(transferRequest);
        } catch (RuntimeException e) {
            log.error("Error while sending deploy request to agent", e);

            // Update the request with the failure
            contentManager.failRequest(persistedRequest.getId(), e);

            // Throw so caller knows an error happened
            throw e;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentServiceRequest createRemoveRequest(int resourceId, String username, Set<Integer> installedPackageIds) {
        Resource resource = entityManager.find(Resource.class, resourceId);

        ContentServiceRequest persistedRequest = new ContentServiceRequest(resource, username,
            ContentRequestType.DELETE);
        persistedRequest.setStatus(ContentRequestStatus.IN_PROGRESS);

        Date timestamp = new Date();

        for (Integer installedPackageId : installedPackageIds) {
            // Load the InstalledPackage to get its package version for the relationship
            InstalledPackage ip = entityManager.find(InstalledPackage.class, installedPackageId);
            PackageVersion packageVersion = ip.getPackageVersion();

            // Create the history entity
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setContentServiceRequest(persistedRequest);
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setStatus(InstalledPackageHistoryStatus.BEING_DELETED);
            history.setTimestamp(timestamp);

            persistedRequest.addInstalledPackageHistory(history);
        }

        entityManager.persist(persistedRequest);

        return persistedRequest;
    }

    @SuppressWarnings("unchecked")
    public void completeDeletePackageRequest(RemovePackagesResponse response) {
        log.info("Completing delete package response: " + response);

        // Load persisted request
        Query query = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID_WITH_INSTALLED_PKG_HIST);
        query.setParameter("id", response.getRequestId());
        ContentServiceRequest persistedRequest = (ContentServiceRequest) query.getSingleResult();

        Resource resource = persistedRequest.getResource();
        int resourceTypeId = resource.getResourceType().getId();

        // Update the persisted request
        persistedRequest.setErrorMessage(response.getOverallRequestErrorMessage());
        persistedRequest.setStatus(translateRequestResultStatus(response.getOverallRequestResult()));

        // Handle each individual package
        Date timestamp = new Date();

        for (RemoveIndividualPackageResponse singleResponse : response.getPackageResponses()) {
            // Load the package version for the relationship
            PackageDetailsKey key = singleResponse.getKey();
            Query packageVersionQuery = entityManager
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            packageVersionQuery.setParameter("packageName", key.getName());
            packageVersionQuery.setParameter("packageTypeName", key.getPackageTypeName());
            packageVersionQuery.setParameter("architectureName", key.getArchitectureName());
            packageVersionQuery.setParameter("version", key.getVersion());
            packageVersionQuery.setParameter("resourceTypeId", resourceTypeId);

            PackageVersion packageVersion = (PackageVersion) packageVersionQuery.getSingleResult();

            // Create the history entity
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setContentServiceRequest(persistedRequest);
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setTimestamp(timestamp);

            if (singleResponse.getResult() == ContentResponseResult.SUCCESS) {
                history.setStatus(InstalledPackageHistoryStatus.DELETED);

                // We used to remove the InstalledPackage entity here, but now we'll rely on the plugin container
                // to trigger a discovery after the delete request finishes.
            } else {
                history.setStatus(InstalledPackageHistoryStatus.FAILED);
                history.setErrorMessage(singleResponse.getErrorMessage());
            }
        }
    }

    public void retrieveBitsFromResource(Subject user, int resourceId, int installedPackageId) {
        log.info("Retrieving bits for package [" + installedPackageId + "] on resource ID [" + resourceId + "]");

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("User [" + user.getName() + "] does not have permission to delete package "
                + installedPackageId + " for resource ID [" + resourceId + "]");
        }

        // Load entities for references later in the method
        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();
        InstalledPackage installedPackage = entityManager.find(InstalledPackage.class, installedPackageId);

        // Persist in separate transaction so it is committed immediately, before the request is sent to the agent
        ContentServiceRequest persistedRequest = contentManager.createRetrieveBitsRequest(resourceId, user.getName(),
            installedPackageId);

        // Package into transfer object
        ResourcePackageDetails transferPackage = ContentManagerHelper.installedPackageToDetails(installedPackage);
        RetrievePackageBitsRequest transferRequest = new RetrievePackageBitsRequest(persistedRequest.getId(),
            resourceId, transferPackage);

        // Make call to agent
        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ContentAgentService agentService = agentClient.getContentAgentService();
            agentService.retrievePackageBits(transferRequest);
        } catch (RuntimeException e) {
            log.error("Error while sending deploy request to agent", e);

            // Update the request with the failure
            contentManager.failRequest(persistedRequest.getId(), e);

            // Throw so caller knows an error happened
            throw e;
        }
    }

    public List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails)
        throws Exception {
        log.info("Retrieving installation steps for package [" + packageDetails + "]");

        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();

        // Make call to agent
        List<DeployPackageStep> packageStepList;
        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ContentAgentService agentService = agentClient.getContentAgentService();
            packageStepList = agentService.translateInstallationSteps(resourceId, packageDetails);
        } catch (PluginContainerException e) {
            log.error("Error while sending deploy request to agent", e);

            // Throw so caller knows an error happened
            throw e;
        }

        return packageStepList;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentServiceRequest createRetrieveBitsRequest(int resourceId, String username, int installedPackageId) {
        Resource resource = entityManager.find(Resource.class, resourceId);

        ContentServiceRequest persistedRequest = new ContentServiceRequest(resource, username,
            ContentRequestType.DEPLOY);
        persistedRequest.setStatus(ContentRequestStatus.IN_PROGRESS);

        Date timestamp = new Date();

        // Load the InstalledPackage to get its package version for the relationship
        InstalledPackage ip = entityManager.find(InstalledPackage.class, installedPackageId);
        PackageVersion packageVersion = ip.getPackageVersion();

        // Create the history entity
        InstalledPackageHistory history = new InstalledPackageHistory();
        history.setContentServiceRequest(persistedRequest);
        history.setPackageVersion(packageVersion);
        history.setResource(resource);
        history.setStatus(InstalledPackageHistoryStatus.BEING_RETRIEVED);
        history.setTimestamp(timestamp);

        persistedRequest.addInstalledPackageHistory(history);

        entityManager.persist(persistedRequest);

        return persistedRequest;
    }

    @TransactionTimeout(1000 * 60 * 30)
    public void completeRetrievePackageBitsRequest(ContentServiceResponse response, InputStream bitStream) {
        log.info("Completing retrieve package bits response: " + response);

        // Load persisted request
        ContentServiceRequest persistedRequest = entityManager.find(ContentServiceRequest.class, response
            .getRequestId());
        Resource resource = persistedRequest.getResource();

        // There is some inconsistency if we're completing a request that was not in the database
        if (persistedRequest == null) {
            log
                .error("Attempting to complete a request that was not found in the database: "
                    + response.getRequestId());
            return;
        }

        InstalledPackageHistory initialRequestHistory = persistedRequest.getInstalledPackageHistory().iterator().next();
        PackageVersion packageVersion = initialRequestHistory.getPackageVersion();

        // Read the stream from the agent and store in the package version
        try {
            log.debug("Saving content for response: " + response);

            PackageBits packageBits = new PackageBits();
            entityManager.persist(packageBits);

            packageVersion.setPackageBits(packageBits);
            entityManager.flush(); // push the new package bits row to the DB

            Connection conn = null;
            PreparedStatement ps = null;

            try {
                // fallback to JDBC so we can stream the data to the blob column
                conn = dataSource.getConnection();
                ps = conn.prepareStatement("UPDATE " + PackageBits.TABLE_NAME + " SET BITS = ? WHERE ID = ?");
                ps.setBinaryStream(1, bitStream, packageVersion.getFileSize().intValue()); // TODO: DO I HAVE FILESIZE???
                ps.setInt(2, packageBits.getId());
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Did not stream the package bits to the DB for [" + packageVersion + "]");
                }
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception e) {
                        log.warn("Failed to close prepared statement for package version [" + packageVersion + "]");
                    }
                }

                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception e) {
                        log.warn("Failed to close connection for package version [" + packageVersion + "]");
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error while reading content from agent stream", e);
            // TODO: don't want to throw exception here? does the tx rollback automatically anyway?
        }

        // Update the persisted request
        persistedRequest.setErrorMessage(response.getErrorMessage());
        persistedRequest.setStatus(response.getStatus());

        // Add a new audit trail entry
        InstalledPackageHistory completedHistory = new InstalledPackageHistory();
        completedHistory.setContentServiceRequest(persistedRequest);
        completedHistory.setResource(resource);
        completedHistory.setTimestamp(new Date());
        completedHistory.setPackageVersion(packageVersion);

        if (response.getStatus() == ContentRequestStatus.SUCCESS) {
            completedHistory.setStatus(InstalledPackageHistoryStatus.RETRIEVED);
        } else {
            completedHistory.setStatus(InstalledPackageHistoryStatus.FAILED);
            completedHistory.setErrorMessage(response.getErrorMessage());
        }

    }

    @SuppressWarnings("unchecked")
    public Set<ResourcePackageDetails> loadDependencies(int requestId, Set<PackageDetailsKey> keys) {
        Set<ResourcePackageDetails> dependencies = new HashSet<ResourcePackageDetails>();

        // Load the persisted request
        ContentServiceRequest persistedRequest = entityManager.find(ContentServiceRequest.class, requestId);

        // There is some inconsistency if the request is not in the database
        if (persistedRequest == null) {
            log.error("Could not find request with ID: " + requestId);
            return dependencies;
        }

        // Load the resource so we can get its type for the package version queries
        Resource resource = persistedRequest.getResource();
        ResourceType resourceType = resource.getResourceType();

        // For each package requested, load the package version and convert to a transfer object
        Date installationDate = new Date();
        for (PackageDetailsKey key : keys) {
            Query packageQuery = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            packageQuery.setParameter("packageName", key.getName());
            packageQuery.setParameter("packageTypeName", key.getPackageTypeName());
            packageQuery.setParameter("architectureName", key.getArchitectureName());
            packageQuery.setParameter("version", key.getVersion());
            packageQuery.setParameter("resourceTypeId", resourceType.getId());

            List persistedPackageList = packageQuery.getResultList();

            // If we don't know anything about the package, skip it
            if (persistedPackageList.size() == 0) {
                continue;
            }

            if (persistedPackageList.size() != 1) {
                log.error("Multiple packages found. Found: " + persistedPackageList.size() + " for key: " + key);
            }

            // Convert to transfer object to be sent to the agent
            PackageVersion packageVersion = (PackageVersion) persistedPackageList.get(0);
            ResourcePackageDetails details = ContentManagerHelper.packageVersionToDetails(packageVersion);
            dependencies.add(details);

            // Create an installed package history and attach to the request
            InstalledPackageHistory dependencyPackage = new InstalledPackageHistory();
            dependencyPackage.setContentServiceRequest(persistedRequest);
            dependencyPackage.setPackageVersion(packageVersion);
            dependencyPackage.setResource(resource);
            dependencyPackage.setStatus(InstalledPackageHistoryStatus.BEING_INSTALLED);
            dependencyPackage.setTimestamp(installationDate);

            persistedRequest.addInstalledPackageHistory(dependencyPackage);

            entityManager.persist(dependencyPackage);
        }

        return dependencies;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void failRequest(int requestId, Throwable error) {
        Query query = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID_WITH_INSTALLED_PKG_HIST);
        query.setParameter("id", requestId);

        ContentServiceRequest persistedRequest = (ContentServiceRequest) query.getSingleResult();
        Resource resource = persistedRequest.getResource();

        persistedRequest.setErrorMessageFromThrowable(error);
        persistedRequest.setStatus(ContentRequestStatus.FAILURE);

        // This should only be called as the result of an exception during the user initiated action. As such,
        // every package history entity represents an in progress state. Add a new entry for each in the failed state.
        Date timestamp = new Date();
        for (InstalledPackageHistory history : persistedRequest.getInstalledPackageHistory()) {
            InstalledPackageHistory failedEntry = new InstalledPackageHistory();
            failedEntry.setContentServiceRequest(persistedRequest);
            failedEntry.setDeploymentConfigurationValues(history.getDeploymentConfigurationValues());
            failedEntry.setErrorMessageFromThrowable(error);
            failedEntry.setPackageVersion(history.getPackageVersion());
            failedEntry.setResource(resource);
            failedEntry.setStatus(InstalledPackageHistoryStatus.FAILED);
            failedEntry.setTimestamp(timestamp);

            persistedRequest.addInstalledPackageHistory(failedEntry);
        }
    }

    @SuppressWarnings("unchecked")
    public void checkForTimedOutRequests(Subject subject) {
        if (!authorizationManager.isOverlord(subject)) {
            log.debug("Unauthorized user " + subject + " tried to execute checkForTimedOutRequests; "
                + "only the overlord may execute this system operation");
            return;
        }

        try {
            Query query = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_WITH_STATUS);
            query.setParameter("status", ContentRequestStatus.IN_PROGRESS);
            List<ContentServiceRequest> inProgressRequests = query.getResultList();

            if (inProgressRequests == null) {
                return;
            }

            Date timestamp = new Date();
            for (ContentServiceRequest request : inProgressRequests) {
                long duration = request.getDuration();

                // If the duration exceeds the timeout threshold, mark it as timed out
                if (duration > REQUEST_TIMEOUT) {
                    log.debug("Timing out request after duration: " + duration + " Request: " + request);

                    request.setErrorMessage("Request with duration " + duration + " exceeded the timeout threshold of "
                        + REQUEST_TIMEOUT);
                    request.setStatus(ContentRequestStatus.TIMED_OUT);

                    Resource resource = request.getResource();

                    // Need to add audit trail entries for each package as well, so the audit trail doesn't read
                    // as the operation is still being performed
                    Set<InstalledPackageHistory> requestPackages = request.getInstalledPackageHistory();
                    for (InstalledPackageHistory history : requestPackages) {
                        InstalledPackageHistoryStatus packageStatus = history.getStatus();

                        // Just to be safe, we're only going to "close out" any in progress entries. All entries in this
                        // list will likely be in this state, and we'd need to handle resubmissions differently anyway.
                        switch (packageStatus) {
                        case BEING_DELETED:
                        case BEING_INSTALLED:
                        case BEING_RETRIEVED:
                            InstalledPackageHistory closedHistory = new InstalledPackageHistory();
                            closedHistory.setContentServiceRequest(request);
                            closedHistory.setPackageVersion(history.getPackageVersion());
                            closedHistory.setResource(resource);
                            closedHistory.setStatus(InstalledPackageHistoryStatus.TIMED_OUT);
                            closedHistory.setTimestamp(timestamp);

                            entityManager.persist(closedHistory);
                            break;

                        default:
                            log.warn("Found a history entry on the request with an unexpected status. Id: "
                                + history.getId() + ", Status: " + packageStatus);
                            break;

                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Error while processing timed out requests", e);
        }
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public PackageVersion createPackageVersion(String packageName, int packageTypeId, String version,
        int architectureId, InputStream packageBitStream) {
        // See if the package version already exists and return that if it does
        Query packageVersionQuery = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH);
        packageVersionQuery.setParameter("name", packageName);
        packageVersionQuery.setParameter("packageTypeId", packageTypeId);
        packageVersionQuery.setParameter("architectureId", architectureId);
        packageVersionQuery.setParameter("version", version);

        // Result of the query should be either 0 or 1
        List existingVersionList = packageVersionQuery.getResultList();
        if (existingVersionList.size() > 0) {
            return (PackageVersion) existingVersionList.get(0);
        }

        // If the package doesn't exist, create that here
        Query packageQuery = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
        packageQuery.setParameter("name", packageName);
        packageQuery.setParameter("packageTypeId", packageTypeId);

        Package existingPackage = null;

        List existingPackageList = packageQuery.getResultList();

        if (existingPackageList.size() == 0) {
            PackageType packageType = entityManager.find(PackageType.class, packageTypeId);
            existingPackage = new Package(packageName, packageType);
            entityManager.persist(existingPackage);
        } else {
            existingPackage = (Package) existingPackageList.get(0);
        }

        // Create a package version and add it to the package
        Architecture architecture = entityManager.find(Architecture.class, architectureId);

        PackageVersion newPackageVersion = new PackageVersion(existingPackage, version, architecture);
        newPackageVersion.setDisplayName(existingPackage.getName());

        // Write the content into the newly created package version. This may eventually move, but for now we'll just
        // use the byte array in the package version to store the bits.
        byte[] packageBits;
        try {
            packageBits = StreamUtil.slurp(packageBitStream);
        } catch (RuntimeException re) {
            throw new RuntimeException("Error reading in the package file", re);
        }

        PackageBits bits = new PackageBits();
        bits.setBits(packageBits);

        newPackageVersion.setPackageBits(bits);

        entityManager.persist(newPackageVersion);

        existingPackage.addVersion(newPackageVersion);

        return newPackageVersion;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public PackageVersion persistPackageVersion(PackageVersion pv) {
        // EM.persist requires related entities to be attached, let's attach them now

        // package has persist cascade enabled, so skip loading it if we'll allow it to be created here
        if (pv.getGeneralPackage().getId() > 0) {
            pv.setGeneralPackage(entityManager.find(Package.class, pv.getGeneralPackage().getId()));
        }

        // arch has persist cascade enabled, so skip loading it if we'll allow it to be created here
        if (pv.getArchitecture().getId() > 0) {
            pv.setArchitecture(entityManager.find(Architecture.class, pv.getArchitecture().getId()));
        }

        // config is optional but has persist cascade enabled, so skip loading it if we'll allow it to be created here
        if (pv.getExtraProperties() != null && pv.getExtraProperties().getId() > 0) {
            pv.setExtraProperties(entityManager.find(Configuration.class, pv.getExtraProperties().getId()));
        }

        // our object's relations are now full attached, we can persist it
        entityManager.persist(pv);
        return pv;
    }

    public PackageVersion persistOrMergePackageVersionSafely(PackageVersion pv) {
        PackageVersion attached = null;
        RuntimeException error = null;

        try {
            attached = contentManager.persistPackageVersion(pv);
        } catch (RuntimeException re) {
            error = re;
        }

        // If not attached, the PV already exists, so we should be able to find it.
        if (attached == null) {
            Query q = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            q.setParameter("packageName", pv.getGeneralPackage().getName());
            q.setParameter("packageTypeName", pv.getGeneralPackage().getPackageType().getName());
            q.setParameter("architectureName", pv.getArchitecture().getName());
            q.setParameter("version", pv.getVersion());
            q.setParameter("resourceTypeId", pv.getGeneralPackage().getPackageType().getResourceType().getId());

            List<PackageVersion> found = q.getResultList();
            if (error != null && found.size() == 0) {
                throw error;
            }
            if (found.size() != 1) {
                throw new RuntimeException("Expecting 1 package version - got: " + found);
            }

            pv.setId(found.get(0).getId());
            attached = entityManager.merge(pv);

            log.warn("There was probably a very big and ugly EJB/hibernate error just above this log message - "
                + "you can normally ignore that. We detected that a package version was already created when we"
                + " tried to do it also - we will ignore this and just use the new package version that was "
                + "created in the other thread");
        }

        return attached;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Package persistPackage(Package pkg) {
        // EM.persist requires related entities to be attached, let's attach them now
        pkg.setPackageType(entityManager.find(PackageType.class, pkg.getPackageType().getId()));

        // our object's relations are now full attached, we can persist it
        entityManager.persist(pkg);
        return pkg;
    }

    public Package persistOrMergePackageSafely(Package pkg) {
        Package attached = null;
        RuntimeException error = null;

        try {
            attached = contentManager.persistPackage(pkg);
        } catch (RuntimeException re) {
            error = re;
        }

        // If not attached, the package already exists, so we should be able to find it.
        if (attached == null) {
            Query q = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
            q.setParameter("name", pkg.getName());
            q.setParameter("packageTypeId", pkg.getPackageType().getId());

            List<Package> found = q.getResultList();
            if (error != null && found.size() == 0) {
                throw error;
            }
            if (found.size() != 1) {
                throw new RuntimeException("Expecting 1 package - got: " + found);
            }
            pkg.setId(found.get(0).getId());
            attached = entityManager.merge(pkg);

            log.warn("There was probably a very big and ugly EJB/hibernate error just above this log message - "
                + "you can normally ignore that. We detected that a package was already created when we"
                + " tried to do it also - we will ignore this and just use the new package that was "
                + "created in the other thread");
        }

        return attached;
    }

    // Private  --------------------------------------------

    private ContentRequestStatus translateRequestResultStatus(ContentResponseResult result) {
        switch (result) {
        case SUCCESS: {
            return ContentRequestStatus.SUCCESS;
        }

        default: {
            return ContentRequestStatus.FAILURE;
        }
        }
    }

    /**
     * Translates the transfer object representation of package deployment steps into domain entities.
     *
     * @param transferSteps cannot be <code>null</code>
     * @param history       history item the steps are a part of, this will be used when creating the domain entities
     *                      to establish the relationship
     * @return list of domain entities
     */
    private List<PackageInstallationStep> translateInstallationSteps(List<DeployPackageStep> transferSteps,
        InstalledPackageHistory history) {
        List<PackageInstallationStep> steps = new ArrayList<PackageInstallationStep>(transferSteps.size());
        int stepOrder = 0;

        for (DeployPackageStep transferStep : transferSteps) {
            PackageInstallationStep step = new PackageInstallationStep();
            step.setDescription(transferStep.getDescription());
            step.setKey(transferStep.getStepKey());
            step.setResult(transferStep.getStepResult());
            step.setErrorMessage(transferStep.getStepErrorMessage());
            step.setOrder(stepOrder++);
            step.setInstalledPackageHistory(history);

            steps.add(step);
        }

        return steps;
    }
}