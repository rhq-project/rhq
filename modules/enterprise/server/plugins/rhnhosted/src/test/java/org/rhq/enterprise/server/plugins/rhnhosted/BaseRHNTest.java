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

import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.ApacheXmlRpcExecutor;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.MockRhnHttpURLConnection;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.MockRhnXmlRpcExecutor;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnHttpURLConnectionFactory;

public class BaseRHNTest extends TestCase {

    protected static final String TEST_SERVER_URL = "http://satellite.rhn.redhat.com/rpc/api";

    // protected static final String TEST_SYSTEM_ID = RHNConstants.DEFAULT_SYSTEM_ID;

    public void setUp() throws Exception {
        super.setUp();
        /* OVERRIDE THE XMLRPC CLIENT WITH A MOCK OBJECT */
        System.setProperty(ApacheXmlRpcExecutor.class.getName(), MockRhnXmlRpcExecutor.class.getName());
        System.setProperty(RhnHttpURLConnectionFactory.RHN_MOCK_HTTP_URL_CONNECTION, MockRhnHttpURLConnection.class
            .getName());

    }

    public static String ENT_CERT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<rhn-cert version=\"0.1\">"
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

    public static String SYSTEM_ID = "<?xml version=\"1.0\"?>" + "<params>\n" + "<param>\n" + "<value><struct>\n"
        + "<member>\n" + "<name>username</name>\n" + "<value><string>unknown</string></value>\n" + "</member>\n"
        + "<member>\n" + "<name>operating_system</name>\n" + "<value><string>redhat-release</string></value>\n"
        + "</member>\n" + "<member>\n" + "<name>description</name>\n"
        + "<value><string>Initial Registration Parameters:\n" + "OS: redhat-release\n" + "Release: 5Server\n"
        + "CPU Arch: athlon-redhat-linux</string></value>\n" + "</member>\n" + "<member>\n" + "<name>checksum</name>\n"
        + "<value><string>b57e3b8f0ffe10807b041905197068a7</string></value>\n" + "</member>\n" + "<member>\n"
        + "<name>profile_name</name>\n" + "<value><string>unknown</string></value>\n" + "</member>\n" + "<member>\n"
        + "<name>system_id</name>\n" + "<value><string>ID-1234</string></value>\n" + "</member>\n" + "<member>\n"
        + "<name>architecture</name>\n" + "<value><string>athlon-redhat-linux</string></value>\n" + "</member>\n"
        + "<member>\n" + "<name>os_release</name>\n" + "<value><string>5Server</string></value>\n" + "</member>\n"
        + "<member>\n" + "<name>fields</name>\n" + "<value><array><data>\n"
        + "<value><string>system_id</string></value>\n" + "<value><string>os_release</string></value>\n"
        + "<value><string>operating_system</string></value>\n" + "<value><string>architecture</string></value>\n"
        + "<value><string>username</string></value>\n" + "<value><string>type</string></value>\n"
        + "</data></array></value>\n" + "</member>\n" + "<member>\n" + "<name>type</name>\n"
        + "<value><string>REAL</string></value>\n" + "</member>\n" + "</struct></value>\n" + "</param>\n"
        + "</params>\n";

    public static String SYSTEM_ID_BAD = "<?xml version=\"1.0\"?>" + "<params>\n" + "<param>\n" + "<value><struct>\n"
        + "<member>\n" + "<name>username</name>\n" + "<value><string>unknown</string></value>\n" + "</member>\n"
        + "<member>\n" + "<name>operating_system</name>\n" + "<value><string>redhat-release</string></value>\n"
        + "</member>\n" + "<member>\n" + "<name>description</name>\n"
        + "<value><string>Initial Registration Parameters:\n" + "OS: redhat-release\n" + "Release: 5Server\n"
        + "CPU Arch: athlon-redhat-linux</string></value>\n" + "</member>\n" + "<member>\n" + "<name>checksum</name>\n"
        + "<value><string>b57e3b8f0ffe10807b041905197068a7</string></value>\n" + "</member>\n" + "<member>\n"
        + "<name>profile_name</name>\n" + "<value><string>unknown</string></value>\n" + "</member>\n" + "<member>\n"
        + "<name>system_id</name>\n" + "<value><string>ID-0000000000</string></value>\n" + "</member>\n" + "<member>\n"
        + "<name>architecture</name>\n" + "<value><string>athlon-redhat-linux</string></value>\n" + "</member>\n"
        + "<member>\n" + "<name>os_release</name>\n" + "<value><string>5Server</string></value>\n" + "</member>\n"
        + "<member>\n" + "<name>fields</name>\n" + "<value><array><data>\n"
        + "<value><string>system_id</string></value>\n" + "<value><string>os_release</string></value>\n"
        + "<value><string>operating_system</string></value>\n" + "<value><string>architecture</string></value>\n"
        + "<value><string>username</string></value>\n" + "<value><string>type</string></value>\n"
        + "</data></array></value>\n" + "</member>\n" + "<member>\n" + "<name>type</name>\n"
        + "<value><string>REAL</string></value>\n" + "</member>\n" + "</struct></value>\n" + "</param>\n"
        + "</params>\n";

}
