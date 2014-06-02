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

import java.util.Set;

import javax.transaction.Status;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.ResourceType;

/**
 * Note, plugins are registered in new transactions. for tests, this means
 * you can't do everything in a trans and roll back at the end. You must clean up manually.
 */
public class UpdateResourceSubsystemTest extends UpdatePluginMetadataTestBase {

    private static final boolean ENABLED = true;

    @Override
    protected String getSubsystemDirectory() {
        return "resource";
    }


    @Test(enabled = ENABLED)
    public void testSubcategoryAddFromEmpty() throws Exception {
        try {
            registerPlugin("no-subcat.xml", "0.0");
            registerPlugin("nested-subcat-services-v1_0.xml");
            getTransactionManager().begin();

            ResourceType service1 = getResourceType("testService1");
            service1 = em.find(ResourceType.class, service1.getId());
            assert service1.getSubCategory().equals("services");

            ResourceType service2 = getResourceType("testService2");
            service2 = em.find(ResourceType.class, service2.getId());
            assert service2.getSubCategory().equals("services");

            ResourceType app1 = getResourceType("testApp1");
            app1 = em.find(ResourceType.class, app1.getId());
            assert app1.getSubCategory().equals("applications");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testNoSubcategory() throws Exception {
        try {
            registerPlugin("no-subcat.xml");
            getTransactionManager().begin();

            ResourceType server1 = getResourceType("testServer1");
            server1 = em.find(ResourceType.class, server1.getId());
            assert server1.getSubCategory() == null;
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testOldSubcategory() throws Exception {
        try {
            registerPlugin("old-subcat.xml");
            getTransactionManager().begin();

            ResourceType server1 = getResourceType("testServer1");
            server1 = em.find(ResourceType.class, server1.getId());
            assert server1.getSubCategory() == null;
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Test(enabled = true)
    public void testNestedSubcategories() throws Exception {
        try {
            registerPlugin("nested-subcat-services-v2_0.xml");
            getTransactionManager().begin();

            ResourceType server1 = getResourceType("testServer1");
            server1 = em.find(ResourceType.class, server1.getId());
            assert server1.getSubCategory() == null;
            //test to make sure old remantants like subcategories are not populated anymore
            assert server1.getChildSubCategories() == null;


            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 2;

            ResourceType service = getResourceType("testService2");
            assert service.getSubCategory().equals("application|applications2");

            ResourceType app = getResourceType("testApp1");
            assert app.getSubCategory().equals("application|applications2");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSubcategoryUpdate() throws Exception {
        try {
            registerPlugin("nested-subcat-services-v1_0.xml", "1.0");
            // pretend to be an updated plugin
            registerPlugin("nested-subcat-services-v2_0.xml", "2.0");

            getTransactionManager().begin();

            // now test how the subcategories got updated
            ResourceType server1 = getResourceType("testServer1");
            server1 = em.find(ResourceType.class, server1.getId());

            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 2;

            ResourceType service = getResourceType("testService2");
            assert service.getSubCategory().equals("application|applications2");

            ResourceType app = getResourceType("testApp1");
            assert app.getSubCategory().equals("application|applications2");

            ResourceType server2 = getResourceType("testServer2");
            server2 = em.find(ResourceType.class, server2.getId());
            assert server2.getSubCategory() == null;
            assert server2.getChildResourceTypes().size() == 0;
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testSubcategoryUpdateMore() throws Exception {
        try {
            registerPlugin("nested-subcat-services-v1_0.xml", "1.0");
            registerPlugin("nested-subcat-services-v2_0.xml", "2.0");
            registerPlugin("nested-subcat-services-v3_0.xml", "3.0");

            getTransactionManager().begin();

            // now test how the subcategories got updated
            ResourceType server1 = getResourceType("testServer1");
            server1 = em.find(ResourceType.class, server1.getId());

            Set<ResourceType> children = server1.getChildResourceTypes();
            assert children.size() == 4;

            ResourceType service1 = getResourceType("testService1");
            assert service1.getSubCategory().equals("resources|services");

            ResourceType service2 = getResourceType("testService2");
            assert service2.getSubCategory().equals("resources|services");

            ResourceType app1 = getResourceType("testApp1");
            assert app1.getSubCategory().equals("resources|applications");

            ResourceType app2 = getResourceType("testApp2");
            assert app2.getSubCategory().equals("resources|applications");

            ResourceType server2 = getResourceType("testServer2");
            server2 = em.find(ResourceType.class, server2.getId());
            assert server2.getSubCategory() == null;
            assert server2.getChildResourceTypes().size() == 1;

            ResourceType app3 = getResourceType("testApp3");
            assert app3.getSubCategory().equals("resources|applications");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
        }
    }

    @Test(enabled = ENABLED)
    public void testReferenceToUndefinedChildSubCategory() throws Exception {
        System.out.println("= testReferenceToUndefinedChildSubCategory");
        try {
            try {
                registerPlugin("undefined-child-subcat-1.xml");
            } catch (Exception ignored) {
                fail("Exception was thrown.");
            }
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
        }
    }
}