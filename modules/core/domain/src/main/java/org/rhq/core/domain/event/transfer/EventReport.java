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
package org.rhq.core.domain.event.transfer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A report of resource {@link Event}s that the Plugin Container periodically sends to the Server. The report contains
 * all Events that have occurred since the last time a report was successfully sent.
 *
 * @author Ian Springer
 */
public class EventReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final int MAX_EVENTS_PER_SOURCE = 1000;

    private final Log log = LogFactory.getLog(this.getClass());

    private Map<EventSource, Set<Event>> events = new HashMap<EventSource, Set<Event>>();

    /**
     * Adds the given Event to this report.
     *
     * @param event the Event to be added
     * @param eventSource the source of the Event to be added
     */
    public void addEvent(@NotNull Event event, @NotNull EventSource eventSource) {
        Set<Event> eventSet = this.events.get(eventSource);
        if (eventSet == null) {
            eventSet = new LinkedHashSet<Event>();
            this.events.put(eventSource, eventSet);
        }
        if (eventSet.size() < MAX_EVENTS_PER_SOURCE)
            eventSet.add(event);
        else if (eventSet.size() == MAX_EVENTS_PER_SOURCE)
            log.warn(eventSource + " contains the maximum allowed Events per source (" + MAX_EVENTS_PER_SOURCE + ") - no more Events from this source will be added to this report.");
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
