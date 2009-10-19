package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class RhnComm {

    protected String serverUrl = "http://satellite.rhn.redhat.com";
    protected String SAT_HANDLER = "/SAT";
    protected String SATDUMP_HANDLER = "/SAT-DUMP";
    protected String XML_DUMP_VERSION = "3.3";

    public RhnComm() {
    }

    public RhnComm(String serverUrl) {
        setServerURL(serverUrl);
    }

    public void setServerURL(String url) {
        serverUrl = url;
    }

    public String getServerURL() {
        return serverUrl;
    }


    protected Map getRequestProperties() {
        Map reqProps = new HashMap();
        reqProps.put("X-RHN-Satellite-XML-Dump-Version", XML_DUMP_VERSION);
        return reqProps;
    }
            
    /**
     * Expected return header values for: X-Client-Version, X-RHN-Server-Id, X-RHN-Auth
     * X-RHN-Auth-User-Id, X-RHN-Auth-Expire-Offset, X-RHN-Auth-Server-Time
    */
    public Map login(String systemId) throws IOException, XmlRpcException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl+SAT_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        CustomReqPropTransportFactory transportFactory = new CustomReqPropTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        client.setTransportFactory(transportFactory);

        Object[] params = new Object[]{systemId};
        Map result = (Map) client.execute("authentication.login", params);
        return result;
    }

    public boolean getRPM(String systemId, String channelName, String rpmName, String saveFilePath) 
        throws IOException, XmlRpcException {

        String baseUrl = "http://satellite.rhn.redhat.com";
        String extra = "/SAT/$RHN/" + channelName + "/getPackage/" + rpmName;
        URL url = new URL(serverUrl + extra);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Map props = login(systemId);
        for (Object key: props.keySet()) {
            conn.setRequestProperty((String)key, props.get(key).toString());
        }
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getInputStream();
        OutputStream out = new FileOutputStream(saveFilePath);

        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        finally {
            in.close();
            out.close();
            conn.disconnect();
        }
        return true;
    }
}
