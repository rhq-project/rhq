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
package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;


import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;

public class RhnSSLTransport extends XmlRpcSun15HttpTransport
{
    private final Log log = LogFactory.getLog(RhnSSLTransport.class);

    /**
     * Creates a new instance.
     * @param pClient The client controlling this instance.
     */
    public RhnSSLTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    protected SSLSocketFactory initSSLSocketFactory() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Trust always
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Trust always
                }
            }
        };

        // Install the all-trusting trust manager
        SSLContext sc = null;
        try
        {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error(e);
        }
        catch (KeyManagementException e)
        {
            e.printStackTrace();
            log.error(e);
        }
        return sc.getSocketFactory();
    }

    @Override
    protected URLConnection newURLConnection(URL url) throws IOException
    {
        URLConnection c =  super.newURLConnection(url);
        if (c instanceof HttpsURLConnection) {
            ((HttpsURLConnection)c).setSSLSocketFactory(initSSLSocketFactory());
            log.debug("SSLSocketFactory has been set with a custom TrustManager");;
        }
        return c;
    }
}
