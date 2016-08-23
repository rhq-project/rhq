/*
 * RHQ Management Platform
 * Copyright (C) 2005-2016 Red Hat, Inc.
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

import java.io.File;

public class AgentTrustStore {

    private File file;
    private String type;
    private String algorithm;
    private String password;

    /**
     *
     *
     * @param file the truststore file containing authorized certificates
     * @param type the type of the truststore file (e.g. "JKS"); if <code>null</code>, then the JVM's
     *                       default type is used (see <code>java.security.KeyStore.getDefaultType()</code>)
     * @param algorithm the standard name of the trust management algorithm (e.g. "SunX509")
     *                  if <code>null</code>, then the JVM's default algorithm is used (see
     *                            <code>javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()</code>)
     * @param password the password to the truststore file (if a file is given, this must not be <code>null</code>)
     */
    public AgentTrustStore(File file, String type, String algorithm, String password) {
        this.setFile(file);
        this.setPassword(password);
        this.setType(type);
        this.setAlgorithm(algorithm);
    }

    public File getFile() {
        return file;
    }

    public String getType() {
        return type;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPassword() {
        return password;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
