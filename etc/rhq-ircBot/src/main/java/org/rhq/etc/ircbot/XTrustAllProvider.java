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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.etc.ircbot;

import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * @author Jirka Kremser
 *
 */
public final class XTrustAllProvider extends java.security.Provider {

    private static final long serialVersionUID = 1L;

    public XTrustAllProvider() {
        super("XTrustJSSE", 0.1d, "XTrust JSSE Provider (implements trust factory with truststore validation disabled)");

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                put("TrustManagerFactory." + TrustManagerFactoryImpl.getAlgorithm(),
                    TrustManagerFactoryImpl.class.getName());
                return null;
            }
        });
    }

    public static void install() {
        Security.insertProviderAt(new XTrustAllProvider(), 2);
        Security.setProperty("ssl.TrustManagerFactory.algorithm", TrustManagerFactoryImpl.getAlgorithm());
    }

    public final static class TrustManagerFactoryImpl extends TrustManagerFactorySpi {
        public static String getAlgorithm() {
            return "XTrust509";
        }

        protected TrustManager[] engineGetTrustManagers() {
            return new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            } };
        }

        protected void engineInit(ManagerFactoryParameters mgrparams) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException("XTrustJSSE does not use ManagerFactoryParameters");
        }

        protected void engineInit(KeyStore keystore) throws KeyStoreException {
        }
    }
}
