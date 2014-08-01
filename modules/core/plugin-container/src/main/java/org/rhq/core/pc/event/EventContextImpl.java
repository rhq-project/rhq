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
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * @author Ian Springer
 */
public class EventContextImpl implements EventContext {
    private final Resource resource;
    private final PluginContainer pluginContainer;

    public EventContextImpl(@NotNull
    Resource resource, PluginContainer pluginContainer) {
        this.resource = resource;
        this.pluginContainer = pluginContainer;
    }

    @Override
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
        pluginContainer.getEventManager().publishEvents(events, this.resource);
    }

    @Override
    public void registerEventPoller(@NotNull EventPoller poller, int pollingInterval) {
        //noinspection ConstantConditions
        if (poller == null)
            throw new IllegalArgumentException("poller parameter must not be null.");
        registerEventPollerInternal(poller, pollingInterval, null);
    }

    @Override
    public void registerEventPoller(@NotNull EventPoller poller, int pollingInterval, @NotNull String sourceLocation) {
        //noinspection ConstantConditions
        if (poller == null)
            throw new IllegalArgumentException("poller parameter must not be null.");
        //noinspection ConstantConditions
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        registerEventPollerInternal(poller, pollingInterval, sourceLocation);
    }

    @Override
    public void unregisterEventPoller(@NotNull String eventType) {
        //noinspection ConstantConditions
        if (eventType == null)
            throw new IllegalArgumentException("eventType parameter must not be null.");
        unregisterEventPollerInternal(eventType, null);
    }

    @Override
    public void unregisterEventPoller(@NotNull String eventType, @NotNull String sourceLocation) {
        //noinspection ConstantConditions
        if (eventType == null)
            throw new IllegalArgumentException("eventType parameter must not be null.");
        //noinspection ConstantConditions
        if (sourceLocation == null)
            throw new IllegalArgumentException("sourceLocation parameter must not be null.");
        unregisterEventPollerInternal(eventType, sourceLocation);
    }

    @Override
    @Nullable
    public SigarProxy getSigar() {
        return pluginContainer.getEventManager().getSigar();
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
        pluginContainer.getEventManager()
            .registerEventPoller(poller, adjustedPollingInterval, resource, sourceLocation);
    }

    private void unregisterEventPollerInternal(String eventType, String sourceLocation) {
        EventDefinition eventDefinition = EventUtility.getEventDefinition(eventType, this.resource.getResourceType());
        if (eventDefinition == null)
            throw new IllegalArgumentException("Unknown event type - no EventDefinition exists with name '" + eventType
                + "'.");
        pluginContainer.getEventManager().unregisterEventPoller(this.resource, eventType, sourceLocation);
    }
}
