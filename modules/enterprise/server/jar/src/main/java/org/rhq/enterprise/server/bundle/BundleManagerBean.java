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
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.clientapi.agent.bundle.BundlePurgeRequest;
import org.rhq.core.clientapi.agent.bundle.BundlePurgeResponse;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleRequest;
import org.rhq.core.clientapi.agent.bundle.BundleScheduleResponse;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
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
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.StringUtils;
import org.rhq.core.util.NumberUtil;
import org.rhq.core.util.exception.ThrowableUtil;
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
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
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

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @Override
    public ResourceTypeBundleConfiguration getResourceTypeBundleConfiguration(Subject subject, int compatGroupId)
        throws Exception {

        // Even though its harmless to return metadata (bundle config) about a resource type, we are getting that through
        // a relationship from a resource group. To prevent someone from probing the inventory to see which groups
        // are types that support bundles, we only allow someone to traverse the relationship from group to type
        // if that someone has access to the group.
        if (authorizationManager.canViewGroup(subject, compatGroupId)) {
            Query q = entityManager.createNamedQuery(ResourceType.QUERY_GET_BUNDLE_CONFIG_BY_GROUP_ID);
            q.setParameter("groupId", compatGroupId);
            ResourceTypeBundleConfiguration bundleConfig = null;
            try {
                Configuration config = (Configuration) q.getSingleResult();
                if (config != null) {
                    bundleConfig = new ResourceTypeBundleConfiguration(config);
                }
            } catch (EntityNotFoundException enfe) {
                // ignore this - this is just a group that isn't a compatible group
                // or it is, but its type cannot be a target for bundle deployments
            }

            return bundleConfig;
        } else {
            throw new Exception("[" + subject.getName() + "] is not authorized to access the group");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

        // create the repo as overlord, this allows users without MANAGE_INVENTORY permission to create bundles
        repo = repoManager.createRepo(subjectManager.getOverlord(), repo);

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

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleDeployment createBundleDeploymentInNewTrans(Subject subject, int bundleVersionId,
        int bundleDestinationId, String name, String description, Configuration configuration) throws Exception {

        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
        }
        BundleDestination bundleDestination = entityManager.find(BundleDestination.class, bundleDestinationId);
        if (null == bundleDestination) {
            throw new IllegalArgumentException("Invalid bundleDestinationId: " + bundleVersionId);
        }

        return createBundleDeploymentImpl(subject, bundleVersion, bundleDestination, name, description, configuration);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleDeployment createBundleDeployment(Subject subject, int bundleVersionId, int bundleDestinationId,
        String description, Configuration configuration) throws Exception {

        BundleVersion bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
        }
        BundleDestination bundleDestination = entityManager.find(BundleDestination.class, bundleDestinationId);
        if (null == bundleDestination) {
            throw new IllegalArgumentException("Invalid bundleDestinationId: " + bundleVersionId);
        }

        String name = getBundleDeploymentNameImpl(subject, bundleDestination, bundleVersion, null);
        return this.createBundleDeploymentImpl(subject, bundleVersion, bundleDestination, name, description,
            configuration);
    }

    private BundleDeployment createBundleDeploymentImpl(Subject subject, BundleVersion bundleVersion,
        BundleDestination bundleDestination, String name, String description, Configuration configuration)
        throws Exception {

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

        BundleDeployment deployment = new BundleDeployment(bundleVersion, bundleDestination, name);
        deployment.setDescription(description);
        deployment.setConfiguration(configuration);
        deployment.setSubjectName(subject.getName());

        entityManager.persist(deployment);

        return deployment;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleDestination createBundleDestination(Subject subject, int bundleId, String name, String description,
        String destBaseDirName, String deployDir, Integer groupId) throws Exception {

        Bundle bundle = entityManager.find(Bundle.class, bundleId);
        if (null == bundle) {
            throw new IllegalArgumentException("Invalid bundleId [" + bundleId + "]");
        }

        // validate that the group exists and is a compatible group that can support bundle deployments
        ResourceGroupCriteria c = new ResourceGroupCriteria();
        c.addFilterId(groupId);
        c.addFilterBundleTargetableOnly(true);
        List<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(subject, c);
        if (null == groups || groups.isEmpty()) {
            throw new IllegalArgumentException("Invalid groupId [" + groupId
                + "]. It must be an existing compatible group whose members must be able to support bundle deployments");
        }
        ResourceGroup group = entityManager.find(ResourceGroup.class, groups.get(0).getId());

        BundleDestination dest = new BundleDestination(bundle, name, group, destBaseDirName, deployDir);
        dest.setDescription(description);
        entityManager.persist(dest);

        return dest;
    }

    @Override
    public String getBundleDeploymentName(Subject subject, int bundleDestinationId, int bundleVersionId,
        int prevDeploymentId) {
        BundleDestination bundleDestination = entityManager.find(BundleDestination.class, bundleDestinationId);
        if (null == bundleDestination) {
            throw new IllegalArgumentException("Invalid bundleDestinationId: " + bundleVersionId);
        }

        BundleVersion bundleVersion = null;
        BundleDeployment prevDeployment = null;

        if (bundleVersionId > 0) {
            bundleVersion = entityManager.find(BundleVersion.class, bundleVersionId);
            if (null == bundleVersion) {
                throw new IllegalArgumentException("Invalid bundleVersionId: " + bundleVersionId);
            }
        } else if (prevDeploymentId > 0) {
            prevDeployment = entityManager.find(BundleDeployment.class, prevDeploymentId);
            if (null == prevDeployment) {
                throw new IllegalArgumentException("Invalid prevDeploymentId: " + prevDeploymentId);
            }
        } else {
            throw new IllegalArgumentException("Must specify either a valid bundleVersionId [" + bundleVersionId
                + "] or prevDeploymentId [" + prevDeploymentId + "]");
        }

        return getBundleDeploymentNameImpl(subject, bundleDestination, bundleVersion, prevDeployment);
    }

    private String getBundleDeploymentNameImpl(Subject subject, BundleDestination bundleDestination,
        BundleVersion bundleVersion, BundleDeployment prevDeployment) {

        BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
        criteria.addFilterDestinationId(bundleDestination.getId());
        criteria.addFilterIsLive(true);
        criteria.fetchBundleVersion(true);
        List<BundleDeployment> liveDeployments = bundleManager.findBundleDeploymentsByCriteria(subject, criteria);
        BundleDeployment liveDeployment = (liveDeployments.isEmpty()) ? null : liveDeployments.get(0);

        String deploymentName = null;

        if (null != bundleVersion) {
            boolean isInitialDeployment = (null == liveDeployment);
            int deploy = 1;
            String version = bundleVersion.getVersion();
            String dest = bundleDestination.getName();

            if (isInitialDeployment) {
                deploymentName = "Deployment [" + deploy + "] of Version [" + version + "] to [" + dest + "]";
            } else {
                String liveName = liveDeployment.getName();
                String liveVersion = liveDeployment.getBundleVersion().getVersion();
                if (liveVersion.equals(version)) {
                    // redeploy
                    int iStart = liveName.indexOf("[") + 1, iEnd = liveName.indexOf("]");
                    deploy = Integer.valueOf(liveName.substring(iStart, iEnd)) + 1;
                    deploymentName = "Deployment [" + deploy + "] of Version [" + version + "] to [" + dest + "]";
                } else {
                    // upgrade
                    deploymentName = "Deployment [" + deploy + "] of Version [" + version + "] to [" + dest
                        + "]. Upgrade from Version [" + liveVersion + "]";
                }
            }
        } else {
            // revert
            if (null == liveDeployment) {
                throw new IllegalArgumentException("Invalid Revert, no live deployment for destination"
                    + bundleDestination);
            }

            String liveName = liveDeployment.getName();
            int iStart = liveName.indexOf("[") + 1, iEnd = liveName.indexOf("]");
            int deploy = Integer.valueOf(liveName.substring(iStart, iEnd)) + 1;
            deploymentName = "Deployment [" + deploy + "] Revert To: " + prevDeployment.getName();
        }

        return deploymentName;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleVersion createBundleVersionViaRecipe(Subject subject, String recipe) throws Exception {

        BundleServerPluginManager manager = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager();
        BundleDistributionInfo info = manager.parseRecipe(recipe);
        BundleVersion bundleVersion = createBundleVersionViaDistributionInfo(subject, info);

        return bundleVersion;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public BundleVersion createBundleVersionViaFile(Subject subject, File distributionFile) throws Exception {

        BundleServerPluginManager manager = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager();
        BundleDistributionInfo info = manager.processBundleDistributionFile(distributionFile);
        BundleVersion bundleVersion = createBundleVersionViaDistributionInfo(subject, info);

        return bundleVersion;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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
        PackageVersion packageVersion = contentManager.createPackageVersion(subject, name, packageType.getId(),
            version, architecture.getId(), fileStream);

        // set the PackageVersion's filename to the bundleFile name, it's left null by default
        packageVersion.setFileName(name);
        packageVersion = entityManager.merge(packageVersion);

        // Create the mapping between the Bundle's Repo and the BundleFile's PackageVersion
        Repo repo = bundle.getRepo();
        // add the packageVersion as overlord, this allows users without MANAGE_INVENTORY permission to add bundle files
        repoManager.addPackageVersionsToRepo(subjectManager.getOverlord(), repo.getId(), new int[] { packageVersion
            .getId() });

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

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleFile addBundleFileViaByteArray(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, byte[] fileBytes) throws Exception {

        return addBundleFile(subject, bundleVersionId, name, version, architecture, new ByteArrayInputStream(fileBytes));
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleFile addBundleFileViaURL(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, String bundleFileUrl) throws Exception {

        // validate by immediately creating a URL
        URL url = new URL(bundleFileUrl);

        return addBundleFile(subject, bundleVersionId, name, version, architecture, url.openStream());
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void purgeBundleDestination(Subject subject, int bundleDestinationId) throws Exception {
        // find the live bundle deployment for this destination, and get all the resource deployments for that live deployment
        BundleDeploymentCriteria bdc = new BundleDeploymentCriteria();
        bdc.addFilterDestinationId(bundleDestinationId);
        bdc.addFilterIsLive(true);
        bdc.fetchBundleVersion(true);
        bdc.fetchResourceDeployments(true);
        bdc.fetchDestination(true);
        List<BundleDeployment> liveDeployments = bundleManager.findBundleDeploymentsByCriteria(subject, bdc);
        if (1 != liveDeployments.size()) {
            throw new IllegalArgumentException("No live deployment to purge is found for destinationId ["
                + bundleDestinationId + "]");
        }
        BundleDeployment liveDeployment = liveDeployments.get(0);
        List<BundleResourceDeployment> resourceDeploys = liveDeployment.getResourceDeployments();
        if (resourceDeploys == null || resourceDeploys.isEmpty()) {
            return; // nothing to do
        }

        // we need to obtain the bundle type (the remote plugin container needs it). our first criteria can't fetch this deep, we have to do another query.
        BundleVersionCriteria bvc = new BundleVersionCriteria();
        bvc.addFilterId(liveDeployment.getBundleVersion().getId());
        bvc.fetchBundle(true); // will eagerly fetch the bundle type
        PageList<BundleVersion> bvs = bundleManager.findBundleVersionsByCriteria(subject, bvc);
        liveDeployment.setBundleVersion(bvs.get(0)); // wire up the full bundle version back into the live deployment
        // the bundle type doesn't eagerly load the resource type - the remote plugin container needs that too
        ResourceTypeCriteria rtc = new ResourceTypeCriteria();
        rtc.addFilterBundleTypeId(liveDeployment.getBundleVersion().getBundle().getBundleType().getId());
        PageList<ResourceType> rts = resourceTypeManager.findResourceTypesByCriteria(subject, rtc);
        liveDeployment.getBundleVersion().getBundle().getBundleType().setResourceType(rts.get(0));

        // we need to obtain the resources for all resource deployments - our first criteria can't fetch this deep, we have to do another query.
        List<Integer> resourceDeployIds = new ArrayList<Integer>();
        for (BundleResourceDeployment resourceDeploy : resourceDeploys) {
            resourceDeployIds.add(resourceDeploy.getId());
        }
        BundleResourceDeploymentCriteria brdc = new BundleResourceDeploymentCriteria();
        brdc.addFilterIds(resourceDeployIds.toArray(new Integer[resourceDeployIds.size()]));
        brdc.fetchResource(true);
        brdc.setPageControl(PageControl.getUnlimitedInstance());
        PageList<BundleResourceDeployment> brdResults = bundleManager.findBundleResourceDeploymentsByCriteria(subject,
            brdc);
        resourceDeploys.clear();
        resourceDeploys.addAll(brdResults);
        // need to wire the live bundle deployment back in - no need for another query or fetch it above because we have it already
        for (BundleResourceDeployment brd : brdResults) {
            brd.setBundleDeployment(liveDeployment);
        }

        // loop through each deployment and purge it on agent
        Map<BundleResourceDeployment, String> failedToPurge = new HashMap<BundleResourceDeployment, String>();
        for (BundleResourceDeployment resourceDeploy : resourceDeploys) {
            try {
                // first put the user name that requested the purge in the audit trail
                BundleResourceDeploymentHistory history = new BundleResourceDeploymentHistory(subject.getName(),
                    "Purge Requested", "User [" + subject.getName() + "] requested to purge this deployment", null,
                    BundleResourceDeploymentHistory.Status.SUCCESS, null, null);
                bundleManager.addBundleResourceDeploymentHistory(subject, resourceDeploy.getId(), history);

                // get a connection to the agent and tell it to purge the bundle from the file system
                Subject overlord = subjectManager.getOverlord();
                AgentClient agentClient = agentManager.getAgentClient(overlord, resourceDeploy.getResource().getId());
                BundleAgentService bundleAgentService = agentClient.getBundleAgentService();
                BundlePurgeRequest request = new BundlePurgeRequest(resourceDeploy);
                BundlePurgeResponse results = bundleAgentService.purge(request);
                if (!results.isSuccess()) {
                    String errorMessage = results.getErrorMessage();
                    failedToPurge.put(resourceDeploy, errorMessage);
                }
            } catch (Exception e) {
                String errorMessage = ThrowableUtil.getStackAsString(e);
                failedToPurge.put(resourceDeploy, errorMessage);
            }
        }

        // marks the live deployment "no longer live"
        bundleManager._finalizePurge(subjectManager.getOverlord(), liveDeployment, failedToPurge);

        // throw an exception if we failed to purge one or more resource deployments.
        // since we are not in a tx context, we lose nothing. All DB updates have already been committed by now
        // which is what we want. All this does is inform the caller something went wrong.
        if (!failedToPurge.isEmpty()) {
            int totalDeployments = liveDeployment.getResourceDeployments().size();
            int failedPurges = failedToPurge.size();
            throw new Exception("Failed to purge [" + failedPurges + "] of [" + totalDeployments
                + "] remote resource deployments");
        }
        return;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_SECURITY)
    // no one should be calling us except overlord
    public void _finalizePurge(Subject subject, BundleDeployment bundleDeployment,
        Map<BundleResourceDeployment, String> failedToPurge) throws Exception {

        bundleDeployment = entityManager.find(BundleDeployment.class, bundleDeployment.getId());
        if (failedToPurge.isEmpty()) {
            bundleDeployment.setLive(false); // all deployments are purged, no where is this live anymore
            bundleDeployment.setErrorMessage(null);
            bundleDeployment.setStatus(BundleDeploymentStatus.SUCCESS);
        } else {
            bundleDeployment.setLive(true); // not all deployments are purged - error indicates it is still live somewhere

            StringBuilder errorStr = new StringBuilder();
            int totalDeployments = bundleDeployment.getResourceDeployments().size();
            int failedPurges = failedToPurge.size();
            if (failedPurges < totalDeployments) {
                bundleDeployment.setStatus(BundleDeploymentStatus.MIXED); // some deployments were purged, so show MIXED status
                errorStr.append("Failed to purge [" + failedPurges + "] of [" + totalDeployments
                    + "] remote resource deployments");
            } else {
                bundleDeployment.setStatus(BundleDeploymentStatus.FAILURE); // all deployments failed to be purged
                errorStr.append("Failed to purge all [" + failedPurges + "] remote resource deployments");
            }

            // key is the resource deployment that failed to be purged; value is the error message
            for (Map.Entry<BundleResourceDeployment, String> entry : failedToPurge.entrySet()) {
                errorStr.append("\n\n");
                errorStr.append(entry.getKey().getResource().getName()).append(": ").append(entry.getValue());
            }

            bundleDeployment.setErrorMessage(errorStr.toString());
        }

        return;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleDeployment scheduleBundleDeployment(Subject subject, int bundleDeploymentId, boolean isCleanDeployment)
        throws Exception {
        return scheduleBundleDeploymentImpl(subject, bundleDeploymentId, isCleanDeployment, false, null);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleDeployment scheduleRevertBundleDeployment(Subject subject, int bundleDestinationId,
        String deploymentDescription, boolean isCleanDeployment) throws Exception {

        BundleDeploymentCriteria c = new BundleDeploymentCriteria();
        c.addFilterDestinationId(bundleDestinationId);
        c.addFilterIsLive(true);
        c.fetchDestination(true);
        List<BundleDeployment> liveDeployments = bundleManager.findBundleDeploymentsByCriteria(subject, c);
        if (1 != liveDeployments.size()) {
            throw new IllegalArgumentException("No live deployment found for destinationId [" + bundleDestinationId
                + "]");
        }
        BundleDeployment liveDeployment = liveDeployments.get(0);
        Integer prevDeploymentId = liveDeployment.getReplacedBundleDeploymentId();
        if (null == prevDeploymentId) {
            throw new IllegalArgumentException(
                "Live deployment ["
                    + liveDeployment
                    + "] can not be reverted. The Live deployment is either an initial deployment or a reverted deployment for destinationId ["
                    + bundleDestinationId + "]");
        }
        BundleDeployment prevDeployment = entityManager.find(BundleDeployment.class, prevDeploymentId);
        if (null == prevDeployment) {
            throw new IllegalArgumentException("Live deployment [" + liveDeployment
                + "] can not be reverted. There is no prior deployment for destinationId [" + bundleDestinationId + "]");
        }

        // A revert is done by deploying a new deployment that mirrors "prevDeployment". It uses the same
        // bundleVersion, destination and config as prevDeployment.  It can have a new name and new desc, and
        // may opt to clean the deploy dir.  It must be a new deployment so that all status/auditing/history starts
        // fresh and can be tracked. The key difference in the schedule request is that we set isRevert=true,
        // tell the bundle handler that we are in fact reverting from the current live deployment. The
        // deployment creation is done in a new transaction so it can then be scheduled.
        String name = getBundleDeploymentNameImpl(subject, liveDeployment.getDestination(), null, prevDeployment);
        String desc = (null != deploymentDescription) ? deploymentDescription : prevDeployment.getDescription();
        Configuration config = (null == prevDeployment.getConfiguration()) ? null : prevDeployment.getConfiguration()
            .deepCopy(false);
        BundleDeployment revertDeployment = bundleManager.createBundleDeploymentInNewTrans(subject, prevDeployment
            .getBundleVersion().getId(), bundleDestinationId, name, desc, config);

        return scheduleBundleDeploymentImpl(subject, revertDeployment.getId(), isCleanDeployment, true, prevDeployment
            .getReplacedBundleDeploymentId());
    }

    // revertedDeploymentReplacedDeployment is only meaningful if isRevert is true
    private BundleDeployment scheduleBundleDeploymentImpl(Subject subject, int bundleDeploymentId,
        boolean isCleanDeployment, boolean isRevert, Integer revertedDeploymentReplacedDeployment) throws Exception {

        BundleDeployment newDeployment = entityManager.find(BundleDeployment.class, bundleDeploymentId);
        if (null == newDeployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + bundleDeploymentId);
        }

        BundleDestination destination = newDeployment.getDestination();
        ResourceGroup group = destination.getGroup();

        // Create and persist updates for each of the group members.
        Set<Resource> platforms = group.getExplicitResources();
        if (platforms.isEmpty()) {
            throw new IllegalArgumentException("Destination [" + destination
                + "] group has no platforms. Invalid deployment destination");
        }

        for (Resource platform : platforms) {
            try {
                scheduleBundleResourceDeployment(subject, newDeployment, platform, isCleanDeployment, isRevert);
            } catch (Throwable t) {
                log.error("Failed to complete scheduling of platform deployment to [" + platform
                    + "]. Other platforms may have been scheduled. ", t);
            }
        }

        // make sure the new deployment is set as the live deployment and properly replaces the
        // previously live deployment.
        destination = entityManager.find(BundleDestination.class, destination.getId());
        List<BundleDeployment> currentDeployments = destination.getDeployments();
        if (null != currentDeployments) {
            for (BundleDeployment d : currentDeployments) {
                if (d.isLive()) {
                    d.setLive(false);
                    if (!isRevert) {
                        newDeployment.setReplacedBundleDeploymentId(d.getId());
                    } else {
                        // we are doing a revert; so our "replacedDeployment" should be what the deployment we
                        // are reverting to replaced. For example, assume I deployed three bundles:
                        //   Deployment #1 - replaced nothing (hence replacedBundleDeploymentId == null)
                        //   Deployment #2 - replaced #1
                        //   Deployment #3 - replaced #2
                        // Now do a revert. Reverting the live deployment #3 means we really want to re-deploy #2.
                        // This new deployment gets a new ID of #4, but it is actually a deployment equivalent to #2.
                        // If our deploy #4 is actually a redeploy of #2, we need to prepare for the user wanting
                        // to revert #4 by setting the replacedBundleDeploymentId to that which #2 had - this being #1.
                        //   Deployment #4 - replaced #1
                        // Now if we ask to revert #4, we will actually be re-deploying #1, which is what we want.
                        // This allows us to revert back multiple steps.
                        newDeployment.setReplacedBundleDeploymentId(revertedDeploymentReplacedDeployment);
                    }
                    break;
                }
            }
        }
        newDeployment.setLive(true);

        return newDeployment;
    }

    private BundleResourceDeployment scheduleBundleResourceDeployment(Subject subject, BundleDeployment deployment,
        Resource platform, boolean isCleanDeployment, boolean isRevert) throws Exception {

        int platformId = platform.getId();
        AgentClient agentClient = agentManager.getAgentClient(subjectManager.getOverlord(), platformId);
        BundleAgentService bundleAgentService = agentClient.getBundleAgentService();

        // The BundleResourceDeployment record must exist in the db before the agent request because the agent may try        
        // to add History to it during immediate deployments. So, create and persist it (requires a new trans).
        BundleResourceDeployment resourceDeployment = bundleManager.createBundleResourceDeployment(subject, deployment
            .getId(), platformId);

        if (ResourceCategory.PLATFORM.equals(platform.getResourceType().getCategory())) {

            // Ask the agent to schedule the request. The agent should add history as needed.
            try {
                BundleScheduleRequest request = bundleManager.getScheduleRequest(subject, resourceDeployment.getId(),
                    isCleanDeployment, isRevert);

                // add the deployment request history (in a new trans)
                BundleResourceDeploymentHistory history = new BundleResourceDeploymentHistory(subject.getName(),
                    AUDIT_ACTION_DEPLOYMENT_REQUESTED, deployment.getName(), null,
                    BundleResourceDeploymentHistory.Status.SUCCESS, "Requested deployment time: "
                        + request.getRequestedDeployTimeAsString(), null);
                bundleManager.addBundleResourceDeploymentHistory(subject, resourceDeployment.getId(), history);

                BundleScheduleResponse response = bundleAgentService.schedule(request);

                // Handle Schedule Failures. This may include deployment failures for immediate deployment request
                if (!response.isSuccess()) {
                    bundleManager.setBundleResourceDeploymentStatus(subject, resourceDeployment.getId(),
                        BundleDeploymentStatus.FAILURE);
                    history = new BundleResourceDeploymentHistory(subject.getName(), AUDIT_ACTION_DEPLOYMENT,
                        deployment.getName(), null, BundleResourceDeploymentHistory.Status.FAILURE, response
                            .getErrorMessage(), null);
                    bundleManager.addBundleResourceDeploymentHistory(subject, resourceDeployment.getId(), history);
                }
            } catch (Throwable t) {
                // fail the unlaunched resource deployment
                BundleResourceDeploymentHistory failureHistory = new BundleResourceDeploymentHistory(subject.getName(),
                    this.AUDIT_ACTION_DEPLOYMENT, deployment.getName(), null,
                    BundleResourceDeploymentHistory.Status.FAILURE, "Failed to schedule, agent on [" + platform
                        + "] may be down: " + t, null);
                bundleManager.addBundleResourceDeploymentHistory(subject, resourceDeployment.getId(), failureHistory);
                bundleManager.setBundleResourceDeploymentStatus(subject, resourceDeployment.getId(),
                    BundleDeploymentStatus.FAILURE);
            }

        } else {
            bundleManager.setBundleResourceDeploymentStatus(subject, resourceDeployment.getId(),
                BundleDeploymentStatus.FAILURE);
            BundleResourceDeploymentHistory history = new BundleResourceDeploymentHistory(subject.getName(),
                AUDIT_ACTION_DEPLOYMENT, deployment.getName(), null, BundleResourceDeploymentHistory.Status.FAILURE,
                "Target resource is not a platform [id=" + platform.getId() + "]. Fix target group for destination ["
                    + deployment.getDestination().getName() + "]", null);
            bundleManager.addBundleResourceDeploymentHistory(subject, resourceDeployment.getId(), history);
        }

        return resourceDeployment;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleScheduleRequest getScheduleRequest(Subject subject, int resourceDeploymentId,
        boolean isCleanDeployment, boolean isRevert) throws Exception {

        // make sure the deployment contains the info required by the schedule service
        BundleResourceDeploymentCriteria brdc = new BundleResourceDeploymentCriteria();
        brdc.addFilterId(resourceDeploymentId);
        brdc.fetchResource(true);
        brdc.fetchBundleDeployment(true);
        List<BundleResourceDeployment> resourceDeployments = bundleManager.findBundleResourceDeploymentsByCriteria(
            subject, brdc);
        if (null == resourceDeployments || resourceDeployments.isEmpty()) {
            throw new IllegalArgumentException("Can not deploy using invalid resourceDeploymentId ["
                + resourceDeploymentId + "].");
        }
        BundleResourceDeployment resourceDeployment = resourceDeployments.get(0);

        // make sure the deployment contains the info required by the schedule service
        BundleDeploymentCriteria bdc = new BundleDeploymentCriteria();
        bdc.addFilterId(resourceDeployment.getBundleDeployment().getId());
        bdc.fetchBundleVersion(true);
        bdc.fetchConfiguration(true);
        bdc.fetchDestination(true);
        BundleDeployment deployment = bundleManager.findBundleDeploymentsByCriteria(subject, bdc).get(0);

        BundleCriteria bc = new BundleCriteria();
        bc.addFilterDestinationId(deployment.getDestination().getId());
        Bundle bundle = bundleManager.findBundlesByCriteria(subject, bc).get(0);

        ResourceTypeCriteria rtc = new ResourceTypeCriteria();
        rtc.addFilterBundleTypeId(bundle.getBundleType().getId());
        ResourceType resourceType = resourceTypeManager.findResourceTypesByCriteria(subject, rtc).get(0);
        bundle.getBundleType().setResourceType(resourceType);

        deployment.getBundleVersion().setBundle(bundle);
        deployment.getDestination().setBundle(bundle);

        resourceDeployment.setBundleDeployment(deployment);

        // now scrub the hibernate entity to make it a pojo suitable for sending to the client
        HibernateDetachUtility.nullOutUninitializedFields(resourceDeployment, SerializationType.SERIALIZATION);

        BundleScheduleRequest request = new BundleScheduleRequest(resourceDeployment);
        request.setCleanDeployment(isCleanDeployment);
        request.setRevert(isRevert);

        entityManager.clear();
        return request;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleResourceDeployment createBundleResourceDeployment(Subject subject, int bundleDeploymentId,
        int resourceId) throws Exception {

        BundleDeployment deployment = entityManager.find(BundleDeployment.class, bundleDeploymentId);
        if (null == deployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + bundleDeploymentId);
        }
        Resource resource = (Resource) entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new IllegalArgumentException("Invalid resourceId (Resource does not exist): " + resourceId);
        }

        BundleResourceDeployment resourceDeployment = new BundleResourceDeployment(deployment, resource);

        entityManager.persist(resourceDeployment);
        return resourceDeployment;
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public BundleResourceDeployment setBundleResourceDeploymentStatus(Subject subject, int resourceDeploymentId,
        BundleDeploymentStatus status) throws Exception {

        // set the status of the individual resource deployment
        BundleResourceDeployment resourceDeployment = entityManager.find(BundleResourceDeployment.class,
            resourceDeploymentId);
        if (null == resourceDeployment) {
            throw new IllegalArgumentException("Invalid bundleDeploymentId: " + resourceDeploymentId);
        }

        // update the status
        resourceDeployment.setStatus(status);

        // update the status on the overall deployment
        BundleDeployment deployment = resourceDeployment.getBundleDeployment();

        List<BundleResourceDeployment> deployments = deployment.getResourceDeployments();
        boolean someInProgress = false;
        boolean someSuccess = false;
        boolean someFailure = false;
        for (BundleResourceDeployment rd : deployments) {
            switch (rd.getStatus()) {
            case SUCCESS:
                someSuccess = true;
                break;
            case FAILURE:
                someFailure = true;
                break;
            case IN_PROGRESS:
                someInProgress = true;
                break;
            }
        }
        if (someInProgress) {
            deployment.setStatus(BundleDeploymentStatus.IN_PROGRESS);
        } else if (someSuccess) {
            deployment.setStatus(someFailure ? BundleDeploymentStatus.MIXED : BundleDeploymentStatus.SUCCESS);
        } else {
            deployment.setStatus(BundleDeploymentStatus.FAILURE);
        }

        return resourceDeployment;
    }

    //    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    //  public BundleGroupDeployment createBundleGroupDeployment(BundleGroupDeployment groupDeployment) throws Exception {
    //    entityManager.persist(groupDeployment);
    //  return groupDeployment;
    //}

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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

    @Override
    @SuppressWarnings("unchecked")
    public List<BundleType> getAllBundleTypes(Subject subject) {
        // the list of types will be small, no need to support paging
        Query q = entityManager.createNamedQuery(BundleType.QUERY_FIND_ALL);
        List<BundleType> types = q.getResultList();
        return types;
    }

    @Override
    public BundleType getBundleType(Subject subject, String bundleTypeName) {
        // the list of types will be small, no need to support paging
        Query q = entityManager.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
        q.setParameter("name", bundleTypeName);
        BundleType type = (BundleType) q.getSingleResult();
        return type;
    }

    @Override
    public PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<BundleDeployment> queryRunner = new CriteriaQueryRunner<BundleDeployment>(criteria,
            generator, entityManager);
        return queryRunner.execute();
    }

    @Override
    public PageList<BundleDestination> findBundleDestinationsByCriteria(Subject subject,
        BundleDestinationCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<BundleDestination> queryRunner = new CriteriaQueryRunner<BundleDestination>(criteria,
            generator, entityManager);
        return queryRunner.execute();
    }

    @Override
    public PageList<BundleResourceDeployment> findBundleResourceDeploymentsByCriteria(Subject subject,
        BundleResourceDeploymentCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);

        if (!authorizationManager.isInventoryManager(subject)) {
            if (criteria.isInventoryManagerRequired()) {
                // TODO: MANAGE_INVENTORY was too restrictive as a bundle manager could not then
                // see his resource deployments. Until we can handle granular authorization checks on
                // optionally fetched resource member data, allow a bundle manager to see
                // resouce deployments to any platform.
                if (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_BUNDLE)) {
                    throw new PermissionException("Subject [" + subject.getName()
                        + "] requires InventoryManager or BundleManager permission for requested query criteria.");
                }
            }
        }

        CriteriaQueryRunner<BundleResourceDeployment> queryRunner = new CriteriaQueryRunner<BundleResourceDeployment>(
            criteria, generator, entityManager);

        return queryRunner.execute();
    }

    @Override
    public PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<BundleVersion> queryRunner = new CriteriaQueryRunner<BundleVersion>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    @Override
    public PageList<BundleFile> findBundleFilesByCriteria(Subject subject, BundleFileCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<BundleFile> queryRunner = new CriteriaQueryRunner<BundleFile>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    @Override
    public PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<Bundle> queryRunner = new CriteriaQueryRunner<Bundle>(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @Override
    public PageList<BundleWithLatestVersionComposite> findBundlesWithLatestVersionCompositesByCriteria(Subject subject,
        BundleCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
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

    // to avoid deadlocks, you cannot delete multiple bundles concurrently (see BZ 606530)
    // instead, this simple method just loops over the given array and deletes them serially
    // note they all get deleted in their own transaction; this method is never in a tx itself
    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void deleteBundles(Subject subject, int[] bundleIds) throws Exception {
        if (bundleIds != null) {
            for (int bundleId : bundleIds) {
                bundleManager.deleteBundle(subject, bundleId);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_BUNDLE)
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
            entityManager.flush();
        }

        // we need to whack the Repo once the Bundle no longer refers to it
        Repo bundleRepo = bundle.getRepo();

        this.entityManager.remove(bundle);
        this.entityManager.flush();

        // delete the repo as overlord, this allows users without MANAGE_INVENTORY permission to delete bundles
        repoManager.deleteRepo(subjectManager.getOverlord(), bundleRepo.getId());
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public void deleteBundleDeployment(Subject subject, int bundleDeploymentId) throws Exception {
        BundleDeployment doomed = this.entityManager.find(BundleDeployment.class, bundleDeploymentId);
        if (null == doomed) {
            return;
        }
        // only allow deployments to be deleted if they are finished
        if (BundleDeploymentStatus.SUCCESS == doomed.getStatus()
            || BundleDeploymentStatus.FAILURE == doomed.getStatus()
            || BundleDeploymentStatus.MIXED == doomed.getStatus()) {
            entityManager.remove(doomed);
        } else {
            throw new IllegalArgumentException("Can not delete deployment with status [" + doomed.getStatus() + "]");
        }
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public void deleteBundleDestination(Subject subject, int destinationId) throws Exception {
        BundleDestination doomed = this.entityManager.find(BundleDestination.class, destinationId);
        if (null == doomed) {
            return;
        }

        // deployments replace other deployments and have a self-referring FK.  The deployments
        // need to be removed in a way that will ensure that a replaced deployment is not removed
        // prior to the replacer.  To do this we'll just blanket update all the doomed deployments
        // to break the FK dependency with nulls.
        Query q = entityManager.createNamedQuery(BundleDeployment.QUERY_UPDATE_FOR_DESTINATION_REMOVE);
        q.setParameter("destinationId", destinationId);
        q.executeUpdate();
        entityManager.flush();

        entityManager.remove(doomed);
    }

    @Override
    @RequiredPermission(Permission.MANAGE_BUNDLE)
    public void deleteBundleVersion(Subject subject, int bundleVersionId, boolean deleteBundleIfEmpty) throws Exception {
        BundleVersion bundleVersion = this.entityManager.find(BundleVersion.class, bundleVersionId);
        if (null == bundleVersion) {
            return;
        }

        int bundleId = 0;
        if (deleteBundleIfEmpty) {
            bundleId = bundleVersion.getBundle().getId(); // note that we lazy load this if we never plan to delete the bundle
        }

        // deployments replace other deployments and have a self-referring FK.  The deployments
        // need to be removed in a way that will ensure that a replaced deployment is not removed
        // prior to the replacer.  To do this we'll just blanket update all the doomed deployments
        // to break the FK dependency with nulls.
        Query q = entityManager.createNamedQuery(BundleDeployment.QUERY_UPDATE_FOR_VERSION_REMOVE);
        q.setParameter("bundleVersionId", bundleVersionId);
        @SuppressWarnings("unused")
        int rowsUpdated = q.executeUpdate();
        entityManager.flush();

        // remove the bundle version - cascade remove the deployments which will cascade remove the resource deployments.
        this.entityManager.remove(bundleVersion);

        if (deleteBundleIfEmpty) {
            this.entityManager.flush();
            q = entityManager.createNamedQuery(BundleVersion.QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID);
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
