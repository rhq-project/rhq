package org.rhq.enterprise.client.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.script.ScriptEngine;

public class ScriptUtil {

    private ScriptEngine scriptEngine;

    public ScriptUtil(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void assertEqual(Object lefty, Object righty) {
        assertEqual(lefty, righty, null);
    }

    public void assertEqual(Object lefty, Object righty, String errorMessage) {
        boolean result;
        if (lefty == null) {
            if (righty == null) {
                result = true; // both null
            } else {
                result = false; // only lefty is null
            }
        } else {
            if (righty == null) {
                result = false; // only righty is null
            } else {
                result = lefty.equals(righty) && righty.equals(lefty);
            }
        }
        if (result == false) {
            if (errorMessage == null) {
                throw new AssertionError("values were not equal");
            } else {
                throw new AssertionError(errorMessage);
            }
        }
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

    public void assertExists(String identifier) {
        if (scriptEngine.get(identifier) == null) {
            throw new AssertionError(identifier + " was not defined");
        }
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
