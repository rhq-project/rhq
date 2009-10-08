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
package org.rhq.enterprise.server.content.test;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.ProductVersionManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ProductVersionManagerBeanTest extends AbstractEJB3Test {
    // Attributes  --------------------------------------------

    private ResourceType resourceType;

    private ProductVersionManagerLocal productManager;

    // Setup  --------------------------------------------

    @BeforeClass
    public void setupBeforeClass() throws Exception {
        productManager = LookupUtil.getProductVersionManager();
    }

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        setupTestEnvironment();
    }

    @AfterMethod
    public void teardownAfterMethod() throws Exception {
        teardownTestEnvironment();
    }

    // Test Cases  --------------------------------------------

    @Test
    public void addNewProductVersion() throws Exception {
        // Test
        productManager.addProductVersion(resourceType, "1.0.0");

        // Verify
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                // Verify
                resourceType = em.find(ResourceType.class, resourceType.getId());

                Query query = em.createNamedQuery(ProductVersion.QUERY_FIND_BY_RESOURCE_TYPE_AND_VERSION);
                query.setParameter("resourceType", resourceType);
                query.setParameter("version", "1.0.0");

                List addedProductVersion = query.getResultList();

                assert addedProductVersion.size() == 1 : "Incorrect number of versions persisted. Expected: 1, Found: "
                    + addedProductVersion.size();

                // Clean up
                em.remove(addedProductVersion.get(0));

                getTransactionManager().commit();
            } catch (Exception e) {
                getTransactionManager().rollback();
            }
        } finally {
            em.close();
        }
    }

    @Test
    public void addExistingProductVersion() throws Exception {
        // Setup
        productManager.addProductVersion(resourceType, "1.0.0");
        productManager.addProductVersion(resourceType, "1.0.0");

        // Verify
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                // Verify
                resourceType = em.find(ResourceType.class, resourceType.getId());

                Query query = em.createNamedQuery(ProductVersion.QUERY_FIND_BY_RESOURCE_TYPE_AND_VERSION);
                query.setParameter("resourceType", resourceType);
                query.setParameter("version", "1.0.0");

                List addedProductVersion = query.getResultList();

                assert addedProductVersion.size() == 1 : "Incorrect number of versions persisted. Expected: 1, Found: "
                    + addedProductVersion.size();

                // Clean up
                em.remove(addedProductVersion.get(0));

                getTransactionManager().commit();
            } catch (Exception e) {
                getTransactionManager().rollback();
            }
        } finally {
            em.close();
        }
    }

    // Private  --------------------------------------------

    private void setupTestEnvironment() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                resourceType = new ResourceType("testResourceType", "testPlugin", ResourceCategory.PLATFORM, null);

                em.persist(resourceType);

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            em.close();
        }
    }

    private void teardownTestEnvironment() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                resourceType = em.find(ResourceType.class, resourceType.getId());
                em.remove(resourceType);

                getTransactionManager().commit();
            } catch (Exception e) {
                e.printStackTrace();
                getTransactionManager().rollback();
                throw e;
            }
        } finally {
            em.close();
        }
    }
}