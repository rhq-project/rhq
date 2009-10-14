package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.apache.xmlrpc.serializer.XmlRpcWriter;
import org.apache.xmlrpc.util.SAXParsers;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class RhnJaxbTransport extends CustomReqPropTransport {

    String jaxbDomain;

    public RhnJaxbTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setJaxbDomain(String domain) {
        jaxbDomain = domain;
    }

    public String getJaxbDomain() {
        return jaxbDomain;
    }

    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {
       ///*
        String data = "";
        try {
            System.err.println("inside RhnJaxbTransport " + pStream.available()
                    + " bytes available.");
            BufferedReader reader = new BufferedReader(new InputStreamReader(pStream));
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            data = sb.toString();
            //System.err.println("Contents = " + data);
            FileWriter fstream = new FileWriter("/tmp/rhnhosted-xmlrpc-debug_dump.xml");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data);
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        ByteArrayInputStream str = new ByteArrayInputStream(data.getBytes());
        //*/

        /**
         * Point of this method is to not require the traditional "methodResponse" xml wrapping
         * around the response.  RHN just returns the pure XML data.
         * */
        try {
            JAXBContext jc = JAXBContext.newInstance(jaxbDomain);
            Unmarshaller u = jc.createUnmarshaller();
            //return u.unmarshal(pStream);
            return u.unmarshal(str);
        }
        catch (JAXBException e) {
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        }
    }
}
