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

package org.rhq.plugins.mysql;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * Tests MySql Server.
 */
@Test
public class PluginTest extends ComponentTest {

    String host = System.getProperty("host", "localhost");
    String principal = System.getProperty("principal", "mysql");
    String credentials = System.getProperty("credentials", "");
    String rtn = "MySql Server";

    {
        setProcessScan(false);
    }

    @Override
    protected void setConfiguration(Configuration c, ResourceType resourceType) {
        if (resourceType.getName().equals(rtn)) {
            c.getSimple("host").setStringValue(host);
            c.getSimple("principal").setStringValue(principal);
            c.getSimple("credentials").setStringValue(credentials);
        }
        if (resourceType.getName().equals("Database")) {
            c.getSimple("tableDiscovery").setBooleanValue(false);
        }
    }

    public void test() throws Exception {
        manuallyAdd("MySql Server");
        ResourceComponent resourceComponent = getComponent("MySql Server");
//        assertUp(resource); // TODO this requires a running mysql server
        assert resourceComponent != null;
    }

}
