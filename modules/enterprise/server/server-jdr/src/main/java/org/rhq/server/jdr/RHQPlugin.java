/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.server.jdr;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.jdr.commands.JdrCommand;
import org.jboss.as.jdr.commands.RHQCollectFiles;
import org.jboss.as.jdr.commands.RHQStatusCommand;
import org.jboss.as.jdr.plugins.JdrPlugin;
import org.jboss.as.jdr.plugins.PluginId;
import org.jboss.as.jdr.util.Sanitizer;
import org.jboss.as.jdr.util.Sanitizers;
import org.jboss.as.jdr.util.Utils;

/**
 * Plugin to retrieve data from RHQ and add it to the JDR report
 * @author Heiko W. Rupp
 */
public class RHQPlugin implements JdrPlugin {

    private final PluginId pluginId = new PluginId("RHQ", 1, 0, null);

    @Override
    public List<JdrCommand> getCommands() throws Exception {
        Sanitizer passwordSanitizer = Sanitizers.pattern("password=.*", "password=*****");
        return Arrays.asList(
            new RHQStatusCommand(),
            new RHQCollectFiles("bin/rhq-server.properties").sanitizer(passwordSanitizer),
            new RHQCollectFiles("logs/server.log").limit(50 * Utils.ONE_MB),
            new RHQCollectFiles("logs/server.log*").omit("logs/server.log"),
            new RHQCollectFiles("logs/rhq-storage.log")
        );
    }

    @Override
    public PluginId getPluginId() {
        return pluginId;
    }
}
