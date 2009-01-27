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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;

/**
 * A report of resource {@link Event}s that the Plugin Container periodically sends to the Server. The report contains
 * all Events that have occurred since the last time a report was successfully sent.
 *
 * @author Ian Springer
 */
public class EventReport implements Serializable {
    private static final long serialVersionUID = 2L;

    private final int maxEventsPerSource;
    private final int maxEventsPerReport;

    // The log field must be either static final or transient, since sending this class over the wire will cause
    // InvalidClassExceptions (due to the Server having a different version of Commons Logging).
    private static final Log LOG = LogFactory.getLog(EventReport.class);

    private Map<EventSource, Set<Event>> events = new HashMap<EventSource, Set<Event>>();
    private int totalEventsInReport = 0;

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
        if (this.totalEventsInReport >= this.maxEventsPerReport) {
            LOG.warn("Event Report Limit Reached: this report contains the maximum allowed Events ["
                + this.maxEventsPerReport + "] - no more Events will be added to this report. source=[" + eventSource
                + "]");
            return;
        }

        Set<Event> eventSet = this.events.get(eventSource);
        if (eventSet == null) {
            eventSet = new LinkedHashSet<Event>();
            this.events.put(eventSource, eventSet);
        }

        if (eventSet.size() < this.maxEventsPerSource) {
            eventSet.add(event);
            this.totalEventsInReport++;
        } else {
            LOG.warn("Event Report Limit Reached: this report contains the maximum allowed Events ["
                + this.maxEventsPerSource + "] for the source [" + eventSource
                + "] - no more Events from this source will be added to this report.");
        }
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

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + this.events + "]";
    }
}
