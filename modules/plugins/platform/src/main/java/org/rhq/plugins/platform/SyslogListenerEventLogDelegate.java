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
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.event.Event;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Listens for syslog messages coming in over a socket.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class SyslogListenerEventLogDelegate extends SyslogProcessor implements Runnable {

    private final Log log = LogFactory.getLog(SyslogListenerEventLogDelegate.class);

    private String host;
    private int port;
    private ServerSocket serverSocket;

    private Thread thread;
    private boolean run = true;

    public SyslogListenerEventLogDelegate(ResourceContext resourceContext, PropertyMap logProperties) {
        super(resourceContext, logProperties, "port#" + getPortFromPropertyMap(logProperties));

        this.host = logProperties.getSimpleValue(LinuxPlatformComponent.PLUGIN_CONFIG_EVENT_TRACKING_BIND_ADDR,
            "127.0.0.1");
        this.port = getPortFromPropertyMap(logProperties);

        try {
            this.thread = new Thread(this, "Syslog Listener-" + this.host + ':' + this.port);
            this.thread.setDaemon(true);
            this.serverSocket = new ServerSocket(this.port, 100, InetAddress.getByName(this.host));
        } catch (Exception e) {
            throw new RuntimeException("Failed attempt to bind syslog listener.", e);
        }

        this.thread.start();
        return;
    }

    private static Integer getPortFromPropertyMap(PropertyMap logProperties) {
        return logProperties.getSimple(LinuxPlatformComponent.PLUGIN_CONFIG_EVENT_TRACKING_PORT).getIntegerValue();
    }

    public void run() {
        while (this.run) {
            try {
                Socket socket = this.serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line = reader.readLine(); // prime the pump and read the first line

                try {
                    while (this.run && line != null) {
                        Event e = convertLine(line);
                        if (e != null) {
                            getEventContext().publishEvent(e);
                        }
                        line = reader.readLine(); // read the next line
                    }
                } finally {
                    try {
                        socket.close(); // closes the input stream too
                    } catch (Throwable t) {
                        log.debug("Failed to close syslog input socket stream: " + t);
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to read syslog message: " + e);
                }
            }
        }

        return;
    }

    public void shutdown() {
        this.run = false;
        this.thread.interrupt();

        try {
            this.serverSocket.close();
        } catch (Throwable t) {
            log.warn("Failed to close syslog listener server socket: " + t);
        }

        return;
    }
}
