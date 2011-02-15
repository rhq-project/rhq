/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provide connections to the AS and reading / writing date from/to it.
 * @author Heiko W. Rupp
 */
public class ASConnection {

    private final Log log = LogFactory.getLog(ASConnection.class);
    private int port;
    private String host;
    URL url;
    String urlString;

    public ASConnection(String host, int port) {
        this.host = host;
        this.port = port;

        try {
            url = new URL("http",host,port,"/domain-api");
            urlString = url.toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Return the JSON-Ojbect for a certain path.
     *
     *
     * @param base Path to the object/subsystem. Can be null/"" for the base objects
     * @param recursive Shall lover levels be recursively obtained. May generate a lot of data.
     * @param includeMetrics Should metrice be requested as well?
     * @return  A JSONObject encoding the level plus sub levels provided
     * @throws Exception If anything goes wrong
     */
    JSONObject getLevelData(String base, boolean recursive, boolean includeMetrics) throws Exception {

        URL url2;
        if (base!=null && !base.isEmpty()) {
            String spec;
            if (!base.startsWith("/")) {
                spec = urlString + "/" + base;
            }
            else {
                spec = urlString + base;
            }
            if (recursive)
                spec += "?recursive";
            if (includeMetrics)
                spec += "?include-runtime=true";  // TODO this will change ?query-metrics=true for metrics only

            url2 = new URL(spec);
        }
        else
            url2 = url;

        URLConnection conn = url2.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            conn.getInputStream()));

        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = in.readLine()) != null) {
            builder.append(line);
        }

        JSONObject object = new JSONObject(builder.toString());

        in.close();
        return object;
    }


    boolean isErrorReply(JSONObject in) {
        if (in.has("outcome")) {
            String outcome = null;
            try {
                outcome = in.getString("outcome");
                if (outcome.equals("failed")) {
                    String reason = in.getString("failure-description");
                    log.info(reason);
                    return true;
                }

            } catch (JSONException e) {
                e.printStackTrace(); // TODO
                return true;
            }
        }
        return false;
    }


}
