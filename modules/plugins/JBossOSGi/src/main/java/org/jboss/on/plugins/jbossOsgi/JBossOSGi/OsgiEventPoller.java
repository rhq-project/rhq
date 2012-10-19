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
package org.jboss.on.plugins.jbossOsgi.JBossOSGi;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

import org.jboss.osgi.spi.logging.LogEntryCache;
import org.jboss.osgi.spi.logging.LogEntryFilter;

import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;

/**
 * Remotely poll the logs from the JBossOSGi server and feed into the
 * Events subsystem.
 *
 * @author Heiko W. Rupp
 */
public class OsgiEventPoller implements EventPoller{

    private final Log log = LogFactory.getLog(OsgiEventPoller.class);
    private LogEntryCache entryCache;

    public OsgiEventPoller() {
        entryCache = new LogEntryCache();// new LogEntryFilter(".*"), LogService.LOG_INFO, ".*");
        // TODO register with the remote
    }


    public void tearDown() {

    }

    @NotNull
    public String getEventType() {
        return JBossOsgiServerComponent.EVENT_TYPE_LOG;
    }

    public Set<Event> poll() {
        Set<Event> eventSet = new HashSet<Event>();
        List<LogEntry> entries = entryCache.getLog();
        for (LogEntry entry: entries ) {
            StringBuilder builder = new StringBuilder();
            builder.append(entry.getMessage());
            if (entry.getBundle()!=null)
                builder.append("\nBundle: ").append(entry.getBundle().getSymbolicName());
            if (entry.getException()!=null)
                builder.append("\nTrace: ").append(entry.getException());

            Event event = new Event(getEventType(),"RemoteServer",entry.getTime(),
                    levelToSeverity(entry.getLevel()),builder.toString());
            eventSet.add(event);
        }
        return eventSet;
    }


    /**
     * Translate the OSGi log levels into Jopr EventSeverity
     * @param level OSGi Log level
     * @return Corresonding EventSeverity
     * @see org.osgi.service.log.LogService
     */
    private EventSeverity levelToSeverity(int level) {
        EventSeverity ret;
        switch (level) {
            case 1: ret = EventSeverity.ERROR;
                break;
            case 2: ret = EventSeverity.WARN;
                break;
            case 3: ret = EventSeverity.INFO;
                break;
            case 4: ret = EventSeverity.DEBUG;
                break;
            default:
                ret = EventSeverity.INFO;
                log.warn("Got an unknown log level: " + level);
        }
        return ret;
    }
}
