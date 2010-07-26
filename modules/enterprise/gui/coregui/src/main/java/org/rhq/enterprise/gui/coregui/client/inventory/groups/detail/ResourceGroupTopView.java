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
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeView;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTopView extends HLayout implements BookmarkableView {

    private Canvas contentCanvas;

    private ResourceGroup currentGroup;
    //private Resource resourcePlatform;

    private ResourceGroupTreeView treeView;
    private ResourceGroupDetailView detailView = new ResourceGroupDetailView();

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceGroupTopView() {

    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();


        treeView = new ResourceGroupTreeView();
        addMember(treeView);

        contentCanvas = new Canvas();
        addMember(contentCanvas);


        // created above
        detailView = new ResourceGroupDetailView();

//        treeView.addResourceSelectListener(detailView);


        setContent(detailView);

    }
/*
    public void setSelectedResource(final int resourceId, final ViewPath view) {
        Resource resource = this.treeView.getResource(resourceId);
        if (resource != null) {
            setSelectedResource(resource, view);
        } else {
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterId(resourceId);
            criteria.fetchTags(true);
            //criteria.fetchParentResource(true);
            resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getMessageCenter().notify(new Message("Resource with id [" + resourceId +
                            "] does not exist or is not accessible.", Message.Severity.Warning));

                    CoreGUI.goTo(InventoryView.VIEW_PATH);
                }

                public void onSuccess(PageList<Resource> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                    } else {
                        Resource resource = result.get(0);
                        setSelectedResource(resource, view);
                    }
                }
            });
        }
    }

    private void setSelectedResource(Resource resource, ViewPath viewPath) {
        this.currentResource = resource;
        this.treeView.setSelectedResource(resource, viewPath.getCurrent());
        this.detailView.onResourceSelected(resource);
    }*/

    public void setContent(Canvas newContent) {
        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();
        contentCanvas.addChild(newContent);
        contentCanvas.markForRedraw();
    }


    public void renderView(ViewPath viewPath) {
        if (viewPath.isEnd()) {
            // default detail view
            viewPath.getViewPath().add(new ViewId("Summary"));
            viewPath.getViewPath().add(new ViewId("Overview"));
        }

        Integer groupId = Integer.parseInt(viewPath.getCurrent().getPath());




        if (currentGroup == null || currentGroup.getId() != groupId) {

//            setSelectedResource(resourceId, viewPath);

            this.treeView.setSelectedGroup(groupId);




            viewPath.next();

            this.detailView.renderView(viewPath);

        }
    }

}
