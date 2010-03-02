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

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.UnknownViewException;
import org.rhq.enterprise.gui.coregui.client.View;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewRenderer;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourcesView;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;

import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ResourceView extends HLayout implements Presenter, ViewRenderer {

    private Canvas contentCanvas;

    private Resource selectedResource;
    //private Resource resourcePlatform;

    private ResourceTreeView treeView;
    private ResourceDetailView detailView;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceView() {

    }

    public ResourceView(Resource selectedResource) {
        this.selectedResource = selectedResource;

        System.out.println("Displaying Resource: " + selectedResource);
    }

    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();


        treeView = new ResourceTreeView(selectedResource);
        addMember(treeView);

        contentCanvas = new Canvas();
        addMember(contentCanvas);


        detailView = new ResourceDetailView();

        treeView.addResourceSelectListener(detailView);


        setContent(detailView);

    }

    public boolean fireDisplay(Place base, List<Place> subLocations) {
        try {
            if (base.equals(getPlace())) {
                if (subLocations.size() > 0) {
                    Place resourcePlace = subLocations.get(0);
                    int resourceId = Integer.parseInt(resourcePlace.getId());

                    if (selectedResource == null || selectedResource.getId() != resourceId) {
                        setSelectedResource(resourceId, null);
                    }
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }


    public void setSelectedResource(final int resourceId, final ViewId viewId) {
        Resource resource = this.treeView.getResource(resourceId);
        if (resource != null) {
            setSelectedResource(resource);
            CoreGUI.updateBreadCrumbDisplayName(viewId, resource.getName());
        } else {
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterId(resourceId);
            resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
                public void onFailure(Throwable caught) {
                    SC.say("Failed to load Resource with id " + resourceId + ": " + caught);
                    // TODO: Display this error in a red box at top of page instead?
                    CoreGUI.goTo(ResourcesView.VIEW_PATH);
                }

                public void onSuccess(PageList<Resource> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                    } else {
                        Resource resource = result.get(0);
                        setSelectedResource(resource);
                        CoreGUI.updateBreadCrumbDisplayName(viewId, resource.getName());
                    }
                }
            });
        }
    }


    public void setSelectedResource(Resource resource) {
        this.selectedResource = resource;
        this.treeView.setSelectedResource(resource);
        this.detailView.onResourceSelected(resource);
    }


    public Place getPlace() {
//        return new Place("Resource[" + selectedResource.getId() + "]", "Resource - " + selectedResource.getName());
        return new Place("Resource", "Resource");
    }

    public void setContent(Canvas newContent) {
        if (contentCanvas.getChildren().length > 0)
            contentCanvas.getChildren()[0].destroy();
        contentCanvas.addChild(newContent);
        contentCanvas.draw();
    }

    public View renderView(ViewId viewId, View parentView, boolean lastNode) throws UnknownViewException {
        String parentPath = parentView.getId().getPath();
        if (!parentPath.equals("Resource")) {
            throw new UnknownViewException();
        }
        int resourceId;
        try {
            resourceId = Integer.parseInt(viewId.getName());
        } catch (NumberFormatException e) {
            // not a valid Resource id - nothing for us to do
            throw new UnknownViewException("Invalid Resource id [" + viewId + "]");
        }
        if (this.selectedResource == null || this.selectedResource.getId() != resourceId) {
            setSelectedResource(resourceId, viewId);
        }
        // Use "..." as temporary display name for breadcrumb. If the Resource is fetched successfully, the display name
        // will be updated to be the Resource's name.
        Breadcrumb breadcrumb = new Breadcrumb(viewId.getName(), "...");
        return new View(viewId, breadcrumb);
    }
}
