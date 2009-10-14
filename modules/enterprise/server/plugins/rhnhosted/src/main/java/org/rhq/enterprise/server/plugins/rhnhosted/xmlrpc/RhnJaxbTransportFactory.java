package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.net.InetSocketAddress;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;

public class RhnJaxbTransportFactory extends CustomReqPropTransportFactory {
    String jaxbDomain;

    /**
     * Creates a new factory, which creates transports for the given client.
     * @param pClient The client, which is operating the factory.
     */
    public RhnJaxbTransportFactory(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setJaxbDomain(String domain) {
        jaxbDomain = domain;
    }

    public String getJaxbDomain() {
        return jaxbDomain;
    }

    protected CustomReqPropTransport newTransportInstance() {
        RhnJaxbTransport transport = new RhnJaxbTransport(getClient());
        transport.setJaxbDomain(jaxbDomain);
        return transport;
    }
}


