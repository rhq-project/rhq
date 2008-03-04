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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.event.EventSeverity;

/**
 * A {@link org.rhq.core.pluginapi.event.log.LogEntryProcessor} for Log4J log files.
 *
 * @author Ian Springer
 */
public class Log4JLogEntryProcessor extends MultiLineLogEntryProcessor {
    // For now, we only support the default pattern: date priority '['category']' message
    // e.g.: 2007-12-09 15:32:49,514 DEBUG [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000
    // NOTE: The message portion may contain multiple lines.
    private static final String REGEX = "(.*) (TRACE|DEBUG|INFO|WARN|ERROR|FATAL) (.*)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private static final String ISO8601_DATE_PATTERN = "yyyy-MM-dd kk:mm:ss,SSS";
    private static final DateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat(ISO8601_DATE_PATTERN);
    private static final String ABSOLUTE_DATE_PATTERN = "kk:mm:ss,SSS";
    private static final DateFormat ABSOLUTE_DATE_FORMAT = new SimpleDateFormat(ABSOLUTE_DATE_PATTERN);
    private static final String DATE_DATE_PATTERN = "dd MMM yyyy kk:mm:ss,SSS";
    private static final DateFormat DATE_DATE_FORMAT = new SimpleDateFormat(DATE_DATE_PATTERN);

    private static final Map<Priority, EventSeverity> PRIORITY_TO_SEVERITY_MAP = new LinkedHashMap();

    static {
        PRIORITY_TO_SEVERITY_MAP.put(Priority.TRACE, EventSeverity.DEBUG);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.DEBUG, EventSeverity.DEBUG);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.INFO, EventSeverity.INFO);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.WARN, EventSeverity.WARN);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.ERROR, EventSeverity.ERROR);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.FATAL, EventSeverity.FATAL);
    }

    public Log4JLogEntryProcessor(String eventType, File logFile) {
        super(eventType, logFile);
    }

    protected Pattern getPattern() {
        return PATTERN;
    }

    protected LogEntry processPrimaryLine(Matcher matcher) throws ParseException {
        String dateString = matcher.group(1);
        Date timestamp = parseDateString(dateString);
        String priorityString = matcher.group(2);
        Priority priority;
        try {
            priority = Priority.valueOf(priorityString);
        }
        catch (IllegalArgumentException e) {
            throw new ParseException("Unknown priority: " + priorityString);
        }
        EventSeverity severity = PRIORITY_TO_SEVERITY_MAP.get(priority);
        String detail = matcher.group(3);
        return new LogEntry(timestamp, severity, detail);
    }

    protected DateFormat getDefaultDateFormat() {
        return ISO8601_DATE_FORMAT;
    }

    protected Date parseDateString(String dateString) throws ParseException {
        Date timestamp;
        try {
            timestamp = super.parseDateString(dateString);
        }
        catch (ParseException e) {
            try {
                timestamp = DATE_DATE_FORMAT.parse(dateString);
            }
            catch (java.text.ParseException e1) {
                try {
                    timestamp = ABSOLUTE_DATE_FORMAT.parse(dateString);
                }
                catch (java.text.ParseException e2) {
                    throw new ParseException(
                            "Unable to parse date '" + dateString +
                                    "' using either ISO8601, DATE, or ABSOLUTE date formats. Please specify a date format.");
                }
            }
        }
        setDateIfNotSet(timestamp);
        return timestamp;
    }

    private enum Priority {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }
}
