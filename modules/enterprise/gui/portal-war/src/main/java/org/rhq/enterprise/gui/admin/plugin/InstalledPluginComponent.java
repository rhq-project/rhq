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

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Component responsible for looking up a {@link Plugin}, obtaining
 * the <code>name</code> and <code>type</code> from request parameters.
 *
 * @author jharris
 */
@Name("installedPlugin")
public class InstalledPluginComponent {

    @RequestParameter("plugin")
    private String name;
    @RequestParameter("pluginType")
    private PluginDeploymentType type;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PluginDeploymentType getType() {
        return this.type;
    }

    public void setType(PluginDeploymentType type) {
        this.type = type;
    }

    @Factory(value="plugin", autoCreate=true, scope=ScopeType.PAGE)
    public Plugin lookupPlugin() {
        if (this.type == PluginDeploymentType.AGENT) {
            return LookupUtil.getResourceMetadataManager().getPlugin(this.name);
        } else if (this.type == PluginDeploymentType.SERVER) {
            ServerPluginsLocal serverPluginsBean = LookupUtil.getServerPlugins();
            Plugin plugin = serverPluginsBean.getServerPlugin(this.name);
            return serverPluginsBean.getServerPluginRelationships(plugin);
        }

        return null;
    }
}
