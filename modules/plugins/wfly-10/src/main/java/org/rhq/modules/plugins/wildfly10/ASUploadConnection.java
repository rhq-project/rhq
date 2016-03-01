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

package org.rhq.modules.plugins.wildfly10;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.modules.plugins.wildfly10.helper.ServerPluginConfiguration;

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

    private final ASConnectionParams asConnectionParams;
    private final int timeout;
    private final URI triggerAuthUri;
    private final URI uploadUri;
    private final UsernamePasswordCredentials credentials;
    private String filename;
    private File cacheFile;
    private BufferedOutputStream cacheOutputStream;

    /**
     * @deprecated as of 4.6. This class is not reusable so there is no reason not to provide the filename to the 
     * constructor. Use {@link #ASUploadConnection(ASConnectionParams, String)}  instead.
     */
    @Deprecated
    public ASUploadConnection(String host, int port, String user, String password) {
        this(new ASConnectionParamsBuilder() //
            .setHost(host) //
            .setPort(port) //
            .setUsername(user) //
            .setPassword(password) //
            .createASConnectionParams(), null);
    }

    /**
     * @deprecated as of RHQ 4.10. Use {@link #ASUploadConnection(ASConnectionParams, String)} instead.
     */
    @Deprecated
    public ASUploadConnection(String host, int port, String user, String password, String fileName) {
        this(new ASConnectionParamsBuilder() //
            .setHost(host) //
            .setPort(port) //
            .setUsername(user) //
            .setPassword(password) //
            .createASConnectionParams(), fileName);
    }

    /**
     * @deprecated as of 4.6. This class is not reusable so there is no reason not to provide the filename to the 
     * constructor. Use {@link #ASUploadConnection(ASConnection, String)} instead.
     */
    @Deprecated
    public ASUploadConnection(ASConnection asConnection) {
        this(asConnection.getAsConnectionParams(), null);
    }

    /**
     * @param asConnection the object which will provide the {@link ASConnectionParams}
     * @param fileName
     * @see #ASUploadConnection(ASConnectionParams, String)
     */
    public ASUploadConnection(ASConnection asConnection, String fileName) {
        this(asConnection.getAsConnectionParams(), fileName);
    }

    /**
     * Creates a new {@link ASUploadConnection} for a remote http management interface.
     *
     * It's the responsibility of the caller to make sure either {@link #finishUpload()} or {@link #cancelUpload()}
     * will be called to free resources this class helds.
     *
     * @param params
     */
    public ASUploadConnection(ASConnectionParams params, String filename) {
        asConnectionParams = params;
        if (asConnectionParams.getHost() == null) {
            throw new IllegalArgumentException("Management host cannot be null.");
        }
        if (asConnectionParams.getPort() <= 0 || asConnectionParams.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid port: " + asConnectionParams.getPort());
        }
        this.filename = filename;
        timeout = SOCKET_READ_TIMEOUT;
        triggerAuthUri = buildTriggerAuthUri();
        uploadUri = buildUploadUri();
        if (asConnectionParams.getUsername() != null && asConnectionParams.getPassword() != null) {
            credentials = new UsernamePasswordCredentials(asConnectionParams.getUsername(),
                asConnectionParams.getPassword());
        } else {
            credentials = null;
        }
    }

    private URI buildTriggerAuthUri() {
        try {
            return new URIBuilder() //
                .setScheme(asConnectionParams.isSecure() ? ASConnection.HTTPS_SCHEME : ASConnection.HTTP_SCHEME) //
                .setHost(asConnectionParams.getHost()) //
                .setPort(asConnectionParams.getPort()) //
                .setPath(TRIGGER_AUTH_URI) //
                .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not build auth trigger URI: " + e.getMessage(), e);
        }
    }

    private URI buildUploadUri() {
        try {
            return new URIBuilder() //
                .setScheme(asConnectionParams.isSecure() ? ASConnection.HTTPS_SCHEME : ASConnection.HTTP_SCHEME) //
                .setHost(asConnectionParams.getHost()) //
                .setPort(asConnectionParams.getPort()) //
                .setPath(UPLOAD_URI) //
                .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not build upload URI: " + e.getMessage(), e);
        }
    }

    /**
     * @deprecated as of RHQ 4.10. Use {@link #ASUploadConnection(ASConnectionParams, String)} instead.
     */
    @Deprecated
    public static ASUploadConnection newInstanceForServerPluginConfiguration(ServerPluginConfiguration pluginConfig,
        String fileName) {
        return new ASUploadConnection(ASConnectionParams.createFrom(pluginConfig), fileName);
    }

    /**
     * @deprecated as of 4.6. Instances of this class should be created with fileName supplied to the constructor. 
     * If the caller does that there is no reason for late initialization of fileName. Then use 
     * {@link #getOutputStream()} instead.  
     */
    @Deprecated
    public OutputStream getOutputStream(String fileName) {
        filename = fileName;
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
            LOG.error("Could not create outputstream for " + filename, e);
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
        if (filename == null) {
            // At this point the fileName should have been set whether at instanciation or in #getOutputStream(String)
            throw new IllegalStateException("Upload fileName is null");
        }

        closeQuietly(cacheOutputStream);

        SchemeRegistry schemeRegistry = new SchemeRegistryBuilder(asConnectionParams).buildSchemeRegistry();
        ClientConnectionManager httpConnectionManager = new BasicClientConnectionManager(schemeRegistry);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpConnectionManager);
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, SOCKET_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, timeout);

        if (credentials != null && !asConnectionParams.isClientcertAuthentication()) {
            httpClient.getCredentialsProvider().setCredentials(
                new AuthScope(asConnectionParams.getHost(), asConnectionParams.getPort()), credentials);

            // If credentials were provided, we will first send a GET request to trigger the authentication challenge
            // This allows to send the potentially big file only once to the server
            // The typical resulting http exchange would be:
            //
            // GET without auth <- 401 (start auth challenge : the server will name the realm and the scheme)
            // GET with auth <- 200
            // POST big file
            //
            // Note this only works because we use SimpleHttpConnectionManager which maintains only one HttpConnection
            //
            // A better way to avoid uploading a big file twice would be to use the header "Expect: Continue"
            // Unfortunately AS7 replies "100 Continue" even if authentication headers are not present yet
            //
            // There is no need to trigger digest authentication when client certification authentication is used

            HttpGet triggerAuthRequest = new HttpGet(triggerAuthUri);
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
        }

        String uploadURL = (asConnectionParams.isSecure() ? ASConnection.HTTPS_SCHEME : ASConnection.HTTP_SCHEME) + "://"
            + asConnectionParams.getHost() + ":" + asConnectionParams.getPort() + UPLOAD_URI;
        HttpPost uploadRequest = new HttpPost(uploadUri);
        try {

            // Now upload file with multipart POST request
            MultipartEntity multipartEntity = new MultipartEntity();
            multipartEntity.addPart(filename, new FileBody(cacheFile));
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
     * @deprecated there is no reason to expose this attribute
     */
    @Deprecated
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set upload timeout in seconds.
     * @param timeout upload timeout in seconds
     * @deprecated there is no reason to expose this attribute
     */
    @Deprecated
    public void setTimeout(int timeout) {
    }
}
