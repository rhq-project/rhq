package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;

public class OperationMetadataManagerBeanTest extends MetadataBeanTest {

    @Test(groups = {"plugin.metadata", "Operations.NewPlugin"})
    public void registerOperationsPlugin() throws Exception {
        createPlugin("operation-test-plugin", "1.0", "plugin_v1.xml");
    }

    @Test(groups = {"plugin.metadata", "Operations.NewPlugin"}, dependsOnMethods = {"registerOperationsPlugin"})
    public void createTypeWithOperationDefWithNoParamsAndNoResults() throws Exception {
        assertResourceTypeAssociationEquals(
            "OperationServer1",
            "OperationMetadataManagerBeanTestPlugin",
            "operationDefinitions",
            asList("OperationServer1.Operation1")
        );
    }

    @Test(groups = {"plugin.metadata", "Operations.NewPlugin"}, dependsOnMethods = {"registerOperationsPlugin"})
    public void createTypeWithOperationDefWithParams() {
        ResourceType resourceType = loadResourceTypeWithOperationDefs("OperationServer11",
            "OperationMetadataManagerBeanTestPlugin");
        List<OperationDefinition> operationDefs =
            new ArrayList<OperationDefinition>(resourceType.getOperationDefinitions());

        assertEquals("Expected to find an operation definition", 1, operationDefs.size());

        ConfigurationDefinition params = operationDefs.get(0).getParametersConfigurationDefinition();

        assertNotNull("Expected to find parameters for operation definition", params);
        assertEquals("Expected to find 2 parameters", 2, params.getPropertyDefinitions().size());
        assertNotNull("Expected to find parameter named <param1>", params.get("param1"));
        assertNotNull("Expected to find parameter named <param2>", params.get("param2"));
    }

    @Test(groups = {"plugin.metadata", "Operations.UpgradePlugin"}, dependsOnGroups = {"Operations.NewPlugin"})
    public void upgradeOperationsPlugin() throws Exception {
        createPlugin("operation-test-plugin", "2.0", "plugin_v2.xml");
    }

    @Test(groups = {"plugin.metadata", "Operations.UpgradePlugin"}, dependsOnMethods = {"upgradeOperationsPlugin"})
    public void addOperationToTypeThatPreviouslyHadNoOperations() throws Exception {
        assertResourceTypeAssociationEquals(
            "OperationServer2",
            "OperationMetadataManagerBeanTestPlugin",
            "operationDefinitions",
            asList("OperationServer2.Operation1")
        );
    }

    @Test(groups = {"plugin.metadata", "Operations.UpgradePlugin"}, dependsOnMethods = {"upgradeOperationsPlugin"})
    public void removeOperationDefFromTypeThatPreviouslyDefinedOperationDef() throws Exception {
        assertResourceTypeAssociationEquals(
            "OperationServer21",
            "OperationMetadataManagerBeanTestPlugin",
            "operationDefinitions",
            EMPTY_LIST
        );
    }

    @Test(groups = {"plugin.metadata", "Operations.UpgradePlugin"}, dependsOnMethods = {"upgradeOperationsPlugin"})
    public void removeParamsFromUpgradedOperationDef() {
        OperationDefinition operationDef = loadOperationDefinition("OperationServer22.Operation1", "OperationServer22",
        "OperationMetadataManagerBeanTestPlugin");

        assertNull("Operation parameters should have been removed", operationDef.getParametersConfigurationDefinition());
    }

    @Test(groups = {"plugin.metadata", "Operations.UpgradePlugin"}, dependsOnMethods = {"upgradeOperationsPlugin"})
    public void addResultsToUpgradedOperationDef() {
        OperationDefinition operationDef = loadOperationDefinition("OperationServer23.Operation1", "OperationServer23",
            "OperationMetadataManagerBeanTestPlugin");
        ConfigurationDefinition resultsDef = operationDef.getResultsConfigurationDefinition();

        assertNotNull("Results definition should have been added", resultsDef);
        assertEquals("Expected results to contain one property", 1, resultsDef.getPropertyDefinitions().size());
        assertNotNull("Expected results to contain property named <exitCode>", resultsDef.get("exitCode"));
    }

    @Test(groups = {"plugin.metadata", "Operations.UpgradePlugin"}, dependsOnMethods = {"upgradeOperationsPlugin"})
    public void addParamsToUpgradedOperationDef() {
        OperationDefinition operationDef = loadOperationDefinition("OperationServer24.Operation1", "OperationServer24",
            "OperationMetadataManagerBeanTestPlugin");
        ConfigurationDefinition paramsDef = operationDef.getParametersConfigurationDefinition();

        assertNotNull("Operation parameters definition should have been added.", paramsDef);
        assertEquals("Expected to find one parameter definition.", 1, paramsDef.getPropertyDefinitions().size());
        assertNotNull("Expected parameters to contain property named <param1>",
            paramsDef.getPropertyDefinitions().get("param1"));
  }

    @Test(groups = {"plugin.metadata", "Operations.UpgradePlugin"}, dependsOnMethods = {"upgradeOperationsPlugin"})
    public void removeResultsInUpgradedOperationDef() {
        OperationDefinition operationDef = loadOperationDefinition("OperationServer24.Operation1", "OperationServer24",
            "OperationMetadataManagerBeanTestPlugin");
        ConfigurationDefinition resultsDef = operationDef.getResultsConfigurationDefinition();

        assertNull("Results definition should have been removed", resultsDef);
    }

    ResourceType loadResourceTypeWithOperationDefs(String resourceType, String plugin) {
        return (ResourceType) getEntityManager().createQuery(
            "from  ResourceType t left join fetch t.operationDefinitions " +
            "where t.name = :resourceType and t.plugin = :plugin")
            .setParameter("resourceType", resourceType)
            .setParameter("plugin", plugin)
            .getSingleResult();
    }

    OperationDefinition loadOperationDefinition(String opName, String resourceType, String plugin) {
        return (OperationDefinition)  getEntityManager().createQuery(
            "from  OperationDefinition o " +
            "where o.name = :operationName and o.resourceType.name = :resourceType and o.resourceType.plugin = :plugin")
            .setParameter("operationName", opName)
            .setParameter("resourceType", resourceType)
            .setParameter("plugin", plugin)
            .getSingleResult();
    }

}
