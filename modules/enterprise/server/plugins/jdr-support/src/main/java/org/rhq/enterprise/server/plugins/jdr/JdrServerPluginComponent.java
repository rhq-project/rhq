/*
 * RHQ Management Platform
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.plugins.jdr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.system.SystemInfoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class JdrServerPluginComponent implements ServerPluginComponent {

    private UUID accessToken;
    private static final Log log = LogFactory.getLog(JdrServerPluginComponent.class);
    private static final int LISTEN = 7079;
    private static final int SOCK_TIMEOUT = 5 * 1000;
    private static final String TOKEN_FILE_NAME = "jdr-token";
    private static final int TOKEN_SIZE = UUID.randomUUID().toString().length();
    private static final Object lock = new Object();
    private Thread serverThread;
    private ServerSocket server;

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        setAccessToken();
        log.debug("Plugin initialized");
    }

    private void setAccessToken() {
        synchronized (lock) {
            accessToken = UUID.randomUUID();
            writeAccessToken();
        }
    }

    private String getAccessToken() {
        synchronized (lock) {
            return accessToken.toString();
        }
    }

    private void writeAccessToken() {
        File dataDir = new File(System.getProperty("jboss.server.data.dir"));
        if (!dataDir.exists() || !dataDir.canWrite()) {
            log.error("Failed to write access token, jboss.server.data.dir="+dataDir+" does not exist or not writable");
            return;
        }
        File file = new File(dataDir,TOKEN_FILE_NAME);

        try {
            PrintWriter pw = new PrintWriter(file);
            pw.println(accessToken);
            pw.close();
            // Clear permissions
            file.setReadable(false, false);
            file.setWritable(false, false);
            file.setExecutable(false, false);
            // Only set permissions to user
            file.setWritable(true, true);
            file.setReadable(true, true);
        } catch (FileNotFoundException fnfe) {
            log.error("Failed to write acces token, jboss.server.data.dir="+file.getParent()+" does not exist or not writable");
        }
    }

    @Override
    public void start() {

        try {
            server = new ServerSocket(LISTEN, 1, InetAddress.getByName(null));
            serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.debug("Listening on " + LISTEN);
                        while (true) {
                            Socket socket = server.accept();
                            socket.setSoTimeout(SOCK_TIMEOUT);
                            log.debug("Connection successfull");
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader( socket.getInputStream()));
                            String inputLine = null;
                            try {
                                char[] buffer = new char[TOKEN_SIZE];
                                int read = in.read(buffer);
                                if (read == TOKEN_SIZE) {
                                    inputLine = new String(buffer);
                                    if (inputLine.equals(getAccessToken())) {
                                        log.debug("Client authorized");
                                        out.println(getSystemInfo());
                                        log.debug("SystemInfo returned");
                                        setAccessToken(); // regenerate
                                     }
                                    else {
                                        log.debug("Invalid token recieved");
                                        out.println("Bye!");
                                    }
                                }
                                else {
                                    log.debug("Invalid token recieved");
                                    out.println("Bye!");
                                }
                            } catch (SocketTimeoutException ex) {
                                log.debug("Client timed out to send token");
                                out.println("Bye!");
                            }
                            in.close();
                            out.close();
                            socket.close();
                        }
                    }
                    catch (SocketException e) {
                        // listening was canceled
                    }
                    catch (Exception e) {
                        log.error("Socket server interrupted", e);
                    }
                }
            });
            serverThread.start();
        }
        catch (Exception ex) {
            log.error(ex);
        }
    }

    private String getSystemInfo() {
        try {
            SystemInfoManagerLocal mgr = LookupUtil.getSystemInfoManager();
            Map<String, String> systemInfo = mgr.getSystemInformation(LookupUtil.getSubjectManager().getOverlord());
            final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            return writer.writeValueAsString(systemInfo);
        }
        catch (Exception e) {
            log.error(e);
            return "ERROR retrieving system info : "+e.getMessage();
        }
    }

    @Override
    public void stop() {
        if (serverThread != null) {
            serverThread.interrupt();
        }
        if (server!=null) {
            try {
                if (!server.isClosed()) {
                    server.close();
                    log.debug("Socket server closed");
                }
            }
            catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Override
    public void shutdown() {
       stop();
    }

}
