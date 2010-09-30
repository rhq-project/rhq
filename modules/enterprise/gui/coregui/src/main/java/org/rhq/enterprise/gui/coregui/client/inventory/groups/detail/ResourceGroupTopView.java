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
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTopView extends LocatableHLayout implements BookmarkableView {
    public static final String VIEW_ID = "ResourceGroup";

    private Canvas contentCanvas;
    private ResourceGroupTreeView treeView;
    private ResourceGroupDetailView detailView;

    public ResourceGroupTopView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        treeView = new ResourceGroupTreeView(extendLocatorId("Tree"));
        detailView = new ResourceGroupDetailView(extendLocatorId("Detail"), ResourceGroupTopView.VIEW_ID);
        addMember(treeView);

        contentCanvas = new Canvas();
        addMember(contentCanvas);

        setContent(detailView);
    }

    public void setContent(Canvas newContent) {
        for (Canvas child : this.contentCanvas.getChildren()) {
            child.destroy();
        }
        this.contentCanvas.addChild(newContent);
        this.contentCanvas.markForRedraw();
    }

    public void renderView(final ViewPath viewPath) {
        treeView.renderView(viewPath);
        detailView.renderView(viewPath);
    }
}
