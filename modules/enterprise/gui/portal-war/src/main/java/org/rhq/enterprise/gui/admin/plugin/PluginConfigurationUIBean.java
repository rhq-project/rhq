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

import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
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
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

@Name("pluginConfigUIBean")
public class PluginConfigurationUIBean {

    @Logger
    private Log log;

    private final ServerPluginsLocal serverPluginsBean = LookupUtil.getServerPlugins();

    private TreeNode root;
    private ServerPlugin currentPlugin;
    private ConfigurationDefinition pluginConfigurationDefinition;
    private ConfigurationDefinition scheduledJobsDefinition;


    public TreeNode getRoot() {
        return root;
    }
    
    public ServerPlugin getCurrentPlugin() {
        return currentPlugin;
    }

    public ConfigurationDefinition getPluginConfigurationDefinition() {
        return this.pluginConfigurationDefinition;
    }

    public ConfigurationDefinition getScheduledJobsDefinition() {
        return this.scheduledJobsDefinition;
    }

    @Create
    public void createTree() {
        hasPermission();

        this.root = new TreeNodeImpl();
        Map<ServerPluginType, List<PluginKey>> types = serverPluginsBean.getInstalledServerPluginsGroupedByType();

        for (ServerPluginType type : types.keySet()) {
            TreeNode typeNode = createTypeNode(type, types.get(type));

            if (typeNode != null) {
                typeNode.setParent(root);
                root.addChild(type, typeNode);
            }
        }
    }

    private TreeNode createTypeNode(ServerPluginType type, List<PluginKey> plugins) {
        TreeNode typeNode = new TreeNodeImpl();
        typeNode.setData(type);

        if (plugins != null && plugins.size() > 0) {
            for (PluginKey pluginKey : plugins) {
                TreeNode pluginNode = createPluginNode(pluginKey);
                pluginNode.setParent(typeNode);

                typeNode.addChild(pluginKey, pluginNode);
            }
        }

        return typeNode;
    }

    private TreeNode createPluginNode(PluginKey pluginKey) {
        TreeNode pluginNode = new TreeNodeImpl();
        pluginNode.setData(pluginKey);

        return pluginNode;
    }

    public void processSelection(NodeSelectedEvent event) {
        HtmlTree tree = (HtmlTree) event.getSource();
        PluginKey pluginKey = (PluginKey) tree.getRowData();

        this.currentPlugin = serverPluginsBean.getServerPlugin(pluginKey);
        this.currentPlugin = serverPluginsBean.getServerPluginRelationships(currentPlugin);

        lookupConfigDefinitions();
    }

    public void lookupConfigDefinitions() {
        if (this.currentPlugin.getDeployment() == PluginDeploymentType.SERVER) {
            PluginKey pluginKey = new PluginKey(this.currentPlugin);
            
            try {
                ServerPluginDescriptorType descriptor = serverPluginsBean.getServerPluginDescriptor(pluginKey);
                this.pluginConfigurationDefinition = ConfigurationMetadataParser.parse("pc:" + pluginKey.getPluginName(), descriptor.getPluginConfiguration());
                this.scheduledJobsDefinition = ConfigurationMetadataParser.parse("jobs:" + pluginKey.getPluginName(), descriptor.getScheduledJobs());
            } catch (Exception e) {
                String err = "Cannot determine what the plugin configuration or scheduled jobs configuration looks like";
                log.error(err + " - Cause: " + e);
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, err, e);
            }
        }
    }

    // TODO:  Find a more centralized way to do this - maybe via annotation
    //        on the class??
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