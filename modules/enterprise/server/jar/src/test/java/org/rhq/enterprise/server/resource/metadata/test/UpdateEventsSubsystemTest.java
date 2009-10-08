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
import java.util.Set;

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class UpdateEventsSubsystemTest extends UpdateSubsytemTestBase {

    @Override
    protected String getSubsystemDirectory() {
        return "events";
    }

    @Test
    public void testCreateDeleteEvent() throws Exception {

        System.out.println("= testCreateDeleteEvent");
        getTransactionManager().begin();
        try {
            registerPlugin("event1-1.xml");
            ResourceType platform = getResourceType("events");
            assert platform != null;
            Set<EventDefinition> eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1;
            EventDefinition hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

            System.out.println("==> Done with v1");

            registerPlugin("event1-2.xml");
            platform = getResourceType("events");
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

            System.out.println("==> Done with v2");

            registerPlugin("event1-1.xml", "3.0");
            platform = getResourceType("events");
            eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1 : "Did not find 1 EventDefinition, but " + eDefs.size();
            hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

            System.out.println("==> Done with v1");

        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Simulate just redeploying the plugin with no change in descriptor.
     * @throws Exception
     */
    @Test
    public void testNoOpChange() throws Exception {
        System.out.println("= testNoOpChange");
        getTransactionManager().begin();
        try {
            registerPlugin("event1-1.xml");
            ResourceType platform = getResourceType("events");
            assert platform != null;
            Set<EventDefinition> eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1;
            EventDefinition hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

            System.out.println("==> Done with v1");

            registerPlugin("event1-1.xml", "2.0");
            platform = getResourceType("events");
            assert platform != null;
            eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1;
            hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

            System.out.println("==> Done with v2");
        } finally {
            getTransactionManager().rollback();
        }

    }

    @Test
    public void testDeleteEventStuff() throws Exception {
        System.out.println("= testDeleteEvent");
        EventManagerLocal eventManager = LookupUtil.getEventManager();

        ResourceType platform = null;
        Resource testResource = null;

        // prepare basic stuff
        getTransactionManager().begin();
        EntityManager entityManager = getEntityManager();
        try {
            registerPlugin("event1-2.xml");
            getPluginId(entityManager);

            platform = getResourceType("events");

            testResource = new Resource("-test-", "-test resource", platform);
            entityManager.persist(testResource);
            setUpAgent(entityManager, testResource);

            getTransactionManager().commit();
        } catch (Exception e) {
            getTransactionManager().rollback();
            throw e;
        }

        getTransactionManager().begin();
        // add event source + events
        try {
            platform = getResourceType("events");
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
        } catch (Exception e) {
            getTransactionManager().rollback();
            throw e;
        }

        Throwable savedThrowable = null;
        getTransactionManager().begin();
        try {

            /*
             * --- done with the setup ---
             * Now check that the event source + events are gone.
             */

            registerPlugin("event1-1.xml", "3.0");
            platform = getResourceType("events");

            Set<EventDefinition> eDefs = platform.getEventDefinitions();
            eDefs = platform.getEventDefinitions();
            assert eDefs != null;
            assert eDefs.size() == 1 : "Did not find 1 EventDefinition, but " + eDefs.size();
            EventDefinition hugo = eDefs.iterator().next();
            assert hugo.getDescription().equals("One");
            assert hugo.getDisplayName().equals("HugoOne");

        } catch (Throwable t) {
            savedThrowable = t;
        } finally {
            getTransactionManager().rollback();
        }

        // now clean up
        try {
            getTransactionManager().begin();

            entityManager = getEntityManager();
            resMgr = LookupUtil.getResourceManager();

            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            resMgr.deleteResource(overlord, testResource.getId());
            resMgr.deleteSingleResourceInNewTransaction(overlord, testResource.getId());

            /* deleting the platform deletes the agent now, so ask the resMgr to do it all for us
            platform = entityManager.getReference(ResourceType.class, platform.getId());
            entityManager.remove(platform);

            Agent agent = entityManager.getReference(Agent.class, agentId);
            entityManager.remove(agent);
            */

            Plugin plugin1 = entityManager.getReference(Plugin.class, plugin1Id);
            entityManager.remove(plugin1);

        } finally {
            getTransactionManager().commit();
        }

        if (savedThrowable != null)
            throw new Exception(savedThrowable);

    }
}
