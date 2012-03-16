/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.arquillian;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.system.JavaSystemInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * 
 *
 * @author Lukas Krejci
 */
@RunDiscovery
public class NativeSystemInfoEnablementTest extends Arquillian {

    @Deployment(name = "with-native")
    @TargetsContainer("sigar-enabled-pc")
    public static RhqAgentPluginArchive getPlugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "test-systeminfo-plugin.jar")
            .addClass(TestDiscoveryComponent.class).addClass(TestResourceComponent.class)
            .setPluginDescriptor("test-rhq-plugin.xml");
    }

    @Deployment(name = "without-native")
    @TargetsContainer("pc")
    public static RhqAgentPluginArchive getAnotherPlugin() {
        return getPlugin();
    }

    @Test
    @OperateOnDeployment("with-native")
    public void testNativeSystemInfoAvailable() {
        SystemInfo info = SystemInfoFactory.createSystemInfo();
        Assert.assertNotEquals(info.getClass(), JavaSystemInfo.class);
    }

    @Test
    @OperateOnDeployment("without-native")
    public void testJavaSystemInfoUsed() {
        SystemInfo info = SystemInfoFactory.createSystemInfo();
        Assert.assertEquals(info.getClass(), JavaSystemInfo.class);
    }
}
