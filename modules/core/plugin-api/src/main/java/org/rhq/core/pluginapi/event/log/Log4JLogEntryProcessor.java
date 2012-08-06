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
 * <pre>
 * By default we support log entries of the format: date [delimiter]severity[delimiter]  message
 * Where:
 * - the optional delimiters are either square brackets ort parens, with or without spaces
 * - the date format is one of the built-ins below, or a user-specified date format for the logfile source
 * - the message portion may contain multiple lines.    
 * For example:
 * 2007-12-21 15:32:49,514 DEBUG [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000
 * 2007/12/21 15:32:49,514 (DEBUG) [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000    
 * 2007-Dec-21 15:32:49 [ DEBUG ] [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000    
 * </pre>
 * @author Ian Springer
 */
public class Log4JLogEntryProcessor extends MultiLineLogEntryProcessor {

    private static final String REGEX;
    private static final Pattern PATTERN;

    //note that the DateFormat instances are INTENTIONALLY instance fields.
    //DateFormats are not thread safe and because we can have multiple log processors
    //each processing its log file in a separate thread, we'd get formatting errors
    //where there weren't any...
    private static final String ISO8601_DATE_PATTERN = "yyyy-MM-dd kk:mm:ss,SSS";
    private final DateFormat iso8601DateFormat = new SimpleDateFormat(ISO8601_DATE_PATTERN);
    private static final String ABSOLUTE_DATE_PATTERN = "kk:mm:ss,SSS";
    private final DateFormat absoluteDateFormat = new SimpleDateFormat(ABSOLUTE_DATE_PATTERN);
    private static final String DATE_DATE_PATTERN = "dd MMM yyyy kk:mm:ss,SSS";
    private final DateFormat dateDateFormat = new SimpleDateFormat(DATE_DATE_PATTERN);

    private static final Map<Priority, EventSeverity> PRIORITY_TO_SEVERITY_MAP = new LinkedHashMap<Priority, EventSeverity>();

    static {
        // just in case there is something unanticipated that our default pattern doesn't like, allow
        // a backdoor prop to set the REGEX pattern.
        String regex = System.getProperty("rhq.agent.event.log4j.regex");
        REGEX = (null != regex) ? regex : "(.*?) [\\[\\(]??\\s*(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s*[\\]\\)]?? (.*)";
        PATTERN = Pattern.compile(REGEX);

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
        } catch (IllegalArgumentException e) {
            throw new ParseException("Unknown priority: " + priorityString);
        }
        EventSeverity severity = PRIORITY_TO_SEVERITY_MAP.get(priority);
        String detail = matcher.group(3);
        return new LogEntry(timestamp, severity, detail);
    }

    protected DateFormat getDefaultDateFormat() {
        return iso8601DateFormat;
    }

    protected Date parseDateString(String dateString) throws ParseException {
        Date timestamp;
        try {
            timestamp = super.parseDateString(dateString);
        } catch (ParseException e) {
            try {
                timestamp = dateDateFormat.parse(dateString);
            } catch (java.text.ParseException e1) {
                try {
                    timestamp = absoluteDateFormat.parse(dateString);
                } catch (java.text.ParseException e2) {
                    throw new ParseException("Unable to parse date '" + dateString
                        + "' using either ISO8601, DATE, or ABSOLUTE date formats. Please specify a date format.");
                }
            }
        }
        setDateIfNotSet(timestamp);
        return timestamp;
    }

    private enum Priority {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
}
