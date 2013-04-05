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

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.core.util.StringUtil;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;

/**
 * Connection for uploading of content.
 * 
 * This class needs to cache the content to be uploaded. Users of this class should:
 * <ol>
 * <li>Call {@link #getOutputStream()} an write their content to the returned {@link OutputStream}</li>
 * <li>Call {@link #finishUpload()} to actually upload the content</li>
 * </ol>
 * 
 * As instances of this class held some resources it is the caller responsibility to call {@link #cancelUpload()} 
 * instead of {@link #finishUpload()} if, for example, an {@link IOException} occured while writing their content 
 * to the {@link OutputStream}.
 * 
 * Original code taken from https://github.com/jbossas/jboss-as/blob/master/testsuite/smoke/src/test/java/org/jboss/as/test/surefire/servermodule/HttpDeploymentUploadUnitTestCase.java
 *
 * @author Jonathan Pearlin (of the original code)
 * @author Heiko W. Rupp
 * @author Thomas Segismont
 */
public class ASUploadConnection {

    private static final Log LOG = LogFactory.getLog(ASUploadConnection.class);

    private static final int SOCKET_CONNECTION_TIMEOUT = 30 * 1000; // 30sec

    private static final int SOCKET_READ_TIMEOUT = 60 * 1000; // 60sec

    private static final String TRIGGER_AUTH_URI = ASConnection.MANAGEMENT_URI;

    private static final String UPLOAD_URI = ASConnection.MANAGEMENT_URI + "/add-content";

    private static final int FILE_POST_MAX_LOGGABLE_RESPONSE_LENGTH = 1024 * 2; // 2k max 

    private static final String EMPTY_JSON_TREE = "{}";

    private static final String JSON_NODE_FAILURE_DESCRIPTION = "failure-description";

    private static final String JSON_NODE_FAILURE_DESCRIPTION_VALUE_DEFAULT = "FailureDescription: -input was null-";

    private static final String JSON_NODE_OUTCOME = "outcome";

    private static final String JSON_NODE_OUTCOME_VALUE_FAILED = "failed";

    private static final String SYSTEM_LINE_SEPARATOR = System.getProperty("line.separator");

    private String scheme = ASConnection.HTTP_SCHEME;

    private String host;

    private int port;

    private UsernamePasswordCredentials credentials;

    private String fileName;

    private int timeout;

    private File cacheFile;

    private BufferedOutputStream cacheOutputStream;

    /**
     * @deprecated as of 4.6. This class is not reusable so there is no reason not to provide the filename to the 
     * constructor. Use {@link #ASUploadConnection(String, int, String, String, String)}  instead.
     */
    @Deprecated
    public ASUploadConnection(String host, int port, String user, String password) {
        this(host, port, user, password, null);
    }

    /**
     * Creates a new {@link ASUploadConnection} for a remote http management interface.
     * 
     * If null user or password is given, this instance will not be able to reply to an authentication challenge.
     * 
     * It's the responsibility of the caller to make sure either {@link #finishUpload()} or {@link #cancelUpload()}
     * will be called to free resources this class helds.
     * 
     * @param host - hostname of the remote http management interface
     * @param port - port of the remote http management interface
     * @param user - username to logon with to the remote http management interface.
     * @param password - password to logon with to the remote http management interface
     * @param fileName - fileName of the content (to provide in multipart post request)
     */
    public ASUploadConnection(String host, int port, String user, String password, String fileName) {
        if (host == null) {
            throw new IllegalArgumentException("Management host cannot be null.");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        if (StringUtil.isBlank(fileName)) {
            throw new IllegalArgumentException("Filename cannot be blank");
        }
        this.host = host;
        this.port = port;
        if (user != null && password != null) {
            credentials = new UsernamePasswordCredentials(user, password);
        }
        this.fileName = fileName;
        this.timeout = SOCKET_READ_TIMEOUT;
    }

    /**
     * @deprecated as of 4.6. This class is not reusable so there is no reason not to provide the filename to the 
     * constructor. Use {@link #ASUploadConnection(ASConnection, String) instead.
     */
    @Deprecated
    public ASUploadConnection(ASConnection asConnection) {
        this(asConnection.getHost(), asConnection.getPort(), asConnection.getUser(), asConnection.getPassword(), null);
    }

    /**
     * Creates a new {@link ASUploadConnection} from an existing {@link ASConnection}.
     * 
     * This constructor has the same requirements as {@link #ASUploadConnection(String, int, String, String, String)} 
     * which it uses internally.
     *
     * @param asConnection - existing {@link ASConnection} instance
     * @param fileName - fileName of the content (to provide in multipart post request)
     */
    public ASUploadConnection(ASConnection asConnection, String fileName) {
        this(asConnection.getHost(), asConnection.getPort(), asConnection.getUser(), asConnection.getPassword(),
            fileName);
    }

    /**
     * This factory method simplifies creation of a new {@link ASUploadConnection} instance given the caller has
     * access to the {@link ServerPluginConfiguration}. 
     * 
     * @param pluginConfig - the {@link ServerPluginConfiguration}
     * @param fileName - fileName of the content (to provide in multipart post request)
     * @return
     */
    public static ASUploadConnection newInstanceForServerPluginConfiguration(ServerPluginConfiguration pluginConfig,
        String fileName) {
        return new ASUploadConnection(pluginConfig.getHostname(), pluginConfig.getPort(), pluginConfig.getUser(),
            pluginConfig.getPassword(), fileName);
    }

    /**
     * @deprecated as of 4.6. Instances of this class should be created with fileName supplied to the constructor. 
     * If the caller does that there is no reason for late initialization of fileName. Then use 
     * {@link #getOutputStream()} instead.  
     */
    @Deprecated
    public OutputStream getOutputStream(String fileName) {
        this.fileName = fileName;
        return getOutputStream();
    }

    /**
     * Gives an outpustream where callers should write the content which will be uploaded to AS7 
     * when {@link #finishUpload()} will be called.
     * 
     * @return an {@link OutputStream} or null if it could not be created.
     */
    public OutputStream getOutputStream() {
        try {
            cacheFile = File.createTempFile(getClass().getSimpleName(), ".cache");
            cacheOutputStream = new BufferedOutputStream(new FileOutputStream(cacheFile));
            return cacheOutputStream;
        } catch (IOException e) {
            LOG.error("Could not create outputstream for " + fileName, e);
        }
        return null;
    }

    /**
     * To be called instead of {@link #finishUpload()} if one doesn't want to upload the cached content.
     * 
     * It's important to call this method if not actually uploading as it frees resources this class helds. 
     */
    public void cancelUpload() {
        closeQuietly(cacheOutputStream);
        deleteCacheFile();
    }

    /**
     * Triggers the real upload to the AS7 instance. At this point the caller should have written 
     * the content in the {@link OutputStream} given by {@link #getOutputStream()}.
     * 
     * @return a {@link JsonNode} instance read from the upload response body or null if something went wrong.
     */
    public JsonNode finishUpload() {

        if (fileName == null) {
            // At this point the fileName should have been set whether at instanciation or in #getOutputStream(String)
            throw new IllegalStateException("Upload fileName is null");
        }

        closeQuietly(cacheOutputStream);

        // We will first send a simple get request in order to trigger authentication challenge.
        // This allows to send the potentially big file only once to the server
        // The typical resulting http exchange would be:
        //
        // GET without auth <- 401 (start auth challenge : the server will name the realm and the scheme)
        // GET with auth <- 200
        // POST big file
        //
        // Note this only works because we use SimpleHttpConnectionManager which maintains
        // only one HttpConnection
        //
        // A better way to avoid uploading a big file multiple times would be to use header Expect: Continue
        // Unfortunately AS7 responds 100 Continue even if the authentication headers are not yet present

        ClientConnectionManager httpConnectionManager = new BasicClientConnectionManager();
        DefaultHttpClient httpClient = new DefaultHttpClient(httpConnectionManager);
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, SOCKET_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, timeout);
        if (credentials != null) {
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(host, port), credentials);
        }

        String triggerAuthUrl = scheme + "://" + host + ":" + port + TRIGGER_AUTH_URI;
        HttpGet triggerAuthRequest = new HttpGet(triggerAuthUrl);
        try {
            // Send GET request in order to trigger authentication
            // We don't check response code because we're not already uploading the file
            httpClient.execute(triggerAuthRequest);
        } catch (Exception ignore) {
            // We don't stop trying upload if triggerAuthRequest raises exception
            // See comment above
        } finally {
            triggerAuthRequest.abort();
        }

        String uploadURL = scheme + "://" + host + ":" + port + UPLOAD_URI;
        HttpPost uploadRequest = new HttpPost(uploadURL);
        try {

            // Now upload file with multipart POST request
            MultipartEntity multipartEntity = new MultipartEntity();
            multipartEntity.addPart(fileName, new FileBody(cacheFile));
            uploadRequest.setEntity(multipartEntity);
            HttpResponse uploadResponse = httpClient.execute(uploadRequest);
            if (uploadResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logUploadDoesNotEndWithHttpOkStatus(uploadResponse);
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            InputStream responseBodyAsStream = uploadResponse.getEntity().getContent();
            if (responseBodyAsStream == null) {
                LOG.warn("POST request has no response body");
                return objectMapper.readTree(EMPTY_JSON_TREE);
            }
            return objectMapper.readTree(responseBodyAsStream);

        } catch (Exception e) {
            LOG.error(e);
            return null;
        } finally {
            // Release httpclient resources
            uploadRequest.abort();
            httpConnectionManager.shutdown();
            // Delete cache file
            deleteCacheFile();
        }
    }

    /**
     * Inspects the supplied {@link JsonNode} instance and returns the json 'failure-description' node value as text.
     * 
     * @param jsonNode
     * @return
     */
    public static String getFailureDescription(JsonNode jsonNode) {
        if (jsonNode == null) {
            return JSON_NODE_FAILURE_DESCRIPTION_VALUE_DEFAULT;
        }
        JsonNode node = jsonNode.findValue(JSON_NODE_FAILURE_DESCRIPTION);
        if (node == null) {
            return JSON_NODE_FAILURE_DESCRIPTION_VALUE_DEFAULT;
        }
        return node.getValueAsText();
    }

    /**
     * Inspects the supplied {@link JsonNode} instance to determine if it represents an error outcome.
     * 
     * @param jsonNode
     * @return
     */
    public static boolean isErrorReply(JsonNode jsonNode) {
        if (jsonNode == null) {
            return true;
        }

        if (jsonNode.has(JSON_NODE_OUTCOME)) {
            String outcome = null;
            try {
                JsonNode outcomeNode = jsonNode.findValue(JSON_NODE_OUTCOME);
                outcome = outcomeNode.getTextValue();
                if (outcome.equals(JSON_NODE_OUTCOME_VALUE_FAILED)) {
                    return true;
                }
            } catch (Exception e) {
                LOG.error(e);
                return true;
            }
        }
        return false;
    }

    private boolean credentialsProvided() {
        // If null user or password is given to the constructor
        // no credentials instance is created
        return credentials != null;
    }

    private void logUploadDoesNotEndWithHttpOkStatus(HttpResponse uploadResponse) {
        StringBuilder logMessageBuilder = new StringBuilder("File upload failed: ").append(ASConnection
            .statusAsString(uploadResponse.getStatusLine()));
        // If it's sure there is a response body and it's not too long 
        if (uploadResponse.getEntity().getContentLength() > 0
            && uploadResponse.getEntity().getContentLength() < FILE_POST_MAX_LOGGABLE_RESPONSE_LENGTH) {
            try {
                // It is safe to get response body as String as we know the body is not too long
                String responseBodyAsString = EntityUtils.toString(uploadResponse.getEntity());
                logMessageBuilder.append(SYSTEM_LINE_SEPARATOR).append(responseBodyAsString);
            } catch (IOException ignore) {
                // If we can't get the response body, we'll just not log it
            }
        }
        LOG.warn(logMessageBuilder.toString());
    }

    private void closeQuietly(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException ignore) {
            }
        }
    }

    private void deleteCacheFile() {
        if (cacheFile != null) {
            cacheFile.delete();
        }
    }

    /**
     * Get the currently active upload timeout
     * @return timeout in seconds
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set upload timeout in seconds.
     * @param timeout upload timeout in seconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

}
