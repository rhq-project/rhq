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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.rhq.enterprise.server.plugins.rhnhosted.RHNConstants;
import org.rhq.enterprise.server.util.MethodUtil;

/**
 * Class responsible for handing out XmlRpcClient classes, either real or mocked.
 */
public class XmlRpcExecutorFactory {

    protected static String XML_DUMP_VERSION = "3.3";
    protected static String sslCertPath = RHNConstants.DEFAULT_SSL_CERT_PATH;

    private static XmlRpcExecutor getExecutor(XmlRpcClient wrappedClient) {
        Object[] args = { wrappedClient };
        XmlRpcExecutor retval = (XmlRpcExecutor) MethodUtil.getClassFromSystemProperty(ApacheXmlRpcExecutor.class
            .getName(), args);
        return retval;
    }

    public static XmlRpcExecutor getJaxbClient(String url) {
        return getJaxbClient(url, sslCertPath);
    }

    public static XmlRpcExecutor getJaxbClient(String url, String sslCertPathIn) {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

        try {
            config.setServerURL(new URL(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        transportFactory.setSSLCert(sslCertPathIn);
        client.setTransportFactory(transportFactory);
        return getExecutor(client);
    }

    public static XmlRpcExecutor getClient(String url) {
        return getClient(url, sslCertPath);
    }

    public static XmlRpcExecutor getClient(String url, String sslCertPathIn) {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

        try {
            config.setServerURL(new URL(url));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        CustomReqPropTransportFactory transportFactory = new CustomReqPropTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setSSLCert(sslCertPathIn);
        client.setTransportFactory(transportFactory);

        return getExecutor(client);
    }

    public static Map getRequestProperties() {
        Map reqProps = new HashMap();
        reqProps.put("X-RHN-Satellite-XML-Dump-Version", XML_DUMP_VERSION);
        return reqProps;
    }

}
