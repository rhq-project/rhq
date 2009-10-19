package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;

import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnComm;

public class RhnCommTest extends TestCase {
    
    public String systemIdPath = "./src/test/resources/systemid";

    public RhnCommTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(RhnCommTest.class);
    }

    protected String getSystemId() throws Exception {
        if (new File(systemIdPath).exists() == false) {
            return "";
        }
        return FileUtils.readFileToString(new File(systemIdPath));
    }

    protected RhnComm getRhnComm() {
        RhnComm comm = new RhnComm();
        comm.setServerURL("http://satellite.rhn.redhat.com");
        return comm;
    }

    public void testGetRPM() throws Exception {
        boolean success = false;
        try {
        System.err.println("testGetRPM");
        RhnComm comm = getRhnComm();
        String channelName = "rhel-x86_64-server-5";
        String rpmName = "openhpi-2.4.1-6.el5.1.x86_64.rpm";
        String saveFilePath = "./target/" + rpmName;
        assertTrue(comm.getRPM(getSystemId(), channelName, rpmName, saveFilePath));
        File t = new File(saveFilePath);
        assertTrue(t.exists());
        success = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }
}
