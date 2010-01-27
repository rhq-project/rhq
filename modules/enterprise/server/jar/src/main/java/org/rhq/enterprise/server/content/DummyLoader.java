/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.content;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.rhq.core.domain.content.transfer.EntitlementCertificate;

/**
 * @author Jason Dobies
 */
public class DummyLoader {

    static final String dir = "/etc/pki/rhq";

    EntitlementCertificate load(int resourceId) throws IOException {
        String name = String.valueOf(resourceId);
        EntitlementCertificate x509 = load("client");
        x509.setName(name);
        return x509;
    }

    EntitlementCertificate load(String name) throws IOException {
        String key = read(name + ".key");
        String pem = read(name + ".pem");
        return new EntitlementCertificate(name, key, pem);
    }

    private String read(String fn) throws IOException {
        String path = dir + File.separator + fn;
        File f = new File(path);
        byte[] buffer = new byte[(int) f.length()];
        BufferedInputStream istr = new BufferedInputStream(new FileInputStream(f));
        istr.read(buffer);
        String result = new String(buffer);
        istr.close();
        return result;
    }

}
