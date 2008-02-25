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
package org.rhq.core.pluginapi.event;

import org.jetbrains.annotations.NotNull;
import org.hyperic.sigar.Sigar;

import org.rhq.core.domain.event.Event;

/**
 * @author Ian Springer
 */
public interface EventContext {
    /**
     * Publishes the specified Event. This means the Plugin Container will queue the Event to be sent to the Server.
     *
     * @param event the Event to be published
     * @param sourceLocation the location of the source of the Event
     */
    void publishEvent(@NotNull Event event, @NotNull String sourceLocation);

    /**
     * Tells the plugin container to start polling the specified source for Events using the specified EventPoller.
     *
     * @param poller the poller that will be used
     */
    void registerEventPoller(@NotNull EventPoller poller);

    /**
     * Tells the plugin container to stop polling the specified source for Events of the specified type (i.e.
     * {@link org.rhq.core.domain.event.EventDefinition} name).
     *
     * @param eventType the type of Event to stop polling for
     * @param sourceLocation the location of the source to stop polling
     */
    void unregisterEventPoller(@NotNull String eventType, @NotNull String sourceLocation);

    /**
     * Gets an instance of Sigar. Plugins that need to use Sigar, should use this method to get an instance, rather than
     * instantiating Sigar themselves.
     *
     * @return an instance of Sigar
     */
    @NotNull
    Sigar getSigar();
}
