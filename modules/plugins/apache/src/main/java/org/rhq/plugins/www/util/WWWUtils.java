/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.www.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.handler.LogicalHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class that contains methods that send http requests and evaluate results
 * @author Ian Springer
 */
public abstract class WWWUtils {

        /**
         * Sends a HEAD request to the passed url and returns if the server was reachable
         * @param  httpURL a http or https URL to check
         * @return true if connecting to the URL succeeds, or false otherwise
         */

    public static boolean isAvailable(URL httpURL, boolean disableCertValidation) {
        String failMsg = "URL [" + httpURL + "] returned unavailable";
        try {
            HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);

            if ((connection instanceof HttpsURLConnection) && disableCertValidation) {
                disableCertificateVerification((HttpsURLConnection) connection);
            }

            connection.connect();
            // get the respone code to actually trigger sending the Request.
            connection.getResponseCode();
        } catch (SSLException e) {
            Log log = LogFactory.getLog(WWWUtils.class);
            log.warn(failMsg + " due to: " + e.getLocalizedMessage(), e);
            return false;
        } catch (IOException e) {
            Log log = LogFactory.getLog(WWWUtils.class);
            log.debug(failMsg + " due to: " + e.getLocalizedMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * Get the content of the 'Server:' header
     * @param httpURL a http or https URL to get the header from
     * @return The contents of the header or null if anything went wrong or the field was not present.
     */
    public static String getServerHeader(URL httpURL) {
        String ret;

        try {
            HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.connect();
            // get the respone code to actually trigger sending the Request.
            connection.getResponseCode();
            ret = connection.getHeaderField("Server");
        }
        catch (IOException e) {
            ret = null;
        }
        return ret;
    }

    // This method has been added in support of https://bugzilla.redhat.com/show_bug.cgi?id=690430.
    private static void disableCertificateVerification(HttpsURLConnection connection) {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            Log log = LogFactory.getLog(WWWUtils.class);
            log.warn("Failed to disable certificate validation.", e);
        } catch (KeyManagementException e) {
            Log log = LogFactory.getLog(WWWUtils.class);
            log.warn("Failed to disable certificate validation.", e);
        }
    }

}