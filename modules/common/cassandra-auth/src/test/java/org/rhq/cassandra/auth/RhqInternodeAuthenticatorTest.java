package org.rhq.cassandra.auth;

import static org.testng.Assert.assertEquals;

import java.net.InetAddress;

import org.testng.annotations.Test;

@Test
public class RhqInternodeAuthenticatorTest {

    public void testIt() throws Exception {
        RhqInternodeAuthenticator auth = new RhqInternodeAuthenticator();
        auth.validateConfiguration();
        auth.reloadConfiguration();
        int port = 5555;
        InetAddress address = InetAddress.getByName("127.0.0.1");
        for (int i = 0; i < 50; i++) {
            assertEquals(auth.authenticate(address, port), false);
        }
        address = InetAddress.getByName("127.0.0.20");
        assertEquals(auth.authenticate(address, port), true);
    }

}
