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
import org.rhq.enterprise.client.Controller;

/**
 * @author Greg Hinkle
 */
public class LogoutCommand implements ClientCommand {

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public String getPromptCommandString() {
        return "logout";
    }

    public boolean execute(ClientMain client, String[] args) {
        controller.logout();
        client.getPrintWriter().println("Logout successful");
        
        return true;
    }

    public String getSyntax() {
        return "logout";
    }

    public String getHelp() {
        return "Logout from the server connection but leave the client running";
    }

    public String getDetailedHelp() {
        return "";
    }
}