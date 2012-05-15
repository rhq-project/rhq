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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
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
import org.jetbrains.annotations.NotNull;

import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Provide management connections to an AS7 instance and reading/writing data from/to it.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
public class ASConnection {

    public static final String MANAGEMENT = "/management";
    private static final String FAILURE_DESCRIPTION = "\"failure-description\"";

    // This is a variable on purpose, so devs can switch it on in the debugger or in the agent
    public static boolean verbose = false;
    private static final int HTTP_TEMPORARY_REDIRECT = 307;

    private static final String POST_HTTP_METHOD = "POST";

    private static final String CONTENT_LENGTH_HTTP_HEADER = "Content-Length";
    private static final String ACCEPT_HTTP_HEADER = "Accept";
    private static final String CONTENT_TYPE_HTTP_HEADER = "Content-Type";

    private static final String JSON_MIME_TYPE = "application/json";

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
            conn.setRequestMethod(POST_HTTP_METHOD);
            conn.addRequestProperty(CONTENT_TYPE_HTTP_HEADER, JSON_MIME_TYPE);
            conn.addRequestProperty(ACCEPT_HTTP_HEADER, JSON_MIME_TYPE);
            conn.setInstanceFollowRedirects(false);
            int timeoutMillis = timeoutSec * 1000;
            conn.setConnectTimeout(timeoutMillis);
            conn.setReadTimeout(timeoutMillis);
            if (conn.getReadTimeout() != timeoutMillis) {
                log.warn("Read timeout did not get set on HTTP connection - the JRE uses a broken timeout mechanism - nothing we can do.");
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
            failure.setFailureDescription(e.toString());
            failure.setOutcome("failure");
            failure.setRhqThrowable(e);
            JsonNode ret = mapper.valueToTree(failure);
            return ret;
        }

        try {
            String json_to_send = mapper.writeValueAsString(operation);

            // Check for spaces in the path, which the AS7 server will reject. Log verbose error and
            // generate failure indicator.
            if ((operation != null) && (operation.getAddress() != null) && operation.getAddress().getPath() != null) {
                if (containsSpaces(operation.getAddress().getPath())) {
                    Result noResult = new Result();
                    String outcome = "- Path '" + operation.getAddress().getPath()
                        + "' is invalid as it contains spaces -";
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
                log.info("JSON to send: " + json_to_send);
            }

            mapper.writeValue(out, operation);

            out.flush();
            out.close();

            String responseBody = getResponseBody(conn);
            JsonNode operationResult;
            if (!responseBody.isEmpty()) {
                operationResult = mapper.readTree(responseBody);
                if (verbose) {
                    ObjectMapper om2 = new ObjectMapper();
                    om2.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
                    String tmp = om2.writeValueAsString(operationResult);
                    log.info(tmp);
                }
            } else {
                int responseCode = conn.getResponseCode();
                if (isAuthorizationFailureResponse(responseCode)) {
                    handleAuthorizationFailureResponse(operation, conn);
                }
                Result noResult = new Result();
                noResult.setOutcome("failure");

                String responseCodeString = conn.getResponseCode() + " (" + getResponseMessage(conn) + ")";
                noResult.setFailureDescription("- empty response body with HTTP status code " + responseCodeString + " -");
                operationResult = mapper.valueToTree(noResult);
            }

            return operationResult;
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
            // This typically indicates a 5xx HTTP response code (i.e. a server error). Unfortunately, AS7 returns 500
            // responses for client errors (e.g. invalid resource path, attribute name, etc.).

            // On error conditions, it's still necessary to slurp the response stream so the JDK knows it can reuse the
            // persistent HTTP connection behind the scenes.
            String responseBody = getResponseBody(conn);

            String responseCodeString;
            try {
                int responseCode = conn.getResponseCode();
                responseCodeString = responseCode + " (" + getResponseMessage(conn) + ")";

                if (isAuthorizationFailureResponse(responseCode)) {
                    handleAuthorizationFailureResponse(operation, conn);
                } else {
                    if (responseBody.isEmpty()) {
                        log.warn("Response body for " + operation + " was empty and response code was "
                                + responseCodeString + ".");
                    } else {
                        if (responseBody.contains("JBAS014807") || responseBody.contains("JBAS010850") || responseBody.contains("JBAS014793")) {
                            // management resource not found or not readable or no known child-type
                            if (log.isDebugEnabled()) {
                                log.debug("Requested management resource not found: " + operation.getAddress().getPath());
                            }
                        }
                        else {
                            log.warn("We got a " + responseCode + " with the following response body back: " + responseBody);
                        }
                    }
                }
            } catch (IOException ioe) {
                responseCodeString = "unknown response code";
            }
            String failureDescription = operation + " failed with " + responseCodeString + " - response body was ["
                + responseBody + "].";
            if (verbose)
                log.debug(failureDescription);

            JsonNode operationResult = null;
            if (!responseBody.isEmpty()) {
                try {
                    operationResult = mapper.readTree(responseBody);
                } catch (IOException ioe) {
                    log.error("Failed to deserialize response body [" + responseBody + "] to JsonNode: " + ioe);
                }
            }

            if (operationResult == null) {
                Result result = new Result();
                result.setOutcome("failure");
                result.setFailureDescription(failureDescription);
                result.setRolledBack(responseBody.contains("rolled-back=true"));
                result.setRhqThrowable(e);
                operationResult = mapper.valueToTree(result);
            }

            return operationResult;
        } finally {
            long requestEndTime = System.currentTimeMillis();
            PluginStats stats = PluginStats.getInstance();
            stats.incrementRequestCount();
            stats.addRequestTime(requestEndTime - requestStartTime);
        }

        return null;
    }

    // When no management users have been configured, a 307 (Temporary Redirect) response will be returned, and
    // when authorization has failed due to an invalid username or password, a 401 (Unauthorized) response will be
    // returned.
    private boolean isAuthorizationFailureResponse(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HTTP_TEMPORARY_REDIRECT;
    }

    private void handleAuthorizationFailureResponse(Operation operation, HttpURLConnection conn) throws IOException {
        if (log.isDebugEnabled()) {
            String responseCodeString = conn.getResponseCode() + " (" + getResponseMessage(conn) + ")";
            log.debug("Response to " + operation + " was " + responseCodeString
                + " - throwing InvalidPluginConfigurationException...");
        }
        // Throw a InvalidPluginConfigurationException, so the user will get a yellow plugin connection
        // warning message in the GUI.
        String message;
        if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            message = "Credentials for plugin to connect to AS7 management interface are invalid - update Connection Settings with valid credentials.";
        } else {
            message = "Authorization to AS7 failed - did you install a management user?";
        }
        throw new InvalidPluginConfigurationException(message);
    }

    private String getResponseMessage(HttpURLConnection conn) throws IOException {
        String responseMessage = conn.getResponseMessage();
        if ((responseMessage == null) && (conn.getResponseCode() == HTTP_TEMPORARY_REDIRECT)) {
            responseMessage = "Temporary Redirect";
        }
        return responseMessage;
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
        return execute(op, false, 10);
    }

    /**
     * Execute the passed Operation and return its Result. This is a shortcut of
     * #execute(Operation, false)
     * @param op Operation to execute
     * @param timeoutSec Timeout to wait in seconds. Default is 10 sec
     * @return Result of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public Result execute(Operation op, int timeoutSec) {
        return execute(op, false, timeoutSec);
    }

    /**
     * Execute the passed Operation and return its ComplexResult. This is a shortcut of
     * #execute(Operation, true)
     * @param op Operation to execute
     * @return ComplexResult of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public ComplexResult executeComplex(Operation op) {
        return (ComplexResult) execute(op, true, 10);
    }

    /**
     * Execute the passed Operation and return its ComplexResult. This is a shortcut of
     * #execute(Operation, true)
     * @param op Operation to execute
     * @param timeoutSec Timeout to wait in seconds. Default is 10 sec
     * @return ComplexResult of the execution
     * @see #execute(org.rhq.modules.plugins.jbossas7.json.Operation, boolean)
     */
    public ComplexResult executeComplex(Operation op, int timeoutSec) {
        return (ComplexResult) execute(op, true, timeoutSec);
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
        JsonNode node = executeRaw(op, timeoutSec);

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

    @NotNull
    private String getResponseBody(HttpURLConnection connection) {
        InputStream inputStream;
        try {
            inputStream = (connection.getInputStream() != null) ? connection.getInputStream() :
                    connection.getErrorStream();
        } catch (IOException e) {
            log.debug("Error occurred while reading response.", e);
            inputStream = null;
        }
        if (inputStream == null) {
            return "";
        }

        int available;
        try {
            available = inputStream.available();
        } catch (IOException e) {
            // The stream has most likely already been read and closed by a previous call to this method.
            available = 0;
            log.error("Possible attempt to read a response that has already been read.", e);
        }
        if (available == 0) {
            return "";
        }

        int contentLength = connection.getHeaderFieldInt(CONTENT_LENGTH_HTTP_HEADER, -1);

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringWriter stringWriter = (contentLength != -1) ? new StringWriter(contentLength) : new StringWriter();
        BufferedWriter writer = new BufferedWriter(stringWriter);
        try {
            long numCharsCopied = 0;
            char[] buffer = new char[10240];

            int cnt;
            while (((contentLength == -1) || (numCharsCopied < contentLength)) && ((cnt = reader.read(buffer)) != -1)) {
                numCharsCopied += cnt;
                writer.write(buffer, 0, cnt);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read response.", e);
        } finally {
            try {
                writer.close();
            } catch (IOException ioe) {
                log.debug("Failed to close writer.", ioe);
            }

            try {
                reader.close();
            } catch (IOException ioe) {
                log.debug("Failed to close reader.", ioe);
            }
        }

        return stringWriter.getBuffer().toString();
    }

}
