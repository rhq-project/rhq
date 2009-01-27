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
package org.rhq.core.domain.event.test;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.transfer.EventReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

@Test
public class EventReportTest {
    public void testEventReport() {
        ResourceType resourceType = new ResourceType("foo", "foo", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource(1);
        EventDefinition eventDefinition = new EventDefinition(resourceType, "foo");
        EventSource eventSource = new EventSource("foo", eventDefinition, resource);
        EventReport report = new EventReport(10, 10);
        report.addEvent(new Event("foo", "foo", 0, EventSeverity.DEBUG, "foo-first", eventSource), eventSource);
        Map<EventSource, Set<Event>> allEvents = report.getEvents();
        assert allEvents.size() == 1;
        assert allEvents.get(eventSource).size() == 1;

        report.addEvent(new Event("foo", "foo", 1, EventSeverity.DEBUG, "foo-second", eventSource), eventSource);
        allEvents = report.getEvents();
        assert allEvents.size() == 1; // only one event source still!
        assert allEvents.get(eventSource).size() == 2;

        // make sure they are the ones we expect
        for (Event e : allEvents.get(eventSource)) {
            assert e.getDetail().startsWith("foo-");
        }

        // use a second event source
        EventSource eventSource2 = new EventSource("bar", eventDefinition, resource);
        report.addEvent(new Event("bar", "bar", 2, EventSeverity.DEBUG, "bar-first", eventSource2), eventSource2);
        allEvents = report.getEvents();
        assert allEvents.size() == 2;
        assert allEvents.get(eventSource).size() == 2;
        assert allEvents.get(eventSource2).size() == 1;

        // make sure they are the ones we expect
        for (Event e : allEvents.get(eventSource)) {
            assert e.getDetail().startsWith("foo-");
        }
        for (Event e : allEvents.get(eventSource2)) {
            assert e.getDetail().startsWith("bar-");
        }
    }

    public void testEventReportMaxPerSource() {
        ResourceType resourceType = new ResourceType("foo", "foo", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource(1);
        EventDefinition eventDefinition = new EventDefinition(resourceType, "foo");
        EventSource eventSource = new EventSource("foo", eventDefinition, resource);
        EventReport report = new EventReport(1, 10);

        // add the first
        report.addEvent(new Event("foo", "foo", 0, EventSeverity.DEBUG, "foo-first", eventSource), eventSource);
        Map<EventSource, Set<Event>> allEvents = report.getEvents();
        assert allEvents.size() == 1; // only one event source still!
        assert allEvents.get(eventSource).size() == 1;

        // add the second (this is over the max)
        report.addEvent(new Event("foo", "foo", 1, EventSeverity.DEBUG, "foo-second", eventSource), eventSource); // OVER MAX!
        allEvents = report.getEvents();
        assert allEvents.size() == 1; // only one event source still!
        assert allEvents.get(eventSource).size() == 2; // the second one is our "over the max" message
        int foo_count = 0;
        int limit_count = 0;
        for (Event e : allEvents.get(eventSource)) {
            if (e.getDetail().startsWith("foo-")) {
                foo_count++;
                continue;
            }
            if (e.getDetail().contains("Event Report Limit Reached:")) {
                limit_count++;
                continue;
            }
            assert false : "this event was unexpected: " + e;
        }
        assert foo_count == 1 : "there should have only been one of our events in the report: " + foo_count;
        assert limit_count == 1 : "there should have been an event warning of the limit breach: " + limit_count;

        // add the third (this is over the max)
        allEvents = report.getEvents();
        report.addEvent(new Event("foo", "foo", 2, EventSeverity.DEBUG, "foo-third", eventSource), eventSource); // STILL OVER MAX!
        assert allEvents.size() == 1; // only one event source still!
        assert allEvents.get(eventSource).size() == 2; // no others have been added
        foo_count = 0;
        limit_count = 0;
        for (Event e : allEvents.get(eventSource)) {
            if (e.getDetail().startsWith("foo-")) {
                foo_count++;
                continue;
            }
            if (e.getDetail().contains("Event Report Limit Reached:")) {
                limit_count++;
                continue;
            }
            assert false : "this event was unexpected: " + e;
        }

        // use a second event source - since we didn't hit our max total, this should work
        EventSource eventSource2 = new EventSource("bar", eventDefinition, resource);
        report.addEvent(new Event("bar", "bar", 2, EventSeverity.DEBUG, "bar-first", eventSource2), eventSource2);
        allEvents = report.getEvents();
        assert allEvents.size() == 2;
        assert allEvents.get(eventSource).size() == 2; // still have our two from above
        assert allEvents.get(eventSource2).size() == 1; // our new one

        // make sure they are the ones we expect
        for (Event e : allEvents.get(eventSource)) {
            assert e.getDetail().startsWith("foo-") || e.getDetail().contains("Event Report Limit Reached:");
        }
        for (Event e : allEvents.get(eventSource2)) {
            assert e.getDetail().startsWith("bar-");
        }
    }

    public void testEventReportMaxTotal() {
        ResourceType resourceType = new ResourceType("foo", "foo", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource(1);
        EventDefinition eventDefinition = new EventDefinition(resourceType, "foo");
        EventSource eventSource = new EventSource("foo", eventDefinition, resource);
        EventReport report = new EventReport(10, 1); // max total takes precedence!

        // add the first
        report.addEvent(new Event("foo", "foo", 0, EventSeverity.DEBUG, "foo-first", eventSource), eventSource);
        Map<EventSource, Set<Event>> allEvents = report.getEvents();
        assert allEvents.size() == 1; // only one event source still!
        assert allEvents.get(eventSource).size() == 1;

        // add the second (this is over the max)
        report.addEvent(new Event("foo", "foo", 1, EventSeverity.DEBUG, "foo-second", eventSource), eventSource); // OVER MAX!
        allEvents = report.getEvents();
        assert allEvents.size() == 1; // only one event source still!
        assert allEvents.get(eventSource).size() == 2; // the second one is our "over the max" message
        int foo_count = 0;
        int limit_count = 0;
        for (Event e : allEvents.get(eventSource)) {
            if (e.getDetail().startsWith("foo-")) {
                foo_count++;
                continue;
            }
            if (e.getDetail().contains("Event Report Limit Reached:")) {
                limit_count++;
                continue;
            }
            assert false : "this event was unexpected: " + e;
        }
        assert foo_count == 1 : "there should have only been one of our events in the report: " + foo_count;
        assert limit_count == 1 : "there should have been an event warning of the limit breach: " + limit_count;

        // use a second event source - since we are over the total, this should add nothing
        EventSource eventSource2 = new EventSource("bar", eventDefinition, resource);
        report.addEvent(new Event("bar", "bar", 2, EventSeverity.DEBUG, "bar-first", eventSource2), eventSource2);
        allEvents = report.getEvents();
        assert allEvents.size() == 1;
        assert allEvents.get(eventSource).size() == 2;
        assert allEvents.containsKey(eventSource2) == false;
    }
}
