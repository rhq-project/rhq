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
import java.util.List;
import java.util.Map;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.richfaces.component.UITree;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

@Scope(ScopeType.PAGE)
@Name("pluginConfigUIBean")
public class PluginConfigurationUIBean extends AbstractPluginConfigurationUIBean implements Serializable {

    @RequestParameter
    private String pluginName;
    private TreeNode root;

    public TreeNode getRoot() {
        return root;
    }

    @Create
    public void init() {
        checkPermission();
        createTree();
    }

    private void createTree() {
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
        ServerPlugin plugin = serverPluginsBean.getServerPlugin(pluginKey);
        plugin = serverPluginsBean.getServerPluginRelationships(plugin);

        TreeNode pluginNode = new TreeNodeImpl();
        pluginNode.setData(plugin);

        return pluginNode;
    }

    public boolean adviseOpened(UITree node) {
        // Expand the whole tree by default
        return true;
    }

    /**
     * Advise the plugin tree if the provided node should be selected.
     *
     * @param node the {@link UITree} in question - will either have data a
     *             {@link ServerPlugin} or {@link ServerPluginType} is the row
     *             data.
     * 
     * @return <code>true</code> if this node should be selected,
     *         <code>false</code> otherise
     */
    public boolean adviseSelected(UITree node) {
        if (isServerPluginSelected(node)) {
            if (getPlugin() != null) {
                return node.getRowData() == getPlugin();
            } else { // there is no plugin currently selected
                ServerPlugin plugin = (ServerPlugin) node.getRowData();

                // if this is the node that matches the request parameter,
                // go ahead and load the config and select this node
                if (plugin.getName().equals(this.pluginName)) {
                    setPlugin(plugin);
                    lookupConfigDefinitions();

                    return true;
                }
            }
        }

        // the node is a plugin server type - we don't want to select
        // that if we don't have to.
        return false;
    }

    /**
     * Responds to node selection events in the plugin tree widget.
     *
     * @param event
     */
    public void processSelection(NodeSelectedEvent event) {
        HtmlTree node = (HtmlTree) event.getSource();

        if (isServerPluginSelected(node)) {
            setPlugin((ServerPlugin) node.getRowData());
            lookupConfigDefinitions();

            node.setSelected();
        }
    }

    private boolean isServerPluginSelected(UITree tree) {
        return tree.getRowData() instanceof ServerPlugin;
    }
}
