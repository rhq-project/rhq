/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.resource.test;

import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test
public class ResourceErrorTest extends AbstractEJB3Test {
    private Resource newResource;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        newResource = createNewResource();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        deleteNewResource(newResource);
    }

    public void testCreateErrors() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            ResourceError re;

            re = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary",
                "test detail", 12345);

            em.persist(re);

            ResourceError error = em.find(ResourceError.class, re.getId());
            assert error != null;
            assert error.getId() > 0;
            assert error.getResource().getId() == newResource.getId();
            assert error.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
            assert error.getSummary().equals("test summary");
            assert error.getDetail().equals("test detail");
            assert error.getTimeOccurred() == 12345;

            re = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary 2",
                "test detail 2", 56789);

            em.persist(re);

            error = em.find(ResourceError.class, re.getId());
            assert error != null;
            assert error.getId() > 0;
            assert error.getResource().getId() == newResource.getId();
            assert error.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
            assert error.getSummary().equals("test summary 2");
            assert error.getDetail().equals("test detail 2");
            assert error.getTimeOccurred() == 56789;

            Query q = em.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID);
            q.setParameter("resourceId", newResource.getId());
            assert q.getResultList().size() == 2;

            q = em.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPES);
            q.setParameter("resourceId", newResource.getId());
            q.setParameter("errorType", ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
            assert q.getResultList().size() == 2;
        } finally {
            getTransactionManager().rollback();
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public void testQueries() throws Exception {
        ResourceError re;
        Query q;
        List<ResourceError> errors;

        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            q = em.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID);
            q.setParameter("resourceId", newResource.getId());
            errors = q.getResultList();
            assert errors.size() == 0;

            re = new ResourceError(newResource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION, "test summary",
                "test detail", 12345);

            em.persist(re);
            errors = q.getResultList();
            assert errors.size() == 1;
            assert errors.get(0).getResource().getId() == newResource.getId();
            assert errors.get(0).getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
            assert errors.get(0).getSummary().equals("test summary");
            assert errors.get(0).getDetail().equals("test detail");
            assert errors.get(0).getTimeOccurred() == 12345;

            q = em.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPES);
            q.setParameter("resourceId", newResource.getId());
            q.setParameter("errorType", ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);
            errors = q.getResultList();
            assert errors.size() == 1;
            assert errors.get(0).getResource().getId() == newResource.getId();
            assert errors.get(0).getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION;
            assert errors.get(0).getSummary().equals("test summary");
            assert errors.get(0).getDetail().equals("test detail");
            assert errors.get(0).getTimeOccurred() == 12345;

        } finally {
            getTransactionManager().rollback();
            em.close();
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
                resource.setUuid("" + new Random().nextInt());
                em.persist(resource);
                System.out.println("Created resource with id " + resource.getId());
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
}