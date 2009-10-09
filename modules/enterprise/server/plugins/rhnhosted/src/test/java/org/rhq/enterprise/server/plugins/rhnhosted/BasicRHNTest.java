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

    /**
     * Tests get version
     * */
    public void testGetVersion() throws Exception
    {
        boolean success = true;

        try {
            XmlRpcClient client = new XmlRpcClient( "http://xmlrpc.rhn.redhat.com/rpc/api", true );
            String version = (String) client.invoke( "api.getVersion", new Object[] {});
            assertTrue(version.length() > 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }
}
