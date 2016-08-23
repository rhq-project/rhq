/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.agent;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * If you need to make a connection to an external HTTP server using the https protocol,
 * use this class to build the connection. You can specify which trusted certificates can be used
 * to verify and authenticate the remote server via truststore file parameters passed into the constructor.
 * In other words, you don't have to rely on the global system properties javax.net.ssl.keyStore and
 * javax.net.ssl.trustStore to make your connection.
 * 
 * This is useful if some other object running in your JVM requires
 * javax.net.ssl.keyStore/javax.net.ssl.trustStore set to something other than what you want to use.
 * Since those system properties can only be set once during the lifetime of the JVM, it is not possible
 * to change them at runtime and expect the change to take effect. Besides, even if you could change them,
 * this could then effect the other objects that do need those properties set to particular values.
 *
 * This object has the ability to make an encrypted call without requiring a truststore to exist (that is,
 * you can encrypt the call but you will not have the server endpoint verified and authenticated). This
 * can be useful if you aren't concerned with authenticating the endpoint but do want to encrypt the
 * traffic.
 *
 * @author John Mazzitelli
 */
public class SecureConnector {
    private final String secureSocketProtocol;
    private final AgentTrustStore agentTrustStore;
    private final AgentKeystore agentKeystore;

    /**
     * The {@link #openSecureConnection(URL) secure connections} built by this object will
     * not authenticate the server endpoint, but they will use the given secure socket protocol
     * to encrypt the connection traffic.
     * 
     * @param secureSocketProtocol the secure socket protocol to use (e.g. "TLS")
     */
    public SecureConnector(String secureSocketProtocol) {
        this(secureSocketProtocol, null, null);
    }

    /**
     * The {@link #openSecureConnection(URL) secure connections} built by this object will
     * authenticate the server endpoint using the given truststore and keystore.
     * The connection will use the given secure socket protocol to encrypt the connection traffic.
     *
     * @param secureSocketProtocol the secure socket protocol to use (e.g. "TLS")
     * @param agentTrustStore the truststore containing authorized certificates
     * @param agentKeystore the keystore containing authorized keys
     */
    public SecureConnector(String secureSocketProtocol, AgentTrustStore agentTrustStore, AgentKeystore agentKeystore) {

        if (secureSocketProtocol == null) {
            throw new IllegalArgumentException("secure socket protocol cannot be null");
        }
        this.secureSocketProtocol = secureSocketProtocol;

        this.agentTrustStore = agentTrustStore;
        if (agentTrustStore != null) {
            if (agentTrustStore.getPassword() == null) {
                throw new IllegalArgumentException("truststore password cannot be null");
            }

            if (agentTrustStore.getType() == null) {
                agentTrustStore.setType(KeyStore.getDefaultType());
            }

            if (agentTrustStore.getAlgorithm() == null) {
                agentTrustStore.setAlgorithm(TrustManagerFactory.getDefaultAlgorithm());
            }
        }

        this.agentKeystore = agentKeystore;
        if (agentKeystore != null) {
            if (agentKeystore.getPassword() == null) {
                throw new IllegalArgumentException("keystore password cannot be null");
            }
            if (agentKeystore.getKeyPassword() == null) {
                throw new IllegalArgumentException("keystore key-password cannot be null");
            }

            if (agentKeystore.getType() == null) {
                agentKeystore.setType(KeyStore.getDefaultType());
            }

            if (agentKeystore.getAlgorithm() == null) {
                agentKeystore.setAlgorithm(TrustManagerFactory.getDefaultAlgorithm());
            }
        }

        return;
    }

    public HttpsURLConnection openSecureConnection(URL url) throws Exception {
        // we assume the URL is https - if it is not, its an error so just let the cast throw exception
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        TrustManager[] trustManagers;
        SSLContext sslContext = SSLContext.getInstance(getSecureSocketProtocol());
        KeyManager[] keyManagers = null;
        if (getAgentTrustStore() == null) {
            // we are configured to not care about authenticating the server, just encrypt but don't worry about certificates
            trustManagers = new TrustManager[] { NO_OP_TRUST_MANAGER };
            connection.setHostnameVerifier(NO_OP_HOSTNAME_VERIFIER);
        } else {
            // We need to configure our SSL connection with the agent's truststore so we can authenticate the server.
            // First, create a KeyStore, but load it with our truststore entries.
            KeyStore keyStore = KeyStore.getInstance(getAgentTrustStore().getType());
            keyStore.load(new FileInputStream(getAgentTrustStore().getFile()), getAgentTrustStore().getPassword().toCharArray());
            // now create a truststore manager instance and initialize it with our KeyStore we created with all our truststore entries
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(getAgentTrustStore().getAlgorithm());
            tmf.init(keyStore);
            trustManagers = tmf.getTrustManagers();
        }

        if (getAgentKeystore() != null) {
            KeyStore keyStore = KeyStore.getInstance(getAgentKeystore().getType());
            keyStore.load(new FileInputStream(getAgentKeystore().getFile()), getAgentKeystore().getPassword().toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(getAgentKeystore().getAlgorithm());
            keyManagerFactory.init(keyStore, getAgentKeystore().getKeyPassword().toCharArray());
            keyManagers = keyManagerFactory.getKeyManagers();
        }

        sslContext.init(keyManagers, trustManagers, null);
        connection.setSSLSocketFactory(sslContext.getSocketFactory());

        return connection;
    }

    public String getSecureSocketProtocol() {
        return this.secureSocketProtocol;
    }

    public AgentTrustStore getAgentTrustStore() {
        return agentTrustStore;
    }

    public AgentKeystore getAgentKeystore() {
        return agentKeystore;
    }

    private static TrustManager NO_OP_TRUST_MANAGER = new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
            return;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
            return;
        }
    };

    private static HostnameVerifier NO_OP_HOSTNAME_VERIFIER = new HostnameVerifier() {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    };

    public static void main(String[] args) throws Exception {
        HttpsURLConnection conn = new SecureConnector("TLS").openSecureConnection(new URL(args[0]));
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.InputStream input = new java.io.BufferedInputStream(conn.getInputStream(), 32768);
        byte[] buffer = new byte[32768];
        for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
        System.out.println(out.toString());
    }

}