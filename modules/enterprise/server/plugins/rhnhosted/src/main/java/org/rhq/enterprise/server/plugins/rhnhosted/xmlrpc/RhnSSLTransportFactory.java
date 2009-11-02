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

import java.net.Proxy;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.rhq.enterprise.server.plugins.rhnhosted.RHNConstants;


public class RhnSSLTransportFactory extends XmlRpcSun15HttpTransportFactory
{
    protected String sslCertPath = RHNConstants.DEFAULT_SSL_CERT_PATH;
     /**
     * Creates a new factory, which creates transports for the given client.
     * @param pClient The client, which is operating the factory.
     */
    public RhnSSLTransportFactory(XmlRpcClient pClient) {
        super(pClient);
    }

    /**
     * Override to specify a different Transport
     * */
    protected RhnSSLTransport newTransportInstance() {
        return new RhnSSLTransport(getClient());
    }

    public void setSSLCert(String certPath) {
        sslCertPath = certPath;
    }

    public String getSSLCert(String certPath) {
        return sslCertPath;
    }

    /**
     * We can't access the proxy member directly from our super class
     * so we are grabbing it from the transport instance.
     * Would prefer if XmlRpcSun15HttpTransportFactory had a getProxy method
     * */
    protected Proxy getProxy() {
        XmlRpcSun15HttpTransport hackGetProxy = (XmlRpcSun15HttpTransport)super.getTransport();
        return hackGetProxy.getProxy();
    }
  
    @Override
    public XmlRpcTransport getTransport()
    {
        RhnSSLTransport transport = new RhnSSLTransport(getClient());
        transport.setSSLSocketFactory(getSSLSocketFactory());
        transport.setProxy(getProxy());
        transport.setSSLCertPath(sslCertPath);
        return transport;
    }
}
