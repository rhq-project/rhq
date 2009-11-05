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

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.FileUtils;


public class RHNSSLSocketFactory
{
    /**
     * This method will read in 'certPath' and uses these certs to restrict validation.
     *
     * @param certPath path to SSL Cert file
     * @return a SSLSocketFactory instance which will restrict validation to SSL certs in 'certPath'
     * @throws IOException
     * @throws GeneralSecurityException
     */
    static public SSLSocketFactory getSSLSocketFactory(String certPath)
            throws IOException, GeneralSecurityException
    {
        RHNSSLCertReader certReader = new RHNSSLCertReader();
        String rawCert = FileUtils.readFileToString(new File(certPath));
        List<X509Certificate> sslCerts = certReader.getSSLCertificates(rawCert);
        return getSSLSocketFactory(sslCerts);
    }

    /**
     *
     * @param sslCerts these certs will be used to validate the ssl connection
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    static public SSLSocketFactory getSSLSocketFactory(List<X509Certificate> sslCerts)
            throws IOException, GeneralSecurityException
    {
        SSLContext sc = null;
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);  //Important, this intializes the keystore
        int counter = 0;
        for (X509Certificate cert: sslCerts) {
            ks.setCertificateEntry("rhn-key-"+counter, cert);
            counter++;
        }
        sc = SSLContext.getInstance("SSL");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        sc.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
        return sc.getSocketFactory();
    }
}
