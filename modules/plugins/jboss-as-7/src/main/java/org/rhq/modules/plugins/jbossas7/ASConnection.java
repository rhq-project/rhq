/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import org.apache.commons.httpclient.UsernamePasswordCredentials;
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

    private UsernamePasswordCredentials credentials;

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

        // This will hold a reference to user and password
        // and will be used in the future when switching this class communication layer to httpclient
        credentials = new UsernamePasswordCredentials(user, password);

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
            String jsonToSend = mapper.writeValueAsString(operation);

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
                log.info("JSON to send: " + jsonToSend);
            }

            mapper.writeValue(out, operation);

            out.flush();
            out.close();

            ResponseStatus responseStatus = new ResponseStatus(conn);
            if (isAuthorizationFailureResponse(responseStatus.getResponseCode())) {
                handleAuthorizationFailureResponse(operation, responseStatus);
            }

            String responseBody = getResponseBody(conn);
            if (responseStatus.getResponseCode() >= 400) {
                if (verbose) {
                    log.debug(operation + " failed with " + responseStatus + " - response body was [" + responseBody
                        + "].");
                }

                if (responseBody.contains("JBAS014807") || responseBody.contains("JBAS010850")
                    || responseBody.contains("JBAS014793")) {
                    // management resource not found or not readable or no known child-type
                    if (log.isDebugEnabled()) {
                        log.debug("Requested management resource not found: " + operation.getAddress().getPath());
                    }
                } else {
                    log.warn("Received " + responseStatus + " response to " + operation + " - response body was ["
                        + responseBody + "].");
                }
            }

            JsonNode operationResult;
            if (!responseBody.isEmpty()) {
                try {
                    operationResult = mapper.readTree(responseBody);
                } catch (IOException ioe) {
                    log.error("Failed to deserialize response to " + operation + " to JsonNode - response status was "
                        + responseStatus + ", and body was [" + responseBody + "]: " + ioe);
                    Result result = new Result();
                    result.setOutcome("failure");
                    result.setFailureDescription("Failed to deserialize response to " + operation
                        + " to JsonNode - response status was " + responseStatus + ", and body was [" + responseBody
                        + "]: " + ioe);
                    result.setRolledBack(responseBody.contains("rolled-back=true"));
                    result.setRhqThrowable(ioe);
                    operationResult = mapper.valueToTree(result);
                }

                if (verbose) {
                    ObjectMapper om2 = new ObjectMapper();
                    om2.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
                    try {
                        String resultString = om2.writeValueAsString(operationResult);
                        log.info(resultString);
                    } catch (IOException ioe) {
                        log.error("Failed to convert result of " + operation + " to string.", ioe);
                    }
                }
            } else {
                Result noResult = new Result();
                noResult.setOutcome("failure");
                noResult.setFailureDescription("- empty response body with HTTP status code " + responseStatus + " -");
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
        } catch (IOException ioe) {
            conn.disconnect();
            Result failure = new Result();
            failure.setFailureDescription(ioe.getMessage());
            failure.setOutcome("failure");
            failure.setRhqThrowable(ioe);
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

    // When no management users have been configured, a 307 (Temporary Redirect) response will be returned, and
    // when authorization has failed due to an invalid username or password, a 401 (Unauthorized) response will be
    // returned.
    private boolean isAuthorizationFailureResponse(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HTTP_TEMPORARY_REDIRECT;
    }

    private void handleAuthorizationFailureResponse(Operation operation, ResponseStatus responseStatus) {
        if (log.isDebugEnabled()) {
            log.debug("Response to " + operation + " was " + responseStatus
                + " - throwing InvalidPluginConfigurationException...");
        }
        // Throw a InvalidPluginConfigurationException, so the user will get a yellow plugin connection
        // warning message in the GUI.
        String message;
        if (responseStatus.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            message = "Credentials for plugin to connect to AS7 management interface are invalid - update Connection Settings with valid credentials.";
        } else {
            message = "Authorization to AS7 failed - did you install a management user?";
        }
        throw new InvalidPluginConfigurationException(message);
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
            log.warn("Operation [" + op + "] returned null.");
            Result failure = new Result();
            failure.setFailureDescription("Operation [" + op + "] returned null.");
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

    public String getUser() {
        return credentials.getUserName();
    }

    public String getPassword() {
        return credentials.getPassword();
    }

    @NotNull
    private String getResponseBody(HttpURLConnection connection) {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            // This means the server returned a 4xx (client error) or 5xx (server error) response, e.g.:
            // "java.io.IOException: Server returned HTTP response code: 500 for URL: http://127.0.0.1:9990/management"
            // Unfortunately, AS7 incorrectly returns 500 responses for client errors (e.g. invalid resource path,
            // attribute name, etc.).
            inputStream = null;
        }
        if (inputStream == null) {
            inputStream = connection.getErrorStream();
        }
        if (inputStream == null) {
            return "";
        }

        int contentLength = connection.getHeaderFieldInt(CONTENT_LENGTH_HTTP_HEADER, -1);

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringWriter stringWriter = (contentLength != -1) ? new StringWriter(contentLength) : new StringWriter();
        BufferedWriter writer = new BufferedWriter(stringWriter);
        try {
            long numCharsCopied = 0;
            char[] buffer = new char[1024];

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

    private class ResponseStatus {

        private Integer responseCode;
        private String responseMessage;

        ResponseStatus(HttpURLConnection connection) throws IOException {
            try {
                responseCode = connection.getResponseCode();
            } catch (IOException e) {
                // try one more time
                responseCode = connection.getResponseCode();
            }

            try {
                responseMessage = connection.getResponseMessage();
            } catch (IOException e) {
                // try one more time
                responseMessage = connection.getResponseMessage();
            }

            if (responseMessage == null) {
                responseMessage = (getResponseCode() == HTTP_TEMPORARY_REDIRECT) ? "Temporary Redirect" : "";
            }
        }

        public int getResponseCode() {
            return responseCode;
        }

        @NotNull
        public String getResponseMessage() {
            return responseMessage;
        }

        @Override
        public String toString() {
            return getResponseCode() + " (" + getResponseMessage() + ")";
        }

    }

}
