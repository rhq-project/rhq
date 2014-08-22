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

package org.rhq.modules.plugins.jbossas7.util;

import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * @author Thomas Segismont
 */
public class SecurityUtil {

    private SecurityUtil() {
        // Utility class
    }

    /**
     * Creates a {@link java.security.KeyStore} instance and loads the content of the specified file.
     *
     * @param keystoreType a keystore type name supported by the JVM (for example "jks" or "pkcs12")
     * @param keystore path to the keystore file to load
     * @param keystorePassword the password protecting the keystore file, or null
     * @return an instance of {@link java.security.KeyStore} which content is loaded from the keystore file
     * @throws Exception if the keystore type is not supported or if the file cannot be read
     */
    public static KeyStore loadKeystore(String keystoreType, String keystore, String keystorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance(keystoreType);
        char[] password = keystorePassword == null ? null : keystorePassword.toCharArray();
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
}
