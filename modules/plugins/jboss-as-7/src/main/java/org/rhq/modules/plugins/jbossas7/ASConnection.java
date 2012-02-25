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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.modules.plugins.jbossas7.json.Address;
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
    private String UNPARSABLE_JSON = ",\"server-groups\":null";
    private static String EAP_PREFIX = AbstractBaseDiscovery.EAP_PREFIX;
    private static String EDG_PREFIX = AbstractBaseDiscovery.EDG_PREFIX;
    private static String NAME = "name";
    private static String OPERATION_SERVER_CONFIG = "server-config=";
    private static String JSON_SERVER_CONFIG = "{\"server-config\":\"";
    private static String JSON_NAME = "\"name\":\"";

    /**
     * Construct an ASConnection object. The real "physical" connection is done in
     * #executeRaw.
     * @param host Host of the DomainController or standalone server
     * @param port Port of the JSON api.
     * @param user user needed for authentication
     * @param password password needed for authentication
     */
    public ASConnection(String host, int port, String user, String password) {
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

        InputStream inputStream = null;
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

            String json_to_send = mapper.writeValueAsString(operation);
            //BZ:785128. removing artificial prefix for messages going back to the server.
            //check for prefixed names
            if ((json_to_send.indexOf(JSON_SERVER_CONFIG + EAP_PREFIX) > -1)
                || (json_to_send.indexOf(JSON_SERVER_CONFIG + EDG_PREFIX) > -1)
                || (json_to_send.indexOf(JSON_NAME + EAP_PREFIX) > -1)
                || (json_to_send.indexOf(JSON_NAME + EAP_PREFIX) > -1)) {

                //delve into Operation and remove prefixes
                Operation purgedOperation = purgeOperation(operation);
                if (verbose) {
                    log.warn("---------------- Purging EAP/EDG prefixes detected. Was <" + operation + "> but now <"
                        + purgedOperation + ">.");
                }
                operation = purgedOperation;
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
                JsonNode operationResult = null;
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
                log.error("IS was null and code was " + responseCode);
                //if not properly authorized sends plugin exception for visual indicator in the ui.
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw new InvalidPluginConfigurationException(
                        "Credentials for plugin to communicate with are invalid. Update Connection Settings with valid credentials.");
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

    /** This operation purges the Operation passed in of EAP/EDG prefixes in the
     *  Address portion and in the AdditionalProperties section. The returned 
     *  operation should be identical in every other way and should be used 
     *  instead of the unpurged operation which will cause unrecognized property
     *  errors.
     * 
     * @param operation to be purged.
     * @return The same operation minus the prefixes.
     */
    private Operation purgeOperation(Operation operation) {
        //purge prefixes from name
        if (operation != null) {
            boolean prefixLocated = false;

            //Ex.
            //Operation{operation='remove', address=Address{path: host=master,server-config=EAP server-six}, 
            //additionalProperties={socket-binding-port-offset=0, name=EAP server-six, auto-start=false, group=, 
            //socket-binding-group=}}

            //ADDRESS parsing
            String path = operation.getAddress().getPath();
            if (path.indexOf(OPERATION_SERVER_CONFIG + EAP_PREFIX) > -1
                || path.indexOf(OPERATION_SERVER_CONFIG + EDG_PREFIX) > -1) {
                int index = -1;
                prefixLocated = true;
                if ((index = path.indexOf(OPERATION_SERVER_CONFIG + EAP_PREFIX)) > -1) {
                    path = path.substring(0, index + OPERATION_SERVER_CONFIG.length())
                        + path.substring(index + OPERATION_SERVER_CONFIG.length() + EAP_PREFIX.length());
                }
                if ((index = path.indexOf(OPERATION_SERVER_CONFIG + EDG_PREFIX)) > -1) {
                    path = path.substring(0, index + OPERATION_SERVER_CONFIG.length())
                        + path.substring(index + OPERATION_SERVER_CONFIG.length() + EDG_PREFIX.length());
                }
            }

            //ADDITIONAL-PROPERTIES parsing
            Map<String, Object> additionalProperties = operation.getAdditionalProperties();
            if ((additionalProperties != null) && !additionalProperties.isEmpty()
                && additionalProperties.containsKey(NAME)) {
                String contents = (String) additionalProperties.get(NAME);
                if (contents.startsWith(EAP_PREFIX) || contents.startsWith(EDG_PREFIX)) {
                    prefixLocated = true;
                    if (contents.startsWith(EAP_PREFIX)) {
                        contents = contents.substring(EAP_PREFIX.length());
                    } else if (contents.startsWith(EDG_PREFIX)) {
                        contents = contents.substring(EDG_PREFIX.length());
                    }
                    additionalProperties.put(NAME, contents);
                }
            }

            if (prefixLocated) {
                //create new Operation to return.
                Operation newOperation = new Operation(operation.getOperation(), new Address(path),
                    additionalProperties);
                return newOperation;
            }
        }
        return operation;
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
            //spinder 2/22/12: if unparsable JSON detected remove it. TODO: see if fixed with later version of jackson
            if (as7ResultSerialization.indexOf(UNPARSABLE_JSON) > -1) {
                if (verbose) {
                    log.warn("------ Detected unparsable JSON <" + as7ResultSerialization + ">.");
                }
                String trimExtraJson = "";
                int index = as7ResultSerialization.indexOf(UNPARSABLE_JSON);
                trimExtraJson = as7ResultSerialization.substring(0, index)
                    + as7ResultSerialization.substring(index + UNPARSABLE_JSON.length());
                res = (isComplex) ? mapper.readValue(trimExtraJson, ComplexResult.class) : mapper.readValue(
                    trimExtraJson, Result.class);
                return res;
            }

            if (isComplex) {
                res = mapper.readValue(node, ComplexResult.class);
            } else {
                res = mapper.readValue(node, Result.class);
            }
            return res;
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
