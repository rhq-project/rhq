package org.rhq.enterprise.server.resource.metadata

import org.testng.annotations.Test
import org.testng.Assert

import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.toPluginDescriptor
import org.rhq.core.domain.plugin.Plugin
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor
import org.apache.maven.artifact.versioning.ComparableVersion
import org.rhq.core.util.MessageDigestGenerator
import org.rhq.enterprise.server.test.AbstractEJB3Test
import org.rhq.enterprise.server.util.LookupUtil
import org.rhq.core.domain.resource.ResourceType
import org.rhq.test.AssertUtils
import org.rhq.core.domain.criteria.ResourceTypeCriteria
import org.rhq.core.domain.criteria.OperationDefinitionCriteria

class ResourceMetadataManagerBeanTest extends AbstractEJB3Test {

  @Test(groups = ['NewPlugin'])
  void registerPlugin() {
    def pluginDescriptor =
    """
    <plugin name="TestPlugin" displayName="Test Plugin" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ServerA"
              description="Server A description"
              class="org.rhq.plugins.test.ServerA"
              discovery="org.rhq.plugins.test.ServerADiscoveryComponent">

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

        <service name="Child1"
                 description="Child 1 description"
                 class="org.rhq.plugins.test.Child1"
                 discovery="org.rhq.plugins.test.Child1ServiceDiscoveryComponent"/>
        <service name="Child2"
                 description="Child 2 description"
                 class="org.rhq.plugins.test.Child2"
                 discovery="org.rhq.plugins.test.Child2ServiceDiscoveryComponent"/>
      </server>

      <server name="ServerB"
              description="Server B description"
              class="org.rhq.plugins.test.ServerB"
              discovery="org.rhq.plugins.test.ServerBDiscoveryComponent"/>

      <server name="ServerC"
              description="Server C description"
              class="org.rhq.plugins.test.ServerC"
              discovery="org.rhq.plugins.test.ServerCDiscoveryComponent">

        <operation name="run">
          <parameters>
            <c:simple-property name="script"/>
          </parameters>
          <results>
            <c:simple-property name="errors"/>
          </results>
        </operation>

        <event name="serverCEvent" description="an entry was appended to a log file"/>
      </server>

      <server name="ServerD">
        <service name="ServerD.Child1">
          <service name="ServerD.GrandChild1"/>
        </service>
      </server>
    </plugin>
    """

    def args = createPlugin("test-plugin", pluginDescriptor, "1.0")
    Assert.assertNotNull args.plugin
    Assert.assertNotNull args.pluginFile
    Assert.assertNotNull args.pluginDescriptor

    def subjectMgr = LookupUtil.subjectManager
    def resourceMetadataMgr = LookupUtil.resourceMetadataManager

    resourceMetadataMgr.registerPlugin(subjectMgr.overlord, args.plugin, args.pluginDescriptor, args.pluginFile, false) 
  }

  @Test(dependsOnMethods = ['registerPlugin'], groups = ['NewPlugin'])
  void persistNewTypes() {
    def newTypes = ['ServerA', 'ServerB']
    assertTypesPersisted "Failed to persist new types", newTypes, 'TestPlugin'
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistSubcategories() {
    assertAssociationEquals(
        'ServerA',
        'subCategories',
        ['Resources', 'Applications']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistMeasurementDefinitions() {
    assertAssociationEquals(
        'ServerA',
        'metricDefinitions',
        ['metric1', 'metric2']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistEventDefinitions() {
    assertAssociationEquals(
        'ServerA',
        'eventDefinitions',
        ['logAEntry', 'logBEntry']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistOperationDefinitions() {
    assertAssociationEquals(
        'ServerA',
        'operationDefinitions',
        ['start', 'stop']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistProcessScans() {
    assertAssociationEquals(
        'ServerA',
        'processScans',
        ['serverA']
    )
  }

  @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
  void persistChildTypes() {
    assertAssociationEquals(
        'ServerA',
        'childResourceTypes',
        ['Child1', 'Child2']
    )

    @Test(dependsOnMethods = ['persistNewTypes'], groups = ['NewPlugin'])
    void persistPluginConfigurationDefinition
    assertAssociationExists('ServerA', 'pluginConfigurationDefinition')
  }

  @Test(groups = ['UpgradePlugin'], dependsOnGroups = ['NewPlugin'])
  void upgradePlugin() {
    def pluginDescriptor =
    """
    <plugin name="TestPlugin" displayName="Test Plugin" package="org.rhq.plugins.test"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="urn:xmlns:rhq-plugin"
        xmlns:c="urn:xmlns:rhq-configuration">
      <server name="ServerA"
              description="Server A description"
              class="org.rhq.plugins.test.ServerA"
              discovery="org.rhq.plugins.test.ServerADiscoveryComponent">

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

        <service name="Child1"
                 description="Child 1 description"
                 class="org.rhq.plugins.test.Child1"
                 discovery="org.rhq.plugins.test.Child1ServiceDiscoveryComponent"/>

        <service name="Child3"
                 description="Child 3 description"
                 class="org.rhq.plugins.test.Child3"
                 discovery="org.rhq.plugins.test.Child3ServiceDiscoveryComponent"/>        
      </server>
      <server name="ServerB"
              description="Server B description"
              class="org.rhq.plugins.test.ServerB"
              discovery="org.rhq.plugins.test.ServerBDiscoveryComponent">
        <service name="Child2"
                 description="Child 2 description"
                 class="org.rhq.plugins.test.Child2"
                 discovery="org.rhq.plugins.test.Child2ServiceDiscoveryComponent"/>
      </server>

      <server name="ServerD">
        <service name="ServerD.GrandChild1"/>
      </server>
    </plugin>
    """

    def args = createPlugin("test-plugin", pluginDescriptor, "2.0")
    Assert.assertNotNull args.plugin
    Assert.assertNotNull args.pluginFile
    Assert.assertNotNull args.pluginDescriptor

    def subjectMgr = LookupUtil.subjectManager
    def resourceMetadataMgr = LookupUtil.resourceMetadataManager

    resourceMetadataMgr.registerPlugin(subjectMgr.overlord, args.plugin, args.pluginDescriptor, args.pluginFile, true)
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeOperationDefinitions() {
    assertAssociationEquals(
        'ServerA',
        'operationDefinitions',
        ['start', 'shutdown', 'restart']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeChildResources() {
    assertAssociationEquals(
        'ServerA',
        'childResourceTypes',
        ['Child1', 'Child3']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeParentTypeWhenTypeChangesParents() {
    assertAssociationEquals(
        'ServerB',
        'childResourceTypes',
        ['Child2']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeEventDefinitions() {
    assertAssociationEquals(
        'ServerA',
        'eventDefinitions',
        ['logAEntry', 'logCEntry']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void upgradeProcessScans() {
    assertAssociationEquals(
        'ServerA',
        'processScans',
        ['processA', 'processB']
    )
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void deleteOperationDefsForRemovedType() {
    def operationMgr = LookupUtil.operationManager
    def subjectMgr = LookupUtil.subjectManager

    def criteria = new OperationDefinitionCriteria()
    criteria.addFilterResourceTypeName 'ServerC'
    criteria.addFilterName 'run'

    def operationDefs = operationMgr.findOperationDefinitionsByCriteria(subjectMgr.overlord, criteria)

    assertEquals "The operation definition should have been deleted", 0, operationDefs.size()
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void deleteEventDefsForRemovedType() {
    def results = entityManager.createQuery(
        "from EventDefinition e where e.name = :ename and e.resourceType.name = :rname")
        .setParameter("ename", "serverCEvent")
        .setParameter("rname", "ServerC")
        .getResultList()

    assertEquals "The event definition(s) should have been deleted", 0, results.size()
  }

  @Test(dependsOnMethods = ['upgradePlugin'], groups = ['UpgradePlugin'])
  void deleteParent() {
    def subjectMgr = LookupUtil.subjectManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager

    def criteria = new ResourceTypeCriteria()
    criteria.addFilterName "GrandChild1"
    criteria.addFilterPluginName "TestPlugin"
    criteria.fetchParentResourceTypes true

    def types = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.overlord, criteria)

    assertEquals "Expected to get back one resource type", 1, types.size()

    def type = types[0]

    assertEquals "Expected to find one parent type", 1, type.parentResourceTypes.size()

    def parentType = type.parentResourceTypes.find { it.name == "ServerD" }

    assertNotNull "Expected to find 'ServerD' as the parent, but found, $type.parentResourceTypes", parentType
  }

  /**
   * This method creates the plugin-related artifacts that are need to call
   * ResourceMetadataManager.registerPlugin. It creates the PluginDescriptor object, and
   * then it generates the plugin jar file. Lastly, the Plugin object is created. A map
   * is returned containing these generated artifacts. The map keys are pluginDescriptor,
   * pluginFile, and plugin.
   *
   * @param pluginFileName The name to give the generated plugin file. This should not
   * include the file extension, i.e., the '.jar'
   *
   * @param descriptor The plugin descriptor as a string
   *
   * @param version The plugin version which will be the version stored in MANIFEST.MF
   *
   * @return A map containing the generated artifacts. The maps keys are pluginDescriptor,
   * pluginFile, and plugin
   */
  def createPlugin(String pluginFileName, String descriptor, String version) {
    def pluginDescriptor = toPluginDescriptor(descriptor)
    def pluginFilePath = "$currentWorkingDir/${pluginFileName}.jar"

    def ant = new AntBuilder()
    ant.delete(dir: "$pluginWorkDir")
    ant.delete(file: pluginFilePath)

    ant.mkdir(dir: "$pluginWorkDir/META-INF")
    new File("$pluginWorkDir/META-INF/rhq-plugin.xml").text = descriptor
    ant.jar(destfile: "$currentWorkingDir/${pluginFileName}.jar", basedir: "$pluginWorkDir") {
      manifest {
        attribute(name: "Specification-Version", value: version)
        attribute(name: "Implementation-Version", value: version)
      }
    }

    def pluginFile = new File(pluginFilePath)

    def plugin = new Plugin(pluginDescriptor.name, pluginFilePath)
    plugin.displayName = pluginDescriptor.name
    plugin.enabled = true
    plugin.description = pluginDescriptor.description
    plugin.ampsVersion = getAmpsVersion(pluginDescriptor)
    plugin.version = pluginDescriptor.version
    plugin.MD5 = MessageDigestGenerator.getDigestString(pluginFile)

    return [pluginDescriptor: pluginDescriptor, pluginFile: pluginFile, plugin: plugin]
  }

  String getPluginWorkDir() {
    "${currentWorkingDir}/work"
  }

  String getCurrentWorkingDir() {
    getClass().getResource(".").toURI().path
  }

  String getAmpsVersion(PluginDescriptor pluginDescriptor) {
    if (pluginDescriptor.ampsVersion == null) {
      return "2.0"
    }

    ComparableVersion version = new ComparableVersion(pluginDescriptor.ampsVersion)
    ComparableVersion version2 = new ComparableVersion("2.0")

    if (version.compareTo(version2) <= 0) {
      return "2.0"
    }

    return pluginDescriptor.ampsVersion
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

  /**
   * This custom assertion looks up the specified resource type and then looks at the value
   * of the specified property name which is assumed to be a collection. It is also assumed
   * that each element in the collection has a name property, and it is the value of the name
   * property that is compared against the expected values.
   *
   * This method fails if either any of the expected names is not found or if one of the
   * actual names does not exist in the expected list.
   *
   * @param resourceTypeName The resource type to look up
   * @param propertyName The name of the property to be inspected
   * @param expected A list of names expected to be found in each of the elements of the property
   */
  void assertAssociationEquals(String resourceTypeName, String propertyName, List expected) {
    def subjectMgr = LookupUtil.subjectManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager

    def fetch = "fetch${propertyName.capitalize()}"
    def criteria = new ResourceTypeCriteria()
    criteria.addFilterName resourceTypeName
    criteria.addFilterPluginName 'TestPlugin'
    criteria."$fetch" true

    def resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.overlord, criteria)
    def resourceType = resourceTypes[0]
    def expectedSet = expected as Set
    def missing = []
    def unexpected = []

    expectedSet.each { expectedProperty ->
      if (resourceType[propertyName].find { it.name == expectedProperty } == null) {
        missing << it
      }
    }

    resourceType[propertyName].each { actualProperty ->
      if (expectedSet.find { it == actualProperty.name } == null) {
        unexpected << actualProperty.name
      }
    }

    def errors = ""
    if (missing.size() > 0) {
      errors = "Failed to find the following $propertyName(s) for type '$resourceTypeName': $missing"
    }
    if (unexpected.size() > 0) {
      errors += "\n The following $propertyName(s) were found but not expected for type '$resourceTypeName': $unexpected"
    }

    assertTrue(errors, errors.length() == 0)
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
