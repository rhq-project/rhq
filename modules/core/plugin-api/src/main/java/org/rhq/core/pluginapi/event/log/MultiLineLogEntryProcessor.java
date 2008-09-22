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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;

/**
 * A {@link LogEntryProcessor} for multi-line log files - provides several abstract methods that subclasses must
 * implement.
 *
 * @author Ian Springer
 */
public abstract class MultiLineLogEntryProcessor implements LogEntryProcessor {
    protected final Log log = LogFactory.getLog(this.getClass());

    protected String eventType;
    protected File logFile;
    protected EventSeverity minimumSeverity;
    protected Pattern includesPattern;
    protected DateFormat dateFormat;

    public MultiLineLogEntryProcessor(String eventType, File logFile) {
        this.eventType = eventType;
        this.logFile = logFile;
    }

    @Nullable
    public Set<Event> processLines(BufferedReader bufferedReader) throws IOException {
        // Use a LinkedHashSet so the Events are in the same order as the log entries they correspond to.
        Set<Event> events = new LinkedHashSet();
        LogEntry currentEntry = null;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            currentEntry = processLine(line, events, currentEntry);
        }
        // We've reached the end of the passed-in buffer, so assume the current entry is complete, and add an Event for
        // it.
        addEventForCurrentEntry(events, currentEntry);
        return events;
    }

    public void setMinimumSeverity(EventSeverity minimumSeverity) {
        this.minimumSeverity = minimumSeverity;
    }

    public void setIncludesPattern(Pattern includesPattern) {
        this.includesPattern = includesPattern;
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    protected LogEntry processLine(String line, Set<Event> events, LogEntry currentEntry) {
        Matcher matcher = getPattern().matcher(line);
        if (matcher.matches()) {
            // A matching line means this is the beginning of a new entry, which tells us the current entry
            // (if there is one) has no more additional lines; we can therefore add an Event for that entry.
            addEventForCurrentEntry(events, currentEntry);
            // And then start building up a new entry...
            try {
                currentEntry = processPrimaryLine(matcher);
            } catch (ParseException e) {
                // NOTE: Do not throw an exception here, otherwise none of the remaining lines that were passed in to
                //       processLines() will get processed.
                log.warn("Failed to parse line [" + line + "].");
                currentEntry = null;
            }
        } else {
            // If the line didn't match, assume it's an additional line (e.g. part of a stack trace).
            if (currentEntry != null) {
                currentEntry.appendLineToDetail(line);
            }
        }
        return currentEntry;
    }

    protected abstract Pattern getPattern();

    private void addEventForCurrentEntry(Set<Event> events, LogEntry currentEntry) {
        if (currentEntry != null) {
            if (currentEntry.getSeverity().isAtLeastAsSevereAs(this.minimumSeverity)
                && (this.includesPattern == null || this.includesPattern.matcher(currentEntry.getDetail()).find())) {
                Event event = new Event(this.eventType, this.logFile.getPath(), currentEntry.getDate().getTime(),
                    currentEntry.getSeverity(), currentEntry.getDetail());
                events.add(event);
            }
        }
    }

    protected abstract LogEntry processPrimaryLine(Matcher matcher) throws ParseException;

    protected abstract DateFormat getDefaultDateFormat();

    protected Date parseDateString(String dateString) throws ParseException {
        Date timestamp = null;
        DateFormat dateFormat = (this.dateFormat != null) ? this.dateFormat : getDefaultDateFormat();
        if (dateFormat != null) {
            try {
                timestamp = dateFormat.parse(dateString);
            } catch (java.text.ParseException e) {
                throw new ParseException("Unable to parse date [" + dateString + "] using date format [" + dateFormat
                    + "].", e);
            }
            setDateIfNotSet(timestamp);
        }
        return timestamp;
    }

    protected static void setDateIfNotSet(Date timestamp) {
        Calendar date = convertToCalendar(timestamp);
        // If the format specified a time, but no date, the date will be Jan 1, 1970. In this case, set the date to
        // today's date.
        if (date.get(Calendar.YEAR) == 1970) {
            Calendar currentDate = Calendar.getInstance();
            date.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE));
            timestamp.setTime(date.getTimeInMillis());
        }
    }

    private static Calendar convertToCalendar(Date timestamp) {
        Calendar date = Calendar.getInstance();
        date.setTime(timestamp);
        return date;
    }

    protected class LogEntry {
        private Date date;
        private EventSeverity severity;
        private StringBuilder detail;

        public LogEntry(Date date, EventSeverity severity, String detail) {
            this.date = date;
            this.severity = severity;
            this.detail = new StringBuilder(detail);
        }

        Date getDate() {
            return date;
        }

        EventSeverity getSeverity() {
            return severity;
        }

        String getDetail() {
            return detail.toString();
        }

        void appendLineToDetail(String string) {
            this.detail.append("\n");
            this.detail.append(string);
        }
    }

    protected class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}