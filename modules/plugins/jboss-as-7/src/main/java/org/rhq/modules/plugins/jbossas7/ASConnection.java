/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Provide management connections to an AS7 instance and reading/writing data from/to it.
 *
 * @author Heiko W. Rupp
 */
public class ASConnection {

    public static final String MANAGEMENT = "/management";
    private static final String FAILURE_DESCRIPTION = "\"failure-description\"";
    private static final String INCLUDE_DEFAULT = "include-defaults";

    // This is a variable on purpose, so devs can switch it on in the debugger or in the agent
    public static boolean verbose = false;

    private final Log log = LogFactory.getLog(ASConnection.class);

    private URL url;
    private String urlString;
    private ObjectMapper mapper;
    private Authenticator passwordAuthenticator;
    private String host;
    private int port;

    /**
     * Construct an ASConnection object. The real "physical" connection is done in {@link #executeRaw(Operation)}.
     *
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
        return executeRaw(operation, 20);
    }

    /**
     * Execute an operation against the domain api. This method is doing the
     * real work by talking to the remote server and sending JSON data, that
     * is obtained by serializing the operation.
     *
     * Please do not use this API, but rather use {@link #execute(Operation)}.
     *
     * @param operation an Operation that should be run on the domain controller
     * @param timeoutSec Timeout on connect and read in seconds
     *
     * @return JsonNode that describes the result
     *
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation)
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     * @see #executeComplex(org.rhq.modules.plugins.jbossas7.json.Operation)
     */
    public JsonNode executeRaw(Operation operation, int timeoutSec) {
        long requestStartTime = System.currentTimeMillis();

        HttpURLConnection conn;
        OutputStream out;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("Content-Type", "application/json");
            conn.addRequestProperty("Accept", "application/json");
            int timeoutMillis = timeoutSec * 1000;
            conn.setConnectTimeout(timeoutMillis);
            conn.setReadTimeout(timeoutMillis);

            if (conn.getReadTimeout() != timeoutMillis) {
                log.warn("The JRE uses a broken timeout mechanism - nothing we can do.");
            }

            out = conn.getOutputStream();
        } catch (IOException e) {
            // This most likely just means the server is down.
            if (log.isDebugEnabled()) {
                log.debug("Failed to open connection to [" + urlString + "] in order to invoke [" + operation + "]: "
                        + e);
            }
            // TODO (ips): Would it make more sense to return null here, since we didn't even connect?
            Result failure = new Result();
            failure.setFailureDescription(e.getMessage());
            failure.setOutcome("failure");
            failure.setRhqThrowable(e);
            JsonNode ret = mapper.valueToTree(failure);
            return ret;
        }

        try {
            // Add additional request property to include-defaults=true to all requests.
            // If it's already set, we leave it alone and assume that Operation creator is taking over control.
            if (operation.getAdditionalProperties().isEmpty()
                || !operation.getAdditionalProperties().containsKey(INCLUDE_DEFAULT)) {
                operation.addAdditionalProperty("include-defaults", "true");
            }

            String json_to_send = mapper.writeValueAsString(operation);

            // Check for spaces in the path which the AS7 server will reject. Log verbose error and
            // generate failure indicator.
            if ((operation != null) && (operation.getAddress() != null) && operation.getAddress().getPath() != null) {
                if (containsSpaces(operation.getAddress().getPath())) {
                    Result noResult = new Result();
                    String outcome = "- Path '" + operation.getAddress().getPath() + "' is invalid as it contains spaces -";
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

            InputStream inputStream = (conn.getInputStream() != null) ? conn.getInputStream() : conn.getErrorStream();

            if (inputStream != null) {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream));
                // Note: slurp() will close the stream once it's done slurping it.
                String responseBody = StreamUtil.slurp(inputReader);

                String outcome;
                JsonNode operationResult;
                if (responseBody.length() > 0) {
                    outcome = responseBody;
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
                // Empty response body - probably some sort of error - check the response code.
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    if (log.isDebugEnabled()) {
                        log.debug("Response to " + operation + " was empty and response code was " + responseCode + " "
                            + conn.getResponseMessage() + " - throwing InvalidPluginConfigurationException...");
                    }
                    // Throw a InvalidPluginConfigurationException, so the user will get a yellow plugin connection
                    // warning message in the GUI.
                    throw new InvalidPluginConfigurationException(
                        "Credentials for plugin to connect to AS7 management interface are invalid - update Connection Settings with valid credentials.");
                } else {
                    log.warn("Response body for " + operation + " was empty and response code was " + responseCode + " ("
                            + conn.getResponseMessage() + ").");
                }
            }
        } catch (IllegalArgumentException iae) {
            log.error("Illegal argument for input " + operation + ": " + iae.getMessage());
        } catch (SocketTimeoutException ste) {
            log.error(operation + " timed out: " + ste.getMessage());
            conn.disconnect();
            Result failure = new Result();
            failure.setFailureDescription(ste.getMessage());
            failure.setOutcome("failure");
            failure.setRhqThrowable(ste);

            JsonNode ret = mapper.valueToTree(failure);
            return ret;
        } catch (IOException e) {
            // On error conditions, it's still necessary to slurp the response stream so the JDK knows it can reuse the
            // persistent HTTP connection behind the scenes.
            String responseBody;
            if (conn.getErrorStream() != null) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                // Note: slurp() will close the stream once it's done slurping it.
                responseBody = StreamUtil.slurp(errorReader);
            } else {
                responseBody = "";
            }

            String responseCodeString;
            try {
                responseCodeString = conn.getResponseCode() + " (" + conn.getResponseMessage() + ")";
            } catch (IOException ioe) {
                responseCodeString = "unknown response code";
            }
            log.error(operation + " failed with " + responseCodeString + " - response body was [" + responseBody + "].");

            Result failure = new Result();
            failure.setFailureDescription(e.getMessage());
            failure.setOutcome("failure");
            failure.setRhqThrowable(e);

            JsonNode ret = mapper.valueToTree(failure);
            return ret;
        } finally {
            long requestEndTime = System.currentTimeMillis();
            PluginStats stats = PluginStats.getInstance();
            stats.incrementRequestCount();
            stats.addRequestTime(requestEndTime - requestStartTime);
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
        return execute(op, false,10);
    }

    /**
     * Execute the passed Operation and return its Result. This is a shortcut of
     * #execute(Operation, false)
     * @param op Operation to execute
     * @param timeoutSec Timeout to wait in seconds. Default is 10 sec
     * @return Result of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public Result execute(Operation op,int timeoutSec) {
        return execute(op, false,timeoutSec);
    }

    /**
     * Execute the passed Operation and return its ComplexResult. This is a shortcut of
     * #execute(Operation, true)
     * @param op Operation to execute
     * @return ComplexResult of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public ComplexResult executeComplex(Operation op) {
        return (ComplexResult) execute(op, true,10);
    }

    /**
     * Execute the passed Operation and return its ComplexResult. This is a shortcut of
     * #execute(Operation, true)
     * @param op Operation to execute
     * @param timeoutSec Timeout to wait in seconds. Default is 10 sec
     * @return ComplexResult of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public ComplexResult executeComplex(Operation op,int timeoutSec) {
        return (ComplexResult) execute(op, true,timeoutSec);
    }

    /**
     * Execute the passed Operation and return its Result. Depending on <i>isComplex</i>
     * the return type is a simple Result or a ComplexResult. Default timeout here is 10sec
     * @param op Operation to execute
     * @param isComplex should a complex result be returned?
     * @return ComplexResult of the execution
     */
    public Result execute(Operation op, boolean isComplex) {
        return execute(op, isComplex, 10);
    }

    /**
     * Execute the passed Operation and return its Result. Depending on <i>isComplex</i>
     * the return type is a simple Result or a ComplexResult
     *
     * @param op Operation to execute
     * @param isComplex should a complex result be returned?
     * @param timeoutSec
     * @return ComplexResult of the execution
     */
    public Result execute(Operation op, boolean isComplex, int timeoutSec) {
        JsonNode node = executeRaw(op,timeoutSec);

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
