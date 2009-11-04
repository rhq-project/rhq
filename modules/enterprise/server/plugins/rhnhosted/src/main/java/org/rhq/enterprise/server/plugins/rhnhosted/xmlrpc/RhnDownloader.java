package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.rhq.enterprise.server.plugins.rhnhosted.RHNConstants;

public class RhnDownloader {

    protected XmlRpcExecutor satHandler;
    protected String serverUrl;
    protected String SAT_HANDLER = "/SAT";
    protected String sslCertPath;

    public RhnDownloader(String serverUrlIn, String sslCertPathIn) {
        serverUrl = serverUrlIn;
        satHandler = XmlRpcExecutorFactory.getClient(serverUrl + SAT_HANDLER);
        sslCertPath = sslCertPathIn;
    }

    public RhnDownloader(String serverUrlIn) {
        this(serverUrlIn, RHNConstants.DEFAULT_SSL_CERT_PATH);
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    public void setSSLCertPath(String sslCertPathIn) {
        sslCertPath = sslCertPathIn;
    }

    public String getSSLCertPath() {
        return sslCertPath;
    }

    protected HttpURLConnection getNewConnection(String urlIn) throws IOException {
        return (HttpURLConnection) RhnHttpURLConnectionFactory.openConnection(new URL(urlIn), sslCertPath);
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
        HttpURLConnection conn = getNewConnection(serverUrl + extra);
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

    public InputStream getRPMStream(String systemId, String locationUrl) throws IOException, XmlRpcException {

        HttpURLConnection conn = getNewConnection(locationUrl);
        Map props = login(systemId);
        for (Object key : props.keySet()) {
            conn.setRequestProperty((String) key, props.get(key).toString());
        }
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getInputStream();

        return in;
    }

    public InputStream getKickstartTreeFile(String systemId, String channelName, String ksTreeLabel, String ksFilePath)
        throws IOException, XmlRpcException {

        String extra = "/SAT/$RHN/" + channelName + "/getKickstartFile/" + ksTreeLabel + "/" + ksFilePath;
        HttpURLConnection conn = getNewConnection(serverUrl + extra);
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
