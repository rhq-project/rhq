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

import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;

/**
 * A thread for running an {@link EventPoller} to check for new {@link Event}s.
 *
 * @author Ian Springer
 */
public class EventPollerRunner implements Runnable {    
    private EventPoller eventPoller;
    private EventSource eventSource;
    private EventManager eventManager;

    public EventPollerRunner(EventPoller eventPoller, EventSource eventSource, EventManager eventManager) {
        this.eventPoller = eventPoller;
        this.eventSource = eventSource;
        this.eventManager = eventManager;
    }

    public void run() {
        Set<Event> events = this.eventPoller.poll();
        if (events != null) {
            eventManager.publishEvents(events, this.eventSource);
        }
    }
}