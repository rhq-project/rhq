package org.rhq.enterprise.client.utility;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.enterprise.client.Controller;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ScriptUtil {

    private ClientMain client;

    public ScriptUtil(ClientMain client) {
        this.client = client;
    }

    public PageList<Resource> findResources(String string) {
        ResourceManagerRemote resourceManager = client.getRemoteClient().getResourceManagerRemote();

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterName('%' + string + '%');
        return resourceManager.findResourcesByCriteria(client.getSubject(), criteria);
    }


    public byte[] getFileBytes(String fileName) {
        File file = new File(fileName);
        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Can not read file larger than " + Integer.MAX_VALUE + " byte: "
                + fileName);
        }

        byte[] bytes;
        InputStream is = null;
        try {
            bytes = new byte[(int) length];
            is = new FileInputStream(file);

            int offset = 0, bytesRead = 0;
            for (offset = 0, bytesRead = 0; offset < bytes.length && bytesRead >= 0; offset += bytesRead) {
                bytesRead = is.read(bytes, offset, bytes.length - offset);
            }

            if (offset < bytes.length) {
                throw new RuntimeException("Could not read entire file " + file.getName() + ", only " + offset + " of "
                    + bytes.length + " bytes read");
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Error reading file: " + ioe.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                throw new RuntimeException("Error closing file: " + ioe.getMessage());
            }
        }
        return bytes;
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
