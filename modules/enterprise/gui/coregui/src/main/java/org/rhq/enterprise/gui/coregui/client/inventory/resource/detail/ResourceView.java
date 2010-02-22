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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceTreeView;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;

import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ResourceView extends HLayout implements Presenter {

    private Canvas contentCanvas;


    private Resource selectedResource;
    private Resource resourcePlatform;


    private ResourceTreeView treeView;
    private ResourceDetailView detailView;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceView() {

    }

    public ResourceView(Resource selectedResource) {
        this.selectedResource = selectedResource;

        System.out.println("displaying resource: " + selectedResource);
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

    public boolean fireDisplay(Place place, List<Place> children) {
        try {
            if (place.equals(getPlace())) {
                if (children.size() > 0) {
                    Place resourcePlace = children.get(0);
                    int resourceId = Integer.parseInt(resourcePlace.getId());

                    if (selectedResource == null || selectedResource.getId() != resourceId) {
                        setSelectedResource(resourceId);
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



    public void setSelectedResource(int resourceId) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                SC.say("failed to load");
            }

            public void onSuccess(PageList<Resource> result) {
                Resource res = result.get(0);
                setSelectedResource(res);
            }
        });
    }


    public void setSelectedResource(Resource resource) {
        this.selectedResource = resource;
        treeView.setSelectedResource(resource);
        detailView.onResourceSelected(resource);
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
}
