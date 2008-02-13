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
package org.rhq.enterprise.server.util.security;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * The bogus trust manager allows for non-validated remote SSL entities. In the case of people using self-signed
 * certificates, this allows a connection.
 */
public class BogusTrustManager implements X509TrustManager {
    /**
     * @see X509TrustManager#checkClientTrusted(X509Certificate[], String)
     */
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    /**
     * @see X509TrustManager#checkServerTrusted(X509Certificate[], String)
     */
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    /**
     * @see X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}