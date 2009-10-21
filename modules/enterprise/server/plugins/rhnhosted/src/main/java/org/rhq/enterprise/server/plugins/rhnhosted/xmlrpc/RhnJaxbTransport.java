package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.JDOMException;

public class RhnJaxbTransport extends CustomReqPropTransport {

    protected String jaxbDomain;
    protected boolean dumpMessageToFile;
    protected String dumpPath;
    private final Log log = LogFactory.getLog(RhnJaxbTransport.class);

    public RhnJaxbTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setDumpMessageToFile(boolean dump) {
        dumpMessageToFile = dump;
    }

    public boolean getDumpMessageToFile() {
        return dumpMessageToFile;
    }

    public void setDumpFilePath(String path) {
        dumpPath = path;
    }

    public String getDumpFilePath() {
        return dumpPath;
    }

    public void setJaxbDomain(String domain) {
        jaxbDomain = domain;
    }

    public String getJaxbDomain() {
        return jaxbDomain;
    }

    protected boolean isJaxbMessage(String message) {
        try {
            Document doc = new SAXBuilder().build(new ByteArrayInputStream(message.getBytes()));
            String name = doc.getRootElement().getName();
            log.info("Root element name is " + name);
            if (name.equalsIgnoreCase("rhn-satellite")) {
                return true;
            }
        }
        catch (JDOMException e) {
            log.error("isJaxbMessage()", e);
        }
        catch (IOException e) {
            log.error("isJaxbMessage()", e);
        }

        return false;
    }

    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {
        InputStream stream;
        String data = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(pStream));
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            data = sb.toString();
            log.debug("Message received is: "  + data);
            stream = new ByteArrayInputStream(data.getBytes());
            if (dumpMessageToFile) {
                FileWriter fstream = new FileWriter(getDumpFilePath());
                BufferedWriter out = new BufferedWriter(fstream);
                try {
                    out.write(data);
                }
                finally {
                    out.close();
                }
            }
        }
        catch (Exception e) {
            log.warn("RhnJaxbTransport readResponse exception", e);
            throw new XmlRpcException(e.getMessage());
        }


        /**
         * Point of this method is to not require the traditional "methodResponse" xml wrapping
         * around the response.  RHN just returns the pure XML data.
         *
         * For error conditions, RHN defaults back to using "methodResponse".
         * We need to check what the top element is.  If it's "rhn-satellite" do JAXB parsing,
         * if it's "methodResponse" do traditional XMLRPC parsing.
         *
         * */
         if (isJaxbMessage(data) == false) {
            log.info("Message is not a JAXB element");
            //Remember to use 'stream' and not 'pStream', as 'pStream' was consumed earlier
            return super.readResponse(pConfig, stream);
         }


        try {
            JAXBContext jc = JAXBContext.newInstance(jaxbDomain);
            Unmarshaller u = jc.createUnmarshaller();
            return u.unmarshal(stream);
        }
        catch (JAXBException e) {
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        }
    }
}
