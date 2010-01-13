package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugins.rhnhosted.RHNConstants;

public class RhnDownloader {
    private final Log log = LogFactory.getLog(RhnDownloader.class);
    protected XmlRpcExecutor satHandler;
    protected String serverUrl;
    protected String SAT_HANDLER = "/SAT";
    protected String sslCertPath;
    protected Map<String, Map> cachedAuthMap;
    protected final String RHN_AUTH_EXPIRE_OFFSET = "X-RHN-Auth-Expire-Offset";
    protected final String RHN_AUTH_SERVER_TIME = "X-RHN-Auth-Server-Time";
    protected final String RHN_AUTH = "X-RHN-Auth";
    protected final String RHQ_CACHED_TIME = "RHQ-Time-Auth-Cached";

    public RhnDownloader(String serverUrlIn, String sslCertPathIn) {
        serverUrl = serverUrlIn;
        satHandler = XmlRpcExecutorFactory.getClient(serverUrl + SAT_HANDLER);
        sslCertPath = sslCertPathIn;
        cachedAuthMap = new HashMap<String, Map>();
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

    protected boolean verifyAuthMap(Map authProps) {
        if (authProps.containsKey(RHN_AUTH_EXPIRE_OFFSET) == false) {
            log.info("Invalid cached auth, No key for " + RHN_AUTH_EXPIRE_OFFSET + " present.");
            return false;
        }
        if (authProps.containsKey(RHN_AUTH_SERVER_TIME) == false) {
            log.info("Invalid cached auth, No key for " + RHN_AUTH_SERVER_TIME + " present.");
            return false;
        }
        if (authProps.containsKey(RHN_AUTH) == false) {
            log.info("Invalid cached auth, No key for " + RHN_AUTH + " present.");
            return false;
        }
        if (authProps.containsKey(RHQ_CACHED_TIME) == false) {
            log.info("Invalid cached auth, No key for " + RHQ_CACHED_TIME + " present.");
            return false;
        }
        long cachedTime = (Long) authProps.get(RHQ_CACHED_TIME);
        long expireOffset = Float.valueOf((String) authProps.get(RHN_AUTH_EXPIRE_OFFSET)).longValue();
        long expireTime = cachedTime + (expireOffset * 1000); //remember to convert offset from seconds to milliseconds
        long currentTime = System.currentTimeMillis();
        log.debug("Current time = " + currentTime + ", This Authentication map was cached at: " + cachedTime
            + " and will expire at " + expireTime);
        if (currentTime >= expireTime) {
            log.debug("Cached authentication is invalid, time has expired: Current time is: " + currentTime
                + ", expireTime = " + expireTime);
            return false;
        }
        return true;
    }

    public Map login(String systemId) throws IOException, XmlRpcException {
        return login(systemId, false);
    }

    /**
     * @param systemId systemid string contents
     * @param refresh true will cause a refresh of cached login info if any exists
     * @return Map with values for the keys: 
     *  X-RHN-Server-Id 
     *  X-RHN-Auth
     *  X-RHN-Auth-User-Id 
     *  X-RHN-Auth-Expire-Offset 
     *  X-RHN-Auth-Server-Time
     */
    public Map login(String systemId, boolean refresh) throws IOException, XmlRpcException {
        Map result;

        if (refresh && cachedAuthMap.containsKey(systemId)) {
            log.info("login was invoked with refresh = true, therefore removing cached auth data.");
            cachedAuthMap.remove(systemId);
        }

        if (!refresh && cachedAuthMap.containsKey(systemId)) {
            result = (Map) cachedAuthMap.get(systemId);
            if (verifyAuthMap(result)) {
                log.debug("Using cached credentials");
                return result;
            }
            log.info("Removing cached authentication data because it is invalid");
            cachedAuthMap.remove(systemId);
        }
        log.info("Calling out to remote server for authentication.login info");
        Object[] params = new Object[] { systemId };
        result = (Map) satHandler.execute("authentication.login", params);
        // Record when we are caching this auth token
        result.put(RHQ_CACHED_TIME, Long.valueOf(System.currentTimeMillis()));
        log.info("Saving authentication data to map.");
        cachedAuthMap.put(systemId, result);
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

    /**
     * @param systemId string containing system id data
     * @param locationUrl url to fetch for package/file/etc
     * @return InputStream 
     */
    public InputStream getFileStream(String systemId, String locationUrl) throws IOException, XmlRpcException {
        try {
            //
            // We are wrapping _getFileStream so we can catch an IOException and refresh our authentication credentials
            // After we have refreshed the login credentials, we'll re-try the _getFileStream call one more time
            //
            return _getFileStream(systemId, locationUrl);
        } catch (IOException e) {
            log.info("Caught exception: " + e.getMessage());
            log.info(e);
            log.info("Cached authentication might be invalid, will refresh login authentication and retry.");
            login(systemId, true);
            return _getFileStream(systemId, locationUrl);
        }
    }

    /**
     * 
     * @param systemId
     * @param locationUrl
     * @return
     * @throws IOException
     * @throws XmlRpcException
     */
    protected InputStream _getFileStream(String systemId, String locationUrl) throws IOException, XmlRpcException {

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

}
