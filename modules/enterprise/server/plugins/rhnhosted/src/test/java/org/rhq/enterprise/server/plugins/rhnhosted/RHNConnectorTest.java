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

public class RHNConnectorTest extends TestCase {

    private static final String TEST_CERT_PATH = "./entitlement-cert.xml";
    private static final String TEST_SERVER_URL = "http://satellite.rhn.redhat.com/rpc/api";

    private RHNConnector rhnObject;

    public void setUp() throws Exception {
        rhnObject = new RHNConnector(RHNConstants.DEFAULT_SYSTEM_ID, TEST_CERT_PATH, TEST_SERVER_URL);
        assertNotNull(rhnObject);
    }

    public final void testActivate() throws Exception {
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

}
