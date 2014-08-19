/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.event;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.sigar.SigarProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * @author Ian Springer
 */
public class EventContextImpl implements EventContext {
    private final Resource resource;
    private final EventManager eventManager;

    public EventContextImpl(@NotNull Resource resource, EventManager eventManager) {
        this.resource = resource;
        this.eventManager = eventManager;
    }

    public void publishEvent(@NotNull Event event) {
        //noinspection ConstantConditions
        if (event == null)
            throw new IllegalArgumentException("event parameter must not be null.");
        EventDefinition eventDefinition = EventUtility.getEventDefinition(event.getType(),
            this.resource.getResourceType());
        if (eventDefinition == null)
            throw new IllegalArgumentException("Event has unknown event type - no EventDefinition exists with name '"
                + event.getType() + "'.");
        Set<Event> events = new HashSet<Event>();
        events.add(event);
        eventManager.publishEvents(events, this.resource);
    }

    public void registerEventPoller(@NotNull EventPoller poller, int pollingInterval) {
        //noinspection ConstantConditions
        if (poller == null)
            throw new IllegalArgumentException("poller parameter must not be null.");
        String sourceLocation = null;
        registerEventPollerInternal(poller, pollingInterval, sourceLocation);
    }

    public void registerEventPoller(@NotNull EventPoller poller, int pollingInterval, @NotNull String sourceLocation) {
        //noinspection ConstantConditions
        if (poller == null)
            throw new IllegalArgumentException("poller parameter must not be null.");
        //noinspection ConstantConditions
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        registerEventPollerInternal(poller, pollingInterval, sourceLocation);
    }

    public void unregisterEventPoller(@NotNull String eventType) {
        //noinspection ConstantConditions
        if (eventType == null)
            throw new IllegalArgumentException("eventType parameter must not be null.");
        String sourceLocation = null;
        unregisterEventPollerInternal(eventType, sourceLocation);
    }

    public void unregisterEventPoller(@NotNull String eventType, @NotNull String sourceLocation) {
        //noinspection ConstantConditions
        if (eventType == null)
            throw new IllegalArgumentException("eventType parameter must not be null.");
        //noinspection ConstantConditions
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        unregisterEventPollerInternal(eventType, sourceLocation);
    }

    @Nullable
    public SigarProxy getSigar() {
        return eventManager.getSigar();
    }

    /**
     * Only used for testing purposes.
     */
    public Resource getResource() {
        return resource;
    }

    private void registerEventPollerInternal(final EventPoller poller, int pollingInterval, final String sourceLocation) {
        EventDefinition eventDefinition = EventUtility.getEventDefinition(poller.getEventType(),
            this.resource.getResourceType());
        if (eventDefinition == null)
            throw new IllegalArgumentException("Poller has unknown event type - no EventDefinition exists with name '"
                + poller.getEventType() + "'.");
        final int adjustedPollingInterval = Math.max(EventContext.MINIMUM_POLLING_INTERVAL, pollingInterval);
        eventManager.registerEventPoller(poller, adjustedPollingInterval, resource, sourceLocation);
    }

    private void unregisterEventPollerInternal(String eventType, String sourceLocation) {
        EventDefinition eventDefinition = EventUtility.getEventDefinition(eventType, this.resource.getResourceType());
        if (eventDefinition == null)
            throw new IllegalArgumentException("Unknown event type - no EventDefinition exists with name '" + eventType
                + "'.");
        eventManager.unregisterEventPoller(this.resource, eventType, sourceLocation);
    }
}
