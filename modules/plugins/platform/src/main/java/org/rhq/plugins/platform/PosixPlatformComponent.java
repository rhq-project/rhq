/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.platform;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Greg Hinkle
 */
public class PosixPlatformComponent extends PlatformComponent {

    private final Log log = LogFactory.getLog(PosixPlatformComponent.class);


    protected List<SyslogListenerEventLogDelegate> listenerEventDelegates;
    protected List<SyslogFileEventLogDelegate> fileEventDelegates;
    // event tracking plugin config names
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_LOGS = "logs";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_ENABLED = "logTrackingEnabled";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_INCLUDES_REGEX = "logTrackingIncludesPattern";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_MIN_SEV = "logTrackingMinimumSeverity";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_PARSER_REGEX = "logTrackingParserRegex";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_DATETIME_FORMAT = "logTrackingDateTimeFormat";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_TYPE = "logTrackingType";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_PORT = "logTrackingPort";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_BIND_ADDR = "logTrackingBindAddress";
    public static final String PLUGIN_CONFIG_EVENT_TRACKING_FILE_PATH = "logTrackingFilePath";


    protected enum EventTrackingType {
        listener, file
    };


    
    @Override
    public void start(ResourceContext context) {
        super.start(context);

        Configuration pluginConfiguration = context.getPluginConfiguration();

        // prepare the syslog listeners - must shutdown any lingering ones first
        PropertyList logs = pluginConfiguration.getList(PLUGIN_CONFIG_EVENT_TRACKING_LOGS);
        if (logs != null && logs.getList() != null && logs.getList().size() > 0) {
            for (Property logProp : logs.getList()) {
                try {
                    PropertyMap singleLog = (PropertyMap) logProp;
                    if (singleLog.getSimple(PLUGIN_CONFIG_EVENT_TRACKING_ENABLED).getBooleanValue()) {
                        if (getEventTrackingType(singleLog) == EventTrackingType.listener) {
                            // Start up the syslog listener
                            SyslogListenerEventLogDelegate delegate = new SyslogListenerEventLogDelegate(context,
                                singleLog);
                            if (this.listenerEventDelegates == null) {
                                this.listenerEventDelegates = new ArrayList<SyslogListenerEventLogDelegate>();
                            }
                            this.listenerEventDelegates.add(delegate);
                        } else if (getEventTrackingType(singleLog) == EventTrackingType.file) {
                            // Start up the syslog file poller
                            SyslogFileEventLogDelegate delegate = new SyslogFileEventLogDelegate(context, singleLog);
                            if (this.fileEventDelegates == null) {
                                this.fileEventDelegates = new ArrayList<SyslogFileEventLogDelegate>();
                            }
                            this.fileEventDelegates.add(delegate);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to prepare for event log [" + logProp + "]", e);
                }
            }
        }
    }

    private LinuxPlatformComponent.EventTrackingType getEventTrackingType(PropertyMap logConfiguration) {
        // default is "file" as described in plugin descriptor
        String type = logConfiguration.getSimpleValue(PLUGIN_CONFIG_EVENT_TRACKING_TYPE, LinuxPlatformComponent.EventTrackingType.file.name());
        LinuxPlatformComponent.EventTrackingType typeEnum;
        try {
            typeEnum = LinuxPlatformComponent.EventTrackingType.valueOf(type.toLowerCase());
        } catch (Exception e) {
            typeEnum = LinuxPlatformComponent.EventTrackingType.file;
            log.warn("event tracking type is invalid [" + type + "], defaulting to: " + typeEnum);
        }
        return typeEnum;
    }

    protected void shutdownSyslogDelegates() {
        if (this.listenerEventDelegates != null) {
            for (SyslogListenerEventLogDelegate delegate : this.listenerEventDelegates) {
                try {
                    delegate.shutdown();
                } catch (Exception e) {
                    log.warn("Failed to shutdown a syslog listener", e);
                }
            }
            this.listenerEventDelegates.clear();
        }

        if (this.fileEventDelegates != null) {
            for (SyslogFileEventLogDelegate delegate : this.fileEventDelegates) {
                try {
                    delegate.shutdown();
                } catch (Exception e) {
                    log.warn("Failed to shutdown a syslog file poller", e);
                }
            }
            this.fileEventDelegates.clear();
        }
    }

    @Override
    public void stop() {
        shutdownSyslogDelegates();

        super.stop();
    }
}
