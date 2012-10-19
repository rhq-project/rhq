/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.plugins.platform.win;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.EventLog;
import org.hyperic.sigar.win32.EventLogRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * A delegate for reading windows event logs and returning them as RHQ events. Supports
 * filtering by regular expression of the content, as well as minimum severity.
 *
 * @author Greg Hinkle, Jay Shaughnessy
 */
public class Win32EventLogDelegate implements EventPoller {
    private static final String EVENT_TYPE = "Event Log";

    private final Log log = LogFactory.getLog(Win32EventLogDelegate.class);

    private String[] logNames;
    private EventLog[] eventLogs;
    private int[] lastCollectedEventId;

    private EventSeverity minimumSeverity;
    private Pattern regularExpression;

    private int eventsChecked;
    private int eventsFired;

    public Win32EventLogDelegate(Configuration config) {

        this.logNames = EventLog.getLogNames();

        String minimumSeverityString = config.getSimpleValue("minimumSeverity", "Error");
        if ("Information".equals(minimumSeverityString)) {
            minimumSeverity = EventSeverity.INFO;
        } else if ("Warning".equals(minimumSeverityString)) {
            minimumSeverity = EventSeverity.WARN;
        } else if ("Error".equals(minimumSeverityString)) {
            minimumSeverity = EventSeverity.ERROR;
        }

        String regexString = config.getSimpleValue("regularExpression", null);
        try {
            if (regexString != null) {
                regularExpression = Pattern.compile(regexString);
            }
        } catch (PatternSyntaxException pse) {
            log.warn("Event tracking regular expression not valid, no filtering will take place", pse);
        }

        eventLogs = new EventLog[logNames.length];
        lastCollectedEventId = new int[logNames.length];
    }

    public void open() {
        for (int i = 0; i < eventLogs.length; i++) {
            try {
                if (eventLogs[i] == null) {
                    eventLogs[i] = new EventLog();
                    eventLogs[i].open(logNames[i]);

                    // note, the first processed event will be the next one generated, this one
                    // was generated in the past, prior to this call to open(). 
                    lastCollectedEventId[i] = eventLogs[i].getNewestRecord();
                }
            } catch (Exception e) {
                log.warn("Failed to open Windows Event Log [" + logNames[i] + "]; will not collect its events", e);
                eventLogs[i] = null;
            }
        }
    }

    public void close() {
        for (int i = 0; i < eventLogs.length; i++) {
            try {
                if (eventLogs[i] != null) {
                    eventLogs[i].close();
                    eventLogs[i] = null;
                }
            } catch (Exception e) {
                log.warn("Failed to close Windows Event Log [" + logNames[i] + "]", e);
            }
        }
    }

    @Nullable
    public Set<Event> checkForNewEvents() {
        Set<Event> convertedEvents = null;
        for (int i = 0; i < eventLogs.length; i++) {
            try {
                if (eventLogs[i] != null) {
                    int newest = eventLogs[i].getNewestRecord();
                    if (newest > lastCollectedEventId[i]) {
                        for (int eventId = lastCollectedEventId[i] + 1; eventId <= newest; eventId++) {
                            eventsChecked++;

                            EventLogRecord event = eventLogs[i].read(eventId);
                            Event convertedEvent = handleEvent(event);

                            if (null != convertedEvent) {
                                if (null == convertedEvents) {
                                    convertedEvents = new HashSet<Event>();
                                }

                                convertedEvents.add(convertedEvent);
                            }
                        }
                        lastCollectedEventId[i] = newest;
                    }
                }
            } catch (Exception e) {
                log.info("An error occurred while reading the Windows Event Log [" + logNames[i] + "]", e);
            }
        }
        return convertedEvents;
    }

    public Event handleEvent(EventLogRecord event) {
        if (regularExpression != null) {
            if (!regularExpression.matcher(event.getMessage()).find()) {
                return null;
            }
        }

        if (!convertSeverity(event.getEventType()).isAtLeastAsSevereAs(minimumSeverity)) {
            return null;
        }

        Event convertedEvent = new Event(EVENT_TYPE, event.getLogName(), event.getTimeGenerated() * 1000,
            convertSeverity(event.getEventType()), event.getMessage());
        eventsFired++;
        return convertedEvent;
    }

    private EventSeverity convertSeverity(short type) {
        switch (type) {
        case EventLog.EVENTLOG_INFORMATION_TYPE:
            return EventSeverity.INFO;
        case EventLog.EVENTLOG_WARNING_TYPE:
            return EventSeverity.WARN;
        case EventLog.EVENTLOG_ERROR_TYPE:
            return EventSeverity.ERROR;
        default:
            return EventSeverity.DEBUG;
        }
    }

    @NotNull
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Nullable
    public Set<Event> poll() {
        return checkForNewEvents();
    }

    public int getEventsChecked() {
        return eventsChecked;
    }

    public int getEventsFired() {
        return eventsFired;
    }
}
