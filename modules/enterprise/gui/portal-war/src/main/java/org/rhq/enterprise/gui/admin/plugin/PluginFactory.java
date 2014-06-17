/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.gui.admin.plugin;

import org.rhq.core.domain.plugin.AbstractPlugin;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Component responsible for providing a {@link Plugin} via request
 * parameters.
 *
 * @author jharris
 */
//@Name("pluginFactory")
public class PluginFactory {

    //@RequestParameter("plugin")
    private String name;
    //@RequestParameter("deployment")
    private PluginDeploymentType deployment;
    //@RequestParameter("pluginType")
    private String pluginType;

    //@Factory(value = "plugin", autoCreate = true)
    public AbstractPlugin lookupPlugin() {
        if (this.deployment == PluginDeploymentType.AGENT) {
            return LookupUtil.getPluginManager().getPlugin(this.name);
        } else if (this.deployment == PluginDeploymentType.SERVER) {
            PluginKey pluginKey = new PluginKey(this.deployment, this.pluginType, this.name);
            ServerPluginManagerLocal serverPluginsBean = LookupUtil.getServerPluginManager();
            ServerPlugin plugin = serverPluginsBean.getServerPlugin(pluginKey);
            return serverPluginsBean.getServerPluginRelationships(plugin);
        }

        return null;
    }
}
