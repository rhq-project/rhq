/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.upgrade.rhq3_0_1;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class UpgradeMessyConfigurationFromRHQ3_0_1Test extends UpgradeNestedConfigurationFromRHQ3_0_1Test {

    public UpgradeMessyConfigurationFromRHQ3_0_1Test() {
        super("/mocked-inventories/rhq-3.0.1/mess/inventory-with-snmp.xml",
            "/mocked-inventories/rhq-3.0.1/mess/inventory-without-snmp.xml",
            "/mocked-inventories/rhq-3.0.1/mess/inventory-with-snmp-anyaddr.xml",
            "/mocked-inventories/rhq-3.0.1/mess/inventory-with-duplicate-main.xml",
            "/full-configurations/2.2.x/mess/httpd.conf", "/full-configurations/2.2.x/mess/1.vhost.conf",
            "/full-configurations/2.2.x/mess/2.vhost.conf");
    }

}
