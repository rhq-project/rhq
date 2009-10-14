package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;

public class CustomReqPropTransportFactory extends XmlRpcSun15HttpTransportFactory {

    protected Map reqProps;

    /**
     * Creates a new factory, which creates transports for the given client.
     * @param pClient The client, which is operating the factory.
     */
    public CustomReqPropTransportFactory(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setRequestProperties(Map props) {
        reqProps = props;
    }

    public Map<String,String> getRequestProperties() {
        return reqProps;
    }

    public void setRequestProperty(String name, String value) {
        if (reqProps == null) {
            reqProps = new HashMap<String,String>();
        }
        reqProps.put(name, value);
    }

    public String getRequestProperty(String name, String value) {
        return (String)reqProps.get(name);
    }

    /**
     * Override to specify a different Transport 
     * */
    protected CustomReqPropTransport newTransportInstance() {
        return new CustomReqPropTransport(getClient());
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

    public XmlRpcTransport getTransport() {
        CustomReqPropTransport transport = newTransportInstance();
        if (reqProps != null) {
            transport.setRequestProperties(reqProps);
        }
        transport.setSSLSocketFactory(getSSLSocketFactory());
        transport.setProxy(getProxy());
        return transport;
    }
}


