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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.Nullable;

import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.NameValuePair;
import org.rhq.modules.plugins.jbossas7.json.Operation;

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
    private StringBuilder builder;

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
     * @param ops OperationDescription to run on the api can be null
     * @return  A JSONObject encoding the level plus sub levels provided
     * @throws Exception If anything goes wrong
     */
    JsonNode getLevelData(@Nullable String base, @Nullable String ops) throws Exception {

        URL url2;
        String spec;
        url2 = getBaseUrl(base, ops);

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

    JsonNode getAttributeValue(@Nullable String base, @Nullable String attributeName) throws Exception {
        String op = "operation=attribute&name=" + attributeName;
        return getLevelData(base,op);
    }


    boolean isErrorReply(JsonNode in) {
        if (in == null)
            return true;

        if (in.has("outcome")) {
            String outcome = null;
            try {
                JsonNode outcomeNode = in.findValue("outcome");
                outcome = outcomeNode.getTextValue();
                if (outcome.equals("failed")) {
                    JsonNode reasonNode = in.findValue("failure-description");
                    if (reasonNode==null)
                        reasonNode = in.findValue("domain-failure-description");// TODO JBAS-9182
                    if (reasonNode==null)
                        reasonNode = in.findValue("host-failure-descriptions"); // TODO JBAS-9182

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

    /**
     * Execute an operation against the domain api
     * @return JsonNode that describes the result
     * @param operation an Operation that should be run on the domain controller
     */
    public JsonNode execute(Operation operation) {

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            OutputStream out = conn.getOutputStream();

            ObjectMapper mapper = new ObjectMapper();

            String result = mapper.writeValueAsString(operation);
            System.out.println("Json to send: " + result);
            mapper.writeValue(out, operation);

            out.flush();
            out.close();

            InputStream inputStream;
            if (conn.getResponseCode()==HttpURLConnection.HTTP_OK) {
                inputStream = conn.getInputStream();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        inputStream));
                String line;
                builder = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                }
                in.close();
            }
            else {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream!=null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(errorStream));
                    String line;
                    builder = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        builder.append(line);
                    }
                    br.close();
                }
            }
            String outcome;
            JsonNode operationResult=null;
            if (builder!=null) {
                outcome= builder.toString();
                operationResult = mapper.readTree(outcome);
            }
            else {
                outcome="- no response from server -";
            }
            System.out.println("==> " + outcome);
            return operationResult;


        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }

        return null;
    }


    private URL getBaseUrl(String base, String ops) throws MalformedURLException {
        String spec;
        URL url2;
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
        return url2;
    }

    public String getFailureDescription(JsonNode jsonNode) {
        if (jsonNode==null)
            return "getFailureDescription: -input was null-";
        JsonNode node = jsonNode.findValue("failure-description");
        if (node==null)
            node = jsonNode.findValue("domain-failure-description"); // TODO JBAS-9182
        if (node==null)
            node = jsonNode.findValue("host-failure-descriptions"); // TODO JBAS-9182
        return node.getValueAsText();
    }

    public String getSuccessDescription(JsonNode jsonNode) {
        if (jsonNode==null)
            return "No message found";
        JsonNode node = jsonNode.findValue("result");
        return node.getValueAsText();
    }
}
