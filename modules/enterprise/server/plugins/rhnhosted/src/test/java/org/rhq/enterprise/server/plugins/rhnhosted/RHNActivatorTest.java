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

public class RHNActivatorTest extends BaseRHNTest {

    private static final String TEST_SERVER_URL = "http://satellite.rhn.redhat.com/rpc/api";

    private RHNActivator rhnObject;

    public void setUp() throws Exception {
        super.setUp();
        rhnObject = new RHNActivator(SYSTEM_ID, ENT_CERT, TEST_SERVER_URL);

        assertNotNull(rhnObject);

    }

    public final void testActivate() throws Exception {
        boolean success = true;
        try {
            rhnObject.processActivation();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertEquals(true, success);
    }

    public final void testDeActivate() throws Exception {
        boolean success = true;
        try {
            rhnObject.processDeActivation();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertEquals(true, success);
    }

    private static String ENT_CERT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<rhn-cert version=\"0.1\">"
        + "  <rhn-cert-field name=\"product\">RHN-SATELLITE-001</rhn-cert-field>"
        + "  <rhn-cert-field name=\"owner\">Test Lab - Base Organization</rhn-cert-field>"
        + "  <rhn-cert-field name=\"issued\">2009-08-17 00:00:00</rhn-cert-field>"
        + "  <rhn-cert-field name=\"expires\">2010-08-17 00:00:00</rhn-cert-field>"
        + "  <rhn-cert-field name=\"slots\">10</rhn-cert-field>"
        + "  <rhn-cert-field name=\"monitoring-slots\">10</rhn-cert-field>"
        + "  <rhn-cert-field name=\"provisioning-slots\">10</rhn-cert-field>"
        + "  <rhn-cert-field name=\"virtualization_host\">10</rhn-cert-field>"
        + "  <rhn-cert-field name=\"virtualization_host_platform\">10</rhn-cert-field>"
        + "  <rhn-cert-field name=\"channel-families\" quantity=\"10\" family=\"rhel-server\"/>"
        + "  <rhn-cert-field name=\"channel-families\" quantity=\"10\" family=\"rhel-server-vt\"/>"
        + "  <rhn-cert-field name=\"channel-families\" quantity=\"10\" family=\"rhn-tools\"/>"
        + "  <rhn-cert-field name=\"satellite-version\">5.3</rhn-cert-field>"
        + "  <rhn-cert-field name=\"generation\">2</rhn-cert-field>" + "  <rhn-cert-signature>"
        + "-----BEGIN PGP SIGNATURE-----" + "-----END PGP SIGNATURE-----" + "</rhn-cert-signature>" + "</rhn-cert>";

}
