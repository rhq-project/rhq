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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.testng.annotations.Test;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Note, plugins are registered in new transactions. for tests, this means
 * you can't do everything in a trans and roll back at the end. You must clean up manually.
 */
public class UpdateEventsSubsystemTest extends UpdateSubsytemTestBase {

    @Override
    protected String getSubsystemDirectory() {
        return "events";
    }

    @Test
    public void testCreateDeleteEvent() throws Exception {
        System.out.println("= testCreateDeleteEvent");
        try {
            registerPlugin("event1-1.xml");
            ResourceType platform = getResourceType("events");
            assert platform != null;
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            Set<EventDefinition> eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1;
            EventDefinition hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");
            getTransactionManager().rollback();

            System.out.println("==> Done with v1");

            registerPlugin("event1-2.xml");
            platform = getResourceType("events");
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 2 : "Did not find 2 EventDefinitions, but " + eDefs.size();
            Iterator<EventDefinition> eIter = eDefs.iterator();
            while (eIter.hasNext()) {
                EventDefinition def = eIter.next();
                if (def.getName().equals("hugo")) {
                    assert def.getDescription().equals("Two") : "Expected 'Two', but got : " + def.getDescription();
                    assert def.getDisplayName().equals("HugoTwo");
                }
            }
            getTransactionManager().rollback();

            System.out.println("==> Done with v2");

            registerPlugin("event1-1.xml", "3.0");
            platform = getResourceType("events");
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1 : "Did not find 1 EventDefinition, but " + eDefs.size();
            hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

            System.out.println("==> Done with v1");

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testCreateDeleteEvent");
            }
        }
    }

    /**
     * Simulate just redeploying the plugin with no change in descriptor.
     * @throws Exception
     */
    @Test
    public void testNoOpChange() throws Exception {
        System.out.println("= testNoOpChange");
        try {
            registerPlugin("event1-1.xml");
            ResourceType platform = getResourceType("events");
            assert platform != null;
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            Set<EventDefinition> eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1;
            EventDefinition hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");
            getTransactionManager().rollback();

            System.out.println("==> Done with v1");

            registerPlugin("event1-1.xml", "2.0");
            platform = getResourceType("events");
            assert platform != null;
            getTransactionManager().begin();
            em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1;
            hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

            System.out.println("==> Done with v2");
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName() + ".testNoOpChange");
            }
        }
    }

    @Test
    public void testDeleteEventStuff() throws Exception {
        System.out.println("= testDeleteEvent");
        EventManagerLocal eventManager = LookupUtil.getEventManager();

        ResourceType platform = null;
        Resource testResource = null;
        EntityManager entityManager = null;

        // prepare basic stuff
        try {
            registerPlugin("event1-2.xml");
            getTransactionManager().begin();
            entityManager = getEntityManager();
            getPluginId(entityManager);

            platform = getResourceType("events");

            testResource = new Resource("-test-", "-test resource", platform);
            testResource.setUuid("" + new Random().nextInt());
            entityManager.persist(testResource);
            setUpAgent(entityManager, testResource);
            getTransactionManager().commit();

            getTransactionManager().begin();
            entityManager = getEntityManager();
            //platform = getResourceType("events");
            platform = entityManager.find(ResourceType.class, platform.getId());
            testResource = entityManager.find(Resource.class, testResource.getId());
            Set<EventDefinition> eDefs = platform.getEventDefinitions();
            assert eDefs.size() == 2 : "Did not find the expected 2 eventDefinitions, but " + eDefs.size();
            Iterator<EventDefinition> eIter = eDefs.iterator();
            boolean found = false;
            while (eIter.hasNext()) {
                EventDefinition def = eIter.next();
                if (def.getName().equals("hans")) {
                    found = true;
                    // We got the definition that will vanish later, so attach some stuff to it
                    EventSource source = new EventSource("test location", def, testResource);
                    entityManager.persist(source);
                    Event ev = new Event(def.getName(), source.getLocation(), System.currentTimeMillis(),
                        EventSeverity.INFO, "This is a test");
                    //                    entityManager.persist(ev);  // We can't do this, as Event.source does not get filled this way :(
                    Map<EventSource, Set<Event>> events = new HashMap<EventSource, Set<Event>>(1);
                    Set<Event> evSet = new HashSet<Event>(1);
                    evSet.add(ev);
                    events.put(source, evSet);
                    eventManager.addEventData(events);
                }
            }
            assert found : "Hans was not found";
            getTransactionManager().commit();

            /*
             * --- done with the setup ---
             * Now check that the event source + events are gone.
             */
            registerPlugin("event1-1.xml", "3.0");
            platform = getResourceType("events");
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            platform = em.find(ResourceType.class, platform.getId());

            eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1 : "Did not find 1 EventDefinition, but " + eDefs.size();
            EventDefinition hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testSingleSubCategoryCreate");
            }
        }
    }
}
