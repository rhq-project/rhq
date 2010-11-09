package org.rhq.enterprise.server.resource.metadata

import org.testng.annotations.Test
import org.rhq.core.domain.alert.AlertDampening
import org.rhq.core.domain.alert.BooleanExpression
import org.rhq.core.domain.alert.AlertPriority
import org.rhq.core.domain.alert.AlertDefinition
import org.rhq.enterprise.server.util.LookupUtil

class AlertMetadataManagerBeanTest extends MetadataTest {

  @Test(groups = ['NewPlugin'])
  void registerPlugin() {
    def pluginDescriptor =
    """
    <plugin name="AlertMetadataManagerBeanTestPlugin"
            displayName="AlertMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="AlertServer"/>
    </plugin>
    """

    createPlugin("alert-test-plugin", "1.0", pluginDescriptor)
  }

  @Test(groups = ['NewPlugin'], dependsOnMethods = ['registerPlugin'])
  void createAlertTemplates() {
    createAlertTemplate 'AlertServer Template 1', 'AlertServer', 'AlertMetadataManagerBeanTestPlugin'
    createAlertTemplate 'AlertServer Template 2', 'AlertServer', 'AlertMetadataManagerBeanTestPlugin'
  }

  @Test(groups = ['UpgradePlugin'], dependsOnGroups = ['NewPlugin'])
  void upgradePlugin() {
    def pluginDescriptor =
    """
    <plugin name="AlertMetadataManagerBeanTestPlugin"
            displayName="AlertMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
    </plugin>
    """

    createPlugin("alert-test-plugin", "2.0", pluginDescriptor)
  }

  @Test(groups = ['UpgradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void deleteAlertTemplates() {
    def templates = entityManager.createQuery("from AlertDefinition a where a.resourceType.name = :type")
        .setParameter('type', 'AlertServer')
        .getResultList()

    assertEquals "Alert templates should have been deleted.", 0, templates.size()
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

}
