package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.net.InetSocketAddress;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;

public class RhnJaxbTransportFactory extends CustomReqPropTransportFactory {
    protected String jaxbDomain;
    protected boolean dumpMessageToFile;
    protected String dumpPath = "/tmp/rhnhosted-xmlrpc-debug_dump.xml";

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

    public void setDumpMessageToFile(boolean dump) {
        dumpMessageToFile = dump;
    }

    public boolean getDumpMessageToFile() {
        return dumpMessageToFile;
    }

    public void setDumpFilePath(String path) {
        dumpPath = path;
    }

    public String getDumpFilePath() {
        return dumpPath;
    }


    protected CustomReqPropTransport newTransportInstance() {
        RhnJaxbTransport transport = new RhnJaxbTransport(getClient());
        transport.setJaxbDomain(jaxbDomain);
        transport.setDumpMessageToFile(dumpMessageToFile);
        transport.setDumpFilePath(dumpPath);
        return transport;
    }
}


