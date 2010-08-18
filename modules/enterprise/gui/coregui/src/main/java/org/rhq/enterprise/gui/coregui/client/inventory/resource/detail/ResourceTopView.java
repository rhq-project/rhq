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

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceTopView extends HLayout implements BookmarkableView {

    private Canvas contentCanvas;

    private ResourceComposite currentResource;
    //private Resource resourcePlatform;

    private ResourceTreeView treeView;
    private ResourceDetailView detailView = new ResourceDetailView();

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();


    public ResourceTopView() {

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
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        criteria.fetchTags(true);
        //criteria.fetchParentResource(true);
        resourceService.findResourceCompositesByCriteria(criteria, new AsyncCallback<PageList<ResourceComposite>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getMessageCenter().notify(new Message("Resource with id [" + resourceId +
                        "] does not exist or is not accessible.", Message.Severity.Warning));

                CoreGUI.goTo(InventoryView.VIEW_PATH);
            }

            public void onSuccess(PageList<ResourceComposite> result) {
                if (result.isEmpty()) {
                    //noinspection ThrowableInstanceNeverThrown
                    onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                } else {
                    final ResourceComposite resourceComposite = result.get(0);
                    loadResourceType(resourceComposite, view);
                }
            }
        });
    }


    private void loadResourceType(final ResourceComposite resourceComposite, final ViewPath view) {
        final Resource resource = resourceComposite.getResource();
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resource.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.content, ResourceTypeRepository.MetadataType.operations,
                ResourceTypeRepository.MetadataType.events,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    resource.setResourceType(type);
                    completeSetSelectedResource(resourceComposite, view);
                }
            });
    }


    private void completeSetSelectedResource(ResourceComposite resourceComposite, ViewPath viewPath) {
        this.currentResource = resourceComposite;
        this.detailView.onResourceSelected(resourceComposite);
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

        if (currentResource == null || currentResource.getResource().getId() != resourceId) {
            // The previous history item did not already point to this Resource.
            setSelectedResource(resourceId, viewPath);
        }

        this.treeView.renderView(viewPath);

        viewPath.next();
        this.detailView.renderView(viewPath);
    }

}
