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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

//@Scope(ScopeType.PAGE)
//@Name("editPluginConfigurationUIBean")
public class EditPluginConfigurationUIBean extends AbstractPluginConfigurationUIBean implements Serializable {

    //    @Logger
    //    private Log log;

    private Configuration currentConfiguration;
    private ConfigurationDefinition currentConfigurationDefinition;

    public Configuration getCurrentConfiguration() {
        return currentConfiguration;
    }

    public ConfigurationDefinition getCurrentConfigurationDefinition() {
        return currentConfigurationDefinition;
    }

    //    @Create
    public void init() {
        checkPermission();
        lookupConfigDefinitions();
    }

    // The superclass holds the plugin instance variable, but in this
    // case we want it injected via the PluginFactory - so this serves as
    // the injection point
    //    @In(value = "plugin", required = false)
    public void setCurrentPlugin(ServerPlugin currentPlugin) {
        if (currentPlugin != null) {
            setPlugin(currentPlugin);
        }
    }

    //    @RequestParameter("listName")
    public void setListName(String listName) {
        ServerPlugin plugin = getPlugin();

        if (listName != null && plugin != null) {
            if (hasListName(plugin.getPluginConfiguration(), listName)) {
                this.currentConfiguration = plugin.getPluginConfiguration();
                this.currentConfigurationDefinition = getPluginConfigurationDefinition();
            } else if (hasListName(plugin.getScheduledJobsConfiguration(), listName)) {
                this.currentConfiguration = plugin.getScheduledJobsConfiguration();
                this.currentConfigurationDefinition = getScheduledJobsDefinition();
            }
        }
    }

    private boolean hasListName(Configuration config, String listName) {
        if (config != null) {
            return config.getList(listName) != null;
        }

        return false;
    }

    public String finishMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map updated.");
        return "success";
    }

    public String updatePlugin() {
        try {
            ServerPluginManagerLocal serverPlugins = LookupUtil.getServerPluginManager();
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            ServerPlugin plugin = serverPlugins.updateServerPluginExceptContent(subject, getPlugin());
            setPlugin(plugin);

            // Since the config has changed, we can tell the server to re-load the plugin now
            // in order for it to pick up the changes immediately. Any other servers in the HA Server Cloud
            // will pick up these changes later, when they scan for changes in the database.
            // Note that if the plugin is disabled, don't bother since the plugin isn't really running anyway.
            if (plugin.isEnabled()) {
                // enabling an already enabled plugin forces the plugin to reload
                serverPlugins.enableServerPlugins(subject, getPluginIdList());
            }

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration settings saved.");
        } catch (Exception e) {
            //            log.error("Error updating the plugin configurations.", e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "There was an error changing the configuration settings.", e);

            return null;
        }

        return "success";
    }

    private List<Integer> getPluginIdList() {
        List<Integer> idList = new ArrayList<Integer>(1);
        ServerPlugin plugin = getPlugin();

        if (plugin != null) {
            idList.add(plugin.getId());
        }

        return idList;
    }
}
