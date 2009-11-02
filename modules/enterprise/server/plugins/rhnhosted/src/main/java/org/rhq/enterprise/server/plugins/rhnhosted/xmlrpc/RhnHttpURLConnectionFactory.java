package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import org.rhq.enterprise.server.util.MethodUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;


public class RhnHttpURLConnectionFactory {

    final public static String RHN_MOCK_HTTP_URL_CONNECTION = "RhnMockHttpUrlConnection";
    final private static Log log = LogFactory.getLog(RhnHttpURLConnectionFactory.class);

    public static HttpURLConnection openConnection(URL urlIn) throws IOException {

        String propMockHttpURLConn = System.getProperty(RHN_MOCK_HTTP_URL_CONNECTION);
        if (StringUtils.isBlank(propMockHttpURLConn)) {
            return (HttpURLConnection) urlIn.openConnection();
        }
        log.info("Using a Mock HttpURLConnection object, Property for RHN_MOCK_HTTP_URL_CONNECTION is " +
                propMockHttpURLConn);
        Object[] args = { urlIn };
        HttpURLConnection retval =
                (HttpURLConnection) MethodUtil.getClassFromSystemProperty(RHN_MOCK_HTTP_URL_CONNECTION, args);
        return retval;
    }
}
