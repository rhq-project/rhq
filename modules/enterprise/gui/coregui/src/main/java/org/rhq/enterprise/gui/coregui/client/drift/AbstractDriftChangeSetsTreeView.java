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
package org.rhq.enterprise.gui.coregui.client.drift;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeClickHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author John Mazzitelli
 */
public abstract class AbstractDriftChangeSetsTreeView extends LocatableTreeGrid {

    // We may need to wait for tree data to be fetched before we can complete processing the selected path.
    // If so, hold it here and retry after the datasource pulls the data.
    private ViewPath pendingPath = null;

    public AbstractDriftChangeSetsTreeView(String locatorId, boolean canManageDrift) {
        super(locatorId);
        setWidth100();
        setHeight100();
        setLeaveScrollbarGap(false);
        // fetch the top nodes at the inital onDraw()
        setAutoFetchData(true);
        setAnimateFolders(false);
        setSelectionType(SelectionStyle.SINGLE);
        setShowRollOver(false);
        setSortField(DriftChangeSetsTreeDataSource.ATTR_NAME);
        setShowHeader(false);

        setDataSource(new DriftChangeSetsTreeDataSource(canManageDrift));

        addNodeClickHandler(new NodeClickHandler() {
            public void onNodeClick(NodeClickEvent event) {
                TreeNode node = event.getNode();
                String link = getNodeTargetLink(node);
                CoreGUI.goToView(link);
            }
        });
    }

    @Override
    protected void onInit() {
        super.onInit();

        // We may need to wait for tree data to be fetched before we can complete processing the selected path.
        // When the datasource pulls data keep processing the selectedPath if necessary.
        this.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                if (null != pendingPath) {
                    selectPath(pendingPath);
                }
            }
        });
    }

    public void selectPath(ViewPath viewPath) {
        Tree theTree = getTree();

        if (viewPath.viewsLeft() > 0) {
            String key = "";
            for (ViewId view : viewPath.getViewPath().subList(2, viewPath.getViewPath().size())) {
                if (key.length() > 0) {
                    key += "_";
                }

                key += view.getPath();

                TreeNode node = theTree.findById(key);

                if (node != null) {
                    // open the node, this will force a fetch of child data if necessary
                    theTree.openFolder(node);
                } else {
                    // wait for data to get loaded...
                    pendingPath = new ViewPath(viewPath.toString());
                    return;
                }
            }

            // we found the node, so keep going
            pendingPath = null;

            final String finalKey = key;
            GWT.runAsync(new RunAsyncCallback() {
                public void onFailure(Throwable reason) {
                }

                public void onSuccess() {
                    TreeNode node = getTree().findById(finalKey);
                    if (node != null) {
                        deselectAllRecords();
                        selectRecord(node);
                    }
                }
            });
        } else {
            deselectAllRecords();
            if (getTotalRows() > 0)
                selectRecord(0);
        }
    }

    public void refresh() {
        invalidateCache();
    }

    /**
     * Returns the link (as a string) that the client should be redirected to
     * if the given node is clicked.
     * 
     * @param node the node whose target link is to be returned
     * @return the node's link
     */
    protected abstract String getNodeTargetLink(TreeNode node);
}
