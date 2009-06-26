/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.test;

import org.testng.annotations.Test;

import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.PluginContainer;

/**
 * @author Ian Springer
 */
@Test(groups = "as5-plugin", enabled = AbstractPluginTest.ENABLE_TESTS)
public class GeneralPluginTest extends AbstractPluginTest {    
    @Test(enabled = ENABLE_TESTS)
    public void testPluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(getPluginName());
        assert (pluginEnvironment != null) : "Null plugin environment - " + getPluginName() + " plugin was not loaded.";
        assert (pluginEnvironment.getPluginName().equals(getPluginName()));
    }
}
