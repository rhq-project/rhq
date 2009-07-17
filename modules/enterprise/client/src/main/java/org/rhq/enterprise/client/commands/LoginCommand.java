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

/**
 * @author Greg Hinkle
 * @author Simeon Pinder
 */
public class LoginCommand implements ClientCommand {

    private Controller controller;

    public void setController(Controller client) {
        controller = client;
    }

    public String getPromptCommandString() {
        return "login";
    }

    public boolean execute(ClientMain client, String[] args) {
        try {
            String user = args[1];
            String pass = args[2];
            String host;
            int port;
            if (args.length == 5) {
                host = args[3];
                port = Integer.parseInt(args[4]);
            } else {
                host = "localhost";
                port = 7080;
            }

            controller.setServer(new RHQServer(host, port));
            controller.login(user, pass);

            client.getPrintWriter().println("Login successful");
        } catch (Exception e) {
            client.getPrintWriter().println("Login failed: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    public String getSyntax() {
        return "login user pass (host) (port)";
    }

    public String getHelp() {
        return "Login to a server with specified user and password";
    }

    public String getDetailedHelp() {
        return "";
    }
}
