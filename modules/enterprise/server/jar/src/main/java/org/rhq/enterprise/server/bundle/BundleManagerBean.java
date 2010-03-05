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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeployDefinitionCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * Manages the creation and usage of bundles.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Stateless
public class BundleManagerBean implements BundleManagerLocal, BundleManagerRemote {
    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private ContentManagerLocal contentManager;

    @EJB
    private RepoManagerLocal repoManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Bundle createBundle(Subject subject, String name, int bundleTypeId) {
        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleName: " + name);
        }

        BundleType bundleType = entityManager.find(BundleType.class, bundleTypeId);
        if (null == bundleType) {
            throw new IllegalArgumentException("Invalid bundleTypeId: " + bundleTypeId);
        }

        Bundle bundle = new Bundle(name, bundleType);

        // add the implicit bundle repo
        Repo repo = new Repo(name);
        repo.setCandidate(false);
        repo.setSyncSchedule(null);
        bundle.setRepo(repo);

        log.info("Creating bundle: " + bundle);
        entityManager.persist(bundle);

        return bundle;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleDeployDefinition createBundleDeployDefinition(Subject subject, int bundleVersionId, String name,
        String description, Configuration configuration, boolean enforcePolicy, int enforcementInterval,
        boolean pinToBundle) throws Exception {

        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleDeployDefinitionName: " + name);
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

        BundleDeployDefinition deployDef = new BundleDeployDefinition(bundleVersion, name);
        deployDef.setDescription(description);
        deployDef.setConfiguration(configuration);
        deployDef.setEnforcePolicy(enforcePolicy);
        deployDef.setEnforcementInterval(enforcementInterval);
        if (pinToBundle) {
            deployDef.setBundle(bundleVersion.getBundle());
        }

        entityManager.persist(deployDef);

        return deployDef;
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
    public BundleVersion createBundleVersion(Subject subject, int bundleId, String name, String version, String recipe)
        throws Exception {
        if (null == name || "".equals(name.trim())) {
            throw new IllegalArgumentException("Invalid bundleVersionName: " + name);
        }

        Bundle bundle = entityManager.find(Bundle.class, bundleId);
        if (null == bundle) {
            throw new IllegalArgumentException("Invalid bundleId: " + bundleId);
        }

        // parse the recipe (validation occurs here) and get the config def and list of files
        BundleType bundleType = bundle.getBundleType();
        BundleServerPluginFacet bp = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager()
            .getBundleServerPluginFacet(bundleType.getName());
        RecipeParseResults results = bp.parseRecipe(recipe);

        // ensure we have a version
        version = getVersion(version, bundle);

        BundleVersion bundleVersion = new BundleVersion(name, version, bundle, recipe);
        bundleVersion.setConfigurationDefinition(results.getConfigDef());

        entityManager.persist(bundleVersion);
        return bundleVersion;
    }

    private String getVersion(String version, Bundle bundle) {
        if (!(null == version || "".equals(version.trim()))) {
            return version;
        }

        BundleVersion currentBundleVersion = null;
        for (BundleVersion bundleVersion : bundle.getBundleVersions()) {
            if ((null == currentBundleVersion) || (bundleVersion.getId() > currentBundleVersion.getId())) {
                currentBundleVersion = bundleVersion;
            }
        }

        // note - this is the same algo used by ResourceClientProxy in updatebackingContent (for a resource)
        String oldVersion = (null == currentBundleVersion) ? null : currentBundleVersion.getVersion();
        String newVersion = "1.0";
        if (oldVersion != null && oldVersion.length() != 0) {
            String[] parts = oldVersion.split("[^a-zA-Z0-9]");
            String lastPart = parts[parts.length - 1];
            try {
                int lastNumber = Integer.parseInt(lastPart);
                newVersion = oldVersion.substring(0, oldVersion.length() - lastPart.length()) + (lastNumber + 1);
            } catch (NumberFormatException nfe) {
                newVersion = oldVersion + ".1";
            }
        }

        return newVersion;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleFile addBundleFile(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, InputStream fileStream, boolean pinToPackage) throws Exception {

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
        PackageType packageType = getBundleTypePackageType(bundle.getBundleType());
        architecture = (null == architecture) ? contentManager.getNoArchitecture() : architecture;
        PackageVersion packageVersion = contentManager.createPackageVersion(name, packageType.getId(), version,
            architecture.getId(), fileStream);

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
        if (pinToPackage) {
            bundleFile.setPackage(generalPackage);
        }

        entityManager.persist(bundleFile);

        return bundleFile;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleFile addBundleFileViaByteArray(Subject subject, int bundleVersionId, String name, String version,
        Architecture architecture, byte[] fileBytes, boolean pinToPackage) throws Exception {

        return addBundleFile(subject, bundleVersionId, name, version, architecture,
            new ByteArrayInputStream(fileBytes), pinToPackage);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public BundleFile addBundleFileViaPackageVersion(Subject subject, int bundleVersionId, String name,
        int packageVersionId, boolean pinToPackage) throws Exception {

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
        if (pinToPackage) {
            bundleFile.setPackage(packageVersion.getGeneralPackage());
        }

        entityManager.persist(bundleFile);

        return bundleFile;
    }

    private PackageType getBundleTypePackageType(BundleType bundleType) {

        Query packageTypeQuery = entityManager.createNamedQuery(PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME);
        packageTypeQuery.setParameter("typeId", bundleType.getResourceType().getId());
        packageTypeQuery.setParameter("name", bundleType.getName());

        return (PackageType) packageTypeQuery.getSingleResult();
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public List<BundleDeployment> deployBundle(Subject subject, int bundleDeployDefinitionId, int[] resourceIds)
        throws Exception {

        if (null == resourceIds || 0 == resourceIds.length) {
            throw new IllegalArgumentException("Invalid resourceIds: " + resourceIds);
        }

        BundleDeployDefinition deployDef = entityManager.find(BundleDeployDefinition.class, bundleDeployDefinitionId);
        if (null == deployDef) {
            throw new IllegalArgumentException("Invalid bundleDeployDefinitionId: " + bundleDeployDefinitionId);
        }
        Resource[] resources = new Resource[resourceIds.length];
        for (int i = 0; (i < resourceIds.length); ++i) {
            resources[i] = (Resource) entityManager.find(Resource.class, resourceIds[i]);
            if (null == resources[i]) {
                throw new IllegalArgumentException("Invalid resourceId (Resource does not exist): " + resources[i]);
            }
        }

        List<BundleDeployment> result = new ArrayList<BundleDeployment>(resourceIds.length);
        // create a BundleDeploy record for each deployment
        for (Resource resource : resources) {
            BundleDeployment deployment = new BundleDeployment(deployDef, resource);
            deployDef.addDeployment(deployment);
            result.add(deployment);
            entityManager.persist(deployment);
        }

        return result;
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
        BundleServerPluginFacet bp = BundleManagerHelper.getPluginContainer().getBundleServerPluginManager()
            .getBundleServerPluginFacet(bundleType.getName());
        RecipeParseResults parseResults = bp.parseRecipe(bundleVersion.getRecipe());

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

    @SuppressWarnings("unchecked")
    public List<BundleType> getAllBundleTypes(Subject subject) {
        // the list of types will be small, no need to support paging
        Query q = entityManager.createNamedQuery(BundleType.QUERY_FIND_ALL);
        List<BundleType> types = q.getResultList();
        return types;
    }

    public PageList<BundleDeployDefinition> findBundleDeployDefinitionsByCriteria(Subject subject,
        BundleDeployDefinitionCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<BundleDeployDefinition> queryRunner = new CriteriaQueryRunner<BundleDeployDefinition>(
            criteria, generator, entityManager);
        return queryRunner.execute();
    }

    public PageList<BundleDeployment> findBundleDeploymentsByCriteria(Subject subject, BundleDeploymentCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);
        if (!authorizationManager.isInventoryManager(subject)) {
            if (criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            }

            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, null,
                subject.getId());
        }

        CriteriaQueryRunner<BundleDeployment> queryRunner = new CriteriaQueryRunner<BundleDeployment>(criteria,
            generator, entityManager);

        return queryRunner.execute();
    }

    public PageList<BundleVersion> findBundleVersionsByCriteria(Subject subject, BundleVersionCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<BundleVersion> queryRunner = new CriteriaQueryRunner<BundleVersion>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    public PageList<Bundle> findBundlesByCriteria(Subject subject, BundleCriteria criteria) {
        Query totalCountQuery = PersistenceUtility.createCountQuery(entityManager, Bundle.QUERY_FIND_ALL);
        long totalCount = (Long) totalCountQuery.getSingleResult();
        if (totalCount == 0) {
            List<BundleType> bundleTypes = getAllBundleTypes(subject);
            for (int i = 0; i < 50; i++) {
                createMockBundle(subject, bundleTypes);
            }
        }

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<Bundle> queryRunner = new CriteriaQueryRunner<Bundle>(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    public PageList<BundleDeployment> findBundleDeploymentsByCriteria(BundleDeploymentCriteria criteria) {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteBundles(Subject subject, int[] bundleIds) {
        for (int bundleId : bundleIds) {
            Bundle bundle = this.entityManager.find(Bundle.class, bundleId);
            this.entityManager.remove(bundle);
        }
    }

    public void deleteBundleVersions(Subject subject, int[] bundleVersionIds) {
        for (int bundleVersionId : bundleVersionIds) {
            BundleVersion bundleVersion = this.entityManager.find(BundleVersion.class, bundleVersionId);
            this.entityManager.remove(bundleVersion);
        }
    }

    public BundleType createMockBundleType(Subject subject) {

        try {
            ResourceType linuxPlatformResourceType = this.resourceTypeManager.getResourceTypeByNameAndPlugin("Linux",
                "Platforms");
            return createBundleType(subject, UUID.randomUUID().toString(), linuxPlatformResourceType.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bundle createMockBundle(Subject subject, List<BundleType> bundleTypes) {
        Random random = new Random();
        BundleType bundleType;
        if (bundleTypes.isEmpty()) {
            bundleType = createMockBundleType(subject);
        } else {
            int randomIndex = random.nextInt(bundleTypes.size());
            bundleType = bundleTypes.get(randomIndex);
        }

        Bundle bundle = createBundle(subject, UUID.randomUUID().toString(), bundleType.getId());

        // Add 1 to 5 bundle versions.
        int bundleVersionCount = random.nextInt(5) + 1;
        for (int i = 0; i < bundleVersionCount; i++) {
            String bundleVersionName = UUID.randomUUID().toString();
            final String RECIPE = "repo rhel-x86_64-5\n" //
                + "package foo-1.25.rpm\n" //
                + "package bar-1.25.rpm\n" //
                + "script foo.bash -c some parameter\n" //
                + "deploy jboss.tar %{jboss.home.directory}\n" //                
                + "realize %{jboss.home.directory}/server/default/setting.xml\n" //                
                + "file example.setting /etc/some/setting.ini\n" + "service example restart\n";
            BundleVersion bundleVersion = new BundleVersion(bundleVersionName, String.valueOf(i + 1), null, RECIPE);
            bundle.addBundleVersion(bundleVersion);
        }

        bundle = entityManager.merge(bundle);
        return bundle;
    }
}
