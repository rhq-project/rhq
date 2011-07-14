package org.rhq.enterprise.server.resource.metadata;

import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.Test;

import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.ResourceType;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;

public class ContentMetadataManagerBeanTest extends MetadataBeanTest {

    @Test(groups = {"plugin.metadata", "Content.NewPlugin"})
    public void registerContentPlugin() throws Exception {
        createPlugin("content-test-plugin", "1.0", "plugin_v1.xml");
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnGroups = {"Content.NewPlugin"})
    public void upgradeContentPlugin() throws Exception {
        createPlugin("content-test-plugin", "2.0", "plugin_v2.xml");
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void addPackageTypes() throws Exception {
        assertResourceTypeAssociationEquals(
            "ContentServer1",
            "ContentMetadataManagerBeanTestPlugin",
            "packageTypes",
            asList("ContentServer1.Content.1", "ContentServer1.Content.2")
        );
  }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void deletePackageTypes() throws Exception {
        assertResourceTypeAssociationEquals(
            "ContentServer2",
            "ContentMetadataManagerBeanTestPlugin",
            "packageTypes",
            EMPTY_LIST
        );
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void deletePackageTypesThatExistsInOldResourceTypeButNotInNewResourceType() throws Exception {
        assertResourceTypeAssociationEquals(
            "ContentServer3",
            "ContentMetadataManagerBeanTestPlugin",
            "packageTypes",
            asList("ContentServer3.Content.2", "ContentServer3.Content.3")
        );
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void addPackageThatAreAddedInNewResourceType() throws Exception {
        assertResourceTypeAssociationEquals(
            "ContentServer4",
            "ContentMetadataManagerBeanTestPlugin",
            "packageTypes",
            asList("ContentServer4.Content.1", "ContentServer4.Content.2")
        );
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void addNewDeploymentConfigurationDefinition() throws Exception {
        PackageType packageType = loadPackageType("ContentServer", "ContentMetadataManagerBeanTestPlugin",
            "ContentServer.Content.1");

        ConfigurationDefinition deploymentConfigDef = packageType.getDeploymentConfigurationDefinition();

        assertNotNull(
            "Failed to create new deployment configuration definition for package type that previously did not have one",
            deploymentConfigDef
        );
        assertEquals(
            "Expected to find 1 property definition in new deployment configuration definition",
            1,
            deploymentConfigDef.getPropertyDefinitions().size()
        );
        assertNotNull(
            "Expected to find 1 property definition, <version>, in new deployment configuration definition",
            deploymentConfigDef.get("version")
        );
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void updateDeploymentConfigDefThatExistsInOldAndNewResourceType() throws Exception {
        PackageType packageType = loadPackageType("ContentServer5", "ContentMetadataManagerBeanTestPlugin",
            "ContentServer5.Content.1");
        ConfigurationDefinition deploymentConfigDef = packageType.getDeploymentConfigurationDefinition();

        assertNotNull("Failed to update deployment configuration definition for package type", deploymentConfigDef);
        assertEquals(
            "Expected to find 2 property definitions in updated deployment configuration definition",
            2,
            deploymentConfigDef.getPropertyDefinitions().size()
        );
        assertNotNull(
            "Expected existing property definition to be retained across update",
            deploymentConfigDef.get("x")
        );
        assertNotNull(
            "Expected new property definition to be added during update",
            deploymentConfigDef.get("y")
        );
  }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void deleteDeploymentConfigDefThatIsRemovedInNewResourceType() {
        PackageType packageType = loadPackageType("ContentServer5", "ContentMetadataManagerBeanTestPlugin",
            "ContentServer5.Content.2");

        assertNull(
            "Expected deployment configuration definition to be removed since it was removed from new resource type",
            packageType.getDeploymentConfigurationDefinition()
        );
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void updateBundleType() {
        ResourceType resourceType = loadResourceTypeWithBundleType("ContentServer7",
            "ContentMetadataManagerBeanTestPlugin");
        BundleType bundleType = resourceType.getBundleType();

        assertNotNull("Failed to upgrade bundle type", bundleType);
        assertEquals(
            "Failed to upgrade bundle type correctly. The bundle type name is wrong",
            "ContentServer.Bundle.2",
            bundleType.getName()
        );
    }

    @Test(groups = {"plugin.metadata", "Content.UpgradePlugin"}, dependsOnMethods = {"upgradeContentPlugin"})
    public void addBundleTypeThatOnlyExistsInNewResourceType() {
        ResourceType resourceType = loadResourceTypeWithBundleType("ContentServer6",
            "ContentMetadataManagerBeanTestPlugin");
        BundleType bundleType = resourceType.getBundleType();

        assertNotNull("Expected to find bundle type added during upgrade", bundleType);
        assertEquals("Failed to correctly add bundle type during upgrade", "ContentServer6.Bundle.1",
            bundleType.getName());
    }

    PackageType loadPackageType(String resourceType, String plugin, String packageType) {
        return (PackageType) getEntityManager().createQuery(
            "from PackageType p left join fetch p.deploymentConfigurationDefinition " +
                "where p.name = :packageType and " +
                "p.resourceType.name = :resourceType and " +
                "p.resourceType.plugin = :plugin")
            .setParameter("packageType", packageType)
            .setParameter("plugin", plugin)
            .setParameter("resourceType", resourceType)
            .getSingleResult();
    }

    ResourceType loadResourceTypeWithBundleType(String resourceType, String plugin) {
        return (ResourceType) getEntityManager().createQuery(
            "from  ResourceType t left join fetch t.bundleType where t.name = :resourceType and t.plugin = :plugin")
            .setParameter("resourceType", resourceType)
            .setParameter("plugin", plugin)
            .getSingleResult();
     }

}
