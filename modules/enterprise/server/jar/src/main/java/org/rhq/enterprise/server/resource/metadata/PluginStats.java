package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.domain.plugin.Plugin;

public class PluginStats {
    private Plugin plugin;

    private Integer resourceTypeCount;

    private Integer resourceCount;

    public PluginStats(Plugin plugin, Integer resourceTypeCount, Integer resourceCount) {
        this.plugin = plugin;
        this.resourceTypeCount = resourceTypeCount;
        this.resourceCount = resourceCount;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Integer getResourceTypeCount() {
        return resourceTypeCount;
    }

    public Integer getResourceCount() {
        return resourceCount;
    }
}
