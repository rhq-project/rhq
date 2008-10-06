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
package org.rhq.plugins.platform.content.yum;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides a bare-bones http server for yum requests. It is designed to be a non-concurrent server to ensure that
 * requests are handled <i>one at a tiem</i>. This is neccessary because the server caches the local metadata files. A
 * concurrent server would either duplicate the work required (separate files per thread) to construct the <i>local</i>
 * metadata or each thread would step-on the shared metadata files. Either way, a concurrent server should be sufficient
 * for a single client.
 *
 * @author jortel
 */
public class YumServer {
    /**
     * The server socket used to listen for requests.
     */
    private ServerSocket socket = null;

    /**
     * The server thread.
     */
    private Thread thread = null;

    /**
     * The server thread run flag.
     */
    private boolean run = true;

    /**
     * The server's context.
     */
    YumContext context;

    /**
     * The yum configuration file constant
     */
    private static final String yumconf = "/etc/yum.repos.d/rhq.repo";

    private final Log log = LogFactory.getLog(YumServer.class);

    /**
     * Starts the server. This will initialize and start a server thread. If the server is already running, this request
     * is treated as a restart. The server thread is stopped (run=false) and a new thread is initialized are started.
     *
     * @param  context The server's context.
     *
     * @throws Exception On all errors.
     */
    public void start(YumContext context) throws Exception {
        this.context = context;
        if (thread != null) {
            log.warn("Already started, restarting ...");
            halt();
        }

        setupYumConfiguration();
        int port = context.baseurl().getPort();
        InetAddress host = bindAddress();
        log.info("Binding: " + host + ":" + port);
        socket = new ServerSocket(port, 20, host);
        socket.setSoTimeout(2000);
        start(port);
    }

    /**
     * Stop (halt/run=false) the server thread. The request is ignored if the server thread isn't running.
     */
    public void halt() {
        if (thread == null) {
            log.warn("Stop ignored: not running");
            return;
        }

        try {
            run = false;
            thread.join();
            thread = null;
            socket.close();
            log.info("Stopped");
        } catch (Exception e) {
            log.error("halt falied", e);
        } finally {
            removeYumConfiguration();
        }
    }

    /**
     * Clean the cached metadata.
     */
    public void cleanMetadata() {
        try {
            Request request = new Request(this, null);
            request.cleanMetadata();
        } catch (Exception e) {
            log.error("Clean metadata failed", e);
        }
    }

    /**
     * Start a server thread listening on the <i>loopback</i> ethernet interface on the specifed port. The thread
     * listens for yum/http requests uisng a finite timeout so changes in the <i>run</i> flag can be detected.
     *
     * @param port The tcp port to listen on.
     */
    private void start(int port) {
        run = true;
        thread = new Thread("yum:" + port) {
            @Override
            public void run() {
                log.info("listening ...");
                while (run) {
                    listen();
                }
            }
        };
        thread.start();
        log.info("Started, listening on port: " + port);
    }

    /**
     * Listen for and process requests.
     */
    private void listen() {
        try {
            Socket client = socket.accept();
            client.setTcpNoDelay(true);
            client.setSoLinger(false, 0);
            Request request = new Request(this, client);
            request.process();
        } catch (SocketTimeoutException te) {
            // expected
        } catch (Exception e) {
            log.warn("listen failed", e);
            run = false;
        }
    }

    /**
     * Setup the /etc/yum.repos.d configuration for this server. The file is over-written to ensure proper content.
     */
    private void setupYumConfiguration() {
        File file = new File(yumconf);
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.println("[rhq]");
            writer.println("name=RHQ");
            writer.printf("baseurl=%s\n", context.baseurl());
            writer.printf("metadata_expire=%d\n", context.getMetadataCacheTimeout());
            writer.println("enabled=1");
            writer.println("gpgcheck=0");
            writer.println("keepalive=0");
            writer.println("timeout=90");
            writer.close();
        } catch (Exception e) {
            String msg = "The yum repo configuration file '" + file + "' could not be created/updated!";
            log.error(msg, e);
        }
    }

    /**
     * Remove the etc/yum.repos.d/ackbar.conf configuration file.
     */
    public void removeYumConfiguration() {
        File file = new File(yumconf);
        try {
            file.delete();
        } catch (Exception e) {
            String msg = "The yum repo configuration file '" + file + "' could not be removed!";
            log.error(msg, e);
        }
    }

    /**
     * Get the bind IP address.
     *
     * @return An address to bind.
     *
     * @throws UnknownHostException
     */
    private InetAddress bindAddress() throws UnknownHostException {
        byte[] host = { 127, 0, 0, 1 };
        return InetAddress.getByAddress(host);
    }
}