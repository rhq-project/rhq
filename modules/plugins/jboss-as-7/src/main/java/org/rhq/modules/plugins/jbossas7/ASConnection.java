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
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Provide connections to the AS and reading / writing date from/to it.
 * @author Heiko W. Rupp
 */
public class ASConnection {

    public static final String MANAGEMENT = "/management";
    private final Log log = LogFactory.getLog(ASConnection.class);
    URL url;
    String urlString;
    private ObjectMapper mapper;
    public static boolean verbose = false; // This is a variable on purpose, so devs can switch it on in the debugger or in the agent
    Authenticator passwordAuthenticator;
    private String host;
    private int port;
    private String FAILURE_DESCRIPTION = "\"failure-description\"";
    private String INCLUDE_DEFAULT = "include-defaults";

    /**
     * Construct an ASConnection object. The real "physical" connection is done in
     * #executeRaw.
     * @param host Host of the DomainController or standalone server
     * @param port Port of the JSON api.
     * @param user user needed for authentication
     * @param password password needed for authentication
     */
    public ASConnection(String host, int port, String user, String password) {
        if (host == null) {
            throw new IllegalArgumentException("Management host cannot be null.");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this.host = host;
        this.port = port;

        try {
            url = new URL("http", host, port, MANAGEMENT);
            urlString = url.toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        passwordAuthenticator = new AS7Authenticator(user, password);
        Authenticator.setDefault(passwordAuthenticator);

        // read system property "as7plugin.verbose"
        verbose = Boolean.getBoolean("as7plugin.verbose");

        mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Execute an operation against the domain api. This method is doing the
     * real work by talking to the remote server and sending JSON data, that
     * is obtained by serializing the operation.
     *
     * Please do not use this API , but execute()
     * @return JsonNode that describes the result
     * @param operation an Operation that should be run on the domain controller
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation)
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     * @see #executeComplex(org.rhq.modules.plugins.jbossas7.json.Operation)
     */
    public JsonNode executeRaw(Operation operation) {
        InputStream inputStream;
        BufferedReader br = null;
        InputStream es = null;
        HttpURLConnection conn = null;
        long t1 = System.currentTimeMillis();
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("Content-Type", "application/json");
            conn.addRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10 * 1000); // 10s
            conn.setReadTimeout(10 * 1000); // 10s

            if (conn.getReadTimeout() != 10 * 1000)
                log.warn("JRE uses a broken timeout mechanism - nothing we can do");

            OutputStream out = conn.getOutputStream();

            //add additional request property to include-defaults=true to all requests.
            //if it's already set we leave it alone and assume that Operation creator is taking over control.
            if (operation.getAdditionalProperties().isEmpty()
                || !operation.getAdditionalProperties().containsKey(INCLUDE_DEFAULT)) {
                operation.addAdditionalProperty("include-defaults", "true");
            }

            String json_to_send = mapper.writeValueAsString(operation);

            //check for spaces in the path which the AS7 server will reject. Log verbose error and
            // generate failure indicator.
            if ((operation != null) && (operation.getAddress() != null) && operation.getAddress().getPath() != null) {
                if (containsSpaces(operation.getAddress().getPath())) {
                    Result noResult = new Result();
                    String outcome = "- Path '" + operation.getAddress().getPath()
                        + "' in invalid as it cannot contain spaces -";
                    if (verbose) {
                        log.error(outcome);
                    }
                    noResult.setFailureDescription(outcome);
                    noResult.setOutcome("failure");
                    JsonNode invalidPathResult = mapper.valueToTree(noResult);
                    return invalidPathResult;
                }
            }

            if (verbose) {
                log.info("Json to send: " + json_to_send);
            }

            mapper.writeValue(out, operation);

            out.flush();
            out.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            if (inputStream != null) {

                br = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    builder.append(line);
                }

                String outcome;
                JsonNode operationResult;
                if (builder.length() > 0) {
                    outcome = builder.toString();
                    operationResult = mapper.readTree(outcome);
                    if (verbose) {
                        ObjectMapper om2 = new ObjectMapper();
                        om2.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
                        String tmp = om2.writeValueAsString(operationResult);
                        log.info(tmp);
                    }
                } else {
                    outcome = "- no response from server -";
                    Result noResult = new Result();
                    noResult.setFailureDescription(outcome);
                    noResult.setOutcome("failure");
                    operationResult = mapper.valueToTree(noResult);
                }
                return operationResult;
            } else {
                //if not properly authorized sends plugin exception for visual indicator in the ui.
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED
                    || responseCode == HttpURLConnection.HTTP_BAD_METHOD) {
                    if (log.isDebugEnabled()) {
                        log.debug("[" + url + "] Response was empty and response code was " + responseCode + " "
                            + conn.getResponseMessage() + ".");
                    }
                    throw new InvalidPluginConfigurationException(
                        "Credentials for plugin to connect to AS7 management interface are invalid. Update Connection Settings with valid credentials.");
                } else {
                    log.error("[" + url + "] Response was empty and response code was " + responseCode + " "
                        + conn.getResponseMessage() + ".");
                }
            }
        } catch (IllegalArgumentException iae) {
            log.error("Illegal argument " + iae);
            log.error("  for input " + operation);
        } catch (SocketTimeoutException ste) {
            log.error("Request to AS timed out " + ste.getMessage());
            conn.disconnect();
            Result failure = new Result();
            failure.setFailureDescription(ste.getMessage());
            failure.setOutcome("failure");
            failure.setRhqThrowable(ste);

            JsonNode ret = mapper.valueToTree(failure);
            return ret;

        } catch (IOException e) {
            log.error("Failed to get data: " + e.getMessage());

            //the following code is in place to help keep-alive http connection re-use to occur.
            if (conn != null) {//on error conditions it's still necessary to read the response so JDK knows can reuse
                //the http connections behind the scenes.
                es = conn.getErrorStream();
                if (es != null) {
                    BufferedReader dr = new BufferedReader(new InputStreamReader(es));
                    String ignore = null;
                    try {
                        while ((ignore = dr.readLine()) != null) {
                            //already reported error. just empty stream.
                        }
                        es.close();
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }

            Result failure = new Result();
            failure.setFailureDescription(e.getMessage());
            failure.setOutcome("failure");
            failure.setRhqThrowable(e);

            JsonNode ret = mapper.valueToTree(failure);
            return ret;

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            if (es != null) {
                try {
                    es.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            long t2 = System.currentTimeMillis();
            PluginStats stats = PluginStats.getInstance();
            stats.incrementRequestCount();
            stats.addRequestTime(t2 - t1);
        }

        return null;
    }

    /** Method parses Operation.getAddress().getPath() for invalid spaces in the path passed in.
     * 
     * @param path Operation.getAddress().getPath() value.
     * @return boolean indicating invalid spaces found.
     */
    private boolean containsSpaces(String path) {
        boolean includesSpaces = false;
        StringTokenizer components = new StringTokenizer(path, " ");
        if (components.countTokens() > 1) {
            includesSpaces = true;
        }
        return includesSpaces;
    }

    /**
     * Execute the passed Operation and return its Result. This is a shortcut of
     * #execute(Operation, false)
     * @param op Operation to execute
     * @return Result of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public Result execute(Operation op) {
        return execute(op, false);
    }

    /**
     * Execute the passed Operation and return its ComplexResult. This is a shortcut of
     * #execute(Operation, true)
     * @param op Operation to execute
     * @return ComplexResult of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public ComplexResult executeComplex(Operation op) {
        return (ComplexResult) execute(op, true);
    }

    /**
     * Execute the passed Operation and return its Result. Depending on <i>isComplex</i>
     * the return type is a simple Result or a ComplexResult
     * @param op Operation to execute
     * @param isComplex should a complex result be returned?
     * @return ComplexResult of the execution
     */
    public Result execute(Operation op, boolean isComplex) {
        JsonNode node = executeRaw(op);

        if (node == null) {
            log.warn("Operation [" + op + "] returned null");
            Result failure = new Result();
            failure.setFailureDescription("Operation [" + op + "] returned null");
            return failure;
        }
        Result res;
        try {

            //check for failure-description indicator, otherwise ObjectMapper will try to deserialize as json. Ex.
            // {"outcome":"failed","failure-description":"JBAS014792: Unknown attribute number-of-timed-out-transactions","rolled-back":true}
            String as7ResultSerialization = node.toString();

            if (as7ResultSerialization.indexOf(FAILURE_DESCRIPTION) > -1) {
                if (verbose) {
                    log.warn("------ Detected 'failure-description' when communicating with server."
                        + as7ResultSerialization);
                }
                Result failure = new Result();
                int failIndex = as7ResultSerialization.indexOf(FAILURE_DESCRIPTION);
                String failMessage = "";
                failMessage = as7ResultSerialization.substring(failIndex + FAILURE_DESCRIPTION.length() + 1);
                failure.setFailureDescription("Operation <" + op + "> returned <" + failMessage + ">");
                return failure;
            }

            if (isComplex) {
                res = mapper.readValue(node, ComplexResult.class);
            } else {
                res = mapper.readValue(node, Result.class);
            }
            return res;
        } catch (IOException e) {
            log.error(e.getMessage());
            if (verbose) {
                log.error("----------- Operation execution unparsable. Request " + ":[" + op + "] Response:<" + node
                    + ">");
            }
            Result failure = new Result();
            failure.setFailureDescription("Operation <" + op + "> returned unparsable JSON, <" + node + ">.");
            return failure;
            //don't return null.
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
