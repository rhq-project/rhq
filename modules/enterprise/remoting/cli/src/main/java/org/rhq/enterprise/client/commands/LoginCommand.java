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
package org.rhq.enterprise.client.commands;

import java.io.PrintWriter;

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

    private final Log log = LogFactory.getLog(LoginCommand.class);

    //Added to switch between jbossRemoting and WS subsystems
    private String subsystem = null;

    public String getPromptCommandString() {
        return "login";
    }

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

        try {
            if (args.length == 5) {
                host = args[3];
                port = Integer.parseInt(args[4]);
            } else if (args.length == 6) {
                host = args[3];
                port = Integer.parseInt(args[4]);
                transport = args[5];
            } else if (args.length == 7) {
                host = args[3];
                port = Integer.parseInt(args[4]);
                transport = args[5];
                //to activate subsystem must pass in all 7 parameters ex. ... https WSREMOTEAPI
                subsystem = args[6];
            }

            execute(client, user, pass, host, port, transport);

            printWriter.println("Login successful");
        } catch (Exception e) {
            printWriter.println("Login failed: " + e.getMessage());
            printWriter.println(usage());
            log.debug("Login failed for " + user + " on " + host + ":" + port + " over transport: " + transport, e);
        }

        return true;
    }

    public Subject execute(ClientMain client, String username, String password) throws Exception {
        return execute(client, username, password, "localhost", 7080, null);
    }

    public Subject execute(ClientMain client, String username, String password, String host, int port, String transport)
        throws Exception {

        //add call to different subsystem if it exists
        RemoteClient remoteClient = null;
        if ((subsystem != null) && (subsystem.trim().equalsIgnoreCase("WSREMOTEAPI"))) {
            remoteClient = new RemoteClient(transport, host, port, subsystem);
        } else {
            remoteClient = new RemoteClient(transport, host, port);
        }

        client.setTransport(remoteClient.getTransport()); // in case transport was null, let the client tell us what it'll use
        client.setHost(host);
        client.setPort(port);
        client.setUser(username);
        client.setPass(password);

        Subject subject = remoteClient.login(username, password);

        String versionUpdate = remoteClient.getServerVersionUpdate();
        String version = "";
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

    public String getSyntax() {
        return getPromptCommandString() + " username password [host port [transport]]";
    }

    public String getHelp() {
        return "Log into a server with specified username and password";
    }

    public String getDetailedHelp() {
        return "Log into a server with the specified username and password. The server host "
            + "name and port may optionally be specified. The host name defaults to "
            + "localhost and the port to 7080. You may also specify the transport "
            + "to use when communicating with the server; it must be one " //
            + "of 'servlet' or 'sslservlet'.";
    }
}
