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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.pluginapi.event.EventContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ian Springer
 */
public class LogFileEventResourceComponentHelper
{
    public static final String LOG_ENTRY_EVENT_TYPE = "logEntry";

    public static final String LOG_EVENT_SOURCES_CONFIG_PROP = "logEventSources";
    public static final String LOG_EVENT_SOURCE_CONFIG_PROP = "logEventSource";

    public abstract static class LogEventSourcePropertyNames {
        public static final String LOG_FILE_PATH = "logFilePath";
        public static final String ENABLED = "enabled";
        public static final String DATE_FORMAT = "dateFormat";
        public static final String INCLUDES_PATTERN = "includesPattern";
        public static final String MINIMUM_SEVERITY = "minimumSeverity";
    }

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;

    public LogFileEventResourceComponentHelper(ResourceContext resourceContext)
    {
        this.resourceContext = resourceContext;
    }

    public void startLogFileEventPollers() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertyList logEventSources = pluginConfig.getList(LOG_EVENT_SOURCES_CONFIG_PROP);
        for (Property prop : logEventSources.getList()) {
            PropertyMap logEventSource = (PropertyMap) prop;
            Boolean enabled = Boolean.valueOf(logEventSource.getSimpleValue(LogEventSourcePropertyNames.ENABLED, null));
            if (enabled) {
                String logFilePathname = logEventSource.getSimpleValue(LogEventSourcePropertyNames.LOG_FILE_PATH, null);
                if (logFilePathname==null) {
                    log.info("LOGFILE: No logfile path given, can not watch this event log.");
                    return;
                }
                File logFile = new File(logFilePathname);
                if (!logFile.exists() || !logFile.canRead()) {
                    log.error("LOGFILE: Logfile at location " + logFilePathname + " does not exist or is not readable. Can not start watching the event log.");
                    return;
                }

                Log4JLogEntryProcessor processor = new Log4JLogEntryProcessor(LOG_ENTRY_EVENT_TYPE, logFile);
                String dateFormatString = logEventSource.getSimpleValue(LogEventSourcePropertyNames.DATE_FORMAT, null);
                if (dateFormatString != null) {
                    try {
                        DateFormat dateFormat = new SimpleDateFormat(dateFormatString); // TODO locale specific ?
                        processor.setDateFormat(dateFormat);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidPluginConfigurationException("Date format [" + dateFormatString
                            + "] is not a valid simple date format.");
                    }
                }
                String includesPatternString = logEventSource.getSimpleValue(
                    LogEventSourcePropertyNames.INCLUDES_PATTERN, null);
                if (includesPatternString != null) {
                    try {
                        Pattern includesPattern = Pattern.compile(includesPatternString);
                        processor.setIncludesPattern(includesPattern);
                    } catch (PatternSyntaxException e) {
                        throw new InvalidPluginConfigurationException("Includes pattern [" + includesPatternString
                            + "] is not a valid regular expression.");
                    }
                }
                String minimumSeverityString = logEventSource.getSimpleValue(
                    LogEventSourcePropertyNames.MINIMUM_SEVERITY, null);
                if (minimumSeverityString != null) {
                    EventSeverity minimumSeverity = EventSeverity.valueOf(minimumSeverityString.toUpperCase());
                    processor.setMinimumSeverity(minimumSeverity);
                }
                EventContext eventContext = this.resourceContext.getEventContext();
                EventPoller poller = new LogFileEventPoller(eventContext, LOG_ENTRY_EVENT_TYPE, logFile, processor);
                eventContext.registerEventPoller(poller, 60, logFile.getPath());
            }
        }
    }

    public void stopLogFileEventPollers() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertyList logEventSources = pluginConfig.getList(LOG_EVENT_SOURCES_CONFIG_PROP);
        for (Property prop : logEventSources.getList()) {
            PropertyMap logEventSource = (PropertyMap) prop;
            String logFilePath = logEventSource.getSimpleValue(LogEventSourcePropertyNames.LOG_FILE_PATH, null);
            EventContext eventContext = this.resourceContext.getEventContext();
            eventContext.unregisterEventPoller(LOG_ENTRY_EVENT_TYPE, logFilePath);
        }
    }
}
