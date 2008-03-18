/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.platform.win;

import org.hyperic.sigar.win32.EventLog;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.sigar.win32.EventLogRecord;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.configuration.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A delegate for reading windows event logs and returning them as RHQ events. Supports
 * filtering by regular expression of the content, as well as minimum severity.
 *
 * @author Greg Hinkle
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

    }

    public void open() {
        try {
            eventLogs = new EventLog[logNames.length];
            lastCollectedEventId = new int[logNames.length];

            int i = 0;
            for (String logName : logNames) {
                eventLogs[i] = new EventLog();
                eventLogs[i].open(logName);
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

    public Set<Event> checkForNewEvents() {
        Set<Event> convertedEvents = null;
        for (int i = 0; i < eventLogs.length; i++) {
            try {
                int newest = eventLogs[i].getNewestRecord();
                if (newest > lastCollectedEventId[i]) {
                    for (int eventId = lastCollectedEventId[i]; eventId <= newest; eventId++) {
                        eventsChecked++;

                        EventLogRecord event = eventLogs[i].read(eventId);
                        Event convertedEvent = handleEvent(event);

                        if (convertedEvents == null) {
                            convertedEvents = new HashSet<Event>();
                        }
                        convertedEvents.add(convertedEvent);
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

        Event convertedEvent =
                new Event(
                        EVENT_TYPE,
                        event.getLogName(),
                        new Date(event.getTimeGenerated() * 1000),
                        convertSeverity(event.getEventType()),
                        event.getMessage());
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
