package org.rhq.enterprise.server.resource.metadata;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Test(groups = { "plugin.metadata.ignoretypes" })
public class MetadataUpdateWithIgnoredTypesTest extends MetadataBeanTest {

    private static final String PLUGIN_NAME = "RemoveIgnoredTypesPlugin";

    @Test
    public void upgradePluginWithIgnoredTypes() throws Exception {
        performTest(true);
    }

    // required cleanup
    @Test(priority = 10, alwaysRun = true)
    public void afterClassWorkTest() throws Exception {
        afterClassWork();
    }

    private void performTest(boolean ignoreTypes) throws Exception {
        createPlugin("remove-types-plugin", "1.0", "remove_types_v1.xml");

        ignoreAndGetPersistedType("ServerA", ignoreTypes);
        ignoreAndGetPersistedType("ServerA.Child1", ignoreTypes);
        ignoreAndGetPersistedType("ServerB", ignoreTypes);
        ignoreAndGetPersistedType("ServerB.Child1", ignoreTypes);
        ignoreAndGetPersistedType("ServerB.GrandChild1", ignoreTypes);
        ignoreAndGetPersistedType("ServerC", ignoreTypes);
        ignoreAndGetPersistedType("ServerC.Child1", ignoreTypes);
        ignoreAndGetPersistedType("ServerD", ignoreTypes);

        createPlugin("remove-types-plugin", "2.0", "remove_types_v2.xml");

        final String newDescription = "v2";
        ResourceType serverA = getPersistedTypeAndAssert("ServerA", newDescription, ignoreTypes);
        ResourceType serverA_Child1 = getPersistedTypeAndAssert("ServerA.Child1", newDescription, ignoreTypes);
        ResourceType serverB = getPersistedTypeAndAssert("ServerB", newDescription, ignoreTypes);
        assertTypeDeleted("ServerB.Child1");
        ResourceType serverB_GrandChild1 = getPersistedTypeAndAssert("ServerB.GrandChild1", newDescription, ignoreTypes);
        ResourceType serverC = getPersistedTypeAndAssert("ServerC", newDescription, ignoreTypes);
        assertTypeDeleted("ServerC.Child1");
        assertTypeDeleted("ServerD");
    }

    private ResourceType getPersistedTypeAndAssert(String typeName, String description, boolean isIgnored) {
        ResourceType rt = ignoreAndGetPersistedType(typeName, false);
        assertEquals(description, rt.getDescription());
        assertEquals(isIgnored, rt.isIgnored());
        return rt;
    }

    // this will ignore the resource type if "ignore" is true - if ignore is false, this just returns the type
    private ResourceType ignoreAndGetPersistedType(String typeName, boolean ignore) {
        ResourceTypeManagerLocal typeMgr = LookupUtil.getResourceTypeManager();

        ResourceType rt = typeMgr.getResourceTypeByNameAndPlugin(typeName, PLUGIN_NAME);
        if (rt == null) {
            fail("The following type was not found: " + typeName);
        }

        if (ignore) {
            SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
            typeMgr.setResourceTypeIgnoreFlagAndUninventoryResources(subjectMgr.getOverlord(), rt.getId(), true);
            rt = typeMgr.getResourceTypeByNameAndPlugin(typeName, PLUGIN_NAME);
            if (rt == null) {
                fail("Failed to reload type after ignoring it: " + typeName);
            }
            assert rt.isIgnored() : "Somehow the type didn't get ignored: " + rt;
        }

        return rt;
    }

    private void assertTypeDeleted(String typeName) {
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();
        ResourceType rt = resourceTypeMgr.getResourceTypeByNameAndPlugin(typeName, PLUGIN_NAME);
        if (rt != null) {
            fail("The following type was supposed to be deleted: " + typeName);
        }
        return;
    }

}
