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

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.AbstractPlugin;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
@Scope(ScopeType.PAGE)
@Name("InstalledPluginUIBean")
public class InstalledPluginUIBean {

    private static final String OUTCOME_SUCCESS_SERVER_PLUGIN = "successServerPlugin";
    public static final String MANAGED_BEAN_NAME = InstalledPluginUIBean.class.getSimpleName();
    private final Log log = LogFactory.getLog(InstalledPluginUIBean.class);
    @In
    private AbstractPlugin plugin;
    private ConfigurationDefinition pluginConfigurationDefinition;
    private ConfigurationDefinition scheduledJobsDefinition;

    public AbstractPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(AbstractPlugin plugin) {
        this.plugin = plugin;
    }

    public ServerPlugin getServerPlugin() {
        return (ServerPlugin) this.plugin;
    }

    public void setServerPlugin(ServerPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigurationDefinition getPluginConfigurationDefinition() {
        return this.pluginConfigurationDefinition;
    }

    public ConfigurationDefinition getScheduledJobsDefinition() {
        return this.scheduledJobsDefinition;
    }

    public boolean isEditable() {
        return this.pluginConfigurationDefinition != null || this.scheduledJobsDefinition != null;
    }

    public String updatePlugin() {
        // note we assume we are editing a server plugin - we don't support editing agent plugins yet
        try {
            ServerPluginsLocal serverPlugins = LookupUtil.getServerPlugins();
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            serverPlugins.updateServerPluginExceptContent(subject, getServerPlugin());
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration settings saved.");

            return OUTCOME_SUCCESS_SERVER_PLUGIN;
        } catch (Exception e) {
            log.error("Error updating the plugin configurations.", e);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "There was an error changing the configuration settings.", e);

            return null;
        }
    }

    @Create
    public void lookupConfigDefinitions() {
        hasPermission();
        String pluginName = this.plugin.getName();

        if (this.plugin.getDeployment() == PluginDeploymentType.SERVER) {
            try {
                ServerPluginsLocal serverPluginsBean = LookupUtil.getServerPlugins();
                PluginKey pluginKey = new PluginKey((ServerPlugin) plugin);
                ServerPluginDescriptorType descriptor = serverPluginsBean.getServerPluginDescriptor(pluginKey);
                this.pluginConfigurationDefinition = ConfigurationMetadataParser.parse("pc:" + pluginName, descriptor
                    .getPluginConfiguration());
                this.scheduledJobsDefinition = ConfigurationMetadataParser.parse("jobs:" + pluginName, descriptor
                    .getScheduledJobs());
            } catch (Exception e) {
                String err = "Cannot determine what the plugin configuration or scheduled jobs configuration looks like";
                log.error(err + " - Cause: " + e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, err, e);
                return;
            }
        }
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
}
