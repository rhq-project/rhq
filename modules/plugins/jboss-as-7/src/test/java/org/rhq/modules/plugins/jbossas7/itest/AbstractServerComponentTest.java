/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.itest;

import org.testng.Assert;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.modules.plugins.jbossas7.util.ResourceUtility;

/**
 * The base class for the integration tests for the two AS7 server types.
 *
 * @author Ian Springer
 */
public abstract class AbstractServerComponentTest extends AbstractJBossAS7PluginTest {

    protected abstract ResourceType getServerResourceType();

    protected abstract String getServerResourceKey();

    protected Resource getServerResource() {
        InventoryManager inventoryManager = this.pluginContainer.getInventoryManager();
        return ResourceUtility.getChildResource(inventoryManager.getPlatform(), getServerResourceType(),
                getServerResourceKey());
    }

    public void testAutoDiscovery() throws Exception {
        System.out.println("\n****** Running " + getClass().getSimpleName() + ".testAutoDiscovery...");
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Assert.assertNotNull(getServerResource(),
                getServerResourceType() + " Resource with key [" + getServerResourceKey() + "] was not discovered.");
        System.out.println("===== Discovered: " + getServerResource());
        System.out.println("---------- " + getServerResource().getPluginConfiguration().toString(true));
    }

}
