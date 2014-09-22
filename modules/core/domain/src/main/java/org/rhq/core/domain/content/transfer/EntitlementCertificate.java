/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.content.transfer;

public class EntitlementCertificate {

    private String name;
    private String certificate;
    private String key;

    /**
     *
     */
    public EntitlementCertificate() {

    }

    /**
    *
    * @param certificate
    * @param key
    */
    public EntitlementCertificate(String name, String certificate, String key) {
        this.name = name;
        this.certificate = certificate;
        this.key = key;
    }

    /**
     * @return the certificate
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * @param certificate the certificate to set
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the private key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the private key to set
     */
    public void setKey(String key) {
        this.key = key;
    }
}