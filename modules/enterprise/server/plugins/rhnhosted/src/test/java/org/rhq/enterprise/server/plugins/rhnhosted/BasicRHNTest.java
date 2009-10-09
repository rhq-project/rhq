package org.rhq.enterprise.server.plugins.rhnhosted;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.io.FileUtils;
import redstone.xmlrpc.XmlRpcClient;

/**
 * Unit test for checking xmlrpc communication with RHN hosted
 */
public class BasicRHNTest extends TestCase
{
    public BasicRHNTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BasicRHNTest.class );
    }

    /*
     * Returns a String representation of a 
     * known systemId file registered to RHN Hosted.
     * */
    protected String getSystemId() throws Exception {
        String systemIdPath = "/etc/sysconfig/rhn/systemid";
        File systemId = new File(systemIdPath);
        if (!systemId.exists()) {
            System.out.println("Unable to read systemid file at: " + systemIdPath);
            assertTrue(false);
        }
        return FileUtils.readFileToString(systemId);
    }

    /**
     * Tests authentication.check
     * */
    public void testAuthenticationCheck() throws Exception
    {
        boolean success = true;
        String systemId = getSystemId();

        try {
            XmlRpcClient client = new XmlRpcClient( "http://xmlrpc.rhn.redhat.com/SAT", true );
            Integer authYN = (Integer) client.invoke( "authentication.check", new Object[] {systemId} );
            assertTrue(authYN.intValue() == 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }


    /**
     * Tests up2date.login
     */
    public void testUp2dateLogin() throws Exception
    {
        boolean success = true;
        String systemId = getSystemId();

        try {
            XmlRpcClient client = new XmlRpcClient( "http://xmlrpc.rhn.redhat.com/XMLRPC", true );
            Object token = client.invoke( "up2date.login", new Object[] { systemId } );
            assertTrue(token != null);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }
}
