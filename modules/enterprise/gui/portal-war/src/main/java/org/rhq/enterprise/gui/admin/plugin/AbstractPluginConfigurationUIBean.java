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

import javax.faces.application.FacesMessage;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginDeploymentType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

public abstract class AbstractPluginConfigurationUIBean {

    protected final ServerPluginManagerLocal serverPluginsBean = LookupUtil.getServerPluginManager();
    //    @Logger
    //    private Log log;

    private ServerPlugin plugin;
    private ConfigurationDefinition pluginConfigurationDefinition;
    private ConfigurationDefinition scheduledJobsDefinition;

    public ServerPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(ServerPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigurationDefinition getPluginConfigurationDefinition() {
        return this.pluginConfigurationDefinition;
    }

    public ConfigurationDefinition getScheduledJobsDefinition() {
        return this.scheduledJobsDefinition;
    }

    public boolean isEditable() {
        return this.plugin != null &&
                (this.plugin.getPluginConfiguration() != null ||
                this.plugin.getScheduledJobsConfiguration() != null);
    }

    // TODO:  Find a more centralized way to do this - maybe via annotation
    //        on the class??
    /**
     * Throws a permission exception if the user is not allowed to access this functionality.
     */
    protected void checkPermission() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
            throw new PermissionException("User [" + subject.getName() + "] does not have the proper permissions to view or manage plugins");
        }
    }

    protected void lookupConfigDefinitions() {
        if (this.plugin.getDeployment() == PluginDeploymentType.SERVER) {
            PluginKey pluginKey = new PluginKey(this.plugin);

            try {
                ServerPluginDescriptorType descriptor = serverPluginsBean.getServerPluginDescriptor(pluginKey);
                this.pluginConfigurationDefinition = ConfigurationMetadataParser.parse("pc:" + pluginKey.getPluginName(), descriptor.getPluginConfiguration());
                this.scheduledJobsDefinition = ConfigurationMetadataParser.parse("jobs:" + pluginKey.getPluginName(), descriptor.getScheduledJobs());
            } catch (Exception e) {
                String err = "Cannot determine what the plugin configuration or scheduled jobs configuration looks like";
                // log.error(err + " - Cause: " + e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, err, e);
            }
        }
    }
}
