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
package org.rhq.core.pc.event;

import java.util.Set;
import java.util.HashSet;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.hyperic.sigar.Sigar;

import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;

/**
 * @author Ian Springer
 */
public class EventContextImpl implements EventContext {
    private Resource resource;
    private EventManager eventManager;

    public EventContextImpl(@NotNull Resource resource) {
        this.resource = resource;
        this.eventManager = PluginContainer.getInstance().getEventManager();
    }

    public void publishEvent(@NotNull Event event, @NotNull String sourceLocation) {
        //noinspection ConstantConditions
        if (event == null)
            throw new IllegalArgumentException("event parameter must not be null.");
        //noinspection ConstantConditions
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        Set<Event> events = new HashSet<Event>();
        events.add(event);
        EventSource eventSource = createEventSource(event.getType(), sourceLocation);
        this.eventManager.publishEvents(events, eventSource);
    }

    public void registerEventPoller(@NotNull EventPoller poller) {
        //noinspection ConstantConditions
        if (poller == null)
            throw new IllegalArgumentException("poller parameter must not be null.");
        EventSource eventSource = createEventSource(poller.getEventType(), poller.getSourceLocation());
        this.eventManager.registerEventPoller(poller, eventSource);
    }

    public void unregisterEventPoller(@NotNull String eventType, @NotNull String sourceLocation) {
        //noinspection ConstantConditions
        if (eventType == null)
            throw new IllegalArgumentException("eventType parameter must not be null.");
        //noinspection ConstantConditions
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        EventSource eventSource = createEventSource(eventType, sourceLocation);
        this.eventManager.unregisterEventPoller(eventSource);
    }

    @NotNull
    public Sigar getSigar() {
        return this.eventManager.getSigar();
    }

    private EventSource createEventSource(String eventType, String sourceLocation) {
        EventDefinition eventDefinition = getEventDefinition(eventType);
        if (eventDefinition == null)
        {
            throw new IllegalArgumentException("Unknown type - no EventDefinition found with name '" + eventType + "'.");
        }
        EventSource eventSource = new EventSource(sourceLocation, eventDefinition, this.resource);
        return eventSource;
    }

    /**
     * Returns the {@link org.rhq.core.domain.event.EventDefinition} for the {@link Event} with the specified name.
     *
     * @param  eventName an <code>Event</code> name
     *
     * @return the event definition for the <code>Event</code> with the specified name
     */
    @Nullable
    private EventDefinition getEventDefinition(String eventName) {
        Set<EventDefinition> eventDefinitions = this.resource.getResourceType().getEventDefinitions();
        if (eventDefinitions != null) {
            for (EventDefinition eventDefinition : eventDefinitions) {
                if (eventDefinition.getName().equals(eventName)) {
                    return eventDefinition;
                }
            }
        }
        return null;
    }
}
