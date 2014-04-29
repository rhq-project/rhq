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

package org.rhq.coregui.server.gwt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.AbstractPlugin;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.domain.plugin.ServerPluginControlDefinition;
import org.rhq.core.domain.plugin.ServerPluginControlResults;
import org.rhq.coregui.client.gwt.PluginGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ControlDefinition;

/**
 * @author John Mazzitelli
 */
public class PluginGWTServiceImpl extends AbstractGWTServiceImpl implements PluginGWTService {
    private static final long serialVersionUID = 1L;

    private final Log log = LogFactory.getLog(PluginGWTServiceImpl.class);

    private PluginManagerLocal pluginManager = LookupUtil.getPluginManager();
    private ServerPluginsLocal serverPluginManager = LookupUtil.getServerPlugins();
    

    @Override
    public void restartMasterPluginContainer() throws RuntimeException {
        try {
            serverPluginManager.restartMasterPluginContainer(getSessionSubject());
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Plugin getAgentPlugin(int pluginId) throws RuntimeException {
        try {
            List<Plugin> result = getSelectedAgentPlugins(new int[] { pluginId });
            if (result == null || result.isEmpty()) {
                return null;
            }
            return SerialUtility.prepare(result.get(0), "PluginGWTService.getAgentPlugin");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ServerPlugin getServerPlugin(int pluginId, boolean includeRelationships) throws RuntimeException {
        try {
            ArrayList<Integer> ids = new ArrayList<Integer>(1);
            ids.add(pluginId);
            List<ServerPlugin> result = serverPluginManager.getServerPluginsById(ids); // use this API to get config, too
            if (result == null || result.isEmpty()) {
                return null;
            }
            ServerPlugin plugin = result.get(0);
            if (includeRelationships) {
                plugin = serverPluginManager.getServerPluginRelationships(plugin);
            }
            return SerialUtility.prepare(plugin, "PluginGWTService.getServerPlugin");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<Plugin> getAgentPlugins(boolean includeDeletedPlugins) throws RuntimeException {
        try {
            ArrayList<Plugin> result;
            if (includeDeletedPlugins) {
                result = new ArrayList<Plugin>(pluginManager.getPlugins());
            } else {
                result = new ArrayList<Plugin>(pluginManager.getInstalledPlugins());
            }
            return SerialUtility.prepare(result, "PluginGWTService.getAgentPlugins");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<ServerPlugin> getServerPlugins(boolean includeDeletedPlugins) throws RuntimeException {
        try {
            ArrayList<ServerPlugin> result;
            if (includeDeletedPlugins) {
                result = new ArrayList<ServerPlugin>(serverPluginManager.getAllServerPlugins());
            } else {
                result = new ArrayList<ServerPlugin>(serverPluginManager.getServerPlugins());
            }
            return SerialUtility.prepare(result, "PluginGWTService.getServerPlugins");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void scanAndRegister() throws RuntimeException {
        try {
            PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();
            scanner.scanAndRegister();
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> enableAgentPlugins(int[] selectedPluginIds) throws RuntimeException {
        try {
            List<Plugin> allSelectedPlugins = getSelectedAgentPlugins(selectedPluginIds);
            ArrayList<String> selectedPluginNames = new ArrayList<String>();
            ArrayList<Plugin> pluginsToEnable = new ArrayList<Plugin>();

            for (Plugin selectedPlugin : allSelectedPlugins) {
                if (!selectedPlugin.isEnabled() && selectedPlugin.getStatus() == PluginStatusType.INSTALLED) {
                    selectedPluginNames.add(selectedPlugin.getDisplayName());
                    pluginsToEnable.add(selectedPlugin);
                }
            }

            if (selectedPluginNames.isEmpty()) {
                log.debug("No disabled agent plugins were selected. Nothing to enable");
                return selectedPluginNames;
            }

            pluginManager.enablePlugins(getSessionSubject(), getIds(pluginsToEnable));
            log.info("Enabled agent plugins: " + selectedPluginNames);
            return selectedPluginNames;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> disableAgentPlugins(int[] selectedPluginIds) throws RuntimeException {
        try {
            List<Plugin> allSelectedPlugins = getSelectedAgentPlugins(selectedPluginIds);
            ArrayList<String> selectedPluginNames = new ArrayList<String>();
            ArrayList<Plugin> pluginsToDisable = new ArrayList<Plugin>();

            for (Plugin selectedPlugin : allSelectedPlugins) {
                if (selectedPlugin.isEnabled()) {
                    selectedPluginNames.add(selectedPlugin.getDisplayName());
                    pluginsToDisable.add(selectedPlugin);
                }
            }

            if (selectedPluginNames.isEmpty()) {
                log.debug("No enabled agent plugins were selected. Nothing to disable");
                return selectedPluginNames;
            }

            pluginManager.disablePlugins(getSessionSubject(), getIds(pluginsToDisable));
            log.info("Disabled agent plugins: " + selectedPluginNames);
            return selectedPluginNames;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> deleteAgentPlugins(int[] selectedPluginIds) throws RuntimeException {
        try {
            List<Plugin> allSelectedPlugins = getSelectedAgentPlugins(selectedPluginIds);

            if (allSelectedPlugins.isEmpty()) {
                log.debug("No agent plugins were selected. Nothing to delete");
                return new ArrayList<String>(0);
            }

            ArrayList<String> pluginNames = new ArrayList<String>();
            for (Plugin plugin : allSelectedPlugins) {
                pluginNames.add(plugin.getDisplayName());
            }

            pluginManager.deletePlugins(getSessionSubject(), getIds(allSelectedPlugins));
            log.info("Deleted agent plugins: " + pluginNames);
            PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();
            scanner.scanAndRegister();
            return pluginNames;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> enableServerPlugins(int[] selectedPluginIds) throws RuntimeException {
        try {
            List<ServerPlugin> allSelectedPlugins = getSelectedServerPlugins(selectedPluginIds);
            ArrayList<String> selectedPluginNames = new ArrayList<String>();
            ArrayList<ServerPlugin> pluginsToEnable = new ArrayList<ServerPlugin>();

            for (ServerPlugin selectedPlugin : allSelectedPlugins) {
                if (!selectedPlugin.isEnabled() && selectedPlugin.getStatus() == PluginStatusType.INSTALLED) {
                    selectedPluginNames.add(selectedPlugin.getDisplayName());
                    pluginsToEnable.add(selectedPlugin);
                }
            }

            if (selectedPluginNames.isEmpty()) {
                log.debug("No disabled server plugins were selected. Nothing to enable");
                return selectedPluginNames;
            }

            List<PluginKey> enabled = serverPluginManager.enableServerPlugins(getSessionSubject(),
                getIds(pluginsToEnable));
            if (enabled.size() == pluginsToEnable.size()) {
                return selectedPluginNames;
            } else {
                ArrayList<String> enabledPlugins = new ArrayList<String>();
                ArrayList<String> failedPlugins = new ArrayList<String>();
                for (ServerPlugin pluginToEnable : pluginsToEnable) {
                    PluginKey key = PluginKey.createServerPluginKey(pluginToEnable.getType(), pluginToEnable.getName());
                    if (enabled.contains(key)) {
                        enabledPlugins.add(pluginToEnable.getDisplayName());
                    } else {
                        failedPlugins.add(pluginToEnable.getDisplayName());
                    }
                }
                if (enabledPlugins.size() > 0) {
                    log.info("Enabled server plugins: " + enabledPlugins);
                }
                if (failedPlugins.size() > 0) {
                    log.info("Failed to enable server plugins: " + failedPlugins);
                }
                return enabledPlugins;
            }
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> disableServerPlugins(int[] selectedPluginIds) throws RuntimeException {
        try {
            List<ServerPlugin> allSelectedPlugins = getSelectedServerPlugins(selectedPluginIds);
            ArrayList<String> selectedPluginNames = new ArrayList<String>();
            ArrayList<ServerPlugin> pluginsToDisable = new ArrayList<ServerPlugin>();

            for (ServerPlugin selectedPlugin : allSelectedPlugins) {
                if (selectedPlugin.isEnabled()) {
                    selectedPluginNames.add(selectedPlugin.getDisplayName());
                    pluginsToDisable.add(selectedPlugin);
                }
            }

            if (selectedPluginNames.isEmpty()) {
                log.debug("No enabled server plugins were selected. Nothing to disable");
                return selectedPluginNames;
            }

            serverPluginManager.disableServerPlugins(getSessionSubject(), getIds(pluginsToDisable));
            log.info("Disabled server plugins: " + selectedPluginNames);
            return selectedPluginNames;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<String> deleteServerPlugins(int[] selectedPluginIds) throws RuntimeException {
        try {
            List<ServerPlugin> allSelectedPlugins = getSelectedServerPlugins(selectedPluginIds);
            ArrayList<String> selectedPluginNames = new ArrayList<String>();
            ArrayList<ServerPlugin> pluginsToUndeploy = new ArrayList<ServerPlugin>();
            for (ServerPlugin selectedPlugin : allSelectedPlugins) {
                if (selectedPlugin.getStatus() == PluginStatusType.INSTALLED) {
                    selectedPluginNames.add(selectedPlugin.getDisplayName());
                    pluginsToUndeploy.add(selectedPlugin);
                }
            }

            if (selectedPluginNames.isEmpty()) {
                log.debug("No deployed server plugins were selected. Nothing to undeploy");
                return selectedPluginNames;
            }

            serverPluginManager.deleteServerPlugins(getSessionSubject(), getIds(pluginsToUndeploy));
            log.info("Undeployed server plugins: " + selectedPluginNames);
            return selectedPluginNames;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ConfigurationDefinition getServerPluginConfigurationDefinition(PluginKey pluginKey) throws RuntimeException {
        try {
            ConfigurationDefinition def;
            def = serverPluginManager.getServerPluginConfigurationDefinition(pluginKey);
            return def;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ConfigurationDefinition getServerPluginScheduledJobsDefinition(PluginKey pluginKey) throws RuntimeException {
        try {
            ConfigurationDefinition def;
            def = serverPluginManager.getServerPluginScheduledJobsDefinition(pluginKey);
            return def;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ArrayList<ServerPluginControlDefinition> getServerPluginControlDefinitions(PluginKey serverPluginKey)
        throws RuntimeException {
        try {
            List<ControlDefinition> defs = serverPluginManager.getServerPluginControlDefinitions(serverPluginKey);

            // ControlDefinition is a server-side only class - we need to convert it to our domain DTO
            if (defs == null || defs.isEmpty()) {
                return new ArrayList<ServerPluginControlDefinition>(0);
            }
            ArrayList<ServerPluginControlDefinition> spcd = new ArrayList<ServerPluginControlDefinition>(defs.size());
            for (ControlDefinition cd : defs) {
                spcd.add(new ServerPluginControlDefinition(cd.getName(), cd.getDisplayName(), cd.getDescription(), cd
                    .getParameters(), cd.getResults()));
            }
            return SerialUtility.prepare(spcd, "PluginGWTService.getServerPluginControlDefinitions");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ServerPluginControlResults invokeServerPluginControl(PluginKey serverPluginKey, String controlName,
        Configuration params) throws RuntimeException {
        try {
            ControlResults results;
            results = serverPluginManager.invokeServerPluginControl(getSessionSubject(), serverPluginKey, controlName,
                params);

            // ControlDefinition is a server-side only class - we need to convert it to our domain DTO
            ServerPluginControlResults spcr = new ServerPluginControlResults();
            for (Property prop : results.getComplexResults().getProperties()) {
                spcr.getComplexResults().put(prop.deepCopy(true));
            }
            if (!results.isSuccess()) {
                spcr.setError(results.getError());
            }
            return spcr;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }

    }

    @Override
    public void updateServerPluginConfiguration(PluginKey serverPluginKey, Configuration config)
        throws RuntimeException {
        try {
            ServerPluginsLocal serverPlugins = LookupUtil.getServerPlugins();

            // first load the full original plugin data
            ServerPlugin plugin = serverPlugins.getServerPlugin(serverPluginKey);
            if (plugin == null) {
                throw new IllegalArgumentException("Unknown plugin key: " + serverPluginKey);
            }

            plugin = serverPlugins.getServerPluginRelationships(plugin);

            // now overwrite the config that we want to set and tell the server about it
            plugin.setPluginConfiguration(config);
            serverPlugins.updateServerPluginExceptContent(getSessionSubject(), plugin);

            // Since the config has changed, we can tell the server to re-load the plugin now
            // in order for it to pick up the changes immediately. Any other servers in the HA Server Cloud
            // will pick up these changes later, when they scan for changes in the database.
            // Note that if the plugin is disabled, don't bother since the plugin isn't really running anyway.
            if (plugin.isEnabled()) {
                // enabling an already enabled plugin forces the plugin to reload
                ArrayList<Integer> id = new ArrayList<Integer>(1);
                id.add(plugin.getId());
                serverPlugins.enableServerPlugins(getSessionSubject(), id);
            }
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updateServerPluginScheduledJobs(PluginKey serverPluginKey, Configuration jobsConfig)
        throws RuntimeException {
        try {
            ServerPluginsLocal serverPlugins = LookupUtil.getServerPlugins();

            // first load the full original plugin data
            ServerPlugin plugin = serverPlugins.getServerPlugin(serverPluginKey);
            if (plugin == null) {
                throw new IllegalArgumentException("Unknown plugin key: " + serverPluginKey);
            }

            plugin = serverPlugins.getServerPluginRelationships(plugin);

            // now overwrite the config that we want to set and tell the server about it
            plugin.setScheduledJobsConfiguration(jobsConfig);
            serverPlugins.updateServerPluginExceptContent(getSessionSubject(), plugin);

            // Since the config has changed, we can tell the server to re-load the plugin now
            // in order for it to pick up the changes immediately. Any other servers in the HA Server Cloud
            // will pick up these changes later, when they scan for changes in the database.
            // Note that if the plugin is disabled, don't bother since the plugin isn't really running anyway.
            if (plugin.isEnabled()) {
                // enabling an already enabled plugin forces the plugin to reload
                ArrayList<Integer> id = new ArrayList<Integer>(1);
                id.add(plugin.getId());
                serverPlugins.enableServerPlugins(getSessionSubject(), id);
            }
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public void updatePluginsOnAgents(long delayInMilliseconds) {
        try {
            pluginManager.schedulePluginUpdateOnAgents(getSessionSubject(), delayInMilliseconds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    private List<Plugin> getSelectedAgentPlugins(int[] selectedPluginIds) {
        if (selectedPluginIds == null || selectedPluginIds.length == 0) {
            return new ArrayList<Plugin>(0);
        }

        List<Integer> idList = new ArrayList<Integer>(selectedPluginIds.length);
        for (int id : selectedPluginIds) {
            idList.add(id);
        }
        List<Plugin> plugins = pluginManager.getAllPluginsById(idList);
        return plugins;
    }

    private List<ServerPlugin> getSelectedServerPlugins(int[] selectedPluginIds) {
        if (selectedPluginIds == null || selectedPluginIds.length == 0) {
            return new ArrayList<ServerPlugin>(0);
        }

        List<Integer> idList = new ArrayList<Integer>(selectedPluginIds.length);
        for (int id : selectedPluginIds) {
            idList.add(id);
        }
        List<ServerPlugin> plugins = serverPluginManager.getAllServerPluginsById(idList);
        return plugins;
    }

    private List<Integer> getIds(List<? extends AbstractPlugin> plugins) {
        ArrayList<Integer> ids = new ArrayList<Integer>(plugins.size());
        for (AbstractPlugin plugin : plugins) {
            ids.add(plugin.getId());
        }
        return ids;
    }

    @Override
    public ArrayList<CannedGroupExpression> getCannedGroupExpressions() {
        return new ArrayList<CannedGroupExpression>(pluginManager.getCannedGroupExpressions());
    }
}
