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

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.InventoryView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTopView extends HLayout implements BookmarkableView {

    private Canvas contentCanvas;

    private ResourceGroupComposite currentGroup;

    private ResourceGroupTreeView treeView;
    private ResourceGroupDetailView detailView;

    private ResourceGroupGWTServiceAsync groupService = GWTServiceLookup.getResourceGroupService();


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

        detailView = new ResourceGroupDetailView();

//        treeView.addResourceSelectListener(detailView);

        setContent(detailView);
    }


    public void setSelectedGroup(final int groupId, final ViewPath view) {
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        //criteria.fetchTags(true);
        groupService.findResourceGroupCompositesByCriteria(criteria, new AsyncCallback<PageList<ResourceGroupComposite>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getMessageCenter().notify(new Message("Group with id [" + groupId +
                        "] does not exist or is not accessible.", Message.Severity.Warning));
                caught.printStackTrace();
                CoreGUI.goTo(InventoryView.VIEW_PATH);
            }

            public void onSuccess(PageList<ResourceGroupComposite> result) {
                if (result.isEmpty()) {
                    //noinspection ThrowableInstanceNeverThrown
                    onFailure(new Exception("Group with id [" + groupId + "] does not exist."));
                } else {
                    currentGroup = result.get(0);
                    treeView.setSelectedGroup(currentGroup.getResourceGroup().getId());
                    detailView.onGroupSelected(currentGroup);
                }
            }
        });
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

        Integer groupId = Integer.parseInt(viewPath.getCurrent().getPath());

        if (currentGroup == null || currentGroup.getResourceGroup().getId() != groupId) {
            // The previous bookmark did not already point to this group.
            setSelectedGroup(groupId, viewPath);
        }

        viewPath.next();
        this.detailView.renderView(viewPath);                
    }

}
