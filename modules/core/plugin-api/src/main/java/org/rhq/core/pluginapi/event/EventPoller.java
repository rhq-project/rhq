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

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.event.Event;

/**
 * A class that polls at a designated interval for {@link Event}s of a specific type from a specific source.
 *
 * @author Ian Springer
 */
public interface EventPoller {
    /**
     * Returns the type of event (i.e. the {@link org.rhq.core.domain.event.EventDefinition} name) that this poller
     * checks for.
     *
     * @return the type of event (i.e. the {@link org.rhq.core.domain.event.EventDefinition} name) that this poller
     *         checks for
     */
    @NotNull
    String getEventType();

    /**
     * Poll for new Events (i.e. Events that have occurred since the last time poll() was called).
     * 
     * @return any new Events (i.e. Events that have occurred since the last time poll() was called)
     */
    @Nullable
    Set<Event> poll();
}
