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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Listens for syslog messages coming in over a socket.
 * 
 * @author Greg Hinkle
 */
public class SyslogListenerEventLogDelegate implements Runnable {

    private final Log log = LogFactory.getLog(SyslogListenerEventLogDelegate.class);

    private EventContext eventContext;
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader reader;

    private String host;
    private int port;

    private Thread thread;
    private boolean run = true;

    private static final String EVENT_LOG_TYPE = "Event Log";

    public SyslogListenerEventLogDelegate(ResourceContext resourceContext) {

        this.eventContext = resourceContext.getEventContext();

        this.host = resourceContext.getPluginConfiguration().getSimpleValue("host", "127.0.0.1");
        this.port = resourceContext.getPluginConfiguration().getSimple("port").getIntegerValue();

        try {
            this.thread = new Thread(this);
            this.serverSocket = new ServerSocket(this.port, 100, InetAddress.getByName(this.host));
            this.socket = this.serverSocket.accept();
            this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.thread.start();
        } catch (IOException e) {
            log.error("Failed attempt to bind syslog listener. May have been second attempt");
        }
    }

    public void run() {
        while (this.run) {
            try {
                String line = this.reader.readLine();
                if (line == null) {
                    log.info("syslog reader input stream has been closed - syslog events will stop being published");
                    this.run = false;
                } else {
                    Event e = convertLine(line);
                    if (e != null) {
                        this.eventContext.publishEvent(e);
                    }
                }
            } catch (IOException ioe) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to read syslog message: " + ioe);
                }
            }
        }
    }

    public void shutdown() {
        this.run = false;

        try {
            this.serverSocket.close();
        } catch (IOException ioe) {
            log.warn("Failed to close syslog listener server socket: " + ioe);
        }

        try {
            this.reader.close();
        } catch (IOException ioe) {
            log.warn("Failed to close syslog input reader: " + ioe);
        }
    }

    /**
     * Handles the default format
     * "%timegenerated::fulltime%,%syslogpriority-text%,%syslogfacility-text%,%msg%\n"
     *
     * @return
     */
    protected Event convertLine(String data) {

        try {
            String[] info = data.split("\\,", 4);

            // Skip the syslog time for now
            // private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd hh:mm:ss", new DateFormatSymbols(Locale.US));
            // Date d = sdf.parse(info[0]);
            long time = System.currentTimeMillis();

            String sev = info[1];
            EventSeverity severity = EventSeverity.DEBUG;
            if (sev.equalsIgnoreCase("EMERG") || sev.equalsIgnoreCase("CRIT")) {
                severity = EventSeverity.FATAL;
            } else if (sev.equalsIgnoreCase("ERR")) {
                severity = EventSeverity.ERROR;
            } else if (sev.equalsIgnoreCase("WARNING") || sev.equalsIgnoreCase("WARN")) {
                severity = EventSeverity.WARN;
            } else if (sev.equalsIgnoreCase("NOTICE") || sev.equalsIgnoreCase("INFO")) {
                severity = EventSeverity.INFO;
            } else if (sev.equalsIgnoreCase("DEBUG")) {
                severity = EventSeverity.DEBUG;
            }

            Event event = new Event(EVENT_LOG_TYPE, info[2], time, severity, info[3]);
            return event;

        } catch (Exception e) {
            log.warn("Failed to convert syslog input [" + data + "] to event: " + e);
        }

        return null;
    }

}
