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

package org.rhq.enterprise.server.plugin;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A server API into the server plugin infrastructure.
 * 
 * @author John Mazzitelli
 */
@Stateless
public class ServerPluginsBean implements ServerPluginsLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private ServerPluginsLocal serverPluginsBean; //self

    public List<Plugin> getServerPlugins() {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL_SERVER);
        return q.getResultList();
    }

    public Plugin getServerPlugin(String name) {
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
        query.setParameter("name", name);
        Plugin plugin = (Plugin) query.getSingleResult();
        if (plugin.getDeployment() != PluginDeploymentType.SERVER) {
            throw new IllegalArgumentException("Plugin named [" + name + "] is not a server plugin");
        }
        return plugin;
    }

    public List<Plugin> getServerPluginsById(List<Integer> pluginIds) {
        if (pluginIds == null || pluginIds.size() == 0) {
            return new ArrayList<Plugin>();
        }
        Query query = entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_IDS_AND_TYPE);
        query.setParameter("ids", pluginIds);
        query.setParameter("type", PluginDeploymentType.SERVER);
        return query.getResultList();
    }

    public void enableServerPlugins(List<Integer> pluginIds) {
        serverPluginsBean.setPluginEnabledFlag(pluginIds, true);
        LookupUtil.getServerPluginService().restartMasterPluginContainer();
        return;
    }

    public void disableServerPlugins(List<Integer> pluginIds) {
        serverPluginsBean.setPluginEnabledFlag(pluginIds, false);
        LookupUtil.getServerPluginService().restartMasterPluginContainer();
        return;
    }

    public void undeployServerPlugins(List<Integer> pluginIds) {
        serverPluginsBean.setPluginEnabledFlag(pluginIds, false);
        // TODO: actually mark the plugins as deleted, remove their files
        LookupUtil.getServerPluginService().restartMasterPluginContainer();
        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setPluginEnabledFlag(List<Integer> pluginIds, boolean enabled) {
        List<Plugin> plugins = getServerPluginsById(pluginIds);
        for (Plugin plugin : plugins) {
            plugin.setEnabled(enabled);
        }
    }

}
