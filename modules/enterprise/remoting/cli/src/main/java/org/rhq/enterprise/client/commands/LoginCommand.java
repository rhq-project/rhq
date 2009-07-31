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

import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RHQServer;
import org.rhq.enterprise.client.Controller;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.core.domain.auth.Subject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jline.ANSIBuffer;

import javax.script.ScriptEngine;
import javax.script.ScriptContext;

/**
 * @author Greg Hinkle
 * @author Simeon Pinder
 */
public class LoginCommand implements ClientCommand {

    private final Log log = LogFactory.getLog(LoginCommand.class);

    public String getPromptCommandString() {
        return "login";
    }

    public boolean execute(ClientMain client, String[] args) {
        String user = null;
        String pass = null;
        String host = "localhost";
        int port = 7080;

        try {
            user = args[1];
            pass = args[2];

            if (args.length == 5) {
                host = args[3];
                port = Integer.parseInt(args[4]);
            }

            execute(client, user, pass, host, port);

            client.getPrintWriter().println("Login successful");
        } catch (LoginException e) {
            client.getPrintWriter().println("Login failed: " + e.getMessage());
            log.debug("Login failed for " + user + " on " + host + ":" + port, e);
        }

        return true;
    }

    public Subject execute(ClientMain client, String username, String password) throws LoginException {
        return execute(client, username, password, "localhost", 7080);
    }

    public Subject execute(ClientMain client, String username, String password, String host, int port)
            throws LoginException {
        RemoteClient remoteClient = new RemoteClient(host, port);

        client.setHost(host);
        client.setPort(port);
        client.setUser(username);
        client.setPass(password);

        Subject subject = remoteClient.getSubjectManagerRemote().login(username, password);

        remoteClient.setSubject(subject);
        remoteClient.setLoggedIn(true);

        client.setRemoteClient(remoteClient);
        client.setSubject(subject);

        bindSubject(client, subject);

        return subject;
    }

    private void bindSubject(ClientMain client, Subject subject) {
        ScriptCommand cmd = (ScriptCommand) client.getCommands().get("exec");
        ScriptEngine scriptEngine = cmd.getScriptEngine();
        scriptEngine.put("subject", subject);
        scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(client.getRemoteClient().getManagers());
    }

    public String getSyntax() {
        return "login username password [host] [port]";
    }

    public String getHelp() {
        return "Log into a server with specified username and password";
    }

    public String getDetailedHelp() {
        return "Log into a server with the specified username and password. The server host name and port may " +
                "optionally be specified. The host name defaults to localhost and the port to 7080.";
    }
}
