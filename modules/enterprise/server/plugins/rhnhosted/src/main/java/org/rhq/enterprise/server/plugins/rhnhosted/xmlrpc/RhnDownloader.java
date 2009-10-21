package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

public class RhnDownloader {

    protected XmlRpcExecutor satHandler;
    protected String serverUrl;
    protected String SAT_HANDLER = "/SAT";

    public RhnDownloader(String serverUrlIn) {
        serverUrl = serverUrlIn;
        satHandler = XmlRpcExecutorFactory.getClient(serverUrl + SAT_HANDLER);
    }

    /**
     * Expected return header values for: X-Client-Version, X-RHN-Server-Id, X-RHN-Auth
     * X-RHN-Auth-User-Id, X-RHN-Auth-Expire-Offset, X-RHN-Auth-Server-Time
    */
    public Map login(String systemId) throws IOException, XmlRpcException {

        Object[] params = new Object[] { systemId };
        Map result = (Map) satHandler.execute("authentication.login", params);
        return result;
    }

    public boolean checkAuth(String systemId) throws IOException, XmlRpcException {
        Object[] params = new Object[] { systemId };
        Integer result = (Integer) satHandler.execute("authentication.check", params);
        if (result.intValue() == 1) {
            return true;
        }
        return false;
    }

    public boolean getRPM(String systemId, String channelName, String rpmName, String saveFilePath) throws IOException,
        XmlRpcException {

        String extra = "/SAT/$RHN/" + channelName + "/getPackage/" + rpmName;
        URL url = new URL(serverUrl + extra);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Map props = login(systemId);
        for (Object key : props.keySet()) {
            conn.setRequestProperty((String) key, props.get(key).toString());
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
        } finally {
            in.close();
            out.close();
            conn.disconnect();
        }
        return true;
    }

    public InputStream getRPMStream(String systemId, String locationUrl)
        throws IOException, XmlRpcException {

        URL url = new URL(locationUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Map props = login(systemId);
        for (Object key: props.keySet()) {
            conn.setRequestProperty((String)key, props.get(key).toString());
        }
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getInputStream();

        return in;
    }

    public InputStream getKickstartTreeFile(String systemId, String channelName, String ksTreeLabel, String ksFilePath)
        throws IOException, XmlRpcException {

        String extra = "/SAT/$RHN/" + channelName + "/getKickstartFile/" + ksTreeLabel + "/" + ksFilePath;
        URL url = new URL(serverUrl + extra);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Map props = login(systemId);
        for (Object key : props.keySet()) {
            conn.setRequestProperty((String) key, props.get(key).toString());
        }
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getInputStream();
        return in;
    }
}
