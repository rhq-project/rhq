/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.entitlement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;

/**
 * This loads in all entitlement server plugins that can be found. You can obtain a loaded plugin's
 * {@link ServerPluginEnvironment environment}, including its classloader, from this object as well.
 *
 * @author John Mazzitelli
 */
public class EntitlementServerPluginManager extends ServerPluginManager {
    private final Log log = LogFactory.getLog(this.getClass());

    public EntitlementServerPluginManager(EntitlementServerPluginContainer pc) {
        super(pc);
    }

    // TODO override methods like initialize, shutdown, loadPlugin, etc. for custom entitlement functionality

    @Override
    public void initialize() throws Exception {
        super.initialize();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}