package org.rhq.enterprise.server.scheduler.jobs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This job purges plugins from the database asynchronously. It queries for plugins that are scheduled to be purged.
 * A plugin is considered a candidate for purging when its status is <code>DELETED</code> and its <code>ctime</code>
 * is set to {@link Plugin#PURGED}. A plugin is only purged when all of the resource types defined by the plugin have
 * already been purged. Puring resource types is performed by {@link PurgeResourceTypesJob}.
 */
public class PurgePluginsJob extends AbstractStatefulJob {

    private static final Log log = LogFactory.getLog(PurgePluginsJob.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
        List<Plugin> plugins = pluginMgr.findPluginsMarkedForPurge();
        List<Plugin> pluginsToPurge = new ArrayList<Plugin>();

        for (Plugin plugin : plugins) {
            if (pluginMgr.isReadyForPurge(plugin)) {
                log.debug("Preparing to purge plugin [" + plugin.getName() + "]");
                pluginsToPurge.add(plugin);
            }
        }

        if (!pluginsToPurge.isEmpty()) {
            pluginMgr.purgePlugins(pluginsToPurge);
            log.debug("Purged " + pluginsToPurge.size() + " plugins");
        }
    }
}
