/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
import static org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
import static org.rhq.modules.plugins.jbossas7.ASConnection.HTTPS_SCHEME;
import static org.rhq.modules.plugins.jbossas7.ASConnection.HTTP_SCHEME;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;

/**
 * Helper class which creates Apache HTTP Client 4.x {@link SchemeRegistry} instances.
 *
 * @author Thomas Segismont
 */
class SchemeRegistryBuilder {
    private static final TrustSelfSignedStrategy TRUST_SELF_SIGNED_STRATEGY = new TrustSelfSignedStrategy();
    private static final TrustAnyStrategy TRUST_ANY_STRATEGY = new TrustAnyStrategy();

    private final ASConnectionParams asConnectionParams;

    public SchemeRegistryBuilder(ASConnectionParams asConnectionParams) {
        this.asConnectionParams = asConnectionParams;
    }

    public SchemeRegistry buildSchemeRegistry() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        if (asConnectionParams.isSecure()) {
            SSLSocketFactory sslSocketFactory;
            try {
                KeyStore truststore = null;
                if (asConnectionParams.getTruststore() != null) {
                    truststore = loadKeystore(asConnectionParams.getTruststoreType(),
                        asConnectionParams.getTruststore(), asConnectionParams.getTruststorePassword());
                }
                KeyStore keystore = null;
                String keyPassword = null;
                if (asConnectionParams.isClientcertAuthentication()) {
                    if (asConnectionParams.getKeystore() == null) {
                        keystore = loadKeystore( //
                            System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType()), //
                            System.getProperty("javax.net.ssl.keyStore"), //
                            System.getProperty("javax.net.ssl.keyStorePassword") //
                        );
                    } else {
                        keystore = loadKeystore( //
                            asConnectionParams.getKeystoreType(), //
                            asConnectionParams.getKeystore(), //
                            asConnectionParams.getKeystorePassword() //
                        );
                        keyPassword = asConnectionParams.getKeyPassword();
                    }
                }
                sslSocketFactory = new SSLSocketFactory(null, keystore, keyPassword, truststore, null,
                    getTrustStrategy(), getHostnameVerifier());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            schemeRegistry.register(new Scheme(HTTPS_SCHEME, asConnectionParams.getPort(), sslSocketFactory));
        } else {
            schemeRegistry.register(new Scheme(HTTP_SCHEME, asConnectionParams.getPort(), PlainSocketFactory
                .getSocketFactory()));
        }
        return schemeRegistry;
    }

    private KeyStore loadKeystore(String keystoreType, String keystore, String keystorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(keystoreType);
        char[] password = keystorePassword.toCharArray();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(keystore);
            ks.load(fileInputStream, password);
            return ks;
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }

    private TrustStrategy getTrustStrategy() {
        switch (asConnectionParams.getTrustStrategy()) {
        case TRUST_SELFSIGNED:
            return TRUST_SELF_SIGNED_STRATEGY;
        case TRUST_ANY:
            return TRUST_ANY_STRATEGY;
        case STANDARD:
            return null;
        }
        throw new UnsupportedOperationException(asConnectionParams.getTrustStrategy().name);
    }

    private X509HostnameVerifier getHostnameVerifier() {
        switch (asConnectionParams.getHostnameVerification()) {
        case STRICT:
            return STRICT_HOSTNAME_VERIFIER;
        case BROWSER_COMPATIBLE:
            return BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
        case SKIP:
            return ALLOW_ALL_HOSTNAME_VERIFIER;
        }
        throw new UnsupportedOperationException(asConnectionParams.getHostnameVerification().name);
    }

    private static class TrustAnyStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            return true;
        }
    }
}
