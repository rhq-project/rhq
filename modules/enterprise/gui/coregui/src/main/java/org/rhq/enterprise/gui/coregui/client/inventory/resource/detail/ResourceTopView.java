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
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 */
public class ResourceTopView extends LocatableHLayout implements BookmarkableView {

    private Canvas contentCanvas;

    private ResourceTreeView treeView;
    private ResourceDetailView detailView = new ResourceDetailView(extendLocatorId("Detail"));

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceTopView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();

        treeView = new ResourceTreeView(getLocatorId());
        addMember(treeView);

        contentCanvas = new Canvas();
        addMember(contentCanvas);

        setContent(detailView);
    }

    public void setContent(Canvas newContent) {
        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();
        contentCanvas.addChild(newContent);
        contentCanvas.markForRedraw();
    }

    public void renderView(ViewPath viewPath) {
        this.treeView.renderView(viewPath);
        this.detailView.renderView(viewPath);
    }

}
