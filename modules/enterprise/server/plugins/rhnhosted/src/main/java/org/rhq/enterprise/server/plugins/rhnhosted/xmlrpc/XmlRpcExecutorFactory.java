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

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * Class responsible for handing out XmlRpcClient classes, either real or mocked.
 */
public class XmlRpcExecutorFactory {

    protected static String XML_DUMP_VERSION = "3.3";

    public static XmlRpcExecutor getClient(String url) {
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
        client.setTransportFactory(transportFactory);

        return new ApacheXmlRpcExecutor(client);
    }

    private static Map getRequestProperties() {
        Map reqProps = new HashMap();
        reqProps.put("X-RHN-Satellite-XML-Dump-Version", XML_DUMP_VERSION);
        return reqProps;
    }

}

class ApacheXmlRpcExecutor implements XmlRpcExecutor {

    private XmlRpcClient client;

    public ApacheXmlRpcExecutor(XmlRpcClient clientIn) {
        this.client = clientIn;
    }

    @Override
    public Object execute(String methodName, Object[] params) throws XmlRpcException {
        return client.execute(methodName, params);
    }

}
