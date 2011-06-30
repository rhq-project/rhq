package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.WordUtils;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.criteria.OperationDefinitionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
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

import static java.util.Arrays.asList;

public class ResourceMetadataManagerBeanTest extends MetadataBeanTest {

    @Test(groups = {"plugin.metadata", "NewPlugin"})
    public void registerPlugin() throws Exception {
        createPlugin("test-plugin.jar", "1.0", "plugin_v1.xml");
    }

    @Test(dependsOnMethods = {"registerPlugin"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistNewTypes() {
        List<String> newTypes = asList("ServerA", "ServerB");
        assertTypesPersisted("Failed to persist new types", newTypes, "TestPlugin");
    }

//    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
//    public void persistSubcategories() throws Exception {
//        assertResourceTypeAssociationEquals(
//            "ServerA",
//            "TestPlugin",
//            "childSubCategories",
//            asList("Resources", "Applications")
//        );
//    }

    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistMeasurementDefinitions() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "metricDefinitions",
            asList("metric1", "metric2")
        );
    }

    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistEventDefinitions() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "eventDefinitions",
            asList("logAEntry", "logBEntry")
        );
    }

    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistOperationDefinitions() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "operationDefinitions",
            asList("start", "stop")
        );
    }

    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistProcessScans() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "processScans",
            asList("serverA")
        );
    }

    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistDriftConfigurationTemplates() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "driftConfigurationTemplates",
            asList("drift1")
        );
    }

    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistChildTypes() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "childResourceTypes",
            asList("Child1", "Child2")
        );
    }


    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistPluginConfigurationDefinition() throws Exception {
        assertAssociationExists("ServerA", "pluginConfigurationDefinition");
    }

    @Test(dependsOnMethods = {"persistNewTypes"}, groups = {"plugin.metadata", "NewPlugin"})
    public void persistPackageTypes() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "packageTypes",
            asList("ServerA.Content.1", "ServerA.Content.2")
        );
    }

    @Test(groups = {"plugin.metadata", "UpgradePlugin"}, dependsOnGroups = {"NewPlugin"})
    public void upgradePlugin() throws Exception {
        createPlugin("test-plugin.jar", "2.0", "plugin_v2.xml");
    }

    @Test(dependsOnMethods = {"upgradePlugin"}, groups = {"plugin.metadata", "UpgradePlugin"})
    public void upgradeOperationDefinitions() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "operationDefinitions",
            asList("start", "shutdown", "restart")
        );
    }

    @Test(dependsOnMethods = {"upgradePlugin"}, groups = {"plugin.metadata", "UpgradePlugin"})
    public void upgradeChildResources() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "childResourceTypes",
            asList("Child1", "Child3")
        );
    }

    @Test(dependsOnMethods = {"upgradePlugin"}, groups = {"plugin.metadata", "UpgradePlugin"})
    public void upgradeParentTypeOfChild() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerB",
            "TestPlugin",
            "childResourceTypes",
            asList("Child2")
        );
    }

    @Test(dependsOnMethods = {"upgradePlugin"}, groups = {"plugin.metadata", "UpgradePlugin"})
    public void upgradeEventDefinitions() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "eventDefinitions",
            asList("logAEntry", "logCEntry")
        );
    }

    @Test(dependsOnMethods = {"upgradePlugin"}, groups = {"plugin.metadata", "UpgradePlugin"})
    public void upgradeProcessScans() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "processScans",
            asList("processA", "processB")
        );
    }

    @Test(dependsOnMethods = {"upgradePlugin"}, groups = {"plugin.metadata", "UpgradePlugin"})
    public void upgradePackageTypes() throws Exception {
        assertResourceTypeAssociationEquals(
            "ServerA",
            "TestPlugin",
            "packageTypes",
            asList("ServerA.Content.1", "ServerA.Content.3")
        );
    }

    @Test(groups = {"RemoveTypes"}, dependsOnGroups = {"UpgradePlugin"})
    public void upgradePluginWithTypesRemoved() throws Exception {
        createPlugin("remove-types-plugin", "1.0", "remove_types_v1.xml");

        createResources(3, "RemoveTypesPlugin", "ServerC");
        createBundle("test-bundle-1", "Test Bundle", "ServerC", "RemoveTypesPlugin");
        createPackage("ServerC::test-package", "ServerC", "RemoveTypesPlugin");
        createResourceGroup("ServerC Group", "ServerC", "RemoveTypesPlugin");
        createAlertTemplate("ServerC Alert Template", "ServerC", "RemoveTypesPlugin");

        createPlugin("remove-types-plugin", "2.0", "remove_types_v2.xml");
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
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

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteEventDefsForRemovedType() throws Exception {
        List results = getEntityManager().createQuery(
            "from EventDefinition e where e.name = :ename and e.resourceType.name = :rname")
            .setParameter("ename", "serverCEvent")
            .setParameter("rname", "ServerC")
            .getResultList();

        assertEquals("The event definition(s) should have been deleted", 0, results.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteParent() throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName("ServerD.GrandChild1");
        criteria.addFilterPluginName ("RemoveTypesPlugin");
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

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteTypeAndAllItsDescedantTypes() throws Exception {
        List typesNotRemoved = getEntityManager().createQuery(
            "from ResourceType t where t.plugin = :plugin and t.name in (:resourceTypes)")
            .setParameter("plugin", "RemoveTypesPlugin")
            .setParameter("resourceTypes", asList("ServerE", "ServerE1", "ServerE2", "ServerE3", "ServerE4"))
            .getResultList();

        assertEquals("Failed to delete resource type or one or more of its descendant types", 0, typesNotRemoved.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteProcessScans() {
        List processScans = getEntityManager().createQuery(
            "from ProcessScan p where p.name = :name1 or p.name = :name2")
            .setParameter("name1", "scan1")
            .setParameter("name2", "scan2")
            .getResultList();

        assertEquals("The process scans should have been deleted", 0, processScans.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteSubcategories() {
        List subcategories = getEntityManager().createQuery(
            "from ResourceSubCategory r where r.name = :name1 or r.name = :name2 or r.name = :name3")
            .setParameter("name1", "ServerC.Category1")
            .setParameter("name2", "ServerC.Category2")
            .setParameter("name3", "ServerC.NestedCategory1")
            .getResultList();
        assertEquals("The subcategories should have been deleted", 0, subcategories.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteResources() {
        ResourceManagerLocal resourceMgr = LookupUtil.getResourceManager();
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceTypeName("ServerC");
        criteria.addFilterPluginName("RemoveTypesPlugin");

        List<Resource> resources = resourceMgr.findResourcesByCriteria(subjectMgr.getOverlord(), criteria);

        assertTrue(
            "Did not expect to find any more that three resources. Database might need to be reset",
            resources.size() < 4
        );

        // We won't do anything more rigorous that making sure the resources were marked uninventoried.
        // Resource deletion is an expensive, time-consuming process; consequently, it is carried out
        // asynchronously in a scheduled job. The call to initiate the resource deletion returns very
        // quickly as it is basically just updates the the inventory status to UNINVENTORIED for the
        // resources to be deleted.
        for (Resource resource : resources) {
            assertEquals(
                "The resource should have been marked for deletion",
                InventoryStatus.UNINVENTORIED == resource.getInventoryStatus()
            );
        }
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteBundles() {
        List bundles = getEntityManager().createQuery("from Bundle b where b.bundleType.name = :name")
            .setParameter("name", "Test Bundle")
            .getResultList();

        assertEquals("Failed to delete the bundles", 0, bundles.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteBundleTypes() {
        List bundleTypes = getEntityManager().createQuery("from BundleType b where b.name = :name")
            .setParameter("name", "Test Bundle")
            .getResultList();

        assertEquals("The bundle type should have been deleted", 0, bundleTypes.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deletePackages() {
        List packages = getEntityManager().createQuery("from Package p where p.name = :name")
            .setParameter("name", "ServerC::test-package")
            .getResultList();

        assertEquals("All packages should have been deleted", 0, packages.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deletePackageTypes() {
        List packageTypes = getEntityManager().createQuery("from PackageType p where p.name = :name")
            .setParameter("name", "ServerC.Content")
            .getResultList();

        assertEquals("All package types should have been deleted", 0, packageTypes.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteResourceGroups() {
        List groups = getEntityManager().createQuery(
            "from ResourceGroup g where g.name = :name and g.resourceType.name = :typeName")
            .setParameter("name", "ServerC Group")
            .setParameter("typeName", "ServerC")
            .getResultList();

        assertEquals("All resource groups should have been deleted", 0, groups.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteAlertTemplates() {
        List templates = getEntityManager().createQuery(
            "from AlertDefinition a where a.name = :name and a.resourceType.name = :typeName")
            .setParameter("name", "ServerC Alert Template")
            .setParameter("typeName", "ServerC")
            .getResultList();

        assertEquals("Alert templates should have been deleted.", 0, templates.size());
    }

    @Test(dependsOnMethods = {"upgradePluginWithTypesRemoved"}, groups = {"plugin.metadata", "RemoveTypes"})
    public void deleteMeasurementDefinitions() {
        List measurementDefs = getEntityManager().createQuery("from MeasurementDefinition m where m.name = :name")
            .setParameter("name", "ServerC::metric1")
            .getResultList();

        assertEquals("Measurement definitions should have been deleted", 0, measurementDefs.size());
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
        criteria.addFilterPluginName("TestPlugin");
        MethodUtils.invokeMethod(criteria, fetch, true);

        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        ResourceType resourceType = resourceTypes.get(0);

        Object property = PropertyUtils.getProperty(resourceType, propertyName);
        assertNotNull("Failed to find $propertyName for type '$resourceTypeName'", property);
    }

    void createResources(int count, String pluginName, String resourceTypeName) throws Exception {
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);

        assertNotNull("Cannot create resources. Unable to find resource type for [name: " + resourceTypeName +
            ", plugin: " + pluginName + "]", resourceType);

        List<Resource> resources = new ArrayList<Resource>();
        for (int i = 0; i < count; ++i) {
            resources.add(new ResourceBuilder()
                .createServer()
                .withResourceType(resourceType)
                .withName(resourceType.getName() + "--" + i)
                .withUuid(resourceType.getName())
                .withRandomResourceKey(resourceType.getName() + "--" + i)
                .build());
        }

        getTransactionManager().begin();
        for (Resource resource : resources) {
            getEntityManager().persist(resource);
        }
        getTransactionManager().commit();
    }

    void createBundle(String bundleName, String bundleTypeName, String resourceTypeName, String pluginName)
        throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        BundleManagerLocal bundleMgr = LookupUtil.getBundleManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);

        assertNotNull("Cannot create bundle. Unable to find resource type for [name: " + resourceTypeName +
            ", plugin: " + pluginName + "]", resourceType);

        BundleType bundleType = bundleMgr.getBundleType(subjectMgr.getOverlord(), bundleTypeName);
        assertNotNull("Cannot create bundle. Unable to find bundle type for [name: " + bundleTypeName + "]", bundleType);
        Bundle bundle = bundleMgr.createBundle(subjectMgr.getOverlord(), bundleName, "test bundle: " + bundleName,
            bundleType.getId());

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

    void createResourceGroup(String groupName, String resourceTypeName, String pluginName) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        ResourceGroupManagerLocal resourceGroupMgr = LookupUtil.getResourceGroupManager();

        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);

        assertNotNull("Cannot create resource group. Unable to find resource type for [name: " + resourceTypeName +
            ", plugin: " + pluginName + "]", resourceType);

        ResourceGroup resourceGroup = new ResourceGroup(groupName, resourceType);
        resourceGroupMgr.createResourceGroup(subjectMgr.getOverlord(), resourceGroup);
    }

    void createAlertTemplate(String name, String resourceTypeName, String pluginName) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        AlertTemplateManagerLocal alertTemplateMgr = LookupUtil.getAlertTemplateManager();

        ResourceType resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName);
        assertNotNull("Cannot create alert template. Unable to find resource type for [name: " + resourceTypeName +
            ", plugin: " + pluginName + "]", resourceType);

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
