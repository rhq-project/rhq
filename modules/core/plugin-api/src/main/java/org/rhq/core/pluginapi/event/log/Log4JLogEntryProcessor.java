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
import java.io.IOException;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;

/**
 * A {@link org.rhq.core.pluginapi.event.log.LogEntryProcessor} for Log4J log files.
 *
 * @author Ian Springer
 */
public class Log4JLogEntryProcessor implements LogEntryProcessor {
    private static final String REGEX = "(.*) (TRACE|DEBUG|INFO|WARN|ERROR|FATAL) (.*)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    
    private static final String ISO8601_DATE_PATTERN = "yyyy-MM-dd kk:mm:ss,SSS";
    private static final DateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat(ISO8601_DATE_PATTERN);
    private static final String ABSOLUTE_DATE_PATTERN = "kk:mm:ss,SSS";
    private static final DateFormat ABSOLUTE_DATE_FORMAT = new SimpleDateFormat(ABSOLUTE_DATE_PATTERN);
    private static final String DATE_DATE_PATTERN = "dd MMM yyyy kk:mm:ss,SSS";
    private static final DateFormat DATE_DATE_FORMAT = new SimpleDateFormat(DATE_DATE_PATTERN);

    private static final Map<Priority, EventSeverity> PRIORITY_TO_SEVERITY_MAP = new HashMap();

    static {
        PRIORITY_TO_SEVERITY_MAP.put(Priority.TRACE, EventSeverity.DEBUG);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.DEBUG, EventSeverity.DEBUG);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.INFO, EventSeverity.INFO);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.WARN, EventSeverity.WARN);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.ERROR, EventSeverity.ERROR);
        PRIORITY_TO_SEVERITY_MAP.put(Priority.FATAL, EventSeverity.FATAL);
    }

    private String eventType;
    private File logFile;
    private EventSeverity minimumSeverity;
    private Pattern includesPattern;
    private DateFormat dateFormat;

    public Log4JLogEntryProcessor(String eventType, File logFile) {
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

    private LogEntry processLine(String line, Set<Event> events, LogEntry currentEntry) {
        // For now, we only support the default pattern: Date Priority [Category] Message
        // e.g.: 2007-12-09 15:32:49,514 DEBUG [com.example.FooBar] run: IdleRemover notifying pools, interval: 450000
        Matcher matcher = PATTERN.matcher(line);
        if (matcher.matches()) {
            // A matching line tells us the previous entry has no more additional lines; we can therefore add an Event
            // for that entry.
            addEventForCurrentEntry(events, currentEntry);
            // Start building up a new entry...
            currentEntry = processPrimaryLine(matcher);
        } else {
            // If the line didn't match, assume it's an additional line (e.g. part of a stack trace).
            if (currentEntry != null) {
                currentEntry.appendLineToDetail(line);
            }
        }
        return currentEntry;
    }

    private void addEventForCurrentEntry(Set<Event> events, LogEntry currentEntry)
    {
        if (currentEntry != null) {
            EventSeverity severity = PRIORITY_TO_SEVERITY_MAP.get(currentEntry.getPriority());
            if (severity.isAtLeastAsSevereAs(this.minimumSeverity) &&
                    (this.includesPattern == null ||
                     this.includesPattern.matcher(currentEntry.getDetail()).matches())) {
                Event event = new Event(this.eventType, this.logFile.getPath(), currentEntry.getDate(), severity, currentEntry.getDetail());
                events.add(event);
            }
        }
    }

    private LogEntry processPrimaryLine(Matcher matcher) {
        String dateString = matcher.group(1);
        Date timestamp = parseDateString(dateString);
        String priorityString = matcher.group(2);
        Priority priority = Priority.valueOf(priorityString);
        String detail = matcher.group(3);
        return new LogEntry(timestamp, priority, detail);
    }

   private Date parseDateString(String dateString)
   {
      Date timestamp = null;
      if (this.dateFormat != null) {
          try {
              timestamp = this.dateFormat.parse(dateString);
          } catch (ParseException e) {
              throw new RuntimeException("Unable to parse date '" + dateString + "' using specified date format '" + this.dateFormat + "'.");
          }
      }
      if (timestamp == null)
      {
          try {
              timestamp = ISO8601_DATE_FORMAT.parse(dateString);
          } catch (ParseException e) {
              try {
                  timestamp = DATE_DATE_FORMAT.parse(dateString);
              } catch (ParseException e1) {
                  try {
                      timestamp = ABSOLUTE_DATE_FORMAT.parse(dateString);
                  } catch (ParseException e2) {
                      throw new RuntimeException(
                              "Unable to parse date '" + dateString +
                                      "' using either ISO8601, DATE, or ABSOLUTE date formats. Please specify a date format.");
                  }
              }
          }
      }
      setDateIfNotSet(timestamp);
      return timestamp;
   }

    private void setDateIfNotSet(Date timestamp) {
        Calendar date = convertToCalendar(timestamp);
        // If the format specified a time, but no date, the date will be Jan 1, 1970. In this case, set the date to
        // today's date.
        if (date.get(Calendar.YEAR) == 1970) {            
            Calendar currentDate = Calendar.getInstance();
            date.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE));
            timestamp.setTime(date.getTimeInMillis());
        }
    }

    private Calendar convertToCalendar(Date timestamp) {
        Calendar date = Calendar.getInstance();
        date.setTime(timestamp);
        return date;
    }

    private enum Priority {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    private class LogEntry {
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
            this.detail.append("\n");
            this.detail.append(string);
        }
    }
}
