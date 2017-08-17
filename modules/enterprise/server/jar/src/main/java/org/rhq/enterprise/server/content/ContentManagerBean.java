/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
package org.rhq.enterprise.server.content;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import org.jboss.ejb3.annotation.TransactionTimeout;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;
import org.rhq.core.clientapi.server.content.ContentServiceResponse;
import org.rhq.core.clientapi.server.content.DeletePackagesRequest;
import org.rhq.core.clientapi.server.content.DeployPackagesRequest;
import org.rhq.core.clientapi.server.content.RetrievePackageBitsRequest;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
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
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionFormatDescription;
import org.rhq.core.domain.content.ValidatablePackageDetailsKey;
import org.rhq.core.domain.content.composite.PackageAndLatestVersionComposite;
import org.rhq.core.domain.content.composite.PackageTypeAndVersionFormatComposite;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.criteria.InstalledPackageCriteria;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InvalidPackageTypeException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.PackageDetailsValidationException;
import org.rhq.enterprise.server.plugin.pc.content.PackageTypeBehavior;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.scheduler.jobs.DataPurgeJob;
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
    private static final Log LOG = LogFactory.getLog(ContentManagerBean.class);

    // Constants  --------------------------------------------

    /**
     * Amount of time a request may be outstanding against an agent before it is marked as timed out.
     */
    private static final int REQUEST_TIMEOUT = 1000 * 60 * 60;

    private static final String TMP_FILE_PREFIX = "rhq-content-";

    private static final String TMP_FILE_SUFFIX = ".bin";

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
    private ResourceManagerLocal resourceManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private RepoManagerLocal repoManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    // ContentManagerLocal Implementation  --------------------------------------------
    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void mergePackage(Subject subject, int resourceId, ResourcePackageDetails details) {

        // Check permissions first
        if (!authorizationManager.hasResourcePermission(subject, Permission.MANAGE_CONTENT, resourceId)) {
            throw new PermissionException("User [" + subject.getName()
                    + "] does not have permission to merge package details from resource ID [" + resourceId + "]");
        }


        ContentDiscoveryReport report = new ContentDiscoveryReport();
        Set<ResourcePackageDetails> installedPackagesSet = new HashSet<ResourcePackageDetails>();
        installedPackagesSet.add(details);

        report.addAllDeployedPackages(installedPackagesSet);
        report.setResourceId(resourceId);
        contentManager.mergeDiscoveredPackages(report);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void mergeDiscoveredPackages(ContentDiscoveryReport report) {
        int resourceId = report.getResourceId();

        // For performance tracking
        long start = System.currentTimeMillis();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Merging [" + report.getDeployedPackages().size() + "] packages for Resource with id ["
                + resourceId + "]...");
        }

        // Load the resource and its installed packages.
        ResourceCriteria c = new ResourceCriteria();
        c.addFilterId(resourceId);
        c.fetchInstalledPackages(true);
        List<Resource> result = resourceManager.findResourcesByCriteria(subjectManager.getOverlord(), c);
        if (result.isEmpty()) {
            LOG.error("Invalid resource ID specified for merge. Resource ID: " + resourceId);
            return;
        }

        // Installed packages on the Resource that are not referenced in the report will be removed. The doomed
        // list is seeded with all of the currently installed packages, and what remains after processing are
        // those to be removed.
        Resource resource = result.get(0);
        Set<InstalledPackage> doomedPackages = new HashSet<InstalledPackage>(resource.getInstalledPackages());

        // Timestamp to use for all audit trail entries from this report
        long timestamp = System.currentTimeMillis();

        // The report contains an entire snapshot of packages, each has to be represented as an InstalledPackage
        for (ResourcePackageDetails discoveredPackage : report.getDeployedPackages()) {

            // process each in a separate Tx to avoid a large umbrella Tx and locking issues.
            contentManager.handleDiscoveredPackage(resource, discoveredPackage, doomedPackages, timestamp);

        } // end resource package loop

        // Now remove from the resource the remaining doomed installed packages, they were no longer discovered
        contentManager.removeInstalledPackages(resource, doomedPackages, timestamp);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Finished merging [" + report.getDeployedPackages().size() + "] packages in "
                + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    public void handleDiscoveredPackage(Resource resource, ResourcePackageDetails discoveredPackage,
        Set<InstalledPackage> doomedPackages, long timestamp) {

        Package generalPackage = null;
        PackageVersion packageVersion = null;

        // Load the overall package (used in a few places later in this loop)
        Query packageQuery = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_RESOURCE_TYPE);
        packageQuery.setFlushMode(FlushModeType.COMMIT);
        // these form a query for a unique package
        packageQuery.setParameter("name", discoveredPackage.getName());
        packageQuery.setParameter("packageTypeName", discoveredPackage.getPackageTypeName());
        packageQuery.setParameter("resourceTypeId", resource.getResourceType().getId());
        List<Package> resultPackages = packageQuery.getResultList();
        if (resultPackages.size() > 0) {
            generalPackage = resultPackages.get(0); // returns at most 1 Package
        }

        // If the package exists see if package version already exists
        if (null != generalPackage) {
            Query packageVersionQuery = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_VERSION);
            packageVersionQuery.setFlushMode(FlushModeType.COMMIT);
            packageVersionQuery.setParameter("packageId", generalPackage.getId());
            packageVersionQuery.setParameter("version", discoveredPackage.getVersion());
            List<PackageVersion> resultPackageVersions = packageVersionQuery.getResultList();
            // Although the PV unique index is (package,version,arch) in reality the architecture portion is
            // superfluous.  The version is now basically unique (it's basically an enhanced SHA) so it means that
            // two different architectures would basically have two different versions anyway.  So, despite the
            // DB model, this query will return at most 1 PV.
            if (resultPackageVersions.size() > 0) {
                packageVersion = resultPackageVersions.get(0); // returns at most 1 PackageVersion
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
            Architecture packageArchitecture;
            Query architectureQuery = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
            architectureQuery.setFlushMode(FlushModeType.COMMIT);
            architectureQuery.setParameter("name", discoveredPackage.getArchitectureName());

            // We don't have an architecture enum, so it's very possible the plugin will pass in a crap string here.
            // If the architecture is unknown just use "noarch" and log a warning.  We don't want to blow up
            // just because a plugin didn't set this correctly, as architecture is nearly useless at this point.
            try {
                packageArchitecture = (Architecture) architectureQuery.getSingleResult();
            } catch (Exception e) {
                LOG.warn("Discovered Architecture [" + discoveredPackage.getArchitectureName()
                    + "] not found for package [" + discoveredPackage.getName()
                    + "]. Setting to [noarch] and continuing...");
                packageArchitecture = getNoArchitecture();
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

        } else {
            // At this point we know a PackageVersion existed previously in the DB already. If it is already
            // installed to the resource then we are done, and we can remove it from the doomed package list.
            for (Iterator<InstalledPackage> i = doomedPackages.iterator(); i.hasNext();) {
                InstalledPackage ip = i.next();
                PackageVersion pv = ip.getPackageVersion();
                if (pv.getId() == packageVersion.getId()) {
                    i.remove();
                    return;
                }
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
    }

    @Override
    public void removeInstalledPackages(Resource resource, Set<InstalledPackage> doomedPackages, long timestamp) {

        // first, create history records to audit that the previously discovered package was no longer found
        for (InstalledPackage doomedPackage : doomedPackages) {
            InstalledPackageHistory history = new InstalledPackageHistory();
            history.setPackageVersion(doomedPackage.getPackageVersion());
            history.setResource(resource);
            history.setStatus(InstalledPackageHistoryStatus.MISSING);
            history.setTimestamp(timestamp);
            entityManager.persist(history);
        }

        // let's flush these to not buffer too much
        entityManager.flush();

        // now, remove the installed packages (in batches to protect against oracle limit)
        Query query = entityManager.createNamedQuery(InstalledPackage.QUERY_DELETE_BY_IDS);
        final int batchSize = 200;
        List<Integer> doomedIds = new ArrayList(doomedPackages.size());
        for (InstalledPackage ip : doomedPackages) {
            doomedIds.add(ip.getId());
        }

        while (!doomedIds.isEmpty()) {
            int size = doomedIds.size();
            int end = (batchSize < size) ? batchSize : size;

            List<Integer> idBatch = doomedIds.subList(0, end);
            query.setParameter("ids", idBatch);
            query.executeUpdate();

            // Advance our progress and possibly help GC. This will remove the processed ids from the backing list
            idBatch.clear();
        }
    }

    @Override
    public void deployPackages(Subject user, int[] resourceIds, int[] packageVersionIds) {
        this.deployPackagesWithNote(user, resourceIds, packageVersionIds, null);
    }

    @Override
    public void deployPackagesWithNote(Subject user, int[] resourceIds, int[] packageVersionIds, String requestNotes) {
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

            deployPackages(user, resourceId, packages, requestNotes);
        }
    }

    @Override
    public void deployPackages(Subject user, int resourceId, Set<ResourcePackageDetails> packages, String requestNotes) {
        if (packages == null) {
            throw new IllegalArgumentException("packages cannot be null");
        }

        LOG.info("Deploying " + packages.size() + " packages on resource ID [" + resourceId + "]");

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
            LOG.error("Error while sending deploy request to agent", e);

            // Update the request with the failure
            contentManager.failRequest(persistedRequest.getId(), e);

            // Throw so caller knows an error happened
            throw e;
        }
    }

    @Override
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
.createNamedQuery(
                PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE, PackageVersion.class);
            packageVersionQuery.setParameter("packageName", key.getName());
            packageVersionQuery.setParameter("packageTypeName", key.getPackageTypeName());
            packageVersionQuery.setParameter("architectureName", key.getArchitectureName());
            packageVersionQuery.setParameter("version", key.getVersion());
            packageVersionQuery.setParameter("resourceTypeId", resource.getResourceType().getId());

            @SuppressWarnings("unchecked")
            List<PackageVersion> packageVersions = packageVersionQuery.getResultList();
            if (packageVersions.isEmpty()) {
                ResourceType type = resource.getResourceType();
                StringBuilder supportedTypes = new StringBuilder("[");
                for (PackageType pt : resource.getResourceType().getPackageTypes()) {
                    supportedTypes.append(pt.getDisplayName() + " [" + type.getName() + ":" + type.getPlugin() + "],");
                }
                supportedTypes.deleteCharAt(supportedTypes.length() - 1);
                supportedTypes.append("]");
                throw new InvalidPackageTypeException(key.getPackageTypeName(), type, supportedTypes.toString());
            }
            PackageVersion packageVersion = packageVersions.get(0);

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

    @Override
    @SuppressWarnings("unchecked")
    public void completeDeployPackageRequest(DeployPackagesResponse response) {
        LOG.info("Completing deploy package response: " + response);

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
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE);
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
                history.setErrorMessage(response.getOverallRequestErrorMessage());
            }

            entityManager.persist(history);
            persistedRequest.addInstalledPackageHistory(history);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void removeHistoryDeploymentsBits(){
        List<String> resourceTypes = Arrays.asList("Deployment", "DomainDeployment");
        List<String> plugins = Arrays.asList("JBossAS7", "EAP7");
        Query query = entityManager.createQuery("SELECT pk FROM Package pk WHERE pk.packageType   IN ( SELECT pt.id " +
                "FROM PackageType pt WHERE pt.category = :packageTypeCategory AND pt.resourceType IN ( SELECT rt.id " +
                "FROM ResourceType rt WHERE rt.name IN ( :resourceTypes) AND rt.category = :resourceTypeCategory AND " +
                "(rt.plugin IN (:plugins) ) ) )");
        query.setParameter("resourceTypes", resourceTypes);
        query.setParameter("resourceTypeCategory", ResourceCategory.SERVICE);
        query.setParameter("packageTypeCategory", PackageCategory.DEPLOYABLE);
        query.setParameter("plugins", plugins);

        List <Package> packages = query.getResultList();
        for(Package pkg: packages) {
            List<Integer> needUnlink = contentManager.purgePackageBits(pkg.getId());
            if (DatabaseTypeFactory.isPostgres(DatabaseTypeFactory.getDefaultDatabaseType())) {
                for(Integer bitId: needUnlink){
                    contentManager.unlinkBlob(bitId);
                }
            }
        }
        contentManager.removeOrphanedPackageBits();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeOrphanedPackageBits(){
        Query deleteBitsQuery = entityManager.createNamedQuery(PackageBits.DELETE_IF_NO_PACKAGE_VERSION);
        deleteBitsQuery.executeUpdate();
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void unlinkBlob(Integer bitsId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement("SELECT BITS FROM " + PackageBits.TABLE_NAME + " WHERE ID = " + bitsId);
            rs = ps.executeQuery();
            while (rs.next()) {
                int blobId = rs.getInt(1);
                Statement unlinkStatement = conn.createStatement();
                String unlinkSQLProto = "SELECT lo_unlink(%s)";
                String sqlUnlink = String.format(unlinkSQLProto, blobId);
                unlinkStatement.execute(sqlUnlink);
                JDBCUtil.safeClose(unlinkStatement);
            }
        } catch (SQLException e) {
            LOG.warn("Failed to clean package bits with ID " + bitsId);
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Integer> purgePackageBits(int packageId){
        // Cleaning package bits
        final int MAX_HISTORICAL_VERSIONS_PER_PACKAGE = 1;
        Query packageVersionQuery = entityManager
                .createNamedQuery(PackageVersion.QUERY_FIND_PACKAGE_HISTORICAL_VERSIONS);
        ArrayList<Integer> needUnlinking = new ArrayList<Integer>();
        packageVersionQuery.setParameter("packageId",packageId);
        List<PackageVersion> versions = packageVersionQuery.getResultList();
        if(versions.size()>MAX_HISTORICAL_VERSIONS_PER_PACKAGE) {
            /* Remove recent packages from the list*/
            for (int i = 0; i < MAX_HISTORICAL_VERSIONS_PER_PACKAGE; ++i){
                versions.remove(0);
            }

            /* Set to null all other versions */
            for (PackageVersion pv:versions) {
                needUnlinking.add(pv.getPackageBits().getId());
                pv.setPackageBits(null);
                entityManager.merge(pv);
            }
        }
        return needUnlinking;
    }

    @Override
    public void deletePackages(Subject user, int[] resourceIds, int[] installedPackageIds) {
        for (int resourceId : resourceIds) {
            deletePackages(user, resourceId, installedPackageIds, null);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deletePackages(Subject user, int resourceId, int[] installedPackageIds, String requestNotes) {
        if (installedPackageIds == null) {
            throw new IllegalArgumentException("installedPackages cannot be null");
        }

        LOG.info("Deleting " + installedPackageIds.length + " from resource ID [" + resourceId + "]");

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
            LOG.error("Error while sending deploy request to agent", e);

            // Update the request with the failure
            contentManager.failRequest(persistedRequest.getId(), e);

            // Throw so caller knows an error happened
            throw e;
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deletePackageVersion(Subject subject, int packageVersionId) {
        Query q = entityManager.createNamedQuery(PackageVersion.DELETE_SINGLE_IF_NO_CONTENT_SOURCES_OR_REPOS);
        q.setParameter("packageVersionId", packageVersionId);
        q.executeUpdate();
    }

    @Override
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

    @Override
    public void completeDeletePackageRequest(RemovePackagesResponse response) {
        LOG.info("Completing delete package response: " + response);

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
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE);
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

    @Override
    public void retrieveBitsFromResource(Subject user, int resourceId, int installedPackageId) {
        LOG.info("Retrieving bits for package [" + installedPackageId + "] on resource ID [" + resourceId + "]");

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
            LOG.error("Error while sending deploy request to agent", e);

            // Update the request with the failure
            contentManager.failRequest(persistedRequest.getId(), e);

            // Throw so caller knows an error happened
            throw e;
        }
    }

    @Override
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
                + installedPackageId + ".", e);

        }
    }

    @Override
    public List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails)
        throws Exception {
        LOG.info("Retrieving installation steps for package [" + packageDetails + "]");

        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();

        // Make call to agent
        List<DeployPackageStep> packageStepList;
        try {
            AgentClient agentClient = agentManager.getAgentClient(agent);
            ContentAgentService agentService = agentClient.getContentAgentService();
            packageStepList = agentService.translateInstallationSteps(resourceId, packageDetails);
        } catch (PluginContainerException e) {
            LOG.error("Error while sending deploy request to agent", e);

            // Throw so caller knows an error happened
            throw e;
        }

        return packageStepList;
    }

    @Override
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

    @Override
    @TransactionTimeout(45 * 60)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void completeRetrievePackageBitsRequest(ContentServiceResponse response, InputStream bitStream) {
        LOG.info("Completing retrieve package bits response: " + response);

        // Load persisted request
        ContentServiceRequest persistedRequest = entityManager.find(ContentServiceRequest.class,
            response.getRequestId());

        // There is some inconsistency if we're completing a request that was not in the database
        if (persistedRequest == null) {
            LOG.error("Attempting to complete a request that was not found in the database: " + response.getRequestId());
            return;
        }
        Resource resource = persistedRequest.getResource();

        InstalledPackageHistory initialRequestHistory = persistedRequest.getInstalledPackageHistory().iterator().next();
        PackageVersion packageVersion = initialRequestHistory.getPackageVersion();

        if (response.getStatus() == ContentRequestStatus.SUCCESS) {
            // Read the stream from the agent and store in the package version
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Saving content for response: " + response);
                }

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
                            LOG.warn("Failed to close prepared statement for package version [" + packageVersion + "]");
                        }
                    }

                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (Exception e) {
                            LOG.warn("Failed to close connection for package version [" + packageVersion + "]");
                        }
                    }
                }

            } catch (Exception e) {
                LOG.error("Error while reading content from agent stream", e);
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

    @Override
    @SuppressWarnings("unchecked")
    public Set<ResourcePackageDetails> loadDependencies(int requestId, Set<PackageDetailsKey> keys) {
        Set<ResourcePackageDetails> dependencies = new HashSet<ResourcePackageDetails>();

        // Load the persisted request
        ContentServiceRequest persistedRequest = entityManager.find(ContentServiceRequest.class, requestId);

        // There is some inconsistency if the request is not in the database
        if (persistedRequest == null) {
            LOG.error("Could not find request with ID: " + requestId);
            return dependencies;
        }

        // Load the resource so we can get its type for the package version queries
        Resource resource = persistedRequest.getResource();
        ResourceType resourceType = resource.getResourceType();

        // For each package requested, load the package version and convert to a transfer object
        long installationDate = System.currentTimeMillis();

        for (PackageDetailsKey key : keys) {
            Query packageQuery = entityManager
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE);
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
                LOG.error("Multiple packages found. Found: " + persistedPackageList.size() + " for key: " + key);
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

    @Override
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

    @Override
    @SuppressWarnings("unchecked")
    public List<Architecture> findArchitectures(Subject subject) {
        Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_ALL);
        List<Architecture> architectures = q.getResultList();

        return architectures;
    }

    @Override
    public Architecture getNoArchitecture() {
        Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
        q.setParameter("name", "noarch");
        Architecture architecture = (Architecture) q.getSingleResult();

        return architecture;
    }

    @Override
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

    @Override
    public PackageType findPackageType(Subject subject, Integer resourceTypeId, String packageTypeName) {
        Query q = entityManager
            .createNamedQuery(resourceTypeId == null ? PackageType.QUERY_FIND_BY_NAME_AND_NULL_RESOURCE_TYPE
                : PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME);

        if (resourceTypeId != null) {
            q.setParameter("typeId", resourceTypeId);
        }
        q.setParameter("name", packageTypeName);

        @SuppressWarnings("unchecked")
        List<PackageType> results = q.getResultList();

        if (results.size() == 0) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            String message = "2 or more package types with name '" + packageTypeName
                + "' found on the resource type with id " + resourceTypeId + ". This is a bug in the database.";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
    }

    @Override
    public PackageTypeAndVersionFormatComposite findPackageTypeWithVersionFormat(Subject subject,
        Integer resourceTypeId, String packageTypeName) {

        PackageType type = findPackageType(subject, resourceTypeId, packageTypeName);

        PackageVersionFormatDescription format = null;

        try {
            PackageTypeBehavior behavior = ContentManagerHelper.getPackageTypeBehavior(packageTypeName);
            if (behavior != null) {
                format = behavior.getPackageVersionFormat(packageTypeName);
            }
        } catch (Exception e) {
            //well, this shouldn't happen but is not crucial in this case
            LOG.info("Failed to obtain the behavior of package type '" + packageTypeName + "'.", e);
        }

        return new PackageTypeAndVersionFormatComposite(type, format);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void checkForTimedOutRequests(Subject subject) {
        if (!authorizationManager.isOverlord(subject)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unauthorized user " + subject + " tried to execute checkForTimedOutRequests; "
                    + "only the overlord may execute this system operation");
            }
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Timing out request after duration: " + duration + " Request: " + request);
                    }

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
                            LOG.warn("Found a history entry on the request with an unexpected status. Id: "
                                + history.getId() + ", Status: " + packageStatus);
                            break;

                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOG.error("Error while processing timed out requests", e);
        }
    }

    @Override
    public PackageVersion createPackageVersion(Subject subject, String packageName, int packageTypeId, String version,
        Integer architectureId, byte[] packageBytes) {
        return createPackageVersionWithDisplayVersion(subject, packageName, packageTypeId, version, null,
            architectureId, packageBytes);
    }

    @Override
    public PackageVersion createPackageVersionWithDisplayVersion(Subject subject, String packageName,
        int packageTypeId, String version, String displayVersion, Integer architectureId, byte[] packageBytes) {

        // Check permissions first
        if (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_CONTENT)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to create package versions");
        }

        return createPackageVersionWithDisplayVersion(subject, packageName, packageTypeId, version, displayVersion,
            (null == architectureId) ? getNoArchitecture().getId() : architectureId, new ByteArrayInputStream(
                packageBytes));
    }

    @Override
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public PackageVersion createPackageVersionWithDisplayVersion(Subject subject, String packageName,
        int packageTypeId, String version, String displayVersion, int architectureId, InputStream packageBitStream) {
        // See if the package version already exists and return that if it does
        Query packageVersionQuery = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_VER_ARCH);
        packageVersionQuery.setParameter("name", packageName);
        packageVersionQuery.setParameter("packageTypeId", packageTypeId);
        packageVersionQuery.setParameter("architectureId", architectureId);
        packageVersionQuery.setParameter("version", version);

        // Result of the query should be either 0 or 1
        List existingVersionList = packageVersionQuery.getResultList();
        if (existingVersionList.size() > 0) {
            PackageVersion existingPackageVersion = (PackageVersion) existingVersionList.get(0);
            if (displayVersion != null && !displayVersion.trim().isEmpty()) {
                existingPackageVersion.setDisplayVersion(displayVersion);
                existingPackageVersion = persistOrMergePackageVersionSafely(existingPackageVersion);
            }

            return existingPackageVersion;
        }

        Architecture architecture = entityManager.find(Architecture.class, architectureId);
        PackageType packageType = entityManager.find(PackageType.class, packageTypeId);

        //check the validity of the provided data
        try {
            PackageTypeBehavior behavior = ContentManagerHelper.getPackageTypeBehavior(packageTypeId);
            ValidatablePackageDetailsKey key = new ValidatablePackageDetailsKey(packageName, version,
                packageType.getName(), architecture.getName());
            behavior.validateDetails(key, subject);

            packageName = key.getName();
            version = key.getVersion();
            if (!architecture.getName().equals(key.getArchitectureName())) {
                Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
                q.setParameter("name", key.getArchitectureName());
                architecture = (Architecture) q.getSingleResult();
            }
        } catch (PackageDetailsValidationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to get the package type plugin container. This is a bug.", e);
            throw new IllegalStateException("Failed to get the package type plugin container.", e);
        }

        // If the package doesn't exist, create that here
        Query packageQuery = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
        packageQuery.setParameter("name", packageName);
        packageQuery.setParameter("packageTypeId", packageTypeId);

        Package existingPackage;

        List existingPackageList = packageQuery.getResultList();

        if (existingPackageList.size() == 0) {
            existingPackage = new Package(packageName, packageType);
            existingPackage = persistOrMergePackageSafely(existingPackage);
        } else {
            existingPackage = (Package) existingPackageList.get(0);
        }

        // Create a package version and add it to the package
        PackageVersion newPackageVersion = new PackageVersion(existingPackage, version, architecture);
        newPackageVersion.setDisplayName(existingPackage.getName());

        newPackageVersion = persistOrMergePackageVersionSafely(newPackageVersion);

        Map<String, String> contentDetails = new HashMap<String, String>();
        PackageBits bits = loadPackageBits(packageBitStream, newPackageVersion.getId(), packageName, version, null,
            contentDetails);

        newPackageVersion.setPackageBits(bits);
        newPackageVersion.setFileSize(Long.valueOf(contentDetails.get(UPLOAD_FILE_SIZE)).longValue());
        newPackageVersion.setSHA256(contentDetails.get(UPLOAD_SHA256));
        newPackageVersion.setDisplayVersion(displayVersion);

        existingPackage.addVersion(newPackageVersion);

        return newPackageVersion;
    }

    @Override
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

    @Override
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

            ResourceType rt = pv.getGeneralPackage().getPackageType().getResourceType();
            q.setParameter("resourceType", rt);

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
                LOG.warn("There was probably a very big and ugly EJB/hibernate error just above this log message - "
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

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Package persistPackage(Package pkg) {
        // EM.persist requires related entities to be attached, let's attach them now
        pkg.setPackageType(entityManager.find(PackageType.class, pkg.getPackageType().getId()));

        // our object's relations are now full attached, we can persist it
        entityManager.persist(pkg);
        return pkg;
    }

    @Override
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
                LOG.warn("There was probably a very big and ugly EJB/hibernate error just above this log message - "
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

    @Override
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

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findInstalledPackageVersions(Subject user, int resourceId) {
        Query query = entityManager.createNamedQuery(InstalledPackage.QUERY_FIND_PACKAGE_LIST_VERSIONS);
        query.setParameter("resourceId", resourceId);

        List<String> packages = query.getResultList();
        return packages;
    }

    @Override
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

    @Override
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

    @Override
    public PageList<Package> findPackagesByCriteria(Subject subject, PackageCriteria criteria) {

        if (criteria.getFilterRepoId() != null) {
            if (!authorizationManager.canViewRepo(subject, criteria.getFilterRepoId())) {
                throw new PermissionException("Subject [" + subject.getName() + "] cannot view the repo with id "
                    + criteria.getFilterRepoId());
            }
        } else if (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_REPOSITORIES)) {
            throw new PermissionException("Only repository managers can search for packages across all repos.");
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);

        CriteriaQueryRunner<Package> runner = new CriteriaQueryRunner<Package>(criteria, generator, entityManager);

        return runner.execute();
    }

    @Override
    public PageList<PackageAndLatestVersionComposite> findPackagesWithLatestVersion(Subject subject,
        PackageCriteria criteria) {
        if (criteria.getFilterRepoId() == null) {
            throw new IllegalArgumentException("The criteria query has to have a filter for a specific repo.");
        }

        criteria.fetchVersions(true);
        PageList<Package> packages = findPackagesByCriteria(subject, criteria);

        PageList<PackageAndLatestVersionComposite> ret = new PageList<PackageAndLatestVersionComposite>(
            packages.getTotalSize(), packages.getPageControl());

        for (Package p : packages) {
            PackageVersion latest = repoManager.getLatestPackageVersion(subject, p.getId(), criteria.getFilterRepoId());
            ret.add(new PackageAndLatestVersionComposite(p, latest));
        }

        return ret;
    }

    @Override
    public InstalledPackage getBackingPackageForResource(Subject subject, int resourceId) {
        InstalledPackage result = null;

        // check if the resource is content backed if not, return null
        Resource res = resourceManager.getResourceById(subject, resourceId);
        ResourceType type = res.getResourceType();
        if (!ResourceCreationDataType.CONTENT.equals(type.getCreationDataType())) {
            return null;
        }

        InstalledPackageCriteria criteria = new InstalledPackageCriteria();
        criteria.addFilterResourceId(resourceId);
        PageList<InstalledPackage> ips = findInstalledPackagesByCriteria(subject, criteria);

        // should not be more than 1
        if ((null != ips) && (ips.size() > 0)) {
            int mostRecentPackageIndex = 0;

            if (ips.size() > 1) {
                for (int index = 1; index < ips.size(); index++) {
                    if (ips.get(index).getInstallationDate() > ips.get(mostRecentPackageIndex).getInstallationDate()) {
                        mostRecentPackageIndex = index;
                    }
                }
            }

            result = ips.get(mostRecentPackageIndex);

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
    @Override
    @SuppressWarnings("unchecked")
    public PackageVersion getUploadedPackageVersion(Subject subject, String packageName, int packageTypeId,
        String version, int architectureId, InputStream packageBitStream, Map<String, String> packageUploadDetails,
        Integer repoId) {

        PackageVersion packageVersion = null;

        //default version to 1.0 if is null, not provided for any reason.
        if ((version == null) || (version.trim().length() == 0)) {
            version = "1.0";
        }

        Architecture architecture = entityManager.find(Architecture.class, architectureId);
        PackageType packageType = entityManager.find(PackageType.class, packageTypeId);

        // See if package version already exists for the resource package
        Query packageVersionQuery = null;

        if (packageType.getResourceType() != null) {
            packageVersionQuery = entityManager
                .createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY_WITH_NON_NULL_RESOURCE_TYPE);
            packageVersionQuery.setParameter("resourceTypeId", packageType.getResourceType().getId());

        } else {
            packageVersionQuery = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            packageVersionQuery.setParameter("resourceType", null);
        }

        packageVersionQuery.setFlushMode(FlushModeType.COMMIT);
        packageVersionQuery.setParameter("packageName", packageName);

        packageVersionQuery.setParameter("packageTypeName", packageType.getName());

        packageVersionQuery.setParameter("architectureName", architecture.getName());
        packageVersionQuery.setParameter("version", version);

        // Result of the query should be either 0 or 1
        List<PackageVersion> existingPackageVersionList = packageVersionQuery.getResultList();

        if (existingPackageVersionList.size() > 0) {
            packageVersion = existingPackageVersionList.get(0);
        }

        try {
            PackageTypeBehavior behavior = ContentManagerHelper.getPackageTypeBehavior(packageTypeId);

            if (behavior != null) {
                String packageTypeName = packageType.getName();
                String archName = architecture.getName();
                ValidatablePackageDetailsKey key = new ValidatablePackageDetailsKey(packageName, version,
                    packageTypeName, archName);
                behavior.validateDetails(key, subject);

                //update the details from the validation results
                packageName = key.getName();
                version = key.getVersion();

                if (!architecture.getName().equals(key.getArchitectureName())) {
                    Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
                    q.setParameter("name", key.getArchitectureName());
                    architecture = (Architecture) q.getSingleResult();
                }
            }
        } catch (PackageDetailsValidationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to get the package type plugin container. This is a bug.", e);
            throw new IllegalStateException("Failed to get the package type plugin container.", e);
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

        // We are going to replace the package bits a bit later
        // before it happen lets purge the BLOB to avoid leaks
        if (packageVersion.getPackageBits() != null &&
                DatabaseTypeFactory.isPostgres(DatabaseTypeFactory.getDefaultDatabaseType())) {
            contentManager.unlinkBlob(packageVersion.getPackageBits().getId());
        }

        //get the data
        Map<String, String> contentDetails = new HashMap<String, String>();
        PackageBits bits = loadPackageBits(packageBitStream, packageVersion.getId(), packageName, version, null,
            contentDetails);

        packageVersion.setPackageBits(bits);

        packageVersion.setFileSize(Long.valueOf(contentDetails.get(UPLOAD_FILE_SIZE)).longValue());
        packageVersion.setSHA256(contentDetails.get(UPLOAD_SHA256));

        //populate extra details, persist
        if (packageUploadDetails != null) {
            packageVersion.setFileCreatedDate(Long.valueOf(packageUploadDetails
                .get(ContentManagerLocal.UPLOAD_FILE_INSTALL_DATE)));
            packageVersion.setFileName(packageUploadDetails.get(ContentManagerLocal.UPLOAD_FILE_NAME));
            packageVersion.setMD5(packageUploadDetails.get(ContentManagerLocal.UPLOAD_MD5));
            packageVersion.setDisplayVersion(packageUploadDetails.get(ContentManagerLocal.UPLOAD_DISPLAY_VERSION));
        }

        entityManager.merge(packageVersion);

        if (repoId != null) {
            int[] packageVersionIds = new int[] { packageVersion.getId() };
            repoManager.addPackageVersionsToRepo(subject, repoId, packageVersionIds);
        }

        entityManager.flush();

        return packageVersion;

    }

    @Override
    public PackageType persistServersidePackageType(PackageType packageType) {
        if (packageType.getResourceType() != null) {
            throw new IllegalArgumentException("Server-side package types can't be associated with a resource type.");
        }

        entityManager.persist(packageType);

        return packageType;
    }

    /** Pulls in package bits from the stream. Currently inefficient.
     *
     * @param packageBitStream
     * @param packageVersionId
     * @param contentDetails
     * @return PackageBits ref populated.
     */
    private PackageBits loadPackageBits(InputStream packageBitStream, int packageVersionId, String packageName,
        String packageVersion, PackageBits existingBits, Map<String, String> contentDetails) {

        // If/When H2 handles blob update/streaming blobs we can get rid of this conditional code
        if (DatabaseTypeFactory.isH2(DatabaseTypeFactory.getDefaultDatabaseType())) {
            return loadPackageBitsH2(packageBitStream, packageVersionId, packageName, packageVersion, existingBits,
                contentDetails);
        }

        // use existing or instantiate PackageBits instance.
        PackageBits bits = (null == existingBits) ? initializePackageBits(null) : existingBits;

        //locate related packageVersion
        PackageVersion pv = entityManager.find(PackageVersion.class, packageVersionId);

        //associate the two if located.
        if (null != pv) {
            pv.setPackageBits(bits);
            entityManager.flush();
        }

        //write data from stream into db using Hibernate Blob mechanism
        updateBlobStream(packageBitStream, bits, contentDetails);

        return bits;
    }

    private PackageBits loadPackageBitsH2(InputStream packageBitStream, int packageVersionId, String packageName,
        String packageVersion, PackageBits existingBits, Map<String, String> contentDetails) {

        PackageBits bits = null;
        PackageBitsBlob blob = null;

        // The blob cannot be updated, so we'll need to create a whole new row.
        if (null != existingBits) {
            blob = entityManager.find(PackageBitsBlob.class, existingBits.getId());
            entityManager.remove(blob);
            entityManager.flush();
        }

        // We have to work backwards to avoid constraint violations. PackageBits requires a PackageBitsBlob,
        // so create and persist that first, getting the ID
        blob = new PackageBitsBlob();
        // just set the blob now, no streaming. The assumption is that H2 (demo) will not be using large blobs
        byte[] bytes = StreamUtil.slurp(packageBitStream);
        blob.setBits(bytes);
        entityManager.persist(blob);
        entityManager.flush();

        // Now create the PackageBits entity and assign the Id and blob.  Note, do not persist the
        // entity, the row already exists (due to the blob persist above). Just perform and flush the update.
        bits = new PackageBits();
        bits.setId(blob.getId());
        bits.setBlob(blob);
        entityManager.flush();

        //locate related packageVersion
        PackageVersion pv = entityManager.find(PackageVersion.class, packageVersionId);

        //associate the two if packageVersion exists.
        if (null != pv) {
            pv.setPackageBits(bits);
            entityManager.flush();
        }

        // update contentDetails in needed
        if (null != contentDetails) {
            contentDetails.put(UPLOAD_FILE_SIZE, String.valueOf(bytes.length));
            try {
                contentDetails.put(UPLOAD_SHA256,
                    new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(bytes));
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate SHA256 for package bits: ", e);
            }
        }

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
    @Override
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
            try {
                while (rs.next()) {

                    //We can not create a blob directly because BlobImpl from Hibernate is not acceptable
                    //for oracle and Connection.createBlob is not working on postgres.
                    //This blob will be not empty because we saved there PackageBits.EMPTY_BLOB
                    Blob blb = rs.getBlob(1);

                    //copy the stream to the Blob
                    copyAndDigest(stream, blb.setBinaryStream(1), false, contentDetails);
                    stream.close();

                    if(!DatabaseTypeFactory.isPostgres(DatabaseTypeFactory.getDefaultDatabaseType())) {
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
            } finally {
                rs.close();
            }
            ps.close();
            conn.close();
        } catch (Exception e) {
            LOG.error("An error occurred while updating Blob with stream for PackageBits[" + bits.getId() + "], "
                + e.getMessage());
            if (e instanceof  RuntimeException && e.getCause() != null && e.getCause().getMessage().startsWith("ORA-")) {
                throw (RuntimeException) e;
            } else {
                e.printStackTrace();
            }
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    LOG.warn("Failed to close prepared statement for package bits [" + bits.getId() + "]");
                }
            }

            if (ps2 != null) {
                try {
                    ps2.close();
                } catch (Exception e) {
                    LOG.warn("Failed to close prepared statement for package bits [" + bits.getId() + "]");
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    LOG.warn("Failed to close connection for package bits [" + bits.getId() + "]");
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    LOG.warn("Failed to close stream to package bits located at [" + +bits.getId() + "]");
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
    @SuppressWarnings("resource")
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
                    LOG.warn("Streams could not be closed", ioe2);
                }

                try {
                    input.close();
                } catch (IOException ioe2) {
                    LOG.warn("Streams could not be closed", ioe2);
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
    @Override
    public void writeBlobOutToStream(OutputStream stream, PackageBits bits, boolean closeStreams) {

        if (stream == null) {
            return; // no locate to write to
        }
        if ((bits == null) || (bits.getId() <= 0)) {
            //then PackageBits instance passed in is insufficiently initialized.
            LOG.warn("PackageBits insufficiently initialized. No data to write out.");
            return;
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet results = null;
        try {
            //open connection
            conn = dataSource.getConnection();

            //prepared statement for retrieval of Blob.bits
            ps = conn.prepareStatement("SELECT BITS FROM " + PackageBits.TABLE_NAME + " WHERE ID = ?");
            ps.setInt(1, bits.getId());
            results = ps.executeQuery();
            if (results.next()) {
                //retrieve the Blob
                Blob blob = results.getBlob(1);
                //now copy the contents to the stream passed in
                StreamUtil.copy(blob.getBinaryStream(), stream, closeStreams);
            }
        } catch (Exception ex) {
            LOG.error("An error occurred while writing Blob contents out to stream :" + ex.getMessage());
            ex.printStackTrace();
        } finally {
            JDBCUtil.safeClose(conn, ps, results);
        }
    }

    @Override
    public String createTemporaryContentHandle(Subject subject) {
        try {
            return File.createTempFile(TMP_FILE_PREFIX, TMP_FILE_SUFFIX, getTempDirectory()).getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getTempDirectory() {
        String tempDirectoryPath = System.getProperty("java.io.tmpdir");
        return new File(tempDirectoryPath);
    }

    @Override
    public void uploadContentFragment(Subject subject, String temporaryContentHandle, byte[] fragment, int off, int len) {
        File temporaryContentFile = getTemporaryContentFile(temporaryContentHandle);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fragment, off, len);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(temporaryContentFile, true); // append == true
            StreamUtil.copy(inputStream, new BufferedOutputStream(fileOutputStream, 1024 * 32));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtil.safeClose(fileOutputStream);
        }
    }

    @Override
    public PackageVersion createPackageVersionWithDisplayVersion(Subject subject, String packageName,
        int packageTypeId, String version, String displayVersion, Integer architectureId, String temporaryContentHandle) {
        // Check permissions first
        if (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_CONTENT)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have permission to create package versions");
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(getTemporaryContentFile(temporaryContentHandle));
            return createPackageVersionWithDisplayVersion(subject, packageName, packageTypeId, version, displayVersion,
                (null == architectureId) ? getNoArchitecture().getId() : architectureId, fileInputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtil.safeClose(fileInputStream);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public File getTemporaryContentFile(String temporaryContentHandle) {
        File tempDirectory = getTempDirectory();
        File file = new File(tempDirectory, temporaryContentHandle);
        if (!file.isFile()) {
            throw new RuntimeException("Handle [" + temporaryContentHandle + "] does not denote a file");
        }
        return file;
    }
}
