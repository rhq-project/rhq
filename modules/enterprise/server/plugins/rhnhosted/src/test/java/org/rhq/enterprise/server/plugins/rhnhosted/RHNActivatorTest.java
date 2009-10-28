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

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class RHNActivatorTest extends TestCase {

    private static final String TEST_CERT_PATH = "./entitlement-cert.xml";
    private static final String TEST_SERVER_URL = "http://satellite.rhn.redhat.com/rpc/api";
    private static final String TEST_SYSTEM_ID = RHNConstants.DEFAULT_SYSTEM_ID;

    private RHNActivator rhnObject;

    public void setUp() throws Exception {
        rhnObject = new RHNActivator(TEST_SYSTEM_ID, TEST_CERT_PATH, TEST_SERVER_URL);
        assertNotNull(rhnObject);
    }


    public final void testActivate() throws Exception {
        String systemid = readSystemId();
        String cert = readCertificate();
        System.out.println("systemid and cert are:" + systemid + cert);
        if (StringUtils.isBlank(systemid) || StringUtils.isBlank(cert)) {
            System.out.println("Skipping test since systemid or cert is not readable");
            return;
        }
        boolean success = true;
        try {
            rhnObject.processActivation();
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertEquals(true, success);
    }

    public final void testDeActivate() throws Exception {
        String systemid = readSystemId();
        String cert = readCertificate();
        if (StringUtils.isBlank(systemid) || StringUtils.isBlank(cert)) {
            System.out.println("Skipping test since systemid or cert is not readable");
            return;
        }
        boolean success = true;
        try {
            rhnObject.processDeActivation();
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertEquals(true, success);
    }

    protected String readSystemId() throws Exception {
        try {
            return FileUtils.readFileToString(new File(TEST_SYSTEM_ID));
        } catch (IOException e) {
            return "";
        }
    }

    protected String readCertificate() throws Exception {
        try {
            return FileUtils.readFileToString(new File(TEST_CERT_PATH));
        } catch (IOException e) {
            return "";
        }

    }

}
