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
package org.rhq.core.pluginapi.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.hyperic.sigar.Sigar;

import org.rhq.core.domain.event.Event;

/**
 * @author Ian Springer
 */
public interface EventContext {
    /**
     * Minimum polling interval, in seconds.
     */
    int MINIMUM_POLLING_INTERVAL = 60; // 1 minute

    /**
     * Publishes the specified Event. This means the Plugin Container will queue the Event to be sent to the Server.
     *
     * @param event the Event to be published
     */
    void publishEvent(@NotNull Event event);

    /**
     * Tells the plugin container to start polling for Events using the specified EventPoller.
     *
     * @param poller the poller that will be used
     * @param pollingInterval the number of seconds to wait between polls. If the value less than
     *                        {@link #MINIMUM_POLLING_INTERVAL} is specified, {@link #MINIMUM_POLLING_INTERVAL} will be
     *                        used instead.
     */
    void registerEventPoller(@NotNull EventPoller poller, int pollingInterval);

    /**
     * Tells the plugin container to start polling the specified source for Events using the specified EventPoller.
     *
     * @param poller the poller that will be used
     * @param pollingInterval the number of seconds to wait between polls. If the value less than
 *                        {@link #MINIMUM_POLLING_INTERVAL} is specified, {@link #MINIMUM_POLLING_INTERVAL} will be
     * @param sourceLocation the location of the source to start polling
     */
    void registerEventPoller(@NotNull EventPoller poller, int pollingInterval, @NotNull String sourceLocation);

    /**
     * Tells the plugin container to stop polling for Events of the specified type (i.e.
     * {@link org.rhq.core.domain.event.EventDefinition} name).
     *
     * @param eventType the type of Event to stop polling for
     */
    void unregisterEventPoller(@NotNull String eventType);

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
