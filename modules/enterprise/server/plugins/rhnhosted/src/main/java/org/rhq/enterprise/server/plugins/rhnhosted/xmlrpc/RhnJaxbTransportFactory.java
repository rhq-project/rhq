package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import org.apache.xmlrpc.client.XmlRpcClient;

public class RhnJaxbTransportFactory extends CustomReqPropTransportFactory {
    protected String jaxbDomain;

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
