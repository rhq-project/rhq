package org.rhq.enterprise.server.resource.metadata

import org.testng.annotations.Test

class OperationMetadataManagerBeanTest extends MetadataTest {

  @Test(groups = ['plugin.metadata', 'Operations.NewPlugin'])
  void registerOperationsPlugin() {
    def pluginDescriptor =
    """
    <plugin name="OperationMetadataManagerBeanTestPlugin"
            displayName="OperationMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="OperationServer1">
        <operation name="OperationServer1.Operation1"/>
      </server>
      <server name="OperationServer11">
        <operation name="OperationServer11.Operation1">
          <parameters>
            <c:simple-property name="param1"/>
            <c:simple-property name="param2"/>
          </parameters>
        </operation>
      </server>

      <server name="OperationServer2"/>
      <server name="OperationServer21">
        <operation name="OperationServer21.Operation1"/>
      </server>
      <server name="OperationServer22">
        <operation name="OperationServer22.Operation1">
          <parameters>
            <c:simple-property name="param1"/>
            <c:simple-property name="param2"/>
          </parameters>
        </operation>
      </server>
      <server name="OperationServer23">
        <operation name="OperationServer23.Operation1"/>
      </server>
      <server name="OperationServer24">
        <operation name="OperationServer24.Operation1"/>
        <operation name="OperationServer24.Operation2">
          <results>
            <c:simple-property name="exitCode"/>
          </results>
        </operation>
      </server>
    </plugin>
    """

    createPlugin("operation-test-plugin", "1.0", pluginDescriptor)
  }

  @Test(groups = ['plugin.metadata', 'Operations.NewPlugin'], dependsOnMethods = ['registerOperationsPlugin'])
  void createTypeWithOperationDefWithNoParamsAndNoResults() {
    assertResourceTypeAssociationEquals(
        'OperationServer1',
        'OperationMetadataManagerBeanTestPlugin',
        'operationDefinitions',
        ['OperationServer1.Operation1']
    )
  }

  @Test(groups = ['plugin.metadata', 'Operations.NewPlugin'], dependsOnMethods = ['registerOperationsPlugin'])
  void createTypeWithOperationDefWithParams() {
    def resourceType = loadResourceTypeWithOperationDefs('OperationServer11', 'OperationMetadataManagerBeanTestPlugin')
    def operationDefs = resourceType.operationDefinitions as List

    assertEquals('Expected to find an operation definition', 1, operationDefs.size())

    def params = operationDefs[0].parametersConfigurationDefinition

    assertNotNull("Expected to find parameters for operation definition", params)
    assertEquals("Expected to find 2 parameters", 2, params.propertyDefinitions.size())
    assertNotNull("Expected to find parameter named <param1>", params.get('param1'))
    assertNotNull("Expected to find parameter named <param2>", params.get('param2'))
  }

  @Test(groups = ['plugin.metadata', 'Operations.UpgradePlugin'], dependsOnGroups = ['Operations.NewPlugin'])
  void upgradeOperationsPlugin() {
    def pluginDescriptor =
    """
    <plugin name="OperationMetadataManagerBeanTestPlugin"
            displayName="OperationMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="OperationServer1">
        <operation name="OperationServer1.Operation1"/>
      </server>
      <server name="OperationServer11">
        <operation name="OperationServer11.Operation1">
          <parameters>
            <c:simple-property name="param1"/>
            <c:simple-property name="param2"/>
          </parameters>
        </operation>
      </server>

      <server name="OperationServer2">
        <operation name="OperationServer2.Operation1"/>
      </server>
      <server name="OperationServer21"/>
      <server name="OperationServer22">
        <operation name="OperationServer22.Operation1"/>
      </server>
      <server name="OperationServer23">
        <operation name="OperationServer23.Operation1">
          <results>
            <c:simple-property name="exitCode"/>
          </results>
        </operation>
      </server>
      <server name="OperationServer24">
        <operation name="OperationServer24.Operation1">
          <parameters>
            <c:simple-property name="param1"/>
          </parameters>
        </operation>
        <operation name="OperationServer24.Operation2"/>
      </server>
    </plugin>
    """

    createPlugin("operation-test-plugin", "1.0", pluginDescriptor)
1  }

  @Test(groups = ['plugin.metadata', 'Operations.UpgradePlugin'], dependsOnMethods = ['upgradeOperationsPlugin'])
  void addOperationToTypeThatPreviouslyHadNoOperations() {
    assertResourceTypeAssociationEquals(
        'OperationServer2',
        'OperationMetadataManagerBeanTestPlugin',
        'operationDefinitions',
        ['OperationServer2.Operation1']
    )
  }

  @Test(groups = ['plugin.metadata', 'Operations.UpgradePlugin'], dependsOnMethods = ['upgradeOperationsPlugin'])
  void removeOperationDefFromTypeThatPreviouslyDefinedOperationDef() {
    assertResourceTypeAssociationEquals(
        'OperationServer21',
        'OperationMetadataManagerBeanTestPlugin',
        'operationDefinitions',
        []
    )
  }

  @Test(groups = ['plugin.metadata', 'Operations.UpgradePlugin'], dependsOnMethods = ['upgradeOperationsPlugin'])
  void removeParamsFromUpgradedOperationDef() {
    def operationDef = loadOperationDefinition('OperationServer22.Operation1', 'OperationServer22',
        'OperationMetadataManagerBeanTestPlugin')

    assertNull "Operation parameters should have been removed", operationDef.parametersConfigurationDefinition
  }

  @Test(groups = ['plugin.metadata', 'Operations.UpgradePlugin'], dependsOnMethods = ['upgradeOperationsPlugin'])
  void addResultsToUpgradedOperationDef() {
    def operationDef = loadOperationDefinition('OperationServer23.Operation1', 'OperationServer23',
        'OperationMetadataManagerBeanTestPlugin')
    def resultsDef = operationDef.resultsConfigurationDefinition

    assertNotNull('Results definition should have been added', resultsDef)
    assertEquals('Expected results to contain one property', 1, resultsDef.propertyDefinitions.size())
    assertNotNull('Expected results to contain property named <exitCode>', resultsDef.get('exitCode'))
  }

  @Test(groups = ['plugin.metadata', 'Operations.UpgradePlugin'], dependsOnMethods = ['upgradeOperationsPlugin'])
  void addParamsToUpgradedOperationDef() {
    def operationDef = loadOperationDefinition('OperationServer24.Operation1', 'OperationServer24',
        'OperationMetadataManagerBeanTestPlugin')
    def paramsDef = operationDef.parametersConfigurationDefinition

    assertNotNull "Operation parameters definition should have been added.", paramsDef
    assertEquals "Expected to find one parameter definition.", 1, paramsDef.propertyDefinitions.size()
    assertNotNull("Expected parameters to contain property named <param1>",
        paramsDef.propertyDefinitions.get('param1'))
  }

  @Test(groups = ['plugin.metadata', 'Operations.UpgradePlugin'], dependsOnMethods = ['upgradeOperationsPlugin'])
  void removeResultsInUpgradedOperationDef() {
    def operationDef = loadOperationDefinition('OperationServer24.Operation1', 'OperationServer24',
        'OperationMetadataManagerBeanTestPlugin')
    def resultsDef = operationDef.resultsConfigurationDefinition

    assertNull 'Results definition should have been removed', resultsDef
  }

  def loadResourceTypeWithOperationDefs(String resourceType, String plugin) {
    return entityManager.createQuery(
      """
      from  ResourceType t left join fetch t.operationDefinitions
      where t.name = :resourceType and t.plugin = :plugin
      """
    ).setParameter('resourceType', resourceType)
     .setParameter('plugin', plugin)
     .getSingleResult()
  }

  def loadOperationDefinition(String opName, String resourceType, String plugin) {
    return  entityManager.createQuery(
    """
    from  OperationDefinition o
    where o.name = :operationName and o.resourceType.name = :resourceType and o.resourceType.plugin = :plugin
    """
    ).setParameter('operationName', opName)
     .setParameter('resourceType', resourceType)
     .setParameter('plugin', plugin)
     .getSingleResult()
  }

}
