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
package org.rhq.core.pluginapi.event.log;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;

/**
 * @author Ian Springer
 */
public class Log4JLogEntryProcessor implements LogEntryProcessor {
    private static final String REGEX = "(.*) (TRACE|DEBUG|INFO|WARN|ERROR|FATAL) (.*)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    private static final String ISO8601_DATE_PATTERN = "yyyy-MM-dd kk:mm:ss,SSS";
    private static final DateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat(ISO8601_DATE_PATTERN);
    private static final String ABSOLUTE_DATE_PATTERN = "HH:mm:ss,SSS";
    private static final DateFormat ABSOLUTE_DATE_FORMAT = new SimpleDateFormat(ABSOLUTE_DATE_PATTERN);
    private static final String DATE_DATE_PATTERN = "dd MMM yyyy HH:mm:ss,SSS";
    private static final DateFormat DATE_DATE_FORMAT = new SimpleDateFormat(DATE_DATE_PATTERN);

    private static final Map<Priority, EventSeverity> PRIORITY_TO_SEVERITY_MAP = new HashMap();
    static
    {
        PRIORITY_TO_SEVERITY_MAP.put(Priority.TRACE, EventSeverity.DEBUG);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.DEBUG, EventSeverity.DEBUG);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.INFO, EventSeverity.INFO);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.WARN, EventSeverity.WARN);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.ERROR, EventSeverity.ERROR);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.FATAL, EventSeverity.FATAL);
    }

    private String eventType;
    private LogEntry currentEntry;

    public Log4JLogEntryProcessor(String eventType) {
        this.eventType = eventType;
    }

    @Nullable
    public Event processLine(String line) {
        Event event = null;
        // For now, we only support the default pattern: Date Priority [Category] Message
        // e.g.: 2007-12-09 15:32:49,514 DEBUG [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000
        Matcher matcher = PATTERN.matcher(line);
        if (matcher.matches()) {
            if (this.currentEntry != null) {
                // A matching line tells us the previous entry has no more additional lines; we can therefore create an 
                // Event for the entry.
                EventSeverity severity = PRIORITY_TO_SEVERITY_MAP.get(this.currentEntry.getPriority());
                event = new Event(this.eventType, this.currentEntry.getDate(), severity, this.currentEntry.getDetail());
            }
            // Start building up a new entry...
            this.currentEntry = processPrimaryLine(matcher);
        } else {
            // If the line didn't our regex, assume it's an additional line (e.g. part of a stack trace).
            if (this.currentEntry != null) {
                this.currentEntry.appendLineToDetail(line);                
            }
        }
        return event;
    }

    private LogEntry processPrimaryLine(Matcher matcher) {
        String dateString = matcher.group(1);
        String priorityString = matcher.group(2);
        String detail = matcher.group(3);

        Date timestamp;
        // TODO: Make date format configurable via event source config.
        try {
            timestamp = ISO8601_DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            try {
                timestamp = ABSOLUTE_DATE_FORMAT.parse(dateString);
            } catch (ParseException e1) {
                try {
                    timestamp = DATE_DATE_FORMAT.parse(dateString);
                } catch (ParseException e2) {
                    throw new RuntimeException(
                        "Unable to parse date '" + dateString + "' using either ISO8601, ABSOLUTE, or DATE formats.");
                }
            }
        }
        Priority priority = Priority.valueOf(priorityString);
        // Just store the entry until this method is called again, at which point we can see if the entry has any
        // additional lines (e.g. a stack trace).
        return new LogEntry(timestamp, priority, detail);
    }

    private enum Priority
    {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    private class LogEntry
    {
        private Date date;
        private Priority priority;
        private StringBuilder detail;

        LogEntry(Date date, Priority priority, String detail) {
            this.date = date;
            this.priority = priority;
            this.detail = new StringBuilder(detail);
        }

        Date getDate() {
            return date;
        }

        Priority getPriority() {
            return priority;
        }

        String getDetail() {
            return detail.toString();
        }

        void appendLineToDetail(String string) {
            this.detail.append(string);
            this.detail.append("\n");
        }
    }
}
