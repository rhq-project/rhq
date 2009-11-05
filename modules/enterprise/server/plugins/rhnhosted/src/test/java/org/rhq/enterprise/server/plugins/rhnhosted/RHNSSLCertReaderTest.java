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

import org.testng.annotations.Test;

import java.util.List;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

public class RHNSSLCertReaderTest
{

    String cert1Data = RHNSSLCertReader.BEGIN_CERT + "blahblahblah12345\nblahblah\nblah\n" +
            RHNSSLCertReader.END_CERT;
    String cert2Data = RHNSSLCertReader.BEGIN_CERT + "blahblahblah98765434\nblahblah2\nblah3\n" +
            RHNSSLCertReader.END_CERT;

    String dummyData = "Dummy Data\n" +
            "Dummy Data 1\n" +
            cert1Data +
            "Dummy Data 2\n" +
            cert2Data;

    String sampleSSLCert = RHNSSLCertReader.BEGIN_CERT +
        "MIIDODCCAvWgAwIBAgIESuiB4TALBgcqhkjOOAQDBQAwfzELMAkGA1UEBhMCTkMxDjAMBgNVBAgT\n" +
        "BVN0YXRlMQ0wCwYDVQQHEwRDaXR5MRkwFwYDVQQKExBPcmdhbml6YXRpb25OYW1lMRkwFwYDVQQL\n" +
        "ExBPcmdhbml6YXRpb25Vbml0MRswGQYDVQQDExJGaXJzdE5hbWUgTGFzdE5hbWUwHhcNMDkxMDI4\n" +
        "MTczOTQ1WhcNMTAwMTI2MTczOTQ1WjB/MQswCQYDVQQGEwJOQzEOMAwGA1UECBMFU3RhdGUxDTAL\n" +
        "BgNVBAcTBENpdHkxGTAXBgNVBAoTEE9yZ2FuaXphdGlvbk5hbWUxGTAXBgNVBAsTEE9yZ2FuaXph\n" +
        "dGlvblVuaXQxGzAZBgNVBAMTEkZpcnN0TmFtZSBMYXN0TmFtZTCCAbcwggEsBgcqhkjOOAQBMIIB\n" +
        "HwKBgQD9f1OBHXUSKVLfSpwu7OTn9hG3UjzvRADDHj+AtlEmaUVdQCJR+1k9jVj6v8X1ujD2y5tV\n" +
        "bNeBO4AdNG/yZmC3a5lQpaSfn+gEexAiwk+7qdf+t8Yb+DtX58aophUPBPuD9tPFHsMCNVQTWhaR\n" +
        "MvZ1864rYdcq7/IiAxmd0UgBxwIVAJdgUI8VIwvMspK5gqLrhAvwWBz1AoGBAPfhoIXWmz3ey7yr\n" +
        "XDa4V7l5lK+7+jrqgvlXTAs9B4JnUVlXjrrUWU/mcQcQgYC0SRZxI+hMKBYTt88JMozIpuE8FnqL\n" +
        "VHyNKOCjrh4rs6Z1kW6jfwv6ITVi8ftiegEkO8yk8b6oUZCJqIPf4VrlnwaSi2ZegHtVJWQBTDv+\n" +
        "z0kqA4GEAAKBgG+MiW74SeEG8J79c6gzjQZ6ViFvA4oEYIMUNReZ6V3EX5Z5s99qJPIdXb/hICov\n" +
        "T05z3Ug89ARkCsTWEEFwLx+VjnP2qejeTR8VUkkYNujL2ZFHJk7TBfTrpuatUbEINI1drdV6Br0/\n" +
        "iI7wyP6VW/B2HhvW1dQy1Bq7+GuCNg9DMAsGByqGSM44BAMFAAMwADAtAhQA1HJVck/E2v6va473\n" +
        "V0UxRsKp3QIVAIqj+Do5FKdXxTpQETJleh6Y/uSi\n" +
        RHNSSLCertReader.END_CERT;

    @Test
    public void testGetCertText() throws Exception {
        List<String> certs = RHNSSLCertReader.getCertText(dummyData);
        assert certs.size() == 2;
        String parsedCert1 = certs.get(0);
        String parsedCert2 = certs.get(1);
        assert cert1Data.compareTo(parsedCert1) == 0;
        assert cert2Data.compareTo(parsedCert2) == 0;
    }

    @Test
    public void testGetSSLCertificates() throws Exception {
        List<X509Certificate> certs = RHNSSLCertReader.getSSLCertificates(sampleSSLCert);
        System.err.println("cert = " + certs.get(0));
        assert certs.size() == 1;
    }
}
