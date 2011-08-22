/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceTopView extends LocatableHLayout implements BookmarkableView {
    public static final ViewName VIEW_ID = new ViewName("Resource", MSG.common_title_resource());

    private Canvas contentCanvas;
    private ResourceTreeView treeView;
    private ResourceDetailView detailView;
    private ResourceGroupDetailView autoGroupDetailView;

    public ResourceTopView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();

        treeView = new ResourceTreeView(getLocatorId());
        addMember(treeView);

        contentCanvas = new Canvas();
        addMember(contentCanvas);

        setToDetailView();
    }

    public void setContent(Canvas newContent) {
        for (Canvas child : this.contentCanvas.getChildren()) {
            child.destroy();
        }
        this.contentCanvas.addChild(newContent);
        this.contentCanvas.markForRedraw();
    }

    private void setToDetailView() {

        this.detailView = new ResourceDetailView(extendLocatorId("Detail"));
        this.detailView.addViewRenderedListener(new ResourceDetailView.ViewRenderedListener() {

            public void onViewRendered() {
                // re-enable the tree, the detail view has (hopefully) been safely rendered
                treeView.enable();
            }
        });
        this.setContent(detailView);
        this.autoGroupDetailView = null;
    }

    private void setToAutoGroupView() {
        this.autoGroupDetailView = new ResourceGroupDetailView(this.extendLocatorId("AutoGroupDetail"),
            ResourceGroupDetailView.AUTO_GROUP_VIEW);
        this.autoGroupDetailView.addViewRenderedListener(new ResourceGroupDetailView.ViewRenderedListener() {

            public void onViewRendered() {
                // re-enable the tree, the autogroup view has (hopefully) been safely rendered
                treeView.enable();
            }
        });

        this.setContent(this.autoGroupDetailView);
        this.detailView = null;
    }

    public void renderView(ViewPath viewPath) {
        // disable the tree until it's safe to click a new node to prevent fast-click issues
        this.treeView.disable();

        if ("AutoGroup".equals(viewPath.getCurrent().getPath())) {
            if (null == autoGroupDetailView) {
                setToAutoGroupView();
            }

            this.treeView.renderView(viewPath);
            this.autoGroupDetailView.renderView(viewPath.next());
        } else {
            if (null == detailView) {
                setToDetailView();
            }

            this.treeView.renderView(viewPath);
            this.detailView.renderView(viewPath);
        }

    }
}
