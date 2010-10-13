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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

public class ResourceTest extends AbstractEJB3Test {

    @Test(groups = "integration.ejb3")
    public void testCreateAlotOfResources() throws Exception {
        Agent theAgent;
        ResourceType theResourceType;
        List<Resource> resources = new ArrayList<Resource>();
        long start;
        final int count = 2000;

        EntityManager em = getEntityManager();

        start = System.currentTimeMillis();
        getTransactionManager().begin();
        try {
            theAgent = new Agent("testCreateAlotOfResources", "localhost", 1234, "", "anotherToken");
            em.persist(theAgent);

            theResourceType = new ResourceType("testCreateAlotOfResources", "test-plugin", ResourceCategory.PLATFORM,
                null);
            em.persist(theResourceType);

            for (int i = 0; i < count; i++) {
                Resource newResource = new Resource("testCreateAlotOfResources" + i, "res" + i, theResourceType);
                newResource.setAgent(theAgent);
                em.persist(newResource);
                resources.add(newResource);
            }
        } finally {
            getTransactionManager().commit();
        }

        System.out.println("Took [" + (System.currentTimeMillis() - start) + "]ms to create [" + count + "] resources");

        start = System.currentTimeMillis();
        getTransactionManager().begin();
        try {
            for (int i = 0; i < count; i++) {
                Resource doomedResource = em.find(Resource.class, resources.get(i).getId());
                em.remove(doomedResource);
            }

            em.remove(em.find(ResourceType.class, theResourceType.getId()));
            em.remove(em.find(Agent.class, theAgent.getId()));
        } finally {
            getTransactionManager().commit();
        }

        System.out.println("Took [" + (System.currentTimeMillis() - start) + "]ms to remove [" + count + "] resources");
    }

    @Test(groups = "integration.ejb3")
    public void testCreateAlotOfResourcesWithAvailabilities() throws Exception {
        Agent theAgent;
        ResourceType theResourceType;
        List<Resource> resources = new ArrayList<Resource>();
        long start;
        final int count = 2000;

        EntityManager em = getEntityManager();

        start = System.currentTimeMillis();
        getTransactionManager().begin();
        try {
            theAgent = new Agent("testCreateAlotOfResources", "localhost", 1234, "", "someToken");
            em.persist(theAgent);

            theResourceType = new ResourceType("testCreateAlotOfResources", "test-plugin", ResourceCategory.PLATFORM,
                null);
            em.persist(theResourceType);

            for (int i = 0; i < count; i++) {
                Resource newResource = new Resource("testCreateAlotOfResources" + i, "res" + i, theResourceType);
                newResource.setAgent(theAgent);
                em.persist(newResource);
                resources.add(newResource);

                em.persist(new Availability(newResource, new Date(), AvailabilityType.UP));
            }
        } finally {
            getTransactionManager().commit();
        }

        System.out.println("Took [" + (System.currentTimeMillis() - start) + "]ms to create [" + count
            + "] resources with availabilities");

        start = System.currentTimeMillis();
        getTransactionManager().begin();
        try {
            for (int i = 0; i < count; i++) {
                Resource doomedResource = em.find(Resource.class, resources.get(i).getId());
                em.remove(doomedResource);
            }

            em.remove(em.find(ResourceType.class, theResourceType.getId()));
            em.remove(em.find(Agent.class, theAgent.getId()));
        } finally {
            getTransactionManager().commit();
        }

        System.out.println("Took [" + (System.currentTimeMillis() - start) + "]ms to remove [" + count
            + "] resources with availabilities");
    }

    @Test(groups = "integration.ejb3")
    public void testMediumPlatformCreate() {
        int id = 0;
        ResourceType platformType = new ResourceType("FakePlatform", "FakePlugin", ResourceCategory.PLATFORM, null);
        ResourceType serverType = new ResourceType("FakeServer", "FakePlugin", ResourceCategory.SERVER, platformType);
        ResourceType serviceType = new ResourceType("FakeService", "FakePlugin", ResourceCategory.SERVICE, serverType);
        Resource p = new Resource("Test.fqdn", "TestPlatform", platformType);
        Resource s = new Resource("key", "name", serverType);
        p.addChildResource(s);
        for (int i = 0; i < 100; i++) {
            String name = "TestService" + i;
            Resource v = new Resource(name, name, serviceType);
            v.setId(id++);
            s.addChildResource(v);
        }

        //getEntityManager().persist(p);
    }

    @Test(groups = "integration.ejb3")
    public void testCreateMultiParentTypes() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            ResourceType parent1 = new ResourceType("parent1", "a", ResourceCategory.SERVER, null);
            ResourceType parent2 = new ResourceType("parent2", "a", ResourceCategory.SERVER, null);
            ResourceType child = new ResourceType("child", "a", ResourceCategory.SERVER, null);

            em.persist(parent1);
            em.persist(parent2);
            em.persist(child);

            child.addParentResourceType(parent1);
            child = em.merge(child);

            assert child.getParentResourceTypes().size() == 1;
            assert parent1.getChildResourceTypes().size() == 1;
            assert child.getParentResourceTypes().iterator().next().equals(parent1);
            assert parent1.getChildResourceTypes().iterator().next().equals(child);
            assert child.getChildResourceTypes().size() == 0;
            assert parent1.getParentResourceTypes().size() == 0;

            Query query = em.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN);
            query.setParameter("name", "child").setParameter("plugin", "a");
            child = (ResourceType) query.getSingleResult();
            query.setParameter("name", "parent1").setParameter("plugin", "a");
            parent1 = (ResourceType) query.getSingleResult();

            assert child.getParentResourceTypes().size() == 1;
            assert parent1.getChildResourceTypes().size() == 1;
            assert child.getParentResourceTypes().iterator().next().equals(parent1);
            assert parent1.getChildResourceTypes().iterator().next().equals(child);
            assert child.getChildResourceTypes().size() == 0;
            assert parent1.getParentResourceTypes().size() == 0;

            // now add our second parent
            child.addParentResourceType(parent2);
            child = em.merge(child);

            assert child.getParentResourceTypes().size() == 2;
            assert parent1.getChildResourceTypes().size() == 1;
            assert parent2.getChildResourceTypes().size() == 1;
            assert child.getParentResourceTypes().contains(parent1);
            assert child.getParentResourceTypes().contains(parent2);
            assert parent1.getChildResourceTypes().contains(child);
            assert parent2.getChildResourceTypes().contains(child);
            assert child.getChildResourceTypes().size() == 0;
            assert parent1.getParentResourceTypes().size() == 0;
            assert parent2.getParentResourceTypes().size() == 0;

            query.setParameter("name", "child").setParameter("plugin", "a");
            child = (ResourceType) query.getSingleResult();
            query.setParameter("name", "parent1").setParameter("plugin", "a");
            parent1 = (ResourceType) query.getSingleResult();
            query.setParameter("name", "parent2").setParameter("plugin", "a");
            parent2 = (ResourceType) query.getSingleResult();

            assert child.getParentResourceTypes().size() == 2;
            assert parent1.getChildResourceTypes().size() == 1;
            assert parent2.getChildResourceTypes().size() == 1;
            assert child.getParentResourceTypes().contains(parent1);
            assert child.getParentResourceTypes().contains(parent2);
            assert parent1.getChildResourceTypes().contains(child);
            assert parent2.getChildResourceTypes().contains(child);
            assert child.getChildResourceTypes().size() == 0;
            assert parent1.getParentResourceTypes().size() == 0;
            assert parent2.getParentResourceTypes().size() == 0;
        } finally {
            getTransactionManager().rollback();
        }
    }
}