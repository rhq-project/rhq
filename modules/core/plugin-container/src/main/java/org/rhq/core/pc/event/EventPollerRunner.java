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
package org.rhq.core.pc.event;

import java.util.Set;

import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.resource.Resource;

/**
 * A thread for running an {@link EventPoller} to check for new {@link Event}s of a certain type from a particular
 * Resource.
 *
 * @author Ian Springer
 */
public class EventPollerRunner implements Runnable {    
    private EventPoller eventPoller;
    private Resource resource;
    private EventManager eventManager;

    public EventPollerRunner(EventPoller eventPoller, Resource resource, EventManager eventManager) {
        this.eventPoller = eventPoller;
        this.resource = resource;
        this.eventManager = eventManager;
    }

    public void run() {
        Set<Event> events = this.eventPoller.poll();
        if (events != null) {
            this.eventManager.publishEvents(events, this.resource);
        }
    }
}