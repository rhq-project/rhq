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
package org.rhq.enterprise.gui.admin.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.plugin.*;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.richfaces.event.UploadEvent;

import javax.faces.application.FacesMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

/**
 */
public class InstalledPluginsUIBean {
    private final Log log = LogFactory.getLog(InstalledPluginsUIBean.class);

    public static final String MANAGED_BEAN_NAME = InstalledPluginsUIBean.class.getSimpleName();

    private PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
    private ServerPluginsLocal serverPluginsBean = LookupUtil.getServerPlugins();

    public InstalledPluginsUIBean() {
    }

    public void restartMasterPluginContainer() {

        hasPermission();
        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPluginsBean.restartMasterPluginContainer(subject);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Master plugin container has been restarted.");
        } catch (Exception e) {
            processException("Failed to restart the master plugin container", e);
        }
        return;
    }

    public Collection<Plugin> getInstalledAgentPlugins() {
        hasPermission();
        List<Plugin> plugins;

        InstalledPluginsSessionUIBean session = FacesContextUtility.getManagedBean(InstalledPluginsSessionUIBean.class);
        if (session.isShowAllAgentPlugins()) {
            plugins = pluginMgr.getPlugins();
        } else {
            plugins = pluginMgr.getInstalledPlugins();
        }
        return sort(plugins);
    }

    public Collection<ServerPlugin> getInstalledServerPlugins() {

        hasPermission();
        List<ServerPlugin> plugins;

        InstalledPluginsSessionUIBean session = FacesContextUtility.getManagedBean(InstalledPluginsSessionUIBean.class);
        if (session.isShowAllServerPlugins()) {
            plugins = serverPluginsBean.getAllServerPlugins();
        } else {
            plugins = serverPluginsBean.getServerPlugins();
        }
        plugins = sort(plugins);
        return plugins;
    }

    public void scan() {
        hasPermission();

        try {
            PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();
            scanner.scanAndRegister();
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Done scanning for updated plugins.");
        } catch (Exception e) {
            processException("Failed to scan for updated plugins", e);
        }
    }

    public void fileUploadListener(UploadEvent event) {
        hasPermission();

        try {
            File uploadedPlugin = event.getUploadItem().getFile();
            String newPluginFilename = event.getUploadItem().getFileName();

            // some browsers (IE in particular) passes an absolute filename, we just want the name of the file, no paths
            if (newPluginFilename != null) {
                newPluginFilename = newPluginFilename.replace('\\', '/');
                if (newPluginFilename.length() > 2 && newPluginFilename.charAt(1) == ':') {
                    newPluginFilename = newPluginFilename.substring(2);
                }
                newPluginFilename = new File(newPluginFilename).getName();
            }

            log.info("A new plugin [" + newPluginFilename + "] has been uploaded to [" + uploadedPlugin + "]");

            if (uploadedPlugin == null || !uploadedPlugin.exists()) {
                throw new FileNotFoundException("The uploaded plugin file [" + uploadedPlugin + "] does not exist!");
            }

            // put the new plugin file in our plugin dropbox location
            File installDir = LookupUtil.getCoreServer().getInstallDir();
            File dir = new File(installDir, "plugins");
            File pluginFile = new File(dir, newPluginFilename);
            FileOutputStream fos = new FileOutputStream(pluginFile);
            FileInputStream fis = new FileInputStream(uploadedPlugin);
            StreamUtil.copy(fis, fos);
            log.info("A new plugin has been deployed [" + pluginFile
                + "]. A scan is required now in order to register it.");
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "New plugin uploaded: " + newPluginFilename);
        } catch (Exception e) {
            processException("Failed to process uploaded plugin", e);
        }

        return;
    }

    public void enableAgentPlugins() {
        List<Plugin> allSelectedPlugins = getSelectedAgentPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        List<Plugin> pluginsToEnable = new ArrayList<Plugin>();

        for (Plugin selectedPlugin : allSelectedPlugins) {
            if (!selectedPlugin.isEnabled() && selectedPlugin.getStatus() == PluginStatusType.INSTALLED) {
                selectedPluginNames.add(selectedPlugin.getDisplayName());
                pluginsToEnable.add(selectedPlugin);
            }
        }

        if (selectedPluginNames.isEmpty()) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "No disabled plugins were selected. Nothing to enable");
            return;
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            pluginMgr.enablePlugins(subject, getIds(pluginsToEnable));
            FacesContextUtility
                .addMessage(FacesMessage.SEVERITY_INFO, "Enabled server plugins: " + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to enable agent plugins", e);
        }
        return;
    }

    public void disableAgentPlugins() {
        List<Plugin> allSelectedPlugins = getSelectedAgentPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        List<Plugin> pluginsToDisable = new ArrayList<Plugin>();

        for (Plugin selectedPlugin : allSelectedPlugins) {
            if (selectedPlugin.isEnabled()) {
                selectedPluginNames.add(selectedPlugin.getDisplayName());
                pluginsToDisable.add(selectedPlugin);
            }
        }

        if (selectedPluginNames.isEmpty()) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "No enabled plugins were selected. Nothing to disable");
            return;
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            pluginMgr.disablePlugins(subject, getIds(pluginsToDisable));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled plugins: " + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to disable agent plugins", e);
        }
        return;
    }

    public void deleteAgentPlugins() {
        if (getSelectedPluginIds().length == 0) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "No plugins were selected. Nothing to delete");
            return;
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            List<Plugin> pluginsToDelete = getSelectedAgentPlugins();
            List<String> pluginNames = new ArrayList<String>();

            for (Plugin plugin : pluginsToDelete) {
                pluginNames.add(plugin.getDisplayName());
            }

            pluginMgr.deletePlugins(subject, Arrays.asList(getSelectedPluginIds()));
            PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();
            scanner.scanAndRegister();
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted plugins: " + pluginNames);
        } catch (Exception e) {
            processException("Failed to delete agent plugins", e);
        }
        return;
    }

    public void enableServerPlugins() {
        List<ServerPlugin> allSelectedPlugins = getSelectedServerPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        List<ServerPlugin> pluginsToEnable = new ArrayList<ServerPlugin>();

        for (ServerPlugin selectedPlugin : allSelectedPlugins) {
            if (!selectedPlugin.isEnabled() && selectedPlugin.getStatus() == PluginStatusType.INSTALLED) {
                selectedPluginNames.add(selectedPlugin.getDisplayName());
                pluginsToEnable.add(selectedPlugin);
            }
        }

        if (selectedPluginNames.isEmpty()) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "No disabled plugins were selected. Nothing to enable");
            return;
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            List<PluginKey> enabled = serverPluginsBean.enableServerPlugins(subject, getIds(pluginsToEnable));
            if (enabled.size() == pluginsToEnable.size()) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled server plugins: "
                    + selectedPluginNames);
            } else {
                List<String> enabledPlugins = new ArrayList<String>();
                List<String> failedPlugins = new ArrayList<String>();
                for (ServerPlugin pluginToEnable : pluginsToEnable) {
                    PluginKey key = PluginKey.createServerPluginKey(pluginToEnable.getType(), pluginToEnable.getName());
                    if (enabled.contains(key)) {
                        enabledPlugins.add(pluginToEnable.getDisplayName());
                    } else {
                        failedPlugins.add(pluginToEnable.getDisplayName());
                    }
                }
                if (enabledPlugins.size() > 0) {
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled server plugins: "
                        + enabledPlugins);
                }
                if (failedPlugins.size() > 0) {
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to enable server plugins: "
                        + failedPlugins);
                }
            }
        } catch (Exception e) {
            processException("Failed to enable server plugins", e);
        }
        return;
    }

    public void disableServerPlugins() {
        List<ServerPlugin> allSelectedPlugins = getSelectedServerPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        List<ServerPlugin> pluginsToDisable = new ArrayList<ServerPlugin>();

        for (ServerPlugin selectedPlugin : allSelectedPlugins) {
            if (selectedPlugin.isEnabled()) {
                selectedPluginNames.add(selectedPlugin.getDisplayName());
                pluginsToDisable.add(selectedPlugin);
            }
        }

        if (selectedPluginNames.isEmpty()) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "No enabled plugins were selected. Nothing to disable");
            return;
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPluginsBean.disableServerPlugins(subject, getIds(pluginsToDisable));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled server plugins: "
                + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to disable server plugins", e);
        }
        return;
    }

    public void undeployServerPlugins() {
        List<ServerPlugin> allSelectedPlugins = getSelectedServerPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();
        List<ServerPlugin> pluginsToUndeploy = new ArrayList<ServerPlugin>();
        for (ServerPlugin selectedPlugin : allSelectedPlugins) {
            if (selectedPlugin.getStatus() == PluginStatusType.INSTALLED) {
                selectedPluginNames.add(selectedPlugin.getDisplayName());
                pluginsToUndeploy.add(selectedPlugin);
            }
        }

        if (selectedPluginNames.isEmpty()) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "No deployed plugins were selected. Nothing to undeploy");
            return;
        }

        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPluginsBean.deleteServerPlugins(subject, getIds(pluginsToUndeploy));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Undeployed server plugins: "
                + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to undeploy server plugins", e);
        }
        return;
    }

    public void purgeServerPlugins() {
        List<ServerPlugin> allSelectedPlugins = getSelectedServerPlugins();
        List<String> selectedPluginNames = new ArrayList<String>();

        for (ServerPlugin selectedPlugin : allSelectedPlugins) {
            selectedPluginNames.add(selectedPlugin.getDisplayName());
        }

        if (selectedPluginNames.isEmpty()) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "No plugins were selected. Nothing to purge");
            return;
        }

        try {
            //The JSF UI is no longer used for server plugins, so I'm just commenting out the code
            //that would not compile anymore...
            //Subject subject = EnterpriseFacesContextUtility.getSubject();
            //serverPluginsBean.purgeServerPlugins(subject, getIds(allSelectedPlugins));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Purged server plugins: " + selectedPluginNames);
        } catch (Exception e) {
            processException("Failed to undeploy server plugins", e);
        }
        return;
    }

    public void purgeAgentPlugins() {
        try {
            List<Plugin> selectedPlugins = getSelectedAgentPlugins();

            if (selectedPlugins.isEmpty()) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                        "No plugins were selected. Nothing to purge");
                return;
            }

            List<String> pluginsToDelete = new ArrayList<String>();
            for (Plugin plugin : selectedPlugins) {
                if (plugin.getStatus() != PluginStatusType.DELETED) {
                    pluginsToDelete.add(plugin.getName());
                }
            }

            if (!pluginsToDelete.isEmpty()) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "Plugins must be deleted before they " +
                    "they can be purged. The following plugins must first be deleted: " + pluginsToDelete + ". No " +
                    "plugins were purged.");
                return;
            }

            List<String> pluginNames = new ArrayList<String>();
            for (Plugin plugin : selectedPlugins) {
                pluginNames.add(plugin.getName());
            }

            //Plugin deletion has been reimplemented to not require an explicit purge operation
            //This UI bean is now not used, so I just comment out the conflicting pieces of impl.
            //Subject subject = EnterpriseFacesContextUtility.getSubject();
            //pluginMgr.markPluginsForPurge(subject, getIds(getSelectedAgentPlugins()));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Preparing to purge agent plugins: " +
                pluginNames + ". This may take a few minutes since all type definitions from the plugins must " +
                "first be purged from the system. The plugins will still be visible on this page until they have " +
                "been purged. Please note that you must not re-install the plugin while the purge is running, " +
                "as this is going to fail. Wait for re-add until the purge is done."
            );
        } catch (Exception e) {
            processException("Failed to purge agent plugins", e);
        }
    }

    private List<Integer> getIds(List<? extends AbstractPlugin> plugins) {
        ArrayList<Integer> ids = new ArrayList<Integer>(plugins.size());
        for (AbstractPlugin plugin : plugins) {
            ids.add(plugin.getId());
        }
        return ids;
    }

    private List<Plugin> getSelectedAgentPlugins() {
        Integer[] integerItems = getSelectedPluginIds();
        List<Integer> ids = Arrays.asList(integerItems);
        List<Plugin> plugins = pluginMgr.getAllPluginsById(ids);
        return plugins;
    }

    private List<ServerPlugin> getSelectedServerPlugins() {
        Integer[] integerItems = getSelectedPluginIds();
        List<Integer> ids = Arrays.asList(integerItems);
        List<ServerPlugin> plugins = serverPluginsBean.getAllServerPluginsById(ids);
        return plugins;
    }

    private Integer[] getSelectedPluginIds() {
        String[] stringItems = FacesContextUtility.getRequest().getParameterValues("selectedPlugin");
        if (stringItems == null || stringItems.length == 0) {
            return new Integer[0];
        }
        Integer[] integerItems = StringUtility.getIntegerArray(stringItems);
        return integerItems;
    }

    /**
     * Throws a permission exception if the user is not allowed to access this functionality.
     */
    private void hasPermission() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have the proper permissions to view or manage plugins");
        }
    }

    private void processException(String errMsg, Exception e) {
        log.error(errMsg + ". Cause: " + ThrowableUtil.getAllMessages(e));
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, errMsg, e);
    }

    private <T extends AbstractPlugin> List<T> sort(List<T> plugins) {
        TreeMap<String, T> map = new TreeMap<String, T>();
        for (T plugin : plugins) {
            map.put(plugin.getName(), plugin);
        }
        return new ArrayList<T>(map.values());
    }
}
