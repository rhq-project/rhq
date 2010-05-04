/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.bundle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleRequest;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleResponse;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleGroupDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.StringUtils;
import org.rhq.core.util.NumberUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginManager;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.HibernateDetachUtility;
import org.rhq.enterprise.server.util.HibernateDetachUtility.SerializationType;

/**
 * Manages the creation and usage of bundles.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
@Stateless
public class BundleManagerBean implements BundleManagerLocal, BundleManagerRemote {
    private final Log log = LogFactory.getLog(this.getClass());

    private final String AUDIT_ACTION_DEPLOYMENT = "Deployment";
    private final String AUDIT_ACTION_DEPLOYMENT_REQUESTED = "Deployment Requested";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private BundleManagerLocal bundleManager;

    @EJB
    private ContentManagerLocal contentManager;

    @EJB
    private RepoManagerLocal repoManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleResourceDeploymentHistory addBundleResourceDeploymentHistory(Subject subject, int bundleDeploymentId,
        BundleResourceDeploymentHistory history) throws Exception {

        BundleResourceDeployment resourceDeployment = entityManager.find(BundleResourceDeployment.class,
            bundleDeploymentId);
        if (null == resourceDeployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + bundleDeploymentId);
        }

        resourceDeployment.addBundleResourceDeploymentHistory(history);
        this.entityManager.persist(resourceDeployment);

        return history;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Bundle createBundle(Subject subject, String name, String description, int bundleTypeId) throws Exception {
        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleName: " + name);
        }

        BundleType bundleType = entityManager.find(BundleType.class, bundleTypeId);
        if (null == bundleType) {
            throw new IllegalArgumentException("Invalid bundleTypeId: " + bundleTypeId);
        }

        // create and add the required Repo. the Repo is a detached object which helps in its eventual
        // removal.
        Repo repo = new Repo(name);
        repo.setCandidate(false);
        repo.setSyncSchedule(null);
        repo = repoManager.createRepo(subject, repo);

        // add the required PackageType. the PackageType is an attached object which helps in cascade removal
        // of packages in the bundle's repo.
        ResourceType resourceType = entityManager.find(ResourceType.class, bundleType.getResourceType().getId());
        PackageType packageType = new PackageType(name, resourceType);
        packageType.setDescription("Package type for content of bundle " + name);
        packageType.setCategory(PackageCategory.BUNDLE);
        packageType.setSupportsArchitecture(false);
        packageType.setDisplayName(StringUtils.deCamelCase(name));
        packageType.setDiscoveryInterval(-1L);
        packageType.setCreationData(false);
        packageType.setDeploymentConfigurationDefinition(null);

        Bundle bundle = new Bundle(name, bundleType, repo, packageType);
        bundle.setDescription(description);
        bundle.setPackageType(packageType);

        log.info("Creating bundle: " + bundle);
        entityManager.persist(bundle);

        return bundle;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleDeployment createBundleDeployment(Subject subject, int bundleVersionId, String name,
        String description, String installDir, Configuration configuration) throws Exception {

        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleDeploymentName: " + name);
        }
        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
        }
        ConfigurationDefinition configDef = bundleVersion.getConfigurationDefinition();
        if (null != configDef) {
            if (null == configuration) {
                throw new IllegalArgumentException(
                    "Missing Configuration. Configuration is required when the specified BundleVersion defines Configuration Properties.");
            }
            List<String> errors = ConfigurationUtility.validateConfiguration(configuration, configDef);
            if (null != errors && !errors.isEmpty()) {
                throw new IllegalArgumentException("Invalid Configuration: " + errors.toString());
            }
        }

        BundleDeployment deployment = new BundleDeployment(bundleVersion, name, installDir);
        deployment.setDescription(description);
        deployment.setConfiguration(configuration);

        entityManager.persist(deployment);

        return deployment;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleType createBundleType(Subject subject, String name, int resourceTypeId) throws Exception {
        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleTypeName: " + name);
        }

        ResourceType resourceType = entityManager.find(ResourceType.class, resourceTypeId);
        if (null == resourceType) {
            throw new IllegalArgumentException("Invalid resourceeTypeId: " + resourceTypeId);
        }

        BundleType bundleType = new BundleType(name, resourceType);
        entityManager.persist(bundleType);
        return bundleType;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleVersion createBundleAndBundleVersion(Subject subject, String bundleName, String bundleDescription,
        int bundleTypeId, String bundleVersionName, String bundleVersionDescription, String version, String recipe)
        throws Exception {

        // first see if the bundle exists or not; if not, create one
        BundleCriteria criteria = new BundleCriteria();
        criteria.addFilterBundleTypeId(Integer.valueOf(bundleTypeId));
        criteria.addFilterName(bundleName);
        PageList<Bundle> bundles = findBundlesByCriteria(subject, criteria);
        Bundle bundle;
        if (bundles.getTotalSize() == 0) {
            bundle = createBundle(subject, bundleName, bundleDescription, bundleTypeId);
        } else {
            bundle = bundles.get(0);
        }

        // now create the bundle version with the bundle we either found or created
        BundleVersion bv = createBundleVersion(subject, bundle.getId(), bundleVersionName, bundleVersionDescription,
            version, recipe);
        return bv;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleVersion createBundleVersion(Subject subject, int bundleId, String name, String description,
        String version, String recipe) throws Exception {
        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleVersionName: " + name);
        }

        Bundle bundle = entityManager.find(Bundle.class, bundleId);
        if (null == bundle) {
            throw new IllegalArgumentException("Invalid bundleId: " + bundleId);
        }

        // parse the recipe (validation occurs here) and get the config def and list of files
        BundleType bundleType = bundle.getBundleType();
        RecipeParseResults results;

        try {
            results = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager().parseRecipe(
                bundleType.getName(), recipe);
        } catch (Exception e) {
            // ensure that we throw a runtime exception to force a rollback
            throw new RuntimeException("Failed to parse recipe", e);
        }

        // ensure we have a version
        version = getVersion(version, bundle);
        ComparableVersion comparableVersion = new ComparableVersion(version);

        Query q = entityManager.createNamedQuery(BundleVersion.QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID);
        q.setParameter("bundleId", bundle.getId());
        List<Object[]> list = q.getResultList();
        int versionOrder = list.size();
        boolean needToUpdateOrder = false;
        // find out where in the order of versions this new version should be placed (e.g. 2.0 is after 1.0).
        // the query returns a list of arrays - first element in array is version; second is versionOrder
        // the query returns list in desc order - since the normal case is we are creating the latest, highest version,
        // starting at the current highest version is the most efficient (we'll break the for loop after 1 iteration).
        for (Object[] bv : list) {
            ComparableVersion bvv = new ComparableVersion(bv[0].toString());
            int comparision = comparableVersion.compareTo(bvv);
            if (comparision == 0) {
                throw new RuntimeException("Cannot create bundle with version [" + version + "], it already exists");
            } else if (comparision < 0) {
                versionOrder = ((Number) bv[1]).intValue();
                needToUpdateOrder = true;
            } else {
                break; // comparision > 0, means our new version is higher than what's in the DB, because we DESC ordered, we can stop
            }
        }

        if (needToUpdateOrder) {
            entityManager.flush();
            q = entityManager.createNamedQuery(BundleVersion.UPDATE_VERSION_ORDER_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            q.setParameter("versionOrder", versionOrder);
            q.executeUpdate();
            entityManager.flush();
            entityManager.clear();
        }

        BundleVersion bundleVersion = new BundleVersion(name, version, bundle, recipe);
        bundleVersion.setVersionOrder(versionOrder);
        bundleVersion.setDescription(description);
        bundleVersion.setConfigurationDefinition(results.getConfigurationDefinition());

        entityManager.persist(bundleVersion);
        return bundleVersion;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleVersion createBundleVersionViaRecipe(Subject subject, String recipe) throws Exception {

        BundleServerPluginManager manager = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager();
        BundleDistributionInfo info = manager.parseRecipe(recipe);
        BundleVersion bundleVersion = createBundleVersionViaDistributionInfo(subject, info);

        return bundleVersion;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public BundleVersion createBundleVersionViaFile(Subject subject, File distributionFile) throws Exception {

        BundleServerPluginManager manager = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager();
        BundleDistributionInfo info = manager.processBundleDistributionFile(distributionFile);
        BundleVersion bundleVersion = createBundleVersionViaDistributionInfo(subject, info);

        return bundleVersion;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public BundleVersion createBundleVersionViaURL(Subject subject, String distributionFileUrl) throws Exception {

        // validate by immediately creating a URL
        URL url = new URL(distributionFileUrl);

        // get the distro file into a tmp dir
        // create temp file
        File tempDistributionFile = null;
        InputStream is = null;
        OutputStream os = null;
        BundleVersion bundleVersion = null;

        try {
            tempDistributionFile = File.createTempFile("bundle-distribution", ".zip");

            is = url.openStream();
            os = new FileOutputStream(tempDistributionFile);
            long len = StreamUtil.copy(is, os);
            is = null;
            os = null;
            log.debug("Copied [" + len + "] bytes from [" + distributionFileUrl + "] into ["
                + tempDistributionFile.getPath() + "]");

            bundleVersion = createBundleVersionViaFile(subject, tempDistributionFile);
        } finally {
            if (null != tempDistributionFile) {
                tempDistributionFile.delete();
            }
            safeClose(is);
            safeClose(os);
        }

        return bundleVersion;
    }

    private BundleVersion createBundleVersionViaDistributionInfo(Subject subject, BundleDistributionInfo info)
        throws Exception {

        BundleType bundleType = bundleManager.getBundleType(subject, info.getBundleTypeName());
        String bundleName = info.getRecipeParseResults().getBundleMetadata().getBundleName();
        String bundleDescription = info.getRecipeParseResults().getBundleMetadata().getDescription();
        String name = bundleName;
        String description = bundleDescription;
        String version = info.getRecipeParseResults().getBundleMetadata().getBundleVersion();
        String recipe = info.getRecipe();

        // first see if the bundle exists or not; if not, create one
        boolean createdBundle;
        BundleCriteria criteria = new BundleCriteria();
        criteria.setStrict(true);
        criteria.addFilterBundleTypeId(bundleType.getId());
        criteria.addFilterName(bundleName);
        PageList<Bundle> bundles = bundleManager.findBundlesByCriteria(subject, criteria);
        Bundle bundle;
        if (bundles.getTotalSize() == 0) {
            bundle = bundleManager.createBundle(subject, bundleName, bundleDescription, bundleType.getId());
            createdBundle = true;
        } else {
            bundle = bundles.get(0);
            createdBundle = false;
        }

        // now create the bundle version with the bundle we either found or created
        BundleVersion bundleVersion = bundleManager.createBundleVersion(subject, bundle.getId(), name, description,
            version, recipe);

        // now that we have the bundle version we can actually create the bundle files that were provided in
        // the bundle distribution
        try {
            Map<String, File> bundleFiles = info.getBundleFiles();
            if (bundleFiles != null) {
                for (String fileName : bundleFiles.keySet()) {
                    File file = bundleFiles.get(fileName);
                    InputStream is = null;
                    try {
                        is = new FileInputStream(file);
                        // peg the file version to the bundle version. In the future we may allow a distribution
                        // to refer to existing versions of a file.
                        BundleFile bundleFile = bundleManager.addBundleFile(subject, bundleVersion.getId(), fileName,
                            bundleVersion.getVersion(), null, is);
                        log.debug("Added bundle file [" + bundleFile + "] to BundleVersion [" + bundleVersion + "]");
                    } finally {
                        safeClose(is);
                        if (null != file) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // we failed to add one or more bundle files to the bundle version. Since this means the distribution file
            // did not fully get its bundle data persisted, we need to abort the entire effort. Let's delete
            // the bundle version including the bundle definition if we were the ones that initially created it
            // (thus this should completely wipe the database of any knowledge of what we just did previously)
            log.error("Failed to add bundle file to new bundle version [" + bundleVersion
                + "], will not create the new bundle", e);
            try {
                bundleManager.deleteBundleVersion(subjectManager.getOverlord(), bundleVersion.getId(), createdBundle);
            } catch (Exception e1) {
                log.error("Failed to delete the partially created bundle version: " + bundleVersion, e1);
            }
            throw e;
        }

        // because the distribution file can define things like bundle files and default tags, let's
        // ask for the full bundle version data so we can return that back to the caller; thus we let
        // the caller know exactly what the distribution file had inside of it and what we persisted to the DB
        BundleVersionCriteria bvCriteria = new BundleVersionCriteria();
        bvCriteria.addFilterId(bundleVersion.getId());
        bvCriteria.fetchBundle(true);
        bvCriteria.fetchBundleFiles(true);
        bvCriteria.fetchConfigurationDefinition(true);
        bvCriteria.fetchTags(true);
        PageList<BundleVersion> bundleVersions = bundleManager.findBundleVersionsByCriteria(subject, bvCriteria);
        if (bundleVersions != null && bundleVersions.size() == 1) {
            bundleVersion = bundleVersions.get(0);
            List<BundleFile> bundleFiles = bundleVersion.getBundleFiles();
            if (bundleFiles != null && bundleFiles.size() > 0) {
                BundleFileCriteria bfCriteria = new BundleFileCriteria();
                bfCriteria.addFilterBundleVersionId(bundleVersion.getId());
                bfCriteria.fetchPackageVersion(true);
                PageList<BundleFile> bfs = bundleManager.findBundleFilesByCriteria(subjectManager.getOverlord(),
                    bfCriteria);
                bundleFiles.clear();
                bundleFiles.addAll(bfs);
            }
            bundleVersion.setBundleDeployments(new ArrayList<BundleDeployment>());
        } else {
            log.error("Failed to obtain the full bundle version, returning only what we currently know about it: "
                + bundleVersion);
        }

        return bundleVersion;
    }

    @SuppressWarnings("unchecked")
    private String getVersion(String version, Bundle bundle) {
        if (null != version && version.trim().length() > 0) {
            return version;
        }

        BundleVersion latestBundleVersion = null;
        Query q = entityManager.createNamedQuery(BundleVersion.QUERY_FIND_LATEST_BY_BUNDLE_ID);
        q.setParameter("bundleId", bundle.getId());
        List<BundleVersion> list = q.getResultList();
        if (list.size() > 0) {
            if (list.size() == 1) {
                latestBundleVersion = list.get(0);
            } else {
                throw new RuntimeException("Bundle [" + bundle.getName() + "] (id=" + bundle.getId()
                    + ") has more than 1 'latest' version. This should not happen - aborting");
            }
        }

        // note - this is the same algo used by ResourceClientProxy in updatebackingContent (for a resource)
        String latestVersion = latestBundleVersion != null ? latestBundleVersion.getVersion() : null;
        String newVersion = NumberUtil.autoIncrementVersion(latestVersion);
        return newVersion;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleFile addBundleFile(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, InputStream fileStream) throws Exception {

        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleFileName: " + name);
        }
        if (null == version || "".equals(version.trim())) {
            throw new IllegalArgumentException("Invalid bundleFileVersion: " + version);
        }
        if (null == fileStream) {
            throw new IllegalArgumentException("Invalid fileStream: " + null);
        }
        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
        }

        // Create the PackageVersion the BundleFile is tied to.  This implicitly creates the
        // Package for the PackageVersion.
        Bundle bundle = bundleVersion.getBundle();
        PackageType packageType = bundle.getPackageType();
        architecture = (null == architecture) ? contentManager.getNoArchitecture() : architecture;
        if (architecture.getId() == 0) {
            Query q = entityManager.createNamedQuery(Architecture.QUERY_FIND_BY_NAME);
            q.setParameter("name", architecture.getName());
            architecture = (Architecture) q.getSingleResult();
        }
        PackageVersion packageVersion = contentManager.createPackageVersion(name, packageType.getId(), version,
            architecture.getId(), fileStream);

        // set the PackageVersion's filename to the bundleFile name, it's left null by default
        packageVersion.setFileName(name);
        packageVersion = entityManager.merge(packageVersion);

        // Create the mapping between the Bundle's Repo and the BundleFile's PackageVersion
        Repo repo = bundle.getRepo();
        repoManager.addPackageVersionsToRepo(subject, repo.getId(), new int[] { packageVersion.getId() });

        // Classify the Package with the Bundle name in order to distinguish it from the same package name for
        // a different bundle.
        Package generalPackage = packageVersion.getGeneralPackage();
        generalPackage.setClassification(bundle.getName());

        // With all the plumbing in place, create and persist the BundleFile. Tie it to the Package if the caller
        // wants this BundleFile pinned to themost recent version.
        BundleFile bundleFile = new BundleFile();
        bundleFile.setBundleVersion(bundleVersion);
        bundleFile.setPackageVersion(packageVersion);

        entityManager.persist(bundleFile);

        return bundleFile;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleFile addBundleFileViaByteArray(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, byte[] fileBytes) throws Exception {

        return addBundleFile(subject, bundleVersionId, name, version, architecture, new ByteArrayInputStream(fileBytes));
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleFile addBundleFileViaURL(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, String bundleFileUrl) throws Exception {

        // validate by immediately creating a URL
        URL url = new URL(bundleFileUrl);

        return addBundleFile(subject, bundleVersionId, name, version, architecture, url.openStream());
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleFile addBundleFileViaPackageVersion(Subject subject, int bundleVersionId, String name,
        int packageVersionId) throws Exception {

        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleFileName: " + name);
        }
        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
        }
        PackageVersion packageVersion = entityManager.find(PackageVersion.class, packageVersionId);
        if (null == packageVersion) {
            throw new IllegalArgumentException("Invalid packageVersionId: " + packageVersionId);
        }

        // With all the plumbing in place, create and persist the BundleFile. Tie it to the Package if the caller
        // wants this BundleFile pinned to themost recent version.
        BundleFile bundleFile = new BundleFile();
        bundleFile.setBundleVersion(bundleVersion);
        bundleFile.setPackageVersion(packageVersion);

        entityManager.persist(bundleFile);

        return bundleFile;
    }

    /** TODO: Remove after we finalize the move to group only deployment in the public API
     * 
     *  
    public BundleResourceDeployment scheduleBundleResourceDeployment(Subject subject, int bundleDeploymentId,
        int resourceId) throws Exception {
        BundleDeployment deployment = entityManager.find(BundleDeployment.class, bundleDeploymentId);
        if (null == deployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + bundleDeploymentId);
        }

        Resource resource = (Resource) entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new IllegalArgumentException("Invalid resourceId (Resource does not exist): " + resourceId);
        }

        return scheduleBundleResourceDeployment(subject, deployment, resource, null);
    }
    */

    private BundleResourceDeployment scheduleBundleResourceDeployment(Subject subject, BundleDeployment deployment,
        Resource resource, BundleGroupDeployment groupDeployment) throws Exception {

        int resourceId = resource.getId();
        AgentClient agentClient = agentManager.getAgentClient(resourceId);
        BundleAgentService bundleAgentService = agentClient.getBundleAgentService();

        // The BundleResourceDeployment record must exist in the db before the agent request because the agent may try to
        // add History to it during immediate deployments., so create and persist it (requires a new trans).
        BundleResourceDeployment resourceDeployment = bundleManager.createBundleResourceDeployment(subject, deployment
            .getId(), resourceId, (null == groupDeployment) ? 0 : groupDeployment.getId());

        // make sure the deployment contains the info required by the schedule service
        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, deployment.getBundleVersion().getId());
        Configuration config = entityManager.find(Configuration.class, deployment.getConfiguration().getId());
        Bundle bundle = entityManager.find(Bundle.class, bundleVersion.getBundle().getId());
        BundleType bundleType = entityManager.find(BundleType.class, bundle.getBundleType().getId());
        ResourceType resourceType = entityManager.find(ResourceType.class, bundleType.getResourceType().getId());
        bundleType.setResourceType(resourceType);
        bundle.setBundleType(bundleType);
        bundleVersion.setBundle(bundle);
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        resourceDeployment.setBundleDeployment(deployment);
        resourceDeployment.setResource(resource);

        // now scrub the hibernate entity to make it a pojo suitable for sending to the client
        HibernateDetachUtility.nullOutUninitializedFields(resourceDeployment, SerializationType.SERIALIZATION);

        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);

        // add the deployment request history (in a new trans)
        BundleResourceDeploymentHistory history = new BundleResourceDeploymentHistory(subject.getName(),
            AUDIT_ACTION_DEPLOYMENT_REQUESTED, BundleDeploymentStatus.SUCCESS, "Requested deployment time: "
                + request.getRequestedDeployTimeAsString());
        bundleManager.addBundleResourceDeploymentHistory(subject, resourceDeployment.getId(), history);

        // Ask the agent to schedule the request. The agent should add history as needed.
        BundleScheduleResponse response = bundleAgentService.schedule(request);

        // we don't want to commit the scrubbed entities so clear the changes
        this.entityManager.clear();

        // Handle Schedule Failures. This may include deployment failures for immediate deployment request
        if (!response.isSuccess()) {
            history = new BundleResourceDeploymentHistory(subject.getName(), AUDIT_ACTION_DEPLOYMENT,
                BundleDeploymentStatus.FAILURE, response.getErrorMessage());
            bundleManager.setBundleResourceDeploymentStatus(subject, resourceDeployment.getId(),
                BundleDeploymentStatus.FAILURE);
            bundleManager.addBundleResourceDeploymentHistory(subject, resourceDeployment.getId(), history);
        }

        return resourceDeployment;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleGroupDeployment scheduleBundleGroupDeployment(Subject subject, int bundleDeploymentId,
        int resourceGroupId) throws Exception {

        BundleDeployment deployment = entityManager.find(BundleDeployment.class, bundleDeploymentId);
        if (null == deployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + bundleDeploymentId);
        }
        ResourceGroup resourceGroup = (ResourceGroup) entityManager.find(ResourceGroup.class, resourceGroupId);
        if (null == resourceGroup) {
            throw new IllegalArgumentException("Invalid resourceGroupId (ResourceGroup does not exist): "
                + resourceGroupId);
        }

        /*
         * we need to create the group deployment entity in a new transaction before the rest of the
         * processing of this method; the individual deployments need to reference it.
         */
        BundleGroupDeployment groupDeployment = new BundleGroupDeployment(subject.getName(), deployment, resourceGroup);
        groupDeployment = bundleManager.createBundleGroupDeployment(groupDeployment);

        // Create and persist updates for each of the group members.
        for (Resource resource : resourceGroup.getExplicitResources()) {
            BundleResourceDeployment resourceDeployment = scheduleBundleResourceDeployment(subject, deployment,
                resource, groupDeployment);
            groupDeployment.addResourceDeployment(resourceDeployment);
        }

        return groupDeployment;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleResourceDeployment createBundleResourceDeployment(Subject subject, int bundleDeploymentId,
        int resourceId, int groupDeploymentId) throws Exception {

        BundleDeployment deployment = entityManager.find(BundleDeployment.class, bundleDeploymentId);
        if (null == deployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + bundleDeploymentId);
        }
        Resource resource = (Resource) entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new IllegalArgumentException("Invalid resourceId (Resource does not exist): " + resourceId);
        }

        BundleGroupDeployment groupDeployment = (BundleGroupDeployment) entityManager.find(BundleGroupDeployment.class,
            groupDeploymentId);

        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(deployment, resource,
            groupDeployment);

        entityManager.persist(resourceDeployment);
        return resourceDeployment;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleResourceDeployment setBundleResourceDeploymentStatus(Subject subject, int resourceDeploymentId,
        BundleDeploymentStatus status) throws Exception {

        BundleResourceDeployment resourceDeployment = entityManager.find(BundleResourceDeployment.class,
            resourceDeploymentId);
        if (null == resourceDeployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + resourceDeploymentId);
        }

        resourceDeployment.setStatus(status);
        this.entityManager.persist(resourceDeployment);

        // If this is part of a group deployment then update the group status, if necessary. 
        BundleGroupDeployment groupDeployment = resourceDeployment.getGroupDeployment();
        if ((null != groupDeployment) && (BundleDeploymentStatus.INPROGRESS.equals(groupDeployment.getStatus()))) {
            if (BundleDeploymentStatus.FAILURE.equals(status)) {
                groupDeployment.setStatus(status);
            } else {
                BundleResourceDeploymentCriteria c = new BundleResourceDeploymentCriteria();
                c.addFilterGroupDeploymentId(groupDeployment.getId());
                c.addFilterStatus(BundleDeploymentStatus.INPROGRESS);
                List<BundleResourceDeployment> inProgressDeployments = findBundleResourceDeploymentsByCriteria(subject,
                    c);
                if (inProgressDeployments.isEmpty()) {
                    groupDeployment.setStatus(BundleDeploymentStatus.SUCCESS);
                }
            }
        }

        return resourceDeployment;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public BundleGroupDeployment createBundleGroupDeployment(BundleGroupDeployment groupDeployment) throws Exception {
        entityManager.persist(groupDeployment);
        return groupDeployment;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Set<String> getBundleVersionFilenames(Subject subject, int bundleVersionId, boolean withoutBundleFileOnly)
        throws Exception {

        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
        }

        // parse the recipe (validation occurs here) and get the config def and list of files
        BundleType bundleType = bundleVersion.getBundle().getBundleType();
        RecipeParseResults parseResults = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager()
            .parseRecipe(bundleType.getName(), bundleVersion.getRecipe());

        Set<String> result = parseResults.getBundleFileNames();

        if (withoutBundleFileOnly) {
            List<BundleFile> bundleFiles = bundleVersion.getBundleFiles();
            Set<String> allFilenames = result;
            result = new HashSet<String>(allFilenames.size() - bundleFiles.size());
            for (String filename : allFilenames) {
                boolean found = false;
                for (BundleFile bundleFile : bundleFiles) {
                    String name = bundleFile.getPackageVersion().getGeneralPackage().getName();
                    if (name.equals(filename)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    result.add(filename);
                }
            }
        }

        return result;

    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public HashMap<String, Boolean> getAllBundleVersionFilenames(Subject subject, int bundleVersionId) throws Exception {

        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
        }

        // parse the recipe (validation occurs here) and get the config def and list of files
        BundleType bundleType = bundleVersion.getBundle().getBundleType();
        RecipeParseResults parseResults = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager()
            .parseRecipe(bundleType.getName(), bundleVersion.getRecipe());

        Set<String> filenames = parseResults.getBundleFileNames();
        HashMap<String, Boolean> result = new HashMap<String, Boolean>(filenames.size());

        List<BundleFile> bundleFiles = bundleVersion.getBundleFiles();
        for (String filename : filenames) {
            boolean found = false;
            for (BundleFile bundleFile : bundleFiles) {
                String name = bundleFile.getPackageVersion().getGeneralPackage().getName();
                if (name.equals(filename)) {
                    found = true;
                    break;
                }
            }
            result.put(filename, found);
        }

        return result;

    }

    @SuppressWarnings("unchecked")
    public List<BundleType> getAllBundleTypes(Subject subject) {
        // the list of types will be small, no need to support paging
        Query q = entityManager.createNamedQuery(BundleType.QUERY_FIND_ALL);
        List<BundleType> types = q.getResultList();
        return types;
    }

    public BundleType getBundleType(Subject subject, String bundleTypeName) {
        // the list of types will be small, no need to support paging
        Query q = entityManager.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
        q.setParameter("name", bundleTypeName);
        BundleType type = (BundleType) q.getSingleResult();
        return type;
    }

    public PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<BundleDeployment> queryRunner = new CriteriaQueryRunner<BundleDeployment>(criteria,
            generator, entityManager);
        return queryRunner.execute();
    }

    public PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(Subject subject,
        BundleResourceDeploymentCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            if (criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            }

            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, null,
                subject.getId());
        }

        CriteriaQueryRunner<BundleResourceDeployment> queryRunner = new CriteriaQueryRunner<BundleResourceDeployment>(
            criteria, generator, entityManager);

        return queryRunner.execute();
    }

    public PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<BundleVersion> queryRunner = new CriteriaQueryRunner<BundleVersion>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    public PageList<BundleFile> findBundleFilesByCriteria(Subject subject, BundleFileCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<BundleFile> queryRunner = new CriteriaQueryRunner<BundleFile>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    public PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<Bundle> queryRunner = new CriteriaQueryRunner<Bundle>(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    public PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(Subject subject,
        BundleCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        String replacementSelectList = ""
            + " new org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite( "
            + "   bundle.id,"
            + "   bundle.name,"
            + "   bundle.description,"
            + "   ( SELECT bv1.version FROM bundle.bundleVersions bv1 WHERE bv1.versionOrder = (SELECT MAX(bv2.versionOrder) FROM BundleVersion bv2 WHERE bv2.bundle.id = bundle.id) ) AS latestVersion,"
            + "   ( SELECT COUNT(bv3) FROM bundle.bundleVersions bv3 WHERE bv3.bundle.id = bundle.id) AS deploymentCount ) ";
        generator.alterProjection(replacementSelectList);

        CriteriaQueryRunner<BundleWithLatestVersionComposite> queryRunner = new CriteriaQueryRunner<BundleWithLatestVersionComposite>(
            criteria, generator, entityManager);
        PageList<BundleWithLatestVersionComposite> results = queryRunner.execute();
        return results;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteBundle(Subject subject, int bundleId) throws Exception {
        Bundle bundle = this.entityManager.find(Bundle.class, bundleId);
        if (null == bundle) {
            return;
        }

        Query q = entityManager.createNamedQuery(BundleVersion.QUERY_FIND_BY_BUNDLE_ID);
        q.setParameter("bundleId", bundleId);
        List<BundleVersion> bvs = q.getResultList();
        for (BundleVersion bv : bvs) {
            bundleManager.deleteBundleVersion(subject, bv.getId(), false);
        }

        // we need to whack the Repo once the Bundle no longer refers to it
        Repo bundleRepo = bundle.getRepo();

        this.entityManager.remove(bundle);
        this.entityManager.flush();

        repoManager.deleteRepo(subject, bundleRepo.getId());
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteBundleVersion(Subject subject, int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception {
        BundleVersion bundleVersion = this.entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            return;
        }

        int bundleId = 0;
        if (deleteBundleIfEmpty) {
            bundleId = bundleVersion.getBundle().getId(); // note that we lazy load this if we never plan to delete the bundle
        }

        // remove the bundle version - cascade remove the deployments which will cascade remove the resource deployments.
        this.entityManager.remove(bundleVersion);

        if (deleteBundleIfEmpty) {
            this.entityManager.flush();
            Query q = entityManager.createNamedQuery(BundleVersion.QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundleId);
            if (q.getResultList().size() == 0) {
                // there are no more bundle versions left, blow away the bundle and all repo/bundle files associated with it
                deleteBundle(subject, bundleId);
            }
        }

        return;
    }

    private void safeClose(InputStream is) {
        if (null != is) {
            try {
                is.close();
            } catch (Exception e) {
                log.warn("Failed to close InputStream", e);
            }
        }
    }

    private void safeClose(OutputStream os) {
        if (null != os) {
            try {
                os.close();
            } catch (Exception e) {
                log.warn("Failed to close OutputStream", e);
            }
        }
    }

}
