package org.rhq.enterprise.server.resource.metadata

import org.testng.annotations.Test

class ContentMetadataManagerBeanTest extends MetadataTest {

  @Test(groups = ['NewPlugin'])
  void registerPlugin() {
    def pluginDescriptor =
    """
    <plugin name="ContentMetadataManagerBeanTestPlugin"
            displayName="ContentMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ContentServer">
        <bundle type="ContentServer.Bundle.1"/>

        <content name="ContentServer.Content.1" category="deployable"/>
      </server>

      <server name="ContentServer1"/>

      <server name="ContentServer2">
        <content name="ContentServer2.Content.1" category="executableScript"/>
      </server>

      <server name="ContentServer3">
        <content name="ContentServer3.Content.1" category="deployable"/>
        <content name="ContentServer3.Content.2" category="deployable"/>
        <content name="ContentServer3.Content.3" category="deployable"/>
      </server>

      <server name="ContentServer4">
        <content name="ContentServer4.Content.1" category="deployable"/>
      </server>

      <server name="ContentServer5">
        <content name="ContentServer5.Content.1" category="deployable">
          <configuration>
            <c:simple-property name="x"/>
          </configuration>
        </content>
        <content name="ContentServer5.Content.2" category="deployable">
          <configuration>
            <c:simple-property name="x"/>
          </configuration>
        </content>
      </server>

      <server name="ContentServer6"/>
    </plugin>
    """

    createPlugin("content-test-plugin", "1.0", pluginDescriptor)
  }

  @Test(groups = ['UpgradePlugin'], dependsOnGroups = ['NewPlugin'])
  void upgradePlugin() {
    def pluginDescriptor =
    """
    <plugin name="ContentMetadataManagerBeanTestPlugin"
            displayName="ContentMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ContentServer">
        <bundle type="ContentServer.Bundle.2"/>

        <content name="ContentServer.Content.1" category="deployable">
          <configuration>
            <c:simple-property name="version"/>
           </configuration>
        </content>
      </server>

      <server name="ContentServer1">
        <content name="ContentServer1.Content.1" category="deployable"/>
        <content name="ContentServer1.Content.2" category="configuration"/>
      </server>

      <server name="ContentServer2"/>

      <server name="ContentServer3">
        <content name="ContentServer3.Content.2" category="deployable"/>
        <content name="ContentServer3.Content.3" category="deployable"/>
      </server>

      <server name="ContentServer4">
        <content name="ContentServer4.Content.1" category="deployable"/>
        <content name="ContentServer4.Content.2" category="deployable"/>
      </server>

      <server name="ContentServer5">
        <content name="ContentServer5.Content.1" category="deployable">
          <configuration>
            <c:simple-property name="x"/>
            <c:simple-property name="y"/>
          </configuration>
        </content>
        <content name="ContentServer5.Content.2" category="deployable"/>
      </server>

      <server name="ContentServer6">
        <bundle type="ContentServer6.Bundle.1"/>
      </server>
    </plugin>
    """

    createPlugin "content-test-plugin", "2.0", pluginDescriptor
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void addPackageTypes() {
    assertResourceTypeAssociationEquals(
        'ContentServer1',
        'ContentMetadataManagerBeanTestPlugin',
        'packageTypes',
        ['ContentServer1.Content.1', 'ContentServer1.Content.2']
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void deletePackageTypes() {
    assertResourceTypeAssociationEquals(
        'ContentServer2',
        'ContentMetadataManagerBeanTestPlugin',
        'packageTypes',
        []
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void deletePackageTypesThatExistsInOldResourceTypeButNotInNewResourceType() {
    assertResourceTypeAssociationEquals(
        'ContentServer3',
        'ContentMetadataManagerBeanTestPlugin',
        'packageTypes',
        ['ContentServer3.Content.2', 'ContentServer3.Content.3']
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void addPackageThatAreAddedInNewResourceType() {
    assertResourceTypeAssociationEquals(
        'ContentServer4',
        'ContentMetadataManagerBeanTestPlugin',
        'packageTypes',
        ['ContentServer4.Content.1', 'ContentServer4.Content.2']
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void addNewDeploymentConfigurationDefinition() {
    def packageType = loadPackageType('ContentServer', 'ContentMetadataManagerBeanTestPlugin',
        'ContentServer.Content.1')

    def deploymentConfigDef = packageType.deploymentConfigurationDefinition

    assertNotNull(
        "Failed to create new deployment configuration definition for package type that previously did not have one",
        deploymentConfigDef
    )
    assertEquals(
        "Expected to find 1 property definition in new deployment configuration definition",
        1,
        deploymentConfigDef.propertyDefinitions.size()
    )
    assertNotNull(
        "Expected to find 1 property definition, <version>, in new deployment configuration definition",
        deploymentConfigDef.get('version')
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void updateDeploymentConfigDefThatExistsInOldAndNewResourceType() {
    def packageType = loadPackageType('ContentServer5', 'ContentMetadataManagerBeanTestPlugin',
        'ContentServer5.Content.1')
    def deploymentConfigDef = packageType.deploymentConfigurationDefinition

    assertNotNull("Failed to update deployment configuration definition for package type", deploymentConfigDef)
    assertEquals(
        "Expected to find 2 property definitions in updated deployment configuration definition",
        2,
        deploymentConfigDef.propertyDefinitions.size()
    )
    assertNotNull(
        "Expected existing property definition to be retained across update",
        deploymentConfigDef.get('x')
    )
    assertNotNull(
        "Expected new property definition to be added during update",
        deploymentConfigDef.get('y')
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void deleteDeploymentConfigDefThatIsRemovedInNewResourceType() {
    def packageType = loadPackageType('ContentServer5', 'ContentMetadataManagerBeanTestPlugin',
        'ContentServer5.Content.2')

    assertNull(
        "Expected deployment configuration definition to be removed since it was removed from new resource type",
        packageType.deploymentConfigurationDefinition
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void updateBundleType() {
    def resourceType = loadResourceTypeWithBundleType('ContentServer', 'ContentMetadataManagerBeanTestPlugin')
    def bundleType = resourceType.bundleType

    assertNotNull "Failed to upgrade bundle type", bundleType
    assertEquals(
        "Failed to upgrade bundle type correctly. The bundle type name is wrong",
        'ContentServer.Bundle.2',
        bundleType.name
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void addBundleTypeThatOnlyExistsInNewResourceType() {
    def resourceType = loadResourceTypeWithBundleType('ContentServer6', 'ContentMetadataManagerBeanTestPlugin')
    def bundleType = resourceType.bundleType

    assertNotNull "Expected to find bundle type added during upgrade", bundleType
    assertEquals("Failed to correctly add bundle type during upgrade", 'ContentServer6.Bundle.1', bundleType.name)
  }

  def loadPackageType(String resourceType, String plugin, String packageType) {
    return entityManager.createQuery(
      """
      from  PackageType p left join fetch p.deploymentConfigurationDefinition
      where p.name = :packageType and
            p.resourceType.name = :resourceType and
            p.resourceType.plugin = :plugin
      """
    ).setParameter('packageType', packageType)
     .setParameter('plugin', plugin)
     .setParameter('resourceType', resourceType)
     .getSingleResult()
  }

  def loadResourceTypeWithBundleType(String resourceType, String plugin) {
    return entityManager.createQuery(
      """
      from  ResourceType t left join fetch t.bundleType
      where t.name = :resourceType and t.plugin = :plugin
      """
    ).setParameter('resourceType', resourceType)
     .setParameter('plugin', plugin)
     .getSingleResult()
  }

}
