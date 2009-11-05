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

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.DataInputStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.KeyManagementException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.URL;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;

public class RHNSSLCertReader
{
    static final public String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";
    static final public String END_CERT = "-----END CERTIFICATE-----\n";

    
    static protected int getStartPosition(String rawText, int offset) {
        return rawText.indexOf(BEGIN_CERT, offset);
    }

    static protected int getEndPosition(String rawText, int offset) {
        int index = rawText.indexOf(END_CERT, offset);
        if (index > 0) {
            index = index + END_CERT.length();
        }
        return index;
    }

    static public List<String> getCertText(String rawText) {
        ArrayList<String> certs = new ArrayList<String>();
        int start = 0, end = 0;
        while(true) {
            start = getStartPosition(rawText, end);
            end = getEndPosition(rawText, start);
            if ((end < 0) || (start < 0)) {
                break;
            }
            certs.add(rawText.substring(start, end));
        }
        return certs;
    }

    static public List<X509Certificate> getSSLCertificates(String rawText) throws CertificateException {
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        List<String> rawCerts = getCertText(rawText);
        return getSSLCertificates(rawCerts);
    }

    static public List<X509Certificate> getSSLCertificates(List<String> rawCerts) throws CertificateException {
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        for (String raw: rawCerts) {
            certs.add(getCert(raw));
        }
        return certs;
    }

    static public X509Certificate getCert(String rawText) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream stream = new ByteArrayInputStream(rawText.getBytes());
        return (X509Certificate)cf.generateCertificate(stream);
    }

    static public void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please re-run and specify an argument for the location of a RHN SSL Cert.");
            System.exit(0);
        }
        String path = args[0];
        System.out.println("path is " + path);
        String rawCert = null;
        try {
            rawCert = FileUtils.readFileToString(new File(path));
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        List<String> certs = RHNSSLCertReader.getCertText(rawCert);
        for (String cert: certs) {
            System.out.println("Parsed SSL Certificate: \n" + cert);
        }

        List<X509Certificate> sslCerts = new ArrayList<X509Certificate>();
        try {
            sslCerts = getSSLCertificates(rawCert);
        }
        catch (CertificateException e) {
            e.printStackTrace();
            System.exit(0);
        }
        for (X509Certificate c: sslCerts) {
            System.out.println("Cert SigAlgName = " + c.getSigAlgName());
            System.out.println("Cert IssuerDN = " + c.getIssuerDN());
            System.out.println("Cert NotAfter = " + c.getNotAfter());
            System.out.println("Cert NotBefore = " + c.getNotBefore());
            System.out.println("Cert PublicKey = " + c.getPublicKey());
            System.out.println("Cert SubjectDN = " + c.getSubjectDN());
        }
    }
}
