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
package org.rhq.enterprise.gui.coregui.client.content.repository.tree;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.History;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeClickHandler;

import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.tree.BundleTreeDataSource;

/**
 * @author Greg Hinkle
 */
public class ContentRepositoryTreeView extends TreeGrid {

    public ContentRepositoryTreeView() {

        setWidth100();
        setHeight100();

        setWidth100();
        setHeight100();
        setShowRoot(true);
        setAutoFetchData(true);
        setAnimateFolders(false);
        setSelectionType(SelectionStyle.SINGLE);
        setShowRollOver(false);
        setSortField("name");
        setShowHeader(false);

        setDataSource(new ContentRepositoryTreeDataSource());

        addNodeClickHandler(new NodeClickHandler() {
            public void onNodeClick(NodeClickEvent event) {
                String path = event.getNode().getAttribute("id").replaceAll(":", "/");
                History.newItem("Bundles/Repository/" + path);
            }
        });
    }

    public void selectPath(ViewPath viewPath) {

        if (viewPath.viewsLeft() > 0) {
            String key = "";
            for (ViewId view : viewPath.getViewPath().subList(2, viewPath.getViewPath().size())) {
                if (key.length() > 0)
                    key += ":";

                key += view.getPath();

                TreeNode node = getTree().findById(key);
                if (node != null) {
                    getTree().openFolder(node);
                }
            }

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
            selectRecord(0);
        }


    }
}