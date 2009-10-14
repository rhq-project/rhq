package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.apache.xmlrpc.serializer.XmlRpcWriter;
import org.apache.xmlrpc.util.SAXParsers;

public class CustomReqPropTransport extends XmlRpcSun15HttpTransport {

    protected Map<String,String> reqProps;

    public CustomReqPropTransport(XmlRpcClient pClient) {
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

    protected URLConnection newURLConnection(URL pURL) throws IOException {
        URLConnection conn = super.newURLConnection(pURL);
        if (reqProps != null) {
            for (String key : reqProps.keySet()) {
                conn.addRequestProperty(key, reqProps.get(key));
            }
        }
        return conn;
    }

    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {
        /*
        String data = "";
        try {
            System.err.println("inside CustomReqPropTransport " + pStream.available() 
                    + " bytes available.");
            BufferedReader reader = new BufferedReader(new InputStreamReader(pStream));
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            data = sb.toString();
            System.err.println("Contents = " + data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        ByteArrayInputStream str = new ByteArrayInputStream(data.getBytes());
        return super.readResponse(pConfig, str);
        */
        return super.readResponse(pConfig, pStream);
    }
}
