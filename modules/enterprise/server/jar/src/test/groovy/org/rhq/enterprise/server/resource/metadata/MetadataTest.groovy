package org.rhq.enterprise.server.resource.metadata

import org.apache.maven.artifact.versioning.ComparableVersion
import org.dbunit.database.DatabaseConnection
import org.dbunit.dataset.IDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet
import org.dbunit.dataset.xml.FlatXmlProducer
import org.dbunit.operation.DatabaseOperation
import org.hibernate.Session
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor
import org.rhq.core.domain.criteria.ResourceTypeCriteria
import org.rhq.core.domain.plugin.Plugin
import org.rhq.core.util.MessageDigestGenerator
import org.rhq.enterprise.server.bundle.TestBundleServerPluginService
import org.rhq.enterprise.server.test.AbstractEJB3Test
import org.rhq.enterprise.server.util.LookupUtil
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.xml.sax.InputSource
import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.toPluginDescriptor
import org.dbunit.dataset.datatype.IDataTypeFactory
import org.dbunit.database.IDatabaseConnection
import org.testng.annotations.BeforeGroups
import org.testng.annotations.AfterGroups

/**
 * This is a base class for integration tests that exercise plugin installations, upgrades and removals. This class
 * has methods that actually generate a plugin jar file and register the plugin. The motivation for this stems from
 * plugin installation being a time-consuming process. While plugins could be generated and installed on a per-method
 * basis, that might make tests too slow. Instead it probably makes more sense to generate and install plugins on a
 * per-group or per-class basis. This class clears the database, at least those tables involved, prior to any tests
 * running. This ensures that the database is in a known, consistent state. Note that that the database is not reset
 * prior to each test method but rather only once per class.
 */
class MetadataTest extends AbstractEJB3Test {

  static def plugins = []

  @BeforeGroups(groups = ['plugin.metadata'], dependsOnGroups = ['integration.ejb3'])
  void startMBeanServer() {
    setupDB()

    def bundleService = new TestBundleServerPluginService();
    prepareCustomServerPluginService(bundleService)
    bundleService.startMasterPluginContainerWithoutSchedulingJobs()
    prepareScheduler()
  }

  /**
   * Need to delete rows from RHQ_PLUGINS because subsequent tests in server/jar would otherwise fail. Some tests
   * look at what plugins are in the database, and then look for corresponding plugin files on the file system.
   * MetadataTest however removes the generated plugin files during each test run.
   */
  @AfterGroups(groups = ['plugin.metadata'])
  void removePluginsFromDB() {
    unprepareScheduler()
     transaction {
      // using direct hibernate query here because JPA 1.0 lacks support for the IN clause
      // where you can directly specify a collection for the parameter value used in an IN
      // clause
      Session session = entityManager.delegate
      session.createQuery("delete from Plugin p where p.name in (:plugins)").setParameterList("plugins", plugins)
          .executeUpdate()
    }
  }

  void setupDB() {
    def dbunitConnection = new DatabaseConnection(connection);
    setDbType dbunitConnection
    DatabaseOperation.CLEAN_INSERT.execute(dbunitConnection, dataSet);
  }

  void setDbType(IDatabaseConnection connection) {
    def config = connection.config
    def name = connection.connection.metaData.databaseProductName.toLowerCase()
    int major = connection.connection.metaData.databaseMajorVersion
    IDataTypeFactory type = null
    if (name.contains("postgres")) {
      type = new org.dbunit.ext.postgresql.PostgresqlDataTypeFactory()
    } else if (name.contains("oracle")) {
      if (major >= 10) {
        type = new org.dbunit.ext.oracle.Oracle10DataTypeFactory()
      } else {
        type = new org.dbunit.ext.oracle.OracleDataTypeFactory()
      }
    }
    if (type != null) {
      config.setProperty("http://www.dbunit.org/properties/datatypeFactory",type)
    }
  }

  IDataSet getDataSet() {
    def xmlProducer = new FlatXmlProducer(new InputSource(getClass().getResourceAsStream(dataSetFile)));
    xmlProducer.columnSensing = true
    return new FlatXmlDataSet(xmlProducer);
  }

  String getDataSetFile() {
    "MetadataTest.xml"
  }

  def transaction(work) {
    try {
      transactionManager.begin()
      work()
      transactionManager.commit()
    } catch (Throwable t) {
      transactionManager.rollback()
    }
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
  def createPlugin(String pluginFileName, String version, String descriptor) {
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

    Assert.assertNotNull plugin
    Assert.assertTrue pluginFile.exists()
    Assert.assertNotNull pluginDescriptor

    def subjectMgr = LookupUtil.subjectManager
    def pluginMgr = LookupUtil.pluginManager

    pluginMgr.registerPlugin(subjectMgr.overlord, plugin, pluginDescriptor, pluginFile, true)
    plugins << plugin.name
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
   * @param plugin The plugin in which the resource type is defined
   * @param propertyName The name of the property to be inspected
   * @param expected A list of names expected to be found in each of the elements of the property
   */
  void assertResourceTypeAssociationEquals(String resourceTypeName, String plugin, String propertyName, List expected) {
    def subjectMgr = LookupUtil.subjectManager
    def resourceTypeMgr = LookupUtil.resourceTypeManager

    def fetch = "fetch${propertyName.capitalize()}"
    def criteria = new ResourceTypeCriteria()
    criteria.addFilterName resourceTypeName
    criteria.addFilterPluginName plugin
    criteria."$fetch" true
    criteria.strict = true

    def resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.overlord, criteria)
    def resourceType = resourceTypes[0]
    def expectedSet = expected as Set
    def missing = []
    def unexpected = []

    expectedSet.each { expectedProperty ->
      if (resourceType[propertyName].find { it.name == expectedProperty } == null) {
        missing << expectedProperty
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

}
