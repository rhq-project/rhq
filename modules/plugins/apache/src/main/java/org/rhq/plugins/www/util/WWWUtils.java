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
package org.rhq.plugins.www.util;

import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.plugins.apache.AvailabilityResult.availabilityIsDown;
import static org.rhq.plugins.apache.AvailabilityResult.availabilityIsUp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.apache.AvailabilityResult;
import org.rhq.plugins.apache.AvailabilityResult.ErrorType;

/**
 * Helper class that contains methods that send HTTP requests and evaluate results.
 *
 * @author Ian Springer
 */
public class WWWUtils {

    private static final Log LOG = LogFactory.getLog(WWWUtils.class);

    // prevent instantiation
    private WWWUtils() {
    }

    /**
     * Sends a HEAD request to the passed URL and returns true if the URL was reachable.
     *
     * @param httpURL a http or https URL to check
     * @param timeout timeout, in milliseconds
     *
     * @return true if connecting to the URL succeeds, or false otherwise
     *
     * @deprecated as of RHQ 4.10. Use {@link #checkAvailability(java.net.URL, int)} instead.
     */
    @Deprecated
    public static boolean isAvailable(URL httpURL, int timeout) {
        return checkAvailability(httpURL, timeout).getAvailabilityType() == UP;
    }

    /**
     * Checks availability of the <code>httpURL</code>.
     * 
     * Will first try with HEAD request and fallback to GET.
     *
     * @param httpURL a http or https URL to check
     * @param timeout timeout, in milliseconds
     * @return an {@link AvailabilityResult}
     */
    public static AvailabilityResult checkAvailability(URL httpURL, int timeout) {

        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative.");
        }

        if (timeout == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Pinging [" + httpURL + "] with no timeout...");
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Pinging [" + httpURL + "] with timeout of " + timeout + " milliseconds...");
            }
        }

        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) httpURL.openConnection();
            connection.setInstanceFollowRedirects(false);
        } catch (IOException e) {
            String errorMessage = "Failed to open URLConnection for [" + httpURL + "].";
            LOG.error(errorMessage, e);
            return availabilityIsDown(errorMessage, e);
        }

        try {
            connection.setRequestMethod("HEAD");
        } catch (ProtocolException ignore) {
            try {
                connection.setRequestMethod("GET");
            } catch (ProtocolException e) {
                String errorMessage = "Failed to set request method to HEAD or GET on URLConnection for [" + httpURL
                    + "].";
                LOG.error(errorMessage, e);
                return availabilityIsDown(errorMessage, e);
            }
        }

        connection.setConnectTimeout(timeout);
        // Hold off on setting the read timeout until after we connect.

        if (connection instanceof HttpsURLConnection) {
            disableCertificateVerification((HttpsURLConnection) connection);
        }

        // First just connect to the HTTP server.
        long connectStartTime = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connecting to [" + httpURL + "]...");
        }
        try {
            connection.connect();
        } catch (IOException e) {
            String errorMessage;
            ErrorType errorType = ErrorType.UNKNOWN;
            if (e instanceof ConnectException) {
                // This most likely just means the server is down.
                errorMessage = "Failed to connect to [" + httpURL + "].";
                errorType = ErrorType.CANNOT_CONNECT;
            } else if (e instanceof SocketTimeoutException) {
                // This probably means the server is up but not properly accepting connection requests.
                long connectDuration = System.currentTimeMillis() - connectStartTime;
                errorMessage = "Attempt to connect to [" + httpURL + "] timed out after " + connectDuration
                    + " milliseconds.";
                errorType = ErrorType.CONNECTION_TIMEOUT;
            } else {
                errorMessage = "An error occurred while attempting to connect to [" + httpURL + "].";
            }
            if (LOG.isDebugEnabled()) {
                LOG.warn(errorMessage, e);
            }
            return availabilityIsDown(errorType, errorMessage, e);
        }
        int connectDuration = (int) (System.currentTimeMillis() - connectStartTime);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connected to [" + httpURL + "] in " + connectDuration + " milliseconds.");
        }

        if ((timeout > 0) && (connectDuration >= timeout)) {
            String errorMessage = "Attempt to ping [" + httpURL + "] timed out after " + connectDuration
                + " milliseconds.";
            if (LOG.isDebugEnabled()) {
                LOG.warn(errorMessage);
            }
            return availabilityIsDown(ErrorType.CONNECTION_TIMEOUT, errorMessage);
        }

        try {
            int readTimeout = (timeout > 0) ? (timeout - connectDuration) : 0;
            connection.setReadTimeout(readTimeout);
            if (connection.getReadTimeout() != readTimeout) {
                if (LOG.isDebugEnabled()) {
                    LOG.warn("Failed to set read timeout on URLConnection for [" + httpURL
                            + "] - this most likely means we're running in a non-standard JRE.");
                }
            }

            // Now actually send the request and read the response.
            long readStartTime = System.currentTimeMillis();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending " + connection.getRequestMethod() + " request to [" + httpURL + "]...");
            }
            try {
                // Calling getResponseCode() will cause the request to be sent.
                int responseCode = connection.getResponseCode();
                if (LOG.isDebugEnabled()) {
                    if (responseCode == -1) {
                        LOG.warn("Ping request to [" + httpURL + "] returned an invalid response: "
                            + getResponseBody(connection));
                    } else if (responseCode >= 500) {
                        LOG.warn("Ping request to [" + httpURL + "] returned a response with server error "
                            + responseCode + " (" + connection.getResponseMessage() + "): "
                            + getResponseBody(connection));
                    } else if (responseCode >= 400) {
                        LOG.warn("Ping request to [" + httpURL + "] returned a response with client error "
                            + responseCode + " (" + connection.getResponseMessage() + ").");
                    }
                }
            } catch (IOException e) {
                String errorMessage;
                ErrorType errorType = ErrorType.UNKNOWN;
                if (e instanceof SocketTimeoutException) {
                    long readDuration = System.currentTimeMillis() - readStartTime;
                    errorMessage = "Attempt to read response from " + connection.getRequestMethod() + " request to ["
                        + httpURL + "] timed out after " + readDuration + " milliseconds.";
                    errorType = ErrorType.CONNECTION_TIMEOUT;
                } else {
                    errorMessage = "An error occurred while attempting to read response from "
                        + connection.getRequestMethod() + " to [" + httpURL + "].";
                }
                if (LOG.isDebugEnabled()) {
                    LOG.warn(errorMessage, e);
                }
                return availabilityIsDown(errorType, errorMessage, e);
            }
            long readDuration = System.currentTimeMillis() - readStartTime;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Read response from " + connection.getRequestMethod() + " request to [" + httpURL + "] in "
                    + readDuration + " milliseconds.");
            }
        } finally {
            // We don't care about keeping the connection around. We're only going to be pinging each server once every
            // minute.
            connection.disconnect();
        }

        return availabilityIsUp();
    }

    /**
     * Get the content of the 'Server' header.
     *
     * @param httpURL a http or https URL to get the header from
     *
     * @return the contents of the header or null if anything went wrong or the field was not present.
     */
    public static String getServerHeader(URL httpURL) {
        String ret;

        try {
            HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(1000);

            connection.connect();
            // Get the response code to actually trigger sending the request.
            connection.getResponseCode();
            ret = connection.getHeaderField("Server");
        } catch (IOException e) {
            ret = null;
        }
        return ret;
    }



    /**
     * Get the response code.
     *
     * @param httpURL a http or https URL to get the header from
     *
     * @return the response code to actually trigger sending the request.
     */
    public static int getResponseCode(URL httpURL) {
        try {
            HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(1000);

            connection.connect();
            // Get the response code to actually trigger sending the request.
            return connection.getResponseCode();
        } catch (IOException e) {
            return -1;
        }
    }

    private static TrustManager NO_OP_TRUST_MANAGER = new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
            return;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
            return;
        }
    };

    private static HostnameVerifier NO_OP_HOSTNAME_VERIFIER = new HostnameVerifier() {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
        return true;
        }
    };

    // This method has been added in support of https://bugzilla.redhat.com/show_bug.cgi?id=690430.
    private static void disableCertificateVerification(HttpsURLConnection connection) {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{NO_OP_TRUST_MANAGER}, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setHostnameVerifier(NO_OP_HOSTNAME_VERIFIER);
        } catch (Exception e) {
            logWarnWithStackTraceOnlyIfDebugEnabled("Failed to disable certificate and hostname validation on URLConnection for ["
                    + connection.getURL() + "].", e);
        }
    }

    private static void logWarnWithStackTraceOnlyIfDebugEnabled(String message, Exception e) {
        if (LOG.isDebugEnabled()) {
            LOG.warn(message, e);
        } else {
            LOG.warn(message + " (enable DEBUG logging to see stack trace): " + e);
        }
    }

    private static String getResponseBody(HttpURLConnection connection) {
        String responseBody;
        try {
            InputStream inputStream = (connection.getInputStream() != null) ? connection.getInputStream() :
                    connection.getErrorStream();
            responseBody = (inputStream != null) ? StreamUtil.slurp(new InputStreamReader(inputStream)) : "";
        } catch (IOException e) {
            responseBody = "";
        }
        return responseBody;
    }

}
