/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.scheduler.jobs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This job purges plugins from the database asynchronously. It queries for plugins that are scheduled to be purged.
 * A plugin is considered a candidate for purging when its status is <code>DELETED</code>, all its resource types
 * are deleted and all the servers in HA cloud have acknowledged they know about the plugin deletion.
 * Purging resource types is performed by {@link PurgeResourceTypesJob} independently of this job.
 */
public class PurgePluginsJob extends AbstractStatefulJob {
    private static final Log LOG = LogFactory.getLog(PurgePluginsJob.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        purgeAgentPlugins();
        purgeServerPlugins();
    }

    private void purgeAgentPlugins() {
        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
        List<Plugin> plugins = pluginMgr.findAllDeletedPlugins();
        List<Plugin> pluginsToPurge = new ArrayList<Plugin>();

        for (Plugin plugin : plugins) {
            if (pluginMgr.isReadyForPurge(plugin)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Preparing to purge plugin [" + plugin.getName() + "]");
                }
                pluginsToPurge.add(plugin);
            }
        }

        if (!pluginsToPurge.isEmpty()) {
            pluginMgr.purgePlugins(pluginsToPurge);
        }
    }

    private void purgeServerPlugins() {
        ServerPluginsLocal pluginMgr = LookupUtil.getServerPlugins();

        for (ServerPlugin p : pluginMgr.getDeletedPlugins()) {
            if (pluginMgr.isReadyForPurge(p.getId())) {
                pluginMgr.purgeServerPlugin(p.getId());
            }
        }
    }
}
