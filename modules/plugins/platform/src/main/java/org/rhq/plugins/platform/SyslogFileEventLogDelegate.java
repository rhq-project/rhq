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
package org.rhq.plugins.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.event.Event;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.log.LogEntryProcessor;
import org.rhq.core.pluginapi.event.log.LogFileEventPoller;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Reads syslog messages from a file.
 * 
 * @author John Mazzitelli
 */
public class SyslogFileEventLogDelegate extends SyslogProcessor implements LogEntryProcessor {

    private final Log log = LogFactory.getLog(SyslogFileEventLogDelegate.class);

    private String file;
    private LogFileEventPoller poller;

    public SyslogFileEventLogDelegate(ResourceContext resourceContext, PropertyMap logProperties) {
        super(resourceContext, logProperties, getFileFromPropertyMap(logProperties));

        this.file = getFileFromPropertyMap(logProperties);

        try {
            this.poller = new LogFileEventPoller(getEventContext(), EVENT_LOG_TYPE, new File(this.file), this);
            getEventContext().registerEventPoller(this.poller, EventContext.MINIMUM_POLLING_INTERVAL,
                this.poller.getSourceLocation());
        } catch (Exception e) {
            log.error("Failed attempt to setup syslog file poller. Cannot process syslog messages.", e);
        }
    }

    private static String getFileFromPropertyMap(PropertyMap logProperties) {
        return logProperties.getSimpleValue(LinuxPlatformComponent.PLUGIN_CONFIG_EVENT_TRACKING_FILE_PATH,
            "/var/log/messages");
    }

    public void shutdown() {
        if (this.poller != null) {
            getEventContext().unregisterEventPoller(this.poller.getEventType(), this.poller.getSourceLocation());
        }
        return;
    }

    public Set<Event> processLines(BufferedReader bufferedReader) throws IOException {
        // Use a LinkedHashSet so the Events are in the same order as the log entries they correspond to.
        Set<Event> events = new LinkedHashSet<Event>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            Event event = convertLine(line);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }
}
