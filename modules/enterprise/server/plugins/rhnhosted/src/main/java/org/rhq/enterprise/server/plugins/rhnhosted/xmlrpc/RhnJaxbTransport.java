package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class RhnJaxbTransport extends CustomReqPropTransport {

    protected String jaxbDomain;

    private final Log log = LogFactory.getLog(RhnJaxbTransport.class);

    public RhnJaxbTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setJaxbDomain(String domain) {
        jaxbDomain = domain;
    }

    public String getJaxbDomain() {
        return jaxbDomain;
    }

    protected boolean isJaxbMessage(File message) {
        try {
            FileInputStream in = new FileInputStream(message);
            Document doc = new SAXBuilder().build(in);
            String name = doc.getRootElement().getName();
            log.info("Root element name is " + name);
            if (name.equalsIgnoreCase("rhn-satellite")) {
                return true;
            }
        } catch (JDOMException e) {
            log.error("isJaxbMessage()", e);
        } catch (IOException e) {
            log.error("isJaxbMessage()", e);
        }

        return false;
    }

    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {

        log.debug("readResponse invoked");

        long start = System.currentTimeMillis();
        File tempFile = cacheResponseToFile(pStream);
        long end = System.currentTimeMillis();
        float bandwidth = (float) tempFile.length() / (1024);
        bandwidth = (bandwidth / (end - start)) * 1000;
        log.info("response cached " + ((float) tempFile.length() / 1024) + " KB in " + (end - start)
            + "ms.  Estimated Bandwidth: " + bandwidth + " KB/sec");

        /**
         * Point of this method is to not require the traditional "methodResponse" xml wrapping
         * around the response.  RHN just returns the pure XML data.
         *
         * For error conditions, RHN defaults back to using "methodResponse".
         * We need to check what the top element is.  If it's "rhn-satellite" do JAXB parsing,
         * if it's "methodResponse" do traditional XMLRPC parsing.
         *
         * */
        FileInputStream dataStream = null;
        try {
            dataStream = new FileInputStream(tempFile);
            if (isJaxbMessage(tempFile) == false) {
                log.info("Message is not a JAXB element");
                return super.readResponse(pConfig, dataStream);
            }

            JAXBContext jc = JAXBContext.newInstance(jaxbDomain);
            Unmarshaller u = jc.createUnmarshaller();
            return u.unmarshal(dataStream);
        } catch (JAXBException e) {
            log.error(e);
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        } catch (IOException e) {
            log.error(e);
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        } finally {
            if (doWeDeleteTempFile(tempFile)) {
                log.info("Deleting temp file: " + tempFile.getAbsolutePath());
                tempFile.delete();
            } else {
                log.info("Temporary file of xmlrpc data is available at: " + tempFile.getAbsolutePath());
            }
            try {
                if (dataStream != null) {
                    dataStream.close();
                }
            } catch (Exception e) {
                ; //ignore exception from close
            }
        }
    }

}
