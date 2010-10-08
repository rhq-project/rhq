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
package org.rhq.core.domain.event.transfer;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;

/**
 * A report of resource {@link Event}s that the Plugin Container periodically sends to the Server. The report contains
 * all Events that have occurred since the last time a report was successfully sent.
 *
 * Each report has a limit on the number of events they can hold. There are actually two limits: first,
 * each event source has a limit on the number of events they can have and second, the report has a total
 * number of events it can hold at a maximum (i.e. the sum of all events for all event sources cannot
 * exceed this total maximum).
 *   
 * This report maintains a counter of the number of events that have been dropped due to hitting these
 * maximum limits. If you want this report to get additional warning events added to them to indicate
 * if these maximum limits were exceeded, call {@link #addLimitWarningEvents()}.
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class EventReport implements Serializable {
    private static final long serialVersionUID = 2L;

    private final int maxEventsPerSource;
    private final int maxEventsPerReport;

    private Map<EventSource, Set<Event>> events = new HashMap<EventSource, Set<Event>>();

    // these are transient because the are only used when building up the report.
    // after the report has been completed and to be sent over the wire, they are no longer needed.
    // eventsDropped=number of events that were dropped due to limits being reached
    // totalEventsInReport=the total number of events in the report (sum of all events for all event sources)
    // addedLimitWarningEvents=true when one or more limit warning events were added to this report
    private transient Map<EventSource, Integer> eventsDropped;
    private transient int totalEventsInReport = 0;
    private transient boolean addedLimitWarningEvents = false;

    public EventReport(int maxEventsPerSource, int maxEventsPerReport) {
        this.maxEventsPerSource = maxEventsPerSource;
        this.maxEventsPerReport = maxEventsPerReport;
    }

    /**
     * Adds the given <code>event</code> to this report. If this report is too full,
     * the event will be silently rejected (i.e. an exception will not be thrown, but the
     * event will not be sent to the server).
     *
     * @param event the {@link Event} to be added
     * @param eventSource the source of the Event to be added
     */
    public void addEvent(@NotNull Event event, @NotNull EventSource eventSource) {
        if (this.totalEventsInReport < this.maxEventsPerReport) {
            Set<Event> eventSet = getEventsForEventSource(eventSource);
            if (eventSet.size() < this.maxEventsPerSource) {
                eventSet.add(event);
                this.totalEventsInReport++;
            } else {
                // this event source has maxed out its allowed number of events for this report
                droppedEvent(eventSource);
            }
        } else {
            // this event report has maxed out its total allowed number of events.
            droppedEvent(eventSource);
        }
        return;
    }

    private void droppedEvent(EventSource eventSource) {
        if (this.eventsDropped == null) {
            this.eventsDropped = new HashMap<EventSource, Integer>();
        }

        Integer droppedCount = this.eventsDropped.get(eventSource);
        if (droppedCount == null) {
            droppedCount = Integer.valueOf(1);
        } else {
            droppedCount = Integer.valueOf(droppedCount.intValue() + 1);
        }

        this.eventsDropped.put(eventSource, droppedCount);
        return;
    }

    private Set<Event> getEventsForEventSource(EventSource eventSource) {
        Set<Event> eventSet = this.events.get(eventSource);
        if (eventSet == null) {
            eventSet = new LinkedHashSet<Event>();
            this.events.put(eventSource, eventSet);
        }
        return eventSet;
    }

    /**
     * Returns the Events contained in this report; the Events are in a map keyed off Event sources.
     *
     * @return the Events contained in this report
     */
    @NotNull
    public Map<EventSource, Set<Event>> getEvents() {
        return this.events;
    }

    /**
     * This method will check to see if any maximum limits were exceeded and if so adds
     * warning events to indicate that the limits were breached.
     * 
     * The plugin container should call this method before it wants to send the report
     * over the wire to the server side. This method should be called whenever you are done
     * adding events to the report and you want to then process the events.
     * 
     * This method will only add warning events once. If this method adds at least one
     * warning event to the report, further calls to this method will be a no-op
     * (i.e. the method will do nothing and return immediately). Therefore, it is recommended
     * you call this method only after the report is done and you are ready to send the
     * report to the server for further processing.
     * 
     * @return true if some events in teh current report were dropped due to limits
     */
    public boolean addLimitWarningEvents() {
        if (this.addedLimitWarningEvents || this.eventsDropped == null) {
            return false; // we already added them or there is nothing to add
        }

        Event warningEvent;
        String warningMessage;
        long now = System.currentTimeMillis();

        for (Map.Entry<EventSource, Integer> entry : this.eventsDropped.entrySet()) {
            EventSource eventSource = entry.getKey();
            Integer droppedCount = entry.getValue();
            Set<Event> eventSet = getEventsForEventSource(eventSource);

            if (eventSet.size() >= this.maxEventsPerSource) {
                // this event source hit its individual limit
                warningMessage = "Event Report Limit Reached: reached the maximum allowed events ["
                    + this.maxEventsPerSource + "] for this event source - dropped [" + droppedCount + "] events";
            } else {
                // this report reached its total limit, even though this event source did not hit its individual limit
                warningMessage = "Event Report Limit Reached: reached total maximum allowed events ["
                    + this.maxEventsPerReport + "] - dropped [" + droppedCount + "] events";
            }

            warningEvent = new Event(eventSource.getEventDefinition().getName(), eventSource.getLocation(), now,
                EventSeverity.WARN, warningMessage, eventSource);
            eventSet.add(warningEvent);

            this.addedLimitWarningEvents = true;
        }

        return this.eventsDropped.size() > 0;
    }

    public Map<EventSource, Integer> getDroppedEvents() {
        return Collections.unmodifiableMap(eventsDropped);
    }

    public int getMaxEventsPerSource() {
        return this.maxEventsPerSource;
    }

    public int getMaxEventsPerReport() {
        return this.maxEventsPerReport;
    }

    @Override
    public String toString() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1) + "[" + this.events
            + "]";
    }
}
