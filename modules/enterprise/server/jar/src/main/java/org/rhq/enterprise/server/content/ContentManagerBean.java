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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.clientapi.server.content.DeletePackagesRequest;
import org.rhq.core.clientapi.server.content.DeployPackagesRequest;
import org.rhq.core.clientapi.server.content.RetrievePackageBitsRequest;
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
import org.rhq.core.domain.content.PackageBitsBlob;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * EJB that handles content subsystem interaction with resources, including content discovery reports and create/delete
 * functionality.
 *
 * @author Jason Dobies
 */
@Stateless
public class ContentManagerBean implements ContentManagerLocal, ContentManagerRemote {
    // Constants  --------------------------------------------

    /**
     * Amount of time a request may be outstanding against an agent before it is marked as timed out.
     */
    private static final int REQUEST_TIMEOUT = 1000 * 60 * 60;

    public static final String UPLOAD_FILE_SIZE = "fileSize";
    public static final String UPLOAD_FILE_INSTALL_DATE = "fileInstallDate";
    public static final String UPLOAD_OWNER = "owner";
    public static final String UPLOAD_FILE_NAME = "fileName";
    public static final String UPLOAD_MD5 = "md5";
    public static final String UPLOAD_SHA256 = "sha256";

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private ContentManagerLocal contentManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    // ContentManagerLocal Implementation  --------------------------------------------

    @SuppressWarnings("unchecked")
    public void mergeDiscoveredPackages(ContentDiscoveryReport report) {
        int resourceId = report.getResourceId();

        // For performance tracking
        long start = System.currentTimeMillis();

        log.debug("Merging [" + report.getDeployedPackages().size() + "] packages for Resource with id [" + resourceId
            + "]...");

        // Load the resource and its installed packages
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            log.error("Invalid resource ID specified for merge. Resource ID: " + resourceId);
            return;
        }

        // Timestamp to use for all audit trail entries from this report
        long timestamp = System.currentTimeMillis();

        // Before we process the report, get a list of all installed packages on the resource.
        // InstalledPackage objects in this list that are not referenced in the report are to be removed.
        Query currentInstalledPackageQuery = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_BY_RESOURCE_ID);
        currentInstalledPackageQuery.setParameter("resourceId", resource.getId());

        Set<InstalledPackage> doomedPackages = new HashSet<InstalledPackage>(currentInstalledPackageQuery
            .getResultList());

        // The report contains an entire snapshot of packages, so each of these has to be represented
        // as an InstalledPackage
        for (ResourcePackageDetails discoveredPackage : report.getDeployedPackages()) {

            Package generalPackage = null;
            PackageVersion packageVersion = null;

            // Load the overall package (used in a few places later in this loop)
            Query packageQuery = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_RESOURCE_TYPE);
            packageQuery.setFlushMode(FlushModeType.COMMIT);
            packageQuery.setParameter("name", discoveredPackage.getName());
            packageQuery.setParameter("packageTypeName", discoveredPackage.getPackageTypeName());
            packageQuery.setParameter("resourceTypeId", resource.getResourceType().getId());
            List<Package> resultPackages = packageQuery.getResultList();
            if (resultPackages.size() > 0) {
                generalPackage = resultPackages.get(0);
            }

            // If the package exists see if package version already exists
            if (null != generalPackage) {
                Query packageVersionQuery = entityManager
                    .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS);
                packageVersionQuery.setFlushMode(FlushModeType.COMMIT);
                packageVersionQuery.setParameter("packageName", discoveredPackage.getName());
                packageVersionQuery.setParameter("packageTypeName", discoveredPackage.getPackageTypeName());
                packageVersionQuery.setParameter("resourceTypeId", resource.getResourceType().getId());
                packageVersionQuery.setParameter("architectureName", discoveredPackage.getArchitectureName());
                packageVersionQuery.setParameter("version", discoveredPackage.getVersion());
                packageVersionQuery.setParameter("sha", discoveredPackage.getSHA256());
                List<PackageVersion> resultPackageVersions = packageVersionQuery.getResultList();
                if (resultPackageVersions.size() > 0) {
                    packageVersion = resultPackageVersions.get(0);
                }
            }

            // If we didn't find a package version for this deployed package, we will need to create it
            if (null == packageVersion) {
                if (null == generalPackage) {
                    Query packageTypeQuery = entityManager
                        .createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME);
                    packageTypeQuery.setFlushMode(FlushModeType.COMMIT);
                    packageTypeQuery.setParameter("typeId", resource.getResourceType().getId());
                    packageTypeQuery.setParameter("name", discoveredPackage.getPackageTypeName());

                    PackageType packageType = (PackageType) packageTypeQuery.getSingleResult();

                    generalPackage = new Package(discoveredPackage.getName(), packageType);
                    generalPackage = persistOrMergePackageSafely(generalPackage);
                }

                // Create a new package version and attach to the general package
                Query architectureQuery = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
                architectureQuery.setFlushMode(FlushModeType.COMMIT);
                architectureQuery.setParameter("name", discoveredPackage.getArchitectureName());

                Architecture packageArchitecture;

                // We don't have an architecture enum, so it's very possible the plugin will pass in a crap string here.
                // Catch and log a better error message but continue processing the rest of the report
                // TODO: if arch is "none" we should consider manually switching it to be our standard "noarch"
                try {
                    packageArchitecture = (Architecture) architectureQuery.getSingleResult();
                } catch (Exception e) {
                    log.warn("Could not load architecture for architecture name ["
                        + discoveredPackage.getArchitectureName() + "] for package [" + discoveredPackage.getName()
                        + "]. Cause: " + ThrowableUtil.getAllMessages(e));
                    continue;
                }

                packageVersion = new PackageVersion(generalPackage, discoveredPackage.getVersion(), packageArchitecture);
                packageVersion.setDisplayName(discoveredPackage.getDisplayName());
                packageVersion.setDisplayVersion(discoveredPackage.getDisplayVersion());
                packageVersion.setFileCreatedDate(discoveredPackage.getFileCreatedDate());
                packageVersion.setFileName(discoveredPackage.getFileName());
                packageVersion.setFileSize(discoveredPackage.getFileSize());
                packageVersion.setLicenseName(discoveredPackage.getLicenseName());
                packageVersion.setLicenseVersion(discoveredPackage.getLicenseVersion());
                packageVersion.setLongDescription(discoveredPackage.getLongDescription());
                packageVersion.setMD5(discoveredPackage.getMD5());
                packageVersion.setMetadata(discoveredPackage.getMetadata());
                packageVersion.setSHA256(discoveredPackage.getSHA256());
                packageVersion.setShortDescription(discoveredPackage.getShortDescription());
                packageVersion.setExtraProperties(discoveredPackage.getExtraProperties());

                packageVersion = persistOrMergePackageVersionSafely(packageVersion);
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
            newlyInstalledPackage.setInstallationDate(discoveredPackage.getInstallationTimestamp());

            entityManager.persist(newlyInstalledPackage);

            // Create an audit trail entry to show how this package was added to the system
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setDeploymentConfigurationValues(discoveredPackage.getDeploymentTimeConfiguration());
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setStatus(InstalledPackageHistoryStatus.DISCOVERED);
            history.setTimestamp(timestamp);

            entityManager.persist(history);

            entityManager.flush();
        } // end resource package loop

        // For any previously active installed packages that were not found again (and thus removed from the doomed
        // list), delete them.
        int deletedPackages = 0;
        for (InstalledPackage doomedPackage : doomedPackages) {
            doomedPackage = entityManager.find(InstalledPackage.class, doomedPackage.getId());

            // Add an audit trail entry to indicate the package was not rediscovered
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setPackageVersion(doomedPackage.getPackageVersion());
            history.setResource(resource);
            history.setStatus(InstalledPackageHistoryStatus.MISSING);
            history.setTimestamp(timestamp);
            entityManager.persist(history);

            entityManager.remove(doomedPackage);

            // no idea if this helps, but if we are deleting large numbers of packages, it probably does
            if ((++deletedPackages) % 100 == 0) {
                entityManager.flush();
            }
        }

        log.debug("Finished merging [" + report.getDeployedPackages().size() + "] packages in "
            + (System.currentTimeMillis() - start) + "ms");
    }

    public void deployPackages(Subject user, int[] resourceIds, int[] packageVersionIds) {
        for (int resourceId : resourceIds) {
            Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();
            for (int packageVersionId : packageVersionIds) {
                PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);
                if (packageVersion == null) {
                    throw new IllegalArgumentException("PackageVersion: [" + packageVersionId + "] not found!");
                }

                ResourcePackageDetails details = ContentManagerHelper.packageVersionToDetails(packageVersion);
                details.setInstallationTimestamp(System.currentTimeMillis());
                packages.add(details);
            }

            deployPackages(user, resourceId, packages, null);
        }
    }

    public void deployPackages(Subject user, int resourceId, Set<ResourcePackageDetails> packages, String requestNotes) {
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
            packages, requestNotes);

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
        Set<ResourcePackageDetails> packages, String notes) {
        Resource resource = entityManager.find(Resource.class, resourceId);

        ContentServiceRequest persistedRequest = new ContentServiceRequest(resource, username,
            ContentRequestType.DEPLOY);
        persistedRequest.setStatus(ContentRequestStatus.IN_PROGRESS);
        persistedRequest.setNotes(notes);

        long timestamp = System.currentTimeMillis();

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
        Query query = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID);
        query.setParameter("id", response.getRequestId());
        ContentServiceRequest persistedRequest = (ContentServiceRequest) query.getSingleResult();
        Resource resource = persistedRequest.getResource();

        int resourceTypeId = persistedRequest.getResource().getResourceType().getId();

        // Update the persisted request
        persistedRequest.setErrorMessage(response.getOverallRequestErrorMessage());
        persistedRequest.setStatus(translateRequestResultStatus(response.getOverallRequestResult()));

        // All history entries on the request at this point should be considered "in progress". We need to make
        // sure each of these is closed out in some capacity. Typically, this will be done by the response
        // explicitly indicating the result of each individual package. However, we can't rely on the plugin
        // always doing this, so we need to keep track of which ones are not closed to prevent dangling in progress
        // entries.
        Set<InstalledPackageHistory> requestInProgressEntries = persistedRequest.getInstalledPackageHistory();

        // Convert to a map so we can easily remove entries from it as they are closed by the individual
        // package responses.
        Map<PackageVersion, InstalledPackageHistory> inProgressEntries = new HashMap<PackageVersion, InstalledPackageHistory>(
            requestInProgressEntries.size());

        for (InstalledPackageHistory history : requestInProgressEntries) {
            inProgressEntries.put(history.getPackageVersion(), history);
        }

        // Handle each individual package
        long timestamp = System.currentTimeMillis();

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
                deploymentConfiguration = deploymentConfiguration.deepCopy(false);
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

            // We're closing out the package request for this package version, so remove it from the cache of entries
            // that need to be closed
            inProgressEntries.remove(packageVersion);
        }

        // For any entries that were not closed, add closing entries
        for (InstalledPackageHistory unclosed : inProgressEntries.values()) {
            PackageVersion packageVersion = unclosed.getPackageVersion();

            // Create the history entity
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setContentServiceRequest(persistedRequest);
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setTimestamp(timestamp);

            // One option is to create a new status that indicates unknown. For now, just give them the same result
            // as the overall request result
            if (response.getOverallRequestResult() == ContentResponseResult.SUCCESS) {
                history.setStatus(InstalledPackageHistoryStatus.INSTALLED);
            } else {
                history.setStatus(InstalledPackageHistoryStatus.FAILED);
            }

            entityManager.persist(history);
            persistedRequest.addInstalledPackageHistory(history);
        }
    }

    public void deletePackages(Subject user, int[] resourceIds, int[] installedPackageIds) {
        for (int resourceId : resourceIds) {
            deletePackages(user, resourceId, installedPackageIds, null);
        }
    }

    @SuppressWarnings("unchecked")
    public void deletePackages(Subject user, int resourceId, int[] installedPackageIds, String requestNotes) {
        if (installedPackageIds == null) {
            throw new IllegalArgumentException("installedPackages cannot be null");
        }

        log.info("Deleting " + installedPackageIds.length + " from resource ID [" + resourceId + "]");

        if (installedPackageIds.length == 0) {
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
            installedPackageIds, requestNotes);

        // Package into transfer object
        Query query = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_BY_SET_OF_IDS);
        query.setParameter("packageIds", ArrayUtils.wrapInList(installedPackageIds));

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
    public ContentServiceRequest createRemoveRequest(int resourceId, String username, int[] installedPackageIds,
        String requestNotes) {
        Resource resource = entityManager.find(Resource.class, resourceId);

        ContentServiceRequest persistedRequest = new ContentServiceRequest(resource, username,
            ContentRequestType.DELETE);
        persistedRequest.setStatus(ContentRequestStatus.IN_PROGRESS);
        persistedRequest.setNotes(requestNotes);

        long timestamp = System.currentTimeMillis();

        for (int installedPackageId : installedPackageIds) {
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

    public void completeDeletePackageRequest(RemovePackagesResponse response) {
        log.info("Completing delete package response: " + response);

        // Load persisted request
        Query query = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID);
        query.setParameter("id", response.getRequestId());
        ContentServiceRequest persistedRequest = (ContentServiceRequest) query.getSingleResult();

        Resource resource = persistedRequest.getResource();
        int resourceTypeId = resource.getResourceType().getId();

        // Update the persisted request
        persistedRequest.setErrorMessage(response.getOverallRequestErrorMessage());
        persistedRequest.setStatus(translateRequestResultStatus(response.getOverallRequestResult()));

        // All history entries on the request at this point should be considered "in progress". We need to make
        // sure each of these is closed out in some capacity. Typically, this will be done by the response
        // explicitly indicating the result of each individual package. However, we can't rely on the plugin
        // always doing this, so we need to keep track of which ones are not closed to prevent dangling in progress
        // entries.
        Set<InstalledPackageHistory> requestInProgressEntries = persistedRequest.getInstalledPackageHistory();

        // Convert to a map so we can easily remove entries from it as they are closed by the individual
        // package responses.
        Map<PackageVersion, InstalledPackageHistory> inProgressEntries = new HashMap<PackageVersion, InstalledPackageHistory>(
            requestInProgressEntries.size());

        for (InstalledPackageHistory history : requestInProgressEntries) {
            inProgressEntries.put(history.getPackageVersion(), history);
        }

        // Handle each individual package
        long timestamp = System.currentTimeMillis();

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

            entityManager.persist(history);
            persistedRequest.addInstalledPackageHistory(history);

            // We're closing out the package request for this package version, so remove it from the cache of entries
            // that need to be closed
            inProgressEntries.remove(packageVersion);
        }

        // For any entries that were not closed, add closing entries
        for (InstalledPackageHistory unclosed : inProgressEntries.values()) {
            PackageVersion packageVersion = unclosed.getPackageVersion();

            // Create the history entity
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setContentServiceRequest(persistedRequest);
            history.setPackageVersion(packageVersion);
            history.setResource(resource);
            history.setTimestamp(timestamp);

            // One option is to create a new status that indicates unknown. For now, just give them the same result
            // as the overall request result
            if (response.getOverallRequestResult() == ContentResponseResult.SUCCESS) {
                history.setStatus(InstalledPackageHistoryStatus.DELETED);
            } else {
                history.setStatus(InstalledPackageHistoryStatus.FAILED);
            }

            entityManager.persist(history);
            persistedRequest.addInstalledPackageHistory(history);
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

    public byte[] getPackageBytes(Subject user, int resourceId, int installedPackageId) {
        // Check permissions first
        if (!authorizationManager.hasResourcePermission(user, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("User [" + user.getName()
                + "] does not have permission to obtain package content for installed package id ["
                + installedPackageId + "] for resource ID [" + resourceId + "]");
        }
        try {
            InstalledPackage installedPackage = entityManager.find(InstalledPackage.class, installedPackageId);
            PackageBits bits = installedPackage.getPackageVersion().getPackageBits();
            if (bits == null || bits.getBlob().getBits().length == 0) {
                long start = System.currentTimeMillis();
                retrieveBitsFromResource(user, resourceId, installedPackageId);

                bits = installedPackage.getPackageVersion().getPackageBits();
                while ((bits == null || bits.getBlob().getBits() == null)
                    && (System.currentTimeMillis() - start < 30000)) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    entityManager.clear();
                    installedPackage = entityManager.find(InstalledPackage.class, installedPackageId);
                    bits = installedPackage.getPackageVersion().getPackageBits();
                }

                if (bits == null) {
                    throw new RuntimeException("Unable to retrieve package bits for resource: " + resourceId
                        + " and package: " + installedPackageId + " before timeout.");
                }

            }

            return bits.getBlob().getBits();
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve package bits for resource: " + resourceId + " and package: "
                + installedPackageId + " before timeout.");

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
            ContentRequestType.GET_BITS);
        persistedRequest.setStatus(ContentRequestStatus.IN_PROGRESS);

        long timestamp = System.currentTimeMillis();

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

    @TransactionTimeout(45 * 60)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void completeRetrievePackageBitsRequest(ContentServiceResponse response, InputStream bitStream) {
        log.info("Completing retrieve package bits response: " + response);

        // Load persisted request
        ContentServiceRequest persistedRequest = entityManager.find(ContentServiceRequest.class, response
            .getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (persistedRequest == null) {
            log
                .error("Attempting to complete a request that was not found in the database: "
                    + response.getRequestId());
            return;
        }
        Resource resource = persistedRequest.getResource();

        InstalledPackageHistory initialRequestHistory = persistedRequest.getInstalledPackageHistory().iterator().next();
        PackageVersion packageVersion = initialRequestHistory.getPackageVersion();

        if (response.getStatus() == ContentRequestStatus.SUCCESS) {
            // Read the stream from the agent and store in the package version
            try {
                log.debug("Saving content for response: " + response);

                PackageBits packageBits = initializePackageBits(null);

                // Could use the following, but only on jdk6 as builds
                // @since 1.6
                // void setBinaryStream(int parameterIndex, java.io.InputStream x) throws SQLException;

                Long length = packageVersion.getFileSize();
                if (length == null) {
                    File tmpFile = File.createTempFile("rhq", ".stream");
                    FileOutputStream fos = new FileOutputStream(tmpFile);
                    length = StreamUtil.copy(bitStream, fos, true);

                    bitStream = new FileInputStream(tmpFile);
                }
                Connection conn = null;
                PreparedStatement ps = null;

                try {
                    PackageBits bits = entityManager.find(PackageBits.class, packageBits.getId());
                    String pkgName = "(set packageName)";
                    if ((packageVersion != null) && (packageVersion.getGeneralPackage() != null)) {
                        //update it to whatever package name is if we can get to it.
                        pkgName = packageVersion.getGeneralPackage().getName();
                    }
                    bits = loadPackageBits(bitStream, packageVersion.getId(), pkgName, packageVersion.getVersion(),
                        bits, null);

                    entityManager.merge(bits);
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

            } catch (Exception e) {
                log.error("Error while reading content from agent stream", e);
                // TODO: don't want to throw exception here? does the tx rollback automatically anyway?
            }
        }

        // Update the persisted request
        persistedRequest.setErrorMessage(response.getErrorMessage());
        persistedRequest.setStatus(response.getStatus());

        // Add a new audit trail entry
        InstalledPackageHistory completedHistory = new InstalledPackageHistory();
        completedHistory.setContentServiceRequest(persistedRequest);
        completedHistory.setResource(resource);
        completedHistory.setTimestamp(System.currentTimeMillis());
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
        long installationDate = System.currentTimeMillis();

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
        Query query = entityManager.createNamedQuery(ContentServiceRequest.QUERY_FIND_BY_ID);
        query.setParameter("id", requestId);

        ContentServiceRequest persistedRequest = (ContentServiceRequest) query.getSingleResult();
        Resource resource = persistedRequest.getResource();

        persistedRequest.setErrorMessage(ThrowableUtil.getStackAsString(error));
        persistedRequest.setStatus(ContentRequestStatus.FAILURE);

        // This should only be called as the result of an exception during the user initiated action. As such,
        // every package history entity represents an in progress state. Add a new entry for each in the failed state.
        long timestamp = System.currentTimeMillis();

        for (InstalledPackageHistory history : persistedRequest.getInstalledPackageHistory()) {
            InstalledPackageHistory failedEntry = new InstalledPackageHistory();
            failedEntry.setContentServiceRequest(persistedRequest);
            failedEntry.setDeploymentConfigurationValues(history.getDeploymentConfigurationValues());
            failedEntry.setErrorMessage(ThrowableUtil.getStackAsString(error));
            failedEntry.setPackageVersion(history.getPackageVersion());
            failedEntry.setResource(resource);
            failedEntry.setStatus(InstalledPackageHistoryStatus.FAILED);
            failedEntry.setTimestamp(timestamp);

            persistedRequest.addInstalledPackageHistory(failedEntry);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Architecture> findArchitectures(Subject subject) {
        Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_ALL);
        List<Architecture> architectures = q.getResultList();

        return architectures;
    }

    public Architecture getNoArchitecture() {
        Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
        q.setParameter("name", "noarch");
        Architecture architecture = (Architecture) q.getSingleResult();

        return architecture;
    }

    @SuppressWarnings("unchecked")
    public List<PackageType> findPackageTypes(Subject subject, String resourceTypeName, String pluginName)
        throws ResourceTypeNotFoundException {

        ResourceType rt = resourceTypeManager.getResourceTypeByNameAndPlugin(subject, resourceTypeName, pluginName);
        if (null == rt) {
            throw new ResourceTypeNotFoundException(resourceTypeName);
        }

        Query query = entityManager.createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID);
        query.setParameter("typeId", rt.getId());
        List<PackageType> result = query.getResultList();

        return result;
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

            long timestamp = System.currentTimeMillis();

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

    public PackageVersion createPackageVersion(Subject subject, String packageName, int packageTypeId, String version,
        Integer architectureId, byte[] packageBytes) {

        // Check permissions first
        if (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_CONTENT)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to create package versions");
        }

        return createPackageVersion(packageName, packageTypeId, version, (null == architectureId) ? getNoArchitecture()
            .getId() : architectureId, new ByteArrayInputStream(packageBytes));
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

        Package existingPackage;

        List existingPackageList = packageQuery.getResultList();

        if (existingPackageList.size() == 0) {
            PackageType packageType = entityManager.find(PackageType.class, packageTypeId);
            existingPackage = new Package(packageName, packageType);
            existingPackage = persistOrMergePackageSafely(existingPackage);
        } else {
            existingPackage = (Package) existingPackageList.get(0);
        }

        // Create a package version and add it to the package
        Architecture architecture = entityManager.find(Architecture.class, architectureId);

        PackageVersion newPackageVersion = new PackageVersion(existingPackage, version, architecture);
        newPackageVersion.setDisplayName(existingPackage.getName());
        entityManager.persist(newPackageVersion);

        Map<String, String> contentDetails = new HashMap<String, String>();
        PackageBits bits = loadPackageBits(packageBitStream, newPackageVersion.getId(), packageName, version, null,
            contentDetails);

        newPackageVersion.setPackageBits(bits);
        newPackageVersion.setFileSize(Long.valueOf(contentDetails.get(UPLOAD_FILE_SIZE)).longValue());
        newPackageVersion.setSHA256(contentDetails.get(UPLOAD_SHA256));
        newPackageVersion = persistOrMergePackageVersionSafely(newPackageVersion);

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

    @SuppressWarnings("unchecked")
    public PackageVersion persistOrMergePackageVersionSafely(PackageVersion pv) {
        PackageVersion persisted = null;
        RuntimeException error = null;

        try {
            if (pv.getId() == 0) {
                persisted = contentManager.persistPackageVersion(pv);
            }
        } catch (RuntimeException re) {
            error = re;
        }

        // If we didn't persist, the PV already exists, so we should be able to find it.
        if (persisted == null) {
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
                throw new RuntimeException("Expecting 1 package version matching [" + pv + "] but got: " + found);
            }

            pv.setId(found.get(0).getId());
            persisted = entityManager.merge(pv);

            if (error != null) {
                log.warn("There was probably a very big and ugly EJB/hibernate error just above this log message - "
                    + "you can normally ignore that. We detected that a package version was already created when we"
                    + " tried to do it also - we will ignore this and just use the new package version that was "
                    + "created in the other thread", new Throwable("Stack Trace:"));
            }
        } else {
            // the persisted object is unattached right now,
            // we want it attached so the caller always has an attached entity returned to it 
            persisted = entityManager.find(PackageVersion.class, persisted.getId());
            persisted.getGeneralPackage().getId();
            persisted.getArchitecture().getId();
            if (persisted.getExtraProperties() != null) {
                persisted.getExtraProperties().getId();
            }
        }

        return persisted;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Package persistPackage(Package pkg) {
        // EM.persist requires related entities to be attached, let's attach them now
        pkg.setPackageType(entityManager.find(PackageType.class, pkg.getPackageType().getId()));

        // our object's relations are now full attached, we can persist it
        entityManager.persist(pkg);
        return pkg;
    }

    @SuppressWarnings("unchecked")
    public Package persistOrMergePackageSafely(Package pkg) {
        Package persisted = null;
        RuntimeException error = null;

        try {
            if (pkg.getId() == 0) {
                persisted = contentManager.persistPackage(pkg);
            }
        } catch (RuntimeException re) {
            error = re;
        }

        // If we didn't persist, the package already exists, so we should be able to find it.
        if (persisted == null) {
            Query q = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
            q.setParameter("name", pkg.getName());
            q.setParameter("packageTypeId", pkg.getPackageType().getId());

            List<Package> found = q.getResultList();
            if (error != null && found.size() == 0) {
                throw error;
            }
            if (found.size() != 1) {
                throw new RuntimeException("Expecting 1 package matching [" + pkg + "] but got: " + found);
            }
            pkg.setId(found.get(0).getId());
            persisted = entityManager.merge(pkg);

            if (error != null) {
                log.warn("There was probably a very big and ugly EJB/hibernate error just above this log message - "
                    + "you can normally ignore that. We detected that a package was already created when we"
                    + " tried to do it also - we will ignore this and just use the new package that was "
                    + "created in the other thread");
            }
        } else {
            // the persisted object is unattached right now,
            // we want it attached so the caller always has an attached entity returned to it 
            persisted = entityManager.find(Package.class, persisted.getId());
            persisted.getPackageType().getId();
        }

        return persisted;
    }

    public PackageType getResourceCreationPackageType(int resourceTypeId) {
        Query query = entityManager.createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_CREATION_FLAG);
        query.setParameter("typeId", resourceTypeId);

        PackageType packageType = (PackageType) query.getSingleResult();
        return packageType;
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

    @SuppressWarnings("unchecked")
    public List<String> findInstalledPackageVersions(Subject user, int resourceId) {
        Query query = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_PACKAGE_LIST_VERSIONS);
        query.setParameter("resourceId", resourceId);

        List<String> packages = query.getResultList();
        return packages;
    }

    @SuppressWarnings("unchecked")
    public PageList<InstalledPackage> findInstalledPackagesByCriteria(Subject subject, InstalledPackageCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        if (!authorizationManager.isInventoryManager(subject)) {
            // Ensure we limit to packages installed to viewable resources
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "resource", subject.getId());
        }

        CriteriaQueryRunner<InstalledPackage> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);

        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    public PageList<PackageVersion> findPackageVersionsByCriteria(Subject subject, PackageVersionCriteria criteria) {

        Integer resourceId = criteria.getFilterResourceId();

        if (!authorizationManager.isInventoryManager(subject)) {
            if ((null == resourceId) || criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            } else if (!authorizationManager.canViewResource(subject, resourceId)) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] does not have permission to view the specified resource.");
            }
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        CriteriaQueryRunner<PackageVersion> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);

        return queryRunner.execute();
    }

    public InstalledPackage getBackingPackageForResource(Subject subject, int resourceId) {
        InstalledPackage result = null;

        InstalledPackageCriteria criteria = new InstalledPackageCriteria();
        criteria.addFilterResourceId(resourceId);
        PageList<InstalledPackage> ips = findInstalledPackagesByCriteria(subject, criteria);

        // should not be more than 1
        if ((null != ips) && (1 == ips.size())) {
            result = ips.get(0);

            // fetch these
            result.getPackageVersion().getGeneralPackage().getId();
            result.getPackageVersion().getGeneralPackage().getPackageType().getId();
            result.getPackageVersion().getArchitecture().getId();
        }

        return result;
    }

    /** Does much of same functionality as createPackageVersion, but uses same named query
     *  as the agent side discovery mechanism, and passes in additional parameters available
     *  when file has been uploaded via the UI.
     */
    @SuppressWarnings("unchecked")
    public PackageVersion getUploadedPackageVersion(String packageName, int packageTypeId, String version,
        int architectureId, InputStream packageBitStream, Map<String, String> packageUploadDetails,
        int newResourceTypeId) {

        PackageVersion packageVersion = null;

        //default version to 1.0 if is null, not provided for any reason.
        if ((version == null) || (version.trim().length() == 0)) {
            version = "1.0";
        }

        // See if package version already exists for the resource package
        Query packageVersionQuery = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
        packageVersionQuery.setFlushMode(FlushModeType.COMMIT);
        packageVersionQuery.setParameter("packageName", packageName);
        PackageType packageType = contentManager.getResourceCreationPackageType(newResourceTypeId);
        packageVersionQuery.setParameter("packageTypeName", packageType.getName());
        packageVersionQuery.setParameter("resourceTypeId", newResourceTypeId);

        Architecture architecture = entityManager.find(Architecture.class, architectureId);
        packageVersionQuery.setParameter("architectureName", architecture.getName());
        packageVersionQuery.setParameter("version", version);

        // Result of the query should be either 0 or 1
        List<PackageVersion> existingPackageVersionList = packageVersionQuery.getResultList();

        if (existingPackageVersionList.size() > 0) {
            packageVersion = existingPackageVersionList.get(0);
        }

        Package existingPackage = null;

        Query packageQuery = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
        packageQuery.setParameter("name", packageName);
        packageQuery.setParameter("packageTypeId", packageTypeId);
        List<Package> existingPackageList = packageQuery.getResultList();

        if (existingPackageList.size() == 0) {
            // If the package doesn't exist, create that here
            existingPackage = new Package(packageName, packageType);
            existingPackage = persistOrMergePackageSafely(existingPackage);
        } else {
            existingPackage = existingPackageList.get(0);
        }

        //initialize package version if not already
        if (packageVersion == null) {
            packageVersion = new PackageVersion(existingPackage, version, architecture);
            packageVersion.setDisplayName(existingPackage.getName());
            entityManager.persist(packageVersion);
        }

        //get the data
        PackageBits bits = loadPackageBits(packageBitStream, packageVersion.getId(), packageName, version, null, null);

        packageVersion.setPackageBits(bits);

        //populate extra details, persist
        if (packageUploadDetails != null) {
            packageVersion.setFileCreatedDate(Long.valueOf(packageUploadDetails
                .get(ContentManagerBean.UPLOAD_FILE_INSTALL_DATE)));
            packageVersion.setFileName(packageUploadDetails.get(ContentManagerBean.UPLOAD_FILE_NAME));
            packageVersion.setFileSize(Long.valueOf(packageUploadDetails.get(ContentManagerBean.UPLOAD_FILE_SIZE)));
            packageVersion.setMD5(packageUploadDetails.get(ContentManagerBean.UPLOAD_MD5));
            packageVersion.setSHA256(packageUploadDetails.get(ContentManagerBean.UPLOAD_SHA256));
        }

        entityManager.merge(packageVersion);
        entityManager.flush();

        return packageVersion;

    }

    /** Pulls in package bits from the stream. Currently inefficient.
     *
     * @param packageBitStream
     * @param packageVersionId
     * @param contentDetails 
     * @return PackageBits ref populated.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private PackageBits loadPackageBits(InputStream packageBitStream, int packageVersionId, String packageName,
        String packageVersion, PackageBits existingBits, Map<String, String> contentDetails) {

        // use existing or instantiate PackageBits instance.
        PackageBits bits = (null == existingBits) ? initializePackageBits(null) : existingBits;

        //locate related packageVersion
        PackageVersion pv = entityManager.find(PackageVersion.class, packageVersionId);

        //associate the two if located.
        if (pv != null) {//np check.
            pv.setPackageBits(bits);
            entityManager.flush();
        }

        //write data from stream into db using Hibernate Blob mechanism
        updateBlobStream(packageBitStream, bits, contentDetails);

        return bits;
    }

    /**
     * This creates a new PackageBits entity initialized to EMPTY_BLOB for the associated PackageBitsBlob.
     * Note that PackageBits and PackageBitsBlob are two entities that *share* the same db row.  This is
     * done to allow for Lazy load semantics on the Lob.  Hibernate does not honor field-level Lazy load
     * on a Lob unless the entity class is instrumented. We can't usethat approach because it introduces
     * hibernate imports into the domain class, and that violates our restriction of exposing hibernate
     * classes to the Agent and Remote clients.
     * 
     * @return
     */
    private PackageBits initializePackageBits(PackageBits bits) {
        if (null == bits) {
            PackageBitsBlob blob = null;

            // We have to work backwards to avoid constraint violations. PackageBits requires a PackageBitsBlob,
            // so create and persist that first, getting the ID
            blob = new PackageBitsBlob();
            blob.setBits(PackageBits.EMPTY_BLOB.getBytes());
            entityManager.persist(blob);

            // Now create the PackageBits entity and assign the Id and blob.  Note, do not persist the
            // entity, the row already exists. Just perform and flush the update.
            bits = new PackageBits();
            bits.setId(blob.getId());
            bits.setBlob(blob);
        } else {
            PackageBitsBlob blob = entityManager.find(PackageBitsBlob.class, bits.getId());
            // don't bother testing for null, that may pull a large blob, just make sure it's not null
            blob.setBits(PackageBits.EMPTY_BLOB.getBytes());
        }

        // write to the db and return the new PackageBits and associated PackageBitsBlob
        entityManager.flush();
        return bits;
    }

    /** Takes an input stream and copies it into the PackageBits table using Hibernate
     *  Blob mechanism with PreparedStatements.  As all content into Bits are not stored as type OID, t
     *
     * @param stream
     * @param contentDetails Map to store content details in used in PackageVersioning
     */
    @SuppressWarnings("unused")
    public void updateBlobStream(InputStream stream, PackageBits bits, Map<String, String> contentDetails) {

        //TODO: are there any db specific limits that we should check/verify here before stuffing
        // the contents of a stream into the db? Should we just let the db complain and take care of
        // input validation?
        if (stream == null) {
            return; // no stream content to update.
        }

        bits = initializePackageBits(bits);

        //locate the existing PackageBitsBlob instance
        bits = entityManager.find(PackageBits.class, bits.getId());
        PackageBitsBlob blob = bits.getBlob();

        //Create prepared statements to work with Blobs and hibernate.
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            conn = dataSource.getConnection();

            //we are loading the PackageBits saved in the previous step
            //we need to lock the row which will be updated so we are using FOR UPDATE
            ps = conn.prepareStatement("SELECT BITS FROM " + PackageBits.TABLE_NAME + " WHERE ID = ? FOR UPDATE");
            ps.setInt(1, bits.getId());
            ResultSet rs = ps.executeQuery();
            if (rs != null) {
                while (rs.next()) {

                    //We can not create a blob directly because BlobImpl from Hibernate is not acceptable
                    //for oracle and Connection.createBlob is not working on postgres.
                    //This blob will be not empty because we saved there PackageBits.EMPTY_BLOB
                    Blob blb = rs.getBlob(1);

                    //copy the stream to the Blob
                    long transferred = copyAndDigest(stream, blb.setBinaryStream(1), false, contentDetails);
                    stream.close();

                    //populate the prepared statement for update
                    ps2 = conn.prepareStatement("UPDATE " + PackageBits.TABLE_NAME + " SET bits = ? where id = ?");
                    ps2.setBlob(1, blb);
                    ps2.setInt(2, bits.getId());

                    //initiate the update.
                    if (ps2.execute()) {
                        throw new Exception("Unable to upload the package bits to the DB:");
                    }
                    ps2.close();
                }
            }
            ps.close();
            conn.close();
        } catch (Exception e) {
            log.error("An error occurred while updating Blob with stream for PackageBits[" + bits.getId() + "], "
                + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    log.warn("Failed to close prepared statement for package bits [" + bits.getId() + "]");
                }
            }

            if (ps2 != null) {
                try {
                    ps2.close();
                } catch (Exception e) {
                    log.warn("Failed to close prepared statement for package bits [" + bits.getId() + "]");
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    log.warn("Failed to close connection for package bits [" + bits.getId() + "]");
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    log.warn("Failed to close stream to package bits located at [" + +bits.getId() + "]");
                }
            }
        }

        // not sure this merge (or others like it in this file are necessary...
        entityManager.merge(bits);
        entityManager.flush();
    }

    /** Functions same as StreamUtil.copy(), but calculates SHA hash and file size and write it to 
     *  the Map<String,String> passed in.  
     * 
     * @param input
     * @param output
     * @param closeStreams
     * @param contentDetails
     * @return
     * @throws RuntimeException
     */
    private long copyAndDigest(InputStream input, OutputStream output, boolean closeStreams,
        Map<String, String> contentDetails) throws RuntimeException {
        long numBytesCopied = 0;
        int bufferSize = 32768;
        MessageDigestGenerator digestGenerator = null;
        if (contentDetails != null) {
            digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        }
        try {
            // make sure we buffer the input
            input = new BufferedInputStream(input, bufferSize);

            byte[] buffer = new byte[bufferSize];

            for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
                output.write(buffer, 0, bytesRead);
                numBytesCopied += bytesRead;
                if (digestGenerator != null) {
                    digestGenerator.add(buffer, 0, bytesRead);
                }
            }

            if (contentDetails != null) {//if we're calculating a digest as well
                contentDetails.put(UPLOAD_FILE_SIZE, String.valueOf(numBytesCopied));
                contentDetails.put(UPLOAD_SHA256, digestGenerator.getDigestString());
            }
            output.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Stream data cannot be copied", ioe);
        } finally {
            if (closeStreams) {
                try {
                    output.close();
                } catch (IOException ioe2) {
                    log.warn("Streams could not be closed", ioe2);
                }

                try {
                    input.close();
                } catch (IOException ioe2) {
                    log.warn("Streams could not be closed", ioe2);
                }
            }
        }

        return numBytesCopied;
    }

    /** For Testing only<br><br>
     * 
     * Writes the contents of a the Blob out to the stream passed in.
     *
     * @param stream non null stream where contents to be written to.
     */
    public void writeBlobOutToStream(OutputStream stream, PackageBits bits, boolean closeStreams) {

        if (stream == null) {
            return; // no locate to write to
        }
        if ((bits == null) || (bits.getId() <= 0)) {
            //then PackageBits instance passed in is insufficiently initialized.
            log.warn("PackageBits insufficiently initialized. No data to write out.");
            return;
        }
        try {
            //open connection
            Connection conn = dataSource.getConnection();

            //prepared statement for retrieval of Blob.bits
            PreparedStatement ps = conn
                .prepareStatement("SELECT BITS FROM " + PackageBits.TABLE_NAME + " WHERE ID = ?");
            ps.setInt(1, bits.getId());
            ResultSet results = ps.executeQuery();
            if (results.next()) {
                //retrieve the Blob
                Blob blob = results.getBlob(1);
                //now copy the contents to the stream passed in
                StreamUtil.copy(blob.getBinaryStream(), stream, closeStreams);
            }
        } catch (Exception ex) {
            log.error("An error occurred while writing Blob contents out to stream :" + ex.getMessage());
            ex.printStackTrace();
        }
    }

}