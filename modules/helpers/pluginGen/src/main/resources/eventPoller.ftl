<#--
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
-->
<#-- @ftlvariable name="props" type="org.rhq.helpers.pluginGen.Props" -->
package ${props.pkg};

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventPoller;


public class ${props.name?cap_first}EventPoller implements EventPoller {

    public ${props.name?cap_first}EventPoller() {

    }


    /** Return the type of events we handle
     * @see org.rhq.core.pluginapi.event.EventPoller#getEventType()
     */
    public String getEventType() {
        return ${props.componentClass}.DUMMY_EVENT;
    }


    /** Return collected events
     * @see org.rhq.core.pluginapi.event.EventPoller#poll()
     */
    public Set<Event> poll() {
        Set<Event> eventSet = new HashSet<Event>();
        // TODO add your events here. Below is an example that
        /*
        synchronized (events) {
            eventSet.addAll(events);
            events.clear();
        }
        */
        return eventSet;
    }

}