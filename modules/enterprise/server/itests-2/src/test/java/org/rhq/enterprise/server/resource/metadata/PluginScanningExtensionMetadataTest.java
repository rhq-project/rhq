package org.rhq.enterprise.server.resource.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This test shows a plugin extended multiple times via the embedded extension model,
 * but it tests starting with the server's plugin scanner service. This helps
 * simulate more closely the full end-to-end agent plugin scanning and deployment
 * mechanism during which resource types are registered.
 *
 * There is one parent plugin and two child plugins. This test makes sure multiple
 * plugins can extend and be updated; it won't test a full comprehensive set of
 * metadata being upgraded. See {@link PluginExtensionMetadataTest} for a
 * comprehensive test showing all the different kinds of metadata being updated.
 *
 * @author John Mazzitelli
 */
@Test(groups = { "plugin.extension.scanning.metadata", "plugin.metadata" })
public class PluginScanningExtensionMetadataTest extends MetadataBeanTest {

    private static final String PLUGIN_NAME_PARENT = "PluginScanningExtensionMetadataParentTestPlugin";
    private static final String PLUGIN_NAME_CHILD1 = "PluginScanningExtensionMetadataChild1TestPlugin";
    private static final String PLUGIN_NAME_CHILD2 = "PluginScanningExtensionMetadataChild2TestPlugin";
    private static final String TYPE_NAME_PARENT = "ParentServerA";
    private static final String TYPE_NAME_CHILD1 = "Child1ServerA";
    private static final String TYPE_NAME_CHILD2 = "Child2ServerA";

    // names of things from the first version of the plugin metadata
    private static final String OP_NAME = "A-op";
    private static final int OP_TIMEOUT = 123456;
    private static final String OP_DESC = "a op";

    // names of things from the second, updated version of the plugin metadata
    // updated operations
    private static final String NEW_OP_NAME = "A-op-NEW";
    private static final int NEW_OP_TIMEOUT = 987654;
    private static final String NEW_OP_DESC = "a new op";

    private SubjectManagerLocal subjectMgr;
    private ResourceTypeManagerLocal resourceTypeMgr;

    private List<File> createdJarFiles = new ArrayList<File>();

    @Override
    protected void beforeMethod() throws Exception {
        super.beforeMethod();

        preparePluginScannerService();

        subjectMgr = LookupUtil.getSubjectManager();
        resourceTypeMgr = LookupUtil.getResourceTypeManager();

        // clean up any old previously generated jar files so they don't get in the way
        if (!createdJarFiles.isEmpty()) {
            for (File doomed : createdJarFiles) {
                doomed.delete();
            }
        }
        createdJarFiles.clear();
    }

    @Override
    protected void afterMethod() throws Exception {
        // clean up any generated jar files - we want to remove these so they don't get in the way of a future test run
        if (!createdJarFiles.isEmpty()) {
            for (File doomed : createdJarFiles) {
                doomed.delete();
            }
        }
        createdJarFiles.clear();

        unpreparePluginScannerService();

        super.afterMethod();
    }

    public void testRegisterPlugins() throws Exception {
        try {
            registerParentPluginV1(); // create an initial type (called the parent)
            registerChild1PluginV1(); // using plugin extension mechanism, create child #1 type that extends that parent type
            registerChild2PluginV1(); // using plugin extension mechanism, create child #2 type that extends that parent type
            registerParentPluginV2(); // update the parent type
            checkChild1Plugin(); // check that the changes to the parent type propogated to the child #1
            checkChild2Plugin(); // check that the changes to the parent type propogated to the child #2
        } finally {
            // let our superclass know about our plugins so it can clean them up
            pluginDeployed(PLUGIN_NAME_PARENT);
            pluginDeployed(PLUGIN_NAME_CHILD1);
            pluginDeployed(PLUGIN_NAME_CHILD2);
        }
    }

    // this needs to be the last test executed in the class, it does cleanup
    @Test(priority = 10, alwaysRun = true, dependsOnMethods = { "testRegisterPlugins" })
    public void afterClassWorkTest() throws Exception {
        afterClassWork();
    }

    private void registerParentPluginV1() throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createdJarFiles.add(createPluginJarFile("parent-plugin.jar", "parent_plugin_v1.xml"));
        this.pluginScanner.startDeployment(); // first time we need to scan so call startDeployment which will call scanAndRegister
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_PARENT, PLUGIN_NAME_PARENT);
        assert resourceType.getName().equals(TYPE_NAME_PARENT);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_PARENT);
        assertVersion1(resourceType);
    }

    private void registerChild1PluginV1() throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createdJarFiles.add(createPluginJarFile("child1-plugin.jar", "child1_plugin_v1.xml"));
        this.pluginScanner.scanAndRegister();
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD1, PLUGIN_NAME_CHILD1);
        assert resourceType.getName().equals(TYPE_NAME_CHILD1);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD1);
        assertVersion1(resourceType);

        // in our child #1 plugin, our extended type is actually a child of child #1 plugin's root type
        // here we want to make sure that hierarchy remains intact with our extended type - the parent
        // of our extended type should be this child #1 root type
        assert resourceType.getParentResourceTypes() != null;
        assert resourceType.getParentResourceTypes().size() == 1;
        assert resourceType.getParentResourceTypes().iterator().next().getName().equals("OuterServerA");
    }

    private void registerChild2PluginV1() throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createdJarFiles.add(createPluginJarFile("child2-plugin.jar", "child2_plugin_v1.xml"));
        this.pluginScanner.scanAndRegister();
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD2, PLUGIN_NAME_CHILD2);
        assert resourceType.getName().equals(TYPE_NAME_CHILD2);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD2);
        assertVersion1(resourceType);
    }

    private void registerParentPluginV2() throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createdJarFiles.add(createPluginJarFile("parent-plugin.jar", "parent_plugin_v2.xml"));
        this.pluginScanner.scanAndRegister();
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_PARENT, PLUGIN_NAME_PARENT);
        assert resourceType.getName().equals(TYPE_NAME_PARENT);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_PARENT);
        assertVersion2(resourceType);
    }

    private void checkChild1Plugin() throws Exception {
        // load the child #1 type and test to make sure it has been updated to what we expect
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD1, PLUGIN_NAME_CHILD1);
        assert resourceType.getName().equals(TYPE_NAME_CHILD1);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD1);
        assertVersion2(resourceType);

        // in our child #1 plugin, our extended type is actually a child of child #1 plugin's root type
        // here we want to make sure that hierarchy remains intact with our extended type - the parent
        // of our extended type should be this child #1 root type
        assert resourceType.getParentResourceTypes() != null;
        assert resourceType.getParentResourceTypes().size() == 1;
        assert resourceType.getParentResourceTypes().iterator().next().getName().equals("OuterServerA");
    }

    private void checkChild2Plugin() throws Exception {
        // load the child #2 type and test to make sure it has been updated to what we expect
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD2, PLUGIN_NAME_CHILD2);
        assert resourceType.getName().equals(TYPE_NAME_CHILD2);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD2);
        assertVersion2(resourceType);
    }

    private void assertVersion1(ResourceType resourceType) {
        assert resourceType.getOperationDefinitions().size() == 1;
        OperationDefinition op = resourceType.getOperationDefinitions().iterator().next();
        assert op.getName().equals(OP_NAME);
        assert op.getTimeout().intValue() == OP_TIMEOUT;
        assert op.getDescription().equals(OP_DESC);
    }

    private void assertVersion2(ResourceType resourceType) {
        assert resourceType.getOperationDefinitions().size() == 1;
        OperationDefinition op = resourceType.getOperationDefinitions().iterator().next();
        assert op.getName().equals(NEW_OP_NAME);
        assert op.getTimeout().intValue() == NEW_OP_TIMEOUT;
        assert op.getDescription().equals(NEW_OP_DESC);
    }

    private ResourceType loadResourceTypeFully(String typeName, String typePlugin) {
        ResourceTypeCriteria c = new ResourceTypeCriteria();
        c.addFilterName(typeName);
        c.addFilterPluginName(typePlugin);
        c.setStrict(true);
        c.fetchParentResourceTypes(true);
        c.fetchOperationDefinitions(true);
        List<ResourceType> t = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), c);
        ResourceType resourceType = t.get(0);
        return resourceType;
    }
}
