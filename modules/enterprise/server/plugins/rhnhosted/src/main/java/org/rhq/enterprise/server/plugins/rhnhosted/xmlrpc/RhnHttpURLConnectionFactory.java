package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import org.rhq.enterprise.server.util.MethodUtil;
import org.rhq.enterprise.server.plugins.rhnhosted.RHNSSLSocketFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.RHNConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;


public class RhnHttpURLConnectionFactory {

    final public static String RHN_MOCK_HTTP_URL_CONNECTION = "RhnMockHttpUrlConnection";
    final private static Log log = LogFactory.getLog(RhnHttpURLConnectionFactory.class);

    protected static HttpURLConnection _openConnection(URL urlIn, String sslCertPathIn) throws IOException {
        URLConnection conn = urlIn.openConnection();
        if (conn instanceof HttpsURLConnection) {
            try
            {
                ((HttpsURLConnection)conn).setSSLSocketFactory(RHNSSLSocketFactory.getSSLSocketFactory(sslCertPathIn));
            }
            catch (GeneralSecurityException e)
            {
                e.printStackTrace();
                log.error(e);
                throw new IOException(e.getMessage());
            }
            log.debug("SSLSocketFactory has been set with a custom version using certPath: " + sslCertPathIn);
        }
        return (HttpURLConnection)conn;
    }

    public static HttpURLConnection openConnection(URL urlIn, String sslCertPathIn) throws IOException {

        String propMockHttpURLConn = System.getProperty(RHN_MOCK_HTTP_URL_CONNECTION);
        if (StringUtils.isBlank(propMockHttpURLConn)) {
            return _openConnection(urlIn, sslCertPathIn);
        }
        log.info("Using a Mock HttpURLConnection object, Property for RHN_MOCK_HTTP_URL_CONNECTION is " +
                propMockHttpURLConn);
        Object[] args = { urlIn };
        HttpURLConnection retval =
                (HttpURLConnection) MethodUtil.getClassFromSystemProperty(RHN_MOCK_HTTP_URL_CONNECTION, args);
        return retval;
    }
}
