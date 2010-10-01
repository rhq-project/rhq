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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 */
public class AutoGroupTopView extends LocatableHLayout implements BookmarkableView {

    private Canvas contentCanvas;
    private Integer parentResourceId;
    private ResourceTreeView treeView;
    private ResourceGroupDetailView detailView;

    public AutoGroupTopView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        treeView = new ResourceTreeView(getLocatorId());
        detailView = new ResourceGroupDetailView(extendLocatorId("Detail"),
            ResourceGroupDetailView.AUTO_GROUP_VIEW_PATH);
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
        // we need the backing group parent resource id to render the LHS resource tree. Once it's rendered leave
        // it displayed but don't muck with it.  A LHS user selection will drive a view change.
        if (null == this.parentResourceId) {
            final int backingGroupId = Integer.valueOf(viewPath.getCurrent().getPath());
            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.addFilterId(backingGroupId);
            criteria.addFilterVisible(false);
            GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
                new AsyncCallback<PageList<ResourceGroup>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to fetch autogroup backing group", caught);
                    }

                    public void onSuccess(PageList<ResourceGroup> result) {
                        if (result.isEmpty()) {
                            CoreGUI.getErrorHandler().handleError(
                                "Failed to find autogroup backing group: " + backingGroupId);
                        } else {
                            String parentResourceId = String
                                .valueOf(result.get(0).getAutoGroupParentResource().getId());
                            treeView.renderView(new ViewPath(parentResourceId));
                        }
                    }
                });
        }
        detailView.renderView(viewPath);
    }
}
