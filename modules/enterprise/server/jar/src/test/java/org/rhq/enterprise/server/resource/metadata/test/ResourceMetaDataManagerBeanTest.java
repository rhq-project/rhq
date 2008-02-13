/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.resource.metadata.test;

import java.util.List;
import java.util.Set;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;

public class ResourceMetaDataManagerBeanTest extends TestBase {
    @BeforeSuite
    @Override
    protected void init() {
        super.init();
    }

    @Test
    public void testSingleSubCategoryCreate() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/one-subcat-v1_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            assertAppsSubCategory(subCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testSingleSubCategoryAddFromEmpty() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/no-subcat.xml");

            registerPlugin("./test/metadata/one-subcat-v1_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            assertAppsSubCategory(subCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testSingleSubCategoryAddSibling() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/one-subcat-v1_0.xml");

            registerPlugin("./test/metadata/two-subcat.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 2, 0);
            assertAppsSubCategory(subCat);

            subCat = assertSubCategory(server1.getSubCategories(), 2, 1);
            assertServicesSubCategory(subCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testSingleSubCategoryReplace() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/one-subcat-v1_0.xml");

            registerPlugin("./test/metadata/one-subcat-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            assertServicesSubCategory(subCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testSingleSubCategoryRemoveOneFromTwo() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/two-subcat.xml");

            registerPlugin("./test/metadata/one-subcat-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            assertServicesSubCategory(subCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testSingleSubCategoryUpdate() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/one-subcat-v1_0.xml");

            registerPlugin("./test/metadata/one-subcat-v1_1.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            assertApps2SubCategory(subCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testSingleSubCategoryRemove() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/one-subcat-v1_0.xml");

            registerPlugin("./test/metadata/no-subcat.xml");

            ResourceType server1 = getResourceType("testServer1");

            assertSubCategory(server1.getSubCategories(), 0, null);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryReplace() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-v1_0.xml");

            registerPlugin("./test/metadata/nested-subcat-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            assertServicesSubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryAddFromEmpty() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/no-subcat.xml");

            registerPlugin("./test/metadata/nested-subcat-v1_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            assertAppsSubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryRemoveOneFromTwo() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-2children.xml");

            registerPlugin("./test/metadata/nested-subcat-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            assertServicesSubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryAddSibling() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-v2_0.xml");

            registerPlugin("./test/metadata/nested-subcat-2children.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 2, 1);
            assertAppsSubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryRemoveChild() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-grandchild.xml");

            registerPlugin("./test/metadata/nested-subcat-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertServicesSubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryAddChild() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-v2_0.xml");

            registerPlugin("./test/metadata/nested-subcat-grandchild.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);
            ResourceSubCategory grandChildSubCat = assertSubCategory(childSubCat.getChildSubCategories(), 1, 0);
            assertAppsSubCategory(grandChildSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryUpdate() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-v1_0.xml");

            registerPlugin("./test/metadata/nested-subcat-v1_1.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertApps2SubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryAdd() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/one-subcat-v3_0.xml");

            registerPlugin("./test/metadata/nested-subcat-v1_1.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertApps2SubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryCreate() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-v1_1.xml");

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            ResourceSubCategory childSubCat = assertSubCategory(subCat.getChildSubCategories(), 1, 0);

            assertApps2SubCategory(childSubCat);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryRemoveAll() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-v1_0.xml");

            registerPlugin("./test/metadata/no-subcat.xml");

            ResourceType server1 = getResourceType("testServer1");
            assertSubCategory(server1.getSubCategories(), 0, null);
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryCreateWithServices() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-services-v1_0.xml");

            ResourceType server1 = getResourceType("testServer1");
            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 3;

            ResourceType services = getResourceType("testService1");
            assertServicesSubCategory(services.getSubCategory());

            ResourceType apps = getResourceType("testApp1");
            assertAppsSubCategory(apps.getSubCategory());
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test
    public void testNestedSubCategoryCreateWithServices2() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-services-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");
            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 2;

            ResourceType services = getResourceType("testService2");
            assert services.getSubCategory().getName().equals("applications2");

            ResourceType apps = getResourceType("testApp1");
            assert apps.getSubCategory().getName().equals("applications2");
        } finally {
            getTransactionManager().rollback();
        }
    }

    // TODO this currently fails
    //javax.persistence.EntityNotFoundException: Unable to find org.jboss.on.domain.resource.ResourceSubCategory with id 527082
    // when calling  assert children.size() == 2;
    // this test case needs to be deconstructed down to its simplest form
    //@Test
    public void testNestedSubCategoryUpdateWithServices() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/nested-subcat-services-v1_0.xml");
            registerPlugin("./test/metadata/nested-subcat-services-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");
            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 2;

            ResourceType services = getResourceType("testService2");
            assert services.getSubCategory().getName().equals("applications2");

            ResourceType apps = getResourceType("testApp1");
            assert apps.getSubCategory().getName().equals("applications2");
        } finally {
            getTransactionManager().rollback();
        }
    }

    //TODO this should be moved to its own test class
    @Test
    public void testRemoveService() throws Exception {
        getTransactionManager().begin();
        try {
            registerPlugin("./test/metadata/services-v1_0.xml");
            registerPlugin("./test/metadata/services-v2_0.xml");

            ResourceType server1 = getResourceType("testServer1");
            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 2 : "Incorrect number of child resource types, should have been 2 but was ["
                + children.size() + "].";
        } finally {
            getTransactionManager().rollback();
        }
    }

    //@Test
    public void testSimpleSubCategoryCreate() throws Exception {
        getTransactionManager().begin();
        try {
            Plugin testPlugin = new Plugin("ResourceMetaDataManagerBeanTest", "foo.jar", "123561RE1652EF165E");
            PluginDescriptor descriptor = loadPluginDescriptor("./test/metadata/test-subcategories.xml");
            metadataManager.registerPlugin(testPlugin, descriptor);

            getEntityManager().flush();

            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            assert subCat.getName().equals("applications");

            ResourceType server2 = getResourceType("testServer2");

            assert server2.getSubCategories() != null;
            assert server2.getSubCategories().size() == 2 : "Unexpected number of subcategories ["
                + server2.getSubCategories().size() + "]";
            subCat = server2.getSubCategories().get(1);
            assert subCat.getName().equals("resource");

            List<ResourceSubCategory> childSubCats = subCat.getChildSubCategories();
            assert childSubCats.size() == 2;
            subCat = childSubCats.get(1);
            assert subCat.getName().equals("destinations");
        } finally {
            getTransactionManager().rollback();
        }
    }

    //@Test
    public void testSimpleSubCategoryUpdate() throws Exception {
        getTransactionManager().begin();
        try {
            Plugin testPlugin = new Plugin("ResourceMetaDataManagerBeanTest", "foo.jar", "123561RE1652EF165E");
            PluginDescriptor descriptor = loadPluginDescriptor("./test/metadata/test-subcategories.xml");
            metadataManager.registerPlugin(testPlugin, descriptor);

            getEntityManager().flush();

            // pretend to be an updated plugin
            testPlugin = new Plugin("ResourceMetaDataManagerBeanTest", "foo.jar", "222222222");
            descriptor = loadPluginDescriptor("./test/metadata/test-subcategories2.xml");
            metadataManager.registerPlugin(testPlugin, descriptor);

            // now test how the subcategories got updated
            ResourceType server1 = getResourceType("testServer1");

            ResourceSubCategory subCat = assertSubCategory(server1.getSubCategories(), 1, 0);
            assertApps2SubCategory(subCat);

            ResourceType server2 = getResourceType("testServer2");

            assert server2.getSubCategories() != null;
            // TODO somehow we lose both the old services subcat
            // and also the new service2 subcat we tried to add
            // maybe we could rework the example to this check in

            assert server2.getSubCategories().size() == 2 : "Unexpected number of subcategories ["
                + server2.getSubCategories().size() + "]";
            subCat = server2.getSubCategories().get(0);
            assert subCat.getName().equals("services2");
            subCat = server2.getSubCategories().get(1);
            assert subCat.getName().equals("resource");

            List<ResourceSubCategory> childSubCats = subCat.getChildSubCategories();
            assert childSubCats.size() == 2;
            subCat = childSubCats.get(1);
            assert subCat.getName().equals("destinations");
        } finally {
            getTransactionManager().rollback();
        }
    }

    private ResourceSubCategory assertSubCategory(List<ResourceSubCategory> subCats, Integer size, Integer index) {
        assert subCats != null;
        assert subCats.size() == size : "Unexpected number of Sub categories. Expected [" + size + "] got ["
            + subCats.size() + "].";
        ResourceSubCategory subCat = null;
        if (index != null) {
            subCat = subCats.get(index);
        }

        return subCat;
    }

    private void assertServicesSubCategory(ResourceSubCategory subCat) {
        assert subCat != null;
        assert subCat.getName().equals("services");
        assert subCat.getDisplayName().equals("Services");
        assert subCat.getDescription().equals("Some services");
    }

    private void assertAppsSubCategory(ResourceSubCategory subCat) {
        assert subCat != null;
        assert subCat.getName().equals("applications");
        assert subCat.getDisplayName().equals("Apps");
        assert subCat.getDescription().equals("The apps.");
    }

    private void assertApps2SubCategory(ResourceSubCategory subCat) {
        assert subCat != null;
        assert subCat.getName().equals("applications");
        assert subCat.getDisplayName().equals("Apps2");
        assert subCat.getDescription().equals("The apps2.");
    }

    //   @Test(groups = "integration.session")
    //   public void testGetCompatibleGroupById() throws Exception
    //   {
    //      getTransactionManager().begin();
    //      try
    //      {
    //         EntityManager em = getEntityManager();
    //
    //         /* bootstrap */
    //         ResourceType type = new ResourceType("type", "plugin", ResourceCategory.PLATFORM, null);
    //         Subject testSubject = SessionTestHelper.createNewSubject(em, "testSubject");
    //         Role testRole = SessionTestHelper.createNewRoleForSubject(em, testSubject, "testRole");
    //         CompatibleGroup compatGroup = new CompatibleGroup("testCompatGroup", type);
    //         compatGroup.addRole(testRole);
    //         em.persist(type);
    //         em.persist(compatGroup);
    //         em.flush();
    //         testRole.addResourceGroup(compatGroup);
    //         em.merge(testRole);
    //         em.flush();
    //
    //         int id = compatGroup.getId();
    //         try {
    //            resourceGroupManager.getCompatibleGroupById(testSubject, id);
    //         } catch (ResourceGroupNotFoundException e) {
    //            assert (false) : "Could not find recently persisted compatible group by id";
    //         } catch (PermissionException se) {
    //            assert (false) : "Incorrect permissions when getting compatible group by id";
    //         }
    //
    //      }
    //      finally
    //      {
    //         getTransactionManager().rollback();
    //      }
    //   }

    /*
     * CGroup  getCompatibleGroupById(Subject user, int id) MGroup  getMixedGroupById(Subject user, int id) List<>
     * getCompatibleGroupsByResourceType(Subject user, ResourceType type, PC) int     getCompatibleGroupCount(Subject
     * user) int     getMixedGroupCount(Subject user)
     */
}