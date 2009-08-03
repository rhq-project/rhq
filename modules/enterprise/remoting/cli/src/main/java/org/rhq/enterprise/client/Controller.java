/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.client;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.commands.LoginCommand;
import org.rhq.enterprise.client.commands.LogoutCommand;
import org.rhq.enterprise.server.exception.LoginException;

public class Controller {

    private ClientMain client;

    public Controller(ClientMain client) {
        this.client = client;
    }

    public Subject login(String username, String password) throws Exception {
        LoginCommand cmd = (LoginCommand) client.getCommands().get("login");
        return cmd.execute(client, username, password);
    }

    public Subject login(String username, String password, String host, int port) throws LoginException {
        LoginCommand cmd = (LoginCommand) client.getCommands().get("login");
        return cmd.execute(client, username, password, host, port);
    }

    public void logout() {
        LogoutCommand cmd = (LogoutCommand) client.getCommands().get("logout");
        cmd.execute(client);
    }

}
