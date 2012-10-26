/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.enterprise.server.configuration;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.DynamicConfigurationPropertyValue;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class DynamicConfigurationPropertyBeanTest extends AbstractEJB3Test {

    private static final boolean ENABLED = true;

    private DynamicConfigurationPropertyLocal bean;

    @BeforeMethod
    public void setUp() {
        bean = LookupUtil.getDynamicConfigurationProperty();
    }

    @Test(enabled = ENABLED)
    public void invalidKey() throws Exception {
        // Test
        List<DynamicConfigurationPropertyValue> stringList = bean.lookupValues("foo");

        //Verify
        assert stringList != null;
        assert stringList.size() == 0;
    }

    @Test(enabled = ENABLED)
    public void user() throws Exception {
        String queryString = "SELECT s FROM Subject s WHERE s.fsystem = false";

        // Setup
        int existingUserCount = countForQuery(queryString, false);

        // Test
        List<DynamicConfigurationPropertyValue> users = bean.lookupValues(PropertyExpressionEvaluator.KEY_USERS);

        // Verify
        outputList(users);
        assert users.size() == existingUserCount : "Expected: " + existingUserCount + ", Found: " + users.size();
    }

    @Test(enabled = ENABLED)
    public void roles() throws Exception {
        // Setup
        int existingRoleCount = countForQuery(Role.QUERY_FIND_ALL);

        // Test
        List<DynamicConfigurationPropertyValue> roles = bean.lookupValues(PropertyExpressionEvaluator.KEY_ROLES);

        // Verify
        outputList(roles);
        assert roles.size() == existingRoleCount : "Expected: " + existingRoleCount + ", Found: " + roles.size();
    }

    @Test(enabled = ENABLED)
    public void packageType() throws Exception {
        // Setup
        ResourceType resourceType = null;
        PackageType packageType = null;

        TransactionManager tx = getTransactionManager();
        try {
            tx.begin();
            EntityManager entityManager = getEntityManager();

            resourceType = new ResourceType("dynamicConfigPropBeanTestType", "foo", ResourceCategory.PLATFORM, null);
            entityManager.persist(resourceType);

            packageType = new PackageType("dynamicConfigPropBeanTestType", resourceType);
            entityManager.persist(packageType);
            tx.commit();
        } catch (Exception e) {
            if (tx.getStatus() == Status.STATUS_ACTIVE) {
                tx.rollback();
            }
        }

        int existingPackageTypes = countForQuery(PackageType.QUERY_FIND_ALL);
        assert existingPackageTypes > 0 : "Package type created in setup was not written correctly";

        // Test
        List<DynamicConfigurationPropertyValue> types = bean
            .lookupValues(PropertyExpressionEvaluator.KEY_PACKAGE_TYPES);

        // Verify
        assert types.size() == existingPackageTypes : "Expected: " + existingPackageTypes + ", Found: " + types.size();

        // Clean up
        tx = getTransactionManager();
        try {
            tx.begin();
            EntityManager entityManager = getEntityManager();

            resourceType = entityManager.find(ResourceType.class, resourceType.getId());
            entityManager.remove(resourceType);

            packageType = entityManager.find(PackageType.class, packageType.getId());
            entityManager.remove(packageType);

            tx.commit();
        } catch (Exception e) {
            if (tx.getStatus() == Status.STATUS_ACTIVE) {
                tx.rollback();
            }
        }
    }

    private int countForQuery(String queryName) throws NotSupportedException, SystemException {
        return countForQuery(queryName, true);
    }

    @SuppressWarnings("unchecked")
    private int countForQuery(String queryString, boolean isName) throws NotSupportedException, SystemException {

        getTransactionManager().begin();
        EntityManager entityManager = getEntityManager();

        Query query = (isName) ? entityManager.createNamedQuery(queryString) : entityManager.createQuery(queryString);
        List existing = query.getResultList();

        int count = existing.size();

        getTransactionManager().rollback();

        return count;
    }

    private void outputList(List<DynamicConfigurationPropertyValue> items) {
        System.out.println("Items returned from lookupValues:");
        for (DynamicConfigurationPropertyValue item : items) {
            System.out.println(item);
        }
    }
}
