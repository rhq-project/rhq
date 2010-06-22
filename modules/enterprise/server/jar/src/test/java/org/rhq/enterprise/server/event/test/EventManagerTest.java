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
package org.rhq.enterprise.server.event.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Tests around the event subsystem 
 * @author Heiko W. Rupp
 */
public class EventManagerTest extends AbstractEJB3Test {

    EventManagerLocal eventManager;
    EntityManager em;

    @BeforeSuite
    public void init() {
        try {
            eventManager = LookupUtil.getEventManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventsSimple() throws Exception {

        try {
            getTransactionManager().begin();
            em = getEntityManager();
            Resource resource = setUpResource(em);

            EventDefinition eDef = new EventDefinition(resource.getResourceType(), "My definition is this ..");
            em.persist(eDef);

            long now = System.currentTimeMillis();
            EventSource evSrc = new EventSource("ESource", eDef, resource);
            Event ev = new Event("EType", "ESource", now, EventSeverity.INFO, "This is a test", evSrc);
            //Set<Event> eventSet = new HashSet<Event>();
            //eventSet.add(ev);
            //Map<EventSource, Set<Event>> events = new HashMap<EventSource, Set<Event>>();
            //events.put(evSrc, eventSet);
            em.persist(evSrc);
            em.persist(ev);
            em.flush();

            /* 
             * do NOT use addEventData method until this test is refactored to support the fact that
             * insertions made via direct SQL won't be visible to the entity manager in this xaction
             */
            //eventManager.addEventData(events);
            int resourceId = resource.getId();
            Query queryByTime = em.createNamedQuery(Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME);
            long t1 = now - 1000L;
            long t2 = now + 1000L;
            queryByTime.setParameter("resourceId", resourceId);
            queryByTime.setParameter("start", t1);
            queryByTime.setParameter("end", t2);
            List resultsByTime = queryByTime.getResultList();
            assert resultsByTime.size() == 1 : "Expected 1 Event, got " + resultsByTime.size();

            Query queryBySeverity = em.createNamedQuery(Event.FIND_EVENTS_FOR_RESOURCE_ID_AND_TIME_SEVERITY);
            queryBySeverity.setParameter("severity", EventSeverity.INFO);
            queryBySeverity.setParameter("resourceId", resourceId);
            queryBySeverity.setParameter("start", t1);
            queryBySeverity.setParameter("end", t2);
            List resultsBySeverity = queryBySeverity.getResultList();
            assert resultsBySeverity.size() == 1 : "Expected 1 Event, got " + resultsBySeverity.size();

        } finally {
            getTransactionManager().rollback();
            em.close();
        }
    }

    @Test
    public void testEventManager() throws Exception {

        try {
            getTransactionManager().begin();
            em = getEntityManager();
            Resource resource = setUpResource(em);

            EventDefinition eDef = new EventDefinition(resource.getResourceType(), "My definition is this ..");
            em.persist(eDef);
            em.flush();

            long now = System.currentTimeMillis();
            EventSource evSrc = new EventSource("ESource", eDef, resource);
            Event ev = new Event("EType", "ESource", now, EventSeverity.INFO, "This is a 2nd test", evSrc);
            Set<Event> eventSet = new HashSet<Event>();
            eventSet.add(ev);
            Map<EventSource, Set<Event>> events = new HashMap<EventSource, Set<Event>>();
            events.put(evSrc, eventSet);
            //em.persist(evSrc);
            //em.persist(ev);
            //em.flush();

            /* 
             * do NOT use addEventData method until this test is refactored to support the fact that
             * insertions made via direct SQL won't be visible to the entity manager in this xaction
             */
            eventManager.addEventData(events);
            int resourceId = resource.getId();
            long t1 = now - 1000L;
            long t2 = now + 1000L;

            Subject overlord = LookupUtil.getSubjectManager().getOverlord();
            int[] buckets = eventManager.getEventCounts(overlord, resourceId, t1, t2, 3);
            assert buckets != null : "Buckets should not be null, but were null";
            assert buckets.length == 3 : "Expected 3 buckets, but got " + buckets.length;

            boolean bucketCounts = buckets[0] == 0 && buckets[1] == 1 && buckets[2] == 0;
            assert bucketCounts : "Expected bucket counts were [0 1 0] Received [" + buckets[0] + " " + buckets[1]
                + " " + buckets[2] + "]";

            PageControl pc = PageControl.getUnlimitedInstance();
            EntityContext context = EntityContext.forResource(resourceId);

            List<EventComposite> res = eventManager
                .findEventComposites(overlord, context, t1, t2, null, null, null, pc);
            assert res.size() == 1 : "Expected 1 Event, got " + res.size();
            res = eventManager.findEventComposites(overlord, context, t1, t2,
                new EventSeverity[] { EventSeverity.INFO }, null, null, pc);
            assert res.size() == 1 : "Expected 1 Event, got " + res.size();
            res = eventManager.findEventComposites(overlord, context, t1, t2,
                new EventSeverity[] { EventSeverity.WARN }, null, null, pc);
            assert res.size() == 0 : "Expected 0 Events, got " + res.size();

        } finally {
            getTransactionManager().rollback();
            em.close();
        }
    }

    private Resource setUpResource(EntityManager em) {
        ResourceType resourceType = new ResourceType("fake platform", "fake plugin", ResourceCategory.PLATFORM, null);
        em.persist(resourceType);
        Resource platform = new Resource("org.jboss.on.TestPlatform", "Fake Platform", resourceType);
        platform.setUuid("" + new Random().nextInt());
        em.persist(platform);
        em.flush();

        return platform;
    }

}
