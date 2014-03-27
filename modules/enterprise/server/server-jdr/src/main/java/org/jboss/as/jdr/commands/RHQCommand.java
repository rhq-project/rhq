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

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.util.Base64;

/**
 * Command to get data from RHQ
 * @author Heiko W. Rupp
 */
public class RHQCommand extends JdrCommand {

    private static final String RHQ_SERVER_STATE_JSON = "rhq-server-state.json";

    @Override
    public void execute() throws Exception {



        String port = System.getProperty("rhq.server.socket.binding.port.http","7080");
        String username = "rhqadmin";
        String password = "rhqadmin"; // TODO get the user defined one - how?
        String s = username + ":" + password;
        String encodedCredentials = Base64.encodeBytes(s.getBytes(),Base64.DONT_BREAK_LINES);

        try {
            HttpURLConnection connection;
            URL restUrl = new URL("http://localhost:" + port + "/rest/status"); // TODO what if https?
            connection = (HttpURLConnection) restUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Accept","application/json");
            connection.setRequestProperty ("Authorization", "Basic " + encodedCredentials);
            connection.connect();
            int code = connection.getResponseCode();
            System.out.println("Response code " + code + " for url " + restUrl.toExternalForm());
            if (code==200) {
                this.env.getZip().add(connection.getInputStream(), RHQ_SERVER_STATE_JSON);
            }
            else {
                this.env.getZip().add(connection.getErrorStream(), RHQ_SERVER_STATE_JSON);
            }
            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
    }
}
