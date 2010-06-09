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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceView extends HLayout implements BookmarkableView {

    private Canvas contentCanvas;

    private Resource currentResource;
    //private Resource resourcePlatform;

    private ResourceTreeView treeView;
    private ResourceDetailView detailView = new ResourceDetailView();

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceView() {

    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();


        treeView = new ResourceTreeView();
        addMember(treeView);

        contentCanvas = new Canvas();
        addMember(contentCanvas);


        // created above
//        detailView = new ResourceDetailView();

        treeView.addResourceSelectListener(detailView);


        setContent(detailView);

    }

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
    }

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

        Integer resourceId = Integer.parseInt(viewPath.getCurrent().getPath());

        if (currentResource == null || currentResource.getId() != resourceId) {

            setSelectedResource(resourceId, viewPath);

            this.treeView.renderView(viewPath);

            viewPath.next();

            this.detailView.renderView(viewPath);

        }
    }

}
