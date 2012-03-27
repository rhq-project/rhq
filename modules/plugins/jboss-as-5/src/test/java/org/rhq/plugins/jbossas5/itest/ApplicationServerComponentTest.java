/*
 * Jopr Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.itest;

import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.ResourceUtility;
import org.rhq.core.domain.util.TypeAndKeyResourceFilter;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * An integration test for the AS5 server type.
 *
 * @author Ian Springer
 */
@Test(singleThreaded = true)
public class ApplicationServerComponentTest extends AbstractJBossAS5PluginTest {

    protected static final ResourceType RESOURCE_TYPE = new ResourceType("JBossAS Server", PLUGIN_NAME,
            ResourceCategory.SERVER, null);
    // The key of an AS5 Server Resource is its configuration directory.
    protected static final String RESOURCE_KEY = FileUtils.getCanonicalPath(System.getProperty("jboss5.home")
            + "/server/default");

    @Test
    @RunDiscovery
    public void testAutoDiscovery() throws Exception {
        System.out.println("\n****** Running " + getClass().getSimpleName() + ".testAutoDiscovery...");
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Assert.assertNotNull(platform);
        Assert.assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        Assert.assertNotNull(getServerResource(),
                RESOURCE_TYPE + " Resource with key [" + RESOURCE_KEY + "] was not discovered.");
        System.out.println("===== Discovered: " + getServerResource());
        System.out.println("---------- " + getServerResource().getPluginConfiguration().toString(true));
    }

    private Resource getServerResource() {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Set<Resource> childResources = ResourceUtility.getChildResources(platform,
                new TypeAndKeyResourceFilter(RESOURCE_TYPE, RESOURCE_KEY));
        if (childResources.size() > 1) {
            throw new IllegalStateException(platform + " has more than one child Resource with same type ("
                    + RESOURCE_TYPE + ") and key (" + RESOURCE_KEY + ").");
        }
        return (childResources.isEmpty()) ? null : childResources.iterator().next();
    }

}
