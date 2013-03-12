/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * Tests around the event subsystem
 * @author Heiko W. Rupp
 */
public class EventManagerTest extends AbstractEJB3Test {

    private EventManagerLocal eventManager;
    private SubjectManagerLocal subjectManager;

    @Override
    protected void beforeMethod() {
        try {
            eventManager = LookupUtil.getEventManager();
            subjectManager = LookupUtil.getSubjectManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @Test
    public void testEventsSimple() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                Resource resource = SessionTestHelper.createNewResource(em, "fake resource");

                EventDefinition eDef = createEventDefinition(resource);
                em.persist(eDef);

                long now = System.currentTimeMillis();
                EventSource evSrc = new EventSource("ESource", eDef, resource);
                Event ev = new Event("EType", "ESource", now, EventSeverity.INFO, "This is a test", evSrc);
                //Set<Event> eventSet = newEventSet();
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
            }
        });
    }

    @Test
    public void testEventManager() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                Resource resource = SessionTestHelper.createNewResource(em, "fake resource");

                EventDefinition eDef = createEventDefinition(resource);
                em.persist(eDef);
                em.flush();

                long now = System.currentTimeMillis();
                EventSource evSrc = new EventSource("ESource", eDef, resource);
                Event ev = new Event("EType", "ESource", now, EventSeverity.INFO, "This is a 2nd test", evSrc);
                //em.persist(evSrc);
                //em.persist(ev);
                //em.flush();

                /*
                 * do NOT use addEventData method until this test is refactored to support the fact that
                 * insertions made via direct SQL won't be visible to the entity manager in this xaction
                 */
                eventManager.addEventData(newEventMap(evSrc, wrapEvents(ev)));
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

                List<EventComposite> res = eventManager.findEventComposites(overlord, context, t1, t2, null, null,
                    null, pc);
                assert res.size() == 1 : "Expected 1 Event, got " + res.size();
                res = eventManager.findEventComposites(overlord, context, t1, t2,
                    new EventSeverity[] { EventSeverity.INFO }, null, null, pc);
                assert res.size() == 1 : "Expected 1 Event, got " + res.size();
                res = eventManager.findEventComposites(overlord, context, t1, t2,
                    new EventSeverity[] { EventSeverity.WARN }, null, null, pc);
                assert res.size() == 0 : "Expected 0 Events, got " + res.size();

            }
        });
    }

    @Test
    public void testGetEventDetailForEventId() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Resource resource = SessionTestHelper.createNewResource(em, "fake resource");
                EventDefinition eventDefinition = createEventDefinition(resource);
                em.persist(eventDefinition);
                EventSource eventSource = createEventSource(eventDefinition, resource);
                em.persist(eventSource);
                Event event = createEvent(eventSource, EventSeverity.FATAL);
                em.persist(event);
                em.flush();
                EventComposite eventDetail = eventManager.getEventDetailForEventId(subjectManager.getOverlord(),
                    event.getId());
                assertNotNull(eventDetail);
                assertEquals(resource.getName(), eventDetail.getResourceName());
                assertEquals(event.getSourceLocation(), eventDetail.getSourceLocation());
                assertEquals(event.getSeverity(), eventDetail.getSeverity());
            }
        });
    }

    @Test
    public void testPurgeEventsForResourceContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Resource resource = SessionTestHelper.createNewResource(em, "fake resource");
                EventDefinition eventDefinition = createEventDefinition(resource);
                em.persist(eventDefinition);
                EventSource eventSource = createEventSource(eventDefinition, resource);
                em.persist(eventSource);
                int eventCount = 7;
                for (int i = 0; i < eventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.FATAL);
                    em.persist(event);
                }
                em.flush();
                int deletedCount = eventManager.purgeEventsForContext(subjectManager.getOverlord(),
                    EntityContext.forResource(resource.getId()));
                assertEquals(eventCount, deletedCount);
            }
        });
    }

    @Test
    public void testPurgeEventsForResourceGroupContext() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role");
                ResourceGroup resourceGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, role, "fake group");
                Resource resource = SessionTestHelper.createNewResourceForGroup(em, resourceGroup, "fake resource");
                EventDefinition eventDefinition = createEventDefinition(resource);
                em.persist(eventDefinition);
                EventSource eventSource = createEventSource(eventDefinition, resource);
                em.persist(eventSource);
                int eventCount = 7;
                for (int i = 0; i < eventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.FATAL);
                    em.persist(event);
                }
                em.flush();
                int deletedCount = eventManager.purgeEventsForContext(subjectManager.getOverlord(),
                    EntityContext.forGroup(resourceGroup.getId()));
                assertEquals(eventCount, deletedCount);
            }
        });
    }

    @Test
    public void testGetEventCountsBySeverity() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Resource resource = SessionTestHelper.createNewResource(em, "fake resource");
                EventDefinition eventDefinition = createEventDefinition(resource);
                em.persist(eventDefinition);
                EventSource eventSource = createEventSource(eventDefinition, resource);
                em.persist(eventSource);
                int fatalEventCount = 7;
                for (int i = 0; i < fatalEventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.FATAL);
                    em.persist(event);
                }
                int warnEventCount = 5;
                for (int i = 0; i < warnEventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.WARN);
                    em.persist(event);
                }
                int infoEventCount = 9;
                for (int i = 0; i < infoEventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.INFO);
                    em.persist(event);
                }
                em.flush();
                Map<EventSeverity, Integer> eventCountsBySeverity = eventManager.getEventCountsBySeverity(
                    subjectManager.getOverlord(), resource.getId(), 0, System.currentTimeMillis());
                assertEquals(3, eventCountsBySeverity.keySet().size());
                assertEquals(fatalEventCount, eventCountsBySeverity.get(EventSeverity.FATAL).intValue());
                assertEquals(warnEventCount, eventCountsBySeverity.get(EventSeverity.WARN).intValue());
                assertEquals(infoEventCount, eventCountsBySeverity.get(EventSeverity.INFO).intValue());
            }
        });
    }

    @Test
    public void testGetEventCountsBySeverityForGroup() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                Role role = SessionTestHelper.createNewRoleForSubject(em, subject, "fake role");
                ResourceGroup resourceGroup = SessionTestHelper.createNewCompatibleGroupForRole(em, role, "fake group");
                Resource resource = SessionTestHelper.createNewResourceForGroup(em, resourceGroup, "fake resource");
                EventDefinition eventDefinition = createEventDefinition(resource);
                em.persist(eventDefinition);
                EventSource eventSource = createEventSource(eventDefinition, resource);
                em.persist(eventSource);
                int fatalEventCount = 7;
                for (int i = 0; i < fatalEventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.FATAL);
                    em.persist(event);
                }
                int warnEventCount = 5;
                for (int i = 0; i < warnEventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.WARN);
                    em.persist(event);
                }
                int infoEventCount = 9;
                for (int i = 0; i < infoEventCount; i++) {
                    Event event = createEvent(eventSource, EventSeverity.INFO);
                    em.persist(event);
                }
                em.flush();
                Map<EventSeverity, Integer> eventCountsBySeverity = eventManager.getEventCountsBySeverityForGroup(
                    subjectManager.getOverlord(), resourceGroup.getId(), 0, System.currentTimeMillis());
                assertEquals(3, eventCountsBySeverity.keySet().size());
                assertEquals(fatalEventCount, eventCountsBySeverity.get(EventSeverity.FATAL).intValue());
                assertEquals(warnEventCount, eventCountsBySeverity.get(EventSeverity.WARN).intValue());
                assertEquals(infoEventCount, eventCountsBySeverity.get(EventSeverity.INFO).intValue());
            }
        });
    }

    @Test
    public void testFindEventsByCriteria() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Resource resource = SessionTestHelper.createNewResource(em, "fake resource");
                EventDefinition eventDefinition = createEventDefinition(resource);
                em.persist(eventDefinition);
                EventSource eventSource = createEventSource(eventDefinition, resource);
                em.persist(eventSource);
                Set<Event> events = newEventSet();
                for (int i = 0; i < 4; i++) {
                    Event event = createEvent(eventSource, EventSeverity.FATAL);
                    events.add(event);
                    em.persist(event);
                }
                for (int i = 0; i < 7; i++) {
                    Event event = createEvent(eventSource, EventSeverity.WARN);
                    events.add(event);
                    em.persist(event);
                }
                for (int i = 0; i < 13; i++) {
                    Event event = createEvent(eventSource, EventSeverity.INFO);
                    events.add(event);
                    em.persist(event);
                }
                em.flush();
                EventCriteria criteria = new EventCriteria();
                criteria.addFilterResourceId(resource.getId());
                criteria.setStrict(true);
                criteria.clearPaging();
                PageList<Event> foundEvents = eventManager.findEventsByCriteria(subjectManager.getOverlord(), criteria);
                assertNotNull(foundEvents);
                assertTrue("#findEventsByCriteria should have found all generated events",
                    events.containsAll(foundEvents) && foundEvents.containsAll(events));
            }
        });
    }

    private EventDefinition createEventDefinition(Resource resource) {
        return new EventDefinition(resource.getResourceType(), "fake event definition");
    }

    private EventSource createEventSource(EventDefinition eventDefinition, Resource resource) {
        return new EventSource("fake source location", eventDefinition, resource);
    }

    private Event createEvent(EventSource eventSource, EventSeverity eventSeverity) {
        return new Event("fake event type", eventSource.getLocation(), System.currentTimeMillis(), eventSeverity,
            "fake event detail", eventSource);
    }

    private Map<EventSource, Set<Event>> newEventMap(EventSource eventSource, Set<Event> events) {
        Map<EventSource, Set<Event>> eventMap = new HashMap<EventSource, Set<Event>>();
        eventMap.put(eventSource, events);
        return eventMap;
    }

    private Set<Event> wrapEvents(Event... event) {
        Set<Event> set = newEventSet();
        Collections.addAll(set, event);
        return set;
    }

    private Set<Event> newEventSet() {
        return new HashSet<Event>();
    }

}
