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
package org.rhq.core.domain.resource.test;

import javax.persistence.EntityManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.test.AbstractEJB3Test;

public class ResourceGroupTest extends AbstractEJB3Test {
    private Resource newResource;
    private ResourceGroup newGroup;

    @Test(groups = "integration.ejb3")
    public void testCreate() throws Exception {
        EntityManager em = getEntityManager();
        getTransactionManager().begin();
        try {
            assert newResource != null;
            assert newGroup != null;
            assert newResource.getId() > 0;
            assert newGroup.getId() > 0;

            assert newGroup.getExplicitResources() != null;
            assert newGroup.getExplicitResources().size() == 1;
            assert newGroup.getExplicitResources().contains(newResource);

            assert newResource.getExplicitGroups() != null;
            assert newResource.getExplicitGroups().size() == 1;
            assert newResource.getExplicitGroups().iterator().next().getId() == newGroup.getId();
        } finally {
            getTransactionManager().rollback();
        }
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                    ResourceCategory.PLATFORM, null);
                em.persist(resourceType);

                resource = new Resource("key" + System.currentTimeMillis(), "name", resourceType);
                em.persist(resource);

                ResourceGroup group = new ResourceGroup("testgroup" + System.currentTimeMillis(), resourceType);
                em.persist(group);
                group.addExplicitResource(resource);
            } catch (Exception e) {
                System.out.println(e);
                getTransactionManager().rollback();
                throw e;
            }

            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (resource != null) {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            try {
                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Resource res = em.find(Resource.class, resource.getId());
                ResourceGroup group = res.getExplicitGroups().iterator().next();

                group.removeExplicitResource(resource);
                em.remove(group);
                em.remove(res);
                em.remove(type);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println(e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }

                throw e;
            } finally {
                em.close();
            }
        }
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        try {
            newResource = createNewResource();
            newGroup = newResource.getExplicitGroups().iterator().next();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        deleteNewResource(newResource);
    }
}