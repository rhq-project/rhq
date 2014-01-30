/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pluginapi.event.log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.SystemInfoFactory;

/**
 * A helper class that plugins can use to start and stop {@link LogFileEventPoller}s.
 *
 * @since 1.3
 * @author Ian Springer  
 */
public class LogFileEventResourceComponentHelper {
    private static final Log LOG = LogFactory.getLog(LogFileEventResourceComponentHelper.class);

    public static final String LOG_ENTRY_EVENT_TYPE = "logEntry";

    public static final String LOG_EVENT_SOURCES_CONFIG_PROP = "logEventSources";
    public static final String LOG_EVENT_SOURCE_CONFIG_PROP = "logEventSource";

    public abstract static class LogEventSourcePropertyNames {
        public static final String LOG_FILE_PATH = "logFilePath";        // required
        public static final String ENABLED = "enabled";                  // required
        public static final String DATE_FORMAT = "dateFormat";           // optional
        public static final String INCLUDES_PATTERN = "includesPattern"; // optional
        public static final String MINIMUM_SEVERITY = "minimumSeverity"; // optional
    }

    // TODO: Make this configurable via a plugin config prop.
    private static final int POLLING_INTERVAL_IN_SECONDS = 60;

    private ResourceContext<?> resourceContext;
    private List<PropertyMap> startedEventSources = new ArrayList<PropertyMap>();

    public LogFileEventResourceComponentHelper(ResourceContext<?> resourceContext) {
        this.resourceContext = resourceContext;
    }

    public void startLogFileEventPollers() {
        // Grab the list-o-maps of event sources from the plugin config.
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertyList logEventSources = pluginConfig.getList(LOG_EVENT_SOURCES_CONFIG_PROP);
        if (logEventSources == null) {
            throw new IllegalStateException("List property [" + LOG_EVENT_SOURCES_CONFIG_PROP
                    + "] not defined in plugin configuration for " + this.resourceContext.getResourceType().getName()
                    + " Resource with key [" + this.resourceContext.getResourceKey() + "].");
        }

        // Build a new list containing the event sources that are enabled.
        List<PropertyMap> enabledEventSources = new ArrayList<PropertyMap>();
        for (Property prop : logEventSources.getList()) {
            PropertyMap logEventSource = (PropertyMap) prop;
            String enabled = logEventSource.getSimpleValue(LogEventSourcePropertyNames.ENABLED, null);
            if (enabled == null) {
                throw new IllegalStateException("Required property [" + LogEventSourcePropertyNames.ENABLED
                                    + "] is not defined in map.");
            }
            if (Boolean.valueOf(enabled)) {
                enabledEventSources.add(logEventSource);
            }
        }

        // Log a warning then return if SIGAR isn't available, since LogFileEventPoller depends on it. We only log this
        // warning if at least one event source is enabled, since otherwise the user probably doesn't care.
        boolean sigarAvailable = this.resourceContext.getSystemInformation().isNative();
        if (!sigarAvailable && !enabledEventSources.isEmpty()) {
            boolean nativeSystemInfoDisabled = SystemInfoFactory.isNativeSystemInfoDisabled();
            ResourceType resourceType = this.resourceContext.getResourceType();
            List<String> logFilePaths = getLogFilePaths(enabledEventSources);
            LOG.warn("Log files " + logFilePaths + " for [" + resourceType.getPlugin() + ":"
                + resourceType.getName() + "] Resource with key [" + this.resourceContext.getResourceKey()
                + "] cannot be polled, because log file polling requires RHQ native support, which "
                + ((nativeSystemInfoDisabled) ? "has been disabled for this Agent" : "is not available on this platform") + ".");
            return;
        }

        // Start up log file pollers for each of the enabled event sources.
        for (PropertyMap logEventSource : enabledEventSources) {
            String logFilePath = logEventSource.getSimpleValue(LogEventSourcePropertyNames.LOG_FILE_PATH, null);
            if (logFilePath == null) {
                throw new IllegalStateException("Required property [" + LogEventSourcePropertyNames.LOG_FILE_PATH
                                    + "] is not defined in map.");
            }
            File logFile = new File(logFilePath);
            if (!logFile.canRead()) {
                LOG.warn("LOGFILE: Logfile at location " + logFilePath + " does not exist or is not readable. "
                    + "The poller will be started but no events will be polled until the file is created.");
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
            eventContext.registerEventPoller(poller, POLLING_INTERVAL_IN_SECONDS, logFile.getPath());
            this.startedEventSources.add(logEventSource);
        }
    }

    public void stopLogFileEventPollers() {
        boolean sigarAvailable = this.resourceContext.getSystemInformation().isNative();
        if (!sigarAvailable) {
            return;
        }

        for (Iterator<PropertyMap> iterator = this.startedEventSources.iterator(); iterator.hasNext(); ) {
            PropertyMap logEventSource = iterator.next();
            EventContext eventContext = this.resourceContext.getEventContext();
            String logFilePath = logEventSource.getSimpleValue(LogEventSourcePropertyNames.LOG_FILE_PATH, null);
            eventContext.unregisterEventPoller(LOG_ENTRY_EVENT_TYPE, logFilePath);
            iterator.remove();
        }
    }

    private List<String> getLogFilePaths(List<PropertyMap> enabledEventSources) {
        List<String> logFilePaths = new ArrayList<String>(enabledEventSources.size());
        for (PropertyMap logEventSource : enabledEventSources) {
            String logFilePath = logEventSource.getSimpleValue(LogEventSourcePropertyNames.LOG_FILE_PATH, null);
            if (logFilePath == null) {
                throw new IllegalStateException("Required property [" + LogEventSourcePropertyNames.LOG_FILE_PATH
                    + "] is not defined in map.");
            }
            logFilePaths.add(logFilePath);
        }
        return logFilePaths;
    }
}
