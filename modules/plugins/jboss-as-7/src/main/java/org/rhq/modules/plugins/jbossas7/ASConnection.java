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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.rhq.modules.plugins.jbossas7.json.Result.FAILURE;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.util.StringUtil;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Provide management connections to an AS7 instance and reading/writing data from/to it.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 * @author Thomas Segismont
 */
public class ASConnection {
    private static final Log LOG = LogFactory.getLog(ASConnection.class);

    public static final String HTTP_SCHEME = "http";

    public static final String MANAGEMENT_URI = "/management";

    // This is a variable on purpose, so devs can switch it on in the debugger or in the agent
    public static boolean verbose = Boolean.getBoolean("as7plugin.verbose");

    /**
     * @deprecated as of 4.7. Use {@link #MANAGEMENT_URI} constant instead
     */
    @Deprecated
    public static final String MANAGEMENT = MANAGEMENT_URI;

    static final String FAILURE_NO_RESPONSE = "The server closed the connection before sending the response";

    private static final String FAILURE_SHUTDOWN = "The HTTP connection has already been shutdown";

    private static final int MAX_POOLED_CONNECTIONS = 10;

    private static final int DEFAULT_KEEPALIVE_TIMEOUT = 5 * 1000; // 5sec

    private static final String ACCEPT_HTTP_HEADER = "Accept";

    private static final String JSON_NODE_FAILURE_DESCRIPTION = "failure-description";

    // A shared scheduled executor service to free HttpClient resources
    // One thread is enough as tasks will execute quickly
    private static final ScheduledExecutorService cleanerExecutor = Executors
        .newSingleThreadScheduledExecutor(new ThreadFactory());

    private String host;

    private int port;

    private UsernamePasswordCredentials credentials;

    private long keepAliveTimeout;

    private String managementUrl;

    private DefaultHttpClient httpClient;

    private ObjectMapper mapper;

    private volatile boolean shutdown;

    /**
     * Construct an ASConnection object. The real "physical" connection is done in {@link #executeRaw(Operation)}.
     *
     * The returned instance will use the default keep alive connection timeout.
     *
     * @param host Host of the DomainController or standalone server
     * @param port Port of the JSON api.
     * @param user user needed for authentication
     * @param password password needed for authentication
     */
    public ASConnection(String host, int port, String user, String password) {
        this(host, port, user, password, null);
    }

    /**
     * Create a new instance.
     *
     * @param host                        Host of the DomainController or standalone server
     * @param port                        Port of the JSON api.
     * @param user                        User needed for authentication
     * @param password                    Password needed for authentication
     * @param managementConnectionTimeout Maximum time to keep alive a management connection. Zero and negative values
     *                                    will disable connection persistence.
     */
    public ASConnection(String host, int port, String user, String password, Long managementConnectionTimeout) {

        // Check and store the basic parameters
        if (host == null) {
            throw new IllegalArgumentException("Management host cannot be null.");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this.host = host;
        this.port = port;
        if (user != null && password != null) {
            credentials = new UsernamePasswordCredentials(user, password);
        }

        managementUrl = HTTP_SCHEME + "://" + host + ":" + port + MANAGEMENT_URI;

        // Each ASConnection instance will have its own HttpClient instance
        // HttpClient will use a pooling connection manager to allow concurrent request processing
        PoolingClientConnectionManager httpConnectionManager = new PoolingClientConnectionManager();
        httpConnectionManager.setDefaultMaxPerRoute(MAX_POOLED_CONNECTIONS);
        httpConnectionManager.setMaxTotal(MAX_POOLED_CONNECTIONS);
        httpClient = new DefaultHttpClient(httpConnectionManager);
        // Disable stale connection checking on connection lease to get better performance
        HttpParams httpParams = httpClient.getParams();
        // See http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
        HttpConnectionParams.setStaleCheckingEnabled(httpParams, false);
        keepAliveTimeout = managementConnectionTimeout == null ? DEFAULT_KEEPALIVE_TIMEOUT : managementConnectionTimeout;
        // Do not reuse connection if keep alive timeout has zero or negative value
        httpClient.setReuseStrategy(new DefaultConnectionReuseStrategy() {
            @Override
            public boolean keepAlive(HttpResponse response, HttpContext context) {
                return keepAliveTimeout > 0 && super.keepAlive(response, context);
            }
        });
        if (keepAliveTimeout > 0) {
            // The default keep-alive strategy does not expire connections if the 'Keep-Alive' header is not present
            // in the response. This strategy will apply the desired duration in this case.
            httpClient.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                @Override
                public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                    long duration = super.getKeepAliveDuration(response, context);
                    if (duration < 0 || duration > keepAliveTimeout) {
                        duration = keepAliveTimeout;
                    }
                    if (duration < keepAliveTimeout) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn(ASConnection.this.host + ":" + ASConnection.this.port
                                    + " declares a keep alive timeout value of [" + duration
                                    + "] ms. Will now use this value instead of the value from configuration ["
                                    + keepAliveTimeout + "] ms.");
                        }
                        keepAliveTimeout = duration;
                    }
                    return duration;
                }
            });
            // Initial schedule of a cleaning task. Subsequent executions will be scheduled as needed.
            // See ConnectionManagerCleaner implementation.
            cleanerExecutor.schedule(new ConnectionManagerCleaner(this), keepAliveTimeout / 2,
                    TimeUnit.MILLISECONDS);
        }
        HttpClientParams.setRedirecting(httpParams, false);
        if (credentials != null) {
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(host, port), credentials);
        }

        mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        shutdown = false;
    }


    public static ASConnection newInstanceForServerPluginConfiguration(ServerPluginConfiguration serverPluginConfig) {
        return new ASConnection(serverPluginConfig.getHostname(), serverPluginConfig.getPort(), serverPluginConfig.getUser(), serverPluginConfig.getPassword(), serverPluginConfig.getManagementConnectionTimeout());
    }

    public void shutdown() {
        // Defensive call to shutdown the HttpClient connection manager
        // If an ASConnection instance is no longer used, its cleaning task should already
        // have closed expired connections
        httpClient.getConnectionManager().shutdown();
        shutdown = true;
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
        if (shutdown) {
            return resultAsJsonNode(FAILURE, FAILURE_SHUTDOWN, null, FALSE);
        }

        long requestStartTime = System.nanoTime();

        if (addressPathContainsSpaces(operation) == TRUE) {
            // Check for spaces in the path, which the AS7 server will reject. Log verbose error and
            // generate failure indicator.
            String failureDescription = "- Path '" + operation.getAddress().getPath()
                + "' is invalid as it contains spaces -";
            if (verbose) {
                LOG.error(failureDescription);
            }
            return resultAsJsonNode(FAILURE, failureDescription, null, FALSE);
        }

        HttpPost httpPost = null;
        try {
            String jsonToSend = mapper.writeValueAsString(operation);
            if (verbose) {
                LOG.info("JSON to send: " + jsonToSend);
            }

            httpPost = initHttpPost(timeoutSec, jsonToSend);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            StatusLine statusLine = httpResponse.getStatusLine();
            if (isAuthorizationFailureResponse(statusLine)) {
                handleAuthorizationFailureResponse(operation, statusLine);
            }

            HttpEntity httpResponseEntity = httpResponse.getEntity();
            String responseBody = httpResponseEntity == null ? StringUtil.EMPTY_STRING : EntityUtils
                .toString(httpResponseEntity);
            if (verbose && statusLine.getStatusCode() >= 400) {
                logHttpError(operation, statusLine, responseBody);
            }

            JsonNode operationResult;
            if (!responseBody.isEmpty()) {
                operationResult = deserializeResponseBody(operation, statusLine, responseBody);
                if (verbose) {
                    logFormatted(operationResult);
                }
            } else {
                operationResult = resultAsJsonNode(FAILURE, "- empty response body with HTTP status code "
                    + statusAsString(statusLine) + " -", null, FALSE);
            }
            return operationResult;
        } catch (NoHttpResponseException e) {
            // For some operations like reload or shutdown, the server closes the connection before sending the
            // response. We use a specific description here so that callers can write code to decide what to do
            // in this situation.
            return resultAsJsonNode(FAILURE, FAILURE_NO_RESPONSE, e, FALSE);
        } catch (IOException e) {
            return resultAsJsonNode(FAILURE, e.getMessage(), e, FALSE);
        } finally {
            if (httpPost != null) {
                // Release of httpclient resources
                httpPost.abort();
            }
            updateStatistics(requestStartTime, System.nanoTime());
        }
    }

    private JsonNode resultAsJsonNode(String outcome, String failureDescription, Throwable rhqThrowable,
                                      Boolean rolledBack) {
        Result result = new Result();
        result.setOutcome(outcome);
        if (failureDescription != null) {
            result.setFailureDescription(failureDescription);
        }
        if (rhqThrowable != null) {
            result.setRhqThrowable(rhqThrowable);
        }
        if (rolledBack == TRUE) {
            result.setRolledBack(true);
        }
        return mapper.valueToTree(result);
    }

    private Boolean addressPathContainsSpaces(Operation operation) {
        Boolean addressPathContainsSpaces = FALSE;
        if ((operation != null) && (operation.getAddress() != null) && operation.getAddress().getPath() != null) {
            if (containsSpaces(operation.getAddress().getPath())) {
                addressPathContainsSpaces = TRUE;
            }
        }
        return addressPathContainsSpaces;
    }

    private HttpPost initHttpPost(int timeoutSec, String jsonToSend) {
        HttpPost httpPost = new HttpPost(managementUrl);
        httpPost.addHeader(ACCEPT_HTTP_HEADER, ContentType.APPLICATION_JSON.getMimeType());
        HttpParams httpParams = httpClient.getParams();
        int timeoutMillis = timeoutSec * 1000;
        HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMillis);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMillis);
        httpPost.setEntity(new StringEntity(jsonToSend, ContentType.APPLICATION_JSON));
        return httpPost;
    }

    // When no management users have been configured, a 307 (Temporary Redirect) response will be returned, and
    // when authorization has failed due to an invalid username or password, a 401 (Unauthorized) response will be
    // returned.
    private boolean isAuthorizationFailureResponse(StatusLine statusLine) {
        return statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED
            || statusLine.getStatusCode() == HttpStatus.SC_TEMPORARY_REDIRECT;
    }

    private void handleAuthorizationFailureResponse(Operation operation, StatusLine statusLine) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Response to " + operation + " was " + statusAsString(statusLine)
                    + " - throwing InvalidPluginConfigurationException...");
        }
        // Throw a InvalidPluginConfigurationException, so the user will get a yellow plugin connection
        // warning message in the GUI.
        String message;
        if (statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            message = "Credentials for plugin to connect to AS7 management interface are invalid - update Connection Settings with valid credentials.";
        } else {
            message = "Authorization to AS7 failed - did you install a management user?";
        }
        throw new InvalidPluginConfigurationException(message);
    }

    private void logHttpError(Operation operation, StatusLine statusLine, String responseBody) {
        if (responseBody.contains("JBAS014807") || responseBody.contains("JBAS010850")
                || responseBody.contains("JBAS014792") || responseBody.contains("JBAS014793")
                || responseBody.contains("JBAS014739")) {
            // management resource not found or not readable or no known child-type
            LOG.info("Requested management resource not found: " + operation.getAddress().getPath());
        } else {
            LOG.warn(operation + " failed with " + statusAsString(statusLine) + " - response body was ["
                    + responseBody + "].");
        }
    }

    private void logFormatted(JsonNode operationResult) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        try {
            LOG.info(objectMapper.writeValueAsString(operationResult));
        } catch (IOException ignore) {
        }
    }

    private JsonNode deserializeResponseBody(Operation operation, StatusLine statusLine, String responseBody) {
        JsonNode operationResult;
        try {
            operationResult = mapper.readTree(responseBody);
        } catch (IOException ioe) {
            String failureDescription = "Failed to deserialize response to " + operation
                + " to JsonNode - response status was " + statusAsString(statusLine) + ", and body was ["
                + responseBody + "]: " + ioe;
            LOG.error(failureDescription);
            operationResult = resultAsJsonNode(FAILURE, failureDescription, ioe,
                responseBody.contains("rolled-back=true"));
        }
        return operationResult;
    }

    private void updateStatistics(long requestStartTime, long requestEndTime) {
        PluginStats stats = PluginStats.getInstance();
        stats.incrementRequestCount();
        stats.addRequestTime(NANOSECONDS.toMillis(requestEndTime - requestStartTime));
    }

    /** Method parses Operation.getAddress().getPath() for invalid spaces in the path passed in.
     *
     * @param path Operation.getAddress().getPath() value.
     * @return boolean indicating invalid spaces found.
     */
    private boolean containsSpaces(String path) {
        return path.indexOf(" ") != -1;
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
            LOG.warn("Operation [" + op + "] returned null.");
            Result failure = new Result();
            failure.setFailureDescription("Operation [" + op + "] returned null.");
            return failure;
        }
        Result res;
        try {
            //check for failure-description indicator, otherwise ObjectMapper will try to deserialize as json. Ex.
            // {"outcome":"failed","failure-description":"JBAS014792: Unknown attribute number-of-timed-out-transactions","rolled-back":true}
            String as7ResultSerialization = node.toString();

            if (as7ResultSerialization.indexOf(JSON_NODE_FAILURE_DESCRIPTION) > -1) {
                if (verbose) {
                    LOG.warn("------ Detected 'failure-description' when communicating with server."
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
            LOG.error(e.getMessage());
            if (verbose) {
                LOG.error("----------- Operation execution unparsable. Request " + ":[" + op + "] Response:<" + node
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

    static String statusAsString(StatusLine statusLine) {
        String reasonPhrase = statusLine.getReasonPhrase();
        StringBuilder builder = new StringBuilder(3 + (reasonPhrase == null ? 0 : (1 + reasonPhrase.length())));
        builder.append(statusLine.getStatusCode());
        if (statusLine != null) {
            builder.append(" ").append(statusLine.getReasonPhrase());
        }
        return builder.toString();
    }

    // As soon as an ASConnection instance is created, an instance of this class is scheduled for execution.
    // Instances of this class are responsible for freeing HttpClient expired connections.
    private static final class ConnectionManagerCleaner implements Runnable {

        // Keep a weak reference to the target ASConnection to let it be garbage collected
        private WeakReference<ASConnection> asConnectionWeakReference;

        private ConnectionManagerCleaner(ASConnection asConnection) {
            asConnectionWeakReference = new WeakReference<ASConnection>(asConnection);
        }

        @Override
        public void run() {
            ASConnection asConnection = asConnectionWeakReference.get();
            if (asConnection != null && !asConnection.shutdown) {
                try {
                    asConnection.httpClient.getConnectionManager().closeExpiredConnections();
                    // Defensive call to close idle connections
                    asConnection.httpClient.getConnectionManager().closeIdleConnections(asConnection.keepAliveTimeout,
                            TimeUnit.MILLISECONDS);
                } finally {
                    // Keep cleaning the target ASConnection while it has not been marked for collection
                    cleanerExecutor.schedule(new ConnectionManagerCleaner(asConnection), asConnection.keepAliveTimeout,
                            TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private static class ThreadFactory implements java.util.concurrent.ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("ASConnection Cleaner");
            // With daemon threads, there is no need to call #shutdown on the executor to let the JVM go down
            thread.setDaemon(true);
            return thread;
        }
    }
}
