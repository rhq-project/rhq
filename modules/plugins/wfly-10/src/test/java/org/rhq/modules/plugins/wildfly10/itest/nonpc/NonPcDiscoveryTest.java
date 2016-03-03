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
package org.rhq.modules.plugins.wildfly10.itest.nonpc;

import org.testng.annotations.Test;

import org.rhq.modules.plugins.wildfly10.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Currently the non-pc tests depend on a discovery but don't execute with any test classes
 * that perform discovery on their own. So, this does nothing but a discovery to get things going. If
 * the i-tests executed all together this would not have been necessary.
 *
 * @author Jay Shaughnessy
 */
@Test(groups = {"integration", "pc", "discovery"}, singleThreaded = true)
public class NonPcDiscoveryTest extends AbstractJBossAS7PluginTest {

    @Test(priority = -10000, groups = { "discovery" })
    @RunDiscovery(discoverServers = true, discoverServices = false)
    public void initialDiscoveryTest() throws Exception {

        validateDiscovery();
    }

}
