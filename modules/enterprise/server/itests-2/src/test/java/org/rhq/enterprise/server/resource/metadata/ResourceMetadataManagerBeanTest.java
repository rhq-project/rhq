package org.rhq.enterprise.server.resource.metadata;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.WordUtils;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.drift.Filter;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Test(groups = { "plugin.metadata" })
public class ResourceMetadataManagerBeanTest extends MetadataBeanTest {

    private static final String PLUGIN_NAME = "ResourceMetadataManagerBeanTestPlugin";
    //this is used in afterclass, which might execute with a different instance than the rest of the tests
    //therefore we need to make this static. Not sure who causes this, if it is Arq or TestNG itself.
    private static Set<Integer> groupIds = Collections.synchronizedSet(new HashSet<Integer>());

    @Test(groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void testRemovalOfObsoleteBundleAndDriftConfig() throws Exception {
        // create the initial type that has bundle and drift definitions 
        createPlugin("test-plugin.jar", "1.0", "remove_bundle_drift_config_v1.xml");

        // make sure the drift definition was persisted, and remember the type
        ResourceType type1 = assertResourceTypeAssociationEquals("ServerWithBundleAndDriftConfig", PLUGIN_NAME,
            "driftDefinitionTemplates", asList("drift1"));

        // sanity check, make sure our queries work and that we did persist these things
        Query qTemplate;
        Query qConfig;
        //String qTemplateString = "select ct from ConfigurationTemplate ct where ct.id = :id";
        String qTemplateString = "from DriftDefinitionTemplate where id = :id";
        String qConfigString = "from Configuration c where id = :id";
        DriftDefinitionTemplate driftTemplate = type1.getDriftDefinitionTemplates().iterator().next();
        Configuration bundleConfig = type1.getResourceTypeBundleConfiguration().getBundleConfiguration();
        Configuration driftDefConfig = driftTemplate.getConfiguration();

        getTransactionManager().begin();
        try {
            qTemplate = getEntityManager().createQuery(qTemplateString).setParameter("id", driftTemplate.getId());
            qConfig = getEntityManager().createQuery(qConfigString).setParameter("id", driftDefConfig.getId());
            assertEquals("drift template didn't get persisted", 1, qTemplate.getResultList().size());
            assertEquals("drift template config didn't get persisted", 1, qConfig.getResultList().size());

            qConfig.setParameter("id", bundleConfig.getId());
            assertEquals("bundle config didn't get persisted", 1, qConfig.getResultList().size());
        } finally {
            getTransactionManager().commit();
        }

        assertNotNull(type1.getResourceTypeBundleConfiguration());
        assertEquals("destdir1", type1.getResourceTypeBundleConfiguration().getBundleDestinationBaseDirectories()
            .iterator().next().getName());

        // upgrade the type which removes the bundle config and drift definition
        createPlugin("test-plugin.jar", "2.0", "remove_bundle_drift_config_v2.xml");

        getTransactionManager().begin();
        try {
            qTemplate = getEntityManager().createQuery(qTemplateString).setParameter("id", driftTemplate.getId());
            qConfig = getEntityManager().createQuery(qConfigString).setParameter("id", driftDefConfig.getId());
            assertEquals("drift template didn't get purged", 0, qTemplate.getResultList().size());
            assertEquals("drift template config didn't get purged", 0, qConfig.getResultList().size());

            qConfig.setParameter("id", bundleConfig.getId());
            assertEquals("bundle config didn't get purged", 0, qConfig.getResultList().size());
        } finally {
            getTransactionManager().commit();
        }
    }

    @Test(groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void registerPluginWithDuplicateDriftDefinitions() {
        try {
            createPlugin("test-plugin.jar", "1.0", "dup_drift.xml");
            fail("should not have succeeded - the drift definition had duplicate names");
        } catch (Exception e) {
            // OK, the plugin should have failed to be deployed since it has duplicate drift definitions
        }
    }

    @Test(dependsOnMethods = { "registerPluginWithDuplicateDriftDefinitions" }, groups = {
        "plugin.resource.metadata.test", "NewPlugin" })
    public void registerPlugin() throws Exception {
        createPlugin("test-plugin.jar", "1.0", "plugin_v1.xml");
    }

    @Test(dependsOnMethods = { "registerPluginWithDuplicateDriftDefinitions" }, groups = {
        "plugin.resource.metadata.test", "NewPlugin" })
    public void registerParentResouceTypePlugin() throws Exception {
        createPlugin("parent_resource_type-plugin.jar", "1.0", "parent_resource_type-plugin.xml");
        assertResourceTypeAssociationEquals("Server A First Level", "ParentResourceTypeTestPlugin",
            "childResourceTypes", asList("Server B Second Level", "Service A Second Level"));
        assertResourceTypeAssociationEquals("Server B Second Level", "ParentResourceTypeTestPlugin",
            "childResourceTypes", asList("Server C First Level", "Service B First Level"));
        assertResourceTypeAssociationEquals("Service A Second Level", "ParentResourceTypeTestPlugin",
            "childResourceTypes", asList("Service C First Level"));
    }

    @Test(dependsOnMethods = { "registerPlugin" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistNewTypes() {
        List<String> newTypes = asList("ServerA", "ServerB");
        assertTypesPersisted("Failed to persist new types", newTypes, PLUGIN_NAME);
    }

    //    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.resource.metadata.test", "NewPlugin"})
    //    public void persistSubcategories() throws Exception {
    //        assertResourceTypeAssociationEquals(
    //            "ServerA",
    //            PLUGIN_NAME,
    //            "childSubCategories",
    //            asList("Resources", "Applications")
    //        );
    //    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistMeasurementDefinitions() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "metricDefinitions",
            asList("metric1", "metric2", "rhq.availability"));
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistEventDefinitions() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "eventDefinitions",
            asList("logAEntry", "logBEntry"));
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistOperationDefinitions() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "operationDefinitions", asList("start", "stop"));
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistProcessScans() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "processScans", asList("serverA"));
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistDriftDefinitionTemplates() throws Exception {
        ResourceType type = assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "driftDefinitionTemplates",
            asList("drift-pc", "drift-fs"));

        DriftDefinition driftDef = null;
        Set<DriftDefinitionTemplate> drifts = type.getDriftDefinitionTemplates();
        for (DriftDefinitionTemplate drift : drifts) {
            if (drift.getName().equals("drift-pc")) {
                driftDef = new DriftDefinition(drift.getConfiguration());
                assertTrue(driftDef.isEnabled());
                assertEquals(BaseDirValueContext.pluginConfiguration, driftDef.getBasedir().getValueContext());
                assertEquals("connectionPropertyX", driftDef.getBasedir().getValueName());
                assertEquals(123456L, driftDef.getInterval());
                assertEquals(1, driftDef.getIncludes().size());
                assertEquals(2, driftDef.getExcludes().size());
                Filter filter = driftDef.getIncludes().get(0);
                assertEquals("foo/bar", filter.getPath());
                assertEquals("**/*.blech", filter.getPattern());
                filter = driftDef.getExcludes().get(0);
                assertEquals("/wot/gorilla", filter.getPath());
                assertEquals("*.xml", filter.getPattern());
                filter = driftDef.getExcludes().get(1);
                assertEquals("/hello", filter.getPath());
                assertEquals("", filter.getPattern());
            } else if (drift.getName().equals("drift-fs")) {
                driftDef = new DriftDefinition(drift.getConfiguration());
                assertTrue(driftDef.isEnabled());
                assertEquals(BaseDirValueContext.fileSystem, driftDef.getBasedir().getValueContext());
                assertEquals("/", driftDef.getBasedir().getValueName());
                assertEquals(DriftConfigurationDefinition.DEFAULT_INTERVAL, driftDef.getInterval());
                assertEquals(0, driftDef.getIncludes().size());
                assertEquals(0, driftDef.getExcludes().size());
            } else {
                fail("got an unexpected drift definition: " + driftDef);
            }
        }
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistBundleTargetConfigurations() throws Exception {
        String resourceTypeName = "ServerA";
        String plugin = PLUGIN_NAME;

        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName(resourceTypeName);
        criteria.addFilterPluginName(plugin);
        criteria.fetchBundleConfiguration(true);
        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        assertEquals("too many types returned!", 1, resourceTypes.size());
        ResourceType resourceType = resourceTypes.get(0);

        ResourceTypeBundleConfiguration rtbc = resourceType.getResourceTypeBundleConfiguration();
        assertNotNull("missing bundle configuration", rtbc);
        Set<BundleDestinationBaseDirectory> dirs = rtbc.getBundleDestinationBaseDirectories();
        assertEquals("Should have persisted 2 bundle dest dirs", 2, dirs.size());
        for (BundleDestinationBaseDirectory dir : dirs) {
            if (dir.getName().equals("bundleTarget-pc")) {
                assertEquals(Context.pluginConfiguration, dir.getValueContext());
                assertEquals("connectionPropertyY", dir.getValueName());
                assertEquals("pc-description", dir.getDescription());
            } else if (dir.getName().equals("bundleTarget-fs")) {
                assertEquals(Context.fileSystem, dir.getValueContext());
                assertEquals("/wot/gorilla", dir.getValueName());
                assertNull(dir.getDescription());
            } else {
                fail("got an unexpected bundle target dest dir: " + dir);
            }
        }
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistChildTypes() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "childResourceTypes", asList("Child1", "Child2"));
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistPluginConfigurationDefinition() throws Exception {
        assertAssociationExists("ServerA", "pluginConfigurationDefinition");
    }

    @Test(dependsOnMethods = { "persistNewTypes" }, groups = { "plugin.resource.metadata.test", "NewPlugin" })
    public void persistPackageTypes() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "packageTypes",
            asList("ServerA.Content.1", "ServerA.Content.2"));
    }

    @Test(groups = { "plugin.resource.metadata.test", "UpgradePlugin" }, dependsOnGroups = { "NewPlugin" })
    public void upgradePlugin() throws Exception {
        createPlugin("test-plugin.jar", "2.0", "plugin_v2.xml");
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradeOperationDefinitions() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "operationDefinitions",
            asList("start", "shutdown", "restart"));
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradeChildResources() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "childResourceTypes", asList("Child1", "Child3"));
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradeParentTypeOfChild() throws Exception {
        assertResourceTypeAssociationEquals("ServerB", PLUGIN_NAME, "childResourceTypes", asList("Child2"));
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradeEventDefinitions() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "eventDefinitions",
            asList("logAEntry", "logCEntry"));
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradeProcessScans() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "processScans", asList("processA", "processB"));
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradeDriftDefinitionTemplates() throws Exception {
        ResourceType type = assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "driftDefinitionTemplates",
            asList("drift-rc", "drift-mt"));

        DriftDefinition driftDef = null;
        Set<DriftDefinitionTemplate> drifts = type.getDriftDefinitionTemplates();
        for (DriftDefinitionTemplate drift : drifts) {
            if (drift.getName().equals("drift-rc")) {
                driftDef = new DriftDefinition(drift.getConfiguration());
                assertTrue(driftDef.isEnabled());
                assertEquals(BaseDirValueContext.resourceConfiguration, driftDef.getBasedir().getValueContext());
                assertEquals("resourceConfig1", driftDef.getBasedir().getValueName());
                assertEquals(DriftConfigurationDefinition.DEFAULT_INTERVAL, driftDef.getInterval());
                assertEquals(0, driftDef.getIncludes().size());
                assertEquals(0, driftDef.getExcludes().size());
            } else if (drift.getName().equals("drift-mt")) {
                driftDef = new DriftDefinition(drift.getConfiguration());
                assertTrue(driftDef.isEnabled());
                assertEquals(BaseDirValueContext.measurementTrait, driftDef.getBasedir().getValueContext());
                assertEquals("trait1", driftDef.getBasedir().getValueName());
                assertEquals(DriftConfigurationDefinition.DEFAULT_INTERVAL, driftDef.getInterval());
                assertEquals(0, driftDef.getIncludes().size());
                assertEquals(0, driftDef.getExcludes().size());
            } else {
                fail("got an unexpected drift definition: " + driftDef);
            }
        }
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradeBundleTargetConfigurations() throws Exception {
        String resourceTypeName = "ServerA";
        String plugin = PLUGIN_NAME;

        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName(resourceTypeName);
        criteria.addFilterPluginName(plugin);
        criteria.fetchBundleConfiguration(true);
        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        ResourceType resourceType = resourceTypes.get(0);

        ResourceTypeBundleConfiguration rtbc = resourceType.getResourceTypeBundleConfiguration();
        assertNotNull("missing bundle configuration", rtbc);
        Set<BundleDestinationBaseDirectory> dirs = rtbc.getBundleDestinationBaseDirectories();
        assertEquals("Should have persisted 2 bundle dest dirs", 2, dirs.size());
        for (BundleDestinationBaseDirectory dir : dirs) {
            if (dir.getName().equals("bundleTarget-rc")) {
                assertEquals(Context.resourceConfiguration, dir.getValueContext());
                assertEquals("resourceConfig1", dir.getValueName());
                assertEquals("rc-description", dir.getDescription());
            } else if (dir.getName().equals("bundleTarget-mt")) {
                assertEquals(Context.measurementTrait, dir.getValueContext());
                assertEquals("trait1", dir.getValueName());
                assertEquals("mt-description", dir.getDescription());
            } else {
                assertTrue("got an unexpected bundle target dest dir: " + dir, false);
            }
        }
    }

    @Test(dependsOnMethods = { "upgradePlugin" }, groups = { "plugin.resource.metadata.test", "UpgradePlugin" })
    public void upgradePackageTypes() throws Exception {
        assertResourceTypeAssociationEquals("ServerA", PLUGIN_NAME, "packageTypes",
            asList("ServerA.Content.1", "ServerA.Content.3"));
    }

    @Test(groups = { "RemoveTypes" }, dependsOnGroups = { "UpgradePlugin" })
    public void upgradePluginWithTypesRemoved() throws Exception {
        createPlugin("remove-types-plugin", "1.0", "remove_types_v1.xml");

        createBundle("test-bundle-1", "Test Bundle", "ServerC", "RemoveTypesPlugin");
        createPackage("ServerC::test-package", "ServerC", "RemoveTypesPlugin");
        createAlertTemplate("ServerC Alert Template", "ServerC", "RemoveTypesPlugin");

        // This sort of odd looking group stuff was trying to reproduce a reported issue. Didn't reproduce but
        // I'm leaving here as everything is worth doing for regression reasons
        List<Resource> resourcesServerE = createResources(3, "RemoveTypesPlugin", "ServerE", null);
        List<Resource> resourcesServiceE1 = createResources(2, "RemoveTypesPlugin", "ServiceE1",
            resourcesServerE.get(0));
        List<Resource> resourcesServiceE2 = createResources(2, "RemoveTypesPlugin", "ServiceE2",
            resourcesServiceE1.get(0));
        List<Resource> resourcesServiceE3 = createResources(2, "RemoveTypesPlugin", "ServiceE3",
            resourcesServiceE2.get(0));
        // Intentionally greater than 200 to test an issue with Criteria fetch defaults 
        List<Resource> resourcesServiceE4 = createResources(205, "RemoveTypesPlugin", "ServiceE4",
            resourcesServiceE3.get(0));

        ResourceGroup rgRecursive = createResourceGroup("ServerE Group", "ServerE", "RemoveTypesPlugin", true);
        addResourcesToGroup(rgRecursive, resourcesServerE);

        ResourceGroup rgFlat = createResourceGroup("ServiceE4 Group", "ServiceE4", "RemoveTypesPlugin", false);
        addResourcesToGroup(rgFlat, resourcesServiceE4);

        createPlugin("remove-types-plugin", "2.0", "remove_types_v2.xml");

        //Removal of this resource type exceeds default criteria page size.
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName("ServiceE4");
        criteria.addFilterPluginName("RemoveTypesPlugin");
        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        if ((resourceTypes != null) && (resourceTypes.size() > 0)) {
            //spinder 1-31-13: sleep for 30s to see if type removal has then completed
            //it's possible this could fail on smaller boxes. Not sure how to test this otherwise as
            //after fix to break resource deletion into chunks[BZ 905632] this should work.
            //tsegismo 4-12-13: sleep for 60s as some master builds already failed on Jenkins reporting:
            // "Resource type 'serviceE4' not fully removed"
            Thread.sleep(1000 * 60);
            resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), criteria);
            assertEquals("Resource type '" + resourceTypes.get(0).getName() + "' not fully removed", 0,
                resourceTypes.size());
        }
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteOperationDefsForRemovedType() throws Exception {
        OperationManagerLocal operationMgr = LookupUtil.getOperationManager();
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();

        OperationDefinitionCriteria criteria = new OperationDefinitionCriteria();
        criteria.addFilterResourceTypeName("ServerC");
        criteria.addFilterName("run");

        List<OperationDefinition> operationDefs = operationMgr.findOperationDefinitionsByCriteria(
            subjectMgr.getOverlord(), criteria);

        assertEquals("The operation definition should have been deleted", 0, operationDefs.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteEventDefsForRemovedType() throws Exception {
        List<?> results = getEntityManager()
            .createQuery("from EventDefinition e where e.name = :ename and e.resourceType.name = :rname")
            .setParameter("ename", "serverCEvent").setParameter("rname", "ServerC").getResultList();

        assertEquals("The event definition(s) should have been deleted", 0, results.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteParent() throws Exception {

        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName("ServerD.GrandChild1");
        criteria.addFilterPluginName("RemoveTypesPlugin");
        criteria.fetchParentResourceTypes(true);

        List<ResourceType> types = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), criteria);

        assertEquals("Expected to get back one resource type", 1, types.size());

        ResourceType type = types.get(0);

        assertEquals("Expected to find one parent type", 1, type.getParentResourceTypes().size());

        ResourceType parentType = findByName(type.getParentResourceTypes(), "ServerD");

        assertNotNull("Expected to find 'ServerD' as the parent, but found, " + type.getParentResourceTypes(),
            parentType);
    }

    private ResourceType findByName(Collection<ResourceType> types, String name) {
        for (ResourceType type : types) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteTypeAndAllItsDescendantTypes() throws Exception {

        List<?> typesNotRemoved = getEntityManager()
            .createQuery("from ResourceType t where t.plugin = :plugin and t.name in (:resourceTypes)")
            .setParameter("plugin", "RemoveTypesPlugin")
            // the types that should have been removed, if any show up we have a problem
            .setParameter(
                "resourceTypes",
                asList("ServerC", "ServiceC1", "ServiceE4", "ServerF", "ServiceF1", "ServiceF2", "ServiceF3",
                    "ServiceF4")).getResultList();

        assertEquals("Failed to delete resource type or one or more of its descendant types", 0, typesNotRemoved.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteProcessScans() {
        List<?> processScans = getEntityManager()
            .createQuery("from ProcessScan p where p.name = :name1 or p.name = :name2").setParameter("name1", "scan1")
            .setParameter("name2", "scan2").getResultList();

        assertEquals("The process scans should have been deleted", 0, processScans.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteSubcategories() {
        List<?> subcategories = getEntityManager()
            .createQuery("from ResourceSubCategory r where r.name = :name1 or r.name = :name2 or r.name = :name3")
            .setParameter("name1", "ServerC.Category1").setParameter("name2", "ServerC.Category2")
            .setParameter("name3", "ServerC.NestedCategory1").getResultList();
        assertEquals("The subcategories should have been deleted", 0, subcategories.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteResources() {
        ResourceManagerLocal resourceMgr = LookupUtil.getResourceManager();
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceTypeName("ServerC");
        criteria.addFilterPluginName("RemoveTypesPlugin");

        List<Resource> resources = resourceMgr.findResourcesByCriteria(subjectMgr.getOverlord(), criteria);

        assertTrue("Did not expect to find any more that three resources. Database might need to be reset",
            resources.size() < 4);

        // We won't do anything more rigorous that making sure the resources were marked uninventoried.
        // Resource deletion is an expensive, time-consuming process; consequently, it is carried out
        // asynchronously in a scheduled job. The call to initiate the resource deletion returns very
        // quickly as it is basically just updates the the inventory status to UNINVENTORIED for the
        // resources to be deleted.
        for (Resource resource : resources) {
            assertTrue("The resource should have been marked for deletion",
                InventoryStatus.UNINVENTORIED == resource.getInventoryStatus());
        }
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteBundles() {
        List<?> bundles = getEntityManager().createQuery("from Bundle b where b.bundleType.name = :name")
            .setParameter("name", "Test Bundle").getResultList();

        assertEquals("Failed to delete the bundles", 0, bundles.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteBundleTypes() {
        List<?> bundleTypes = getEntityManager().createQuery("from BundleType b where b.name = :name")
            .setParameter("name", "Test Bundle").getResultList();

        assertEquals("The bundle type should have been deleted", 0, bundleTypes.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deletePackages() {
        List<?> packages = getEntityManager().createQuery("from Package p where p.name = :name")
            .setParameter("name", "ServerC::test-package").getResultList();

        assertEquals("All packages should have been deleted", 0, packages.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deletePackageTypes() {
        List<?> packageTypes = getEntityManager().createQuery("from PackageType p where p.name = :name")
            .setParameter("name", "ServerC.Content").getResultList();

        assertEquals("All package types should have been deleted", 0, packageTypes.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteResourceGroups() {
        List<?> groups = getEntityManager()
            .createQuery("from ResourceGroup g where g.name = :name and g.resourceType.name = :typeName")
            .setParameter("name", "ServerC Group").setParameter("typeName", "ServerC").getResultList();

        assertEquals("All resource groups should have been deleted", 0, groups.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteAlertTemplates() {
        List<?> templates = getEntityManager()
            .createQuery("from AlertDefinition a where a.name = :name and a.resourceType.name = :typeName")
            .setParameter("name", "ServerC Alert Template").setParameter("typeName", "ServerC").getResultList();

        assertEquals("Alert templates should have been deleted.", 0, templates.size());
    }

    @Test(dependsOnMethods = { "upgradePluginWithTypesRemoved" }, groups = { "plugin.resource.metadata.test",
        "RemoveTypes" })
    public void deleteMeasurementDefinitions() {
        List<?> measurementDefs = getEntityManager().createQuery("from MeasurementDefinition m where m.name = :name")
            .setParameter("name", "ServerC::metric1").getResultList();

        assertEquals("Measurement definitions should have been deleted", 0, measurementDefs.size());
    }

    // this needs to be the last test executed in the class, it does cleanup
    @Test(priority = 1000, alwaysRun = true, dependsOnGroups = { "RemoveTypes" })
    public void afterClassWorkTest() throws Exception {
        afterClassWork();

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

        for(int id : groupIds) {
            groupManager.deleteResourceGroup(overlord, id);
        }
    }

    void assertTypesPersisted(String msg, List<String> types, String plugin) {
        List<String> typesNotFound = new ArrayList<String>();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        for (String type : types) {
            if (resourceTypeMgr.getResourceTypeByNameAndPlugin(type, plugin) == null) {
                typesNotFound.add(type);
            }
        }

        if (!typesNotFound.isEmpty()) {
            fail(msg + ": The following types were not found: " + typesNotFound);
        }
    }

    void assertAssociationExists(String resourceTypeName, String propertyName) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        String fetch = "fetch" + WordUtils.capitalize(propertyName);
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName(resourceTypeName);
        criteria.addFilterPluginName(PLUGIN_NAME);
        MethodUtils.invokeMethod(criteria, fetch, true);

        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        ResourceType resourceType = resourceTypes.get(0);

        Object property = PropertyUtils.getProperty(resourceType, propertyName);
        assertNotNull("Failed to find $propertyName for type '$resourceTypeName'", property);
    }

    List<Resource> createResources(int count, String pluginName, String resourceTypeName, Resource parent)
        throws Exception {
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);

        assertNotNull("Cannot create resources. Unable to find resource type for [name: " + resourceTypeName
            + ", plugin: " + pluginName + "]", resourceType);

        List<Resource> resources = new ArrayList<Resource>();
        for (int i = 0; i < count; ++i) {
            resources.add(new ResourceBuilder().createServer().withResourceType(resourceType)
                .withName(resourceType.getName() + "--" + i).withUuid(resourceType.getName())
                .withRandomResourceKey(resourceType.getName() + "--" + i).build());
        }

        getTransactionManager().begin();
        for (Resource resource : resources) {
            resource.setParentResource(parent);
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
            getEntityManager().persist(resource);
        }
        getTransactionManager().commit();

        return resources;
    }

    void createBundle(String bundleName, String bundleTypeName, String resourceTypeName, String pluginName)
        throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        BundleManagerLocal bundleMgr = LookupUtil.getBundleManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);

        assertNotNull("Cannot create bundle. Unable to find resource type for [name: " + resourceTypeName
            + ", plugin: " + pluginName + "]", resourceType);

        BundleType bundleType = bundleMgr.getBundleType(subjectMgr.getOverlord(), bundleTypeName);
        assertNotNull("Cannot create bundle. Unable to find bundle type for [name: " + bundleTypeName + "]", bundleType);
        Bundle bundle = bundleMgr.createBundle(subjectMgr.getOverlord(), bundleName, "test bundle: " + bundleName,
            bundleType.getId(), 0);

        assertNotNull("Failed create bundle for [name: " + bundleName + "]", bundle);
    }

    void createPackage(String packageName, String resourceTypeName, String pluginName) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ContentManagerLocal contentMgr = LookupUtil.getContentManager();

        List<PackageType> packageTypes = contentMgr.findPackageTypes(subjectMgr.getOverlord(), resourceTypeName,
            pluginName);
        Package pkg = new Package(packageName, packageTypes.get(0));

        contentMgr.persistPackage(pkg);
    }

    ResourceGroup createResourceGroup(String groupName, String resourceTypeName, String pluginName, boolean recursive)
        throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        ResourceGroupManagerLocal resourceGroupMgr = LookupUtil.getResourceGroupManager();

        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);

        assertNotNull("Cannot create resource group. Unable to find resource type for [name: " + resourceTypeName
            + ", plugin: " + pluginName + "]", resourceType);

        ResourceGroup resourceGroup = new ResourceGroup(groupName, resourceType);
        resourceGroup.setRecursive(recursive);
        ResourceGroup result = resourceGroupMgr.createResourceGroup(subjectMgr.getOverlord(), resourceGroup);
        groupIds.add(result.getId());
        return result;
    }

    void addResourcesToGroup(ResourceGroup rg, List<Resource> resources) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceGroupManagerLocal resourceGroupMgr = LookupUtil.getResourceGroupManager();
        int[] ids = new int[resources.size()];
        int i = 0;
        for (Resource r : resources) {
            ids[i++] = r.getId();
        }
        resourceGroupMgr.addResourcesToGroup(subjectMgr.getOverlord(), rg.getId(), ids);
    }

    void createAlertTemplate(String name, String resourceTypeName, String pluginName) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        AlertTemplateManagerLocal alertTemplateMgr = LookupUtil.getAlertTemplateManager();

        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);
        assertNotNull("Cannot create alert template. Unable to find resource type for [name: " + resourceTypeName
            + ", plugin: " + pluginName + "]", resourceType);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setName(name);
        alertDef.setPriority(AlertPriority.MEDIUM);
        alertDef.setResourceType(resourceType);
        alertDef.setConditionExpression(BooleanExpression.ALL);
        alertDef.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
        alertDef.setRecoveryId(0);

        alertTemplateMgr.createAlertTemplate(subjectMgr.getOverlord(), alertDef, resourceType.getId());
    }

}
