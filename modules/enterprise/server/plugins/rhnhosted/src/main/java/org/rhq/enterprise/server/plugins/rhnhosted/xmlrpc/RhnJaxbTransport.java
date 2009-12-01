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
import java.io.StringWriter;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.JDOMException;

public class RhnJaxbTransport extends CustomReqPropTransport {

    protected String PROP_NAME_TO_SAVE_TEMP_FILES = "RhnJaxbSaveTempFiles";
    protected String jaxbDomain;
    protected boolean dumpMessageToFile;  // used during debug to save a copy of the data received
    protected String dumpPath;   // location to save
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

    protected boolean isJaxbMessage(File message) {
        try {
            FileInputStream in = new FileInputStream(message);
            Document doc = new SAXBuilder().build(in);
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

    protected File cacheResponseToFile_v2(InputStream inStream) throws XmlRpcException {

        //
        // We will read the response and write it to a file.  Prior approach read all data in memory and parsed it.
        // This created a problem when retrieving package details for large package lists of 6000+ packages.
        // We were seeing out of memory exceptions,
        // estimate on "rhel-i386-server-5" are 6200+ packages, metadata size written to disk in xml is 1.1GB
        //
        File tempFile = null;
        Reader in = null;
        BufferedWriter fileOut = null;
        try {
            if (dumpMessageToFile) {
                tempFile = new File(getDumpFilePath());
            }
            else {
                tempFile = File.createTempFile("rhn-jaxb-xmlrpc-output", ".tmp");
            }
            final char[] buffer = new char[0x100000];

            in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            fileOut = new BufferedWriter(new FileWriter(tempFile, false));

            int read;
            do {
                //long startRead = System.currentTimeMillis();
                read = in.read(buffer, 0, buffer.length);
                if (read>0) {
                    //long endRead = System.currentTimeMillis();
                    //log.debug("Read " + read + " bytes took " + (endRead - startRead) + "ms");
                    //long startWrite = System.currentTimeMillis();
                    fileOut.write(buffer, 0, read);
                    //long endWrite = System.currentTimeMillis();
                    //log.debug("Wrote " + read + " bytes in " + (endWrite - startWrite) + "ms");
                }
            } while (read>=0);
        }
        catch (Exception e) {
            log.warn("RhnJaxbTransport readResponse exception", e);
            throw new XmlRpcException(e.getMessage());
        } finally {
            try {
                if (fileOut != null) {
                    fileOut.close();
                    fileOut = null;
                }
                if (in != null) {
                    in.close();
                    in = null;
                }
            }
            catch (Exception e) {
                ; //ignore exceptions from close
            }
        }
        return tempFile;
    }

    protected File cacheResponseToFile(InputStream inStream) throws XmlRpcException {

        //
        // We will read the response and write it to a file.  Prior approach read all data in memory and parsed it.
        // This created a problem when retrieving package details for large package lists of 6000+ packages.
        // We were seeing out of memory exceptions,
        // estimate on "rhel-i386-server-5" are 6200+ packages, metadata size written to disk in xml is 1.1GB
        // 
        File tempFile = null;
        BufferedOutputStream outStream = null;
        try {
            if (dumpMessageToFile) {
                tempFile = new File(getDumpFilePath());
            }
            else {
                tempFile = File.createTempFile("rhn-jaxb-xmlrpc-output", ".tmp");
            }
            outStream = new BufferedOutputStream(new FileOutputStream(tempFile, false));
            final byte[] buffer = new byte[0x100000];

            BufferedInputStream buffInStream = new BufferedInputStream(inStream);

            int read;
            do {
                //long startRead = System.currentTimeMillis();
                read = buffInStream.read(buffer, 0, buffer.length);
                //read = inStream.read(buffer, 0, buffer.length);
                if (read>0) {
                    //long endRead = System.currentTimeMillis();
                    //log.debug("Read " + read + " bytes took " + (endRead - startRead) + "ms");
                    //long startWrite = System.currentTimeMillis();
                    outStream.write(buffer, 0, read);
                    //long endWrite = System.currentTimeMillis();
                    //log.debug("Wrote " + read + " bytes in " + (endWrite - startWrite) + "ms");
                }
            } while (read>=0);
        }
        catch (Exception e) {
            log.warn("RhnJaxbTransport readResponse exception", e);
            throw new XmlRpcException(e.getMessage());
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                    outStream = null;
                }
            }
            catch (Exception e) {
                ; //ignore exceptions from close
            }
        }
        return tempFile;
    }
    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {

        log.debug("readResponse invoked");

        long start = System.currentTimeMillis();
        File tempFile = cacheResponseToFile(pStream);
        long end = System.currentTimeMillis();
        float bandwidth = (float)tempFile.length() / (1024);
        bandwidth = (bandwidth / (end-start))*1000;
        log.info("response cached " + ((float)tempFile.length()/1024) + " KB in " + (end - start) +
                "ms.  Estimated Bandwidth: " + bandwidth + " KB/sec");

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
        }
        catch (JAXBException e) {
            log.error(e);
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        }
        catch (IOException e) {
            log.error(e);
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        }
        finally {
            if (doWeDeleteTempFile(tempFile)) {
                log.info("Deleting temp file: " + tempFile.getAbsolutePath());
                tempFile.delete();
            }
            else {
                log.info("Temporary file of xmlrpc data is available at: " + tempFile.getAbsolutePath());
            }
            try {
                if (dataStream != null) {
                    dataStream.close();
                }
            }
            catch (Exception e) {
                ; //ignore exception from close
            }
        }
    }

    protected boolean doWeDeleteTempFile(File f) {
        // Check basic file access to ensure we could delete the file
        if ( !f.exists() || f.isDirectory() || !f.canWrite()) {
            return false;
        }
        // If we set dumpMessageToFile, then we obviously don't want to delete it
        if (dumpMessageToFile) {
            return false;
        }
        // Check our System Prop to see if maybe we are debugging and want to keep temp files.
        String value = System.getProperty(PROP_NAME_TO_SAVE_TEMP_FILES);
        if (!StringUtils.isBlank(value)) {
            if (Boolean.parseBoolean(value)) {
                return false;
            }
        }
        return true;
    }
}
