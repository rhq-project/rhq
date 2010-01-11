package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcTransport;

public class CustomReqPropTransportFactory extends RhnSSLTransportFactory {

    protected Map reqProps;
    protected boolean dumpMessageToFile;
    protected String dumpMessagePath;
    protected String PROP_NAME_SAVE_XML_TO_FILE = "RhnXmlrpcSaveTempFiles";

    /**
     * Creates a new factory, which creates transports for the given client.
     * @param pClient The client, which is operating the factory.
     */
    public CustomReqPropTransportFactory(XmlRpcClient pClient) {
        super(pClient);
        String value = System.getProperty(PROP_NAME_SAVE_XML_TO_FILE);
        if (!StringUtils.isBlank(value)) {
            dumpMessageToFile = Boolean.parseBoolean(value);
        }
    }

    public void setRequestProperties(Map props) {
        reqProps = props;
    }

    public Map<String, String> getRequestProperties() {
        return reqProps;
    }

    public void setRequestProperty(String name, String value) {
        if (reqProps == null) {
            reqProps = new HashMap<String, String>();
        }
        reqProps.put(name, value);
    }

    public String getRequestProperty(String name, String value) {
        return (String) reqProps.get(name);
    }

    public void setDumpMessageToFile(boolean dump) {
        dumpMessageToFile = dump;
    }

    public boolean getDumpMessageToFile() {
        return dumpMessageToFile;
    }

    public void setDumpFilePath(String dumpPath) {
        dumpMessagePath = dumpPath;
    }

    public String getDumpFilePath() {
        return dumpMessagePath;
    }

    /**
     * Override to specify a different Transport 
     * */
    protected CustomReqPropTransport newTransportInstance() {
        return new CustomReqPropTransport(getClient());
    }

    public XmlRpcTransport getTransport() {
        CustomReqPropTransport transport = newTransportInstance();
        if (reqProps != null) {
            transport.setRequestProperties(reqProps);
        }
        transport.setSSLSocketFactory(getSSLSocketFactory());
        transport.setProxy(getProxy());
        transport.setDumpMessageToFile(dumpMessageToFile);
        transport.setDumpFilePath(dumpMessagePath);
        return transport;
    }
}
