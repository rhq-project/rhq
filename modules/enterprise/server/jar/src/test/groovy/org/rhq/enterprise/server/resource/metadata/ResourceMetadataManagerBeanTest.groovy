package org.rhq.enterprise.server.resource.metadata

import org.rhq.core.domain.alert.AlertDampening
import org.rhq.core.domain.alert.AlertDefinition
import org.rhq.core.domain.alert.AlertPriority
import org.rhq.core.domain.alert.BooleanExpression
import org.rhq.core.domain.content.Package
import org.rhq.core.domain.criteria.OperationDefinitionCriteria
import org.rhq.core.domain.criteria.ResourceCriteria
import org.rhq.core.domain.criteria.ResourceTypeCriteria
import org.rhq.core.domain.resource.InventoryStatus
import org.rhq.core.domain.resource.ResourceType
import org.rhq.core.domain.resource.group.ResourceGroup
import org.rhq.core.domain.shared.ResourceBuilder
import org.rhq.enterprise.server.util.LookupUtil
import org.rhq.test.AssertUtils
import org.testng.annotations.Test

class ResourceMetadataManagerBeanTest extends MetadataTest {

  @Test(groups = ['NewPlugin'])
  void registerPlugin() {
    def pluginDescriptor =
    """
    <plugin name="TestPlugin" displayName="Test Plugin" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ServerA" description="Server A description">
        <subcategories>
          <subcategory name="Resources" description="Resources subcategory"/>
          <subcategory name="Applications" description="Applications subcategory"/>
        </subcategories>

        <plugin-configuration>
          <c:simple-property name="connectionPropertyX" default="x"/>
          <c:simple-property name="connectionPropertyY" default="y"/>
        </plugin-configuration>

        <process-scan name="serverA" query="process|basename|match=^java.*,arg|org.jboss.Main|match=.*"/>

        <operation name="start">
          <parameters>
            <c:simple-property name="immediate" type="boolean"/>
          </parameters>
          <results>
            <c:simple-property name="exitCode" type="integer"/>
          </results>
        </operation>

        <operation name="stop">
          <parameters>
            <c:simple-property name="immediate" type="boolean"/>
          </parameters>
          <results>
            <c:simple-property name="exitCode" type="integer"/>
          </results>
        </operation>

        <metric displayName="Metric 1" property="metric1" displayType="summary" defaultInterval="300000"/>
        <metric displayName="Metric 2" property="metric2" displayType="summary" defaultInterval="300000"/>

        <event name="logAEntry" description="an entry was appended to a log file"/>
        <event name="logBEntry" description="an entry was appended to a log file"/>

        <content name="ServerA.Content.1" category="deployable">
          <configuration>
            <c:simple-property name="ServerA.Content.1.version"/>
           </configuration>
        </content>

        <content name="ServerA.Content.2" category="deployable">
          <configuration>
            <c:simple-property name="ServerA.Content.2.version"/>
           </configuration>
        </content>

        <service name="Child1" description="Child 1 description"/>
        <service name="Child2" description="Child 2 description"/>
      </server>

      <server name="ServerB" description="Server B description"/>
    </plugin>
    """

    createPlugin("test-plugin", "1.0", pluginDescriptor)
  }

  @Test(dependsOnMethods = ['registerPlugin'], groups = ['NewPlugin'])
  void persistNewTypes() {
    def newTypes = ['ServerA', 'ServerB']
    assertTypesPersisted "Failed to persist new types", newTypes, 'TestPlugin'
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistSubcategories() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'subCategories',
        ['Resources', 'Applications']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistMeasurementDefinitions() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'metricDefinitions',
        ['metric1', 'metric2']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistEventDefinitions() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'eventDefinitions',
        ['logAEntry', 'logBEntry']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistOperationDefinitions() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'operationDefinitions',
        ['start', 'stop']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistProcessScans() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'processScans',
        ['serverA']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistChildTypes() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'childResourceTypes',
        ['Child1', 'Child2']
    )

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistPluginConfigurationDefinition
    assertAssociationExists('ServerA', 'pluginConfigurationDefinition')
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistPackageTypes() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'packageTypes',
        ['ServerA.Content.1', 'ServerA.Content.2']
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnGroups = ['NewPlugin'])
  void upgradePlugin() {
    def pluginDescriptor =
    """
    <plugin name="TestPlugin" displayName="Test Plugin" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ServerA" description="Server A description">

        <subcategories>
          <subcategory name="Resources" description="Resources subcategory"/>
          <subcategory name="Applications" description="Applications subcategory"/>
        </subcategories>

        <plugin-configuration>
          <c:simple-property name="connectionPropertyX" default="x"/>
          <c:simple-property name="connectionPropertyY" default="y"/>
        </plugin-configuration>

        <process-scan name="processA" query="process|basename|match=^java.*,arg|org.jboss.MainA|match=.*"/>
        <process-scan name="processB" query="process|basename|match=^java.*,arg|org.jboss.MainB|match=.*"/>

        <operation name="start">
          <parameters>
            <c:simple-property name="immediate" type="boolean"/>
          </parameters>
          <results>
            <c:simple-property name="exitCode" type="integer"/>
          </results>
        </operation>

        <operation name="shutdown">
          <parameters>
            <c:simple-property name="immediate" type="boolean"/>
          </parameters>
          <results>
            <c:simple-property name="exitCode" type="integer"/>
          </results>
        </operation>

        <operation name="restart">
          <parameters>
            <c:simple-property name="immediate" type="boolean"/>
          </parameters>
          <results>
            <c:simple-property name="exitCode" type="integer"/>
          </results>
        </operation>

        <metric displayName="Metric 1" property="metric1" displayType="summary" defaultInterval="300000"/>
        <metric displayName="Metric 2" property="metric2" displayType="summary" defaultInterval="300000"/>

        <event name="logAEntry" description="an entry was appended to a log file"/>
        <event name="logCEntry" description="an entry was appended to a log file"/>

        <content name="ServerA.Content.1" category="configuration">
          <configuration>
            <c:simple-property name="ServerA.Content.1.property1"/>
            <c:simple-property name="ServerA.Content.1.property2"/>
           </configuration>
        </content>

        <content name="ServerA.Content.3" category="deployable">
          <configuration>
            <c:simple-property name="ServerA.Content.3.version"/>
           </configuration>
        </content>

        <service name="Child1"/>
        <service name="Child3"/>
      </server>

      <server name="ServerB" description="Server B description">
        <service name="Child2"/>
      </server>
    </plugin>
    """

    createPlugin("test-plugin", "2.0", pluginDescriptor)
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeOperationDefinitions() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'operationDefinitions',
        ['start', 'shutdown', 'restart']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeChildResources() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'childResourceTypes',
        ['Child1', 'Child3']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeParentTypeOfChild() {
    assertResourceTypeAssociationEquals(
        'ServerB',
        'TestPlugin',
        'childResourceTypes',
        ['Child2']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeEventDefinitions() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'eventDefinitions',
        ['logAEntry', 'logCEntry']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeProcessScans() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'processScans',
        ['processA', 'processB']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradePackageTypes() {
    assertResourceTypeAssociationEquals(
        'ServerA',
        'TestPlugin',
        'packageTypes',
        ['ServerA.Content.1', 'ServerA.Content.3']
    )
  }

  @Test(groups = ['RemoveTypes'], dependsOnGroups = ['UpgradePlugin'])
  void upgradePluginWithTypesRemoved() {
    def originalDescriptor = """
    <plugin name="RemoveTypesPlugin" displayName="Remove Types Plugin" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ServerC" description="Server C description">

        <subcategories>
          <subcategory name="ServerC.Category1">
            <subcategory name="ServerC.NestedCategory1"/>
          </subcategory>
          <subcategory name="ServerC.Category2"/>
        </subcategories>

        <bundle type="Test Bundle"/>

        <process-scan name="scan1" query="process|basename|match=^java.*,arg|org.rhq.serverC1|match=.*"/>
        <process-scan name="scan2" query="process|basename|match=^java.*,arg|org.rhq.serverC2|match=.*"/>

        <operation name="run">
          <parameters>
            <c:simple-property name="script"/>
          </parameters>
          <results>
            <c:simple-property name="errors"/>
          </results>
        </operation>

        <metric displayName="Metric 1" property="ServerC::metric1" displayType="summary" defaultInterval="300000"/>

        <event name="serverCEvent" description="an entry was appended to a log file"/>

        <content name="ServerC.Content" category="deployable">
          <configuration>
            <c:simple-property name="ServerC.Content.version"/>                
           </configuration>
        </content>
      </server>

      <server name="ServerD">
        <service name="ServerD.Child1">
          <service name="ServerD.GrandChild1"/>
        </service>
      </server>
    </plugin>
    """

    createPlugin 'remove-types-plugin', '1.0', originalDescriptor

    createResources(3, 'RemoveTypesPlugin', 'ServerC')
    createBundle("test-bundle-1", "Test Bundle", "ServerC", "RemoveTypesPlugin")
    createPackage('ServerC::test-package', 'ServerC', 'RemoveTypesPlugin')
    createResourceGroup('ServerC Group', 'ServerC', 'RemoveTypesPlugin')
    createAlertTemplate('ServerC Alert Template', 'ServerC', 'RemoveTypesPlugin')

    def updatedDescriptor = """
    <plugin name="RemoveTypesPlugin" displayName="Remove Types Plugin" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ServerD">
        <service name="ServerD.GrandChild1"/>
      </server>
    </plugin>
    """

    createPlugin 'remove-types-plugin', '2.0', updatedDescriptor
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteOperationDefsForRemovedType() {
    def operationMgr = LookupUtil.operationManager
    def subjectMgr = LookupUtil.subjectManager

    def criteria = new OperationDefinitionCriteria()
    criteria.addFilterResourceTypeName 'ServerC'
    criteria.addFilterName 'run'

    def operationDefs = operationMgr.findOperationDefinitionsByCriteria(subjectMgr.overlord, criteria)

    assertEquals "The operation definition should have been deleted", 0, operationDefs.size()
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteEventDefsForRemovedType() {
    def results = entityManager.createQuery(
        "from EventDefinition e where e.name = :ename and e.resourceType.name = :rname")
        .setParameter("ename", "serverCEvent")
        .setParameter("rname", "ServerC")
        .getResultList()

    assertEquals "The event definition(s) should have been deleted", 0, results.size()
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteParent() {
    def subjectMgr = LookupUtil.subjectManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager

    def criteria = new ResourceTypeCriteria()
    criteria.addFilterName "ServerD.GrandChild1"
    criteria.addFilterPluginName "RemoveTypesPlugin"
    criteria.fetchParentResourceTypes true

    def types = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.overlord, criteria)

    assertEquals "Expected to get back one resource type", 1, types.size()

    def type = types[0]

    assertEquals "Expected to find one parent type", 1, type.parentResourceTypes.size()

    def parentType = type.parentResourceTypes.find { it.name == "ServerD" }

    assertNotNull "Expected to find 'ServerD' as the parent, but found, $type.parentResourceTypes", parentType
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteProcessScans() {
    def processScans = entityManager.createQuery("from ProcessScan p where p.name = :name1 or p.name = :name2")
        .setParameter("name1", "scan1")
        .setParameter("name2", "scan2")
        .getResultList()

    assertEquals "The process scans should have been deleted", 0, processScans.size()
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteSubcategories() {
    def subcategories = entityManager.createQuery("""
    from ResourceSubCategory r
    where r.name = :name1 or r.name = :name2 or r.name = :name3""")
        .setParameter("name1", "ServerC.Category1")
        .setParameter("name2", "ServerC.Category2")
        .setParameter("name3", "ServerC.NestedCategory1")
        .getResultList()

    assertEquals "The subcategories should have been deleted", 0, subcategories.size()
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteResources() {
    def resourceMgr = LookupUtil.resourceManager
    def subjectMgr = LookupUtil.subjectManager

    def criteria = new ResourceCriteria()
    criteria.addFilterResourceTypeName 'ServerC'
    criteria.addFilterPluginName 'RemoveTypesPlugin'

    def resources = resourceMgr.findResourcesByCriteria(subjectMgr.overlord, criteria)

    assertTrue(
        "Did not expect to find any more that three resources. Database might need to be reset",
        resources.size() < 4
    )

    // We won't do anything more rigorous that making sure the resources were marked uninventoried.
    // Resource deletion is an expensive, time-consuming process; consequently, it is carried out
    // asynchronously in a scheduled job. The call to initiate the resource deletion returns very
    // quickly as it is basically just updates the the inventory status to UNINVENTORIED for the
    // resources to be deleted.
    resources.each {
      assertEquals(
          "The resource should have been marked for deletion",
          InventoryStatus.UNINVENTORIED == it.inventoryStatus
      ) 
    }
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteBundles() {
    def bundles = entityManager.createQuery("from Bundle b where b.bundleType.name = :name")
        .setParameter("name", "Test Bundle")
        .getResultList()

    assertEquals("Failed to delete the bundles", 0, bundles.size())
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteBundleTypes() {
    def bundleTypes = entityManager.createQuery("from BundleType b where b.name = :name")
        .setParameter("name", "Test Bundle")
        .getResultList()

    assertEquals("The bundle type should have been deleted", 0, bundleTypes.size())    
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deletePackages() {
    def packages = entityManager.createQuery("from Package p where p.name = :name")
        .setParameter("name", "ServerC::test-package")
        .getResultList()

    assertEquals "All packages should have been deleted", 0, packages.size()
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deletePackageTypes() {
    def packageTypes = entityManager.createQuery("from PackageType p where p.name = :name")
        .setParameter("name", "ServerC.Content")
        .getResultList()

    assertEquals "All package types should have been deleted", 0, packageTypes.size()    
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteResourceGroups() {
    def groups = entityManager.createQuery("from ResourceGroup g where g.name = :name and g.resourceType.name = :typeName")
        .setParameter("name", "ServerC Group")
        .setParameter("typeName", "ServerC")
        .getResultList()

    assertEquals "All resource groups should have been deleted", 0, groups.size()
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteAlertTemplates() {
    def templates = entityManager.createQuery("from AlertDefinition a where a.name = :name and a.resourceType.name = :typeName")
        .setParameter("name", "ServerC Alert Template")
        .setParameter("typeName", "ServerC")
        .getResultList()

    assertEquals "Alert templates should have been deleted.", 0, templates.size()
  }

  @Test(dependsOnMethods = ['upgradePluginWithTypesRemoved'], groups = ['RemoveTypes'])
  void deleteMeasurementDefinitions() {
    def measurementDefs = entityManager.createQuery("from MeasurementDefinition m where m.name = :name")
        .setParameter("name", "ServerC::metric1")
        .getResultList()

    assertEquals "Measurement definitions should have been deleted", 0, measurementDefs.size()
  }


  def createResources(Integer count, String pluginName, String resourceTypeName) {
    def resourceTypeMgr = LookupUtil.resourceTypeManager
    def resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName)

    assertNotNull(
        "Cannot create resources. Unable to find resource type for [name: $resourceTypeName, plugin: $pluginName]",
        resourceType
    )

    def resources = []
    count.times {
      resources << new ResourceBuilder()
        .createServer()
        .withResourceType(resourceType)
        .withName("${resourceType.name}-$it")
        .withUuid("$resourceType.name:")
        .withRandomResourceKey("${resourceType.name}-$it")
        .build()
    }

    transaction {
      resources.each { resource -> entityManager.persist(resource) }
    }
  }

  def createBundle(bundleName, bundleTypeName, resourceTypeName, pluginName) {
    def subjectMgr = LookupUtil.subjectManager
    def bundleMgr = LookupUtil.bundleManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager
    def resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName)

    assertNotNull(
        "Cannot create bundle. Unable to find resource type for [name: $resourceTypeName, plugin: $pluginName]",
        resourceType
    )

    def bundleType = bundleMgr.getBundleType(subjectMgr.overlord, bundleTypeName)

    assertNotNull("Cannot create bundle. Unable to find bundle type for [name: $bundleTypeName]")

    def bundle = bundleMgr.createBundle(subjectMgr.overlord, bundleName, "test bundle: $bundleName", bundleType.id)

    assertNotNull("Failed create bundle for [name: $bundleName]", bundle)

    return bundle
  }

  def createPackage(packageName, resourceTypeName, pluginName) {
    def subjectMgr = LookupUtil.subjectManager
    def contentMgr = LookupUtil.contentManager

    def packageTypes = contentMgr.findPackageTypes(subjectMgr.overlord, resourceTypeName, pluginName)
    def pkg = new Package(packageName, packageTypes[0])

    contentMgr.persistPackage(pkg)
  }

  def createResourceGroup(groupName, resourceTypeName, pluginName) {
    def subjectMgr = LookupUtil.subjectManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager
    def resourceGroupMgr = LookupUtil.resourceGroupManager

    def resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName)

    assertNotNull(
        "Cannot create resource group. Unable to find resource type for [name: $resourceTypeName, plugin: $pluginName]",
        resourceType
    )

    def resourceGroup = new ResourceGroup(groupName, resourceType)
    resourceGroupMgr.createResourceGroup(subjectMgr.overlord, resourceGroup)
  }

  def createAlertTemplate(name, resourceTypeName, pluginName) {
    def subjectMgr = LookupUtil.subjectManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager
    def alertTemplateMgr = LookupUtil.alertTemplateManager

    def resourceType = resourceTypeMgr.getResourceTypeByNameAndPlugin(resourceTypeName, pluginName)
    assertNotNull(
        "Cannot create alert template. Unable to find resource type for [name: $resourceTypeName, plugin: $pluginName]",
        resourceType
    )

    def alertDef = new AlertDefinition()
    alertDef.name = name
    alertDef.priority = AlertPriority.MEDIUM
    alertDef.resourceType = resourceType
    alertDef.conditionExpression = BooleanExpression.ALL
    alertDef.alertDampening = new AlertDampening(AlertDampening.Category.NONE)
    alertDef.recoveryId = 0

    alertTemplateMgr.createAlertTemplate(subjectMgr.overlord, alertDef, resourceType.id)
  }

  void assertTypesPersisted(msg, types, plugin) {
    def typesNotFound = []
    def resourceTypeMgr = LookupUtil.resourceTypeManager

    types.each {
      if (resourceTypeMgr.getResourceTypeByNameAndPlugin(it, plugin) == null) {
        typesNotFound << it
      }
    }

    if (typesNotFound.size() > 0) {
      fail "$msg: The following types were not found: $typesNotFound"
    }
  }

  void assertAssociationExists(String resourceTypeName, String propertyName) {
    def subjectMgr = LookupUtil.subjectManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager

    def fetch = "fetch${propertyName.capitalize()}"
    def criteria = new ResourceTypeCriteria()
    criteria.addFilterName resourceTypeName
    criteria.addFilterPluginName 'TestPlugin'
    criteria."$fetch" true

    def resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.overlord, criteria)
    def resourceType = resourceTypes[0]

    assertNotNull("Failed to find $propertyName for type '$resourceTypeName'", resourceType[propertyName])
  }

  void assertResourceTypeEquals(String msg, ResourceType expected, ResourceType actual) {
    AssertUtils.assertPropertiesMatch(msg, expected, actual, "childResourceTypes", "parentResourceTypes",
        "pluginConfigurationDefinition", "resourceConfigurationDefinition", "subCategory", "metricDefinitions",
        "eventDefinitions", "operationDefinitions", "processScans", "packageTypes", "resources", "resourceGroups",
        "productVersions", "bundleType", "childSubCategories", "helpText", "classLoaderType")
  }

}
