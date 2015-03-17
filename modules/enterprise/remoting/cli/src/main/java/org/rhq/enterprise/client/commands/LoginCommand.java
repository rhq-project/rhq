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

package org.rhq.enterprise.client.commands;

import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.script.CommandLineParseException;
import org.rhq.enterprise.clientapi.RemoteClient;

/**
 * @author Greg Hinkle
 * @author Simeon Pinder
 */
public class LoginCommand implements ClientCommand {
    private static final Log LOG = LogFactory.getLog(LoginCommand.class);
    private static final List<String> SUPPORTED_TRANSPORTS = Arrays.asList("http", "https", "socket", "sslsocket");

    //Added to switch between jbossRemoting and WS subsystems
    private String subsystem = null;

    @Override
    public String getPromptCommandString() {
        return "login";
    }

    @Override
    public boolean execute(ClientMain client, String[] args) {
        if (args.length < 3) {
            throw new CommandLineParseException("Too few arguments");
        }
        if (args.length > 7) {
            throw new CommandLineParseException("Too many arguments");
        }

        String user = args[1];
        String pass = args[2];
        String host = "localhost";
        String transport = null;
        int port = 7080;

        PrintWriter printWriter = client.getPrintWriter();

        int argIndex = 3;
        if (args.length > argIndex) {
            host = args[argIndex];
        } else {
            printWriter.println("Logging in with default host: [" + host + "]");
        }
        argIndex++;
        if (args.length > argIndex) {
            try {
                port = Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException e) {
                printWriter.println("Invalid port [" + args[argIndex] + "]");
                return true;
            }
        } else {
            printWriter.println("Logging in with default port: [" + port + "]");
        }
        argIndex++;
        if (args.length > argIndex) {
            transport = args[argIndex];
            if (!SUPPORTED_TRANSPORTS.contains(transport)) {
                printWriter.println("Invalid transport [" + transport + "], must be one of "
                    + Arrays.toString(SUPPORTED_TRANSPORTS.toArray()));
                return true;
            }
        }
        argIndex++;
        if (args.length > argIndex) {
            //to activate subsystem must pass in all 7 parameters ex. ... https WSREMOTEAPI
            subsystem = args[argIndex];
        }

        try {
            execute(client, user, pass, host, port, transport);
            printWriter.println("Login successful");
        } catch (Exception e) {
            String message = "Login failed for [" + user + "] on [" + host + ":" + port + "]";
            Throwable cause = e.getCause();
            String details;
            if (cause instanceof UnknownHostException) {
                details = "Unknown host [" + host + "]";
            } else {
                details = cause == null ? e.getMessage() : cause.getMessage();
            }
            printWriter.println(message + ": " + details);
            printWriter.println(usage());
            if (LOG.isDebugEnabled()) {
                LOG.debug(message + " over transport: " + transport, e);
            }
        }

        return true;
    }

    public Subject execute(ClientMain client, String username, String password) throws Exception {
        return execute(client, username, password, "localhost", 7080, null);
    }

    public Subject execute(ClientMain client, String username, String password, String host, int port, String transport)
        throws Exception {

        //add call to different subsystem if it exists
        RemoteClient remoteClient;
        if ((subsystem != null) && (subsystem.trim().equalsIgnoreCase("WSREMOTEAPI"))) {
            remoteClient = new RemoteClient(transport, host, port, subsystem);
        } else {
            remoteClient = new RemoteClient(transport, host, port);
        }

        // in case transport was null, let the client tell us what it'll use
        client.setTransport(remoteClient.getTransport());
        client.setHost(host);
        client.setPort(port);
        client.setUser(username);
        client.setPass(password);

        Subject subject = remoteClient.login(username, password);

        String versionUpdate = remoteClient.getServerVersionUpdate();
        String version;
        //Conditionally check for and apply update/patch version details
        if ((versionUpdate != null) && (!versionUpdate.trim().isEmpty())) {
            version = remoteClient.getServerVersion() + " " + versionUpdate + " ("
                + remoteClient.getServerBuildNumber() + ")";
        } else {
            version = remoteClient.getServerVersion() + " (" + remoteClient.getServerBuildNumber() + ")";
        }

        client.getPrintWriter().println("Remote server version is: " + version);

        // this call has the side effect of setting bindings for the new remote client and its subject
        client.setRemoteClient(remoteClient);
        client.setSubject(subject);

        return subject;
    }

    private String usage() {
        return "Usage: " + getSyntax();
    }

    @Override
    public String getSyntax() {
        StringBuilder transports = new StringBuilder();
        for (String t : SUPPORTED_TRANSPORTS) {
            transports.append("<" + t + ">|");
        }
        transports.deleteCharAt(transports.length() - 1);
        return getPromptCommandString() + " username password [host]|[host port]|[host port " + transports + "]";
    }

    @Override
    public String getHelp() {
        return "Log into a server with specified username and password";
    }

    @Override
    public String getDetailedHelp() {
        return "Log into a server with the specified username and password. The server host "
            + "name and port may optionally be specified. The host name defaults to "
            + "localhost and the port to 7080. You may also specify the transport "
            + "to use when communicating with the server; it must be one " //
            + "of " + SUPPORTED_TRANSPORTS + ".";
    }
}
