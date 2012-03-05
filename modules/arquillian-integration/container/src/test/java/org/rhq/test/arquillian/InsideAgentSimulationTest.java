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
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.pc.PluginContainer;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * Test for making sure that we can simulate the plugin container running inside a full agent
 * being connected to a mocked out server.
 *
 * @author Lukas Krejci
 */
public class InsideAgentSimulationTest extends Arquillian {

    @Deployment(name = "simplePlugin")
    @TargetsContainer("manual-pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "test-plugin2-1.0.0.jar")
            .addClasses(TestDiscoveryComponent.class, TestResourceComponent.class)
            .setPluginDescriptor("test-rhq-plugin.xml");
    }

    @ArquillianResource
    private PluginContainer pc;
    
    @Test
    public void notNull() {
        Assert.assertNotNull(pc);
    }
}
