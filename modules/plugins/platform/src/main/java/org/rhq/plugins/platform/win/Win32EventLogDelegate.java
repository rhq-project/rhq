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
package org.rhq.plugins.platform.win;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.EventLog;
import org.hyperic.sigar.win32.EventLogRecord;
import org.hyperic.sigar.win32.Win32Exception;
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

        for (int i = 0; (i < logNames.length); ++i) {
            eventLogs[i] = new EventLog();
        }
    }

    public void open() {
        try {
            int i = 0;
            for (String logName : logNames) {

                eventLogs[i].open(logName);

                // note, the first processed event will be the next one generated, this one
                // was generated in the past, prior to this call to open(). 
                lastCollectedEventId[i] = eventLogs[i].getNewestRecord();

                i++;
            }
        } catch (Win32Exception e) {
            log.warn("Failed to open Windows Event Log, log tracking will not return events", e);
        }
    }

    public void close() {
        for (EventLog eventLog : eventLogs) {
            try {
                eventLog.close();
            } catch (Win32Exception e) {
                log.warn("Failed to close Windows Event Log", e);
            }
        }
    }

    @Nullable
    public Set<Event> checkForNewEvents() {
        Set<Event> convertedEvents = null;
        for (int i = 0; i < eventLogs.length; i++) {
            try {
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
            } catch (Win32Exception e) {
                log.info("An error occurred while reading the Windows Event Log", e);
            }
        }
        return convertedEvents;
    }

    public Event handleEvent(EventLogRecord event) {
        eventsChecked++;

        if (regularExpression != null) {
            if (!regularExpression.matcher(event.getMessage()).find())
                return null;
        }

        if (!convertSeverity(event.getEventType()).isAtLeastAsSevereAs(minimumSeverity))
            return null;

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
