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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.plugins.alertSnmp;

import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.DEFAULT_PORT;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_HOST;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_PORT;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_TRAP_OID;
import static org.rhq.enterprise.server.plugins.alertSnmp.SnmpInfo.PARAM_VARIABLE_BINDING_PREFIX;
import static org.testng.Assert.*;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Thomas Segismont
 */
public class SnmpInfoTest {

    @Test
    public void shouldDetectMissingParams() throws Exception {
        Configuration configuration = new Configuration();

        SnmpInfo snmpInfo = SnmpInfo.load(configuration);
        assertNotNull(snmpInfo.error, "SnmpInfo#load should have detected an error");
        assertEquals(snmpInfo.error, "Missing: OID, host");

        configuration.setSimpleValue(PARAM_HOST, "pipo");
        snmpInfo = SnmpInfo.load(configuration);
        assertNotNull(snmpInfo.error, "SnmpInfo#load should have detected an error");
        assertEquals(snmpInfo.error, "Missing: OID");

        configuration.remove(PARAM_HOST);
        configuration.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, "molo");
        snmpInfo = SnmpInfo.load(configuration);
        assertNotNull(snmpInfo.error, "SnmpInfo#load should have detected an error");
        assertEquals(snmpInfo.error, "Missing: host");
    }

    @Test
    public void shouldAssumeDefaultValues() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setSimpleValue(PARAM_HOST, "pipo");
        configuration.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, "molo");

        SnmpInfo snmpInfo = SnmpInfo.load(configuration);
        assertNull(snmpInfo.error, "SnmpInfo#load should not have detected an error");
        assertEquals(snmpInfo.host, "pipo");
        assertEquals(snmpInfo.oid, "molo");
        assertEquals(snmpInfo.port, SnmpInfo.DEFAULT_PORT);
    }

    @Test
    public void fallBackToGlobalValues() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, "molo");

        Configuration preferences = new Configuration();
        preferences.put(new PropertySimple("defaultTargetHost","hugo"));
        SnmpInfo snmpInfo = SnmpInfo.load(configuration,preferences);
        assertNull(snmpInfo.error, "SnmpInfo#load should not have detected an error");
        assertEquals(snmpInfo.host, "hugo");
        assertEquals(snmpInfo.oid, "molo");
        assertEquals(snmpInfo.port, SnmpInfo.DEFAULT_PORT);


        configuration.setSimpleValue(PARAM_HOST, "pipo");

        snmpInfo = SnmpInfo.load(configuration,preferences);
        assertNull(snmpInfo.error, "SnmpInfo#load should not have detected an error");
        assertEquals(snmpInfo.host, "pipo");
        assertEquals(snmpInfo.oid, "molo");
        assertEquals(snmpInfo.port, SnmpInfo.DEFAULT_PORT);
    }

    @Test
    public void shouldExposeAllParams() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setSimpleValue(PARAM_HOST, "pipo");
        configuration.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, "molo");
        configuration.setSimpleValue(PARAM_PORT, "123");
        configuration.setSimpleValue(PARAM_TRAP_OID, "logo");

        SnmpInfo snmpInfo = SnmpInfo.load(configuration);
        assertNull(snmpInfo.error, "SnmpInfo#load should not have detected an error");
        assertEquals(snmpInfo.host, "pipo");
        assertEquals(snmpInfo.port, "123");
        assertEquals(snmpInfo.oid, "molo");
        assertEquals(snmpInfo.trapOid, "logo");
    }

    @Test
    public void testStringRepresentation() {
        // We are not testing something really tricky here...
        // But the String representation of an SnmpInfo instance is used in the UI as a description in the alert
        // definition notification list. So this test is here to remind us in the future that the format of the
        // String representation is constrained (even not strongly).
        Configuration configuration = new Configuration();
        assertEquals(SnmpInfo.load(configuration).toString(), "UnknownHost:" + DEFAULT_PORT
            + " (UnknownOID) (DefaultTrapOID)");
        configuration.setSimpleValue(PARAM_HOST, "pipo");
        assertEquals(SnmpInfo.load(configuration).toString(), "pipo:" + DEFAULT_PORT + " (UnknownOID) (DefaultTrapOID)");
        configuration.setSimpleValue(PARAM_PORT, "35162");
        assertEquals(SnmpInfo.load(configuration).toString(), "pipo:35162 (UnknownOID) (DefaultTrapOID)");
        configuration.setSimpleValue(PARAM_VARIABLE_BINDING_PREFIX, "molo");
        assertEquals(SnmpInfo.load(configuration).toString(), "pipo:35162 (molo) (DefaultTrapOID)");
        configuration.setSimpleValue(PARAM_TRAP_OID, "logo");
        assertEquals(SnmpInfo.load(configuration).toString(), "pipo:35162 (molo) (logo)");
    }

}
