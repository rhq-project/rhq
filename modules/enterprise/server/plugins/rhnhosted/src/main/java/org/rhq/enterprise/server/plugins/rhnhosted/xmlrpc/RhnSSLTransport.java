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

import java.security.GeneralSecurityException;
import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.rhq.enterprise.server.plugins.rhnhosted.RHNSSLSocketFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.RHNConstants;

public class RhnSSLTransport extends XmlRpcSun15HttpTransport
{
    private final Log log = LogFactory.getLog(RhnSSLTransport.class);
    private String sslCertPath = RHNConstants.DEFAULT_SSL_CERT_PATH;

    /**
     * Creates a new instance.
     * @param pClient The client controlling this instance.
     */
    public RhnSSLTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setSSLCertPath(String path) {
        sslCertPath = path;
    }

    public String getSSLCertPath() {
        return sslCertPath;
    }

    @Override
    protected URLConnection newURLConnection(URL url) throws IOException
    {
        URLConnection c =  super.newURLConnection(url);
        if (c instanceof HttpsURLConnection) {
            try
            {
                ((HttpsURLConnection)c).setSSLSocketFactory(RHNSSLSocketFactory.getSSLSocketFactory(sslCertPath));
            }
            catch (GeneralSecurityException e)
            {
                e.printStackTrace();
                log.error(e);
                throw new IOException(e.getMessage());
            }
            log.debug("SSLSocketFactory has been set with a custom version using cert path: " + sslCertPath);
        }
        return c;
    }
}
