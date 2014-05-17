/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.jboss.as.jdr.commands;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.io.FileUtils;

/**
 * Command to get data from the running RHQ server
 * @author Libor Zoubek
 */
public class RHQStatusCommand extends JdrCommand {

    private static final String RHQ_PREFIX = "rhq" + File.separator;
    private static final String RHQ_SERVER_STATE_JSON = RHQ_PREFIX + "rhq-server-state.json";

    @Override
    public void execute() throws Exception {

        File keyFile = new File(System.getProperty("jboss.server.data.dir"), "jdr-token");
        if (!keyFile.exists() || !keyFile.canRead()) {
            this.env.getZip().add("Cannot read access token file provided by [JDR Support] RHQ server plugin", RHQ_SERVER_STATE_JSON);
            return;
        }
        String token = FileUtils.readFileToString(keyFile);

        try {
            Socket socket = new Socket(InetAddress.getByName(null), 7079);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(token);
            this.env.getZip().add(socket.getInputStream(), RHQ_SERVER_STATE_JSON);
            out.close();
            socket.close();
        }
        catch (SocketException se) {
            this.env.getZip().add(se.getMessage() + " - Make sure [JDR Support] server plugin is enabled", RHQ_SERVER_STATE_JSON);
        }
        catch (Exception e) {
            this.env.getZip().add("Failed to retrieve status : "+e.getMessage(), RHQ_SERVER_STATE_JSON);
        }
    }
}
