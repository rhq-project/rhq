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

package org.rhq.enterprise.server.bundle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJBException;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.hibernate.LazyInitializationException;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleGroupCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.updater.DeploymentProperties;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestAgentClient;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;

/**
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@SuppressWarnings({ "unused" })
@Test
public class BundleManagerBeanTest extends AbstractEJB3Test {

    private static final boolean TESTS_ENABLED = true;

    private static final String TEST_PREFIX = BundleManagerBeanTest.class.getSimpleName();
    private static final String TEST_BUNDLE_DESTBASEDIR_PROP = TEST_PREFIX + ".destBaseDirProp";
    private static final String TEST_BUNDLE_DESTBASEDIR_PROP_VALUE = TEST_PREFIX + "/destBaseDir";
    private static final String TEST_BUNDLE_GROUP_NAME = TEST_PREFIX + ".bundleGroup";
    private static final String TEST_DESTBASEDIR_NAME = TEST_PREFIX + ".destBaseDirName";
    private static final String TEST_ROLE_NAME = TEST_PREFIX + ".role";
    private static final String TEST_USER_NAME = TEST_PREFIX + ".user";

    private BundleManagerLocal bundleManager;
    private ResourceManagerLocal resourceManager;
    private SubjectManagerLocal subjectManager;
    private static final boolean ENABLED = true;
    private static final boolean DISABLED = false;

    private TestBundleServerPluginService ps;
    private TestBundlePluginComponent bpc;
    private MasterServerPluginContainer pc;
    private Subject overlord;
    TestServerCommunicationsService agentServiceContainer;

    @Override
    protected void beforeMethod() throws Exception {
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.bundleService = new TestAgentClient(null, agentServiceContainer);
        prepareScheduler();
        this.bpc = new TestBundlePluginComponent();
        this.ps = new TestBundleServerPluginService(getTempDir(), bpc);
        prepareCustomServerPluginService(this.ps);
        bundleManager = LookupUtil.getBundleManager();
        resourceManager = LookupUtil.getResourceManager();
        subjectManager = LookupUtil.getSubjectManager();
        overlord = subjectManager.getOverlord();
        this.ps.startMasterPluginContainer();

        // try and clean up any junk that may be lying around from a failed run
        cleanupDatabase();
    }

    @Override
    protected void afterMethod() throws Exception {
        unprepareForTestAgents();
        try {
            this.ps = null;
            cleanupDatabase();
        } finally {
            unprepareServerPluginService();
            unprepareScheduler();
        }
    }

    private void cleanupDatabase() {
        try {
            RoleCriteria roleCriteria = new RoleCriteria();
            roleCriteria.addFilterName(TEST_ROLE_NAME);
            List<Role> testRoles = LookupUtil.getRoleManager().findRolesByCriteria(overlord, roleCriteria);
            for (Role testRole : testRoles) {
                LookupUtil.getRoleManager().deleteRoles(overlord, new int[] { testRole.getId() });
            }

            SubjectCriteria subjectCriteria = new SubjectCriteria();
            subjectCriteria.addFilterName(TEST_USER_NAME);
            List<Subject> testSubjects = LookupUtil.getSubjectManager().findSubjectsByCriteria(overlord,
                subjectCriteria);
            for (Subject testSubject : testSubjects) {
                LookupUtil.getSubjectManager().deleteSubjects(overlord, new int[] { testSubject.getId() });
            }

            getTransactionManager().begin();

            Query q;
            List<?> doomed;

            // clean up any tests that don't already clean up after themselves

            // remove bundleversions which cascade remove bundlefiles and bundledeployments
            // bundlefiles cascaderemove packageversions
            // bundledeployments cascade remove bundleresourcedeployments
            // bundleresourcedeployments cascade remove bundleresourcedeploymenthistory
            q = em.createQuery("SELECT bv FROM BundleVersion bv WHERE bv.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleVersion.class, ((BundleVersion) removeMe).getId()));
            }
            em.flush();
            // remove any orphaned bfs
            q = em.createQuery("SELECT bf FROM BundleFile bf WHERE bf.packageVersion.generalPackage.name LIKE '"
                + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleFile.class, ((BundleFile) removeMe).getId()));
            }
            // remove any orphaned deployment history
            q = em
                .createQuery("SELECT brdh FROM BundleResourceDeploymentHistory brdh WHERE brdh.resourceDeployment.bundleDeployment.name LIKE '"
                    + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleResourceDeploymentHistory.class,
                    ((BundleResourceDeploymentHistory) removeMe).getId()));
            }
            // remove any orphaned brds
            q = em.createQuery("SELECT brd FROM BundleResourceDeployment brd WHERE brd.bundleDeployment.name LIKE '"
                + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleResourceDeployment.class, ((BundleResourceDeployment) removeMe).getId()));
            }
            // remove any orphaned bds
            q = em.createQuery("SELECT bd FROM BundleDeployment bd WHERE bd.description LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDeployment.class, ((BundleDeployment) removeMe).getId()));
            }

            // remove bundle groups to free up bundles
            q = em.createQuery("SELECT bg FROM BundleGroup bg WHERE bg.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                BundleGroup doomedBundleGroup = em.find(BundleGroup.class, ((BundleGroup) removeMe).getId());
                doomedBundleGroup.setBundles(new HashSet<Bundle>());
                em.remove(doomedBundleGroup);
            }

            // remove bundles which cascade remove packageTypes and destinations
            // packagetypes cascade remove packages
            // package cascade remove packageversions
            q = em.createQuery("SELECT b FROM Bundle b WHERE b.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Bundle.class, ((Bundle) removeMe).getId()));
            }
            em.flush();

            // remove any orphaned pvs
            q = em.createQuery("SELECT pv FROM PackageVersion pv WHERE pv.generalPackage.name LIKE '" + TEST_PREFIX
                + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(PackageVersion.class, ((PackageVersion) removeMe).getId()));
            }
            // remove any oprphaned packages
            q = em.createQuery("SELECT p FROM Package p WHERE p.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Package.class, ((Package) removeMe).getId()));
            }
            // remove any orphaned packagetypes
            q = em.createQuery("SELECT pt FROM PackageType pt WHERE pt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(PackageType.class, ((PackageType) removeMe).getId()));
            }
            // remove any orphaned destinations
            q = em.createQuery("SELECT bd FROM BundleDestination bd WHERE bd.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDestination.class, ((BundleDestination) removeMe).getId()));
            }

            // remove repos no longer referenced by bundles
            q = em.createQuery("SELECT r FROM Repo r WHERE r.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Repo.class, ((Repo) removeMe).getId()));
            }

            // remove Resource Groups left over from test deployments freeing up test resources
            q = em.createQuery("SELECT rg FROM ResourceGroup rg WHERE rg.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(ResourceGroup.class, ((ResourceGroup) removeMe).getId()));
            }

            // remove Resources in test groups
            q = em.createQuery("SELECT r FROM Resource r WHERE r.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                ResourceTreeHelper.deleteResource(em, em.getReference(Resource.class, ((Resource) removeMe).getId()));
            }

            // remove ResourceTypes which cascade remove BundleTypes
            q = em.createQuery("SELECT rt FROM ResourceType rt WHERE rt.deleted = false and rt.name LIKE '"
                + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(ResourceType.class, ((ResourceType) removeMe).getId()));
            }
            em.flush();
            //  remove any orphaned BundleTypes
            q = em.createQuery("SELECT bt FROM BundleType bt WHERE bt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleType.class, ((BundleType) removeMe).getId()));
            }

            // remove Agents left over from test resources
            q = em.createQuery("SELECT a FROM Agent a WHERE a.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Agent.class, ((Agent) removeMe).getId()));
            }

            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleVersionFromDistributionFile() throws Exception {

        File tmpDir = FileUtil.createTempDirectory("createBundleFromDistro", ".dir", null);
        try {
            String bundleFile1 = TEST_PREFIX + "_subdir1/bundle-file-1.txt";
            String bundleFile2 = TEST_PREFIX + "_subdir2/bundle-file-2.txt";
            String bundleFile3 = TEST_PREFIX + "_bundle-file-3.txt";
            writeFile(new File(tmpDir, bundleFile1), "first bundle file found inside bundle distro");
            writeFile(new File(tmpDir, bundleFile2), "second bundle file found inside bundle distro");
            writeFile(new File(tmpDir, bundleFile3), "third bundle file found inside bundle distro");

            String bundleName = TEST_PREFIX + "-create-from-distro";
            String bundleVersion = "1.2.3";
            String bundleDescription = "test bundle desc";
            DeploymentProperties bundleMetadata = new DeploymentProperties(0, bundleName, bundleVersion,
                bundleDescription);

            ConfigurationDefinition configDef = new ConfigurationDefinition("foo", null);
            int propDefaultValue1 = 998877;
            String propDefaultValue4 = "this.is.the.default";
            String propName1 = "prop1requiredWithDefault";
            String propName2 = "prop2";
            String propName3 = "prop3requiredWithNoDefault";
            String propName4 = "prop4notRequiredWithDefault";
            String propDesc1 = "prop1desc";
            String propDesc2 = "prop2desc";
            String propDesc3 = "prop3desc";
            String propDesc4 = "prop4desc";
            PropertyDefinitionSimple propdef1requiredWithDefault = new PropertyDefinitionSimple(propName1, propDesc1,
                true, PropertySimpleType.INTEGER);
            PropertyDefinitionSimple propdef2 = new PropertyDefinitionSimple(propName2, propDesc2, false,
                PropertySimpleType.STRING);
            PropertyDefinitionSimple propdef3requiredWithNoDefault = new PropertyDefinitionSimple(propName3, propDesc3,
                true, PropertySimpleType.STRING);
            PropertyDefinitionSimple propdef4notRequiredWithDefault = new PropertyDefinitionSimple(propName4,
                propDesc4, false, PropertySimpleType.STRING);
            propdef1requiredWithDefault.setDefaultValue(String.valueOf(propDefaultValue1));
            propdef4notRequiredWithDefault.setDefaultValue(propDefaultValue4);
            configDef.put(propdef1requiredWithDefault);
            configDef.put(propdef2);
            configDef.put(propdef3requiredWithNoDefault);
            configDef.put(propdef4notRequiredWithDefault);

            Map<String, File> bundleFiles = new HashMap<String, File>(3);
            bundleFiles.put(bundleFile1, new File(tmpDir, bundleFile1));
            bundleFiles.put(bundleFile2, new File(tmpDir, bundleFile2));
            bundleFiles.put(bundleFile3, new File(tmpDir, bundleFile3));

            File bundleDistroFile = tmpDir; // not a real distro zip, but its just a simulation anyway - SLSB will pass this to our mock PC
            String recipe = "mock recipe";
            BundleType bt1 = createBundleType("one");

            // prepare our mock bundle PC
            bpc.parseRecipe_returnValue = new RecipeParseResults(bundleMetadata, configDef, new HashSet<String>(
                bundleFiles.keySet()));
            bpc.processBundleDistributionFile_returnValue = new BundleDistributionInfo(recipe,
                bpc.parseRecipe_returnValue, bundleFiles);
            bpc.processBundleDistributionFile_returnValue.setBundleTypeName(bt1.getName());

            // now ask the SLSB to persist our bundle data given our mock distribution
            BundleVersion bv1 = bundleManager.createBundleVersionViaURL(overlord, bundleDistroFile.toURI().toURL()
                .toString());

            // to a db lookup to make sure our bundle version is queryable
            BundleVersionCriteria criteria = new BundleVersionCriteria();
            criteria.addFilterId(bv1.getId());
            criteria.fetchBundle(true);
            criteria.fetchConfigurationDefinition(true);
            criteria.fetchBundleFiles(true);
            criteria.fetchTags(true);
            BundleVersion bv2 = bundleManager.findBundleVersionsByCriteria(overlord, criteria).get(0);
            List<BundleFile> bv2BundleFiles = bv2.getBundleFiles();
            BundleFileCriteria bfCriteria = new BundleFileCriteria();
            bfCriteria.addFilterBundleVersionId(bv2.getId());
            bfCriteria.fetchPackageVersion(true);
            PageList<BundleFile> bfs = bundleManager.findBundleFilesByCriteria(overlord, bfCriteria);
            bv2BundleFiles.clear();
            bv2BundleFiles.addAll(bfs);
            bv2.setBundleDeployments(new ArrayList<BundleDeployment>());

            // test that the PC's return value and our own DB lookup match the bundle version we expect to be in the DB
            BundleVersion[] bvs = new BundleVersion[] { bv1, bv2 };
            for (BundleVersion bv : bvs) {
                assert bv.getId() > 0 : bv;
                assert bv.getBundle().getName().equals(bundleName) : bv;
                assert bv.getBundle().getDescription().equals(bundleDescription) : bv;
                assert bv.getDescription().equals(bundleDescription) : "the bundle version desc should be the same as the bundle desc";
                assert bv.getVersion().equals(bundleVersion) : bv;
                assert bv.getBundleFiles().size() == 3 : bv;
                ArrayList<String> bundleFileNames = new ArrayList<String>(3);
                for (BundleFile bf : bv.getBundleFiles()) {
                    bundleFileNames.add(bf.getPackageVersion().getFileName());
                }
                assert bundleFileNames.contains(bundleFile1) : bv;
                assert bundleFileNames.contains(bundleFile2) : bv;
                assert bundleFileNames.contains(bundleFile3) : bv;
                assert bv.getBundleDeployments().isEmpty() : bv;
                assert bv.getConfigurationDefinition().getPropertyDefinitions().size() == 4;
                assert bv.getConfigurationDefinition().get(propName1) != null;
                assert bv.getConfigurationDefinition().get(propName1).getDescription().equals(propDesc1);
                assert bv.getConfigurationDefinition().get(propName1).isRequired() == true;
                assert bv.getConfigurationDefinition().getPropertyDefinitionSimple(propName1).getType() == PropertySimpleType.INTEGER;
                assert bv.getConfigurationDefinition().get(propName2) != null;
                assert bv.getConfigurationDefinition().get(propName2).getDescription().equals(propDesc2);
                assert bv.getConfigurationDefinition().get(propName2).isRequired() == false;
                assert bv.getConfigurationDefinition().getPropertyDefinitionSimple(propName2).getType() == PropertySimpleType.STRING;
                assert bv.getConfigurationDefinition().get(propName3) != null;
                assert bv.getConfigurationDefinition().get(propName3).getDescription().equals(propDesc3);
                assert bv.getConfigurationDefinition().get(propName3).isRequired() == true;
                assert bv.getConfigurationDefinition().getPropertyDefinitionSimple(propName3).getType() == PropertySimpleType.STRING;
                assert bv.getConfigurationDefinition().get(propName4) != null;
                assert bv.getConfigurationDefinition().get(propName4).getDescription().equals(propDesc4);
                assert bv.getConfigurationDefinition().get(propName4).isRequired() == false;
                assert bv.getConfigurationDefinition().getPropertyDefinitionSimple(propName4).getType() == PropertySimpleType.STRING;
                assert bv.getRecipe().equals(recipe);

                // make sure the default template is correct
                ConfigurationTemplate defaultTemplate = bv.getConfigurationDefinition().getDefaultTemplate();
                Configuration defaultConfig = defaultTemplate.getConfiguration();
                assert defaultConfig.getProperties().size() == 3; // prop2 is not required and has no default, thus is missing
                PropertySimple prop1 = defaultConfig.getSimple(propName1);
                PropertySimple prop2 = defaultConfig.getSimple(propName2);
                PropertySimple prop3 = defaultConfig.getSimple(propName3);
                PropertySimple prop4 = defaultConfig.getSimple(propName4);
                assert prop1 != null;
                assert prop2 == null : "prop2 was not required and has no default, it should not be in the default template config";
                assert prop3 != null;
                assert prop4 != null;
                assert prop1.getIntegerValue() != null;
                assert prop1.getIntegerValue().intValue() == propDefaultValue1;
                assert prop3.getStringValue() == null : "prop3 was required but had no default, its template value should have been null";
                assert prop4.getStringValue().equals(propDefaultValue4);
            }
        } finally {
            FileUtil.purge(tmpDir, true);
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testGetBundleTypes() throws Exception {
        BundleType bt1 = createBundleType("one");
        BundleType bt2 = createBundleType("two");
        List<BundleType> bts = bundleManager.getAllBundleTypes(overlord);
        assert bts.size() >= 2 : "should have at least 2 bundle types";

        List<String> btNames = new ArrayList<String>();
        for (BundleType bundleType : bts) {
            btNames.add(bundleType.getName());
        }

        assert btNames.contains(bt1.getName());
        assert btNames.contains(bt2.getName());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundle() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleVersion() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        assert 0 == bv1.getVersionOrder();
        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());
        assert 1 == bv2.getVersionOrder();
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeleteBundle() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());

        // let's add a bundle file so we can ensure our deletion will also delete the file too
        bundleManager.addBundleFileViaByteArray(overlord, bv2.getId(), "testDeleteBundle", "1.0", new Architecture(
            "noarch"), "content".getBytes());
        BundleFileCriteria bfCriteria = new BundleFileCriteria();
        bfCriteria.addFilterBundleVersionId(bv2.getId());
        bfCriteria.fetchPackageVersion(true);
        PageList<BundleFile> files = bundleManager.findBundleFilesByCriteria(overlord, bfCriteria);
        assert files.size() == 1 : files;
        assert files.get(0).getPackageVersion().getGeneralPackage().getName().equals("testDeleteBundle") : files;

        bundleManager.deleteBundle(overlord, b1.getId());

        BundleCriteria bCriteria = new BundleCriteria();
        bCriteria.addFilterId(b1.getId());
        PageList<Bundle> bResults = bundleManager.findBundlesByCriteria(overlord, bCriteria);
        assert bResults.size() == 0;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeleteBundleDeployment() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull("Instance of newly created bundle should not be null", b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull("Instance of newly created bundle version should not be null", bv1);

        ResourceGroup platformResourceGroup = createTestResourceGroup(false);
        assertNotNull("Instance of newly created resource group should not be null", platformResourceGroup);
        BundleDestination dest1 = createDestination(b1, "one", "/test", platformResourceGroup);
        assertNotNull("Instance of newly created bundle destination should not be null", dest1);

        BundleDeployment deployment1 = createDeployment("one", bv1, dest1,
            Configuration.builder().addSimple("bundletest.property", "bundletest.property value").build());
        assertNotNull("Instance of newly created bundle deployment should not be null", deployment1);
        getTransactionManager().begin();
        deployment1.setStatus(BundleDeploymentStatus.SUCCESS);
        em.merge(deployment1);
        getTransactionManager().commit();

        BundleDeployment deployment2 = createDeployment("two", bv1, dest1,
            Configuration.builder().addSimple("bundletest.property", "bundletest.property value").build());
        assertNotNull("Instance of newly created bundle deployment should not be null", deployment2);
        getTransactionManager().begin();
        deployment2.setStatus(BundleDeploymentStatus.SUCCESS);
        deployment2.setReplacedBundleDeploymentId(deployment1.getId());
        em.merge(deployment2);
        getTransactionManager().commit();

        BundleDeployment deployment3 = createDeployment("three", bv1, dest1,
            Configuration.builder().addSimple("bundletest.property", "bundletest.property value").build());
        assertNotNull("Instance of newly created bundle deployment should not be null", deployment3);
        getTransactionManager().begin();
        deployment3.setStatus(BundleDeploymentStatus.SUCCESS);
        deployment3.setReplacedBundleDeploymentId(deployment2.getId());
        em.merge(deployment3);
        getTransactionManager().commit();

        BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
        criteria.addFilterBundleId(b1.getId());
        List<BundleDeployment> deployments = bundleManager.findBundleDeploymentsByCriteria(
            subjectManager.getOverlord(), criteria);
        assertNotNull("List of bundle deployments should not be null", deployments);
        assertEquals(3, deployments.size());

        // delete the middle bundle deployment
        bundleManager.deleteBundleDeployment(subjectManager.getOverlord(), deployment2.getId());
        deployments = bundleManager.findBundleDeploymentsByCriteria(subjectManager.getOverlord(), criteria);
        assertNotNull("List of bundle deployments should not be null", deployments);
        assertEquals(2, deployments.size());
        assertTrue("When the middle chain was removed the links should be modified correctly. "
            + "Assume A -> B -> C deployment structure, remove B, this should result in A -> C",
            deployments.get(0).getReplacedBundleDeploymentId() == null ? deployments.get(1)
                .getReplacedBundleDeploymentId() == deployments.get(0).getId() : deployments.get(0)
                .getReplacedBundleDeploymentId() == deployments.get(1).getId());

        bundleManager.deleteBundleDeployment(subjectManager.getOverlord(), deployment1.getId());
        deployments = bundleManager.findBundleDeploymentsByCriteria(subjectManager.getOverlord(), criteria);
        assertNotNull("List of bundle deployments should not be null", deployments);
        assertEquals("1 bundle deployment should be found, 2 were deleted.", 1, deployments.size());
        assertEquals(
            "replacedBundleDeploymentId should be set to null, because all the previous deployments were deleted.",
            deployments.get(0).getReplacedBundleDeploymentId(), null);

        bundleManager.deleteBundleDeployment(subjectManager.getOverlord(), deployment3.getId());
        deployments = bundleManager.findBundleDeploymentsByCriteria(subjectManager.getOverlord(), criteria);
        assertNotNull("List of bundle deployments should not be null", deployments);
        assertEquals("No bundle deployments should be found, all were deleted.", 0, deployments.size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeleteBundleVersion() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());

        // let's add a bundle file so we can ensure our deletion will also delete the file too
        bundleManager.addBundleFileViaByteArray(overlord, bv2.getId(), "testDeleteBundleVersion", "1.0",
            new Architecture("noarch"), "content".getBytes());
        BundleFileCriteria bfCriteria = new BundleFileCriteria();
        bfCriteria.addFilterBundleVersionId(bv2.getId());
        bfCriteria.fetchPackageVersion(true);
        PageList<BundleFile> files = bundleManager.findBundleFilesByCriteria(overlord, bfCriteria);
        assert files.size() == 1 : files;
        assert files.get(0).getPackageVersion().getGeneralPackage().getName().equals("testDeleteBundleVersion") : files;

        BundleVersionCriteria bvCriteria = new BundleVersionCriteria();
        BundleCriteria bCriteria = new BundleCriteria();

        // delete the first one - this deletes the BV but the bundle should remain intact
        bundleManager.deleteBundleVersion(overlord, bv2.getId(), true);
        bvCriteria.addFilterId(bv2.getId());
        PageList<BundleVersion> bvResults = bundleManager.findBundleVersionsByCriteria(overlord, bvCriteria);
        assert bvResults.size() == 0;
        bCriteria.addFilterId(b1.getId());
        PageList<Bundle> bResults = bundleManager.findBundlesByCriteria(overlord, bCriteria);
        assert bResults.size() == 1 : "Should not have deleted bundle yet, 1 version still exists";

        // delete the second one - this deletes last BV thus the bundle should also get deleted
        bundleManager.deleteBundleVersion(overlord, bv1.getId(), true);
        bvCriteria.addFilterId(bv1.getId());
        bvResults = bundleManager.findBundleVersionsByCriteria(overlord, bvCriteria);
        assert bvResults.size() == 0;
        bCriteria.addFilterId(b1.getId());
        bResults = bundleManager.findBundlesByCriteria(overlord, bCriteria);
        assert bResults.size() == 0 : "Should have deleted bundle since no versions exists anymore";

        // make sure our composite query is OK and can show us 0 bundles, too
        PageList<BundleWithLatestVersionComposite> composites;
        bCriteria = new BundleCriteria();
        composites = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, bCriteria);
        assert composites.size() == 0;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleVersionOrdering() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);

        BundleCriteria criteria = new BundleCriteria();
        criteria.addSortName(PageOrdering.ASC);
        PageList<BundleWithLatestVersionComposite> results;

        // verify there are no bundle versions yet
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion() == null;
        assert results.get(0).getVersionsCount().longValue() == 0L;

        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", "1.0", b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        assert 0 == bv1.getVersionOrder();
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("1.0");
        assert results.get(0).getVersionsCount().longValue() == 1L;

        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", "2.0", b1);
        assertNotNull(bv2);
        assertEquals("2.0", bv2.getVersion());
        assert 1 == bv2.getVersionOrder();
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 2L;

        BundleVersion bv3 = createBundleVersion(b1.getName() + "-3", "1.5", b1);
        assertNotNull(bv3);
        assertEquals("1.5", bv3.getVersion());
        assert 1 == bv3.getVersionOrder();
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 3L;

        BundleVersionCriteria c = new BundleVersionCriteria();
        PageList<BundleVersion> bvs;

        c.addFilterId(bv1.getId()); // 1.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 0; // 1st is the 1.0 version

        c.addFilterId(bv2.getId()); // 2.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 2; // 3rd is the 2.0 version

        c.addFilterId(bv3.getId()); // 1.5
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 1; // 2nd is the 1.5 version

        // see that we can create a really old bundle and versionOrder gets updated properly
        BundleVersion bv4 = createBundleVersion(b1.getName() + "-4", "0.5", b1);
        assertNotNull(bv4);
        assertEquals("0.5", bv4.getVersion());

        c.addFilterId(bv4.getId()); //0.5
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 0; // 1st is the 0.5 version

        c.addFilterId(bv1.getId()); // 1.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 1; // 2nd is the 1.0 version

        c.addFilterId(bv3.getId()); // 1.5
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 2; // 3nd is the 1.5 version

        c.addFilterId(bv2.getId()); // 2.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 3; // 4th is the 2.0 version

        // verify our composite criteria query can return more than one item
        Bundle b2 = createBundle("two");
        assertNotNull(b2);
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 4L;
        assert results.get(1).getBundleId().equals(b2.getId());
        assert results.get(1).getBundleName().equals(b2.getName());
        assert results.get(1).getBundleDescription().equals(b2.getDescription());
        assert results.get(1).getLatestVersion() == null;
        assert results.get(1).getVersionsCount().longValue() == 0L;

        BundleVersion b2_bv1 = createBundleVersion(b2.getName() + "-5", "9.1", b2);
        assertNotNull(b2_bv1);
        assertEquals("9.1", b2_bv1.getVersion());

        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 4L;
        assert results.get(1).getBundleId().equals(b2.getId());
        assert results.get(1).getBundleName().equals(b2.getName());
        assert results.get(1).getBundleDescription().equals(b2.getDescription());
        assert results.get(1).getLatestVersion().equals("9.1");
        assert results.get(1).getVersionsCount().longValue() == 1L;

        // test sorting of the BundleWithLastestVersionComposite
        criteria.addSortName(PageOrdering.DESC);
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(1).getBundleId().equals(b1.getId());
        assert results.get(1).getBundleName().equals(b1.getName());
        assert results.get(1).getBundleDescription().equals(b1.getDescription());
        assert results.get(1).getLatestVersion().equals("2.0");
        assert results.get(1).getVersionsCount().longValue() == 4L;
        assert results.get(0).getBundleId().equals(b2.getId());
        assert results.get(0).getBundleName().equals(b2.getName());
        assert results.get(0).getBundleDescription().equals(b2.getDescription());
        assert results.get(0).getLatestVersion().equals("9.1");
        assert results.get(0).getVersionsCount().longValue() == 1L;

        criteria.addSortName(PageOrdering.ASC);
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 4L;
        assert results.get(1).getBundleId().equals(b2.getId());
        assert results.get(1).getBundleName().equals(b2.getName());
        assert results.get(1).getBundleDescription().equals(b2.getDescription());
        assert results.get(1).getLatestVersion().equals("9.1");
        assert results.get(1).getVersionsCount().longValue() == 1L;

        // now delete one of the older bundle versions and make sure the ordering remains intact
        bundleManager.deleteBundleVersion(overlord, bv1.getId(), true);
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 3L;
        assert results.get(1).getBundleId().equals(b2.getId());
        assert results.get(1).getBundleName().equals(b2.getName());
        assert results.get(1).getBundleDescription().equals(b2.getDescription());
        assert results.get(1).getLatestVersion().equals("9.1");
        assert results.get(1).getVersionsCount().longValue() == 1L;

        // now add another bundle version and make sure the ordering is updated properly [BZ 828905]
        BundleVersion bv5 = createBundleVersion(b1.getName() + "-5", "5.0", b1);
        assertNotNull(bv5);
        assertEquals("5.0", bv5.getVersion());
        assert 3 == bv5.getVersionOrder();
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("5.0");
        assert results.get(0).getVersionsCount().longValue() == 4L;

        // delete the latest bundle version and make sure we didn't screw up the order
        bundleManager.deleteBundleVersion(overlord, bv5.getId(), true);
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 3L;

        // delete the oldest bundle version and make sure we didn't screw up the order
        bundleManager.deleteBundleVersion(overlord, bv4.getId(), true); // deleting version 0.5
        results = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 2L;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAddBundleFiles() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        assertNotNull(bv1);
        BundleFile bf1 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), "bundletest-bundlefile-1",
            "1.0", null, "Test Bundle File # 1".getBytes());
        BundleFile bf2 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), "bundletest-bundlefile-2",
            "1.0", null, "Test Bundle File # 2".getBytes());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAddBundleFilesToDifferentBundles() throws Exception {
        // create a bundle type to use for both bundles.
        BundleType bt = createBundleType("one");
        Bundle b1 = createBundle(overlord, "one", bt, null);
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        assertNotNull(bv1);
        BundleFile b1f1 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), TEST_PREFIX + "-file1", "1.0",
            null, "Bundle #1 File # 1".getBytes());

        // create a second bundle but create file of the same name as above
        Bundle b2 = createBundle(overlord, "two", bt, null);
        assertNotNull(b2);
        BundleVersion bv2 = createBundleVersion(b2.getName(), "1.0", b2);
        assertNotNull(bv2);
        BundleFile b2f1 = bundleManager.addBundleFileViaByteArray(overlord, bv2.getId(), TEST_PREFIX + "-file1", "1.0",
            null, "Bundle #2 File # 1".getBytes());

        BundleFileCriteria bfc = new BundleFileCriteria();
        bfc.addFilterBundleVersionId(bv1.getId());
        PageList<BundleFile> bundleFiles = bundleManager.findBundleFilesByCriteria(overlord, bfc);
        assert bundleFiles.size() == 1 : bundleFiles;
        assert bundleFiles.get(0).getId() == b1f1.getId() : bundleFiles;

        bfc = new BundleFileCriteria();
        bfc.addFilterBundleVersionId(bv2.getId());
        bundleFiles = bundleManager.findBundleFilesByCriteria(overlord, bfc);
        assert bundleFiles.size() == 1 : bundleFiles;
        assert bundleFiles.get(0).getId() == b2f1.getId() : bundleFiles;

        assert b1f1.getId() != b2f1.getId() : "should have been different bundle files";
        assert b1f1.getPackageVersion().getId() != b2f1.getPackageVersion().getId() : "should be different PV";
        assert b1f1.getPackageVersion().getGeneralPackage().getId() != b2f1.getPackageVersion().getGeneralPackage()
            .getId() : "package IDs should be different";
        assert !b1f1.getPackageVersion().getGeneralPackage().equals(b2f1.getPackageVersion().getGeneralPackage()) : "should be different packages";
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleDeployment() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        ResourceGroup platformResourceGroup = createTestResourceGroup(false);
        assertNotNull(platformResourceGroup);
        BundleDestination dest1 = createDestination(b1, "one", "/test", platformResourceGroup);
        assertNotNull(dest1);
        Configuration config = new Configuration();
        try {
            createDeployment("one", bv1, dest1, config);
            fail("Bad config was accepted");
        } catch (Exception e) {
            // expected due to bad config
        }
        config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        assertNotNull(createDeployment("one", bv1, dest1, config));
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeployBundle() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        Configuration config = new Configuration();
        config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        ResourceGroup platformResourceGroup = createTestResourceGroup(false);
        assertNotNull(platformResourceGroup);
        BundleDestination dest1 = createDestination(b1, "one", "/test", platformResourceGroup);
        assertNotNull(dest1);
        BundleDeployment bd1 = createDeployment("one", bv1, dest1, config);
        assertNotNull(bd1);
        assertEquals(BundleDeploymentStatus.PENDING, bd1.getStatus());

        BundleDeployment bd1d = bundleManager.scheduleBundleDeployment(overlord, bd1.getId(), false);
        assertNotNull(bd1d);
        assertEquals(bd1.getId(), bd1d.getId());

        BundleDeploymentCriteria bdc = new BundleDeploymentCriteria();
        bdc.addFilterId(bd1d.getId());
        bdc.fetchBundleVersion(true);
        bdc.fetchDestination(true);
        bdc.fetchResourceDeployments(true);
        bdc.fetchTags(true);
        List<BundleDeployment> bds = bundleManager.findBundleDeploymentsByCriteria(overlord, bdc);
        assertEquals(1, bds.size());
        bd1d = bds.get(0);

        assertEquals(platformResourceGroup, bd1d.getDestination().getGroup());
        assertEquals(dest1.getId(), bd1d.getDestination().getId());

        BundleResourceDeploymentCriteria c = new BundleResourceDeploymentCriteria();
        c.addFilterBundleDeploymentId(bd1d.getId());
        c.fetchBundleDeployment(true);
        c.fetchHistories(true);
        c.fetchResource(true);
        List<BundleResourceDeployment> brds = bundleManager.findBundleResourceDeploymentsByCriteria(overlord, c);
        assertEquals(1, brds.size());
        assertEquals(1, bd1d.getResourceDeployments().size());
        assertEquals(bd1d.getResourceDeployments().get(0).getId(), brds.get(0).getId());
        BundleResourceDeployment brd = brds.get(0);

        assertNotNull(brd.getBundleResourceDeploymentHistories());
        int size = brd.getBundleResourceDeploymentHistories().size();
        assertTrue(size > 0);
        String auditMessage = "BundleTest-Message";
        bundleManager.addBundleResourceDeploymentHistoryInNewTrans(overlord, brd.getId(),
            new BundleResourceDeploymentHistory(overlord.getName(), auditMessage, auditMessage,
                BundleResourceDeploymentHistory.Category.DEPLOY_STEP, BundleResourceDeploymentHistory.Status.SUCCESS,
                auditMessage, auditMessage));

        brds = bundleManager.findBundleResourceDeploymentsByCriteria(overlord, c);
        assertEquals(1, brds.size());
        assertEquals(brd.getId(), brds.get(0).getId());
        brd = brds.get(0);
        assertNotNull(brd.getBundleResourceDeploymentHistories());
        assertTrue((size + 1) == brd.getBundleResourceDeploymentHistories().size());
        BundleResourceDeploymentHistory newHistory = null;
        for (BundleResourceDeploymentHistory h : brd.getBundleResourceDeploymentHistories()) {
            if (auditMessage.equals(h.getMessage())) {
                newHistory = h;
                break;
            }
        }
        assertNotNull(newHistory);
        assertEquals(auditMessage, newHistory.getAction());
        assertEquals(BundleResourceDeploymentHistory.Status.SUCCESS, newHistory.getStatus());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCannotDeployToSyntheticResources() throws Exception {
        Bundle b1 = createBundle("one-synthetic");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        Configuration config = new Configuration();
        config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        ResourceGroup platformResourceGroup = createTestResourceGroup(true);
        assertNotNull(platformResourceGroup);
        BundleDestination dest1 = createDestination(b1, "one-synthetic", "/test", platformResourceGroup);
        assertNotNull(dest1);
        BundleDeployment bd1 = createDeployment("one-synthetic", bv1, dest1, config);
        assertNotNull(bd1);
        assertEquals(BundleDeploymentStatus.PENDING, bd1.getStatus());

        BundleDeployment bd1d = bundleManager.scheduleBundleDeployment(overlord, bd1.getId(), false);
        assertNotNull(bd1d);
        assertEquals(bd1.getId(), bd1d.getId());

        BundleDeploymentCriteria bdc = new BundleDeploymentCriteria();
        bdc.addFilterId(bd1d.getId());
        bdc.fetchBundleVersion(true);
        bdc.fetchDestination(true);
        bdc.fetchResourceDeployments(true);
        bdc.fetchTags(true);
        List<BundleDeployment> bds = bundleManager.findBundleDeploymentsByCriteria(overlord, bdc);
        assertEquals(1, bds.size());
        bd1d = bds.get(0);

        assertEquals(platformResourceGroup, bd1d.getDestination().getGroup());
        assertEquals(dest1.getId(), bd1d.getDestination().getId());

        BundleResourceDeploymentCriteria c = new BundleResourceDeploymentCriteria();
        c.addFilterBundleDeploymentId(bd1d.getId());
        c.fetchBundleDeployment(true);
        c.fetchHistories(true);
        c.fetchResource(true);
        List<BundleResourceDeployment> brds = bundleManager.findBundleResourceDeploymentsByCriteria(overlord, c);
        assertEquals(1, brds.size());
        assertEquals(1, bd1d.getResourceDeployments().size());
        assertEquals(bd1d.getResourceDeployments().get(0).getId(), brds.get(0).getId());
        BundleResourceDeployment brd = brds.get(0);

        assertNotNull(brd.getBundleResourceDeploymentHistories());
        int size = brd.getBundleResourceDeploymentHistories().size();
        assertTrue(size == 1);

        BundleResourceDeploymentHistory hist = brd.getBundleResourceDeploymentHistories().get(0);
        assertEquals(hist.getStatus(), BundleResourceDeploymentHistory.Status.FAILURE);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testGetBundleFilenames() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        assertNotNull(bv1);
        Set<String> filenames = bundleManager.getBundleVersionFilenames(overlord, bv1.getId(), true);
        assertNotNull(filenames);
        assertEquals(DEFAULT_CRITERIA_PAGE_SIZE + 2, filenames.size());
        BundleFile bf1 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), "bundletest-bundlefile-1",
            "1.0", null, "Test Bundle File # 1".getBytes());
        filenames = bundleManager.getBundleVersionFilenames(overlord, bv1.getId(), true);
        assertNotNull(filenames);
        assertEquals(DEFAULT_CRITERIA_PAGE_SIZE + 1, filenames.size());
        BundleFile bf2 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), "bundletest-bundlefile-2",
            "1.0", null, "Test Bundle File # 2".getBytes());
        filenames = bundleManager.getBundleVersionFilenames(overlord, bv1.getId(), true);
        assertNotNull(filenames);
        assertEquals(DEFAULT_CRITERIA_PAGE_SIZE, filenames.size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindBundlesByCriteria() throws Exception {
        Bundle b1 = createBundle("one");
        Bundle b2 = createBundle("two");
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        BundleVersion bv2 = createBundleVersion(b2.getName(), "1.0", b2);
        BundleCriteria c = new BundleCriteria();
        PageList<Bundle> bundles;
        Bundle b;
        String name;

        // return all with no optional data
        c.addFilterName(TEST_PREFIX);
        bundles = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bundles);
        assertEquals(2, bundles.size());
        b = bundles.get(0);
        name = "one";
        assertNotNull(b);
        assertTrue(b.getBundleType().getName(), b.getName().contains(name));
        assertTrue(b.getBundleType().getName(), b.getBundleType().getName().contains(name));
        try {
            b.getBundleVersions().isEmpty();
            fail("Should have thrown LazyInitializationException");
        } catch (LazyInitializationException e) {
            // expected
        } catch (Exception e) {
            fail("Should have thrown LazyInitializationException");
        }

        b = bundles.get(1);
        name = "two";
        assertNotNull(b);
        assertTrue(b.getBundleType().getName(), b.getName().contains(name));
        assertTrue(b.getBundleType().getName(), b.getBundleType().getName().contains(name));

        // return bundle "two" using all criteria and with all optional data
        c.addFilterId(b.getId());
        c.addFilterName(b.getName());
        c.addFilterBundleTypeName(b.getBundleType().getName());
        c.fetchBundleVersions(true);
        c.fetchRepo(true);
        bundles = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bundles);
        assertEquals(1, bundles.size());
        b = bundles.get(0);
        assertTrue(b.getBundleType().getName(), b.getName().contains(name));
        assertTrue(b.getBundleType().getName(), b.getBundleType().getName().contains(name));
        assertNotNull(b.getBundleVersions());
        assertEquals(1, b.getBundleVersions().size());
        BundleVersion bv = b.getBundleVersions().get(0);
        assertEquals(bv2, bv);
        assertEquals(b, bv.getBundle());
        Repo r = b.getRepo();
        assertNotNull(r);
        assertEquals(b.getName(), r.getName());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindBundlesByCriteriaPaging() throws Exception {
        Bundle b = null;
        for (int i = 0; i < 9; i++) {
            createBundle("name" + (i + 1));
        }
        Bundle b10 = createBundle("name10");

        BundleCriteria c = new BundleCriteria();
        PageList<Bundle> bs;

        // return first 5
        c.addFilterName(TEST_PREFIX);
        c.setPaging(0, 5);
        c.fetchBundleVersions(true);
        c.fetchDestinations(true);
        c.fetchPackageType(true);
        c.fetchRepo(true);
        bs = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bs);
        assertEquals(5, bs.size());
        assertFalse(bs.get(0).equals(bs.get(1)));

        // return last 2
        c.addFilterName(TEST_PREFIX);
        c.setPaging(4, 2);
        c.fetchBundleVersions(true);
        c.fetchDestinations(true);
        c.fetchPackageType(true);
        c.fetchRepo(true);
        bs = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bs);
        assertEquals(2, bs.size());
        assertEquals(b10, bs.get(1));

        // return last 1
        c.addFilterName(TEST_PREFIX);
        c.setCaseSensitive(true);
        c.setPaging(1, 9);
        c.fetchBundleVersions(true);
        c.fetchDestinations(true);
        c.fetchPackageType(true);
        c.fetchRepo(true);
        bs = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bs);
        assertEquals(1, bs.size());
        assertEquals(b10, bs.get(0));
    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindAndDeleteBundlesByCriteriaQuery() throws Exception {
        // verify that all bundle version objects are actually parsed. 
        Map<String, Bundle> bundleNames = new HashMap<String, Bundle>();
        final int bundleCount = 50;

        Bundle b01 = createBundle("name01");
        for (int i = 1; i < bundleCount; i++) {
            createBundle("name" + String.format("%02d", i + 1));
        }

        BundleCriteria criteria = new BundleCriteria();
        criteria.addFilterName(TEST_PREFIX);
        final int pageSize = 10;
        criteria.setPaging(0, pageSize);
        criteria.addSortName(PageOrdering.DESC);

        // iterate over the results with CriteriaQuery
        CriteriaQueryExecutor<Bundle, BundleCriteria> queryExecutor = new CriteriaQueryExecutor<Bundle, BundleCriteria>() {
            @Override
            public PageList<Bundle> execute(BundleCriteria criteria) {
                return bundleManager.findBundlesByCriteria(overlord, criteria);
            }
        };

        CriteriaQuery<Bundle, BundleCriteria> bundles = new CriteriaQuery<Bundle, BundleCriteria>(criteria,
            queryExecutor);

        List<Integer> toRemove = new ArrayList<Integer>(bundleNames.size());
        String prevName = null;
        // iterate over the entire result set efficiently
        String errMsg = "Results should be sorted by names, something is out of order";
        for (Bundle b : bundles) {
            assertTrue(errMsg, null == prevName || prevName.compareTo(b.getName()) > 0);
            prevName = b.getName();
            toRemove.add(b.getId());
            bundleNames.remove(String.valueOf(b.getName()));
        }

        // remove the bundles
        for (int id : toRemove) {
            bundleManager.deleteBundle(overlord, id);
        }

        // check if the last name is equal to "name01" 
        assertEquals("The name should be \"name01\"", b01.getName(), prevName);

        // test that entire list parsed spanning multiple pages
        assertTrue("Expected bundleNames to be empty. Still " + bundleNames.size() + " bundle(s).",
            bundleNames.isEmpty());

        // check if everything is deleted
        criteria = new BundleCriteria();
        criteria.addFilterName(TEST_PREFIX);
        criteria.clearPaging(); // fetch all
        PageList<Bundle> bvs = bundleManager.findBundlesByCriteria(overlord, criteria);
        assertNotNull(bvs);
        assertTrue(bvs.isEmpty());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindBundleVersionsByCriteria() throws Exception {
        Bundle b1 = createBundle("one");
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        BundleVersion bv2 = createBundleVersion(b1.getName(), "2.0", b1);
        BundleVersion bv3 = createBundleVersion(b1.getName(), "2.1", b1);
        BundleVersionCriteria c = new BundleVersionCriteria();
        PageList<BundleVersion> bvs;
        BundleVersion bvOut;

        // return all with no optional data
        c.addFilterName(TEST_PREFIX);
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(3, bvs.size());
        assertFalse(bvs.get(0).equals(bvs.get(1)));
        assertFalse(bvs.get(0).equals(bvs.get(2)));
        assertFalse(bvs.get(1).equals(bvs.get(2)));
        assertTrue(bvs.get(0).equals(bvs.get(0)));
        assertTrue(bvs.get(0).equals(bv2) || bvs.get(1).equals(bv2) || bvs.get(2).equals(bv2));

        // return bundle version using all criteria and with all optional data
        BundleVersion bvIn = bvs.get(1);
        c.addFilterId(bvIn.getId());
        c.addFilterName(bvIn.getName());
        c.addFilterBundleName("one");
        c.addFilterVersion(bvIn.getVersion());
        c.fetchBundle(true);
        c.fetchBundleDeployments(true);
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        bvOut = bvs.get(0);
        assertEquals(bvIn, bvOut);
        assertEquals(bvOut.getBundle(), b1);
        assertNotNull(bvOut.getBundleDeployments());
        assertTrue(bvOut.getBundleDeployments().isEmpty());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindBundleVersionsByCriteriaPaging() throws Exception {
        Bundle b1 = createBundle("one");
        for (int i = 0; i < 59; i++) {
            createBundleVersion(b1.getName(), "1." + String.format("%02d", i + 1), b1);
        }
        BundleVersion bv60 = createBundleVersion(b1.getName(), "1.60", b1);

        BundleVersionCriteria c = new BundleVersionCriteria();
        PageList<BundleVersion> bvs;
        BundleVersion bvOut = null;

        // return first ten
        c.addFilterName(TEST_PREFIX);
        c.addSortId(PageOrdering.ASC); // without sorting we'll get no predictable ordering
        c.setPaging(0, 10);
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(10, bvs.size());
        assertFalse(bvs.get(0).equals(bvs.get(1)));

        // return last 3
        c.addFilterName(TEST_PREFIX);
        c.setPaging(19, 3);
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(3, bvs.size());
        assertFalse(bvs.get(0).equals(bvs.get(1)));
        assertEquals(bv60, bvs.get(2));
    }

    @Test(enabled = ENABLED)
    public void testFindAndDeleteBundleVersionsByCriteriaQuery() throws Exception {
        //verify that all bundle version objects are actually parsed. 
        Map<String, BundleVersion> bundleVersionVersions = new HashMap<String, BundleVersion>();

        final int bundleVersionCount = 220;
        Bundle bundle = createBundle("one");
        for (int i = 0; i < bundleVersionCount; i++) {
            String version = "1." + String.format("%03d", i + 1);
            BundleVersion bundleVersion = createBundleVersion(bundle.getName(), version, bundle);
            bundleVersionVersions.put(version, bundleVersion);
        }

        final int pageSize = 20;
        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.addFilterName(TEST_PREFIX);
        criteria.setPaging(0, pageSize);
        criteria.addSortId(PageOrdering.DESC);

        // iterate over the results with CriteriaQuery
        CriteriaQueryExecutor<BundleVersion, BundleVersionCriteria> queryExecutor = new CriteriaQueryExecutor<BundleVersion, BundleVersionCriteria>() {
            @Override
            public PageList<BundleVersion> execute(BundleVersionCriteria criteria) {
                return bundleManager.findBundleVersionsByCriteria(overlord, criteria);
            }
        };

        // initiate first/(total depending on page size) request.
        CriteriaQuery<BundleVersion, BundleVersionCriteria> bundleVersions = new CriteriaQuery<BundleVersion, BundleVersionCriteria>(
            criteria, queryExecutor);

        List<Integer> toDelete = new ArrayList<Integer>(bundleVersionVersions.size());
        Integer prevId = null;
        // iterate over the entire result set efficiently
        String errMsg = "Results should be sorted by id, something is out of order";
        for (BundleVersion bv : bundleVersions) {
            assertTrue(errMsg, null == prevId || prevId > bv.getId());
            prevId = bv.getId();
            toDelete.add(bv.getId());
            bundleVersionVersions.remove(String.valueOf(bv.getVersion()));
        }

        // delete all
        for (int id : toDelete) {
            bundleManager.deleteBundleVersion(overlord, id, true);
        }

        // check whether every record was processed when iterating over the bundleVersions
        assertTrue("Expected bundleVersions to be empty. Still " + bundleVersionVersions.size() + " version(s).",
            bundleVersionVersions.isEmpty());

        // test that entire list parsed spanning multiple pages
        assertTrue("Expected bundleVersions to be empty. Still " + bundleVersionVersions.size() + " version(s).",
            bundleVersionVersions.isEmpty());

        // check if everything is deleted
        criteria = new BundleVersionCriteria();
        criteria.addFilterName(TEST_PREFIX);
        criteria.clearPaging(); // fetch all
        PageList<BundleVersion> bvs = bundleManager.findBundleVersionsByCriteria(overlord, criteria);
        assertNotNull(bvs);
        assertTrue(bvs.isEmpty());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testGetAllBundleVersionFilenames() throws Exception {
        final Bundle b1 = createBundle("one");
        assertNotNull(b1);
        final BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        assertNotNull(bv1);
        final HashMap<String, Boolean> files = bundleManager.getAllBundleVersionFilenames(overlord, bv1.getId());
        assertNotNull(files);
        assertEquals(DEFAULT_CRITERIA_PAGE_SIZE + 2, files.keySet().size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleAndBundleVersionStrictName1() throws Exception {
        final Bundle b1 = createBundle("one");
        final String name = "on";
        final String fullName = TEST_PREFIX + "-bundle-" + name;
        final BundleType type = createBundleType(name);
        final String recipe = "deploy -f " + TEST_PREFIX + ".zip -d @@ test.path @@";
        final BundleVersion bundleVerison = bundleManager.createBundleAndBundleVersion(overlord, fullName,
            "description", type.getId(), null, fullName, fullName + "-desc", "3.0", recipe);
        assertNotNull(bundleVerison);

        // find the previously created bundle
        BundleCriteria c = new BundleCriteria();
        c.addFilterName(TEST_PREFIX + "-bundle-one");
        c.setStrict(true);
        PageList<Bundle> bundles1 = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bundles1);
        assertEquals(1, bundles1.size());
        Bundle fetchedBundle1 = bundles1.get(0);

        // find the newly created bundle
        c = new BundleCriteria();
        c.addFilterName(fullName);
        c.setStrict(true);
        PageList<Bundle> bundles2 = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bundles2);
        assertEquals(1, bundles2.size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleAndBundleVersionStrictName2() throws Exception {
        final String name = "one";
        final Bundle bundle = createBundle(name);
        final String fullName = TEST_PREFIX + "-bundle-" + name;
        final String recipe = "deploy -f " + TEST_PREFIX + ".zip -d @@ test.path @@";
        final BundleVersion bundleVerison = bundleManager.createBundleAndBundleVersion(overlord, fullName,
            "description", bundle.getBundleType().getId(), null, fullName, fullName + "-desc", "3.0", recipe);

        // find the newly created bundle
        BundleCriteria c = new BundleCriteria();
        c.addFilterName(fullName);
        c.setStrict(true);
        PageList<Bundle> bundles = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bundles);
        assertEquals(1, bundles.size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAuthzBundleGroup() throws Exception {
        Subject subject = createNewSubject(TEST_USER_NAME);
        Role role = createNewRoleForSubject(subject, TEST_ROLE_NAME);

        subject = createSession(subject); // start a session so we can use this subject in SLSB calls 

        BundleGroup bundleGroup = new BundleGroup(TEST_BUNDLE_GROUP_NAME);
        bundleGroup.setDescription("test");

        // deny bundle group create
        try {
            bundleManager.createBundleGroup(subject, bundleGroup);
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow bundle group create
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        bundleGroup = bundleManager.createBundleGroup(subject, bundleGroup);

        // deny bundle group delete
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        try {
            bundleManager.deleteBundleGroups(subject, new int[] { bundleGroup.getId() });
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // deny global perm bundleGroup view
        BundleGroupCriteria bgCriteria = new BundleGroupCriteria();
        List<BundleGroup> bundleGroups = bundleManager.findBundleGroupsByCriteria(subject, bgCriteria);
        assertNotNull(bundleGroups);
        assert bundleGroups.isEmpty() : "Should not be able to see unassociated bundle group";

        // allow global perm bundleGroup view
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        bundleGroups = bundleManager.findBundleGroupsByCriteria(subject, bgCriteria);
        assertNotNull(bundleGroups);
        assertEquals("Should be able to see unassociated bundle group", 1, bundleGroups.size());

        // allow bundle group delete        
        bundleManager.deleteBundleGroups(subject, new int[] { bundleGroup.getId() });
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        // deny unassigned bundle create (no global create or view)
        try {
            createBundle(subject, TEST_PREFIX + ".bundle");
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // deny unassigned bundle create (no global view)
        addRolePermissions(role, Permission.CREATE_BUNDLES);
        try {
            createBundle(subject, TEST_PREFIX + ".bundle");
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // deny unassigned bundle create (no global create)
        removeRolePermissions(role, Permission.CREATE_BUNDLES);
        addRolePermissions(role, Permission.VIEW_BUNDLES);
        try {
            createBundle(subject, TEST_PREFIX + ".bundle");
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow unassigned bundle create
        addRolePermissions(role, Permission.CREATE_BUNDLES);
        Bundle bundle = createBundle(subject, TEST_PREFIX + ".bundle");

        // deny unassigned bundle view
        removeRolePermissions(role, Permission.CREATE_BUNDLES, Permission.VIEW_BUNDLES);
        BundleCriteria bCriteria = new BundleCriteria();
        List<Bundle> bundles = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assertNotNull(bundles);
        assert bundles.isEmpty() : "Should not be able to see unassigned bundle";

        // allow unassigned bundle view
        addRolePermissions(role, Permission.VIEW_BUNDLES);
        bundles = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assertNotNull(bundles);
        assertEquals("Should be able to see unassigned bundle", 1, bundles.size());

        // deny global perm bundle assign
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        bundleGroup = new BundleGroup(TEST_BUNDLE_GROUP_NAME);
        bundleGroup.setDescription("test");

        bundleGroup = bundleManager.createBundleGroup(subject, bundleGroup);
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        try {
            bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup.getId() },
                new int[] { bundle.getId() });
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow bundle assign via global manage_bundle_groups 
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup.getId() },
            new int[] { bundle.getId() });

        // allow bundle unassign via global manage_bundle_groups 
        bundleManager.unassignBundlesFromBundleGroups(subject, new int[] { bundleGroup.getId() },
            new int[] { bundle.getId() });

        // allow bundle assign via global create
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        addRolePermissions(role, Permission.CREATE_BUNDLES);
        bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup.getId() },
            new int[] { bundle.getId() });

        // deny bundle unassign via global create
        try {
            bundleManager.unassignBundlesFromBundleGroups(subject, new int[] { bundleGroup.getId() },
                new int[] { bundle.getId() });
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow bundle unassign via global delete 
        addRolePermissions(role, Permission.DELETE_BUNDLES);
        bundleManager.unassignBundlesFromBundleGroups(subject, new int[] { bundleGroup.getId() },
            new int[] { bundle.getId() });
        removeRolePermissions(role, Permission.DELETE_BUNDLES);

        // deny bundle assign with global create but no view
        removeRolePermissions(role, Permission.VIEW_BUNDLES);
        try {
            bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup.getId() },
                new int[] { bundle.getId() });
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // go back and again assign via global create and view
        addRolePermissions(role, Permission.VIEW_BUNDLES);
        bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup.getId() },
            new int[] { bundle.getId() });

        // deny assigned, unassociated-bundle-group bundle view
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        removeRolePermissions(role, Permission.VIEW_BUNDLES);
        bundles = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assertNotNull(bundles);
        assert bundles.isEmpty() : "Should not be able to see assigned bundle";

        // allow assigned, associated-bundle-group bundle view
        addRoleBundleGroup(role, bundleGroup);
        bundles = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assertNotNull(bundles);
        assertEquals("Should be able to see assigned bundle", 1, bundles.size());

        // check new bundle criteria options (no match)
        bCriteria.addFilterBundleGroupIds(87678);
        bCriteria.fetchBundleGroups(true);
        bundles = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assertNotNull(bundles);
        assert bundles.isEmpty() : "Should not have found anything";

        // check new bundle criteria options (match)
        bCriteria.addFilterBundleGroupIds(bundleGroup.getId());
        bCriteria.fetchBundleGroups(true);
        bundles = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assertNotNull(bundles);
        assertEquals("Should be able to see assigned bundle", 1, bundles.size());
        assertNotNull(bundles.get(0).getBundleGroups());
        assertEquals("Should have fetched bundlegroup", 1, bundles.get(0).getBundleGroups().size());
        assertEquals("Should have fetched expected bundlegroup", bundleGroup, bundles.get(0).getBundleGroups()
            .iterator().next());

        // check new bundle group criteria options (no match)
        bgCriteria.addFilterId(87678);
        bgCriteria.addFilterBundleIds(87678);
        bgCriteria.addFilterRoleIds(87678);
        bgCriteria.fetchBundles(true);
        bgCriteria.fetchRoles(true);
        bundleGroups = bundleManager.findBundleGroupsByCriteria(subject, bgCriteria);
        assertNotNull(bundleGroups);
        assert bundleGroups.isEmpty() : "Should not have found anything";

        // check new bundle group criteria options (no match)
        bgCriteria.addFilterId(bundleGroup.getId());
        bundleGroups = bundleManager.findBundleGroupsByCriteria(subject, bgCriteria);
        assertNotNull(bundleGroups);
        assert bundleGroups.isEmpty() : "Should not have found anything";

        // check new bundle group criteria options (no match)
        bgCriteria.addFilterBundleIds(bundle.getId());
        bundleGroups = bundleManager.findBundleGroupsByCriteria(subject, bgCriteria);
        assertNotNull(bundleGroups);
        assert bundleGroups.isEmpty() : "Should not have found anything";

        // check new bundle group criteria options (match)
        bgCriteria.addFilterRoleIds(role.getId());
        bundleGroups = bundleManager.findBundleGroupsByCriteria(subject, bgCriteria);
        assertNotNull(bundleGroups);
        assertEquals("Should be able to see assigned bundle", 1, bundleGroups.size());
        assertNotNull(bundleGroups.get(0).getBundles());
        assertEquals("Should have fetched bundle in bundle group", 1, bundleGroups.get(0).getBundles().size());
        assertEquals("Should have fetched bundle in bundle group", bundle, bundleGroups.get(0).getBundles().iterator()
            .next());
        assertNotNull(bundleGroups.get(0).getRoles());
        assertEquals("Should have fetched role for bundle group", 1, bundleGroups.get(0).getRoles().size());
        assertEquals("Should have fetched role for bundle group", role, bundleGroups.get(0).getRoles().iterator()
            .next());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAuthzCreateBundleVersion() throws Exception {
        Subject subject = createNewSubject(TEST_USER_NAME);
        Role role = createNewRoleForSubject(subject, TEST_ROLE_NAME);

        subject = createSession(subject); // start a session so we can use this subject in SLSB calls 

        // create bundle group
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        BundleGroup bundleGroup1 = new BundleGroup(TEST_BUNDLE_GROUP_NAME + "_1");
        bundleGroup1.setDescription("bg-1");
        bundleGroup1 = bundleManager.createBundleGroup(subject, bundleGroup1);
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        // add bg1 to the role, but no perms
        addRoleBundleGroup(role, bundleGroup1);

        // deny bundle create in bg1 (no create perm)
        try {
            createBundle(subject, TEST_PREFIX + ".bundle", bundleGroup1.getId());
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow bundle creation in bg1 (has create perm)
        addRolePermissions(role, Permission.CREATE_BUNDLES_IN_GROUP);
        Bundle bundle = createBundle(subject, TEST_PREFIX + ".bundle", bundleGroup1.getId());

        // deny bundle version creation (perm taken away)
        removeRolePermissions(role, Permission.CREATE_BUNDLES_IN_GROUP);
        try {
            BundleVersion bv1 = createBundleVersion(subject, bundle.getName() + "-1", null, bundle);
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow bundle version creation (perm granted)
        addRolePermissions(role, Permission.CREATE_BUNDLES_IN_GROUP);
        BundleVersion bv1 = createBundleVersion(subject, bundle.getName() + "-1", null, bundle);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        assert 0 == bv1.getVersionOrder();

        // create second role 
        Role role2 = createNewRoleForSubject(subject, TEST_ROLE_NAME + "_2");
        addRolePermissions(role2, Permission.CREATE_BUNDLES_IN_GROUP);

        // create second bundle group
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        BundleGroup bundleGroup2 = new BundleGroup(TEST_BUNDLE_GROUP_NAME + "_2");
        bundleGroup2.setDescription("bg-2");
        bundleGroup2 = bundleManager.createBundleGroup(subject, bundleGroup2);
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        // deny bundle create in bg2 (not associated with role)
        try {
            createBundle(subject, TEST_PREFIX + ".bundle", bundleGroup2.getId());
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // deny bundle assign to bg2 (not associated with role)
        try {
            bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup2.getId() },
                new int[] { bundle.getId() });
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // add bg2 to the role
        addRoleBundleGroup(role2, bundleGroup2);

        // deny bundle assign to bg2 (no perm)
        removeRolePermissions(role2, Permission.CREATE_BUNDLES_IN_GROUP);
        try {
            bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup2.getId() },
                new int[] { bundle.getId() });
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow bundle assign to bg2
        addRolePermissions(role2, Permission.ASSIGN_BUNDLES_TO_GROUP);
        bundleManager.assignBundlesToBundleGroups(subject, new int[] { bundleGroup2.getId() },
            new int[] { bundle.getId() });

        // should fetch the single bundle even though it is in two groups
        BundleCriteria bundleCriteria = new BundleCriteria();
        bundleCriteria.addFilterBundleGroupIds(bundleGroup1.getId(), bundleGroup2.getId());
        List<Bundle> bundles = bundleManager.findBundlesByCriteria(subject, bundleCriteria);
        assertNotNull(bundles);
        assertEquals("Should be able to see assigned bundle", 1, bundles.size());
        assertEquals("Should have fetched bundle", bundle, bundles.get(0));

        BundleVersionCriteria bvCriteria = new BundleVersionCriteria();
        bvCriteria.addFilterBundleId(bundle.getId());
        List<BundleVersion> bundleVersions = bundleManager.findBundleVersionsByCriteria(subject, bvCriteria);
        assertNotNull(bundleVersions);
        assertEquals("Should be able to see assigned bundle bundleversion", 1, bundleVersions.size());
        assertEquals("Should have fetched bundleversion", bv1, bundleVersions.get(0));

        // deny unassign
        try {
            bundleManager.unassignBundlesFromBundleGroups(subject, new int[] { bundleGroup2.getId() },
                new int[] { bundle.getId() });
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow unassigns
        addRolePermissions(role, Permission.UNASSIGN_BUNDLES_FROM_GROUP);
        addRolePermissions(role2, Permission.UNASSIGN_BUNDLES_FROM_GROUP);
        bundleManager.unassignBundlesFromBundleGroups(subject, new int[] { bundleGroup1.getId() },
            new int[] { bundle.getId() });
        bundleManager.unassignBundlesFromBundleGroups(subject, new int[] { bundleGroup2.getId() },
            new int[] { bundle.getId() });

        // should not find the now unassigned bundle
        bundles = bundleManager.findBundlesByCriteria(subject, bundleCriteria);
        assertNotNull(bundles);
        assertEquals("Should not be able to see unassigned bundle", 0, bundles.size());

        bundleVersions = bundleManager.findBundleVersionsByCriteria(subject, bvCriteria);
        assertNotNull(bundleVersions);
        assertEquals("Should not be able to see unassigned bundle bundleversion", 0, bundleVersions.size());

        // allow view
        addRolePermissions(role, Permission.VIEW_BUNDLES);

        // should fetch the single unassigned bundle due to global view in one of the assigned roles
        bundleCriteria.addFilterBundleGroupIds((Integer[]) null);
        bundles = bundleManager.findBundlesByCriteria(subject, bundleCriteria);
        assertNotNull(bundles);
        assertEquals("Should be able to see unassigned bundle", 1, bundles.size());
        assertEquals("Should have fetched bundle", bundle, bundles.get(0));

        bundleVersions = bundleManager.findBundleVersionsByCriteria(subject, bvCriteria);
        assertNotNull(bundleVersions);
        assertEquals("Should be able to see unassigned bundle bundleversion", 1, bundleVersions.size());
        assertEquals("Should have fetched bundleversion", bv1, bundleVersions.get(0));
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAuthzDeleteBundleVersion() throws Exception {
        Subject subject = createNewSubject(TEST_USER_NAME);
        Role role = createNewRoleForSubject(subject, TEST_ROLE_NAME);

        subject = createSession(subject); // start a session so we can use this subject in SLSB calls 

        // create bundle group
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        BundleGroup bundleGroup1 = new BundleGroup(TEST_BUNDLE_GROUP_NAME + "_1");
        bundleGroup1.setDescription("bg-1");
        bundleGroup1 = bundleManager.createBundleGroup(subject, bundleGroup1);
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        // add bg1 to the role with group create
        addRoleBundleGroup(role, bundleGroup1);
        addRolePermissions(role, Permission.CREATE_BUNDLES_IN_GROUP);

        // allow bundle creation in bg1 (has create perm)
        Bundle bundle = createBundle(subject, TEST_PREFIX + ".bundle", bundleGroup1.getId());

        // allow delete, global perm
        addRolePermissions(role, Permission.DELETE_BUNDLES);
        deleteBundleVersion(subject, bundle);

        // allow bundle creation in bg1 (has create perm)
        bundle = createBundle(subject, TEST_PREFIX + ".bundle", bundleGroup1.getId());

        // allow delete, bundle group perm
        removeRolePermissions(role, Permission.DELETE_BUNDLES);
        addRolePermissions(role, Permission.DELETE_BUNDLES_FROM_GROUP);
        deleteBundleVersion(subject, bundle);

        // allow bundle creation in bg1 (has create perm)
        bundle = createBundle(subject, TEST_PREFIX + ".bundle", bundleGroup1.getId());

        // deny delete, no delete perms
        removeRolePermissions(role, Permission.DELETE_BUNDLES_FROM_GROUP);
        try {
            deleteBundleVersion(subject, bundle);
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAuthzBundleDest() throws Exception {
        Subject subject = createNewSubject(TEST_USER_NAME);
        Role role = createNewRoleForSubject(subject, TEST_ROLE_NAME);
        subject = createSession(subject); // start a session so we can use this subject in SLSB calls 

        // create bundle group
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        BundleGroup bundleGroup = new BundleGroup(TEST_BUNDLE_GROUP_NAME);
        bundleGroup.setDescription("bg");
        bundleGroup = bundleManager.createBundleGroup(subject, bundleGroup);
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        // add bg to the role with group create
        addRoleBundleGroup(role, bundleGroup);
        addRolePermissions(role, Permission.CREATE_BUNDLES_IN_GROUP);

        // allow bundle creation in bg (has create perm)        
        Bundle b1 = createBundle(subject, "one", bundleGroup.getId());
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(subject, b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        ResourceGroup platformResourceGroup = createTestResourceGroup(false);
        assertNotNull(platformResourceGroup);

        // deny destination create (no view of resource group)
        try {
            BundleDestination dest1 = createDestination(subject, b1, "one", "/test", platformResourceGroup);
            fail("Should have thrown IllegalArgumentException");
        } catch (EJBException e) {
            assert e.getCause() instanceof IllegalArgumentException
                && e.getCause().getMessage().contains("Invalid groupId") : "Should have not had group visibility";
            // expected
        }

        // deny destination create (no deploy perm)
        LookupUtil.getRoleManager().addResourceGroupsToRole(overlord, role.getId(),
            new int[] { platformResourceGroup.getId() });
        try {
            BundleDestination dest1 = createDestination(subject, b1, "one", "/test", platformResourceGroup);
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow global
        addRolePermissions(role, Permission.DEPLOY_BUNDLES);
        BundleDestination dest1 = createDestination(subject, b1, "one", "/test", platformResourceGroup);
        assertNotNull(dest1);
        Configuration config = new Configuration();
        config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        BundleDeployment bd1;
        bd1 = createDeployment(subject, "one", bv1, dest1, config);
        assertNotNull(bd1);

        // allow group
        removeRolePermissions(role, Permission.DEPLOY_BUNDLES);
        addRolePermissions(role, Permission.DEPLOY_BUNDLES_TO_GROUP);
        BundleDestination dest2 = createDestination(subject, b1, "two", "/test2", platformResourceGroup);
        assertNotNull(dest2);
        Configuration config2 = new Configuration();
        config2.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        BundleDeployment bd2;
        bd2 = createDeployment(subject, "two", bv1, dest2, config2);
        assertNotNull(bd1);

        // deny delete deployment
        removeRolePermissions(role, Permission.DEPLOY_BUNDLES_TO_GROUP);
        try {
            bundleManager.deleteBundleDeployment(subject, bd2.getId());
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow delete deployment
        addRolePermissions(role, Permission.DEPLOY_BUNDLES);
        bundleManager.deleteBundleDeployment(subject, bd2.getId());

        // deny delete destination
        removeRolePermissions(role, Permission.DEPLOY_BUNDLES);
        try {
            bundleManager.deleteBundleDestination(subject, dest2.getId());
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow delete destination
        addRolePermissions(role, Permission.DEPLOY_BUNDLES_TO_GROUP);
        bundleManager.deleteBundleDestination(subject, dest2.getId());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAuthzBundleDeploy() throws Exception {
        Subject subject = createNewSubject(TEST_USER_NAME);
        Role role = createNewRoleForSubject(subject, TEST_ROLE_NAME);
        subject = createSession(subject); // start a session so we can use this subject in SLSB calls 

        // create bundle group
        addRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);
        BundleGroup bundleGroup = new BundleGroup(TEST_BUNDLE_GROUP_NAME);
        bundleGroup.setDescription("bg");
        bundleGroup = bundleManager.createBundleGroup(subject, bundleGroup);
        removeRolePermissions(role, Permission.MANAGE_BUNDLE_GROUPS);

        // add bg to the role with group create
        addRoleBundleGroup(role, bundleGroup);
        addRolePermissions(role, Permission.CREATE_BUNDLES_IN_GROUP);

        // allow bundle creation in bg (has create perm)        
        Bundle b1 = createBundle(subject, "one", bundleGroup.getId());
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(subject, b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        ResourceGroup platformResourceGroup = createTestResourceGroup(false);
        assertNotNull(platformResourceGroup);
        LookupUtil.getRoleManager().addResourceGroupsToRole(overlord, role.getId(),
            new int[] { platformResourceGroup.getId() });

        // allow dest/deploy create (global)
        addRolePermissions(role, Permission.DEPLOY_BUNDLES);
        BundleDestination dest1 = createDestination(subject, b1, "one", "/test", platformResourceGroup);
        assertNotNull(dest1);
        Configuration config = new Configuration();
        config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        BundleDeployment bd1;
        bd1 = createDeployment(subject, "one", bv1, dest1, config);
        assertNotNull(bd1);

        // deny schedule
        removeRolePermissions(role, Permission.DEPLOY_BUNDLES);
        try {
            BundleDeployment bd1d = bundleManager.scheduleBundleDeployment(subject, bd1.getId(), false);
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // test with global perm
        testAuthzBundleDeployInternal(subject, role, bd1, dest1, platformResourceGroup, Permission.DEPLOY_BUNDLES);

        // test with bundle group perm        
        testAuthzBundleDeployInternal(subject, role, bd1, dest1, platformResourceGroup,
            Permission.DEPLOY_BUNDLES_TO_GROUP);
    }

    private void testAuthzBundleDeployInternal(Subject subject, Role role, BundleDeployment bd1,
        BundleDestination dest1, ResourceGroup platformResourceGroup, Permission permission) throws Exception {

        // allow
        addRolePermissions(role, permission);

        BundleDeployment bd1d = bundleManager.scheduleBundleDeployment(subject, bd1.getId(), false);
        assertNotNull(bd1d);
        assertEquals(bd1.getId(), bd1d.getId());

        BundleDeploymentCriteria bdc = new BundleDeploymentCriteria();
        bdc.addFilterId(bd1d.getId());
        bdc.fetchBundleVersion(true);
        bdc.fetchDestination(true);
        bdc.fetchResourceDeployments(true);
        bdc.fetchTags(true);
        List<BundleDeployment> bds = bundleManager.findBundleDeploymentsByCriteria(subject, bdc);
        assertEquals(1, bds.size());
        bd1d = bds.get(0);

        assertEquals(platformResourceGroup, bd1d.getDestination().getGroup());
        assertEquals(dest1.getId(), bd1d.getDestination().getId());

        BundleResourceDeploymentCriteria c = new BundleResourceDeploymentCriteria();
        c.addFilterBundleDeploymentId(bd1d.getId());
        c.fetchBundleDeployment(true);
        c.fetchHistories(true);
        c.fetchResource(true);
        List<BundleResourceDeployment> brds = bundleManager.findBundleResourceDeploymentsByCriteria(subject, c);
        assertEquals(1, brds.size());
        assertEquals(1, bd1d.getResourceDeployments().size());
        assertEquals(bd1d.getResourceDeployments().get(0).getId(), brds.get(0).getId());
        BundleResourceDeployment brd = brds.get(0);

        assertNotNull(brd.getBundleResourceDeploymentHistories());
        int size = brd.getBundleResourceDeploymentHistories().size();
        assertTrue(size > 0);
        String auditMessage = "BundleTest-Message";
        bundleManager.addBundleResourceDeploymentHistoryInNewTrans(overlord, brd.getId(),
            new BundleResourceDeploymentHistory(overlord.getName(), auditMessage, auditMessage,
                BundleResourceDeploymentHistory.Category.DEPLOY_STEP, BundleResourceDeploymentHistory.Status.SUCCESS,
                auditMessage, auditMessage));

        brds = bundleManager.findBundleResourceDeploymentsByCriteria(subject, c);
        assertEquals(1, brds.size());
        assertEquals(brd.getId(), brds.get(0).getId());
        brd = brds.get(0);
        assertNotNull(brd.getBundleResourceDeploymentHistories());
        assertTrue((size + 1) == brd.getBundleResourceDeploymentHistories().size());
        BundleResourceDeploymentHistory newHistory = null;
        for (BundleResourceDeploymentHistory h : brd.getBundleResourceDeploymentHistories()) {
            if (auditMessage.equals(h.getMessage())) {
                newHistory = h;
                break;
            }
        }
        assertNotNull(newHistory);
        assertEquals(auditMessage, newHistory.getAction());
        assertEquals(BundleResourceDeploymentHistory.Status.SUCCESS, newHistory.getStatus());

        // deny purge destination
        //TransactionManager txMgr = getTransactionManager();
        //txMgr.begin();
        //bd1 = em.find(BundleDeployment.class, bd1.getId());
        //bd1.setLive(true);
        //txMgr.commit();

        removeRolePermissions(role, permission);
        try {
            bundleManager.purgeBundleDestination(subject, dest1.getId());
            fail("Should have thrown PermissionException");
        } catch (PermissionException e) {
            // expected
        }

        // allow purge destination
        addRolePermissions(role, permission);
        bundleManager.purgeBundleDestination(subject, dest1.getId());

        // leave without the perm being assigned
        removeRolePermissions(role, permission);
    }

    // subject must have create bundle version permission
    private void deleteBundleVersion(Subject subject, Bundle b1) throws Exception {
        assertNotNull(b1);

        BundleVersion bv1 = createBundleVersion(subject, b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        BundleVersion bv2 = createBundleVersion(subject, b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());

        // let's add a bundle file so we can ensure our deletion will also delete the file too
        bundleManager.addBundleFileViaByteArray(subject, bv2.getId(), "testDeleteBundleVersion", "1.0",
            new Architecture("noarch"), "content".getBytes());
        BundleFileCriteria bfCriteria = new BundleFileCriteria();
        bfCriteria.addFilterBundleVersionId(bv2.getId());
        bfCriteria.fetchPackageVersion(true);
        PageList<BundleFile> files = bundleManager.findBundleFilesByCriteria(overlord, bfCriteria);
        assert files.size() == 1 : files;
        assert files.get(0).getPackageVersion().getGeneralPackage().getName().equals("testDeleteBundleVersion") : files;

        BundleVersionCriteria bvCriteria = new BundleVersionCriteria();
        BundleCriteria bCriteria = new BundleCriteria();

        // delete the first one - this deletes the BV but the bundle should remain intact
        bundleManager.deleteBundleVersion(subject, bv2.getId(), true);
        bvCriteria.addFilterId(bv2.getId());
        PageList<BundleVersion> bvResults = bundleManager.findBundleVersionsByCriteria(subject, bvCriteria);
        assert bvResults.size() == 0;
        bCriteria.addFilterId(b1.getId());
        PageList<Bundle> bResults = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assert bResults.size() == 1 : "Should not have deleted bundle yet, 1 version still exists";

        // delete the second one - this deletes last BV thus the bundle should also get deleted
        bundleManager.deleteBundleVersion(subject, bv1.getId(), true);
        bvCriteria.addFilterId(bv1.getId());
        bvResults = bundleManager.findBundleVersionsByCriteria(subject, bvCriteria);
        assert bvResults.size() == 0;
        bCriteria.addFilterId(b1.getId());
        bResults = bundleManager.findBundlesByCriteria(subject, bCriteria);
        assert bResults.size() == 0 : "Should have deleted bundle since no versions exists anymore";

        // make sure our composite query is OK and can show us 0 bundles, too
        PageList<BundleWithLatestVersionComposite> composites;
        bCriteria = new BundleCriteria();
        composites = bundleManager.findBundlesWithLatestVersionCompositesByCriteria(subject, bCriteria);
        assert composites.size() == 0;
    }

    private Subject createNewSubject(String subjectName) throws Exception {

        Subject newSubject = new Subject();
        newSubject.setName(subjectName);
        newSubject.setFactive(true);
        newSubject.setFsystem(false);

        return LookupUtil.getSubjectManager().createSubject(overlord, newSubject);
    }

    private Role createNewRoleForSubject(Subject subject, String roleName) throws Exception {
        Role newRole = new Role(roleName);
        newRole.setFsystem(false);
        newRole.addSubject(subject);

        return LookupUtil.getRoleManager().createRole(overlord, newRole);
    }

    private void addRolePermissions(Role role, Permission... permissions) throws Exception {

        for (Permission p : permissions) {
            role.getPermissions().add(p);
        }
        LookupUtil.getRoleManager().setPermissions(overlord, role.getId(), role.getPermissions());
    }

    private void removeRolePermissions(Role role, Permission... permissions) throws Exception {

        for (Permission p : permissions) {
            role.getPermissions().remove(p);
        }
        LookupUtil.getRoleManager().setPermissions(overlord, role.getId(), role.getPermissions());
    }

    private void addRoleBundleGroup(Role role, BundleGroup bundleGroup) throws Exception {

        int[] ids = new int[1];
        ids[0] = bundleGroup.getId();
        LookupUtil.getRoleManager().addBundleGroupsToRole(overlord, role.getId(), ids);
    }

    private void removeRoleBundleGroup(Role role, BundleGroup bundleGroup) throws Exception {

        int[] ids = new int[1];
        ids[0] = bundleGroup.getId();
        LookupUtil.getRoleManager().removeBundleGroupsFromRole(overlord, role.getId(), ids);
    }

    // helper methods
    private BundleType createBundleType(String name) throws Exception {
        final String fullName = TEST_PREFIX + "-type-" + name;
        BundleType bt = null;

        getTransactionManager().begin();
        try {
            Query q = em.createQuery("SELECT bt FROM BundleType bt WHERE bt.name = '" + fullName + "'");
            bt = (BundleType) q.getSingleResult();
        } catch (Throwable t) {
            // nothing
        } finally {
            getTransactionManager().commit();
        }

        if (null == bt) {
            ResourceType rt = createResourceTypeForBundleType(name);
            bt = bundleManager.createBundleType(overlord, fullName, rt.getId());
        }

        assert bt.getId() > 0;
        assert bt.getName().endsWith(fullName);
        return bt;
    }

    private Bundle createBundle(String name) throws Exception {
        return createBundle(overlord, name);
    }

    private Bundle createBundle(Subject subject, String name) throws Exception {
        return createBundle(subject, name, null);
    }

    private Bundle createBundle(Subject subject, String name, int bundleGroupId) throws Exception {
        return createBundle(subject, name, new int[] { bundleGroupId });
    }

    private Bundle createBundle(Subject subject, String name, int[] bundleGroupIds) throws Exception {
        BundleType bt = createBundleType(name);
        return createBundle(subject, name, bt, bundleGroupIds);
    }

    private Bundle createBundle(Subject subject, String name, BundleType bt, int[] bundleGroupIds) throws Exception {
        final String fullName = TEST_PREFIX + "-bundle-" + name;
        Bundle b = bundleManager.createBundle(subject, fullName, fullName + "-desc", bt.getId(), bundleGroupIds);

        assert b.getId() > 0;
        assert b.getName().endsWith(fullName);
        return b;
    }

    private BundleVersion createBundleVersion(String name, String version, Bundle bundle) throws Exception {
        return createBundleVersion(overlord, name, version, bundle);
    }

    private BundleVersion createBundleVersion(Subject subject, String name, String version, Bundle bundle)
        throws Exception {
        final String fullName = TEST_PREFIX + "-bundleversion-" + version + "-" + name;
        final String recipe = "deploy -f " + TEST_PREFIX + ".zip -d @@ test.path @@";
        BundleVersion bv = bundleManager.createBundleVersion(subject, bundle.getId(), fullName, fullName + "-desc",
            version, recipe);

        assert bv.getId() > 0;
        assert bv.getName().endsWith(fullName);
        return bv;
    }

    private BundleDestination createDestination(Bundle bundle, String name, String deployDir, ResourceGroup group)
        throws Exception {
        return createDestination(overlord, bundle, name, deployDir, group);
    }

    private BundleDestination createDestination(Subject subject, Bundle bundle, String name, String deployDir,
        ResourceGroup group) throws Exception {
        final String fullName = TEST_PREFIX + "-bundledestination-" + name;
        BundleDestination bd = bundleManager.createBundleDestination(subject, bundle.getId(), fullName, fullName,
            TEST_DESTBASEDIR_NAME, deployDir, group.getId());

        assert bd.getId() > 0;
        assert bd.getName().endsWith(fullName);
        assert bd.getDestinationBaseDirectoryName().equals(TEST_DESTBASEDIR_NAME);
        return bd;
    }

    private BundleDeployment createDeployment(String name, BundleVersion bv, BundleDestination dest,
        Configuration config) throws Exception {
        return createDeployment(overlord, name, bv, dest, config);
    }

    private BundleDeployment createDeployment(Subject subject, String name, BundleVersion bv, BundleDestination dest,
        Configuration config) throws Exception {
        final String fullName = TEST_PREFIX + "-bundledeployment-" + name;
        BundleDeployment bd = bundleManager.createBundleDeployment(subject, bv.getId(), dest.getId(), fullName, config);

        assert bd.getId() > 0;
        assert bd.getDescription().endsWith(fullName);
        return bd;
    }

    private ResourceType createResourceTypeForBundleType(String name) throws Exception {
        final String fullName = TEST_PREFIX + "-resourcetype-" + name;
        ResourceType rt = new ResourceType(fullName, "BundleManagerBeanTest", ResourceCategory.PLATFORM, null);

        TransactionManager txMgr = getTransactionManager();
        txMgr.begin();
        em.persist(rt);
        txMgr.commit();
        return rt;
    }

    // lifted from ResourceManagerBeanTest, with the addition of adding bundle config to the type
    private ResourceGroup createTestResourceGroup(boolean synthetic) throws Exception {
        getTransactionManager().begin();

        ResourceGroup resourceGroup = null;
        Resource resource;

        try {
            // Naming this with TEST_PREFIX allows cleanupDatabase to blow away these test resources along
            // with the bundle resource type
            ResourceType resourceType = new ResourceType(TEST_PREFIX + "-platform-" + System.currentTimeMillis(),
                "test", ResourceCategory.PLATFORM, null);

            // we need to make this test type bundle targetable
            ConfigurationDefinition pcDef = new ConfigurationDefinition(TEST_PREFIX + "-testdef", "bundle test");
            PropertyDefinitionSimple propDef = new PropertyDefinitionSimple(TEST_BUNDLE_DESTBASEDIR_PROP, "", true,
                PropertySimpleType.STRING);
            propDef.setDisplayName(TEST_BUNDLE_DESTBASEDIR_PROP);
            pcDef.put(propDef);
            em.persist(pcDef);

            ResourceTypeBundleConfiguration rtbc = new ResourceTypeBundleConfiguration(new Configuration());
            rtbc.addBundleDestinationBaseDirectory(TEST_DESTBASEDIR_NAME,
                ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context.pluginConfiguration.name(),
                TEST_BUNDLE_DESTBASEDIR_PROP, null);
            resourceType.setResourceTypeBundleConfiguration(rtbc);
            resourceType.setPluginConfigurationDefinition(pcDef);

            em.persist(resourceType);

            // make sure the bundle config is ok
            rtbc = resourceType.getResourceTypeBundleConfiguration();
            assert rtbc != null;
            assert rtbc.getBundleDestinationBaseDirectories().size() == 1;
            BundleDestinationBaseDirectory bdbd = rtbc.getBundleDestinationBaseDirectories().iterator().next();
            assert bdbd.getName().equals(TEST_DESTBASEDIR_NAME);
            assert bdbd.getValueContext() == ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context.pluginConfiguration;
            assert bdbd.getValueName().equals(TEST_BUNDLE_DESTBASEDIR_PROP);

            Agent agent = new Agent(TEST_PREFIX + "-testagent", "testaddress", 1, "", "testtoken");
            agent.setSynthetic(synthetic);
            em.persist(agent);
            em.flush();

            Configuration rc = new Configuration();
            rc.put(new PropertySimple(TEST_BUNDLE_DESTBASEDIR_PROP, TEST_BUNDLE_DESTBASEDIR_PROP_VALUE));
            em.persist(rc);

            resource = new Resource("reskey" + System.currentTimeMillis(), TEST_PREFIX + "-resname", resourceType);
            resource.setUuid("" + System.currentTimeMillis());
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
            resource.setAgent(agent);
            resource.setResourceConfiguration(rc);
            resource.setSynthetic(synthetic);
            em.persist(resource);

            resourceGroup = new ResourceGroup(TEST_PREFIX + "-group-" + System.currentTimeMillis());
            resourceGroup.addExplicitResource(resource);
            resourceGroup.addImplicitResource(resource);
            resourceGroup.setResourceType(resourceType); // need to tell the group the type it is
            em.persist(resourceGroup);

            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                System.out.println("CANNOT PREPARE TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
                //
            }
        }

        return resourceGroup;
    }

    private String readFile(File file) throws Exception {
        return new String(StreamUtil.slurp(new FileInputStream(file)));
    }

    private void writeFile(File file, String content) throws Exception {
        file.getParentFile().mkdirs();
        StreamUtil.copy(new ByteArrayInputStream(content.getBytes()), new FileOutputStream(file));
    }

    private File createZip(File destDir, String zipName, String[] entryNames, String[] contents) throws Exception {
        FileOutputStream stream = null;
        ZipOutputStream out = null;

        try {
            destDir.mkdirs();
            File zipFile = new File(destDir, zipName);
            stream = new FileOutputStream(zipFile);
            out = new ZipOutputStream(stream);

            assert contents.length == entryNames.length;
            for (int i = 0; i < contents.length; i++) {
                ZipEntry zipAdd = new ZipEntry(entryNames[i]);
                zipAdd.setTime(System.currentTimeMillis());
                out.putNextEntry(zipAdd);
                out.write(contents[i].getBytes());
            }
            return zipFile;
        } finally {
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }
}
