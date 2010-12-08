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

import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTopView extends LocatableHLayout implements BookmarkableView {
    public static final ViewName VIEW_ID = new ViewName("ResourceGroup", MSG.common_title_resource_group());

    private Canvas contentCanvas;
    private ResourceGroupTreeView treeView;
    private ResourceGroupDetailView detailView;
    private boolean isAutoClusterView = false;

    public ResourceGroupTopView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();

        treeView = new ResourceGroupTreeView(extendLocatorId("Tree"));
        addMember(treeView);

        contentCanvas = new Canvas();
        addMember(contentCanvas);
    }

    public void setContent(Canvas newContent) {
        for (Canvas child : this.contentCanvas.getChildren()) {
            child.destroy();
        }
        this.contentCanvas.addChild(newContent);
        this.contentCanvas.markForRedraw();
    }

    public void renderView(final ViewPath viewPath) {
        boolean isAutoClusterPath = "AutoCluster".equals(viewPath.getCurrent().getPath());

        if (isAutoClusterPath != isAutoClusterView) {
            detailView = null;
        }

        if (null == detailView) {
            if (isAutoClusterPath) {
                detailView = new ResourceGroupDetailView(this.extendLocatorId("AutoClusterDetail"),
                    ResourceGroupDetailView.AUTO_CLUSTER_VIEW_PATH);
            } else {
                detailView = new ResourceGroupDetailView(this.extendLocatorId("groupDetail"), this.VIEW_ID.getName());
            }

            this.setContent(detailView);
        }

        treeView.renderView(viewPath);
        detailView.renderView(viewPath);
    }
}
