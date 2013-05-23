package org.rhq.enterprise.server.resource.metadata;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This test shows a plugin extended multiple times via the embedded extension model.
 * There is one parent plugin and two child plugins. This test makes sure multiple
 * plugins can extend and be updated; it won't test a full comprehensive set of
 * metadata being upgraded. See {@link PluginExtensionMetadataTest} for a
 * comprehensive test showing all the different kinds of metadata being updated.
 *
 * @author John Mazzitelli
 */
@Test(groups = { "plugin.multiple.extension.metadata", "plugin.metadata" })
public class MultiplePluginExtensionMetadataTest extends MetadataBeanTest {

    private static final String PLUGIN_NAME_PARENT = "MultiplePluginExtensionMetadataParentTestPlugin";
    private static final String PLUGIN_NAME_CHILD1 = "PluginExtensionMetadataChild1TestPlugin";
    private static final String PLUGIN_NAME_CHILD2 = "PluginExtensionMetadataChild2TestPlugin";
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

    public void testRegisterPlugins() throws Exception {
        doTheTest(false);
    }

    public void testRegisterPluginsWithIgnoredTypes() throws Exception {
        doTheTest(true);
    }

    private void doTheTest(boolean ignoreTypes) throws Exception {
        subjectMgr = LookupUtil.getSubjectManager();
        resourceTypeMgr = LookupUtil.getResourceTypeManager();

        registerParentPluginV1(ignoreTypes); // create an initial type (called the parent)
        registerChild1PluginV1(ignoreTypes); // using plugin extension mechanism, create child #1 type that extends that parent type
        registerChild2PluginV1(ignoreTypes); // using plugin extension mechanism, create child #2 type that extends that parent type
        registerParentPluginV2(ignoreTypes); // update the parent type
        checkChild1Plugin(ignoreTypes); // check that the changes to the parent type propogated to the child #1
        checkChild2Plugin(ignoreTypes); // check that the changes to the parent type propogated to the child #2
    }

    // this needs to be the last test executed in the class, it does cleanup
    @Test(priority = 10, alwaysRun = true, dependsOnMethods = { "testRegisterPlugins" })
    public void afterClassWorkTest() throws Exception {
        afterClassWork();
    }

    private void registerParentPluginV1(boolean ignoreTypes) throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createPlugin("parent-plugin.jar", "1.0", "parent_plugin_v1.xml");
        if (ignoreTypes) {
            ignoreType(TYPE_NAME_PARENT, PLUGIN_NAME_PARENT);
        }

        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_PARENT, PLUGIN_NAME_PARENT);
        assert resourceType.getName().equals(TYPE_NAME_PARENT);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_PARENT);
        assert resourceType.isIgnored() == ignoreTypes;
        assertVersion1(resourceType);
    }

    private void registerChild1PluginV1(boolean ignoreTypes) throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createPlugin("child1-plugin.jar", "1.0", "child1_plugin_v1.xml");
        if (ignoreTypes) {
            ignoreType(TYPE_NAME_CHILD1, PLUGIN_NAME_CHILD1);
        }

        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD1, PLUGIN_NAME_CHILD1);
        assert resourceType.getName().equals(TYPE_NAME_CHILD1);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD1);
        assert resourceType.isIgnored() == ignoreTypes;
        assertVersion1(resourceType);

        // in our child #1 plugin, our extended type is actually a child of child #1 plugin's root type
        // here we want to make sure that hierarchy remains intact with our extended type - the parent
        // of our extended type should be this child #1 root type
        assert resourceType.getParentResourceTypes() != null;
        assert resourceType.getParentResourceTypes().size() == 1;
        assert resourceType.getParentResourceTypes().iterator().next().getName().equals("OuterServerA");
    }

    private void registerChild2PluginV1(boolean ignoreTypes) throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createPlugin("child2-plugin.jar", "1.0", "child2_plugin_v1.xml");
        if (ignoreTypes) {
            ignoreType(TYPE_NAME_CHILD2, PLUGIN_NAME_CHILD2);
        }

        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD2, PLUGIN_NAME_CHILD2);
        assert resourceType.getName().equals(TYPE_NAME_CHILD2);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD2);
        assert resourceType.isIgnored() == ignoreTypes;
        assertVersion1(resourceType);
    }

    private void registerParentPluginV2(boolean ignoreTypes) throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createPlugin("parent-plugin.jar", "2.0", "parent_plugin_v2.xml");

        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_PARENT, PLUGIN_NAME_PARENT);
        assert resourceType.getName().equals(TYPE_NAME_PARENT);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_PARENT);
        assert resourceType.isIgnored() == ignoreTypes;
        assertVersion2(resourceType);
    }

    private void checkChild1Plugin(boolean ignoreTypes) throws Exception {
        // load the child #1 type and test to make sure it has been updated to what we expect
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD1, PLUGIN_NAME_CHILD1);
        assert resourceType.getName().equals(TYPE_NAME_CHILD1);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD1);
        assert resourceType.isIgnored() == ignoreTypes;
        assertVersion2(resourceType);

        // in our child #1 plugin, our extended type is actually a child of child #1 plugin's root type
        // here we want to make sure that hierarchy remains intact with our extended type - the parent
        // of our extended type should be this child #1 root type
        assert resourceType.getParentResourceTypes() != null;
        assert resourceType.getParentResourceTypes().size() == 1;
        assert resourceType.getParentResourceTypes().iterator().next().getName().equals("OuterServerA");
    }

    private void checkChild2Plugin(boolean ignoreTypes) throws Exception {
        // load the child #2 type and test to make sure it has been updated to what we expect
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD2, PLUGIN_NAME_CHILD2);
        assert resourceType.getName().equals(TYPE_NAME_CHILD2);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD2);
        assert resourceType.isIgnored() == ignoreTypes;
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
        c.addFilterIgnored(null);
        c.setStrict(true);
        c.fetchParentResourceTypes(true);
        c.fetchOperationDefinitions(true);
        List<ResourceType> t = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), c);
        ResourceType resourceType = t.get(0);
        return resourceType;
    }
}
