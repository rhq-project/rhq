package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;

public class CustomReqPropTransport extends RhnSSLTransport {

    private final Log log = LogFactory.getLog(CustomReqPropTransport.class);
    protected Map<String, String> reqProps;
    protected boolean dumpMessageToFile; // used during debug to save a copy of the data received
    protected String dumpFilePath;
    protected String PROP_NAME_TO_SAVE_TEMP_FILES = "RhnXMLRPCSaveTempFiles";

    public CustomReqPropTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setDumpMessageToFile(boolean dump) {
        dumpMessageToFile = dump;
    }

    public boolean getDumpMessageToFile() {
        return dumpMessageToFile;
    }

    public void setDumpFilePath(String dumpPath) {
        dumpFilePath = dumpPath;
    }

    public String getDumpFilePath() {
        return dumpFilePath;
    }

    public void setRequestProperties(Map props) {
        reqProps = props;
    }

    public Map<String, String> getRequestProperties() {
        return reqProps;
    }

    public void setRequestProperty(String name, String value) {
        if (reqProps == null) {
            reqProps = new HashMap<String, String>();
        }
        reqProps.put(name, value);
    }

    public String getRequestProperty(String name, String value) {
        return (String) reqProps.get(name);
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
            if (!StringUtils.isBlank(dumpFilePath)) {
                // dumpFilePath was specified so use that, even though we may be over-writing it
                tempFile = new File(dumpFilePath);
            }
            tempFile = File.createTempFile("RHQ-rhn-xmlrpc-output", ".tmp");
            outStream = new BufferedOutputStream(new FileOutputStream(tempFile, false));
            final byte[] buffer = new byte[0x100000];

            BufferedInputStream buffInStream = new BufferedInputStream(inStream);

            int read;
            do {
                //long startRead = System.currentTimeMillis();
                read = buffInStream.read(buffer, 0, buffer.length);
                //read = inStream.read(buffer, 0, buffer.length);
                if (read > 0) {
                    //long endRead = System.currentTimeMillis();
                    //log.debug("Read " + read + " bytes took " + (endRead - startRead) + "ms");
                    //long startWrite = System.currentTimeMillis();
                    outStream.write(buffer, 0, read);
                    //long endWrite = System.currentTimeMillis();
                    //log.debug("Wrote " + read + " bytes in " + (endWrite - startWrite) + "ms");
                }
            } while (read >= 0);
        } catch (Exception e) {
            log.warn("RhnJaxbTransport readResponse exception", e);
            throw new XmlRpcException(e.getMessage());
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                    outStream = null;
                }
            } catch (Exception e) {
                ; //ignore exceptions from close
            }
        }
        return tempFile;
    }

    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {

        // During debug modes we may want to cache all the input to a file
        if (dumpMessageToFile) {
            InputStream dataStream = null;
            try {
                File tempFile = cacheResponseToFile(pStream);
                dataStream = new FileInputStream(tempFile);
                return super.readResponse(pConfig, dataStream);
            } catch (FileNotFoundException e) {

            } finally {
                if (dataStream != null) {
                    try {
                        dataStream.close();
                    } catch (Exception e) {
                        ;//ignore
                    }
                }
            }
        }
        // we aren't writing the data to a file
        return super.readResponse(pConfig, pStream);
    }

    protected boolean doWeDeleteTempFile(File f) {
        // Check basic file access to ensure we could delete the file
        if (!f.exists() || f.isDirectory() || !f.canWrite()) {
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
