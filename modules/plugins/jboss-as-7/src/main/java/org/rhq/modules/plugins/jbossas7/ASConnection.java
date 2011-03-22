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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.Nullable;

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


    JsonNode getLevelData(@Nullable String base, boolean recursive, boolean includeMetrics) throws Exception{
        String ops = null;
        if (recursive)
            ops = "recursive";
        if (includeMetrics)
            ops += "&include-runtime=true";

        return getLevelData(base,ops);
}
    /**
     * Return the default data for base
     * @param base
     * @return
     * @throws Exception
     */
    JsonNode getLevelData(@Nullable String base) throws Exception {
        return getLevelData(base,"operation=resource-description&recursive&include-runtime=true");
    }

    /**
     * Return the JSON-Ojbect for a certain path.
     *
     * @param base Path to the object/subsystem. Can be null/"" for the base objects
     * @param ops Operation to run on the api can be null
     * @return  A JSONObject encoding the level plus sub levels provided
     * @throws Exception If anything goes wrong
     */
    JsonNode getLevelData(@Nullable String base, @Nullable String ops) throws Exception {

        URL url2;
        String spec;
        if (base!=null && !base.isEmpty()) {
            if (!base.startsWith("/")) {
                spec = urlString + "/" + base;
            }
            else {
                spec = urlString + base;
            }
            if (ops!=null) {
                if (!ops.startsWith("?"))
                    ops = "?" + ops;
                spec += ops;
            }

            url2 = new URL(spec);
        }
        else
            url2 = url;

        JsonNode tree = null;

        URLConnection conn = url2.openConnection();
        InputStream inputStream = null;
        try {
            inputStream = conn.getInputStream();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return tree;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(
                inputStream));
        try {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = in.readLine()) != null) {
                builder.append(line);
            }

            ObjectMapper mapper = new ObjectMapper();

            tree = mapper.readTree(builder.toString());

        } catch (IOException ioe) {
            System.err.println("for in put " + url2 + " : " + ioe.getMessage());
        } finally {
            in.close();
        }

        return tree;
    }


    boolean isErrorReply(JsonNode in) {
        if (in.has("outcome")) {
            String outcome = null;
            try {
                JsonNode outcomeNode = in.findValue("outcome");
                outcome = outcomeNode.getTextValue();
                if (outcome.equals("failed")) {
                    JsonNode reasonNode = in.findValue("failure-description");
                    String reason = reasonNode.getTextValue();
                    log.info(reason);
                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace(); // TODO
                return true;
            }
        }
        return false;
    }


}
