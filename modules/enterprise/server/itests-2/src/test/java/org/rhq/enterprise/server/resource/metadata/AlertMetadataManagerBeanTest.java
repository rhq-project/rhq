package org.rhq.enterprise.server.resource.metadata;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AlertMetadataManagerBeanTest extends MetadataBeanTest {

    @Test(groups = {"plugin.metadata", "Alerts.NewPlugin"})
    public void registerAlertsPlugin() throws Exception {
        createPlugin("alert-test-plugin", "1.0", "plugin_v1.xml");
    }

    @Test(groups = {"plugin.metadata", "Alerts.NewPlugin"}, dependsOnMethods = {"registerAlertsPlugin"})
    public void createAlertTemplates() throws Exception {
        createAlertTemplate("AlertServer Template 1", "AlertServer", "AlertMetadataManagerBeanTestPlugin");
        createAlertTemplate("AlertServer Template 2", "AlertServer", "AlertMetadataManagerBeanTestPlugin");
    }

    @Test(groups = {"plugin.metadata", "UpgradePlugin"}, dependsOnGroups = {"Alerts.NewPlugin"})
    public void upgradeAlertsPlugin() throws Exception {
        createPlugin("alert-test-plugin", "2.0", "plugin_v2.xml");
    }

    @Test(groups = {"plugin.metadata", "Alerts.UpgradePlugin"}, dependsOnMethods = {"upgradeAlertsPlugin"})
    public void deleteAlertTemplates() {
        List templates = getEntityManager().createQuery("from AlertDefinition a where a.resourceType.name = :type")
            .setParameter("type", "AlertServer")
            .getResultList();

        assertEquals("Alert templates should have been deleted.", 0, templates.size());
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
