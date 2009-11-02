/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.plugins.rhnhosted;

import java.net.URL;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLHandshakeException;

import org.testng.annotations.Test;


public class RHNSSLSocketFactoryTest
{
    String goodUrl = "https://satellite.rhn.redhat.com/rpc/api";
    String badUrl = "https://bankofamerica.com";
    String certPath = RHNConstants.DEFAULT_SSL_CERT_PATH;

    @Test
    public void testBasicConnectionNoCert()
    {
        try
        {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(goodUrl).openConnection();
            conn.connect();
            // On a clean system, this will not execute, a SSLHandshakeException will be thrown
            // if the below line does execute, then it means that the keystore already has the
            // certs loaded.
            System.out.println("Looks like ssl cert is already loaded into keystore.");
        }
        catch (SSLHandshakeException e)
        {
            System.out.println("Caught SSLHandshake exception from connecting with no cert info as expected.");
        }
        catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }


    @Test
    public void testGetSSLSocketFactoryFromFile()
    {
        try
        {
            SSLSocketFactory sslf = RHNSSLSocketFactory.getSSLSocketFactory(certPath);
            HttpsURLConnection conn = (HttpsURLConnection) new URL(goodUrl).openConnection();
            conn.setSSLSocketFactory(sslf);
            conn.connect();
            System.out.println("Successful Connection");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testGetSSLSocketFactoryFromFileBadURL()
    {
        try
        {
            SSLSocketFactory sslf = RHNSSLSocketFactory.getSSLSocketFactory(certPath);
            HttpsURLConnection conn = (HttpsURLConnection) new URL(badUrl).openConnection();
            conn.setSSLSocketFactory(sslf);
            conn.connect();
            assert false;
        }
        catch (SSLHandshakeException e)
        {
            System.out.println("Caught cert validation exception as we wanted.");
        }
        catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }
}
