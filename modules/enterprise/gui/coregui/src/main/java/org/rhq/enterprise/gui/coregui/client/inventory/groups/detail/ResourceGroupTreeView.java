/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeView extends VLayout {

    private TreeGrid treeGrid;

    public ResourceGroupTreeView() {
        setWidth(250);
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.treeGrid = new TreeGrid();
        this.treeGrid.setWidth100();
        this.treeGrid.setHeight100();
        treeGrid.setShowRoot(true);

        addMember(this.treeGrid);
    }

    public void setSelectedGroup(int groupId) {
        GWTServiceLookup.getClusterService().getClusterTree(groupId,
                new AsyncCallback<ClusterFlyweight>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load tree", caught);
                    }

                    public void onSuccess(ClusterFlyweight result) {
                        loadTree(result);
                    }
                });
    }

    private void loadTree(ClusterFlyweight root) {
        TreeNode rootNode = new TreeNode(root.getName());

        loadTree(rootNode, root);

        Tree tree = new Tree();
        tree.setRoot(rootNode);

        treeGrid.setData(tree);
        markForRedraw();
    }

    public void loadTree(TreeNode parent, ClusterFlyweight parentNode) {
        if (!parentNode.getChildren().isEmpty()) {

            ArrayList<TreeNode> childNodes = new ArrayList<TreeNode>();

            HashMap<Integer,TreeNode> typeNodes = new HashMap<Integer, TreeNode>();

            for (ClusterFlyweight child : parentNode.getChildren()) {
                TreeNode node = new TreeNode(child.getName());
                childNodes.add(node);

                if (child.getChildren().isEmpty()) {
                    node.setIsFolder(false);
                } else {
                    node.setIsFolder(true);
                    loadTree(node, child);
                }
            }
            parent.setChildren(childNodes.toArray(new TreeNode[childNodes.size()]));
        }
    }
}
