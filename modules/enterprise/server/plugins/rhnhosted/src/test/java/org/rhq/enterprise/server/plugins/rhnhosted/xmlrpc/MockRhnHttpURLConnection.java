package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;


import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.HashMap;


public class MockRhnHttpURLConnection extends HttpURLConnection {

    protected Map props;
    protected URL url;

    public MockRhnHttpURLConnection(URL u) {
        super(u);
        props = new HashMap();
    }

    @Override
    public void setRequestProperty(String key, String value) {
        props.put(key, value);
    }


    @Override
    public String getRequestProperty(String key) {
        return (String)props.get(key);
    }

    public void disconnect() {

    }

    public boolean usingProxy() {
        return false;
    }


    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream("Test Data".getBytes());
    }
}
