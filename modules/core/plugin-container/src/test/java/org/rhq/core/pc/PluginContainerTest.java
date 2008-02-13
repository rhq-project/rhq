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
package org.rhq.core.pc;

import java.io.File;
import org.testng.annotations.Test;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;

@Test
public class PluginContainerTest {
    public void testStartup() {
        PluginContainerConfiguration config = new PluginContainerConfiguration();
        config.setPluginFinder(new FileSystemPluginFinder(new File(".")));
        PluginContainer pc = PluginContainer.getInstance();
        pc.setConfiguration(config);
        pc.initialize();
        pc.shutdown();
    }
}