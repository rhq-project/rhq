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
package org.rhq.plugins.apache;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;

import org.rhq.core.pluginapi.event.log.MultiLineLogEntryProcessor;
import org.rhq.core.domain.event.EventSeverity;

/**
 * A {@link org.rhq.core.pluginapi.event.log.LogEntryProcessor} for Apache HTTPd error log files.
 *
 * @author Ian Springer
 */
public class ApacheErrorLogEntryProcessor extends MultiLineLogEntryProcessor {
    /**
     * The regex for the primary log line: '['date']' '['severityLevel']' '['clientIP']' message
     * e.g.: [Wed Oct 11 14:32:52 2008] [error] [client 127.0.0.1] client denied by server configuration
     * NOTE: The message portion may contain multiple lines.
     */
    private static final String REGEX = "\\[(.*)\\] \\[(debug|info|notice|warn|error|crit|alert|emerg)\\] (.*)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private static final String DATE_PATTERN = "EEE MMM dd kk:mm:ss yyyy"; // e.g.: Tue Mar 04 14:32:52 2008
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);

    private static final Map<SeverityLevel, EventSeverity> LEVEL_TO_SEVERITY_MAP = new LinkedHashMap();
    static {
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.DEBUG, EventSeverity.DEBUG);
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.INFO, EventSeverity.INFO);
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.NOTICE, EventSeverity.INFO);
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.WARN, EventSeverity.WARN);
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.ERROR, EventSeverity.ERROR);
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.CRIT, EventSeverity.FATAL);
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.ALERT, EventSeverity.FATAL);
        LEVEL_TO_SEVERITY_MAP.put(SeverityLevel.EMERG, EventSeverity.FATAL);
    }

    public ApacheErrorLogEntryProcessor(String eventType, File logFile) {
        super(eventType, logFile);
    }

    protected Pattern getPattern() {
        return PATTERN;
    }

    protected DateFormat getDefaultDateFormat() {
        return DATE_FORMAT;
    }

    protected LogEntry processPrimaryLine(Matcher matcher) throws ParseException {
        String dateString = matcher.group(1);
        Date timestamp = parseDateString(dateString);
        String severityLevelString = matcher.group(2);
        SeverityLevel severityLevel;
        try {
            severityLevel = SeverityLevel.valueOf(severityLevelString.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            throw new ParseException("Unknown severity level: " + severityLevelString);
        }
        EventSeverity severity = LEVEL_TO_SEVERITY_MAP.get(severityLevel);
        String detail = matcher.group(3);
        return new LogEntry(timestamp, severity, detail);
    }

    private enum SeverityLevel {
        DEBUG,
        INFO,
        NOTICE,
        WARN,
        ERROR,
        CRIT,
        ALERT,
        EMERG
    }
}
